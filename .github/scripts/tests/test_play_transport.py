"""Wire-level Publisher regressions, including real localhost redirect behavior."""
from email.message import Message
from http.server import BaseHTTPRequestHandler, HTTPServer
import io
import json
from pathlib import Path
import sys
import tempfile
import threading
import unittest
from unittest.mock import patch
import urllib.error
import urllib.parse

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from play_internal.play import Publisher
from play_internal.policy import Stop
from play_internal.transport import ApiError, PlayApi


class Response(io.BytesIO):
    def __init__(self, raw=b"{}", mime="application/json", status=200):
        super().__init__(raw)
        self.status = status
        self.headers = Message()
        self.headers["Content-Type"] = mime


class TransportTests(unittest.TestCase):
    def api(self):
        return PlayApi("synthetic-private-token", sleep=lambda _: None)

    def test_explicit_json_defaults_no_get_body_and_safe_empty_object(self):
        api = self.api()
        with patch.object(api.opener, "open", return_value=Response(b'{"releases":[]}')) as opened:
            result = api.request("GET", "/tracks/alpha/releases")
        request = opened.call_args.args[0]
        self.assertEqual(result, {"releases": []})
        self.assertEqual(request.get_header("Accept"), "application/json")
        self.assertIsNone(request.get_header("Content-type"))
        self.assertIsNone(request.data)
        self.assertEqual(urllib.parse.parse_qs(urllib.parse.urlparse(request.full_url).query), {"alt": ["json"], "$outputDefaults": ["true"]})

    def test_empty_html_wrong_mime_and_nonobject_never_become_empty_queue(self):
        cases = [(b"", "application/json", 200), (b"", "application/json", 204),
                 (b"<html>synthetic-private-token</html>", "text/html", 200),
                 (b'{"releases":[]}', "text/plain", 200), (b"null", "application/json", 200),
                 (b"[]", "application/json", 200), (b'{"releases":[],"releases":[]}', "application/json", 200)]
        for raw, mime, status in cases:
            api = self.api()
            with self.subTest(raw=raw, mime=mime, status=status), patch.object(api.opener, "open", side_effect=lambda *a, **k: Response(raw, mime, status)) as opened:
                with self.assertRaises(ApiError) as failure:
                    api.request("GET", "/tracks/alpha/releases")
                self.assertEqual(opened.call_count, 3)
                message = str(failure.exception)
                self.assertIn("mime=", message)
                self.assertIn("shape=", message)
                self.assertIn("/tracks/alpha/releases", message)
                self.assertNotIn("synthetic-private-token", message)
                self.assertNotIn("<html>", message)

    def test_malformed_commit_response_is_ambiguous_and_never_retried(self):
        api = self.api()
        with patch.object(api.opener, "open", return_value=Response(b"", status=200)) as opened:
            with self.assertRaises(ApiError):
                api.request("POST", "/edits/123:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        self.assertEqual(opened.call_count, 1)

    def test_snapshot_wire_failure_stops_before_bundle_track_or_commit_write(self):
        for malformed, mime in ((b"", "application/json"), (b"<html>unexpected</html>", "text/html")):
            api = self.api()
            calls = []
            def respond(request, **kwargs):
                path = urllib.parse.urlparse(request.full_url).path.split('/com.artiuillab.tieryourlife')[-1]
                calls.append((request.method,path))
                if path == '/edits':
                    value = {'id':'edit1'}
                elif path == '/edits/edit1/tracks':
                    value = {'tracks':[{'track':'internal'},{'track':'alpha'}]}
                elif path == '/edits/edit1/tracks/internal':
                    value = {'track':'internal'}
                elif path == '/tracks/internal/releases':
                    value = {'releases':[]}
                elif path == '/tracks/alpha/releases':
                    return Response(malformed,mime)
                else:
                    raise AssertionError('No release writes may follow an unknown queue response')
                return Response(json.dumps(value).encode())
            publisher = Publisher(api, {'versionCode':480,'signedSha256':'d'*64,'source':{'sha':'a'*40}}, lambda:None)
            with tempfile.TemporaryDirectory() as directory, self.subTest(malformed=malformed), patch.object(api.opener,'open',side_effect=respond), self.assertRaises(ApiError):
                publisher.begin(Path(directory)/'intent.json')
            self.assertEqual([call for call in calls if call[0]!='GET'],[('POST','/edits')])

    def test_structured_api_error_has_safe_reason_without_body_token(self):
        api = self.api()
        headers = Message()
        headers['Content-Type'] = 'application/json'
        raw = json.dumps({'error': {'status': 'FAILED_PRECONDITION', 'message': 'synthetic-private-token changesNotSentForReview',
                                   'details': [{'reason':'CHANGES_ALREADY_IN_REVIEW'}]}}).encode()
        error = urllib.error.HTTPError('https://unused.invalid', 400, 'Bad Request', headers, io.BytesIO(raw))
        with patch.object(api.opener, "open", side_effect=error), self.assertRaises(ApiError) as failure:
            api.request("POST", "/edits/123:commit")
        self.assertIn('CHANGES_ALREADY_IN_REVIEW', str(failure.exception))
        self.assertIn('changesNotSentForReview-parameter', str(failure.exception))
        self.assertNotIn('synthetic-private-token', str(failure.exception))

    def test_429_get_retries_but_403_is_not_downgraded_to_empty(self):
        for status, count in ((429,3),(403,1)):
            api = self.api()
            def error(*args, **kwargs):
                headers = Message()
                headers['Content-Type'] = 'text/html'
                raise urllib.error.HTTPError('https://unused.invalid', status, 'Failure', headers, io.BytesIO(b'<html>private</html>'))
            with patch.object(api.opener, "open", side_effect=error) as opened, self.assertRaises(ApiError):
                api.request("GET", "/tracks/production/releases")
            self.assertEqual(opened.call_count, count)

    def test_real_redirect_does_not_forward_authorization(self):
        requests = []
        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                requests.append(self.path)
                self.send_response(302)
                self.send_header('Location', f'http://127.0.0.1:{self.server.server_port}/token-leak')
                self.end_headers()
            def log_message(self, *args):
                pass
        server = HTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            api = self.api()
            with patch('play_internal.transport.BASE', f'http://127.0.0.1:{server.server_port}'), self.assertRaises(ApiError) as failure:
                api.request('GET','/origin')
            self.assertEqual(failure.exception.status,302)
            self.assertEqual(requests,['/origin?alt=json'])
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


class ReleaseSchemaTests(unittest.TestCase):
    def publisher(self, data):
        api = unittest.mock.Mock()
        api.request.return_value = data
        return Publisher(api, {'versionCode':480,'signedSha256':'d'*64,'source':{'sha':'a'*40}}, lambda:None)

    def test_only_valid_proto_empty_responses_count_as_empty(self):
        self.assertEqual(self.publisher({}).releases('alpha'), [])
        self.assertEqual(self.publisher({'releases':[]}).releases('alpha'), [])
        for data in (None, [], {'error':{}}, {'other':'not a release list'}, {'releases':None}, {'releases':{}}):
            with self.subTest(data=data), self.assertRaises(Stop):
                self.publisher(data).releases('alpha')

    def test_unknown_pagination_and_maximum_cannot_hide_a_pending_page(self):
        for data in ({'releases':[],'nextPageToken':'next'}, {'releases':[],'tokenPagination':{}},
                     {'releases':[{'track':'alpha'}]*20}):
            with self.subTest(data=data), self.assertRaises(Stop):
                self.publisher(data).releases('alpha')

    def test_wrong_track_and_malformed_artifact_stop(self):
        for release in ({'track':'production'}, {'track':'alpha','activeArtifacts':None},
                        {'track':'alpha','activeArtifacts':[{}]}, {'track':'alpha','activeArtifacts':[{'versionCode':True}]}):
            with self.subTest(release=release), self.assertRaises(Stop):
                self.publisher({'releases':[release]}).releases('alpha')


if __name__ == '__main__':
    unittest.main()
