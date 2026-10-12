package com.example.notificationvault.benchmark

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.NotificationDao
import com.example.notificationvault.data.local.NotificationEntity
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Benchmark and search correctness suite for Keepbell.
 *
 * Implements Phase 4, Task 4.3:
 * - Deterministic synthetic dataset (1,000 and 10,000 records) with fixed random seed.
 * - Rigorous search correctness checks (case-insensitivity, filter intersection, parameterization).
 * - SQL wildcard behavior verification ('%' and '_') based on TC-017 / PRD.
 * - Timed search latency measurement (excluding warm-up iterations).
 * - Explicit separation between SQLite DAO execution latency and UI debounce (300ms).
 * - Does not log or use personal notification content.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchLatencyBenchmarkTest {

    private lateinit var context: Context
    private val testDbName = "benchmark_search_vault.db"
    private lateinit var database: VaultDatabase
    private lateinit var dao: NotificationDao
    private lateinit var repository: NotificationRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    companion object {
        const val SEED = 424242L
        const val COUNT_QUICK = 1_000
        const val COUNT_BENCHMARK = 10_000

        val SAMPLE_PACKAGES = listOf(
            "com.google.android.gm",
            "com.whatsapp",
            "com.slack",
            "com.twitter.android",
            "org.telegram.messenger",
            "com.bank.secure",
            "com.shopping.deals",
            "com.calendar.events"
        )

        val SAMPLE_APP_LABELS = mapOf(
            "com.google.android.gm" to "Gmail",
            "com.whatsapp" to "WhatsApp",
            "com.slack" to "Slack",
            "com.twitter.android" to "X",
            "org.telegram.messenger" to "Telegram",
            "com.bank.secure" to "SecureBank",
            "com.shopping.deals" to "SuperShop",
            "com.calendar.events" to "Calendar"
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
        database = Room.databaseBuilder(context, VaultDatabase::class.java, testDbName)
            .allowMainThreadQueries()
            .build()
        dao = database.notificationDao()
        repository = NotificationRepositoryImpl(dao, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(testDbName)
        Dispatchers.resetMain()
    }

    /**
     * Generates a deterministic list of [NotificationEntity] records.
     * Specific known sentinel records are injected at fixed indices for correctness assertions.
     */
    private fun generateDeterministicDataset(count: Int, seed: Long = SEED): List<NotificationEntity> {
        val random = Random(seed)
        val list = ArrayList<NotificationEntity>(count)

        for (i in 0 until count) {
            val pkg = SAMPLE_PACKAGES[i % SAMPLE_PACKAGES.size]
            val label = SAMPLE_APP_LABELS[pkg] ?: "App"
            val baseTime = 1_700_000_000_000L + (i * 10_000L)

            val title = when (i % 5) {
                0 -> "Security Alert #$i"
                1 -> "New message from colleague $i"
                2 -> "Your weekly summary is ready"
                3 -> "Meeting reminder: standup #$i"
                else -> "Transaction notice ref_$i"
            }

            val body = "Detailed notification content for event index $i with tracking token tok_${random.nextInt(100000, 999999)}."

            list.add(
                NotificationEntity(
                    id = "synth_id_${i.toString().padStart(6, '0')}",
                    packageName = pkg,
                    appLabel = label,
                    title = title,
                    body = body,
                    eventTimeMs = baseTime,
                    capturedAtMs = baseTime + 50L,
                    isBookmarked = (i % 20 == 0),
                    isRead = (i % 3 == 0)
                )
            )
        }

        // Inject Known Target Sentinel Records for Deterministic Verification
        // Target 1: Title query target
        list[100] = list[100].copy(
            id = "sentinel_title_match",
            title = "Special Project Alpha Kickoff",
            body = "Please review kickoff slide deck."
        )

        // Target 2: Body query target
        list[200] = list[200].copy(
            id = "sentinel_body_match",
            title = "Notification Status",
            body = "Secret verification code is 987654."
        )

        // Target 3: App label target
        list[300] = list[300].copy(
            id = "sentinel_label_match",
            appLabel = "SpecialFinancePortal",
            title = "Monthly Statement",
            body = "Account ledger available."
        )

        // Target 4: Wildcard test item containing literal '%' and '_'
        list[400] = list[400].copy(
            id = "sentinel_wildcard_percent",
            title = "Promo: Get 50% discount today!",
            body = "Discount voucher code_promo_2026."
        )

        list[500] = list[500].copy(
            id = "sentinel_wildcard_underscore",
            title = "File report_q3_summary uploaded",
            body = "Detailed report_data."
        )

        return list
    }

    data class LatencySummary(
        val queryName: String,
        val sampleCount: Int,
        val minMs: Double,
        val medianMs: Double,
        val meanMs: Double,
        val p95Ms: Double,
        val maxMs: Double,
        val resultCount: Int
    )

    private fun benchmarkQuery(
        name: String,
        warmupIterations: Int = 3,
        measuredIterations: Int = 20,
        queryExecution: () -> Int
    ): LatencySummary {
        // Warm-up phase (excluded from statistical results)
        repeat(warmupIterations) {
            queryExecution()
        }

        val samples = DoubleArray(measuredIterations)
        var lastResultCount = 0

        for (i in 0 until measuredIterations) {
            val nanos = measureNanoTime {
                lastResultCount = queryExecution()
            }
            samples[i] = nanos / 1_000_000.0
        }

        samples.sort()
        val min = samples.first()
        val max = samples.last()
        val median = samples[samples.size / 2]
        val mean = samples.average()
        val p95 = samples[((samples.size * 95) / 100).coerceAtMost(samples.size - 1)]

        return LatencySummary(
            queryName = name,
            sampleCount = measuredIterations,
            minMs = min,
            medianMs = median,
            meanMs = mean,
            p95Ms = p95,
            maxMs = max,
            resultCount = lastResultCount
        )
    }

    @Test
    fun searchCorrectness_caseInsensitiveAndIntersectingFilters() = runBlocking {
        val dataset = generateDeterministicDataset(COUNT_QUICK)
        dao.insertAll(dataset)

        // 1. Case-insensitive title match
        val titleUpper = repository.filterNotifications(query = "SPECIAL PROJECT ALPHA").first()
        val titleLower = repository.filterNotifications(query = "special project alpha").first()
        assertEquals(1, titleUpper.size)
        assertEquals(1, titleLower.size)
        assertEquals("sentinel_title_match", titleUpper[0].id)
        assertEquals("sentinel_title_match", titleLower[0].id)

        // 2. Case-insensitive body match
        val bodyMatches = repository.filterNotifications(query = "verification code is 987654").first()
        assertEquals(1, bodyMatches.size)
        assertEquals("sentinel_body_match", bodyMatches[0].id)

        // 3. Case-insensitive app label match
        val labelMatches = repository.filterNotifications(query = "specialfinanceportal").first()
        assertEquals(1, labelMatches.size)
        assertEquals("sentinel_label_match", labelMatches[0].id)

        // 4. Non-matching query
        val nonMatching = repository.filterNotifications(query = "NonExistentToken999XYZ").first()
        assertTrue("Non-matching query should return 0 results", nonMatching.isEmpty())

        // 5. Intersecting Package + Date + Query
        val target = dataset[100]
        val intersectingMatch = repository.filterNotifications(
            packageName = target.packageName,
            startTimeMs = target.capturedAtMs - 1000L,
            endTimeMs = target.capturedAtMs + 1000L,
            query = "Alpha Kickoff"
        ).first()
        assertEquals(1, intersectingMatch.size)
        assertEquals("sentinel_title_match", intersectingMatch[0].id)

        // Filter mismatch: Wrong package should yield 0 results even if query matches
        val mismatchedPackage = repository.filterNotifications(
            packageName = "com.unrelated.app",
            query = "Alpha Kickoff"
        ).first()
        assertTrue("Mismatched package must yield empty results", mismatchedPackage.isEmpty())

        // Filter mismatch: Date out of range
        val mismatchedDate = repository.filterNotifications(
            packageName = target.packageName,
            startTimeMs = target.capturedAtMs + 100_000L,
            query = "Alpha Kickoff"
        ).first()
        assertTrue("Date out of range must yield empty results", mismatchedDate.isEmpty())
    }

    @Test
    fun searchCorrectness_sqlWildcardsTreatedSafely() = runBlocking {
        val dataset = generateDeterministicDataset(COUNT_QUICK)
        dao.insertAll(dataset)

        // 1. Querying for literal "50%": must match sentinel_wildcard_percent only, NOT other items
        val percentResults = repository.filterNotifications(query = "50%").first()
        assertEquals(1, percentResults.size)
        assertEquals("sentinel_wildcard_percent", percentResults[0].id)

        // Negative check: query for "50% arbitrary" should NOT match if % is literal
        val percentNegative = repository.filterNotifications(query = "50% arbitrary").first()
        assertTrue(percentNegative.isEmpty())

        // 2. Querying for literal "report_q3": must match sentinel_wildcard_underscore only
        val underscoreResults = repository.filterNotifications(query = "report_q3").first()
        assertEquals(1, underscoreResults.size)
        assertEquals("sentinel_wildcard_underscore", underscoreResults[0].id)

        // Negative check: query "reportXq3" should NOT match "report_q3" when underscore is escaped
        val underscoreNegative = repository.filterNotifications(query = "reportXq3").first()
        assertTrue(underscoreNegative.isEmpty())

        // 3. Querying for escape character itself: '\'
        val backslashItem = NotificationEntity(
            id = "sentinel_backslash",
            packageName = "com.dev.app",
            title = "Path C:\\Users\\User",
            body = "Log output in C:\\Logs",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        dao.insert(backslashItem)

        val backslashResults = repository.filterNotifications(query = "C:\\Users").first()
        assertEquals(1, backslashResults.size)
        assertEquals("sentinel_backslash", backslashResults[0].id)

        // 4. Parameterized query security: raw SQL injection strings must not throw or alter query structure
        val injectionString = "' OR '1'='1"
        val injectionResults = repository.filterNotifications(query = injectionString).first()
        assertTrue("Parameterized query prevents injection; no match expected", injectionResults.isEmpty())
    }

    @Test
    fun benchmark_10000Records_measuresSearchLatencySeparatelyFromDebounce() = runBlocking {
        // Populate 10,000 deterministic records in batches to avoid SQLite variable limit
        val dataset = generateDeterministicDataset(COUNT_BENCHMARK)
        val batchSize = 1000
        for (i in 0 until COUNT_BENCHMARK step batchSize) {
            val chunk = dataset.subList(i, (i + batchSize).coerceAtMost(COUNT_BENCHMARK))
            dao.insertAll(chunk)
        }

        val totalCount = dao.getCount().first()
        assertEquals(COUNT_BENCHMARK, totalCount)

        // Run measurements across diverse query types
        val measurements = mutableListOf<LatencySummary>()

        // 1. Full scan (non-matching query)
        val nonMatchingSummary = benchmarkQuery("Full Table Scan (Non-matching)") {
            runBlocking { dao.filterCombined(query = "NonExistentToken999XYZ").first().size }
        }
        measurements.add(nonMatchingSummary)
        assertEquals(0, nonMatchingSummary.resultCount)

        // 2. Body substring match (selectivity ~1/10000)
        val bodySummary = benchmarkQuery("Body Substring Match (Specific)") {
            runBlocking { dao.filterCombined(query = "verification code is 987654").first().size }
        }
        measurements.add(bodySummary)
        assertEquals(1, bodySummary.resultCount)

        // 3. Title prefix match
        val titleSummary = benchmarkQuery("Title Match (Specific)") {
            runBlocking { dao.filterCombined(query = "Special Project Alpha").first().size }
        }
        measurements.add(titleSummary)
        assertEquals(1, titleSummary.resultCount)

        // 4. Common term match (broad selectivity)
        val broadSummary = benchmarkQuery("Broad Term Match ('summary')") {
            runBlocking { dao.filterCombined(query = "summary").first().size }
        }
        measurements.add(broadSummary)
        assertTrue("Broad term should match multiple items", broadSummary.resultCount > 100)

        // 5. Intersecting search: package + date + query
        val target = dataset[100]
        val intersectingSummary = benchmarkQuery("Intersecting (Package + Date + Query)") {
            runBlocking {
                dao.filterCombined(
                    packageName = target.packageName,
                    startTimeMs = target.capturedAtMs - 10_000L,
                    endTimeMs = target.capturedAtMs + 10_000L,
                    query = "Alpha"
                ).first().size
            }
        }
        measurements.add(intersectingSummary)
        assertEquals(1, intersectingSummary.resultCount)

        // Print benchmark table to standard output for report documentation
        println("\n================ SEARCH LATENCY BENCHMARK (10,000 RECORDS) ================")
        println(String.format("%-36s | %5s | %7s | %7s | %7s | %7s | %7s | %6s", "Query Type", "N", "Min(ms)", "Med(ms)", "Mean(ms)", "P95(ms)", "Max(ms)", "Hits"))
        println("-".repeat(95))
        for (m in measurements) {
            println(
                String.format(
                    "%-36s | %5d | %7.2f | %7.2f | %7.2f | %7.2f | %7.2f | %6d",
                    m.queryName, m.sampleCount, m.minMs, m.medianMs, m.meanMs, m.p95Ms, m.maxMs, m.resultCount
                )
            )
        }
        println("==========================================================================\n")

        // Assert JVM latency target sanity: on host SQLite, queries should easily complete well under 300ms
        for (m in measurements) {
            assertTrue(
                "Benchmark query '${m.queryName}' P95 (${m.p95Ms}ms) should be within 300ms on host SQLite",
                m.p95Ms < 300.0
            )
        }
    }
}
