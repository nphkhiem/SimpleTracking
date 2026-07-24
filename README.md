# Chạy Ngay Đi (SimpleTracking)

A GPS workout tracker for Android. Start a session, watch your route drawn live on a map, then
pause, resume, or stop and see it saved to your history.

## Features

- **History** — a list of past sessions with distance, duration, average speed, and a thumbnail of
  the recorded route.
- **Record** — a live tracking screen showing the route on a map as it's recorded, with
  pause/resume/stop controls and a running distance/speed/duration readout.
- Tracking keeps running in the background via a foreground service, so a session survives the app
  being backgrounded.

## Tech stack

- Kotlin, Coroutines and Flow throughout
- Clean Architecture across three modules: `domain`, `data`, `app`
- Hilt for dependency injection
- Room for local persistence
- AndroidX Jetpack Navigation (with Safe Args)
- Google Maps SDK for the live route map
- Built test-first (strict TDD) — 165 unit tests plus instrumented tests covering the recording and
  recovery flows against real Room persistence

## Module structure

- `domain` — use cases (interactors), models, and repository interfaces. No Android or framework
  dependencies.
- `data` — repository implementations, Room database, location provider, and route thumbnail
  generation.
- `app` — UI (Fragments, ViewModels), the tracking foreground service, and Hilt wiring.

## Building and testing

The app needs a Google Maps API key. Add it to a `local.properties` file at the project root
(not committed to version control):

```
MAPS_API_KEY=your_api_key_here
```

Then:

```
./gradlew build
./gradlew test
./gradlew connectedAndroidTest
```

`connectedAndroidTest` requires a connected device or running emulator.

## Author

khiemnph — nphkhiem@gmail.com
