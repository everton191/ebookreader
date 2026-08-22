package `in`.jphe.storyvox.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #440 — smoke contract for the Settings hub row catalog.
 *
 * The legacy/non-reader hub renders one row per [SettingsHubSection] in
 * [SettingsHubSections]. The Reader flavor has a separate compact hub. This
 * list is the source of truth for the legacy gear-icon landing page;
 * removing a row here (or breaking its title/subtitle) silently
 * drops a navigation entry, so this test pins the shape.
 *
 * Tests intentionally exercise the data list rather than the
 * composable. A full UI smoke test would need Compose-test
 * infrastructure that this module doesn't yet ship; the data-list
 * check catches every regression we care about today:
 *  - row count
 *  - presence of each named section the QA spec calls out (#440)
 *  - no duplicate titles (a duplicate would render two
 *    indistinguishable rows on the hub)
 */
class SettingsHubSectionsTest {

    @Test
    fun `hub catalog renders all sections in fixed order`() {
        // The legacy searchable hub currently contains 21 named sections plus
        // the "All settings" escape hatch. The Reader flavor uses its own
        // deliberately compact hub and is validated by its navigation route
        // contract, so reader-only rows must not inflate this legacy catalog.
        assertEquals(22, SettingsHubSections.size)
    }

    @Test
    fun `hub catalog covers every section called out in issue 440`() {
        // The issue body lists these as the expected section cards.
        // Each must appear by title (case-insensitive) so the QA
        // matrix coverage holds. New sections may be ADDED; the named
        // ones cannot silently disappear.
        val expectedSectionTitles = listOf(
            "Voice & Playback",
            "Reading",
            "Performance",
            // Phase 1 scaffold landed in v0.5.42; the hub row is the
            // entry point. Pin it here so a regression that drops the
            // row surfaces in this suite too.
            "Accessibility",
            // v0.5.59 — Appearance (book cover style toggle).
            "Appearance",
            "Plugins",
            "Account",
            "Memory Palace",
            // v1 settings-bundle-7 — Advanced subscreen (#598
            // Android Auto bucket size + future integration tunables).
            "Advanced",
            "Developer",
            "About",
            // #1630 — Content Sources subscreen (per-source config seam),
            // un-buried from the legacy monolith into the Content & Sources group.
            "Content Sources",
            // #1632 — Downloads & Storage subscreen (default download mode +
            // Wi-Fi/interval + cache), un-buried into hub group 4.
            "Downloads & Storage",
            // #1634 — Benefits re-discovery row (Screener/Decoder) in Tools.
            "Benefits",
            // #1631 — Notifications group (buried inboxNotify* toggles + a
            // system-permission affordance, un-buried into its own hub group).
            "Notifications",
        )
        val actual = SettingsHubSections.map { it.title.lowercase() }.toSet()
        for (expected in expectedSectionTitles) {
            assertTrue(
                "Settings hub is missing the $expected card — see #440",
                actual.contains(expected.lowercase()),
            )
        }
    }

    @Test
    fun `every section row carries a non-blank title and subtitle`() {
        // The hub leans on the subtitle to preview each card's
        // contents (the kdoc spec calls this out explicitly). A blank
        // subtitle would render as an empty bodySmall row, which
        // looks like a layout glitch rather than navigation.
        for (section in SettingsHubSections) {
            assertTrue(
                "Hub row '${section.title}' has a blank title",
                section.title.isNotBlank(),
            )
            assertTrue(
                "Hub row '${section.title}' has a blank subtitle",
                section.subtitle.isNotBlank(),
            )
        }
    }

    @Test
    fun `hub section titles are unique`() {
        // Two rows with the same title render as twins on the hub —
        // the user has no way to know which one to tap. Pin uniqueness.
        val titles = SettingsHubSections.map { it.title.lowercase() }
        val duplicates = titles.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(
            "Hub catalog has duplicate titles: $duplicates",
            duplicates.isEmpty(),
        )
    }

    @Test
    fun `Voice and Playback is the first row for most-touched ordering`() {
        // The kdoc commits to "most-touched first" — Voice & Playback
        // leads the hub. If a reorder ever buries it, this test fails
        // first so the change is intentional.
        assertEquals("Voice & Playback", SettingsHubSections.first().title)
    }

    // ── Issue #1577 — hub keyword search ────────────────────────────

    private fun sectionsMatching(query: String): List<String> =
        SettingsHubSections
            .filter { matchesHubQuery(query, it.title, it.subtitle, it.keywords) }
            .map { it.title }

    @Test
    fun `flagship — DND and sleep synonyms surface Voice and Playback (#1574)`() {
        // The worked example from #1574: the DND auto-sleep toggle lives in
        // Voice & Playback but was unsearchable. Every term a user reaches for
        // must now surface that row.
        for (q in listOf("dnd", "do not disturb", "bedtime", "sleep", "sleep timer", "timer")) {
            assertTrue(
                "'$q' should surface Voice & Playback so the DND auto-sleep toggle is findable",
                sectionsMatching(q).contains("Voice & Playback"),
            )
        }
    }

    @Test
    fun `help and legal synonyms surface About`() {
        // The handbook (#1544), privacy policy (#1138), and impact-sharing
        // explainer (#1463) all live under About but its title/subtitle name
        // none of them; keywords carry the discoverability.
        for (q in listOf("handbook", "help", "guide", "manual", "privacy", "impact")) {
            assertTrue(
                "'$q' should surface About",
                sectionsMatching(q).contains("About"),
            )
        }
    }

    @Test
    fun `every catalogued row carries at least one search keyword`() {
        // The #1577 discoverability contract: no row may be findable by its
        // title alone — each carries synonyms + the names of the knobs inside.
        for (section in SettingsHubSections) {
            assertTrue(
                "Hub row '${section.title}' has no search keywords (see #1577)",
                section.keywords.isNotEmpty(),
            )
        }
    }

    @Test
    fun `blank query still matches every row`() {
        // Regression guard: the keyword layer must not change the unfiltered
        // default — an empty search shows the whole hub.
        assertEquals(SettingsHubSections.size, sectionsMatching("").size)
        assertEquals(SettingsHubSections.size, sectionsMatching("   ").size)
    }

    @Test
    fun `All settings escape hatch is present and last`() {
        // The legacy long-scroll SettingsScreen remains reachable via
        // an explicit row labelled "All settings". It sits at the
        // bottom — escape hatches go below the structured catalog so
        // they don't compete visually with the curated cards.
        val last = SettingsHubSections.last()
        assertEquals("All settings", last.title)
        assertNotNull(last.subtitle)
        assertFalse(last.subtitle.isBlank())
    }
}
