# Room exported schemas

Room exports one JSON file per database version into this directory
(`room.schemaLocation` in `app/build.gradle.kts`), and
`app/src/androidTest` mounts it as an instrumentation asset directory so
migration tests can open an old schema and migrate it forward.

**These files are source, not build output. Commit them.**

The current database version is **4** (`data/local/DataBase.kt`), with
migrations 1→2, 2→3 and 3→4.

## Generating them

```bash
./gradlew :app:kspDebugKotlin
git add app/schemas
```

This produces `app/schemas/com.aliJafari.bbarq.data.local.ADatabase/4.json`.

The file cannot be written by hand: it carries an `identityHash` that Room
computes from the schema and compares at runtime, so a hand-edited file makes
migration tests fail for the wrong reason.

`./gradlew :app:verifyRoomSchemas` fails while this directory has no `*.json`,
and runs as part of `connectedCheck`.

## Rules

- Never edit an exported schema by hand.
- Never delete an old version's JSON: it is the only record of what shipped.
- Bumping `@Database(version = ...)` means committing a new JSON **and** a
  `Migration` in the same pull request.
