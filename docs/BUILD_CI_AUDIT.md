# Build & CI Audit

Audit of the Gradle build, packaging, signing and automation of this repository,
written **before** any change was made. Baseline commit: `f9e9f0a` on `master`.

Scope: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `gradle.properties`,
`gradle/wrapper/gradle-wrapper.properties`, `app/proguard-rules.pro`,
`.gitignore`, `app/.gitignore`, `app/src/main/AndroidManifest.xml`,
`.github/**`, `docs/CORE_ARCHITECTURE.md`, `metadata/**`, both READMEs, and the
unmerged branches. Application logic was read but not touched.

## Method, and what could not be run

This audit was produced by reading the repository. **No Gradle task was
executed**: the environment available for this work has no Android SDK and no
network access to `dl.google.com` / Maven Central, so `assembleDebug`, `test`,
`lint`, `assembleRelease` and `apksigner` could not be run. Every claim below is
static analysis of the sources, and the pull request that carries this file
repeats that caveat under `## Not verified`. Where a change carries runtime risk
that only a real build can settle, it is called out inline.

## Severity legend

| | Meaning |
|---|---|
| **S1** | Ships broken or unpublishable artifacts, or loses data / security |
| **S2** | Defects reach users or maintainers because nothing checks for them |
| **S3** | Hygiene, speed, maintainability |

## Findings

| # | Severity | Finding | Verdict | Fix |
|---|---|---|---|---|
| 1 | S1 | Releases are published debug-signed | Confirmed | Real key from secrets in CI; hard failure for release tasks when `CI` is set and no key is present |
| 2 | S1 | R8 enabled with a template keep file | Confirmed | Real `proguard-rules.pro` for the whole stack |
| 3 | S2 | Release crash reports unreadable | Confirmed | `SourceFile`/`LineNumberTable` kept, `mapping.txt` uploaded |
| 4 | S2 | Room schemas exported but not committed | Confirmed, with a correction | Unconditional assets wiring + `verifyRoomSchemas` gate; **there are no migration tests to degrade** |
| 5 | S3 | Build output committed to git | Confirmed | `app/release/` deleted, `.gitignore` widened |
| 6 | S1 | No continuous integration at all | Confirmed | `ci.yml` on every PR and `master` push |
| 7 | S2 | No Gradle wrapper validation | Confirmed | `gradle/actions/wrapper-validation` in every workflow |
| 8 | S3 | Weak caching (`setup-java` cache) | Confirmed | `gradle/actions/setup-gradle` |
| 9 | S2 | Third-party actions on mutable tags | Confirmed | All non-GitHub actions SHA-pinned |
| 10 | S2 | `contents: write` workflow-wide | Confirmed | `contents: read` default, `write` on the release job only |
| 11 | S3 | No `concurrency` group | Confirmed | Cancel-in-progress for CI, queue (never cancel) for release |
| 12 | S1 | Release step unguarded | Confirmed, and worse than described | `fail_on_unmatched_files`, checksums, notes, tag guard |
| 13 | S2 | No AAB, no ABI/density splits | Partly rejected | AAB added; **ABI splits deliberately not added** (no native code) |
| 14 | S1 | Tag and app version never reconciled | Confirmed | Release job fails when the tag does not match `versionName` |
| 15 | S3 | No dependency automation | Confirmed | `dependabot.yml` for `gradle` + `github-actions` |
| 16 | S3 | No repository hygiene automation | Confirmed, with a correction | PR template, CODEOWNERS; **the listing is Play/Myket-style, not F-Droid** |
| 17 | S3 | `gradle.properties` leaves performance on the table | Mostly confirmed | Parallel, build cache, configuration cache, bigger heap. One flag rejected |
| 18 | S3 | Deprecated `kotlinOptions` | Confirmed | `kotlin { compilerOptions { } }` + `jvmToolchain(17)` |
| 19 | S3 | Version/signing logic inline in the module script | Confirmed | Kept inline, with `providers` APIs. `buildSrc` rejected, see below |
| 20 | S3 | No `debug` build type customisation | Confirmed | `applicationIdSuffix` + `versionNameSuffix` |
| 21 | S2 | Unfiltered `jitpack.io` and `maven.myket.ir` | Confirmed | `exclusiveContent` for JitPack, group filter for Myket |
| 22 | S3 | No lint config, no lockfile, no verification | Confirmed | `lint { }` added; dependency verification rejected, see below |
| 23 | S3 | `maven.myket.ir` serves no dependency | New | Constrained and flagged for removal |
| 24 | S3 | `pluginManagement` also declares JitPack and Myket | New | Removed; no plugin resolves from either |
| 25 | S2 | Both READMEs claim MIT, there is no `LICENSE` file | New | Reported, not fixed (owner's call) |
| 26 | S3 | `.idea/` is partly committed | New | `.gitignore` widened to the whole directory |
| 27 | S2 | Wrapper has no `distributionSha256Sum` | New | Wrapper validation in CI; checksum pinning flagged as follow-up |
| 28 | S3 | Dead entries in the version catalog | New | Reported, not removed (no version churn in this PR) |
| 29 | S2 | READMEs point at two different repositories for downloads | New | Reported, not fixed: needs the owner to say which is canonical |
| 30 | S2 | `workflow_dispatch` on a branch would create a bogus release | New | Release job refuses to run without a `v*` tag |
| 31 | S3 | `enableV1Signing = true` is pointless at `minSdk 24` | New | v1 disabled, v2/v3/v4 kept |
| 32 | S3 | Room schema location passed as an absolute KSP arg | New | Reported; Room Gradle plugin flagged as follow-up |
| 33 | S3 | Template test leftovers (`ExampleUnitTest`, `ExampleInstrumentedTest`) | New | Reported, not deleted (test code, not build config) |

## Blocking correctness defects

### 1. Releases are published debug-signed — S1, confirmed exactly

`app/build.gradle.kts` resolves signing material from `keystore.properties` or
from `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`, and
when nothing is present it falls back to `signingConfigs.getByName("debug")`
behind a single `logger.warn`. `.github/workflows/build-release.yml` provides
none of those values and never writes a keystore, so **every APK ever attached
to a GitHub Release was signed with the debug key** that ships in every
developer's `~/.android/debug.keystore`.

Consequences, in order of severity: anyone can build and sign an "update" that
Android accepts as the same app; the artifact cannot go to Play or Myket;
and the first properly signed release will be rejected as an upgrade
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), so existing users must uninstall first.

Two details the original report missed: the fallback applies to `bundleRelease`
too, and the warning is emitted at configuration time, so it scrolls past long
before the failure it predicts.

**Fix.** Real key from repository secrets in CI (base64 keystore decoded into
`RUNNER_TEMP`, deleted in an `always()` step). Locally the debug fallback stays,
but it now prints a multi-line banner, and any release/publish task fails hard
when `CI` is set. A partially configured keystore (some values present, others
missing) now fails immediately instead of silently falling back — that is the
failure mode most likely to bite a maintainer who typos one secret name.

### 2. R8 with an empty keep file — S1, confirmed

`isMinifyEnabled = true` on `release`; `app/proguard-rules.pro` is the unmodified
Android Studio template (comments only). The stack that needs rules:
`kotlinx.serialization` (generated `$$serializer` objects, `@Serializable`
metadata), Retrofit 2.11 (generic return types, annotations, `Continuation`
signatures, the `retrofit2.Response` type parameter), Room (generated `_Impl`
classes), Hilt/Dagger (generated components, `@AndroidEntryPoint` classes),
Glance (`GlanceAppWidgetReceiver` subclasses referenced only from the manifest,
plus Glance's own reflective session code) and `PersianDate`.

AGP's `proguard-android-optimize.txt` plus the AAR-embedded `consumer-rules.pro`
of each library cover a lot of this in practice — which is why the current
releases install at all — but nothing covers the app's own `@Serializable`
models, and no test ever exercised a minified build.

**Fix.** Real rules, written per library with a comment saying why each rule
exists. Resource shrinking was **not** enabled at the same time (see rejected
alternatives) — one unverifiable change to the release pipeline at a time.

### 3. Unreadable release crash reports — S2, confirmed

`-keepattributes SourceFile,LineNumberTable` and `-renamesourcefileattribute`
are commented out, and `mapping.txt` is discarded with the runner. Any stack
trace from a shipped build is line-number-free and unmappable, permanently.

**Fix.** Both attributes restored; `release.yml` uploads
`app/build/outputs/mapping/release/` (mapping, seeds, usage, configuration) as a
90-day build artifact. Deliberately **not** attached to the GitHub Release:
`mapping.txt` is not a user-facing asset and publishing it is an unnecessary
information leak.

### 4. Room schemas exported but not committed — S2, confirmed, with a correction

`ksp { arg("room.schemaLocation", "$projectDir/schemas") }` and
`@Database(..., version = 4, exportSchema = true)` are both in place, and
`app/schemas/` is absent from version control. The `androidTest` source set adds
it as an asset directory only `if (schemaDir.exists())`.

**Correction to the original report:** there are no migration tests to degrade.
`app/src/androidTest` contains exactly one file, `ExampleInstrumentedTest.kt`
(the Studio template). `room-testing`, `work-testing` and
`compose-ui-test-junit4` are declared in `app/build.gradle.kts` and used by
nothing. So the risk is not "tests silently skip", it is "there is no test at
all, and there is also no committed schema to write one against" — the four
migrations in `DataBase.kt` (1→2, 2→3, 3→4) have never been verified by
anything.

**Fix.** `app/schemas/` is created with a README, the assets wiring is
unconditional, and a `verifyRoomSchemas` task fails when the directory contains
no `*.json`. That task is wired into `connectedCheck` (the instrumented
workflow), **not** into `check`, for one reason: this change cannot generate a
valid schema file. Room's exported JSON contains an `identityHash` computed by
the annotation processor; hand-writing it would produce a file that makes future
migration tests fail for the wrong reason. A maintainer must run
`./gradlew :app:kspDebugKotlin` once and commit `app/schemas/4.json`. Until then
the instrumented workflow is the thing that says so, loudly, instead of the PR
pipeline being red on arrival.

### 5. Build output committed to git — S3, confirmed

`app/release/output-metadata.json` and two `app/release/baselineProfiles/{0,1}/app-release.dm`
files (~14 KB) are tracked. `.gitignore` ignores `/build` (root only) and
`app/.gitignore` ignores `/build`, so `app/release/` — which AGP writes when a
release build runs with a non-default output directory — slipped through. No
`baselineprofile` plugin or `androidx.profileinstaller` dependency exists, so
those `.dm` files are dead weight that documents a build that no longer happens.

**Fix.** Deleted, and `.gitignore` now covers `**/build/`, `app/release/`,
`app/schemas/*.db`, `*.apk`, `*.aab`, `*.dm`, `local.properties` and the whole
`.idea/` directory.

## Pipeline gaps

### 6. No continuous integration — S1, confirmed

One workflow, triggered by `push: tags: ['v*']` and `workflow_dispatch`. Nothing
compiles, tests or lints a pull request. The repository has seven branches and
two merged pull requests, one of which (`fix: release build fails on private
AndroidViewModel.application access`, `4ddb78f`) fixed a **release-only Kotlin
compilation failure** that a PR pipeline would have caught before the tag was
pushed. That is the cost of this finding, already paid once.

**Fix.** `ci.yml`: wrapper validation → `setup-gradle` → `assembleDebug` →
`test` → `lint`, reports uploaded, summary written to the job summary, failing
the PR on any failure.

### 7-11. Supply chain and workflow hygiene — confirmed

No wrapper validation; `cache: 'gradle'` on `setup-java` (no
configuration-cache-aware caching, no job summary, no dependency graph);
`actions/checkout@v4`, `actions/setup-java@v4` and `softprops/action-gh-release@v2`
all floating on mutable tags; `contents: write` granted to the whole workflow;
no `concurrency` group.

**Fix.** Wrapper validation everywhere; `gradle/actions/setup-gradle` (which also
validates wrappers, kept as a separate explicit step anyway so the failure is
named); every non-GitHub action pinned to a full commit SHA with a version
comment; `permissions: contents: read` at the top of every workflow with
`contents: write` on the one release job; `concurrency` with
`cancel-in-progress: true` for CI and `false` for release, so two tags never race
for the same release but a superseded PR run dies immediately.

GitHub-owned actions (`actions/*`) are deliberately left on major tags: they are
the trust anchor of the runner itself, and pinning them buys nothing while
costing a Dependabot PR every fortnight. This is the one place where the
blanket "pin everything" rule is not applied, on purpose.

### 12. The release step is unguarded — S1, confirmed and worse

`files: app/build/outputs/apk/release/*.apk` with no `fail_on_unmatched_files`,
no name, no body, no notes, no checksums, no draft handling. If the build
produces nothing the action creates an empty release and the run is green.

**New, related (finding 30):** `workflow_dispatch` is a trigger, and
`action-gh-release` defaults `tag_name` to `github.ref_name`. Dispatching the
workflow from `master` therefore creates a release **tagged `master`**. Nobody
noticed because nobody dispatched it.

**Fix.** The release job refuses to run unless the ref is a `v*` tag; the tag is
reconciled against `versionName` before anything is built; `fail_on_unmatched_files: true`;
SHA-256 sums generated and attached; `generate_release_notes: true` with a body
that carries the checksums and the install caveat; `prerelease` inferred from the
tag (`-rc`, `-beta`, `-alpha`).

### 13. No AAB and no splits — S2, half rejected

AAB: confirmed and fixed, `bundleRelease` runs in the release job and the `.aab`
is attached.

ABI splits: **rejected.** Nothing in the dependency graph ships JNI libraries —
Compose, Room (platform SQLite), OkHttp, Retrofit, kotlinx-serialization,
WorkManager, DataStore, Glance and PersianDate are pure JVM, and
`security-crypto`/Tink is Java-only. Per-ABI splits would produce three or four
byte-identical APKs, four release assets to choose wrongly from, and a
`versionCode` offset scheme to maintain, for zero bytes saved. Density and
language splits are what the AAB gives for free on Play. If native code ever
lands, revisit. (Caveat: verified by reading the dependency list, not by
unzipping a built APK.)

### 14. Tag and version never reconciled — S1, confirmed

`versionCode`/`versionName` come from two hardcoded `val`s inside
`android { }`; the tag is whatever was pushed. `v9.9` would ship `3.73`.

**Fix.** The version now lives in `gradle.properties` as `bbarq.versionMajor` /
`bbarq.versionPatch`, read through `providers.gradleProperty(...)`, and
`:app:printVersionName` prints it. The release job compares that value with
`${GITHUB_REF_NAME#v}` and fails the run before building anything. The
`versionCode` scheme (`major * 10000 + patch` → `30073`) and the `applicationId`,
`namespace` and `minSdk` are unchanged.

### 15-16. No dependency or repository automation — S3, confirmed with a correction

**Fix.** `dependabot.yml` for `gradle` (weekly, grouped: androidx, compose, hilt+ksp,
room, network, everything else patch-only) and `github-actions` (weekly, one
group). A PR template that asks for the verification a build change needs, and a
`CODEOWNERS` pointing at the repository owner.

**Correction:** `metadata/en_US/` is not an F-Droid listing. F-Droid (and
fastlane) use `en-US` with a hyphen, under `fastlane/metadata/android/`;
`metadata/<locale>/short_description.txt` + `full_description.txt` is the
Play/Myket shape — consistent with `maven.myket.ir` being in the repository list.
Both description files are also **empty (0 bytes)**, and there is no
`changelogs/` directory, so no store listing is being generated from them today.
A reproducibility check for F-Droid was therefore not added: it would be
automation for a distribution channel this repository is not on. `dependenciesInfo`
is disabled in the build anyway, which is the one build-level change that helps
both F-Droid and reproducible builds, and costs nothing.

## Build configuration smells

### 17. `gradle.properties` — confirmed, one item rejected

Before: `-Xmx2048m`, `parallel` commented out, no build cache, no configuration
cache, no Kotlin flags.

**Fix.** `-Xmx4g` + `-XX:MaxMetaspaceSize=1g` (KSP + Hilt + R8 in one daemon is
where 2 GB dies), `org.gradle.parallel=true`, `org.gradle.caching=true`,
`org.gradle.configuration-cache=true`, `kotlin.incremental=true`,
`android.nonTransitiveRClass=true` kept.

`android.nonFinalResIds` was **rejected**: it has defaulted to `true` since AGP
8.0, so writing it down is cargo cult. The configuration cache is the one flag
here that carries risk — AGP 8.11, KSP 2.2 and Hilt 2.57 all support it, but this
change could not run a build to prove it. It is enabled because the first CI run
will say so unambiguously; if it trips, delete one line.

### 18. Deprecated `kotlinOptions` — confirmed

**Fix.** `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }`, plus
`jvmToolchain(17)` so the build no longer depends on whichever JDK the machine
happens to run, plus the Foojay toolchain resolver in `settings.gradle.kts` so a
contributor whose only JDK is 21 gets a provisioned 17 instead of a build
failure. Java source/target stay at 11 deliberately: moving them is a
desugaring-behaviour change, which is app-behaviour, which this PR does not touch.

### 19. Inline version and signing logic — confirmed, `buildSrc` rejected

Kept in `app/build.gradle.kts`, but rewritten to use `providers.gradleProperty`
and `providers.environmentVariable` instead of direct `System.getenv` reads, so
the values are configuration-cache-tracked inputs rather than invisible ones.

`buildSrc` / a convention plugin was **rejected**: with exactly one module there
is nothing to share, and it costs a separate compilation of the plugin before
every build, a slower configuration-cache miss, and one more place a contributor
has to learn. The gain would be type-safe reuse that has no second consumer.
Revisit the day a `:core` or `:widget` module appears.

### 20-22. Debug type, repository filtering, lint — confirmed

**Fix.** `debug` gets `applicationIdSuffix = ".debug"` and
`versionNameSuffix = "-debug"`, so debug and release coexist on one device.
`exclusiveContent` restricts JitPack to `com.github.samanzamani` (JitPack will
otherwise happily resolve, and cache-poison, any group), Myket to `ir.myket.*`,
and `google()` keeps its regex filter. `lint { }` now sets `abortOnError = true`,
`checkDependencies = true`, HTML/XML/SARIF reports, and treats
`MissingTranslation` as informational (the app is intentionally bilingual with
Persian defaults).

No lint baseline is committed: a baseline can only be generated by running lint,
which was impossible here, and committing an empty one would suppress nothing
while pretending to. If `master` has pre-existing lint errors, the first CI run
fails and the fix is one command: `./gradlew :app:updateLintBaseline`.

Dependency verification (`gradle/verification-metadata.xml`) was **rejected**:
JitPack artifacts are unsigned and its checksums change when a build is
re-triggered, so the file would need a manual override the day PersianDate is
touched, and a stale one blocks every build including CI. Dependency locking was
rejected for the same reason it usually is on Android: the version catalog plus
weekly Dependabot already pins every coordinate, and lockfiles add a second
source of truth. Wrapper validation covers the highest-value part of the supply
chain today.

## New findings, detail

- **23 / 24.** `maven.myket.ir` appears in both `pluginManagement` and
  `dependencyResolutionManagement` and serves **no** coordinate in
  `libs.versions.toml`. JitPack serves exactly one (`com.github.samanzamani:PersianDate`)
  and no plugins. Both are now absent from `pluginManagement` and constrained in
  `dependencyResolutionManagement`; Myket is kept only because a store SDK may be
  planned, and is flagged for deletion if not.
- **25.** Both READMEs say "MIT License"; there is no `LICENSE` file. Without one
  the code is legally all-rights-reserved regardless of what the README says, and
  both F-Droid and Play listings ask for it. Not fixed here: adding a licence is
  the copyright holder's decision, not a build change.
- **26.** `.idea/` is committed with only seven specific children ignored.
- **27.** `gradle-wrapper.properties` has no `distributionSha256Sum` and no
  `validateDistributionUrl`. Wrapper *jar* validation in CI now covers the jar;
  pinning the distribution checksum is a one-line follow-up best done by whoever
  next bumps Gradle.
- **28.** `constraintlayout`, `navigation-fragment-ktx`, `navigation-ui-ktx` and
  `okhttp-mockwebserver` are declared in the catalog and referenced by nothing.
  Left alone: removing catalog entries is version churn, and this PR does not do
  version churn.
- **29.** `README.md` links downloads to `hesCalledAJ/B-Barq/releases`,
  `README.en.md` to `alijafari-gd/B-Barq/releases`, and releases are actually cut
  from this repository. Two of those three are wrong for any given user and the
  files contradict each other. Not silently repointed — the owner has to say which
  repository is canonical. Both READMEs also still describe Yandex AppMetrica in
  their privacy sections, while no AppMetrica dependency exists in the build and
  `docs/CORE_ARCHITECTURE.md` records it as removed: a false privacy disclosure,
  and the one thing here worth fixing today.
- **31.** `enableV1Signing = true` at `minSdk 24`: every supported device verifies
  v2+. v1 (JAR signing) only matters below API 24, costs build time, and is the
  scheme Janus (CVE-2017-13156) attacks. Disabled; v2/v3/v4 kept.
- **32.** `arg("room.schemaLocation", "$projectDir/schemas")` bakes an absolute
  path into the KSP task inputs, which makes the build cache non-relocatable
  between machines and CI. The clean fix is the Room Gradle plugin
  (`androidx.room` 2.7.2 ships one) with `room { schemaDirectory(...) }`. Not done
  here: it means adding a plugin to the catalog, which is exactly the kind of
  change that deserves its own PR and a green instrumented run.

## Layering enforcement

`docs/CORE_ARCHITECTURE.md` defines `ui/ → domain/ → data/` with `data/`
implementing `domain/` interfaces, and states one rule that is mechanically
checkable: **`domain/` imports no `android.*` and no `androidx.*`**. A
`verifyDomainPurity` task now enforces exactly that, wired into `check`, so it
runs on every PR.

The other rule (`ui/` never touches a repository, DAO or DTO) is reported as a
warning rather than a failure. `master` is mid-rewrite: the domain layer already
has the target shape (`error/`, `model/`, `status/`, `time/`, `usecase/`) while
parts of the UI still reach into `data/` directly. Making that check fatal now
would block every PR on unrelated refactoring work. It flips to fatal when
`refactor/core-rewrite` lands.

## Unmerged branches, checked for overlap

| Branch | Overlaps this work? |
|---|---|
| `fix/sign-release-apk` | **Yes.** `a86f844` added the `signingConfig` with the env/properties lookup and the debug fallback, and `ad1c3d8` added the `keystore.properties` / `*.jks` ignores. Both are already on `master`. This PR keeps the working parts (multi-scheme signing, `keystore.properties` support) and fixes what that branch left open: CI never supplies the secrets, and the fallback is silent. Nothing is reverted. |
| `refactor/core-rewrite` | No build or CI files. Source-layer rewrite. |
| `feature/modern-ui-ux-overhaul`, `feature/ui-ux-enhancements-and-features` | No build or CI files. |
| `fix/preferences-viewmodel-application-access` | Merged (#2). Fixed a release-only compile failure — the strongest single argument for finding 6. |
| `docs/build-ci-rewrite-prompt` | The brief for this work. |

## Follow-ups deliberately out of scope

1. `androidx.security:security-crypto` `1.1.0-alpha06` is **deprecated** by
   Google and stuck on an alpha. Token storage should move to a maintained
   approach. Security-relevant, needs app code, so it is not in a build PR.
2. Room Gradle plugin instead of the raw KSP arg (finding 32).
3. `distributionSha256Sum` on the wrapper (finding 27).
4. `isShrinkResources = true` once a minified build has been smoke-tested on a
   device.
5. A `LICENSE` file (finding 25).
6. ktlint or detekt, plus an `.editorconfig`. Formatting is unowned today.
7. Delete the two Studio template test classes and write the migration tests the
   `room-testing` dependency was added for.
8. Commit `app/schemas/4.json` — the one manual step this PR cannot do itself.
