package com.todoshift.app.v11.ui

import java.text.Normalizer
import java.util.Locale

/**
 * Searchable settings are indexed in Arabic AND English regardless of UI language.
 * A result points to an existing settings destination; never create speculative routes.
 * When adding a setting, add its labels here along with the screen that owns it.
 */
data class NeoSettingSearchHit(
    val ar: String,
    val en: String,
    val destination: String,
    val group: String,
    val synonyms: String = "",
)

object NeoSettingsSearch {
    private fun hit(ar: String, en: String, route: String, group: String, terms: String = "") =
        NeoSettingSearchHit(ar, en, route, group, terms)

    // Concrete settings available in the current screens, not merely category labels.
    private val controls = listOf(
        hit("العرض الافتراضي", "Default calendar view", "CALENDAR_DISPLAY", "CALENDAR", "شهر اسبوع يوم month week day"),
        hit("حجم خلايا التقويم", "Calendar cell size", "CALENDAR_DISPLAY", "CALENDAR", "كثافة الخلايا تقويم calendar grid density"),
        hit("إظهار التواريخ خارج الشهر", "Outside-month dates", "CALENDAR_DISPLAY", "CALENDAR", "ايام الشهر التالي السابق"),
        hit("إظهار اسم مكان العمل", "Show workplace name", "CALENDAR_DISPLAY", "CALENDAR", "مكان الشغل اسم الشغل workplace work name label"),
        hit("أسماء الشيفتات", "Shift names", "CALENDAR_DISPLAY", "CALENDAR", "اسم الشيفت الوردية الورديات shift shifts label names"),
        hit("نص الملاحظات", "Calendar note text", "CALENDAR_DISPLAY", "CALENDAR", "ملاحظه note notes day"),
        hit("رموز الملاحظات", "Note icons", "CALENDAR_DISPLAY", "CALENDAR", "ملاحظه رموز"),
        hit("مؤشرات المهام", "Task indicators", "CALENDAR_DISPLAY", "CALENDAR", "علامة عدد مهام check tasks count"),
        hit("دليل ألوان التقاويم", "Calendar color legend", "CALENDAR_DISPLAY", "CALENDAR", "الوان التقويم مفتاح الالوان colors"),
        hit("تمييز اليوم", "Today highlight", "TODAY_STYLE", "CALENDAR", "today circle indicator اليوم الحالي"),
        hit("تمييز الغد", "Tomorrow marker", "CALENDAR_DISPLAY", "CALENDAR", "tomorrow next day"),
        hit("لون اليوم", "Today color", "TODAY_STYLE", "CALENDAR", "highlight color"),
        hit("قوة تمييز اليوم", "Today highlight intensity", "TODAY_STYLE", "CALENDAR", "highlight strength"),
        hit("سرعة النبض", "Pulse speed", "TODAY_STYLE", "CALENDAR", "animation"),
        hit("لون الشيفت", "Shift paint", "PAINT_STYLE", "CALENDAR", "الوان الشغل الوان الورديات shift colors paints workplace"),
        hit("لون مكان العمل الأصلي", "Original workplace color", "PAINT_STYLE", "CALENDAR", "restore original color gradient"),
        hit("التدرج اللوني للشيفت", "Shift gradient", "PAINT_STYLE", "CALENDAR", "لون متدرج colors gradient"),
        hit("اتجاه تدرج الشيفت", "Shift gradient direction", "PAINT_STYLE", "CALENDAR", "horizontal vertical diagonal"),
        hit("شدة تدرج الشيفت", "Shift gradient strength", "PAINT_STYLE", "CALENDAR", "smooth intensity"),
        hit("لغة التطبيق", "App language", "LANGUAGE", "LANGUAGE", "عربي انجليزي english arabic"),
        hit("اتجاه الواجهة", "UI direction", "UI_DIRECTION", "LANGUAGE", "rtl ltr left right"),
        hit("اتجاه التنقل", "Navigation direction", "NAV_DIRECTION", "LANGUAGE", "rtl ltr"),
        hit("اتجاه التقويم", "Calendar grid direction", "GRID_DIRECTION", "LANGUAGE", "rtl ltr"),
        hit("اتجاه أيام الأسبوع", "Weekday direction", "WEEKDAY_DIRECTION", "LANGUAGE", "rtl ltr"),
        hit("أول يوم في الأسبوع", "First weekday", "FIRST_WEEKDAY", "LANGUAGE", "السبت الاحد السبت sunday saturday monday"),
        hit("الهوية والإضاءة", "Theme and brightness", "APPEARANCE", "APPEARANCE", "theme brightness light dark"),
        hit("مجموعة الألوان", "Color palette", "APPEARANCE", "APPEARANCE", "الوان الخلفيه theme cash book blue navy palette"),
        hit("اللون المخصص", "Custom accent color", "APPEARANCE", "APPEARANCE", "accent rgb"),
        hit("حجم الخط", "Font size", "APPEARANCE", "APPEARANCE", "text typography readable"),
        hit("نوع الخط", "Typeface", "APPEARANCE", "APPEARANCE", "font"),
        hit("كثافة الواجهة", "UI density", "APPEARANCE", "APPEARANCE", "compact layout"),
        hit("استدارة الزوايا", "Corner radius", "APPEARANCE", "APPEARANCE", "rounded"),
        hit("شكل الأيقونات", "Icon style", "APPEARANCE", "APPEARANCE", "icons"),
        hit("أسطح شفافة", "Translucent surfaces", "APPEARANCE", "APPEARANCE", "opacity transparent glass"),
        hit("ظلال خفيفة", "Soft shadows", "APPEARANCE", "APPEARANCE", "shadow glow"),
        hit("الخلفية", "Background", "APPEARANCE", "APPEARANCE", "صورة تدرج image gradient wallpaper"),
        hit("شفافية البطاقات", "Card opacity", "APPEARANCE", "APPEARANCE", "transparency alpha"),
        hit("الانتقالات", "Transitions", "APPEARANCE", "APPEARANCE", "animation motion"),
        hit("تقليل الحركة", "Reduce motion", "APPEARANCE", "APPEARANCE", "accessibility animations"),
        hit("إظهار اسم التطبيق في الهيدر", "Header branding", "APPEARANCE", "APPEARANCE", "logo brand"),
        hit("عرض الملخص اليومي", "Day summary", "APPEARANCE", "APPEARANCE", "overview"),
        hit("عرض الشيفت القادم", "Next shift", "APPEARANCE", "APPEARANCE", "upcoming"),
        hit("ملخص المهام", "Task summary", "TASK_OVERVIEW", "ADVANCED", "tasks dashboard"),
        hit("ملخص الحسابات", "Finance summary in tasks", "TASK_OVERVIEW", "ADVANCED", "money overview"),
        hit("البطاقات الثانوية", "Secondary cards", "TASK_OVERVIEW", "ADVANCED", "dashboard"),
        hit("ملاحظات الورديات بالجداول", "Shift notes in schedules", "TASK_OVERVIEW", "ADVANCED", "notes"),
        hit("الثيم المستقل للويدجت", "Independent widget theme", "WIDGETS", "NOTIFICATIONS", "widget ويدجت"),
        hit("شفافية الويدجت", "Widget background opacity", "WIDGETS", "NOTIFICATIONS", "widget background transparent شفافية ودجت"),
        hit("تفاصيل الويدجت", "Widget extra details", "WIDGETS", "NOTIFICATIONS", "show details"),
        hit("إظهار المهام المكتملة في الويدجت", "Show completed widget tasks", "WIDGETS", "NOTIFICATIONS", "done complete tasks"),
        hit("إخفاء محتوى الويدجت", "Hide widget content", "WIDGETS", "NOTIFICATIONS", "privacy widget"),
        hit("حماية مبالغ الويدجت", "Protected widget amounts", "WIDGETS", "NOTIFICATIONS", "money privacy"),
        hit("إعدادات الخصوصية", "Security and privacy", "SECURITY", "PRIVACY", "قفل رقم سري pin lock"),
        hit("النسخ الاحتياطي", "Backup and restore", "BACKUP", "PRIVACY", "استرجاع استعادة import export"),
        hit("سلامة البيانات", "Data health", "HEALTH", "PRIVACY", "integrity"),
        hit("وضع الأداء", "Performance mode", "PERFORMANCE", "ADVANCED", "سرعة خفيف battery"),
        hit("المظهر الكلاسيكي المحفوظ", "Saved Classic style", "LEGACY", "ADVANCED", "classic original"),
        hit("الترحيب في المهام", "Greeting in tasks", "TASK_OVERVIEW", "ADVANCED", "welcome"),
        hit("فتح ملخص المهام تلقائيا", "Expand task overview", "TASK_OVERVIEW", "ADVANCED", "dashboard")
    )

    val entries: List<NeoSettingSearchHit> by lazy {
        (NeoFeatureCatalog.entries.filter { it.group.isNotEmpty() }.map {
            hit(it.ar, it.en, it.id, it.group, it.keywords)
        } + controls).distinctBy { it.ar to it.destination }
    }

    private fun normalize(value: String): String {
        val noAccents = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return noAccents.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
            .replace('ـ', ' ')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    }

    private fun typoDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 2) return 3
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    previous[j + 1] + 1, current[j] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[b.length]
    }

    fun search(query: String): List<NeoSettingSearchHit> {
        val clean = normalize(query)
        if (clean.isBlank()) return emptyList()
        val tokens = clean.split(' ').filter { it.isNotBlank() }
        return entries.mapNotNull { hit ->
            val searchable = normalize("${hit.ar} ${hit.en} ${hit.synonyms} ${hit.group}")
            val words = searchable.split(' ')
            val isPhrase = searchable.contains(clean)
            val scores = tokens.map { token ->
                when {
                    words.contains(token) -> 5
                    words.any { it.startsWith(token) || (token.length >= 3 && it.contains(token)) } -> 3
                    token.length >= 4 && words.any {
                        it.length >= 4 && typoDistance(token, it) <= if (token.length >= 7) 2 else 1
                    } -> 1
                    else -> 0
                }
            }
            if (isPhrase || scores.all { it > 0 }) {
                hit to (if (isPhrase) 100 else scores.sum())
            } else null
        }.sortedWith(compareByDescending<Pair<NeoSettingSearchHit, Int>> { it.second }
            .thenBy { it.first.ar.length })
            .map { it.first }
    }
}
