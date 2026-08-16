# GoodsBuy (谷的拜)

An Android app for managing your anime merchandise collection — track purchases, sales, and real-time profit/loss in one place.

Current version: **v1.4.0**

## Features (MVP)

- **Collection Management** — Record, edit, and categorize your goods; manage order status (owned / sold / pending); attach photos
- **Profit/Loss Stats** — Auto-calculated P&L across total, monthly, and category dimensions; visual charts
- **Dashboard** — At-a-glance overview of investment, revenue, holdings, and key metrics
- **Gallery View** — Grouped by IP/series with search, collapse/expand, focused group views, horizontal browsing, and an optional home-screen switch
- **Backup & Restore** — One-tap ZIP backup (including images), import/restore with preview, and dedup/overwrite/add strategies
- **Batch Operations** — Long-press to enter batch mode for status changes / deletion
- **Drafts & Auto-save** — Automatically save unfinished forms (including images), restore them from the draft box, and choose a 0.5/1/2 second interval
- **Safety & Recovery** — Inline form validation, undo after deletion, stricter backup validation, and transactional imports

## Screenshots

<!-- Place screenshots in docs/screenshots/ directory and uncomment the lines below -->

### Collection Grid
<!-- ![Collection Grid](docs/screenshots/collection_grid.png) -->

### Collectible Detail
<!-- ![Collectible Detail](docs/screenshots/collectible_detail.png) -->

### Add/Edit Collectible
<!-- ![Add Collectible](docs/screenshots/add_collectible.png) -->

### Profit/Loss Statistics
<!-- ![Statistics](docs/screenshots/statistics.png) -->

### Dashboard
<!-- ![Dashboard](docs/screenshots/dashboard.png) -->

### Settings
<!-- ![Settings](docs/screenshots/settings.png) -->

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room 2.6.x |
| DI | Hilt |
| Navigation | Compose Navigation |
| Images | Coil |
| Charts | Vico |
| Async | Coroutines + Flow |

## Requirements

- minSdk: 28 (Android 9)
- JDK 17
- Android SDK 34

## Build

```bash
./gradlew assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

MVVM + Clean Architecture:

```
Presentation (Compose Screens + ViewModels)
      ↓
   Domain (Use Cases + Repository Interfaces)
      ↓
    Data (Room DB + Local File Storage)
```

## License

[Changelog](CHANGELOG.md)

[中文说明](README.md)

MIT
