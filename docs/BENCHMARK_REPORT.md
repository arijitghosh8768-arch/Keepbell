# Keepbell — Search Latency Benchmark Report

## 1. Executive Summary

As part of **Phase 4, Task 4.3 (Search Latency Measurement)**, a comprehensive, deterministic search benchmark and correctness suite was implemented in [`SearchLatencyBenchmarkTest.kt`](file:///d:/NotificationVault_Documentation_Pack/NotificationVault_Documentation_Pack/app/src/test/java/com/example/notificationvault/benchmark/SearchLatencyBenchmarkTest.kt).

### Core Findings
1. **Host SQLite Execution Performance:**
   - On a 10,000-record synthetic database, full table scans and substring queries on SQLite execute with mean latency of **~9.58 ms to ~19.37 ms** and P95 latency of **~8.23 ms to ~28.45 ms** on host JVM SQLite.
   - Broad queries returning 2,001 records completed with a P95 of **28.45 ms**.
   - Combined index queries (Package + Date + Title Query) completed with a P95 of **8.23 ms** by leveraging compound indexes (`(packageName, capturedAtMs)`).
2. **Distinction from Physical Device Target:**
   - **PRD NFR-03** specifies a local search target of **< 300 ms for 10,000 simple records on a mid-range physical test device**.
   - These host JVM/Robolectric benchmark figures comfortably pass the host criteria (< 300 ms). However, **host JVM results DO NOT guarantee physical-device performance**, as mobile devices experience flash storage write-amplification, slower random I/O, SQLite page-cache limits, and thermal throttling. Physical-device validation remains strictly deferred until Phase 5.
3. **Architecture Decision on FTS vs. Standard SQLite LIKE:**
   - Because standard SQLite query latency over 10,000 records is well within the acceptable threshold, **introducing SQLite FTS (Full-Text Search) is NOT currently recommended or required**.
   - Avoiding FTS preserves schema simplicity, eliminates FTS shadow table sync/trigger overhead during high-volume notification capture, and prevents Room migration risks.

---

## 2. Dataset Specification & Determinism

The dataset was generated synthetically via [`generateDeterministicDataset`](file:///d:/NotificationVault_Documentation_Pack/NotificationVault_Documentation_Pack/app/src/test/java/com/example/notificationvault/benchmark/SearchLatencyBenchmarkTest.kt) using a fixed random seed (`SEED = 424242L`). No real or personal notification content is ever used or logged.

### Dataset Distribution
- **Dataset Size:** 10,000 records (`COUNT_BENCHMARK = 10,000`).
- **Package Distribution:** 8 representative package names evenly rotated (`com.google.android.gm`, `com.whatsapp`, `com.slack`, `com.twitter.android`, `org.telegram.messenger`, `com.bank.secure`, `com.shopping.deals`, `com.calendar.events`).
- **App Labels:** 8 corresponding human-readable labels (`Gmail`, `WhatsApp`, `Slack`, `X`, `Telegram`, `SecureBank`, `SuperShop`, `Calendar`).
- **Titles & Bodies:** Representative lengths (Title: 15–40 chars; Body: 60–150 chars).
- **Bookmarks & Read Flags:** Injected deterministically (5% bookmarked, 33% read).
- **Injected Sentinel Verification Targets:**
  - Index 100: Title sentinel (`"Special Project Alpha Kickoff"`)
  - Index 200: Body sentinel (`"Secret verification code is 987654."`)
  - Index 300: Label sentinel (`"SpecialFinancePortal"`)
  - Index 400: Literal `%` wildcard item (`"Promo: Get 50% discount today!"`)
  - Index 500: Literal `_` wildcard item (`"File report_q3_summary uploaded"`)

---

## 3. Benchmark Methodology

- **Measurement Separation:**
  - **SQLite Engine / Room DAO Latency:** Measured directly using high-precision nano timestamps (`kotlin.system.measureNanoTime`).
  - **UI Debounce Delay (300 ms):** Handled in `InboxViewModel` via Kotlin Coroutines `debounce(300ms)` and **excluded** from DAO execution measurements.
- **Warm-Up Phase:**
  - 3 unmeasured warm-up iterations executed prior to recorded runs to ensure JIT warmup and SQLite connection pool initialization.
- **Measurement Runs:**
  - 20 timed executions per query type ($N = 20$).
  - Warm-up executions are strictly excluded from statistical results.

---

## 4. Benchmark Results (10,000 Records)

Measured on Windows 11 host JVM SQLite (Robolectric test runtime):

| Query Scenario | Description / Target | Sample Count ($N$) | Min (ms) | Median (ms) | Mean (ms) | P95 (ms) | Max (ms) | Hits |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Full Table Scan (Non-matching)** | Worst-case scan through 10,000 rows (`"NonExistentToken999XYZ"`) | 20 | 9.84 | 10.90 | 10.81 | 13.23 | 13.23 | 0 |
| **Body Substring Match** | Specific search target in body text (`"verification code is 987654"`) | 20 | 9.20 | 10.19 | 10.53 | 13.37 | 13.37 | 1 |
| **Title Match** | Specific search target in title (`"Special Project Alpha"`) | 20 | 9.04 | 9.41 | 9.58 | 10.78 | 10.78 | 1 |
| **Broad Term Match** | Broad term matching across multiple records (`"summary"`) | 20 | 16.80 | 19.02 | 19.37 | 28.45 | 28.45 | 2,001 |
| **Intersecting Filter** | Combined Package + Date boundaries + Query (`"Alpha"`) | 20 | 4.46 | 5.18 | 5.35 | 8.23 | 8.23 | 1 |

---

## 5. Search Correctness & Safety Verification

All test assertions passed 100%:
1. **Case-Insensitivity:**
   - Both `"SPECIAL PROJECT ALPHA"` and `"special project alpha"` matched expected sentinel record `sentinel_title_match`.
   - Case-insensitive search on body and app labels verified.
2. **Filter Intersection:**
   - When package filter, date range, and search text are provided together, only records satisfying **all three criteria** are returned.
   - Queries with mismatched package or out-of-bounds timestamp return empty results as expected.
3. **SQL Wildcard & Injection Safety (TC-017):**
   - Implemented documented escaping in `NotificationRepositoryImpl.escapeSqlLikeWildcards` paired with SQLite `ESCAPE '\'` in `NotificationDao`.
   - Literal `%` search (`"50%"`) returns only the literal promo item (`"sentinel_wildcard_percent"`), and does not match arbitrary records.
   - Literal `_` search (`"report_q3"`) matches only the exact item with an underscore (`"sentinel_wildcard_underscore"`), avoiding false single-character wildcard matches.
   - Searching for the escape backslash itself (`\` e.g. `"C:\Users"`) is safely escaped and matched.
   - Raw SQL injection payloads (`"' OR '1'='1"`) are safely bound via Room parameters without query alteration or injection vulnerability.

---

## 6. Limitations & Physical Device Testing Notice

- **Host vs. Physical Device:**
  - JVM SQLite execution runs on host SSD storage and desktop multi-core CPU.
  - Mid-range Android devices use UFS/eMMC storage with mobile background thermal throttling.
  - **Status:** **The physical-device 300 ms target remains UNVERIFIED** until the final comprehensive physical-device testing phase.
