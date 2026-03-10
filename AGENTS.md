# M3UAndroid Agent Guide

## Purpose
- This file is the repo-local guide I should follow in future threads.
- Treat it as the first source of truth for architecture, module boundaries, and safe editing rules in this workspace.
- Use repo-relative paths in this document so it stays valid on any machine.

## Project Shape
- This is a modular Android app built with Kotlin, Jetpack Compose, Hilt, Room, WorkManager, Media3/ExoPlayer, and Paging.
- The currently active app target is `:app:smartphone`.
- TV code still exists under `app/tv` and `baselineprofile/tv`, but those modules are commented out in `settings.gradle.kts`.
- The enabled modules are:
  - app: `:app:smartphone`, `:app:extension`
  - shared/core: `:core`, `:core:foundation`, `:core:extension`, `:i18n`
  - data: `:data`
  - business features: `:business:foryou`, `:business:favorite`, `:business:setting`, `:business:playlist`, `:business:playlist-configuration`, `:business:channel`, `:business:extension`
  - tooling: `:baselineprofile:smartphone`, `:lint:annotation`, `:lint:processor`

## How The App Works
- Android startup begins in `app/smartphone/src/main/java/com/m3u/smartphone/M3UApplication.kt`:
  - Hilt app setup
  - WorkManager worker factory wiring
  - Timber in debug
  - ACRA crash reporting
- The main activity is `app/smartphone/src/main/java/com/m3u/smartphone/MainActivity.kt`:
  - installs splash screen
  - enables edge-to-edge
  - applies configuration through `Helper`
  - renders the Compose app
- The top-level Compose shell is `app/smartphone/src/main/java/com/m3u/smartphone/ui/App.kt`:
  - creates the root `NavController`
  - hosts the adaptive navigation scaffold
  - owns the global search UI
  - routes into feature graphs
- Root navigation lives in `app/smartphone/src/main/java/com/m3u/smartphone/ui/common/AppNavHost.kt` and `app/smartphone/src/main/java/com/m3u/smartphone/ui/common/RootGraph.kt`.
- Playback is launched into `PlayerActivity`, while actual playback state is centralized in `PlayerManager`.

## How The Smartphone UI Works
- Theme and composition locals are set up in `app/smartphone/src/main/java/com/m3u/smartphone/ui/common/internal/Toolkit.kt`.
- `Toolkit` is the outer UI wrapper. It:
  - reads theme-related preferences such as dark mode, dynamic colors, compact spacing, and chosen color
  - injects `LocalHelper`, haptics, and spacing locals
  - applies the app `Theme`
  - updates system bar styling through `Helper`
- `App.kt` is the main shell for the smartphone UI. It is responsible for:
  - creating the `NavHostController`
  - rendering the `NavigationSuiteScaffold`
  - mapping bottom/navigation destinations from `Destination`
  - showing a top search bar and a fullscreen search results surface
  - rendering the shared snackbar host area
- Destinations are defined in `app/smartphone/src/main/java/com/m3u/smartphone/ui/material/components/Destination.kt`.
  - Current root tabs are `Foryou`, `Favorite`, `Extension`, and `Setting`.
  - `Destination.of(route)` is used to map the back stack route to the selected nav item.
- `AppNavHost.kt` and `RootGraph.kt` split root-tab navigation from deeper flows.
  - Root tabs render feature routes directly.
  - Playlist and playlist-configuration routes are added outside the root graph as deeper destinations.
  - Playback is not a Compose destination; it launches `PlayerActivity`.
- Most screen-level chrome is driven through `Metadata` and helper abstractions instead of every screen owning its own app bar.
  - Feature routes usually set `Metadata.title`, colors, and actions inside `LifecycleResumeEffect`.
  - Example: `ForyouRoute` sets the screen title and adds the add-playlist action.
- Search is global rather than owned by one feature screen.
  - `AppViewModel` exposes the search result paging flow.
  - `App.kt` renders the search UI once and overlays the results through `ExpandedFullScreenSearchBar`.
- Feature route pattern:
  - route composable lives in `app/smartphone/ui/business/...`
  - state comes from a `business/*ViewModel`
  - route wires callbacks and lifecycle metadata
  - a lower-level screen composable renders the actual layout
- Example screen structure:
  - `ForyouRoute` wires playlists, recommendations, episodes sheet state, and metadata
  - `ForyouScreen` renders the background, recommend header, playlist gallery, and media sheet
  - `PlaylistRoute` wires playlist state, query, sort, permissions, refresh, and channel actions
  - `PlaylistScreen` handles the actual tabbed/paged content and overlays
- Bottom sheets and transient UI are a recurring pattern.
  - Media actions use `MediaSheet`
  - series episode picking uses `EpisodesBottomSheet`
  - sorting uses `SortBottomSheet`
- Preference-driven UI is common.
  - row count, god mode, compact density, theme mode, dynamic colors, and zapping mode all influence layout or behavior
  - before changing UI behavior, search `PreferencesKeys` usage across modules
- `Helper`, `Metadata`, `Action`, and related classes in `app/smartphone/ui/common/helper` are part of the smartphone UI infrastructure.
  - Treat them as shared UI shell state, not feature-local state.

## Architecture Rules
- UI modules should stay thin. Feature screens read state from feature `ViewModel`s in `business/*`.
- Business modules orchestrate flows, paging, settings, workers, and user actions. They should not bypass repositories unless the code already does so and the change is intentionally scoped.
- The data layer owns:
  - Room entities and DAOs
  - repository implementations
  - playlist parsing
  - EPG/Xtream loading
  - player/service logic
  - WorkManager jobs
- Hilt bindings for repositories live in `data/src/main/java/com/m3u/data/repository/RepositoryModule.kt`.
- Room database setup lives in `data/src/main/java/com/m3u/data/database/M3UDatabase.kt` and `data/src/main/java/com/m3u/data/database/DatabaseModule.kt`.

## Important Runtime Flows
- Playlist subscription/import:
  - Settings UI gathers input in `SettingViewModel`.
  - `SubscriptionWorker` is enqueued for M3U, Xtream, or EPG refresh paths.
  - `PlaylistRepositoryImpl` parses and persists playlists/channels.
- Search:
  - global search state starts in `AppViewModel`
  - results are paged from `ChannelRepository.search`
- Playlist browsing:
  - `PlaylistViewModel` combines playlist, categories, sort, paging, programme lookup, and zapping state.
- Playback:
  - `PlayerManagerImpl` is the playback source of truth.
  - It maps a `MediaCommand` to current playlist/channel, creates the Media3 player, manages tracks, cache, continue-watching, and playback error/state flows.
- Recommendations/foryou:
  - `ForyouViewModel` combines playlists, recent playback, favorites, unseen logic, and worker status.

## UI Editing Rules
- Keep route composables responsible for wiring, not heavy business logic.
- Put reusable visual components under the existing `ui/material` or feature `components` folders instead of growing route files indefinitely.
- Reuse existing shared primitives before introducing new UI infrastructure:
  - `Metadata`
  - `SnackHost`
  - `MediaSheet`
  - `EpisodesBottomSheet`
  - helper and composition-local utilities
- When changing a screen, check whether the same behavior exists in:
  - the route composable
  - the feature screen composable
  - a shared component under `ui/material/components`
- Preserve the current shell behavior:
  - global search stays at app-shell level
  - root destination navigation stays in `App.kt` and `AppNavHost.kt`
  - playback launch stays activity-based unless the task explicitly changes that architecture
- Be careful with preference-backed UI state. If a layout toggle should persist, there is a good chance it already belongs in preferences rather than local `remember` state.

## Repo-Specific Working Guidelines
- Prefer fixing smartphone code paths first. Do not assume TV modules are part of the active build unless the task explicitly targets them.
- Before changing a feature, trace all three layers when relevant:
  - screen/composable in `app/smartphone`
  - viewmodel/use-case logic in `business/*`
  - repository/DAO/service/worker code in `data`
- Preserve existing architectural seams:
  - UI -> business ViewModel
  - ViewModel -> repository/service/settings/workers
  - repository -> DAO/parser/network
- Prefer extending repositories or services over adding database or network logic directly inside `ViewModel`s.
- When editing database models or DAOs:
  - update schema/migration implications carefully
  - check `data/schemas`
  - note that the database currently uses `fallbackToDestructiveMigration()`, so destructive compatibility mistakes are easy to hide
- When editing playlist import/refresh:
  - check M3U and Xtream paths separately
  - preserve `PlaylistStrategy` behavior for favorites/hidden channels
  - verify worker tags and progress notifications if work status is used by UI
- When editing playback:
  - inspect `PlayerManager`, `PlayerManagerImpl`, and the calling `ViewModel`
  - avoid duplicating playback state outside `PlayerManager`
  - preserve continue-watching, track selection, and zapping behavior
- When editing settings/preferences:
  - look for `PreferencesKeys` usage across app, business, and data layers
  - many UI behaviors are preference-driven
- When editing strings or user-facing features:
  - base strings live in `i18n/src/main/res/values`
  - translations exist, but unless requested I should update the default locale and flag translation follow-up rather than inventing translations

## Known Project Characteristics
- Compose compiler source info and metrics are enabled from the root Gradle config.
- Kotlin/JVM target is 17 across modules.
- The project uses KSP for Room, Hilt, custom lint/processor work, and extension processing.
- There is generated baseline profile support for smartphone.
- `app/smartphone/build.gradle.kts` currently sets `targetSdk = 33` while the project compiles with SDK 36.
- There are two search-oriented viewmodels in the smartphone layer:
  - `ui/AppViewModel`, which is used by the active app shell
  - `ui/common/SmartphoneViewModel`, which appears older or less central
  - Prefer tracing real call sites before reusing or extending both.

## How I Should Approach New Tasks Here
- Start by identifying which module owns the behavior before editing.
- Use `rg` to trace a feature name across:
  - `app/smartphone`
  - `business/*`
  - `data/*`
- For bugs, inspect the state owner first:
  - navigation bug -> app/navigation layer
  - UI state bug -> feature ViewModel
  - data inconsistency -> repository/DAO/parser
  - playback bug -> `PlayerManagerImpl` and `business/channel`
- For new features, prefer adding logic in existing feature modules rather than creating new cross-cutting abstractions unless duplication is already a problem.
- For reviews, prioritize:
  - broken flow wiring across modules
  - stale worker/tag/state assumptions
  - Room schema or migration breakage
  - player lifecycle/regression risk
  - preference-driven behavior changes

## Validation Defaults
- After meaningful changes, prefer targeted Gradle validation over guessing.
- Good defaults:
  - `./gradlew :app:smartphone:assembleDebug`
  - `./gradlew :app:smartphone:compileDebugKotlin`
  - `./gradlew :data:compileDebugKotlin`
  - `./gradlew :business:<module>:compileDebugKotlin`
- If database entities, DAOs, or migrations change, pay extra attention to Room/KSP output and schema files.

## Current Caveats
- There are existing `FIXME` markers in the codebase, for example direct DAO usage from `SettingViewModel`; do not expand those shortcuts unless explicitly asked.
- TV code may be stale relative to smartphone because it is present but not in the active Gradle include list.
