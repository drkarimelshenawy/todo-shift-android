package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    val securityStore = remember {
        V11SecurityStore(
            context.applicationContext
        )
    }

    var securitySettings by remember {
        mutableStateOf(
            securityStore.load()
        )
    }

    var accountsUnlocked by remember {
        mutableStateOf(
            !securitySettings.protectAccounts ||
                securitySettings.mode ==
                    V11LockMode.NONE
        )
    }

    var backgroundAt by remember {
        mutableLongStateOf(0L)
    }

    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(
        lifecycleOwner,
        securitySettings
    ) {
        val observer =
            LifecycleEventObserver {
                _,
                event ->

                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        backgroundAt =
                            System.currentTimeMillis()

                        if (
                            securitySettings
                                .protectAccounts &&
                            securitySettings
                                .autoLockMinutes == 0
                        ) {
                            accountsUnlocked =
                                false
                        }
                    }

                    Lifecycle.Event.ON_START -> {
                        if (
                            securitySettings
                                .protectAccounts &&
                            securitySettings
                                .mode !=
                                V11LockMode.NONE &&
                            backgroundAt > 0L
                        ) {
                            val limit =
                                securitySettings
                                    .autoLockMinutes *
                                    60_000L

                            if (
                                securitySettings
                                    .autoLockMinutes == 0 ||
                                System
                                    .currentTimeMillis() -
                                    backgroundAt >=
                                    limit
                            ) {
                                accountsUnlocked =
                                    false
                            }
                        }
                    }

                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle
            .addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }

    var db by remember {
        mutableStateOf(runtime.start())
    }

    var oldData by remember {
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

    var showWorkplaces by remember {
        mutableStateOf(false)
    }

    val launchDate =
        initialDate ?: LocalDate.now()

    var month by remember(launchDate) {
        mutableStateOf(
            YearMonth.from(launchDate)
        )
    }

    var moneyVisible by remember {
        mutableStateOf(
            !securitySettings.hideAmounts
        )
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
                        Text("الحسابات")
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
                                .Settings,
                            null
                        )
                    },
                    label = {
                        Text("الإعدادات")
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
            if (showWorkplaces) {
                V11WorkplacesScreen(
                    workplaces = oldData.workplaces,
                    onBack = {
                        showWorkplaces = false
                    },
                    onSaveWorkplaces = { workplaces ->
                        val updated =
                            oldData.copy(
                                workplaces = workplaces
                            )
                        oldStorage.save(updated)
                        oldData = updated
                    }
                )
            } else {
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
                    if (
                        securitySettings
                            .protectAccounts &&
                        securitySettings.mode !=
                            V11LockMode.NONE &&
                        !accountsUnlocked
                    ) {
                        V11AccountsUnlockScreen(
                            mode =
                                securitySettings.mode,
                            securityStore =
                                securityStore,
                            onUnlocked = {
                                accountsUnlocked =
                                    true
                            }
                        )
                    } else {
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
                    }

                else ->
                    V11MoreScreen(
                        db = db,
                        uiSettings =
                            uiSettings,
                        moneyVisible =
                            moneyVisible,
                        securitySettings =
                            securitySettings,
                        securityStore =
                            securityStore,
                        onSecuritySettingsChange = {
                            securitySettings = it

                            moneyVisible =
                                !it.hideAmounts

                            accountsUnlocked =
                                !it.protectAccounts ||
                                    it.mode ==
                                        V11LockMode.NONE
                        },
                        onUiSettingsChange = {
                            saveUi(it)
                        },
                        onToggleMoney = {
                            moneyVisible =
                                !moneyVisible
                        },
                        onOpenWorkplaces = {
                            showWorkplaces = true
                        }
                    )
                }
            }
        }
    }
}
