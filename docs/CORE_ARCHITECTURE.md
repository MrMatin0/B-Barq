# Core Architecture

Target design for the B-Barq core rewrite. Written for the reader who has to
change this code six months from now.

## Layering

```
ui/       Compose screens, ViewModels, string resources, permission plumbing
  |  depends on domain only
domain/   pure Kotlin: models, time, errors, use cases, service interfaces
  |  depends on nothing Android
data/     Retrofit, Room, DataStore, EncryptedSharedPreferences, WorkManager,
          AlarmManager, foreground service. Implements domain interfaces.
```

Rules that are enforced by review, not by the compiler (single-module project):

- `domain/` imports no `android.*` and no `androidx.*`. `java.util.Calendar`,
  `java.util.TimeZone` and `kotlinx.coroutines` are allowed: they run on a plain
  JVM, which is what makes the layer unit-testable without Robolectric.
- `domain/` contains no user-facing text. It returns values (`OutageStatus`,
  `DomainError`); `ui/` maps them to string resources. This is what fixes
  localized strings leaking into the data layer.
- `ui/` never touches a repository, DAO or DTO. It talks to use cases and
  ViewModels.
- `data/` never returns a DTO or a Room entity outward. Mappers convert at the
  boundary.

## Time: the part that was actually broken

All outage times arrive as Jalali date strings plus `HH:mm` strings, in Tehran
local time. Three rules:

1. **One timezone.** `TimeZonePolicy` exposes `Asia/Tehran`. Every
   string -> epoch and epoch -> string conversion goes through a `Calendar`
   created in that zone. Nothing uses the device default zone, so a user
   travelling abroad still sees Tehran outage times.
2. **Store epochs, render fields.** Persistence and comparison use
   `Long` epoch millis. Jalali fields exist only at the edges.
3. **One midnight-wrap rule.** `OutageSchedule.endEpochMillis` adds 24h when the
   end instant is at or before the start instant. That rule exists in exactly
   one place; `18:00 -> 02:00` is a six hour outage everywhere in the app.

### Why `PersianDate` is wrapped rather than used directly

`PersianDate` (samanzamani 1.7.1) is kept for calendar conversion, but only two
of its methods are used: `jalali_to_gregorian` and `gregorian_to_jalali`. They
are pure integer arithmetic (JDF algorithm), stateless and therefore safe to
call from any thread.

Everything else in that class is avoided on purpose:

- Its internal `timeInMilliSecond` is computed with a `SimpleDateFormat` in the
  **device default timezone**. There is no way to pin it to Tehran, so
  `PersianDate.getTime()` is never trusted; `JalaliDateTime.toEpochMillis()`
  builds the instant with an explicit `Asia/Tehran` `Calendar` instead.
- Its setters are order dependent and throw: `setShDay(31)` validates against
  the month that happens to be set at that moment, so the old
  "set year, then month, then day" idiom could throw `IllegalArgumentException`
  on perfectly valid API data.
- Its `isLeap()` uses a 33-year cycle approximation. `JalaliDateTime` derives
  Esfand's length by round-tripping day 30 through the converter instead, so
  validity is defined by the same function that does the conversion. That is
  what guarantees `fromEpochMillis()` can never throw for a real instant.
- Its `after()` is inverted (it returns true when the receiver is *before* the
  argument). Comparison lives in `JalaliDateTime`, which implements
  `Comparable`.

### Behaviour preserved on purpose

`GetOutageStatusUseCase` reproduces the existing labels exactly, including two
quirks worth naming so nobody "fixes" them by accident:

- The "round up when the remainder is >= 50 minutes" rule, so 1h55m displays as
  "in 2 hours".
- A date-only outage today maps to urgency `SOON`, while "tomorrow" maps to
  `TODAY`. Odd, but it is what the notification ticker interval already depends
  on.

One intentional cosmetic change: times are normalised to zero-padded `HH:mm`
via `Locale.ROOT`, so `9:30` renders as `09:30` and Persian locales cannot leak
Persian digits into API payloads or comparisons.

## Data flow after the rewrite

```
WorkManager (periodic, network constrained) ─┐
Foreground service (user visible, ticking)  ─┼─> FetchAndCacheOutagesUseCase
Manual refresh action                       ─┘            |
                                                          v
                             OutageRepository ── Retrofit ── uiapi.saapa.ir
                                    |                 (AuthInterceptor: token, 401)
                                    v
                              Room (source of truth)
                                    |
            ┌───────────────────────┼───────────────────────┐
            v                       v                       v
  ScheduleRemindersUseCase   notification text        UI state (Flow)
            |
            v
  ReminderScheduler -> AlarmManager (exact, reconciled against a persisted registry)
```

The service no longer owns the cache. Room does. The service observes,
renders, and reconciles alarms; a crash or a reboot loses nothing but the
notification itself.

## Error model

`DomainError` is a sealed hierarchy under `Throwable`, carried inside
`kotlin.Result`. Reads return `Flow<Result<T>>`; writes are
`suspend fun ...: Result<T>`. There are no thrown domain exceptions across a
layer boundary and no `null` used to mean "failed".

Because OkHttp only tolerates `IOException` from an interceptor, the 401 path
throws an `IOException` subtype inside `data/remote` which the repository maps
to `DomainError.Auth.SessionExpired`. The UI reacts to that one value by
sending the user back to the login flow.

## Dependency matrix

| Concern | Library | Pinned | Notes |
|---|---|---|---|
| DI | `com.google.dagger:hilt-android` | 2.57.1 | 2.51 from the plan predates Kotlin 2.2 / KSP2 support |
| DI (Worker) | `androidx.hilt:hilt-work` + compiler | 1.4.0 | requires KGP >= 2.2.0, which this project is on |
| HTTP | `retrofit` + `converter-kotlinx-serialization` | 2.11.0 | keeps the existing OkHttp 5 client |
| JSON | `kotlinx-serialization-json` | 1.7.1 | `ignoreUnknownKeys`, defaults for every field |
| DB | `androidx.room` | 2.7.2 | project was already on 2.7.2; plan said 2.6.1 |
| Prefs | `androidx.datastore:datastore-preferences` | 1.2.1 | replaces `PreferencesManager` |
| Token | `androidx.security:security-crypto` | 1.1.0-alpha06 | as specified; note Google has deprecated this artifact, so treat it as replaceable |
| Background | `androidx.work:work-runtime-ktx` | 2.11.2 | periodic refresh + backoff |
| Jalali | `com.github.samanzamani:PersianDate` | 1.7.1 | kept, wrapped, only pure converters used |
| Analytics | AppMetrica | removed | hardcoded key deleted; add Sentry or Crashlytics later if wanted |

## Commit order

1. Docs (this file and `CORE_AUDIT.md`).
2. Build wiring (Hilt, serialization, Room schema export), DI scaffolding,
   domain layer, and the unit tests for the time and status logic. The domain
   tests ship with the code they cover rather than waiting for the test commit,
   because the time layer is the highest-risk part of the rewrite.
3. Data layer: Retrofit + DTOs, Room entities/DAOs, migration 3 -> 4, encrypted
   token store, DataStore preferences, repositories.
4. Service and reminder scheduler.
5. Worker and boot receiver.
6. UI adaptation to the new APIs.
7. Remaining tests (repositories, migration, service lifecycle) and
   `docs/MIGRATION.md`.
