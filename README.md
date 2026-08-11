# GoodsBuy (谷的拜)

An Android app for managing your anime merchandise collection — track purchases, sales, and real-time profit/loss in one place.

## Features (MVP)

- **Collection Management** — Record, edit, and categorize your goods; manage order status (owned / sold / pending); attach photos
- **Profit/Loss Stats** — Auto-calculated P&L across total, monthly, and category dimensions; visual charts
- **Dashboard** — At-a-glance overview of investment, revenue, holdings, and key metrics

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

- minSdk: 29 (Android 10)
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

MIT
