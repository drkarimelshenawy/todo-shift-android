package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

/** Regression guard: a canonical owner, a reachable destination, and predictable back paths. */
class NeoSettingsHierarchyTest {
    @Test fun topLevelGroupsAreUniqueAndOrdered() {
        assertEquals(listOf("APPEARANCE", "LANGUAGE", "SECTIONS", "NOTIFICATIONS", "WIDGETS",
            "SECURITY", "DATA", "ADVANCED"), NeoSettingsHierarchy.rootGroups)
        assertEquals(NeoSettingsHierarchy.rootGroups.size, NeoSettingsHierarchy.rootGroups.distinct().size)
        assertEquals(listOf("CALENDAR", "TASKS", "NOTES", "SALARY", "CASHBOOK"), NeoSettingsHierarchy.sectionGroups)
    }

    @Test fun everyConfiguredSubpageExistsAndHasASingleParent() {
        (listOf("APPEARANCE", "CALENDAR", "TASKS", "NOTES", "SALARY", "CASHBOOK", "DATA", "ADVANCED"))
            .forEach { group ->
                NeoSettingsHierarchy.children(group).forEach { id ->
                    assertNotNull("Missing route: $id in $group", NeoFeatureCatalog.get(id))
                }
            }
        assertEquals("SECTIONS_GROUP", NeoSettingsHierarchy.parent("NOTES_GROUP"))
        assertEquals("NOTES_GROUP", NeoSettingsHierarchy.parent("NOTES_PREFERENCES"))
        assertEquals("CALENDAR_GROUP", NeoSettingsHierarchy.parent("FIRST_WEEKDAY"))
        assertEquals("APPEARANCE_GROUP", NeoSettingsHierarchy.parent("LAUNCH"))
        assertEquals("CASHBOOK_GROUP", NeoSettingsHierarchy.parent("FINANCE_DISPLAY"))
        assertEquals("ADVANCED_GROUP", NeoSettingsHierarchy.parent("ABOUT"))
        assertEquals("HOME", NeoSettingsHierarchy.parent("DATA_GROUP"))
    }

    @Test fun settingsBelongToTheCorrectOwner() {
        val owners = mapOf(
            "FINANCE_DISPLAY" to "CASHBOOK",
            "TASK_OVERVIEW" to "TASKS",
            "NOTES_PREFERENCES" to "NOTES",
            "FIRST_WEEKDAY" to "CALENDAR",
            "BACKUP" to "DATA",
            "HEALTH" to "DATA",
            "STORAGE" to "DATA",
            "SECURITY" to "SECURITY",
            "WIDGETS" to "WIDGETS",
            "LAUNCH" to "APPEARANCE",
            "LEGAL" to "ADVANCED",
        )
        owners.forEach { (route, expected) ->
            assertEquals(route, expected, NeoFeatureCatalog.get(route)?.group)
        }
    }

    @Test fun ArabicAndEnglishSearchesReachCanonicalDestinations() {
        val queryToRoute = mapOf(
            "سلة مهملات الملاحظات" to "NOTES_PREFERENCES",
            "Notes trash retention" to "NOTES_PREFERENCES",
            "أول يوم في الأسبوع" to "FIRST_WEEKDAY",
            "First weekday" to "FIRST_WEEKDAY",
            "ملخص المهام" to "TASK_OVERVIEW",
            "Finance summary in tasks" to "TASK_OVERVIEW",
            "سلامة البيانات" to "HEALTH",
            "Shift notes in schedules" to "CALENDAR_DISPLAY",
            "Backup and restore" to "BACKUP",
            "اختصارات المشغل" to "WIDGETS",
            "Launcher shortcuts" to "WIDGETS",
            "شاشة بداية التطبيق" to "LAUNCH",
            "Nile Jade" to "APPEARANCE",
        )
        queryToRoute.forEach { (query, route) ->
            assertTrue("$query -> $route", NeoSettingsSearch.search(query).any { it.destination == route })
        }
    }
}
