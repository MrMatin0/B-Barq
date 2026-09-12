# Building and releasing B-Barq

Everything a contributor or maintainer needs to build the app, sign a release,
and cut a release from a tag. If something here is wrong, fix this file in the
same pull request.

## Requirements

| | Version | Notes |
|---|---|---|
| JDK | **17** | Declared as a Gradle toolchain. Gradle provisions it automatically (Foojay resolver in `settings.gradle.kts`) if your machine only has, say, 21. |
| Android SDK | platform **36**, build-tools **36.x** | `compileSdk`/`targetSdk` are 36. |
| Gradle | **8.13** | Comes from the wrapper. Always use `./gradlew`, never a system `gradle`. |

Android Studio is optional. A checkout, a JDK and `ANDROID_HOME` are enough.

```bash
git clone https://github.com/MrMatin0/B-Barq.git
cd B-Barq
./gradlew --version        # confirms the wrapper resolves
./gradlew assembleDebug
```

The debug build installs as `com.aliJafari.bbarq.debug` with a `-debug` version
suffix, so it sits happily next to a release build on the same device.

## Everyday commands

```bash
./gradlew assembleDebug            # debug APK -> app/build/outputs/apk/debug/
./gradlew test                     # JVM unit tests
./gradlew lint                     # Android Lint; report in app/build/reports/
./gradlew check                    # tests + lint + layering checks
./gradlew connectedDebugAndroidTest # instrumented tests (device/emulator required)
./gradlew printVersionName          # what a release would be called
```

Two project-specific verification tasks run as part of `check`:

- `verifyDomainPurity` **fails** when anything under `domain/` imports
  `android.*` or `androidx.*`. That is the rule from
  `docs/CORE_ARCHITECTURE.md` that keeps the domain layer unit-testable.
- `reportUiLayerLeaks` **warns** when `ui/` imports `data/` directly. It is a
  warning only until the core rewrite lands; see `docs/BUILD_CI_AUDIT.md`.

## Room schemas

Room exports one JSON schema per database version into `app/schemas/`, and the
instrumented tests mount that directory as assets. **Those files are source.**

```bash
./gradlew :app:kspDebugKotlin
git add app/schemas
```

`./gradlew :app:verifyRoomSchemas` fails while the directory has no `*.json`, and
runs as part of `connectedCheck`. Never hand-edit an exported schema: it carries
an `identityHash` that Room computes and compares at runtime.

## Release signing

### Generating the upload key (once, ever)

Do this once, keep the result forever, and back it up somewhere that is not this
repository. **If this key is lost, existing users cannot be upgraded** — they have
to uninstall and reinstall.

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias bbarq-upload \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -storetype JKS
```

`keytool` will ask for a store password, a key password and a distinguished name.
Use a real password manager entry, not a memorable string.

> `*.jks`, `*.keystore` and `keystore.properties` are git-ignored. Never commit
> them, never paste them into an issue, never put them in a build log.

### Signing locally

Create `keystore.properties` in the repository root (git-ignored):

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=<store password>
keyAlias=bbarq-upload
keyPassword=<key password>
```

Or pass the same four values as environment variables — which is what CI does:

```bash
export KEYSTORE_FILE=/absolute/path/to/upload-keystore.jks
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=bbarq-upload
export KEY_PASSWORD=...
./gradlew assembleRelease
```

Then prove the APK is signed with the key you meant:

```bash
"$ANDROID_HOME"/build-tools/36.0.0/apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

If the certificate says `CN=Android Debug`, the build fell back to the debug key
and the artifact must not be published.

### Building a release without a key

`./gradlew assembleRelease` still works with no signing material: it falls back
to the debug keystore and prints a large banner saying so. That exists so
contributors can test an R8-minified build, and nothing else.

On CI it is a hard error. Any task whose name contains `release`, or starts with
`bundle` or `publish`, fails immediately when `CI` is set and no key is present.
Providing *some* of the four values but not all of them also fails immediately,
because a typo'd secret name must not degrade into a debug-signed release.

## Required GitHub secrets

`release.yml` needs exactly four repository secrets
(Settings -> Secrets and variables -> Actions). Commands assume the
[GitHub CLI](https://cli.github.com/); the `gh secret set` calls read from stdin
so the value never lands in your shell history.

| Secret | What it is | How to produce it |
|---|---|---|
| `KEYSTORE_BASE64` | The whole `.jks`, base64-encoded on a single line. The workflow decodes it into `RUNNER_TEMP` and deletes it afterwards. | Linux: `base64 -w 0 upload-keystore.jks \| gh secret set KEYSTORE_BASE64`<br>macOS: `base64 -i upload-keystore.jks \| tr -d '\n' \| gh secret set KEYSTORE_BASE64` |
| `KEYSTORE_PASSWORD` | The store password from `keytool`. | `gh secret set KEYSTORE_PASSWORD` then paste, Ctrl-D |
| `KEY_ALIAS` | The alias, e.g. `bbarq-upload`. | `printf 'bbarq-upload' \| gh secret set KEY_ALIAS` |
| `KEY_PASSWORD` | The key password (often the same as the store password). | `gh secret set KEY_PASSWORD` then paste, Ctrl-D |

Sanity-check the encoding before trusting it — a truncated base64 blob produces a
confusing "invalid keystore format" ten minutes into a release:

```bash
base64 -w 0 upload-keystore.jks | base64 --decode | cmp - upload-keystore.jks && echo "round-trips cleanly"
```

Nothing else is needed: `GITHUB_TOKEN` is provided by Actions, and the release
job asks for `contents: write` on its own.

## Cutting a release

The tag and the app version must agree; the release workflow checks it before it
builds anything and fails the run if they differ.

1. Bump the version in `gradle.properties`:

   ```properties
   bbarq.versionMajor=3
   bbarq.versionPatch=74
   ```

   That yields `versionName 3.74` and `versionCode 30074`
   (`major * 10000 + patch`). Never renumber `versionCode` downwards.

2. Confirm what the build thinks:

   ```bash
   ./gradlew -q printVersionName   # 3.74
   ./gradlew -q printVersionCode   # 30074
   ```

3. Commit, tag with a leading `v`, push both:

   ```bash
   git commit -am "build: version 3.74"
   git tag v3.74
   git push origin master v3.74
   ```

4. `release.yml` then: verifies the tag against `versionName`, runs the unit
   tests and lint, decodes the keystore, builds a **signed** APK and AAB,
   asserts with `apksigner` that the artifact is not debug-signed, generates
   `SHA256SUMS.txt`, and publishes a release with GitHub-generated notes.
   `mapping.txt` is uploaded as a 90-day build artifact — keep it, or crash
   reports from that build stay unreadable forever.

5. Pre-releases: tag with `-rc`, `-beta` or `-alpha` (`v3.74-rc1`) and the
   release is marked as a pre-release automatically.

A manual run (`workflow_dispatch`) works too, but only when the ref you select is
a `v*` tag. The workflow refuses to run on a branch, because that used to create
a release literally tagged `master`.

## Failure modes worth recognising

| Symptom | Cause | Fix |
|---|---|---|
| `Tag 'vX.Y' does not match versionName` | Tag pushed without bumping `gradle.properties`, or vice versa | Fix one of the two and re-tag |
| `Release signing is half configured` | Some of the four values set, others missing (usually a typo'd secret name) | Provide all four, or none |
| `Refusing to build a release without the upload key on CI` | `KEYSTORE_BASE64` and friends not set on the repository | Add the secrets above |
| `Release APK is signed with the DEBUG key` | The signing config did not apply | Check the secret names, then `apksigner verify --print-certs` locally |
| `No Room schema found under app/schemas/` | Exported schema never committed | `./gradlew :app:kspDebugKotlin && git add app/schemas` |
| `The domain layer must stay Android-free` | An `android.*`/`androidx.*` import crept into `domain/` | Move it to `data/` or `ui/` behind a domain interface |
| Lint fails with pre-existing errors | Lint now aborts on error and no baseline exists | `./gradlew :app:updateLintBaseline` and commit `app/lint-baseline.xml` |
| Configuration-cache errors after a plugin bump | A plugin regressed configuration-cache support | Remove `org.gradle.configuration-cache=true` from `gradle.properties`, open an issue |

## Continuous integration

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | every PR, push to `master` | wrapper validation, `assembleDebug`, `test`, layering checks, `lint`; uploads reports and a debug APK |
| `instrumented-tests.yml` | `instrumented` label on a PR, nightly, manual | `connectedDebugAndroidTest` on an API 24 + API 35 emulator matrix |
| `release.yml` | `v*` tag, manual on a tag | the release process described above |

Every third-party action is pinned to a full commit SHA with a version comment;
Dependabot updates them weekly. GitHub-owned `actions/*` stay on major tags on
purpose.
