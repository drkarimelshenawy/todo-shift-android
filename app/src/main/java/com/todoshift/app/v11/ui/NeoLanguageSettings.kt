package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.todoshift.app.v11.*

internal val neoLanguageRoutes =
    setOf(
        "LANGUAGE_GROUP",
        "LANGUAGE",
        "UI_DIRECTION",
        "NAV_DIRECTION",
        "GRID_DIRECTION",
        "WEEKDAY_DIRECTION",
        "FIRST_WEEKDAY",
    )

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun NeoLanguageSettings(focus: String, onBack: () -> Unit) {
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(neoSpacing())
    ) {
        NeoPageHeader(neoLabel("اللغة والاتجاه", "Language & Direction"), onBack)
        listOf(
                "LANGUAGE",
                "UI_DIRECTION",
                "NAV_DIRECTION",
                "GRID_DIRECTION",
                "WEEKDAY_DIRECTION",
                "FIRST_WEEKDAY",
            )
            .forEach { id ->
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(focus) {
                    if (focus == id) {
                        withFrameNanos {}
                        requester.bringIntoView()
                    }
                }
                Column(Modifier.bringIntoViewRequester(requester)) {
                    when (id) {
                        "LANGUAGE" ->
                            NeoChoice(
                                neoLabel("لغة التطبيق", "App language"),
                                NeoLanguage.entries,
                                p.language,
                                { neoEnumLabel(it) },
                                { update(p.copy(language = it)) },
                            )
                        "FIRST_WEEKDAY" ->
                            NeoChoice(
                                neoLabel("أول يوم", "First weekday"),
                                listOf(6, 7, 1),
                                p.firstWeekday,
                                {
                                    when (it) {
                                        6 -> neoLabel("السبت", "Saturday")
                                        7 -> neoLabel("الأحد", "Sunday")
                                        else -> neoLabel("الاثنين", "Monday")
                                    }
                                },
                                { update(p.copy(firstWeekday = it)) },
                            )
                        else -> {
                            val title = NeoFeatureCatalog.get(id)!!
                            val value =
                                when (id) {
                                    "UI_DIRECTION" -> p.uiDirection
                                    "NAV_DIRECTION" -> p.navigationDirection
                                    "GRID_DIRECTION" -> p.gridDirection
                                    else -> p.weekdayDirection
                                }
                            NeoChoice(
                                neoLabel(title.ar, title.en),
                                NeoDirection.entries,
                                value,
                                { neoEnumLabel(it) },
                            ) { next ->
                                update(
                                    when (id) {
                                        "UI_DIRECTION" -> p.copy(uiDirection = next)
                                        "NAV_DIRECTION" -> p.copy(navigationDirection = next)
                                        "GRID_DIRECTION" -> p.copy(gridDirection = next)
                                        else -> p.copy(weekdayDirection = next)
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
    }
}
