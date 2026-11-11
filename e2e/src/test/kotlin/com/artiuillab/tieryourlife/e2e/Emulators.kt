package com.artiuillab.tieryourlife.e2e

import com.artiuillab.tieryourlife.core.network.networkJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/** Who a phone is signed in as: the Auth emulator's uid and an ID token for it. */
data class Identity(val uid: String, val idToken: String, val email: String?, val password: String?)

/**
 * The proxy under `demo-tieryourlife`, as its CONTRACT.md describes. The
 * addresses come from the environment so a second suite on other ports works:
 * `firebase emulators:exec` sets FIREBASE_AUTH_EMULATOR_HOST itself.
 */
object Emulators {

    val functionsUrl: String =
        (System.getenv("E2E_FUNCTIONS_URL") ?: "http://127.0.0.1:5001/$PROJECT/europe-west1").trimEnd('/') + "/"

    internal val authHost: String = System.getenv("FIREBASE_AUTH_EMULATOR_HOST") ?: "127.0.0.1:9099"

    private val http = OkHttpClient()
    private val jsonType = "application/json".toMediaType()

    fun guest(): Identity = identity("signUp", buildJsonObject { }, email = null, password = null)

    /** A fresh signed-in account per call, so no test sees another's lists or boards. */
    fun newAccount(): Identity {
        val email = "phone-${UUID.randomUUID()}@e2e.test"
        val password = UUID.randomUUID().toString()
        return identity("signUp", credentials(email, password), email, password)
    }

    /** The same account again, as a second phone signing in would get it. */
    fun signIn(account: Identity): Identity {
        val email = checkNotNull(account.email)
        val password = checkNotNull(account.password)
        return identity("signInWithPassword", credentials(email, password), email, password)
    }

    /**
     * The owner `.env.demo-tieryourlife` names, made the way the proxy's seed
     * makes it; already there after a seed or an earlier test, it is only signed into.
     */
    fun owner(): Identity {
        val made = post(
            "http://$authHost/identitytoolkit.googleapis.com/v1/projects/$PROJECT/accounts",
            buildJsonObject {
                put("localId", OWNER_UID)
                put("email", OWNER_EMAIL)
                put("password", OWNER_PASSWORD)
                put("emailVerified", true)
            },
            bearer = "owner",
        )
        check(made.first == HTTP_OK || "DUPLICATE" in made.second || "EXISTS" in made.second) {
            "The Auth emulator would not make the owner (${made.first}): ${made.second}"
        }
        return identity("signInWithPassword", credentials(OWNER_EMAIL, OWNER_PASSWORD), OWNER_EMAIL, OWNER_PASSWORD)
    }

    /** A call the app never makes, for setting the stage: answers the status and the body. */
    fun call(identity: Identity, method: String, path: String, body: JsonObject? = null): Pair<Int, String> {
        val request = Request.Builder()
            .url(functionsUrl + path)
            .header("Authorization", "Bearer ${identity.idToken}")
            .method(method, body?.toString()?.toRequestBody(jsonType))
            .build()
        return http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
    }

    private fun identity(method: String, body: JsonObject, email: String?, password: String?): Identity {
        val (status, text) = post(
            "http://$authHost/identitytoolkit.googleapis.com/v1/accounts:$method?key=demo",
            JsonObject(body + ("returnSecureToken" to JsonPrimitive(true))),
        )
        check(status == HTTP_OK) { "The Auth emulator answered $method with $status: $text" }
        val answer = networkJson.parseToJsonElement(text).jsonObject
        return Identity(
            uid = answer.getValue("localId").jsonPrimitive.content,
            idToken = answer.getValue("idToken").jsonPrimitive.content,
            email = email,
            password = password,
        )
    }

    private fun post(url: String, body: JsonObject, bearer: String? = null): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonType))
            .apply { bearer?.let { header("Authorization", "Bearer $it") } }
            .build()
        return http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
    }

    private fun credentials(email: String, password: String) = buildJsonObject {
        put("email", email)
        put("password", password)
    }

    const val PROJECT = "demo-tieryourlife"

    // The proxy's own test values (functions/scripts/seed-e2e.ts), for a project that exists only in the emulators.
    private const val OWNER_UID = "e2eOwner0001"
    private const val OWNER_EMAIL = "owner@e2e.test"
    private const val OWNER_PASSWORD = "e2e-owner-password"
    private const val HTTP_OK = 200
}
