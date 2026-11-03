package com.todoshift.app.v11.ui

import com.todoshift.app.widget.ToDoShiftWidget

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun V11Home(
    initialDate: LocalDate? = null,
    initialTab: Int = 0,
    initialTaskAction: String? = null,
    initialTaskId: String? = null
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

    val activeCalendarStore = remember {
        V11ActiveCalendarStore(
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
                (
                    securitySettings.mode ==
                        V11LockMode.NONE &&
                    !securitySettings.biometricEnabled
                )
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
                            (
                                securitySettings.mode !=
                                    V11LockMode.NONE ||
                                securitySettings
                                    .biometricEnabled
                            ) &&
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
                            (
                                securitySettings.mode !=
                                    V11LockMode.NONE ||
                                securitySettings
                                    .biometricEnabled
                            ) &&
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

    var activeCalendarId by remember {
        mutableStateOf(
            activeCalendarStore.load()
        )
    }

    LaunchedEffect(
        db.calendars,
        activeCalendarId
    ) {
        val valid =
            db.calendars.any {
                it.id == activeCalendarId
            }

        if (!valid) {
            val fallback =
                db.calendars
                    .sortedBy { it.order }
                    .firstOrNull()
                    ?.id

            activeCalendarId = fallback
            activeCalendarStore.save(
                fallback
            )
        }
    }

    // Repair RC7 workplace UUID mismatch and keep the V11 finance
    // rate table aligned with the workplace editor.
    LaunchedEffect(Unit) {
        val repaired =
            V11WorkplaceSync
                .repairAndSync(
                    db,
                    oldData.workplaces
                )

        if (repaired != db) {
            db = repaired
            runtime.save(repaired)
        }
    }

    var uiSettings by remember {
        mutableStateOf(
            uiStore.load()
        )
    }

    var tab by remember(initialTab) {
        mutableIntStateOf(initialTab)
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

        ToDoShiftWidget.refreshAll(
            context.applicationContext
        )
    }

    BackHandler(enabled = showWorkplaces) {
        showWorkplaces = false
    }

    // RC9: Calendar is the root/home destination.
    // Back from any main section returns to Calendar first.
    BackHandler(
        enabled = !showWorkplaces && tab != 0
    ) {
        tab = 0
    }

    val sectionTitle =
        when {
            showWorkplaces ->
                "أماكن العمل"

            tab == 0 ->
                "التقويم"

            tab == 1 ->
                "جداولي"

            tab == 2 ->
                "الحسابات"

            tab == 3 ->
                "الإعدادات"

            tab == 4 ->
                "المهام"

            else ->
                "التقويم"
        }

    V11AppThemeProvider(
        theme = uiSettings.appTheme,
        experience = uiSettings.experience,
        premiumBrightness = uiSettings.premiumBrightness
    ) {
    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,
        contentWindowInsets =
            WindowInsets(0, 0, 0, 0),
        topBar = {
            V11GlobalHeader(
                title = sectionTitle,
                showMoneyAction =
                    tab == 2 &&
                    !showWorkplaces &&
                    accountsUnlocked,
                moneyVisible = moneyVisible,
                onToggleMoney = {
                    moneyVisible = !moneyVisible
                }
            )
        },
        bottomBar = {
            if (uiSettings.experience == V11Experience.PREMIUM) {
                V11PremiumBottomBar(selectedTab = tab, onSelectTab = { destination ->
                    showWorkplaces = false
                    tab = destination
                })
            } else {
                V11HeroBottomBar(selectedTab = tab, onSelectTab = { destination ->
                    showWorkplaces = false
                    tab = destination
                })
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

                        val syncedDb =
                            V11WorkplaceSync
                                .repairAndSync(
                                    db,
                                    workplaces
                                )

                        saveDb(syncedDb)
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
                        activeCalendarId =
                            activeCalendarId,
                        onActiveCalendarChange = { id ->
                            activeCalendarId = id
                            activeCalendarStore.save(id)
                        },
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
                        (
                            securitySettings.mode !=
                                V11LockMode.NONE ||
                            securitySettings
                                .biometricEnabled
                        ) &&
                        !accountsUnlocked
                    ) {
                        V11AccountsUnlockScreen(
                            settings =
                                securitySettings,
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
                            securitySettings =
                                securitySettings,
                            securityStore =
                                securityStore,
                            onDatabaseChange = {
                                saveDb(it)
                            }
                        )
                    }

                3 ->
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
                                    (
                                        it.mode ==
                                            V11LockMode.NONE &&
                                        !it.biometricEnabled
                                    )
                        },
                        onDatabaseRestored = {
                            saveDb(it)
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

                4 ->
                    V11TasksScreen(
                        db = db,
                        workplaces = oldData.workplaces,
                        initialAction = initialTaskAction,
                        initialTaskId = initialTaskId,
                        onDatabaseChange = {
                            saveDb(it)
                        }
                    )

                else ->
                    V11CalendarScreen(
                        month = month,
                        initialDate = launchDate,
                        db = db,
                        workplaces =
                            oldData.workplaces,
                        uiSettings =
                            uiSettings,
                        activeCalendarId =
                            activeCalendarId,
                        onActiveCalendarChange = { id ->
                            activeCalendarId = id
                            activeCalendarStore.save(id)
                        },
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
                }
            }
        }
    }
}
}

@Composable
private fun V11HeroBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(82.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = "جداولي",
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                icon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null
                    )
                }
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = "المهام",
                selected = selectedTab == 4,
                onClick = { onSelectTab(4) },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                }
            )

            V11CalendarHeroDestination(
                modifier = Modifier.weight(1.15f),
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) }
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = "الحسابات",
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                icon = {
                    Box {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null
                        )

                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                        )
                    }
                }
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = "الإعدادات",
                selected = selectedTab == 3,
                onClick = { onSelectTab(3) },
                icon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun V11BottomDestination(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val color =
        if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant

    TextButton(
        modifier = modifier
            .fillMaxHeight(),
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = 2.dp,
            vertical = 6.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides color
            ) {
                Box(
                    modifier = Modifier.size(25.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight =
                    if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun V11CalendarHeroDestination(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(
                    if (selected) 54.dp else 50.dp
                ),
                shape = MaterialTheme.shapes.extraLarge,
                color =
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                shadowElevation =
                    if (selected) 7.dp else 3.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "التقويم",
                        modifier = Modifier.size(28.dp),
                        tint =
                            if (selected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme
                                    .onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                "التقويم",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun V11GlobalHeader(
    title: String,
    showMoneyAction: Boolean = false,
    moneyVisible: Boolean = true,
    onToggleMoney: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(
                    horizontal = 18.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(
                    R.drawable.todo_shift_icon_source
                ),
                contentDescription = "ToDo Shift",
                modifier = Modifier
                    .size(40.dp)
                    .clip(
                        MaterialTheme.shapes.medium
                    )
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "ToDo Shift",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Black,
                    maxLines = 1
                )

                Text(
                    title,
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (showMoneyAction) {
                IconButton(
                    onClick = onToggleMoney
                ) {
                    Icon(
                        imageVector =
                            if (moneyVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription =
                            if (moneyVisible)
                                "إخفاء المبالغ"
                            else
                                "إظهار المبالغ"
                    )
                }
            }
        }
    }
}


@Composable
private fun V11PremiumBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        listOf(
            Triple(1, "جداولي", Icons.Default.DateRange),
            Triple(4, "المهام", Icons.Default.CheckCircle),
            Triple(0, "التقويم", Icons.Default.CalendarMonth),
            Triple(2, "الحسابات", Icons.Default.Payments),
            Triple(3, "الإعدادات", Icons.Default.Settings)
        ).forEach { (index, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                alwaysShowLabel = false
            )
        }
    }
}
