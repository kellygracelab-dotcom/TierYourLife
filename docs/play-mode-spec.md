# Play mode: the shared engine

Both platforms must turn the same answers into the same tiers. This file describes the engine exactly.

- Reference implementation: `feature/play/domain` (pure Kotlin).
- Shared vectors: [`play-vectors.json`](play-vectors.json). The app's `SharedVectorsTest` checks the file. The website mirrors the file and checks it in its own suite.
- Any change that moves a number is a change on both sides. Regenerate the file with `PLAY_VECTORS_WRITE=1 ./gradlew :feature:play:domain:test` and tell the web side.

Agreed between the app and web sessions on 28.10.2026.

## Cards and answers

- **Cards** are indices `0..n-1` in their original order. For a board that order is: tiers top to bottom, left to right, then the pool. That order breaks every tie.
- **A game** is described by `{kind, count, seed, refine}` together with its history. The engine keeps nothing else, so a game resumes from any device given the same history.
- **History** is a list of outcomes `[first, second, winner|null]`. A `null` winner is a **skip**:
  - A skip is no evidence. The scorer never sees it.
  - When the game needs someone to go on, the card with the lower index goes on.
- **Play is offered for 2–64 cards.** A bigger board asks for a subset first: the pool, or one tier.

## Randomness: mulberry32

The engine has one source of chance, a 32-bit integer generator:

```
state = seed (int32)
next(): state = (state + 0x6D2B79F5) | 0
        t = imul(state ^ (state >>> 15), 1 | state)
        t = (t + imul(t ^ (t >>> 7), 61 | t)) ^ t
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296
```

To shuffle `0..n-1` (Fisher–Yates):

```
for i from n-1 down to 1:
    j = floor(next() * (i + 1))
    swap(i, j)
```

## Games

### King of the hill (`hill`)

1. Shuffle the cards.
2. The champion is `order[0]`.
3. Each next card in the shuffled order challenges the champion. Whoever goes on is the new champion.

The game takes `n − 1` duels.

### Bracket (`bracket`)

1. Shuffle the cards; that is the first round.
2. Each round pairs `(r0, r1), (r2, r3), …`. With an odd count, the last card gets a bye.
3. The next round is the cards that went on, in order, with the bye card added at the end.

Rounds repeat until one card is left: `n − 1` duels in all.

### Swiss (`swiss`, "losers stay")

The game runs at most `swissRounds(n)` rounds, where `swissRounds(n)` = the smallest `r` with `2^r ≥ n`, plus 2. Compute it in integers.

Each round:
1. Rank the cards by real wins (descending), then by index.
2. Go down that ranking. Each card not yet paired takes the first card after it that is also unpaired and that it hasn't met. Met means any earlier outcome between the two, skips included.
3. A card that finds nobody sits the round out.
4. A round with no pairs ends the game.

A round's pairs are fixed from the history before the round starts, and they are asked in the order found.

### Refine (`refine: true`)

Refine runs after the main game ends, with at most `ceil(n / 2)` pairs and each card in at most one:
1. Score the main game's history (next section).
2. **Dashed cards first** (`played < 2`, see below), in standing order, best first. Each takes its nearest free neighbour that it hasn't met:
   - look upwards for the closest card above it, and downwards for the closest card below it;
   - the smaller gap in log-strength wins, and on a tie the card above wins.
3. Then fill the rest with neighbouring pairs in the order that haven't met and are both free, smallest log-strength gap first, then by position.

Every pair is written better-placed card first. The pairs are fixed when refining starts and asked in that order.

## Scoring: Bradley–Terry

- `wins[i]` is the number of real duels card `i` won.
- `met[i][j]` is the number of real duels between `i` and `j`.

Each card also plays a fixed **reference card of strength 1**, once each way, which gives it one virtual win and one virtual loss.

```
p[i] = 1.0 for all i
repeat 200 times:                     # fixed count, no tolerance
    for each i (all from the previous round's p):
        d = 0
        for j = 0..n-1, j != i, met[i][j] > 0:  d += met[i][j] / (p[i] + p[j])
        d += 2 / (p[i] + 1)
        next[i] = (wins[i] + 1) / d
    p = next
x[i] = round9(ln(p[i]))               # round9(v) = floor(v * 1e9 + 0.5) / 1e9
```

- `ln` is fdlibm. Kotlin uses `StrictMath.log`; V8's `Math.log` is an fdlibm port.
- Sums run in index order, as written.
- **The order** is by `x` descending, then `wins` descending, then index ascending. That order is also left to right within a tier.

## Tiers

`k` is the board's tier count (at least 1).

1. If there are no cards, every tier is empty. If `k == 1`, or every `x` is equal, every card goes in the first tier, in order.
2. Scale the cards to `v = (x − min) / (max − min)`, in order (best first).
3. **Groups.** Use Fisher's exact optimal split into consecutive groups, minimising the within-group sum of squared deviations, for `c = 1..min(k, n)`.
   - Prefix sums run in index order: `ssd(a, b) = max(0, S2 − S1² / len)`.
   - When splits cost the same, the earliest start (`<` scanning starts upward) wins.
   - The goodness of variance fit is `GVF(c) = 1 − cost(c) / ssd(all)`.
   - Use the smallest `c` with `GVF ≥ 0.9`. If none reaches it, use the largest allowed `c`.
   - **Thin evidence:** when answered duels per card (each duel counted for both cards: `Σ played / n`) is below 2, `c` is at most 3, whatever the fit.
4. **Placement.**
   - Group `g`, counted from the best, aims at tier `floor((1 − mean(v of g)) × (k − 1) + 0.5)`.
   - The first group always goes to tier 0. Each later group goes at least one tier below the one before.
   - Then, from the last group back, cap group `g` at `k − c + g`, so every group fits.
5. Cards sit in their group's tier in order.

The result: only the first tier is sure to be filled. A real gap leaves the tiers between empty, and the weakest cards still land low.

## Honesty about the result

- `played[i]` counts the real duels card `i` took part in; skips don't count.
- A card with `played < 2` is **dashed** in the result preview.
- The result is **thin**, and shows a banner, when more than a quarter of the cards are dashed (`dashed × 4 > n`).
- An empty tier between filled ones is not thin: it is the tiering showing a real gap.

The vectors carry `played` for every standing, and `thin` for every standing and game.

## Saving

- **Own board:** the result replaces the arrangement of the cards played. Cards that weren't played stay where they were. The board's history keeps the old arrangement.
- **Someone else's list (the app):** the result fills the "Mine" side, and "Save to my lists" keeps it as your own board with `takenFrom`. That counts as a save. Nothing on the backend changes.
- **Someone else's list (the website):** the result becomes the visitor's ranking (`/r/`), as Finish does today.
