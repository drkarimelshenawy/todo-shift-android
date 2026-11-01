package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun V11Home(
    initialDate: LocalDate? = null
) {
    val context = LocalContext.current

    val runtime = remember {
        V11Runtime(
            context.applicationContext
        )
    }

    val oldStorage = remember {
        AppStorage(
            context.applicationContext
        )
    }

    val uiStore = remember {
        V11UiSettingsStore(
            context.applicationContext
        )
    }

    var db by remember {
        mutableStateOf(runtime.start())
    }

    val oldData by remember {
        mutableStateOf(
            oldStorage.load()
        )
    }

    var uiSettings by remember {
        mutableStateOf(
            uiStore.load()
        )
    }

    var tab by remember {
        mutableIntStateOf(0)
    }

    val launchDate =
        initialDate ?: LocalDate.now()

    var month by remember(launchDate) {
        mutableStateOf(
            YearMonth.from(launchDate)
        )
    }

    var moneyVisible by remember {
        mutableStateOf(false)
    }

    fun saveDb(value: V11Database) {
        db = value
        runtime.save(value)
    }

    fun saveUi(
        value: V11UiSettings
    ) {
        uiSettings = value
        uiStore.save(value)
    }

    Scaffold(
        containerColor =
            Color(0xFFF7F8FA),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = {
                        tab = 0
                    },
                    icon = {
                        Icon(
                            Icons.Default
                                .CalendarMonth,
                            null
                        )
                    },
                    label = {
                        Text("التقويم")
                    }
                )

                NavigationBarItem(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                    },
                    icon = {
                        Icon(
                            Icons.Default
                                .DateRange,
                            null
                        )
                    },
                    label = {
                        Text("جداولي")
                    }
                )

                NavigationBarItem(
                    selected = tab == 2,
                    onClick = {
                        tab = 2
                    },
                    icon = {
                        Icon(
                            Icons.Default
                                .Payments,
                            null
                        )
                    },
                    label = {
                        Text("المالية")
                    }
                )

                NavigationBarItem(
                    selected = tab == 3,
                    onClick = {
                        tab = 3
                    },
                    icon = {
                        Icon(
                            Icons.Default
                                .MoreHoriz,
                            null
                        )
                    },
                    label = {
                        Text("المزيد")
                    }
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 ->
                    V11CalendarScreen(
                        month = month,
                        initialDate = launchDate,
                        db = db,
                        workplaces =
                            oldData.workplaces,
                        uiSettings =
                            uiSettings,
                        onMonthChange = {
                            month = it
                        },
                        onDatabaseChange = {
                            saveDb(it)
                        },
                        onUiSettingsChange = {
                            saveUi(it)
                        }
                    )

                1 ->
                    V11SchedulesScreen(
                        month = month,
                        db = db,
                        workplaces =
                            oldData.workplaces,
                        onDatabaseChange = {
                            saveDb(it)
                        }
                    )

                2 ->
                    V11FinanceScreen(
                        month = month,
                        db = db,
                        workplaces =
                            oldData.workplaces,
                        moneyVisible =
                            moneyVisible,
                        onPrevious = {
                            month =
                                month.minusMonths(1)
                        },
                        onNext = {
                            month =
                                month.plusMonths(1)
                        },
                        onToggleMoney = {
                            moneyVisible =
                                !moneyVisible
                        },
                        onDatabaseChange = {
                            saveDb(it)
                        }
                    )

                else ->
                    V11MoreScreen(
                        db = db,
                        uiSettings =
                            uiSettings,
                        moneyVisible =
                            moneyVisible,
                        onUiSettingsChange = {
                            saveUi(it)
                        },
                        onToggleMoney = {
                            moneyVisible =
                                !moneyVisible
                        }
                    )
            }
        }
    }
}
