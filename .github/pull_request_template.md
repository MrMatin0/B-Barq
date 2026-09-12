## What and why

<!-- One or two sentences. What changes, and what problem it solves. -->

## How it was verified

<!--
Tick what you actually ran. Do not tick what CI ran for you; say so instead.
An honest empty section beats a checked box that is not true.
-->

- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew test`
- [ ] `./gradlew lint` (no new errors)
- [ ] `./gradlew connectedDebugAndroidTest` (or added the `instrumented` label)
- [ ] Installed on a device / emulator and exercised the change

### Release builds only

- [ ] `./gradlew assembleRelease` passes with R8 enabled
- [ ] The R8-minified build still parses API responses (serialization + Retrofit)

## Checklist

- [ ] No secrets, keystores or `keystore.properties` in the diff
- [ ] No build output (`app/build/`, `app/release/`) in the diff
- [ ] Layering respected: `ui/` -> `domain/` -> `data/` (`docs/CORE_ARCHITECTURE.md`)
- [ ] Room: `@Database(version = ...)` bumped **and** a `Migration` **and** the
      exported schema in `app/schemas/` committed together, if the DB changed
- [ ] Both READMEs updated if user-facing behaviour, install or release changed
      (`README.md` is the primary one, `README.en.md` mirrors it)

## Notes for the reviewer

<!-- Trade-offs, alternatives you rejected, anything you are unsure about. -->
