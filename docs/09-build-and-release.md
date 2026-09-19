# 09 — Build and Release

## 1. Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Android SDK | compileSdk 36, targetSdk 36, minSdk 24 | `sdk.dir` in `android/local.properties` |
| JDK | 17+ | AGP 8.13.2 requires 17 |
| Gradle | 8.13 | **No wrapper in this repo** — see §2 |
| Kotlin | 2.2.20 | with the Compose compiler plugin |

---

## 2. Building — there is no `gradlew`

`android/` has **no Gradle wrapper**. `./gradlew` will fail. The working invocation is an
absolute path to a Gradle distribution that already exists on the build machine:

```powershell
& "C:\Users\jagad\AppData\Local\Temp\opencode\gradle\gradle-8.13\bin\gradle.bat" `
    -p C:\Users\jagad\Downloads\dev\teamPoshan\android `
    assembleDebug --console=plain
```

> **Gap worth fixing.** The missing wrapper makes builds non-reproducible across machines
> and will break CI immediately. Run `gradle wrapper --gradle-version 8.13` once inside
> `android/` and commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`. Every command in
> this document then becomes `./gradlew …`. `tools/save-apk.ps1` already prefers
> `android/gradlew.bat` and only falls back to the temp-directory Gradle, so it will pick
> the wrapper up automatically once it exists.

Output APK: `android/app/build/outputs/apk/debug/app-debug.apk` (~83 MB debug).

---

## 3. Build configuration

`android/app/build.gradle.kts`:

| Setting | Value |
|---|---|
| `namespace` / `applicationId` | `com.teamposhan.biomed` |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 24 |
| `versionCode` / `versionName` | 8 / `0.7.1` (bumped by `tools/save-apk.ps1`) |
| `testInstrumentationRunner` | declared, but **no `src/test` or `src/androidTest` exists** |
| debug `isMinifyEnabled` | `false` |

There is **no release signing configuration**. Every APK in `apks/` is a debug build,
signed with the local debug keystore. Install with `adb install -r <file>`. A release
track needs a keystore, a `signingConfigs` block, and R8/ProGuard rules before anything
ships to a store or to field devices.

### 3.1 Appwrite build constants

Four `buildConfigField`s are resolved at configure time, each via `prop(key, fallback)`,
which reads a Gradle property first, then `android/local.properties`, then a fallback:

| `BuildConfig` field | Gradle property | Fallback |
|---|---|---|
| `APPWRITE_ENDPOINT` | `poshan.appwrite.endpoint` | `https://cloud.appwrite.io/v1` |
| `APPWRITE_PROJECT_ID` | `poshan.appwrite.projectId` | `poshan-dev` |
| `APPWRITE_DATABASE_ID` | `poshan.appwrite.databaseId` | `poshan_db` |
| `APPWRITE_ENABLED` | `poshan.appwrite.enabled` | `false` |

Current values live in **`android/gradle.properties`** (endpoint
`https://<your-appwrite-host>/v1`, project `<your-project-id>`, enabled `true`).

Two notes:

* `AppwriteService.isConfigured` is `APPWRITE_ENABLED && APPWRITE_PROJECT_ID.isNotBlank()`.
  **If `poshan.appwrite.enabled` is absent the fallback is `false` and the whole sync layer
  is silently inert** — the app works fully offline and never reports an error. If you are
  debugging "sync does nothing", check the generated
  `app/build/generated/source/buildConfig/debug/.../BuildConfig.java` first.
* `.gitignore` excludes `android/local.properties` but **not** `android/gradle.properties`,
  so the endpoint and project id are committed. Neither is a secret — both ship inside the
  APK in `BuildConfig` regardless — but per-developer overrides belong in
  `local.properties`, which the `prop()` helper already supports.

**No API key is ever compiled into the app.** The Appwrite admin key is used only by the
server-side provisioner (`appwrite/schema.ps1`), which reads it from the environment. See
§7.

### 3.2 Dependencies

From `android/gradle/libs.versions.toml`:

| Library | Version | Purpose |
|---|---|---|
| AGP / Kotlin | 8.13.2 / 2.2.20 | build |
| Compose BOM-less UI | 1.11.0-beta02 | UI toolkit |
| Material 3 | 1.5.0-alpha18 | **alpha** — required for the Expressive APIs (emphasized type, wavy progress) |
| material-icons-extended | 1.7.8 | pinned separately from `compose` |
| navigation-compose | 2.8.9 | NavHost |
| lifecycle (runtime/compose/viewmodel) | 2.8.7 | `AppViewModel` |
| activity-compose | 1.10.1 | host activity |
| datastore-preferences | 1.1.4 | `PrefsStore` |
| kotlinx-serialization-json | 1.8.0 | on-disk JSON, Appwrite payloads |
| appwrite sdk-for-android | 25.2.0 | backend client |
| coil-compose | 2.7.0 | image loading |
| CameraX (core/camera2/lifecycle/view) | 1.4.1 | module 2 capture — **current stable is 1.6.2** |
| mlkit face-detection | 16.1.7 | face presence |
| mediapipe tasks-vision | 0.10.26 | 478-landmark face mesh + iris — **current is 1.0.0** |
| work-runtime-ktx | 2.10.0 | background sync |
| appcompat | 1.7.0 | `AppCompatDelegate` locale switching |

Two dependencies are on alpha/beta channels (Compose 1.11.0-beta02, Material3
1.5.0-alpha18). That is deliberate — M3 Expressive is not in a stable release — but it
means an upgrade can break the build in ways unrelated to our code. Pin exactly; never
use dynamic versions.

Upgrade notes for module 2 are in `docs/03-module2-attention-gaze.md` §3.8. Relevant here:
MediaPipe 1.0.0 moved its native libraries into `tasks-core`, whose AAR carries four ABIs
(~22.5 MB). If you upgrade, set `abiFilters` to `arm64-v8a` + `armeabi-v7a` or ship an App
Bundle, or the APK grows sharply.

---

## 4. Archiving a build — `tools/save-apk.ps1`

Every APK worth keeping goes in `apks/` with a name that makes the change set obvious:

```
poshan-v<versionName>-<slug>-<yyyyMMdd-HHmm>.apk
```

```powershell
# from the repo root — bump the version, build, and archive
pwsh -File tools/save-apk.ps1 -Version 0.8.0 -Slug "landscape-gaze" -Build

# archive whatever is already built, without touching the version
pwsh -File tools/save-apk.ps1 -Slug "hotfix" -NoBuild
```

| Parameter | Effect |
|---|---|
| `-Version` | Rewrites `versionName` in `app/build.gradle.kts` and increments `versionCode` |
| `-Slug` | Short kebab-case tag describing the change; becomes part of the filename |
| `-Build` | Runs `assembleDebug` before copying |
| `-NoBuild` | Skips Gradle; archives the existing `app-debug.apk` |

The script prefers `android/gradlew.bat` and falls back to the temp-directory Gradle 8.13.

After archiving, add a row to the table in `apks/README.md` describing what changed. That
file is the human-readable history and is currently maintained by hand.

### 4.1 Archive history

| Version | Slug | What changed |
|---|---|---|
| 0.1.0 | `scaffold` | Initial Kotlin/Compose app: onboarding, STEP assessment, growth z-scores, NIS/IAP immunisation, M3 Expressive theme, offline store |
| 0.2.0 | `module2-gaze-charts-sync` | Module 2 rebuilt on the literature; WHO growth charts drawn and animated; MUAC replaces head circumference; GPS + reverse geocoding; auto-sync |
| 0.3.0 | `module2-jointattention-qc` | Joint-attention trials, per-trial validity gating, session confidence, head-movement rate |
| 0.4.0 | `reve-art-icon` | Reve artwork import, adaptive launcher icon, `MilestoneArt` 18 → 45 mapped questions |
| 0.5.0 | `who-growth-charts-report` | Real WHO LMS tables; LMS maths corrected; WHO-sheet chart rendering; tap/long-press chart detail |
| 0.6.0 | `home-redesign-gps-a11y-charts` | Home rebuilt; GPS active-fix fix; enlarged accessibility type scale; growth charts hub; relationship dropdown |
| 0.7.0 | `sync-fix-catchup-bands-probe` | Appwrite permission fix; catch-up screening; band-based schedule; plotted-value-only probe; Children tab → app bar |
| 0.7.1 | `appwrite-muac-schema-fix` | `muac_mm` attribute added and client repointed; full client↔schema diff verified |

---

## 5. Emulator

An AVD named `poshan` exists (Pixel 6, `android-34` `google_apis` `x86_64`,
`swiftshader_indirect` GPU).

> **It is configured with `hw.ramSize = 1024M`, which is why it hangs.** A Compose app of
> this size needs 2048–3072 MB. Edit `~/.android/avd/poshan.avd/config.ini`, set
> `hw.ramSize = 3072`, and restart it.

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\emulator\emulator.exe" -avd poshan -memory 3072 -netdelay none -netspeed full
& "$sdk\platform-tools\adb.exe" wait-for-device
& "$sdk\platform-tools\adb.exe" install -r apks\poshan-v0.7.1-appwrite-muac-schema-fix-20260917-1523.apk
```

### 5.1 Memory — the recurring failure on this machine

The build host has ~15 GB total with ~2.5 GB typically free. A Gradle daemon, a Kotlin
compile daemon and an emulator together exhaust it, and the JVM dies with:

```
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (malloc) failed to allocate … bytes for Chunk::new
```

The `hs_err_pid*.log` and `replay_pid*.log` files in the repo root are crash dumps from
exactly this. They are build artefacts, not source — they should be deleted and added to
`.gitignore`.

**Rule: build or emulate, not both.** Close the emulator before a build; run
`gradle --stop` before starting the emulator.

---

## 6. Verifying a build actually recompiled

Gradle's up-to-date checks can make a failed edit look like a success. To confirm your
change is in the APK, check the compiled class timestamps rather than trusting the build
log:

```bash
ls -la android/app/build/tmp/kotlin-classes/debug/com/teamposhan/biomed/ui/screens/
ls -la android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. Server-side provisioning

The Appwrite schema is provisioned from the repo, never by hand in the console, so it is
reproducible:

```powershell
$env:APPWRITE_ENDPOINT   = "https://<your-appwrite-host>/v1"
$env:APPWRITE_PROJECT_ID = "<your-project-id>"
$env:APPWRITE_API_KEY    = "<admin key — never commit this>"
pwsh -File appwrite/schema.ps1
```

The key needs these scopes, or the run fails with
`missing scopes (["collections.write"])` and similar:

`databases.read`, `collections.read`, `collections.write`, `documents.read`,
`documents.write`

The script is idempotent: it creates missing collections and attributes and re-applies
collection permissions on every run. Details and the schema itself are in
`docs/07-data-and-sync.md`.

**Never write an API key into source, a doc, or a log.** Keys pasted into a shared context
should be rotated.

---

## 8. Release checklist (not yet satisfiable)

Before anything reaches a field device as a release build:

- [ ] Commit a Gradle wrapper (§2)
- [ ] Create a release keystore and `signingConfigs`; store credentials outside the repo
- [ ] Enable R8 with rules covering kotlinx-serialization, Appwrite and MediaPipe
- [ ] Register the release SHA-1/SHA-256 fingerprint as an Appwrite Android platform
- [ ] Set `abiFilters` or ship an App Bundle (MediaPipe native libs)
- [ ] Add at least smoke instrumentation tests — there are none today
- [ ] Delete the `hs_err_pid*` / `replay_pid*` crash dumps and the `reve_images_*.zip`
      archives from the repo, and gitignore them
- [ ] Confirm `poshan.appwrite.enabled=true` in the release configuration (§3.1)

---

## 9. Repository hygiene

The working tree has **no commits** — everything is untracked on branch `master`. Large
binaries currently sitting in the repo root:

| Path | Size | Should be |
|---|---|---|
| `reve_images_2026-09-16_17-00-12.zip` | 61 MB | out of the repo (asset source archive) |
| `reve_images_2026-09-17_01-45-45.zip` | 73 MB | out of the repo |
| `replay_pid*.log`, `hs_err_pid*.log` | ~3 MB | deleted, gitignored |
| `apks/*.apk` | 83 MB each | already gitignored via `*.apk` |

`.gitignore` covers build output and APKs but not the image archives or the crash dumps.
