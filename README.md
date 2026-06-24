# TierYourLife

> A personal ranking journal — tier everything that matters to you (movies first, then games and beyond)
> on **S / A / B / C / D** boards. Unlike a one-off tier-list maker, TierYourLife is personal and
> evolving: it tracks how your rankings change over time, and an AI helps you place an item and
> *explain why* it belongs there.

🚧 **Work in progress** — built from scratch, in the open, as a learning flagship. The commit history *is* the story.

---

## Why this project

A portfolio-grade Android app built deliberately, one well-understood commit at a time, to demonstrate
modern Android engineering end-to-end: clean architecture, a real test suite, CI, and a genuine AI feature —
not a tutorial clone.

## Tech stack

> Adopted as the roadmap progresses — items appear here once they actually land in the code.

- **Language:** Kotlin
- **UI:** Jetpack Compose · Material 3
- **Async:** Coroutines · Flow
- **Architecture:** Clean Architecture · multi-module · unidirectional data flow
- **Build:** Gradle (Kotlin DSL) · version catalog · convention plugins *(planned)*
- **DI:** Hilt *(planned)*
- **Data:** Room · DataStore *(planned)*
- **Network:** TMDB API for movie metadata *(planned)*
- **AI:** Claude API — tier suggestions & rationale *(planned)*
- **Quality:** JUnit · Turbine · fakes/MockK · Compose UI tests · detekt/ktlint *(planned)*
- **CI:** GitHub Actions *(planned)*

## Roadmap

- [x] **M0** — Project genesis: Compose scaffold, git, GitHub
- [ ] **M1** — Build foundation: version catalog, Kotlin DSL, JDK toolchain
- [ ] **M2** — Modularization + convention plugins
- [ ] **M3** — First feature, clean architecture, TDD (domain → data → presentation)
- [ ] **M4** — CI: tests + static analysis on every PR
- [ ] **M5** — Persistence with Room
- [ ] **M6** — AI feature: LLM-assisted tiering via Claude API + TMDB
- [ ] **M7** — UI tests, polish, portfolio-grade docs

## Building

```bash
git clone https://github.com/kellygracelab-dotcom/TierYourLife.git
```

Open in Android Studio and run the `app` configuration. Minimum SDK 24.

---

<sub>Built with Kotlin & Jetpack Compose · learning flagship, summer 2026</sub>
