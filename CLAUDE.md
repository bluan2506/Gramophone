# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Gramophone is an Android music player built on media3/ExoPlayer and Material 3, targeting Android's platform conventions strictly. Package/namespace: `com.musicdownloader.musicfreeapp825v2` (renamed to match `applicationId`; was `org.akanework.gramophone`). `minSdk = 26`, `targetSdk = 36`; app compiles against `compileSdk` release 37 (minor API 1), other modules against release 36. Code is Kotlin, compiled to JVM 21. SDK levels use the AGP 9.x `compileSdk { version = release(N) { minorApiLevel = 1 } }` DSL.

## Prerequisites & one-time setup

Building requires a **JDK 21+** and the latest **beta** Android Studio. Three local, machine-specific files are needed (all git-ignored — never commit them):

- `package.properties` (repo root) — must contain `releaseType=SelfBuilt`. The build reads `releaseType` from here at configuration time and fails without it. Signing credentials (`AKANE_RELEASE_STORE_FILE`, `AKANE_RELEASE_STORE_PASSWORD`, `AKANE_RELEASE_KEY_ALIAS`, `AKANE_RELEASE_KEY_PASSWORD`, and the `AKANE2_*` variants) may also be supplied here or as Gradle properties.
- `local.properties` (repo root) — `sdk.dir=<Android SDK path>`.
- `media3/local.properties` — **also required**, because `media3` is an included composite build with its own SDK resolution (see Architecture). A missing SDK path here surfaces as an "SDK location not found" error pointing at `media3/local.properties`.

Submodules must be present: `git submodule update --init --recursive` (pulls the `media3` fork and `hificore/.../libusb-cmake`).

## Common commands

Run Gradle with a JDK 21+ on `JAVA_HOME` (Android Studio's bundled JBR works):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :app:assembleDebug            # build the debug APK (auto-signed with the debug key; no keystore needed)
./gradlew :app:assembleRelease          # release build — REQUIRES the AKANE_RELEASE_* signing properties
./gradlew :app:installDebug             # build + install debug on a connected device/emulator

./gradlew :app:testDebugUnitTest        # run all JVM unit tests (JUnit + Robolectric)
./gradlew :app:testDebugUnitTest --tests "com.musicdownloader.musicfreeapp825v2.LrcUtilsTest"   # single test class

./gradlew :app:lintDebug                # Android Lint (config in app/lint.xml)
```

Debug/release variant behavior differs meaningfully — `BuildConfig.DEBUG` and `RELEASE_TYPE` gate developer-only UI and settings; the `userdebug` build type adds `kotlin-reflect` and extra source sets layered on top of `debug`.

## Architecture

**Playback is a background service, not in-Activity.** The player lives in `MusicDownloaderPlaybackService` (`logic/`), a media3 `MediaLibraryService` that owns the `ExoPlayer` and `MediaLibrarySession`. The UI never touches the player directly — it connects as a `MediaController`. `MediaControllerViewModel` (`ui/`) is the single bridge: it establishes the `SessionToken`/`MediaController` connection and exposes playback state to Activities, Fragments, and Compose. When changing playback behavior, decide whether logic belongs service-side (authoritative, survives UI teardown) or controller-side (UI reaction). `MusicDownloaderApplication` is the `Application` entry point.

**media3 is a local fork consumed via composite build.** `settings.gradle.kts` does `includeBuild("media3")` with `dependencySubstitution` mapping `androidx.media3:media3-*` artifacts to local `:lib-*` projects. So `androidx.media3.*` imports resolve to the checked-out `media3/` submodule, not a Maven release — media3 source is editable and rebuilt as part of the app. This is why `media3/` needs its own `local.properties`.

**Library/data layer — `uk.akane.libphonograph`.** Music library reading (querying `MediaStore`, grouping into albums/artists/genres/folders, sorting) is factored into the `libphonograph` package under `app/src/main/java/uk/`, exposed through `reader/` (`Reader`, `SimpleReader`, `FlowReader`, `ReaderResult`). Treat it as the media-catalog module distinct from app-specific UI logic in `com.musicdownloader.musicfreeapp825v2`.

**UI is hybrid Views + Compose.** Predominantly Fragment/View-based (`ui/fragments`, `ui/adapters`, `ui/components`) with Compose interop in `ui/fragments/compose` and `ui/components/compose`. Settings screens use AndroidX `preference` Fragments under `ui/fragments/settings`.

**Native & platform-integration modules:**
- `hificore/` — C++ (CMake, prefab) for bit-perfect/hi-res USB audio via USB Audio Class + libusb; bypasses the Android mixer for supported DACs. Namespace `org.nift4.gramophone.hificore`.
- `misc/alacdecoder` — lightweight Java ALAC decoder so ALAC plays even without a system ALAC codec.
- `misc/audiofxstub`, `audiofxstub2`, `audiofxfwd` — `AudioEffect`/system-Equalizer integration stubs compiled against otherwise-inaccessible platform APIs.
- `baselineprofile/` — Baseline Profile generation for startup/runtime performance.

**Notable logic areas** (`logic/utils`): `SemanticLyrics` (LRC/TTML/SRT + word/syllable karaoke sync — has the most unit-test coverage), `ReplayGainUtil` (ReplayGain 2.0), `LastPlayedManager` (playback restoration), and `exoplayer/` custom players including per-OEM tweaks under `exoplayer/oem`.

## Gotchas

- The build embeds the short git HEAD hash into the version name at configuration time (`git rev-parse --short=7 HEAD`), overridable via a `versionNameSuffixOverride` property.
- `gradle.properties` enables configuration cache, config-on-demand, and parallel builds, and suppresses JDK-mismatch warnings between Gramophone (JDK 21) and the media3 fork (JDK 8) — expect those suppressions to matter when touching either build.
- Codec support is deliberately delegated to the system (see README FAQ): FLAC/xHE-AAC/Dolby availability is OS/device-dependent; ALAC is the one format decoded in-app.
