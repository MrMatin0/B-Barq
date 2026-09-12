# AI Agent Prompt — Full Audit & Rewrite of the Build System and CI

> Copy everything below the line into your AI coding agent (Claude Code, Codex,
> Copilot Agent, Cursor, Jules, ...) with this repository checked out.
> The prompt is intentionally self-contained: it describes the project, the
> current state of the build, the known defects, the required deliverables, and
> the acceptance criteria. Do not summarise it; agents behave worse with less
> context, not better.

---

## ROLE

You are a senior Android build engineer and release engineer. You own the Gradle
build configuration and the CI/CD pipeline of this repository. You are not here
to touch application logic: your mandate is the **build**, the **packaging**, the
**signing**, and the **automation** around them.

You work carefully, you verify your own output by running the build, and you
explain every decision in the commit history and the pull request body.

## PROJECT CONTEXT (verify this yourself, do not trust it blindly)

`B-Barq` is a native Android application that monitors scheduled power outages
for multiple places from the official BargheMan/SAAPA source and fires reminders
before each outage.

**Shape of the project**

- Single Gradle module: `:app`. Root `build.gradle.kts` only declares plugins
  with `apply false`. `settings.gradle.kts` sets `rootProject.name = "BBarq"`,
  uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, and adds the `jitpack.io` and
  `maven.myket.ir` repositories on top of `google()` / `mavenCentral()`.
- Kotlin DSL everywhere, dependencies centralised in
  `gradle/libs.versions.toml` (version catalog).
- Toolchain: Gradle **8.13** (wrapper), AGP **8.11.1**, Kotlin **2.2.0**,
  KSP **2.2.0-2.0.2**.
- `namespace` / `applicationId`: `com.aliJafari.bbarq`.
  `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- `versionCode` is computed as `versionMajor * 10000 + versionPatch` from two
  hardcoded local `val`s inside the `android { }` block
  (currently `3` and `73` → `30073`, `versionName = "3.73"`).
- Java/Kotlin target: **11** (`compileOptions` + the now-deprecated
  `kotlinOptions { jvmTarget = "11" }`). CI runs on **JDK 17**. No Gradle/Kotlin
  toolchain is declared anywhere, so the build silently depends on whatever JDK
  the machine provides.
- Architecture (see `docs/CORE_ARCHITECTURE.md`): strict `ui/` → `domain/` →
  `data/` layering inside the single module, enforced by review only.
- Stack: Jetpack Compose + Material 3 (BOM `2025.05.00`), Hilt `2.57.1` +
  `androidx.hilt:hilt-work` `1.4.0`, Room `2.7.2` via KSP with
  `room.schemaLocation = "$projectDir/schemas"`, Retrofit `2.11.0` +
  `kotlinx-serialization-json` `1.7.1`, OkHttp `5.1.0`, WorkManager `2.11.2`,
  DataStore, `androidx.security:security-crypto` (deprecated artifact),
  Glance app widget `1.1.1`, `com.github.samanzamani:PersianDate` from JitPack.
- Tests: `app/src/test` (JUnit4 + `kotlinx-coroutines-test`, covering the
  domain/time layer, plus a leftover `ExampleUnitTest.kt`) and
  `app/src/androidTest` (Espresso, `room-testing`, `work-testing`,
  `compose-ui-test-junit4`). Room migration tests read exported schemas as
  instrumentation assets via a conditional `assets.srcDir(...)`.
- `metadata/en_US/` exists, which implies an F-Droid style listing.
- Docs live in `docs/` (`CORE_ARCHITECTURE.md`, `CORE_AUDIT.md`), READMEs are
  bilingual (`README.md` Persian, `README.en.md` English).

**Current CI, in full.** There is exactly one workflow,
`.github/workflows/build-release.yml`:

```yaml
name: Build and Release APK
on:
  push:
    tags: ['v*']
  workflow_dispatch:
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17', cache: 'gradle' }
      - run: chmod +x gradlew
      - run: ./gradlew assembleRelease --stacktrace
      - uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/*.apk
```

That is the entire automation surface of this project.

## PHASE 1 — AUDIT (do this before you change a single line)

Read, at minimum: `settings.gradle.kts`, `build.gradle.kts`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`,
`gradle/wrapper/gradle-wrapper.properties`, `app/proguard-rules.pro`,
`.gitignore`, `app/src/main/AndroidManifest.xml`, everything under
`.github/`, and the two files in `docs/`. Also list the unmerged branches
(`fix/sign-release-apk`, `refactor/core-rewrite`, ...) and check whether any of
them already attempted part of this work — do not duplicate or regress it.

Produce a written audit as `docs/BUILD_CI_AUDIT.md`: every finding, its concrete
impact, its severity, and the fix you chose. The findings below are ones already
identified from the outside. **Confirm each one against the code, correct me
where I am wrong, and add whatever you find that is not on this list.**

### Blocking correctness defects

1. **Releases are published debug-signed.** `app/build.gradle.kts` resolves
   signing material from `keystore.properties` or from the `KEYSTORE_FILE` /
   `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` environment variables, and
   when none are present it deliberately falls back to `signingConfigs.debug`
   with a `logger.warn`. The release workflow **never** provides those secrets
   and never materialises a keystore file. Result: every artifact ever attached
   to a GitHub Release was signed with the throwaway debug key. Users cannot
   upgrade across a key change, and the APK is not publishable. The warning is
   buried in build output that nobody reads.
2. **R8 is enabled with an empty keep file.** `isMinifyEnabled = true` on
   `release`, while `app/proguard-rules.pro` is still the unmodified Android
   Studio template (only comments). The project uses `kotlinx.serialization`,
   Retrofit (generic return types + annotations), Room, Hilt and Glance — all of
   which need keep/`-keepattributes` rules or reflective metadata. Release
   builds can fail at runtime in ways debug builds never reproduce. Nothing in
   CI would catch it, because CI never installs or instruments the release APK.
3. **Crash reports from release builds are unreadable.**
   `-keepattributes SourceFile,LineNumberTable` and
   `-renamesourcefileattribute SourceFile` are commented out, and the R8
   `mapping.txt` is discarded rather than uploaded as an artifact or attached to
   the release.
4. **Room schemas are exported but not committed.** KSP writes to
   `app/schemas`, the `androidTest` source set adds that directory as assets
   *only if it exists*, and it does not exist in version control. Migration
   tests therefore silently degrade instead of failing loudly — and instrumented
   tests never run in CI at all.
5. **Build output is committed to git.** `app/release/output-metadata.json` and
   `app/release/baselineProfiles/` are checked in. `.gitignore` only ignores
   `/build` and `app/.gitignore` ignores `/build`, so this directory slipped
   through. No baseline profile is actually wired into the build, so the
   committed one is dead weight and misleading.

### Pipeline gaps

6. **There is no continuous integration.** The only workflow triggers on tag
   push and manual dispatch. Nothing compiles, tests, or lints a pull request or
   a `master` push. `./gradlew test`, `lint`, and `assembleDebug` have never run
   on this repository automatically.
7. **No Gradle wrapper validation** (`gradle/actions/wrapper-validation`), so a
   tampered `gradle-wrapper.jar` would execute unnoticed.
8. **Weak caching.** `actions/setup-java` with `cache: 'gradle'` instead of
   `gradle/actions/setup-gradle`, which gives proper configuration-cache-aware
   caching, build scans, job summaries and dependency-graph submission.
9. **Third-party actions are floating on mutable tags** (`@v4`, `@v2`). Pin
   every non-GitHub-owned action to a full commit SHA with a trailing version
   comment.
10. **`permissions: contents: write` is granted workflow-wide** rather than at
    the one job that needs it. Default to `contents: read` at the top.
11. **No `concurrency` group**, so duplicate/superseded runs pile up and race
    each other on the same release.
12. **The release step is unguarded.** `files: .../release/*.apk` with no
    `fail_on_unmatched_files: true`: if the build produces nothing, the workflow
    still "succeeds" and creates an empty release. No release name, no body, no
    generated notes, no `draft`/`prerelease` handling, no checksums.
13. **No AAB and no ABI/density splits.** Only a universal APK is produced, so
    Play Store distribution is impossible and the download is larger than it
    needs to be.
14. **The git tag and the app version are never reconciled.** Pushing `v9.9`
    would happily publish `versionName 3.73`. Nothing validates the two agree.
15. **No dependency automation** — no `dependabot.yml` (or Renovate) for Gradle
    dependencies and GitHub Actions.
16. **No repository hygiene automation**: no PR template, no CODEOWNERS, no
    stale-lockfile/lint gate, no F-Droid-friendly reproducibility check even
    though `metadata/en_US/` implies an F-Droid listing.

### Build configuration smells

17. `gradle.properties` leaves performance almost entirely on the table:
    `org.gradle.jvmargs=-Xmx2048m` only, `org.gradle.parallel` commented out, no
    `org.gradle.caching`, no `org.gradle.configuration-cache`, no
    `android.nonFinalResIds`, no `kotlin.incremental`.
18. `kotlinOptions { jvmTarget = "11" }` is deprecated on Kotlin 2.2; the modern
    form is the `kotlin { compilerOptions { jvmTarget = ... } }` DSL, ideally
    paired with an explicit `jvmToolchain(...)` so local and CI builds agree.
19. Version/signing logic sits inline in `app/build.gradle.kts`, mixing
    filesystem reads, env lookups and conditional `signingConfigs` creation into
    the module script. With a single module a full `buildSrc`/convention-plugin
    setup may be overkill — decide, justify your decision in the audit, and be
    consistent.
20. No `debug` build type customisation at all: no `applicationIdSuffix`, no
    `versionNameSuffix`, so debug and release builds cannot coexist on a device.
21. `jitpack.io` and `maven.myket.ir` are unfiltered in
    `dependencyResolutionManagement`; only `google()` has content filtering.
    Constrain each repository to the groups it actually serves.
22. No dependency verification or lockfile, no `lint` baseline, and no
    `lintOptions`/`lint { }` configuration deciding whether lint errors fail the
    build.

## PHASE 2 — REWRITE (the deliverables)

Rewrite, do not patch. The end state must be something a new contributor can
read top to bottom and trust.

### A. Gradle build

- Modernise `app/build.gradle.kts`: current Kotlin `compilerOptions` DSL,
  explicit JVM toolchain, cleanly separated `debug`/`release` build types, debug
  suffixes, and a version definition that is single-sourced (version catalog,
  `gradle.properties`, or a dedicated file — your call, justified).
- **Fix signing properly.** `assembleRelease` in CI must use the real upload key
  from repository secrets (base64-encoded keystore decoded to a temp path,
  passwords from secrets, file removed afterwards). Local developer builds
  without a keystore must keep working, but the fallback must be **loud and
  impossible to publish by accident**: fail the build outright for any
  release/publish task in CI (`if (System.getenv("CI") != null) error(...)`) and
  keep the debug fallback for local only. Document the exact secret names.
- Write real `proguard-rules.pro` content for every library in the stack
  (kotlinx.serialization, Retrofit + OkHttp, Room, Hilt, Glance, PersianDate),
  restore `SourceFile`/`LineNumberTable` attributes, and preserve `mapping.txt`.
- Commit the Room schema directory (with a real exported schema) and make the
  `androidTest` assets wiring unconditional so a missing schema fails loudly.
- Add per-ABI splits and/or an `.aab` output, plus tuned `packagingOptions`.
- Tune `gradle.properties` (parallel, build cache, configuration cache,
  sensible heap) and verify each flag actually works rather than cargo-culting
  it.
- Remove `app/release/` from version control and extend `.gitignore` so build
  output can never be committed again.
- Constrain repository content filtering in `settings.gradle.kts`.

### B. CI/CD

Replace the single workflow with a small, purposeful set. Suggested shape —
deviate if you can defend it:

1. `ci.yml` — on `pull_request` and on push to `master`.
   Wrapper validation → `setup-gradle` → `./gradlew assembleDebug test lint`
   with the Kotlin/Android lint and unit-test reports uploaded as artifacts and
   surfaced in the job summary. Must fail the PR on any failure.
2. `instrumented-tests.yml` — Android emulator matrix (`reactivecircus/
   android-emulator-runner`, API 24 as `minSdk` and a current API level) running
   `connectedCheck`, including the Room migration tests. Gate it behind a label
   or a schedule if runtime is a concern, and say so.
3. `release.yml` — on `v*` tag push and `workflow_dispatch`.
   Verify the tag matches `versionName`, run the full test suite first, build
   the **signed** release APK (+ AAB), generate SHA-256 checksums, produce
   release notes from the commit range, and attach everything with
   `fail_on_unmatched_files: true`. Upload `mapping.txt` as a build artifact.
4. `dependabot.yml` for `gradle` and `github-actions` ecosystems, weekly,
   grouped sensibly.

Across all workflows: least-privilege `permissions` per job, a `concurrency`
group with `cancel-in-progress` for CI (never for release), every third-party
action SHA-pinned with a version comment, explicit `timeout-minutes`, and
`JAVA_VERSION` / Gradle flags defined once as `env` rather than repeated.

### C. Documentation

- `docs/BUILD_CI_AUDIT.md` — the Phase 1 findings, as specified above.
- `docs/BUILDING.md` — how to build locally, how to set up
  `keystore.properties`, how to generate the release keystore, the exact list of
  required GitHub secrets with the command to produce each value, and how to cut
  a release.
- Update `README.md` (Persian) and `README.en.md` (English) where they describe
  installation or releases. Both must stay in sync; the Persian file is the
  primary one.

## HARD CONSTRAINTS

- **Do not change application source code** under `app/src/main/java` except
  where a build change strictly requires it (for example a `BuildConfig` field
  rename). If you believe app code must change, stop and explain in the PR
  instead of doing it silently.
- **Do not bump library versions** as part of this work. Version upgrades are a
  separate PR; note anything urgent (`security-crypto` is deprecated) in the
  audit instead.
- **Never commit secrets**: no keystores, no `keystore.properties`, no
  passwords, no tokens, not even as examples. Use placeholders.
- Preserve the existing `applicationId`, `namespace`, `minSdk`, and the
  `versionCode` scheme. Do not silently renumber a shipped app.
- Keep the strict `ui/` → `domain/` → `data/` layering described in
  `docs/CORE_ARCHITECTURE.md` intact; if you add a lint or verification task,
  make it enforce that boundary rather than break it.
- Every YAML you write must be valid and every action reference must exist at
  the SHA you pin. Do not invent action inputs — check the action's README.

## VERIFICATION (mandatory, do not skip)

Before you open the pull request, prove your work:

1. `./gradlew --version` and confirm the wrapper still resolves.
2. `./gradlew assembleDebug` — must pass.
3. `./gradlew test` — must pass.
4. `./gradlew lint` — review the report; no new errors.
5. `./gradlew assembleRelease` locally without a keystore — must produce a
   debug-signed APK **with a loud warning**, and must fail if `CI=true`.
6. `./gradlew assembleRelease` with a throwaway locally generated keystore —
   inspect the output with `apksigner verify --print-certs` and confirm it is
   signed with that key, not the debug key. Delete the keystore afterwards.
7. Confirm the R8-minified release APK actually runs the serialization and
   Retrofit paths (a smoke instrumented test or a manual install both count).
8. Lint every workflow file (`actionlint` if available) and re-read each one
   asking "what happens if this step fails?".

If any step cannot be run in your environment, say so explicitly in the PR body
under a `## Not verified` heading. Do not claim you ran something you did not.

## DELIVERY

1. Create a branch: `chore/build-ci-rewrite`.
2. Commit in reviewable, logically separated steps with conventional-commit
   messages, in this order:
   1. `docs: audit current build and CI` (the audit file only)
   2. `chore: remove committed build output`
   3. `build: modernise gradle configuration and toolchain`
   4. `build: fix release signing and R8 keep rules`
   5. `ci: add pull request pipeline`
   6. `ci: rewrite release pipeline with real signing`
   7. `ci: add dependabot and repository hygiene`
   8. `docs: document building and releasing`
3. Push the branch.
4. Open a pull request against `master` titled
   **`Rewrite build configuration and CI/CD pipeline`**, with a body containing:
   - a two-sentence summary of why this was needed;
   - a findings table (severity, finding, fix);
   - the full list of GitHub secrets a maintainer must add before the next
     release, with the command to generate each;
   - the verification results from the section above;
   - a `## Not verified` section if applicable;
   - a `## Breaking / action required` section — at minimum, the fact that
     previous releases were debug-signed and the signing-key change means users
     on old builds must reinstall rather than upgrade.
5. Do **not** merge. Leave the PR for human review.

## OUTPUT STYLE

Be explicit about trade-offs. When you choose between two valid approaches
(convention plugin vs inline config, emulator tests on every PR vs nightly), name
the alternative and say why you rejected it. If something in this prompt is wrong
or no longer true of the codebase, correct it in the PR body instead of silently
working around it.
