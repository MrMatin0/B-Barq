# Staged workflow files

> [!IMPORTANT]
> The files in this directory belong in `.github/workflows/`. They were pushed
> here because the credential used to open the pull request had no `workflow`
> scope, which GitHub requires for any write under `.github/workflows/`.
> **Nothing in this directory runs.** Move them before merging, or the PR
> delivers documentation instead of automation.

## Moving them into place

```bash
git checkout chore/build-ci-rewrite
git mv docs/ci/ci.yml                 .github/workflows/ci.yml
git mv docs/ci/instrumented-tests.yml .github/workflows/instrumented-tests.yml
git mv docs/ci/release.yml            .github/workflows/release.yml
git rm .github/workflows/build-release.yml
git rm docs/ci/README.md
git commit -m "ci: activate rewritten workflows"
git push
```

`.github/workflows/build-release.yml` **must** be deleted in the same commit:
leaving it in place means two workflows react to a `v*` tag and the old one
still publishes a debug-signed APK.

## What each file does

| File | Trigger | Purpose |
|---|---|---|
| `ci.yml` | every PR, push to `master` | `assembleDebug` + `test` + `lint` + layering checks, reports as artifacts |
| `instrumented-tests.yml` | `instrumented` label, nightly, manual | `connectedCheck` on an API 24 + API 35 emulator matrix |
| `release.yml` | `v*` tag, manual on a tag | tag/version reconciliation, tests, **signed** APK + AAB, checksums, notes |

## Before the next release

`release.yml` needs four repository secrets. `docs/BUILDING.md` has the exact
commands to produce each value:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Without them the release job fails loudly instead of publishing a debug-signed
APK, which is the whole point of this change.
