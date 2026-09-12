# Core Audit

Status of the 18 defects identified in Phase 0 of the core rewrite plan.

Legend: **done** = landed on `refactor/core-rewrite`; **pass 2** = specified
here, implemented in the second batch of commits (data / service / worker / UI
/ tests).

## Lifecycle & scheduling

| # | Defect | Fix | Status |
|---|---|---|---|
| 1 | Refresh scheduled with `Handler.postDelayed` at fixed hours (7/12/17/21); dies with the process, no Doze resilience | `OutageRefreshWorker` (WorkManager periodic, unique work, exponential backoff, `NetworkType.CONNECTED` constraint) | pass 2 |
| 2 | Ad-hoc `CoroutineScope(Dispatchers.IO)` with no cancellation | One `CoroutineScope(SupervisorJob() + Dispatchers.Default)` per service, cancelled in `onDestroy()`; `CoroutineWorker` elsewhere | pass 2 |
| 3 | Notification ticker is a self-reposting `Runnable` on the main `Handler`, not data-driven | Ticker becomes a cold `Flow` that recomputes its own delay from the current `ScheduleUrgency`, collected inside the service scope | pass 2 |
| 4 | No reboot / update persistence | `BootReceiver` on `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`, re-enqueues unique work | pass 2 |
| 5 | `isServiceRunning()` uses deprecated `getRunningServices()` | Service publishes its own state; UI observes it instead of polling ActivityManager | pass 2 |

## Data & persistence

| # | Defect | Fix | Status |
|---|---|---|---|
| 6 | Outages live in memory only (`placeCache`), never persisted; `OutageDao` exists but unused | `OutageEntity` + real `OutageDao` writes; repository reads Room as the single source of truth and exposes `Flow` | pass 2 |
| 7 | `PlaceRepository.getPlaces()` is a blocking DB read called from the service | DAO returns `Flow<List<PlaceEntity>>`; all writes are `suspend` on an injected IO dispatcher | pass 2 |
| 8 | Manual `JSONObject` parsing with `.getString()` (throws on any missing field) | Retrofit + kotlinx.serialization DTOs, every field nullable with defaults, `explicitNulls = false`, `ignoreUnknownKeys = true` | pass 2 |
| 9 | Retry uses `Thread.sleep(1000)`, no backoff, no network-state awareness | Retry/backoff delegated to WorkManager (`BackoffPolicy.EXPONENTIAL`) plus a `NetworkType.CONNECTED` constraint; no sleeping threads | pass 2 |
| 10 | Bearer token plain text, no 401 handling, no "re-auth required" state | `EncryptedTokenStore` (EncryptedSharedPreferences); `AuthInterceptor` injects the token and clears it on 401, surfacing `DomainError.Auth.SessionExpired` | pass 2 |
| 11 | No committed schemas, `Outage.id` is `outage_number` (collision-prone), broken migrations | Schemas exported to `app/schemas` (KSP arg landed in this batch); composite id `"{placeId}_{outageNumber}_{date}"`; migration 3 -> 4 recreates only the never-populated `outages` table and leaves `places` untouched | this batch (schema export) + pass 2 |

## Quality

| # | Defect | Fix | Status |
|---|---|---|---|
| 12 | Exceptions as control flow (`BillIDNotFoundException` thrown from nowhere) | `DomainError` sealed hierarchy carried in `Result`; the three ad-hoc exceptions are deleted | **done** (types) + pass 2 (call sites) |
| 13 | Localized strings (`getString(...)`) inside the data layer | Domain returns `OutageStatus` / `DomainError` values with no strings; the UI maps them to string resources | **done** |
| 14 | Time logic scattered across `Calendar`, `PersianDate`, hardcoded midnight wraps | `domain/time`: `TimeZonePolicy`, `JalaliDateTime`, `OutageSchedule`. One wrap rule, one timezone, unit tested | **done** |
| 15 | Alarm `requestCode` from `.hashCode()` (collisions), every fetch re-schedules all alarms | Stable `AlarmId` string persisted with its request code in a monotonic registry; `reconcile()` diffs desired vs persisted and only touches the difference | pass 2 |
| 16 | Logs with profanity (`"received shit"`), possible PII leaks | `AppLogger`, debug-only, with an explicit no-PII contract (no tokens, bill ids, phone numbers) | pass 2 |
| 17 | AppMetrica key hardcoded | Dependency and key removed entirely | **done** |
| 18 | Exact alarm permission silently ignored | `ReminderScheduler` returns `DomainError.Reminder.ExactAlarmsNotPermitted`; UI already has a permission-fix affordance to hook into | pass 2 |

## Corrections applied to the original plan

The plan's code samples were treated as intent, not as source. These items were
changed deliberately.

1. **Migration 3 -> 4 was destructive and would not run.** The v3 `outages`
   table has columns `id, reason, date, outageTime, outageStartTime,
   outageEndTime, billId, address`, so the proposed `INSERT INTO outages_new
   SELECT id, 0, outageNumber, ...` references columns that do not exist;
   `placeId = 0` would violate the new foreign key; and `AutoMigration(3, 4)`
   declared alongside `addMigrations(migration3to4)` is rejected by Room (with
   no committed v3 schema for the auto-migration to read anyway).
   `fallbackToDestructiveMigration()` would then silently wipe `places`,
   contradicting the "existing users keep their places" rule. Because outages
   were never persisted (defect 6), the migration drops and recreates `outages`
   and does not touch `places`.
2. **Missing Gradle plugins.** Hilt and kotlinx.serialization need Gradle
   plugins, not just dependencies, and `@HiltWorker` needs
   `androidx.hilt:hilt-work`.
3. **Worker injection.** `@Inject lateinit var` never works in a `Worker`;
   it requires `@HiltWorker` with an `@AssistedInject` constructor. The sample
   also returned `Result.retry()` on success, which loops forever.
4. **Writes are not cold Flows.** `savePlace(): Flow<Result<Place>>` never runs
   unless collected. All writes are `suspend fun ...: Result<T>`; only reads
   return `Flow`.
5. **`DomainError` did not compile.** `open val message` clashes with
   `Throwable.message`; the message is passed to the `Throwable` constructor
   instead. Messages are developer-facing English for logs only, never shown to
   users (see defect 13).
6. **Hilt context injection.** Every `AppModule` provider took a bare
   `Context`; they need `@ApplicationContext`.
7. **Alarm ids went back to `.hashCode()`**, reintroducing defect 15, and
   `reconcile()` cannot cancel stale alarms without persisting alarm ids. Both
   are addressed with a persisted alarm registry.
8. **`relativeStatus(context: Context)` cannot live in a pure domain layer.**
   Domain returns `OutageStatus`; string resolution stays in the UI.
9. **`ORDER BY date` on unpadded Jalali strings sorts wrong** (`1404/6/22` vs
   `1404/12/1`), so the entity carries a `startEpochMillis` column to sort and
   range-query on.
10. **Throwing a non-`IOException` from an OkHttp interceptor breaks the call
    chain.** The interceptor throws an `IOException` subtype which the
    repository maps to `DomainError.Auth.SessionExpired`.
11. **`typealias DomainResult<T> = Result<T>` was dropped**: it adds an alias
    without adding type safety.
12. **`TimeZonePolicy.OFFSET_SECONDS = 3.5 * 3600` was dropped.** Iran observed
    DST until 2022, so a hardcoded +03:30 misreads every historical timestamp.
    The policy exposes the `Asia/Tehran` zone and all conversion goes through
    `Calendar` with that zone.

## Not verified here

There is no Android SDK or Gradle in the environment these commits were
authored in, so `./gradlew test`, `./gradlew connectedAndroidTest` and
`./gradlew assembleRelease` have not been run. Dependency versions are pinned
to releases that exist as of this commit; if CI resolves something newer,
prefer the newer patch version.
