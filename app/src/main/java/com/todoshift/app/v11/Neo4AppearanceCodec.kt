package com.todoshift.app.v11

import org.json.JSONObject

/** Versioned UI preference codec; it never serializes user records or security credentials. */
object Neo4AppearanceCodec {
    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? = runCatching { enumValueOf<T>(value) }.getOrNull()
    private fun list(o: JSONObject, key: String, fallback: List<String>): List<String> =
        o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: fallback
    private fun hex(s: String, fallback: String) = s.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: fallback
    fun encode(p: NeoAppearance): JSONObject = JSONObject().apply {
        put("formatVersion", 1)
        put("uiDirection", p.uiDirection.name)
        put("navigationDirection", p.navigationDirection.name)
        put("gridDirection", p.gridDirection.name)
        put("weekdayDirection", p.weekdayDirection.name)
        put("transition", p.transition.name)
        put("transitionSpeed", p.transitionSpeed.name)
        put("performance", p.performance.name)
        put("todayStyle", p.todayStyle.name)
        put("todaySpeed", p.todaySpeed.name)
        put("cellSize", p.cellSize.name)
        put("showNotes", p.showNotes)
        put("showNoteIcons", p.showNoteIcons)
        put("showShiftNames", p.showShiftNames)
        put("showTaskIndicators", p.showTaskIndicators)
        put("showOutsideDates", p.showOutsideDates)
        put("showTomorrow", p.showTomorrow)
        put("firstWeekday", p.firstWeekday)
        put("todayColor", p.todayColor)
        put("todayIntensity", p.todayIntensity)

        put("brightness", p.brightness.name)
        put("palette", p.palette.name)
        put("accentHex", p.accentHex)
        put("intensity", p.intensity)
        put("cardStyle", p.cardStyle.name)
        put("cornerRadius", p.cornerRadius)
        put("glass", p.glass)
        put("glow", p.glow)
        put("motion", p.motion)
        put("reduceMotion", p.reduceMotion)
        put("density", p.density.name)
        put("fontScale", p.fontScale)
        put("typeface", p.typeface.name)
        put("iconStyle", p.iconStyle.name)
        put("calendarView", p.calendarView.name)
        put("calendarDensity", p.calendarDensity.name)
        put("language", p.language.name)
        put("dashboardExpanded", p.dashboardExpanded)
        put("dashboardOrder", org.json.JSONArray(p.dashboardOrder.map { it.name }))
        put("hiddenCards", org.json.JSONArray(p.hiddenCards.map { it.name }))
        put("quickActions", org.json.JSONArray(p.quickActions.map { it.name }))
        put("showShiftNotes", p.showShiftNotes)
        put("timelineFilter", p.timelineFilter.name)
        put("showFinanceChart", p.showFinanceChart)
        put("background", p.background.name)
        put("imageTheme", p.imageTheme.name)
        put("customImageFile", p.customImageFile)
        put("imagePositionX", p.imagePositionX)
        put("imagePositionY", p.imagePositionY)
        put("imageDim", p.imageDim)
        put("layoutPreset", p.layoutPreset.name)
        put("showGreeting", p.showGreeting)
        put("showDailySummary", p.showDailySummary)
        put("showNextShift", p.showNextShift)
        put("showTaskSummary", p.showTaskSummary)
        put("showFinanceSummary", p.showFinanceSummary)
        put("showSecondaryCards", p.showSecondaryCards)
        put("showEffects", p.showEffects)
        put("showOptionalInfo", p.showOptionalInfo)
        put("showSecondaryIndicators", p.showSecondaryIndicators)
        put("showDescriptions", p.showDescriptions)
        put("showShiftPaint", p.showShiftPaint)
        put("showCalendarLegend", p.showCalendarLegend)
        put("showBrand", p.showBrand)
        put("cardOpacity", p.cardOpacity)
        put("glassStrength", p.glassStrength)
        put("optionalTransitions", p.optionalTransitions)
        put("shiftPaints", org.json.JSONObject().apply { p.shiftPaints.forEach { (kind, paint) -> put(kind.name, org.json.JSONObject().put("mode", paint.mode.name).put("first", paint.first).put("second", paint.second).put("direction", paint.direction.name).put("strength", paint.strength.name)) } })
    }
    fun decode(o: JSONObject, defaults: NeoAppearance = NeoAppearance()): NeoAppearance {
        require(o.optInt("formatVersion", 1) == 1) { "Unsupported appearance format" }
        val p = NeoAppearance(
            uiDirection = enumOrNull<NeoDirection>(o.optString("uiDirection")) ?: when(enumOrNull<NeoLanguage>(o.optString("language")) ?: defaults.language) { NeoLanguage.ARABIC -> NeoDirection.RTL; NeoLanguage.ENGLISH -> NeoDirection.LTR; else -> defaults.uiDirection },
            navigationDirection = enumOrNull<NeoDirection>(o.optString("navigationDirection")) ?: defaults.navigationDirection,
            gridDirection = enumOrNull<NeoDirection>(o.optString("gridDirection")) ?: defaults.gridDirection,
            weekdayDirection = enumOrNull<NeoDirection>(o.optString("weekdayDirection")) ?: defaults.weekdayDirection,
            transition = enumOrNull<NeoTransition>(o.optString("transition")) ?: defaults.transition,
            transitionSpeed = enumOrNull<NeoSpeed>(o.optString("transitionSpeed")) ?: defaults.transitionSpeed,
            performance = enumOrNull<NeoPerformance>(o.optString("performance")) ?: defaults.performance,
            todayStyle = enumOrNull<NeoTodayStyle>(o.optString("todayStyle")) ?: defaults.todayStyle,
            todaySpeed = enumOrNull<NeoSpeed>(o.optString("todaySpeed")) ?: defaults.todaySpeed,
            cellSize = enumOrNull<NeoCellSize>(o.optString("cellSize")) ?: defaults.cellSize,
            showNotes = o.optBoolean("showNotes", defaults.showNotes),
            showNoteIcons = o.optBoolean("showNoteIcons", defaults.showNoteIcons),
            showShiftNames = o.optBoolean("showShiftNames", defaults.showShiftNames),
            showTaskIndicators = o.optBoolean("showTaskIndicators", o.optBoolean("showSecondaryIndicators", defaults.showTaskIndicators)),
            showOutsideDates = o.optBoolean("showOutsideDates", defaults.showOutsideDates),
            showTomorrow = o.optBoolean("showTomorrow", defaults.showTomorrow),
            firstWeekday = o.optInt("firstWeekday", 6).takeIf { it in listOf(6, 7, 1) } ?: 6,
            todayColor = hex(o.optString("todayColor"), defaults.todayColor),
            todayIntensity = o.optDouble("todayIntensity", .6).toFloat().takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: .6f,
            brightness = enumOrNull<NeoBrightness>(o.optString("brightness")) ?: defaults.brightness,
            palette = enumOrNull<NeoPalette>(o.optString("palette")) ?: defaults.palette,
            accentHex = o.optString("accentHex", defaults.accentHex),
            intensity = o.optDouble("intensity", defaults.intensity.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.intensity,
            cardStyle = enumOrNull<NeoCardStyle>(o.optString("cardStyle")) ?: defaults.cardStyle,
            cornerRadius = o.optInt("cornerRadius", defaults.cornerRadius),
            glass = o.optBoolean("glass", defaults.glass),
            glow = o.optBoolean("glow", defaults.glow),
            motion = o.optInt("motion", defaults.motion),
            reduceMotion = o.optBoolean("reduceMotion", defaults.reduceMotion),
            density = enumOrNull<NeoDensity>(o.optString("density")) ?: defaults.density,
            fontScale = o.optDouble("fontScale", defaults.fontScale.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.fontScale,
            typeface = enumOrNull<NeoTypeface>(o.optString("typeface")) ?: defaults.typeface,
            iconStyle = enumOrNull<NeoIconStyle>(o.optString("iconStyle")) ?: defaults.iconStyle,
            calendarView = enumOrNull<NeoCalendarView>(o.optString("calendarView")) ?: defaults.calendarView,
            calendarDensity = enumOrNull<NeoDensity>(o.optString("calendarDensity")) ?: defaults.calendarDensity,
            language = enumOrNull<NeoLanguage>(o.optString("language")) ?: defaults.language,
            dashboardExpanded = o.optBoolean("dashboardExpanded", defaults.dashboardExpanded),
            dashboardOrder = (list(o, "dashboardOrder", NeoAppearance().dashboardOrder.map { it.name }).mapNotNull { enumOrNull<NeoDashboardCard>(it) }).distinct().let { order -> order + NeoDashboardCard.entries.filterNot { it in order } },
            hiddenCards = list(o, "hiddenCards", NeoAppearance().hiddenCards.map { it.name }).mapNotNull { enumOrNull<NeoDashboardCard>(it) }.toSet(),
            quickActions = list(o, "quickActions", NeoAppearance().quickActions.map { it.name }).mapNotNull { enumOrNull<NeoQuickAction>(it) }.toSet(),
            showShiftNotes = o.optBoolean("showShiftNotes", defaults.showShiftNotes),
            timelineFilter = enumOrNull<NeoTimelineFilter>(o.optString("timelineFilter")) ?: defaults.timelineFilter,
            showFinanceChart = o.optBoolean("showFinanceChart", defaults.showFinanceChart),
            background = enumOrNull<NeoBackground>(o.optString("background")) ?: defaults.background,
            imageTheme = enumOrNull<NeoImageTheme>(o.optString("imageTheme")) ?: defaults.imageTheme,
            customImageFile = o.optString("customImageFile", defaults.customImageFile),
            imagePositionX = o.optDouble("imagePositionX", defaults.imagePositionX.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.imagePositionX,
            imagePositionY = o.optDouble("imagePositionY", defaults.imagePositionY.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.imagePositionY,
            imageDim = o.optDouble("imageDim", defaults.imageDim.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.imageDim,
            layoutPreset = enumOrNull<NeoLayoutPreset>(o.optString("layoutPreset")) ?: defaults.layoutPreset,
            showGreeting = o.optBoolean("showGreeting", defaults.showGreeting),
            showDailySummary = o.optBoolean("showDailySummary", defaults.showDailySummary),
            showNextShift = o.optBoolean("showNextShift", defaults.showNextShift),
            showTaskSummary = o.optBoolean("showTaskSummary", defaults.showTaskSummary),
            showFinanceSummary = o.optBoolean("showFinanceSummary", defaults.showFinanceSummary),
            showSecondaryCards = o.optBoolean("showSecondaryCards", defaults.showSecondaryCards),
            showEffects = o.optBoolean("showEffects", defaults.showEffects),
            showOptionalInfo = o.optBoolean("showOptionalInfo", defaults.showOptionalInfo),
            showSecondaryIndicators = o.optBoolean("showSecondaryIndicators", defaults.showSecondaryIndicators),
            showDescriptions = o.optBoolean("showDescriptions", defaults.showDescriptions),
            showShiftPaint = o.optBoolean("showShiftPaint", defaults.showShiftPaint),
            showCalendarLegend = o.optBoolean("showCalendarLegend", defaults.showCalendarLegend),
            showBrand = o.optBoolean("showBrand", defaults.showBrand),
            cardOpacity = o.optDouble("cardOpacity", defaults.cardOpacity.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.cardOpacity,
            glassStrength = o.optDouble("glassStrength", defaults.glassStrength.toDouble()).toFloat().takeIf { it.isFinite() } ?: defaults.glassStrength,
            optionalTransitions = o.optBoolean("optionalTransitions", defaults.optionalTransitions),
            shiftPaints = decodePaints(o.optJSONObject("shiftPaints"))
        )
        return p.copy(fontScale = p.fontScale.coerceIn(0.85f, 1.3f), intensity = p.intensity.coerceIn(0f, 1f),
            cornerRadius = p.cornerRadius.coerceIn(8, 32), motion = p.motion.coerceIn(0, 2), cardOpacity = p.cardOpacity.coerceIn(0.65f, 1f),
            glassStrength = p.glassStrength.coerceIn(0f, 0.6f), imageDim = p.imageDim.coerceIn(0f, 1f),
            imagePositionX = p.imagePositionX.coerceIn(0f, 1f), imagePositionY = p.imagePositionY.coerceIn(0f, 1f),
            accentHex = if (p.accentHex.isEmpty()) "" else hex(p.accentHex, ""),
            customImageFile = p.customImageFile.takeIf { it.matches(Regex("neo4-theme-[a-zA-Z0-9-]+[.]jpg")) }.orEmpty())
    }
    private fun decodePaints(o: JSONObject?): Map<ShiftKind, NeoShiftPaint> = buildMap {
        if (o != null) for (key in o.keys()) {
            val kind = enumOrNull<ShiftKind>(key) ?: continue
            val p = o.optJSONObject(key) ?: continue
            put(kind, NeoShiftPaint(enumOrNull<NeoPaintMode>(p.optString("mode")) ?: NeoPaintMode.INHERIT,
                hex(p.optString("first"), "#5BC8B2"), hex(p.optString("second"), "#4D8AE8"),
                enumOrNull<NeoGradientDirection>(p.optString("direction")) ?: NeoGradientDirection.VERTICAL,
                enumOrNull<NeoGradientStrength>(p.optString("strength")) ?: NeoGradientStrength.MEDIUM))
        }
    }
}
