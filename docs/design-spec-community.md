# Community: sections, topics, following, and how a list opens

Decisions from the 25.10.2026 walkthrough, drawn on the Claude Design canvas
"TierYourLife: прогон 25.10" (nine boards) and approved by Danylo the same
evening. The board spec (`design-spec-board.md`) covers the board itself; this
one covers everything around a published list. Each section names what is
built and where the rest stands.

## 1. Sections and topics

Two levels. The nine fixed, localised **sections** (`ListCategory`) stay. Inside
them people create **topics**: a word of somebody's own, "Pokémon", "Marvel",
"Kharkiv restaurants". A topic's name is user text, not translated, searched
as typed.

| Rule | Why |
|---|---|
| A section is required when a list goes public; a topic is optional | A list nobody can browse to is not published, it is lost |
| A new topic waits for a moderator; until then its lists sit under **Other** with the topic as a visible label | The feed must not grow a thousand one-list sections; the label keeps the author's word in view meanwhile |
| On approval the moderator assigns the section (or renames, merges, rejects) and the lists move | One decision moves everything; nobody re-publishes |
| At most **3** pending topics per account | Enough to be useful, too few to flood the queue |
| A new name equal to an approved topic's attaches to it instead | Twins are prevented where they are born |
| Rejected: the label disappears, the lists stay where they are | Nothing a reader kept is taken away |
| Soft guards, in TierMaker's spirit: a suggestion at publish when the title looks like an existing topic, never a block; a report reason "wrong place"; moderators can move any list | Hard checks on free text punish honest people first |

**Proxy** (tieryourlife-proxy#30): `topics`, `topicFollows`, `roles` collections;
`GET lists/topics?q=&section=`, `lists/topics/mine`, `lists/topics/pending`,
`lists/topics/{id}` with follow / approve / rename / merge / reject;
`POST lists/{id}/move`; publish takes `topicId` or `newTopic`;
`GET lists?topic=`; the following feed merges followed people and followed
topics. Moderators: the configured owner alone appoints and removes
(`lists/moderators`). Deployed only when Danylo deploys.

**App** (`topics-in-the-app`, one PR): a board's settings carry a **Topic**
row that opens a picker: search the server's topics, take the one the title
names (suggested, never forced), or propose a new one; the picker says how
many of the three are waiting and refuses the fourth. Publishing sends the
topic by id or the new name and keeps the id the server answers with; Room is
at schema 12 for it. The community home shows **Your topics** as chips above
the sections (a waiting one says so); a card names its topic. A topic page
has the section as its small word, the name, the counts, Follow / Unfollow,
Popular / Newest, and a long press offers **Not this topic**, which files a
`wrong_category` report and hides nothing. Moderation has three chips:
Reports, Topics (approve into a section, rename, merge into a found topic,
reject) and, for the owner alone, Moderators (appoint by account id, remove);
an author's page offers the owner "make / remove moderator" once that list
has answered.

**After review** (same PR): Follow exists only on an approved topic, since
the server refuses the rest. A board whose new name the server took keeps the
name beside the id while a moderator has yet to decide, so the settings row
keeps saying *waiting*; the picker settles it from the server's later word
(approved clears the mark, merged points the board at the surviving topic,
rejected takes the topic off). A topic the server will not take at publish
(`TOPIC_UNAVAILABLE`, tieryourlife-proxy#32) is dropped from the board with
its own message instead of a retry that can never succeed. A `wrong_category`
report has a **Move…** action in the Reports chip: pick the section, the topic
is dropped, the complaints close. The People / Topics chips over a following
page ask for the next pages on their own when the one on screen holds nothing
of what they narrow to, four pages at most, and the strip stays meanwhile.
`GET lists/following` is read once per load.

Left out of this pass, for later: the "new list here" button on a topic page,
the 3×3 section grid with counts on the home screen (the section row stays),
and a board learning that its topic was renamed or placed after the fact
anywhere but in the picker (which settles it when it opens).

## 2. Following

People and topics in one feed, newest first, cut into days (Today, Yesterday,
a date). Above it a strip: "You follow · N", the faces, **Manage**. Manage is a
plain list of everyone followed with **Unfollow** on each row; a tap answers at
once and a refusal puts the person back where they were. A filter
All / People / Topics arrives with topics.

Built for people in #166 on `GET lists/following` (tieryourlife-proxy#29).
Topics arrived with section 1's app work: chips in the strip (a tap opens the
topic), a Topics section in Manage with Unfollow, and the All / People /
Topics filter, which narrows the page the server already sent rather than
asking again, since the proxy hands the following feed over as one.

## 3. Opening a list: no blank frame

The card that is tapped already knows the title, the author, the number of
cards and the palette, so the route carries them and the screen draws a
skeleton from them at once: the real top bar, the author line, one row per
tier colour with grey cards spread by the count. The board replaces it with a
150 ms fade. Moderation, which opens by id alone, keeps the spinner.

Transitions are one set of rules in the host: the next screen slides in
whole over the one beneath, which stays as it is (260 ms); back slides it out
the same way, slower and eased at both ends (340 ms; 220 ms read as yanked); top-level siblings only fade. Nothing else fades, since
two translucent screens over a dark window read as a flash. The skeleton's
rows use the board's own tile size and leave room for the folded pool strip,
so nothing moves when the board lands. "Save to my lists" stays
on the screen: the bar says the copy exists and offers to open it, and
opening takes the published list's place on the back stack.

Built in #161.

## 4. The pool as a sheet

The panel under the rows folds to a strip (handle, count, a glimpse of three
pictures, the Add chip) and opens to the row of cards. It opens on its own
only for someone's own board with cards left to rank; a reader's pool and an
empty one start folded. A tap or a pull switches. Built in #164. On a community list the strip
also holds the reader's Theirs / Mine switch and the Save button, with the
sentence that explains it, so both are in reach whether the pool is folded or
open.

Where the pool stands beside the rows (a short window, a wide one held
sideways; the board spec's section 7), the switch and Save move into the bar
(the switch at 32dp, Save at 36dp) and the sentence goes under the side pool's
title. A short window's single bar also carries the author as a chip, and the
rankings chip when there are any, since the author line has no row of its own
there. On a tablet held upright the bar is 72dp: the title over "author ·
Section ›" (a tap opens the profile), the switch at 40dp and a tonal Save with
the bookmark icon, its label shortened to "Save" while its description stays
"Save to my lists"; the sentence goes under the docked pool's head. The
skeleton follows the same rules, so nothing moves when the list lands.

## 5. The cover

One chosen picture shows alone; two to six make a collage of exactly those, in
the order chosen; six is the most. With nothing chosen, up to six distinct card
pictures, never a repeat (#156); one picture alone; none at all shows the tier
colours. Layout rule, shared with `mosaicRows()`: up to three in a row, from
four two rows (2+2, 3+2, 3+3). The picker has two tabs, the board's own
pictures and the same catalogue search the cards use; results without a
picture are cut (#150). Built in #163; the proxy carries the chosen collage to
the feed and the kept board (tieryourlife-proxy#28); the app's sync of the
collage follows.

## 6. Smaller rules settled the same day

- A card lifts under the finger at once; a vertical move scrolls, a sideways one drags (#154).
- A new board starts with letters alone; the band is one fixed width and a caption wraps or is cut inside it (#151).
- The system back leaves at once, like the in-app arrow (#149).
- "Unfollow" is written on the button that does it (#153).
- A face fills its circle, and can be a photo from the phone (#155, #160).

## 7. Rankings: one list, many placings

Decided with Danylo on 26.10, after looking at how TierMaker lives with
duplicates (templates are shared, a tier list is one person's placing of a
template; twins are only reported and removed by hand). Here it is settled by
the server rather than by a complaint:

| Rule | Why |
|---|---|
| A copy taken from somebody's list and published with the **same cards** (a picture is the same picture, a card without one is its words) becomes this person's **ranking** of that list, not a second list; the original counts it | The feed does not fill with the same twelve posters placed twelve ways |
| Cards added, dropped or swapped make it a **list of its own**, with "based on" naming the source | A changed set is new work; the credit is the only debt |
| The author cannot rank their own list; a list under review takes no rankings; a list taken down takes its rankings with it | Nothing to count, nothing to show without the list |
| Publishing the same copy again replaces the ranking; the switch that published it takes it down | One person, one placing per list |

**Proxy** (tieryourlife-proxy#31): publish carries `takenFrom`; the same
multiset of cards writes `rankings/{listId}_{uid}` (arrangement over the
source's cards, the tier words, who and when) and answers
`{id: <source>, ranking: true}`; otherwise a new list with `basedOn`.
`GET lists/{id}/rankings?after=` pages newest first with the reader's own
ahead of the first page; `DELETE lists/{id}/rankings/mine`. Summaries carry
`rankingCount` and `basedOn`.

**App** (`rankings-of-a-list`): a copy remembers `takenFrom` (Room 13, with
`rankingOf`); the board's publish sends it and, answered as a ranking, keeps
`rankingOf` instead of a `publishedId`, says so in a snackbar and in the
Public row, and shows a credit under the title with a way to the original.
Turning the switch off takes the ranking down; deleting the board does too.
On somebody's list a chip under the author says "N rankings" and opens a
sheet: the author first, then this reader, then the rest by freshness; the
chosen one fills the left half of the Their / Mine switch under their name,
and Save keeps whichever placing is on screen. "Based on" under the author
opens the source. The site is to follow.

Not in this pass: the board learning that the list it ranks has gone (the
next publish simply makes it a list of its own), the count on feed cards.
