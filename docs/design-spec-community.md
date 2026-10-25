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

**App**: not yet built. Screens on the canvas: Community home (your topics as
chips, the nine sections as a grid with counts, the feed), the topic page
(follow, "new list here", popular / recent), the publish sheet (section chips
required, topic field with typeahead and "create a topic", the 1-of-3 note),
the moderation queue with a Topics tab and a Moderators tab (owner only).

## 2. Following

People and topics in one feed, newest first, cut into days (Today, Yesterday,
a date). Above it a strip: "You follow · N", the faces, **Manage**. Manage is a
plain list of everyone followed with **Unfollow** on each row; a tap answers at
once and a refusal puts the person back where they were. A filter
All / People / Topics arrives with topics.

Built for people in #166 on `GET lists/following` (tieryourlife-proxy#29).
Topics in the strip and the filter come with section 1's app work.

## 3. Opening a list: no blank frame

The card that is tapped already knows the title, the author, the number of
cards and the palette, so the route carries them and the screen draws a
skeleton from them at once: the real top bar, the author line, one row per
tier colour with grey cards spread by the count. The board replaces it with a
150 ms fade. Moderation, which opens by id alone, keeps the spinner.

Transitions are one set of rules in the host: the next screen slides in a
tenth of the width and fades up over the one beneath (220 ms); back runs the
same way out (180 ms); top-level siblings only fade. "Save to my lists" stays
on the screen: the bar says the copy exists and offers to open it, and
opening takes the published list's place on the back stack.

Built in #161.

## 4. The pool as a sheet

The panel under the rows folds to a strip (handle, count, a glimpse of three
pictures, the Add chip) and opens to the row of cards. It opens on its own
only for someone's own board with cards left to rank; a reader's pool and an
empty one start folded. A tap or a pull switches. Built in #164. The community
screen's Theirs / Mine switch and the Save bar move into the strip next.

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
