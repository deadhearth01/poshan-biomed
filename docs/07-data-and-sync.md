# 07 — Data and Sync

Poshan is offline-first. Every screen works with no network; all state is written to the
device filesystem synchronously; Appwrite sync is an opportunistic background push that
can fail without the app noticing anything at the UI level.

---

## 1. Local persistence

### 1.1 `JsonFileStore`

`data/local/JsonFileStore.kt` — a typed, whole-file JSON store under
`context.filesDir`, with an atomic write:

```
write(value) → serialise to "<name>.json.tmp" → rename over "<name>.json"
```

The temp-then-rename means a crash mid-write cannot leave a truncated file, though it does
mean the whole collection is rewritten on every change. That is fine at this scale (one
Anganwadi worker's caseload) and would not be at ten thousand records.

| File | Contents |
|---|---|
| `children.json` | `List<ChildProfile>` |
| `sessions.json` | `List<AssessmentSession>` |
| `profiles.json` | `List<UserProfile>` |

`data/local/PrefsStore.kt` wraps DataStore Preferences for scalar settings (language,
onboarding completion, active child id). `data/local/PoshanContainer.kt` is the manual DI
container built once in `PoshanApplication`.

### 1.2 `PoshanRepository`

`data/repo/PoshanRepository.kt` is the single source of truth: it holds the in-memory
`StateFlow`s the UI observes and writes through to the JSON stores on every mutation.
There is no Room database and no migration machinery — schema evolution relies on
`kotlinx.serialization` defaults, so **every new model field must have a default value** or
existing files fail to parse. See `docs/01-architecture.md` §4.

---

## 2. Remote configuration

`data/remote/AppwriteService.kt`:

| Item | Value |
|---|---|
| Endpoint | `BuildConfig.APPWRITE_ENDPOINT`, normalised — a config value missing `/v1` gets it appended, because Appwrite refuses every request without the version segment |
| Project | `BuildConfig.APPWRITE_PROJECT_ID` |
| Database | `BuildConfig.APPWRITE_DATABASE_ID` (`poshan_db`) |
| Enabled | `isConfigured = APPWRITE_ENABLED && APPWRITE_PROJECT_ID.isNotBlank()` |

`APPWRITE_ENABLED` falls back to **`false`** when the Gradle property is absent, which
makes the entire sync layer inert with no error. See `docs/09-build-and-release.md` §3.1.

Collection ids: `profiles`, `children`, `assessments`, `growth_records`,
`attention_records`, `vitamin_d_records`.

### 2.1 Authentication

`data/remote/AuthRepository.kt` — `ensureRemoteSession()` creates an **anonymous** Appwrite
session. It catches `AppwriteException` and, when the message indicates a session is
already active, re-reads `account.get()` rather than reporting a bogus failure. Other
failures surface the Appwrite message and code plus an actionable hint (anonymous sign-in
disabled, project not found, rate limited, server unreachable).

No user credentials are collected or stored. Documents are owned by the anonymous session
user id, which is persisted so the same device keeps access to its own records.

---

## 3. Server schema

Database `poshan_db`. Provisioned by `appwrite/schema.ps1` (PowerShell, run from a
workstation) and `appwrite/functions/schema-provisioner/index.js` (the same schema as an
Appwrite Function). Both are idempotent: they create what is missing and re-apply
collection permissions on every run.

### 3.1 Collection permissions

Every collection:

```
create("users")                              ← an authenticated session may create
read("label:admin")                          ← supervisor access
update("label:admin")
delete("label:admin")
documentSecurity = true                      ← per-document ACLs also apply
```

Documents themselves are written **owner-only**: `read/update/delete(user:<id>)`. The
admin label grants supervisor visibility at the collection level, which is where it
belongs — see §6.1 for why putting it on the document was a bug.

### 3.2 Attributes

**`profiles`**

| Attribute | Type | Required |
|---|---|---|
| `user_id` | string(64) | yes |
| `role` | string(32) | yes |
| `display_name` | string(128) | no |
| `phone` | string(32) | no |
| `relationship` | string(64) | no |
| `location` | string(256) | no |
| `anganwadi_name` | string(128) | no |
| `anganwadi_code` | string(16) | no |
| `facility_name` | string(128) | no |
| `hfr_id` | string(64) | no |
| `employee_id` | string(64) | no |
| `is_guest` | boolean | no |

**`children`**

| Attribute | Type | Required |
|---|---|---|
| `child_id` | string(64) | yes |
| `name` | string(128) | yes |
| `date_of_birth_epoch_day` | integer | yes |
| `gender` | enum(MALE, FEMALE, OTHER) | yes |
| `born_premature` | boolean | no |
| `gestational_age_weeks` | integer | no |
| `parent_name` | string(128) | no |
| `parent_mobile` | string(32) | no |
| `village` | string(128) | no |
| `created_by_role` | string(32) | no |
| `owner_id` | string(64) | no |

**`assessments`** — the large JSON blobs keep the session self-contained rather than
normalised across collections.

| Attribute | Type | Required |
|---|---|---|
| `assessment_id` | string(64) | yes |
| `child_id` | string(64) | yes |
| `assessment_date` | integer (epoch ms) | yes |
| `age_months` | float | no |
| `corrected_age_months` | float | no |
| `answers_json` | string(16384) | no |
| `growth_json` | string(2048) | no |
| `nutrition_json` | string(4096) | no |
| `attention_json` | string(4096) | no |
| `vitamin_d_json` | string(4096) | no |
| `given_vaccines_json` | string(4096) | no |
| `category_results_json` | string(8192) | no |
| `composite_json` | string(4096) | no |
| `dq` | float | no |
| `status` | string(16) | no |
| `assessor_role` | string(32) | no |
| `owner_id` | string(64) | no |

> `answers_json` is capped at 16 KB. With catch-up screening a child assessed at 59 months
> can answer all 123 questions in one session. **Unverified whether 123 serialised answers
> fit in 16 KB** — measure before it bites in the field; the failure mode would be a
> `document_invalid_structure` on the largest, most clinically interesting sessions.

**`growth_records`**

| Attribute | Type | Required |
|---|---|---|
| `child_id` | string(64) | yes |
| `record_date` | integer (epoch ms) | yes |
| `weight_kg` | float | no |
| `height_cm` | float | no |
| `head_circumference_cm` | float | no (legacy — nothing writes it; see §6.2) |
| `muac_mm` | float | no |
| `owner_id` | string(64) | no |

**`attention_records`**

| Attribute | Type | Required |
|---|---|---|
| `child_id` | string(64) | yes |
| `task_type` | string(32) | no |
| `valid_frame_ratio` | float | no |
| `on_screen_ratio` | float | no |
| `social_attention_ratio` | float | no |
| `blink_rate` | float | no |
| `engagement_index` | float | no |
| `band` | string(16) | no |
| `inconclusive` | boolean | no |
| `record_date` | integer | no |
| `owner_id` | string(64) | no |

> This collection stores only 5 of the ~20 metrics `AttentionMetrics` computes. Gaze
> following, disengagement latency, saccade rate, head-movement rate, confidence and the
> touch-task results are **not synced** — they exist only inside `attention_json` on the
> assessment document. Given that several of those metrics are not defensible at our frame
> rate anyway (`docs/03-module2-attention-gaze.md` §4.2), the narrow schema is arguably
> correct, but it is an undocumented asymmetry.

**`vitamin_d_records`**

| Attribute | Type | Required |
|---|---|---|
| `child_id` | string(64) | yes |
| `score` | integer | no |
| `band` | string(16) | no |
| `factors_json` | string(4096) | no |
| `record_date` | integer | no |
| `owner_id` | string(64) | no |

---

## 4. The sync path

`data/remote/AppwriteSyncRepository.kt`.

**Document ids.** The local UUID is reused as the Appwrite document id, so sync is
idempotent — re-syncing overwrites rather than duplicating. Sub-records derived from a
session get a suffix: `<sessionId>_growth`, `_attn`, `_vitd`. Ids are trimmed to
Appwrite's 36-character limit; the current server accepted 41, but the guard keeps it safe
and the ids stay stable.

**`upsert`** attempts `createDocument`, and falls back to `updateDocument` **only** on a
genuine 409 / `document_already_exists` — not on any failure, which is what masked the
permission bug in §6.1.

**`Outcome`** carries the collection, the document id, the verbatim Appwrite message and
code, and a transient/permanent classification. Each record is attempted independently so
one bad document never blocks the rest, and the count of failures plus the first real
error reach the UI:

```
2 of 7 failed — growth_records/<id>: Missing required attribute (code 400)
— the schema is out of date; run appwrite/schema.ps1
```

**Local data is never deleted on sync failure.** The store is authoritative; the server is
a replica.

### 4.1 Background scheduling

`data/sync/SyncWorker.kt` (WorkManager) and `NetworkMonitor.kt`:

| Aspect | Value |
|---|---|
| Periodic backstop | every 15 minutes, `ExistingPeriodicWorkPolicy.KEEP` |
| Constraint | network connected |
| One-shot backoff | exponential from 15 s |
| Periodic backoff | exponential from 30 s |
| Max attempts | 5, then `Result.failure()` |
| Retry condition | **only** when `report.retryable` — network, 429, 5xx |

A schema or permission error returns `Result.failure()` immediately rather than retrying.
That matters on a field device: a permanent error retried with exponential backoff burns
battery for hours and can never succeed. Transient errors retry; permanent errors stop and
surface.

---

## 5. Field mapping

| Kotlin (`data/model/Models.kt`) | Collection · attribute |
|---|---|
| `ChildProfile.id` | `children.child_id` |
| `ChildProfile.sex` | `children.gender` |
| `ChildProfile.dateOfBirthEpochDay` | `children.date_of_birth_epoch_day` |
| `AssessmentSession.id` | `assessments.assessment_id` |
| `AssessmentSession.startedAt` | `assessments.assessment_date` |
| `AssessmentSession.ageMonthsAtAssessment` | `assessments.age_months` |
| `AssessmentSession.answers` | `assessments.answers_json` |
| `AssessmentSession.dq?.dq` | `assessments.dq` |
| `GrowthMeasurement.dateMillis` | `growth_records.record_date` |
| `GrowthMeasurement.weightKg` | `growth_records.weight_kg` |
| `GrowthMeasurement.heightCm` | `growth_records.height_cm` |
| `GrowthMeasurement.muacMm` | `growth_records.muac_mm` |
| `AttentionResult.validFrameRatio` | `attention_records.valid_frame_ratio` |
| `AttentionResult.engagementIndex` | `attention_records.engagement_index` |
| `VitaminDResult.score` | `vitamin_d_records.score` |
| `UserProfile.id` | `profiles.user_id` |

Note the naming convention shift: Kotlin `camelCase` → Appwrite `snake_case`, and
`ChildProfile.sex` → `gender`. These are hand-maintained; there is no generated mapping
layer, so a rename on either side is a silent runtime failure until someone runs the
client↔schema diff (§7.2).

---

## 6. Two resolved bugs worth reading

Both were subtle, both cost real debugging time, and both would recur under a different
name if the lesson is not written down.

### 6.1 The permission bug — every write rejected, silently

`ownerPermissions()` included `Permission.read(Role.label("admin"))`.

Appwrite only lets a client SDK grant roles the **calling session actually holds**. An
anonymous session does not hold the `admin` label, so every document write was rejected
with 401 — and `upsert()` swallowed it into `getOrDefault(false)`, so the UI only ever said
"N of M failed" with no reason.

Proven by curl against the live server: the same anonymous session, the same collection,
the only difference being the `label:admin` entry:

```
A) [read(user:ID), update(user:ID), delete(user:ID), read(label:admin)]
   → 401 {"message":"Permissions must be one of: (any, users, user:<id>, ...)",
          "type":"user_unauthorized"}

B) [read(user:ID), update(user:ID), delete(user:ID)]
   → 200 document created
```

**Fix:** documents are owner-only; admin visibility moved to collection-level permissions
in the provisioner (§3.1).

**Lessons.** A client may only grant roles it holds. And an error-swallowing default
(`getOrDefault(false)`) on a network call will cost you days — `upsert` now returns a
typed `Outcome` instead.

### 6.2 The MUAC schema bug — right value, wrong field, wrong unit

`growth_records` had only `head_circumference_cm`, left over from before MUAC replaced head
circumference as the wasting indicator. The client was writing MUAC **millimetres** into
it. Wrong name, and a field whose name asserts centimetres holding a millimetre value — a
132 mm MUAC would read back as 132 cm to anything trusting the column.

This survived because nothing ever validated the client payload against the schema. It
surfaced only when an end-to-end session write was tested for the first time:

```
400 Invalid document structure: Unknown attribute: "muac_mm"
```

**Fix:** `muac_mm` added to `schema.ps1` and the provisioner, provisioned live, client
repointed. `head_circumference_cm` is retained so documents written before the change
still read back, but nothing writes to it.

**Lesson:** a unit belongs in the attribute name, and a rename of a clinical field needs a
schema migration, not a reinterpretation of the old column.

---

## 7. Operations

### 7.1 Running the provisioner

```powershell
$env:APPWRITE_ENDPOINT   = "https://<your-appwrite-host>/v1"
$env:APPWRITE_PROJECT_ID = "<your-project-id>"
$env:APPWRITE_API_KEY    = "<admin key — never commit, never log>"
pwsh -File appwrite/schema.ps1
```

Required key scopes: `databases.read`, `collections.read`, `collections.write`,
`documents.read`, `documents.write`. A key missing any of them fails with
`missing scopes (["collections.write"])`.

### 7.2 Verifying the client matches the schema

The check that would have caught §6.2. For each collection, diff the keys the client sends
against the server's attributes, **both directions** — keys the client sends that the
server lacks (a 400 on every write), and required attributes the client never sends (also a
400):

```
assessments OK · attention_records OK · children OK
growth_records OK · profiles OK · vitamin_d_records OK
All six collections match the client payloads.
```

Run this after any model or schema change. It is a few minutes of scripting against
`GET /databases/{db}/collections/{id}/attributes` and pays for itself the first time.

### 7.3 End-to-end test that actually proves sync works

Create an anonymous session over HTTPS, then create one realistic document in each of the
six collections using **only that session cookie** — no API key — with the owner-only
permission set the app uses. Then delete them. An API-keyed test proves nothing about
whether the app can sync, because the key bypasses exactly the permission logic that was
broken in §6.1.

### 7.4 Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Sync silently does nothing, no errors | `APPWRITE_ENABLED` false | Check generated `BuildConfig`; set `poshan.appwrite.enabled=true` |
| `Permissions must be one of: (any, users, user:…)` | Client granting a role the session lacks | Grant only owner roles; move supervisor access to collection level |
| `Invalid document structure: Unknown attribute "x"` | Client ahead of schema | Run the provisioner; then §7.2 |
| `Missing required attribute` | Schema ahead of client, or a genuinely absent value | Make it optional, or send it |
| `missing scopes ([...])` | API key lacks scopes | Add scopes in the console, or issue a new key |
| `Could not create a session` | Anonymous auth disabled, or wrong project id | Check Auth settings; verify project id |
| `document_already_exists` (409) | Expected — `upsert` falls back to update | No action |
| Writes fail only on release builds | Release signing fingerprint not registered as an Appwrite Android platform | Register the release SHA-1/SHA-256 |

### 7.5 Security notes

* No API key is ever compiled into the app. The client authenticates as an anonymous
  session user and holds only owner permissions on its own documents.
* Endpoint and project id ship inside the APK in `BuildConfig`; they are not secrets.
* The admin key is a workstation/CI credential only. **A key pasted into a chat, a log or
  a doc must be rotated.**
* These are children's health records. Document-level ACLs plus `documentSecurity=true`
  are what keep one caregiver's device from reading another's data — do not weaken either
  for convenience.
