<h1 align="center">TierYourLife</h1>

<p align="center">
  <b>Rank anything from S to D. Can't decide? Duel it out.</b><br>
  A tier list maker for anime, games, movies, music and food. Free, with no ads.
</p>

<p align="center">
  <a href="https://tieryourlife.com"><b>Open tieryourlife.com</b></a>
  &nbsp;·&nbsp;
  Android: coming to Google Play, in closed testing now
</p>

<p align="center">
  <a href="https://tieryourlife.com"><img src="docs/screenshots/play-gallery.jpg" alt="Six screens of TierYourLife: a tier list of dog breeds, a duel between two ice creams, picture search by name, adding your own photos, a finished ranking, and lists by topic"></a>
</p>

## What you can do

- **Make a tier list in a minute.** Drag cards into S, A, B, C and D. Add or remove tiers, or switch to a plain top 10.
- **Type a name, get the picture.** Films, shows, anime, games, books, people, plants and animals. Or use your own photo or a link.
- **Duel it out.** Any list becomes a "this or that" game: tap the card you like more and the tiers fill in by themselves. There are four ways to play.
- **See how others ranked it.** Open someone's list, rank the same cards your way, and compare the versions.
- **Browse by topic.** Anime, film and TV, games, music, books, food, sport, people and more.
- **Share it** as a picture or a link.
- **Keep it everywhere.** Sign in with Google and your lists follow you to a new phone and to the website. You can start without an account.

Light and dark themes, eleven languages.

## The app

<p align="center">
  <img src="docs/screenshots/board-dark.png" width="24%" alt="A finished board with five tiers">
  <img src="docs/screenshots/duel-dark.png" width="24%" alt="A duel between two cards">
  <img src="docs/screenshots/section-dark.png" width="24%" alt="The Food section with its topics">
  <img src="docs/screenshots/community-dark.png" width="24%" alt="The community's first screen">
</p>

<p align="center"><sub>A board · a duel · a section with its topics · the community</sub></p>

<p align="center">
  <img src="docs/screenshots/rankings-light.png" width="24%" alt="Choosing whose ranking of a list to show">
  <img src="docs/screenshots/pool-light.png" width="24%" alt="A board with cards still waiting below it">
  <img src="docs/screenshots/lists-light.png" width="24%" alt="Your lists">
  <img src="docs/screenshots/settings-light.png" width="24%" alt="Settings">
</p>

<p align="center"><sub>Everyone's rankings of one list · cards still to place · your lists · settings</sub></p>

## Three repositories

| | Repository | Where it runs |
|---|---|---|
| **Android app** | this one: Kotlin, Jetpack Compose | Google Play, in closed testing |
| **Website** | [tieryourlife-web](https://github.com/kellygracelab-dotcom/tieryourlife-web): React, TypeScript | [tieryourlife.com](https://tieryourlife.com) |
| **Server** | [tieryourlife-proxy](https://github.com/kellygracelab-dotcom/tieryourlife-proxy): Cloud Functions in front of Firestore | behind both |

## For developers

[![CI](https://github.com/kellygracelab-dotcom/TierYourLife/actions/workflows/ci.yml/badge.svg)](https://github.com/kellygracelab-dotcom/TierYourLife/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)

- [How it is built](docs/engineering.md): the modules, five files worth reading, and the tests.
- [Building it yourself](docs/building.md).
- [Design specs](docs): why each screen looks the way it does.
- [Release to Google Play](docs/play-internal-delivery.md).
- [Where these pictures come from](docs/screenshots.md).

AI agents wrote a large share of the code against my specifications. The specifications,
architecture decisions, code review and testing on a real phone are mine.

---

<sub>Photo credits for the pictures above: <a href="https://tieryourlife.com/store-credits">tieryourlife.com/store-credits</a>. This product uses the TMDB API but is not endorsed or certified by TMDB.</sub>
