package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun V11Home(
    initialDate: LocalDate? = null,
    initialTab: Int = 0,
    initialTaskAction: String? = null,
    initialTaskId: String? = null,
) {
    val context = LocalContext.current

    val runtime = remember { V11Runtime(context.applicationContext) }

    val oldStorage = remember { AppStorage(context.applicationContext) }

    val uiStore = remember { V11UiSettingsStore(context.applicationContext) }

    val securityStore = remember { V11SecurityStore(context.applicationContext) }

    val activeCalendarStore = remember { V11ActiveCalendarStore(context.applicationContext) }

    val writeScope = rememberCoroutineScope()
    var writeBusy by remember { mutableStateOf(false) }
    var writeFailure by remember { mutableStateOf<String?>(null) }
    var recoveryOpen by remember { mutableStateOf(false) }
    var bootAttempt by remember { mutableIntStateOf(0) }
    var bootFailure by remember { mutableStateOf(false) }
    var boot by
        remember(bootAttempt) { mutableStateOf<com.todoshift.app.backup.Neo4BootState?>(null) }
    LaunchedEffect(bootAttempt) {
        bootFailure = false
        try {
            boot =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    NeoSmartStore(context.applicationContext).snapshot()
                    com.todoshift.app.backup.Neo4BootState(
                        runtime.start(),
                        oldStorage.load(),
                        uiStore.load(),
                        NeoAppearanceStore(context.applicationContext).load(),
                        securityStore.load(),
                        activeCalendarStore.load(),
                    )
                }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            bootFailure = true
        }
    }
    if (recoveryOpen)
        SafeStorageRecovery(
            onDismiss = { recoveryOpen = false },
            onRecovered = {
                // Recreate all remembered state only after a successful, explicitly confirmed
                // recovery.
                (context as? android.app.Activity)?.recreate()
            },
        )
    if (writeBusy || writeFailure != null)
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties =
                androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
        ) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (writeFailure == null) {
                        CircularProgressIndicator()
                        Text("جارٍ الحفظ… / Saving…")
                    } else {
                        Text(
                            "تعذر تأكيد الحفظ / Save could not be confirmed",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "تم إيقاف التعديل لحماية البيانات. أغلق التطبيق وافتحه للتحقق؛ لا تمسح التخزين. / Editing is paused. Close and reopen to verify storage; do not clear app data."
                        )
                        Text(writeFailure!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    val loaded = boot
    if (loaded == null) {
        Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                if (bootFailure)
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("تعذر فتح البيانات بأمان / Unable to open data safely")
                        Text("لا إعادة ضبط تلقائية / No automatic data reset")
                        Button(onClick = { bootAttempt++ }) { Text("إعادة المحاولة / Retry") }
                        OutlinedButton(onClick = { recoveryOpen = true }) {
                            Text("استعادة نسخة موثوقة / Recover from backup")
                        }
                    }
                else CircularProgressIndicator()
            }
        }
        return
    }

    var securitySettings by remember { mutableStateOf(loaded.security) }

    var accountsUnlocked by remember {
        mutableStateOf(
            !securitySettings.protectAccounts ||
                (securitySettings.mode == V11LockMode.NONE && !securitySettings.biometricEnabled)
        )
    }

    DisposableEffect(securitySettings.protectAccounts) {
        val window = (context as? android.app.Activity)?.window
        if (securitySettings.protectAccounts)
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }

    var backgroundAt by remember { mutableLongStateOf(0L) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, securitySettings) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundAt = System.currentTimeMillis()

                    if (
                        securitySettings.protectAccounts &&
                            (securitySettings.mode != V11LockMode.NONE ||
                                securitySettings.biometricEnabled) &&
                            securitySettings.autoLockMinutes == 0
                    ) {
                        accountsUnlocked = false
                    }
                }

                Lifecycle.Event.ON_START -> {
                    if (
                        securitySettings.protectAccounts &&
                            (securitySettings.mode != V11LockMode.NONE ||
                                securitySettings.biometricEnabled) &&
                            backgroundAt > 0L
                    ) {
                        val limit = securitySettings.autoLockMinutes * 60_000L

                        if (
                            securitySettings.autoLockMinutes == 0 ||
                                System.currentTimeMillis() - backgroundAt >= limit
                        ) {
                            accountsUnlocked = false
                        }
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var db by remember { mutableStateOf(loaded.database) }

    var oldData by remember { mutableStateOf(loaded.legacy) }

    var activeCalendarId by remember { mutableStateOf(loaded.activeCalendarId) }

    LaunchedEffect(db.calendars, activeCalendarId) {
        val valid = db.calendars.any { it.id == activeCalendarId }

        if (!valid) {
            val fallback = db.calendars.sortedBy { it.order }.firstOrNull()?.id

            try {
                activeCalendarStore.save(fallback)
                activeCalendarId = fallback
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Unable to save primary calendar"
            }
        }
    }

    // Reachable only after BOTH stores loaded successfully. Failed reads throw into
    // the boot error screen above and never create editable state or run this effect.
    // Repair RC7 workplace UUID mismatch and keep the V11 finance rate table aligned.
    LaunchedEffect(Unit) {
        val repaired = NeoRatePolicy.reconcile(db, oldData.workplaces)

        if (repaired != db) {
            writeBusy = true
            try {
                withContext(Dispatchers.IO) { runtime.saveIfCurrent(db, repaired) }
                db = repaired
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Storage failure"
            } finally {
                writeBusy = false
            }
        }
    }

    var uiSettings by remember { mutableStateOf(loaded.ui) }

    val neoStore = remember { NeoAppearanceStore(context.applicationContext) }
    var neoAppearance by remember { mutableStateOf(loaded.appearance) }
    var pendingTaskAction by rememberSaveable { mutableStateOf(initialTaskAction) }
    var pendingTaskId by rememberSaveable { mutableStateOf(initialTaskId) }
    var modernToolsOpen by rememberSaveable { mutableStateOf(false) }
    var featureRoute by rememberSaveable { mutableStateOf("SEARCH") }
    var featureOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var temporaryFocus by remember { mutableStateOf(false) }
    var calendarAction by remember { mutableStateOf<NeoQuickAction?>(null) }
    var calendarActionId by remember { mutableIntStateOf(0) }
    var calendarManagerRequest by remember { mutableIntStateOf(0) }

    val contentAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }

    LaunchedEffect(
        tab,
        uiSettings.experience,
        neoAppearance.motion,
        neoAppearance.reduceMotion,
        neoAppearance.palette,
        neoAppearance.brightness,
        neoAppearance.accentHex,
    ) {
        if (uiSettings.experience == V11Experience.PREMIUM && neoAppearance.motionMillis > 0) {
            contentAlpha.snapTo(0.86f)
            contentAlpha.animateTo(
                1f,
                androidx.compose.animation.core.tween(neoAppearance.motionMillis),
            )
        } else contentAlpha.snapTo(1f)
    }
    var showWorkplaces by remember { mutableStateOf(false) }

    var launchDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }

    var month by remember(launchDate) { mutableStateOf(YearMonth.from(launchDate)) }

    var moneyVisible by remember { mutableStateOf(!securitySettings.hideAmounts) }

    LaunchedEffect(moneyVisible, securitySettings.protectAccounts, securitySettings.hideAmounts) {
        com.todoshift.app.widget.Neo4WidgetPrivacy.update(
            context.applicationContext,
            !moneyVisible || securitySettings.protectAccounts || securitySettings.hideAmounts,
        )
    }

    val undoHistory = remember { mutableStateListOf<V11Database>() }
    fun saveDb(value: V11Database, recordUndo: Boolean = true) {
        if (writeBusy || writeFailure != null) return
        val expected = db
        writeBusy = true
        writeScope.launch {
            try {
                withContext(Dispatchers.IO) { runtime.saveIfCurrent(expected, value) }
                if (recordUndo) {
                    undoHistory.add(expected)
                    if (undoHistory.size > 20) undoHistory.removeAt(0)
                } else if (undoHistory.isNotEmpty()) undoHistory.removeAt(undoHistory.lastIndex)
                db = value
                withContext(Dispatchers.IO) {
                    runCatching {
                        val changedTasks =
                            expected.tasks.filter { old -> value.tasks.none { it == old } }
                        val changedEvents =
                            expected.dayItems.filter { old -> value.dayItems.none { it == old } }
                        changedTasks.forEach {
                            com.todoshift.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                        }
                        changedEvents.forEach {
                            com.todoshift.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                        }
                        value.tasks
                            .filter { it !in expected.tasks }
                            .forEach {
                                com.todoshift.app.reminders.ToDoShiftReminder.schedule(context, it)
                            }
                        value.dayItems
                            .filter { it !in expected.dayItems }
                            .forEach {
                                com.todoshift.app.reminders.ToDoShiftReminder.schedule(context, it)
                            }
                        val oldSegments = expected.days.flatMap { it.segments }
                        val newSegments = value.days.flatMap { it.segments }
                        if (oldSegments != newSegments) {
                            oldSegments.forEach {
                                com.todoshift.app.reminders.ToDoShiftReminder.cancel(
                                    context,
                                    "shift-${it.id}",
                                )
                            }
                            com.todoshift.app.reminders.ToDoShiftReminder.rescheduleAll(
                                context,
                                value,
                            )
                        }
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Storage failure"
            } finally {
                writeBusy = false
            }
        }
    }

    fun saveUi(value: V11UiSettings) {
        try {
            uiStore.save(value)
            uiSettings = value
        } catch (failure: Exception) {
            writeFailure = failure.message ?: "Settings save failed"
        }

        com.todoshift.app.widget.Neo4WidgetRefresh.schedule(context.applicationContext)
    }

    BackHandler(enabled = showWorkplaces) { showWorkplaces = false }

    // RC9: Calendar is the root/home destination.
    // Back from any main section returns to Calendar first.
    BackHandler(enabled = !showWorkplaces && tab != 0) {
        pendingTaskAction = null
        pendingTaskId = null
        calendarAction = null
        tab = 0
    }

    val sectionTitle =
        if (uiSettings.experience == V11Experience.PREMIUM) {
            when {
                showWorkplaces -> stringResource(R.string.premium_title_workplaces)
                tab == 0 -> stringResource(R.string.premium_nav_calendar)
                tab == 1 -> stringResource(R.string.premium_nav_schedules)
                tab == 2 -> stringResource(R.string.premium_nav_finance)
                tab == 3 -> stringResource(R.string.premium_nav_settings)
                tab == 4 -> stringResource(R.string.premium_nav_tasks)
                else -> stringResource(R.string.premium_nav_calendar)
            }
        } else {
            when {
                showWorkplaces -> "أماكن العمل"
                tab == 0 -> "التقويم"
                tab == 1 -> "جداولي"
                tab == 2 -> "الحسابات"
                tab == 3 -> "الإعدادات"
                tab == 4 -> "المهام"
                else -> "التقويم"
            }
        }

    NeoEnvironment(
        appearance = neoAppearance,
        enabled = uiSettings.experience == V11Experience.PREMIUM,
        onChange = { value ->
            try {
                neoStore.save(value)
                neoAppearance = value
                com.todoshift.app.widget.Neo4WidgetRefresh.schedule(context.applicationContext)
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Appearance save failed"
            }
        },
    ) {
        V11AppThemeProvider(
            theme = uiSettings.appTheme,
            experience = uiSettings.experience,
            premiumBrightness = uiSettings.premiumBrightness,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalV11NeoEnabled provides (uiSettings.experience == V11Experience.PREMIUM),
                LocalNeoFeatures provides
                    NeoFeatureNavigator { id, owner ->
                        when (id) {
                            "WORKPLACES" -> {
                                modernToolsOpen = false
                                showWorkplaces = true
                            }
                            "CALENDAR_MANAGER" -> {
                                modernToolsOpen = false
                                tab = 0
                                calendarManagerRequest++
                            }
                            else -> {
                                featureRoute = id
                                featureOwner = owner
                                modernToolsOpen = true
                            }
                        }
                    },
                LocalNeoUndo provides
                    NeoUndoAction(
                        undoHistory.isNotEmpty() &&
                            !writeBusy &&
                            ModernOperations.canUndo(db, undoHistory.last(), accountsUnlocked)
                    ) {
                        if (
                            undoHistory.isNotEmpty() &&
                                ModernOperations.canUndo(db, undoHistory.last(), accountsUnlocked)
                        ) {
                            val audit =
                                AuditEntryV11(
                                    java.util.UUID.randomUUID().toString(),
                                    "UNDO",
                                    "DATABASE",
                                    "last-edit",
                                    "User confirmed undo",
                                )
                            saveDb(undoHistory.last().copy(auditLog = db.auditLog + audit), false)
                        }
                    },
                LocalNeoFocus provides temporaryFocus,
                LocalNeoFocusChange provides { temporaryFocus = it },
                LocalNeoNavigateDate provides
                    { date ->
                        launchDate = date
                        month = YearMonth.from(date)
                        tab = 0
                        calendarAction = null
                    },
            ) {
                Scaffold(
                    containerColor =
                        if (uiSettings.experience == V11Experience.PREMIUM) Color.Transparent
                        else MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column {
                            V11GlobalHeader(
                                calendarActions = tab == 0 && !showWorkplaces,
                                onToday = {
                                    calendarAction = NeoQuickAction.TODAY
                                    calendarActionId++
                                },
                                onAdd = {
                                    calendarAction = NeoQuickAction.ADD_SHIFT
                                    calendarActionId++
                                },
                                onManage = { calendarManagerRequest++ },
                                onOverview = {
                                    featureRoute = "SEARCH"
                                    featureOwner = null
                                    modernToolsOpen = true
                                },
                                featureDestination =
                                    when (tab) {
                                        0 -> "Calendar"
                                        1 -> "Schedules"
                                        4 -> "Tasks"
                                        2 -> "Finances"
                                        else -> "Settings"
                                    },
                                title =
                                    if (uiSettings.experience == V11Experience.PREMIUM)
                                        when {
                                            showWorkplaces -> neoLabel("أماكن العمل", "Workplaces")
                                            tab == 0 -> neoLabel("التقويم", "Calendar")
                                            tab == 1 -> neoLabel("جداولي", "Shifts")
                                            tab == 2 -> neoLabel("الحسابات", "Finance")
                                            tab == 3 -> neoLabel("الإعدادات", "Settings")
                                            else -> neoLabel("المهام", "Tasks")
                                        }
                                    else sectionTitle,
                                showMoneyAction = tab == 2 && !showWorkplaces && accountsUnlocked,
                                moneyVisible = moneyVisible,
                                onToggleMoney = { moneyVisible = !moneyVisible },
                            )
                        }
                    },
                    bottomBar = {
                        if (uiSettings.experience == V11Experience.PREMIUM) {
                            V11PremiumBottomBar(
                                selectedTab = tab,
                                onSelectTab = { destination ->
                                    showWorkplaces = false
                                    pendingTaskAction = null
                                    pendingTaskId = null
                                    calendarAction = null
                                    tab = destination
                                },
                            )
                        } else {
                            V11HeroBottomBar(
                                selectedTab = tab,
                                onSelectTab = { destination ->
                                    showWorkplaces = false
                                    pendingTaskAction = null
                                    pendingTaskId = null
                                    calendarAction = null
                                    tab = destination
                                },
                            )
                        }
                    },
                ) { padding ->
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(padding)
                                .then(
                                    if (uiSettings.experience == V11Experience.PREMIUM)
                                        Modifier.graphicsLayer { alpha = contentAlpha.value }
                                    else Modifier
                                )
                    ) {
                        if (uiSettings.experience == V11Experience.PREMIUM)
                            Neo4Atmosphere(Modifier.matchParentSize())
                        if (modernToolsOpen) {
                            BackHandler { modernToolsOpen = false }
                            key(featureRoute, featureOwner) {
                                if (
                                    featureRoute in
                                        NeoFeatureCatalog.entries
                                            .filter { it.group.isNotEmpty() }
                                            .map { it.id } &&
                                        featureRoute !in
                                            listOf(
                                                "NOTIFICATIONS",
                                                "HEALTH",
                                                "FOCUS",
                                                "USAGE",
                                                "STORAGE",
                                            )
                                ) {
                                    NeoRescueSettings(
                                        db,
                                        uiSettings,
                                        securitySettings,
                                        securityStore,
                                        accountsUnlocked,
                                        { accountsUnlocked = true },
                                        {
                                            securitySettings = it
                                            moneyVisible = !it.hideAmounts
                                            accountsUnlocked =
                                                !it.protectAccounts ||
                                                    (it.mode == V11LockMode.NONE &&
                                                        !it.biometricEnabled)
                                        },
                                        { saveUi(it) },
                                        { state ->
                                            db = state.database
                                            oldData = state.legacy
                                            uiSettings = state.ui
                                            neoAppearance = state.appearance
                                            activeCalendarId = state.activeCalendarId
                                        },
                                        initialPage = featureRoute,
                                        onBack = { modernToolsOpen = false },
                                    )
                                } else
                                    NeoModernHub(
                                        db,
                                        oldData.workplaces,
                                        accountsUnlocked,
                                        moneyVisible,
                                        { saveDb(it) },
                                        { date ->
                                            modernToolsOpen = false
                                            launchDate = date
                                            month = YearMonth.from(date)
                                            tab = 0
                                            calendarAction = null
                                        },
                                        { modernToolsOpen = false },
                                        initialSection = featureRoute,
                                        focused = true,
                                        initialOwner = featureOwner,
                                    )
                            }
                        } else if (showWorkplaces) {
                            V11WorkplacesScreen(
                                workplaces = NeoRatePolicy.display(db, oldData.workplaces),
                                datedRateKeys = NeoRatePolicy.ownedKeys(db),
                                protectedWorkplaceIds =
                                    db.days
                                        .flatMap { it.segments }
                                        .mapNotNull { it.workplaceId }
                                        .toSet(),
                                onBack = { showWorkplaces = false },
                                onSaveWorkplaces = { workplaces ->
                                    if (!writeBusy && writeFailure == null) {
                                        val expectedLegacy = oldData
                                        val expected = db
                                        writeBusy = true
                                        writeScope.launch {
                                            try {
                                                val (savedLegacy, savedDb) =
                                                    withContext(Dispatchers.IO) {
                                                        com.todoshift.app.backup
                                                            .Neo4LocalBackup(
                                                                context.applicationContext
                                                            )
                                                            .updateWorkplaces(
                                                                expectedLegacy,
                                                                expected,
                                                                workplaces,
                                                            )
                                                    }
                                                oldData = savedLegacy
                                                db = savedDb
                                            } catch (failure: Exception) {
                                                writeFailure =
                                                    failure.message ?: "Workplace save failed"
                                            } finally {
                                                writeBusy = false
                                            }
                                        }
                                    }
                                },
                            )
                        } else {
                            when (tab) {
                                0 ->
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            V11CalendarScreen(
                                                managerRequest = calendarManagerRequest,
                                                onManagerHandled = { calendarManagerRequest = 0 },
                                                onOpenWorkplaces = { showWorkplaces = true },
                                                financeAuthorized = accountsUnlocked,
                                                financeAmountsVisible = moneyVisible,
                                                quickAction = calendarAction,
                                                quickActionId = calendarActionId,
                                                month = month,
                                                initialDate = launchDate,
                                                db = db,
                                                workplaces = oldData.workplaces,
                                                uiSettings = uiSettings,
                                                activeCalendarId = activeCalendarId,
                                                onActiveCalendarChange = { id ->
                                                    try {
                                                        activeCalendarStore.save(id)
                                                        activeCalendarId = id
                                                    } catch (failure: Exception) {
                                                        writeFailure =
                                                            failure.message
                                                                ?: "Unable to save primary calendar"
                                                    }
                                                },
                                                onMonthChange = { month = it },
                                                onDatabaseChange = { saveDb(it) },
                                                onUiSettingsChange = { saveUi(it) },
                                            )
                                        }
                                    }

                                1 ->
                                    V11SchedulesScreen(
                                        financeAuthorized = accountsUnlocked,
                                        financeAmountsVisible = moneyVisible,
                                        onMonthChange = { month = it },
                                        month = month,
                                        db = db,
                                        workplaces = oldData.workplaces,
                                        onDatabaseChange = { saveDb(it) },
                                    )

                                2 ->
                                    if (
                                        securitySettings.protectAccounts &&
                                            (securitySettings.mode != V11LockMode.NONE ||
                                                securitySettings.biometricEnabled) &&
                                            !accountsUnlocked
                                    ) {
                                        V11AccountsUnlockScreen(
                                            settings = securitySettings,
                                            securityStore = securityStore,
                                            onUnlocked = { accountsUnlocked = true },
                                        )
                                    } else {
                                        V11FinanceScreen(
                                            month = month,
                                            db = db,
                                            workplaces = oldData.workplaces,
                                            moneyVisible = moneyVisible,
                                            onPrevious = { month = month.minusMonths(1) },
                                            onNext = { month = month.plusMonths(1) },
                                            onToggleMoney = { moneyVisible = !moneyVisible },
                                            securitySettings = securitySettings,
                                            securityStore = securityStore,
                                            onDatabaseChange = { saveDb(it) },
                                        )
                                    }

                                3 ->
                                    V11MoreScreen(
                                        workplaces = oldData.workplaces,
                                        financeAuthorized = accountsUnlocked,
                                        onAccountsUnlocked = { accountsUnlocked = true },
                                        onNeoRestored = { state ->
                                            db = state.database
                                            oldData = state.legacy
                                            uiSettings = state.ui
                                            neoAppearance = state.appearance
                                            activeCalendarId = state.activeCalendarId
                                        },
                                        db = db,
                                        uiSettings = uiSettings,
                                        moneyVisible = moneyVisible,
                                        securitySettings = securitySettings,
                                        securityStore = securityStore,
                                        onSecuritySettingsChange = {
                                            securitySettings = it

                                            moneyVisible = !it.hideAmounts

                                            accountsUnlocked =
                                                !it.protectAccounts ||
                                                    (it.mode == V11LockMode.NONE &&
                                                        !it.biometricEnabled)
                                        },
                                        onDatabaseRestored = { saveDb(it) },
                                        onUiSettingsChange = { saveUi(it) },
                                        onToggleMoney = { moneyVisible = !moneyVisible },
                                        onOpenWorkplaces = { showWorkplaces = true },
                                    )

                                4 ->
                                    V11TasksScreen(
                                        financeAuthorized = accountsUnlocked,
                                        moneyVisible = moneyVisible,
                                        onQuickAction = { action ->
                                            tab = 0
                                            calendarAction = action
                                            calendarActionId++
                                        },
                                        db = db,
                                        workplaces = oldData.workplaces,
                                        initialAction = pendingTaskAction,
                                        initialTaskId = pendingTaskId,
                                        onInitialActionHandled = {
                                            pendingTaskAction = null
                                            pendingTaskId = null
                                        },
                                        onDatabaseChange = { saveDb(it) },
                                    )

                                else ->
                                    V11CalendarScreen(
                                        managerRequest = calendarManagerRequest,
                                        onManagerHandled = { calendarManagerRequest = 0 },
                                        onOpenWorkplaces = { showWorkplaces = true },
                                        financeAuthorized = accountsUnlocked,
                                        financeAmountsVisible = moneyVisible,
                                        month = month,
                                        initialDate = launchDate,
                                        db = db,
                                        workplaces = oldData.workplaces,
                                        uiSettings = uiSettings,
                                        activeCalendarId = activeCalendarId,
                                        onActiveCalendarChange = { id ->
                                            try {
                                                activeCalendarStore.save(id)
                                                activeCalendarId = id
                                            } catch (failure: Exception) {
                                                writeFailure =
                                                    failure.message
                                                        ?: "Unable to save primary calendar"
                                            }
                                        },
                                        onMonthChange = { month = it },
                                        onDatabaseChange = { saveDb(it) },
                                        onUiSettingsChange = { saveUi(it) },
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V11HeroBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .height(82.dp)
                    .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = neoLiteral("جداولي", "Shifts"),
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = neoLiteral("المهام", "Tasks"),
                selected = selectedTab == 4,
                onClick = { onSelectTab(4) },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            )

            V11CalendarHeroDestination(
                modifier = Modifier.weight(1.15f),
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = neoLiteral("الحسابات", "Finance"),
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                icon = {
                    Box {
                        Icon(Icons.Default.Payments, contentDescription = null)

                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopEnd).size(10.dp),
                        )
                    }
                },
            )

            V11BottomDestination(
                modifier = Modifier.weight(1f),
                label = neoLiteral("الإعدادات", "Settings"),
                selected = selectedTab == 3,
                onClick = { onSelectTab(3) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
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
    icon: @Composable () -> Unit,
) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

    TextButton(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides color) {
                Box(modifier = Modifier.size(25.dp), contentAlignment = Alignment.Center) { icon() }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun V11CalendarHeroDestination(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(if (selected) 54.dp else 50.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = if (selected) 7.dp else 3.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = neoLiteral("التقويم", "Calendar"),
                        modifier = Modifier.size(28.dp),
                        tint =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                neoLiteral("التقويم", "Calendar"),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun V11GlobalHeader(
    title: String,
    showMoneyAction: Boolean = false,
    moneyVisible: Boolean = true,
    onToggleMoney: () -> Unit = {},
    calendarActions: Boolean = false,
    onToday: () -> Unit = {},
    onAdd: () -> Unit = {},
    onManage: () -> Unit = {},
    onOverview: () -> Unit = {},
    featureDestination: String = "",
) {
    if (LocalV11NeoEnabled.current) {
        val p = LocalNeoAppearance.current
        Neo4CompactHeader(
            title,
            calendarActions,
            showMoneyAction,
            moneyVisible,
            onToggleMoney,
            onToday,
            onAdd,
            onManage,
            true,
            onOverview,
            featureDestination,
        )
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (LocalV11NeoEnabled.current) Modifier.heightIn(min = 64.dp)
                        else Modifier.height(64.dp)
                    )
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.todo_shift_icon_source),
                contentDescription = "ToDo Shift",
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.medium),
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ToDo Shift",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )

                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            if (showMoneyAction) {
                IconButton(onClick = onToggleMoney) {
                    Icon(
                        imageVector =
                            if (moneyVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                        contentDescription =
                            if (moneyVisible) neoLiteral("إخفاء المبالغ", "Hide amounts")
                            else neoLiteral("إظهار المبالغ", "Show amounts"),
                    )
                }
            }
        }
    }
}

@Composable
private fun V11PremiumBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    val p = LocalNeoAppearance.current
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            neoDirection(
                p.navigationDirection,
                androidx.compose.ui.platform.LocalLayoutDirection.current,
            )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = if (p.showEffects && p.glow) 4.dp else 0.dp,
        ) {
            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                val outlined = p.iconStyle == NeoIconStyle.OUTLINED
                listOf(
                        Triple(
                            1,
                            neoLabel(neoLiteral("جداولي", "Shifts"), "Shifts"),
                            if (outlined) Icons.Outlined.DateRange else Icons.Rounded.DateRange,
                        ),
                        Triple(
                            4,
                            neoLabel(neoLiteral("المهام", "Tasks"), "Tasks"),
                            if (outlined) Icons.Outlined.CheckCircle else Icons.Rounded.CheckCircle,
                        ),
                        Triple(
                            0,
                            neoLabel(neoLiteral("التقويم", "Calendar"), "Calendar"),
                            if (outlined) Icons.Outlined.CalendarMonth
                            else Icons.Rounded.CalendarMonth,
                        ),
                        Triple(
                            2,
                            neoLabel(neoLiteral("الحسابات", "Finance"), "Finance"),
                            if (outlined) Icons.Outlined.Payments else Icons.Rounded.Payments,
                        ),
                        Triple(
                            3,
                            neoLabel(neoLiteral("الإعدادات", "Settings"), "Settings"),
                            if (outlined) Icons.Outlined.Settings else Icons.Rounded.Settings,
                        ),
                    )
                    .sortedBy { listOf(0, 1, 4, 2, 3).indexOf(it.first) }
                    .forEach { (index, label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onSelectTab(index) },
                            icon = {
                                if (outlined) Icon(icon, contentDescription = null)
                                else
                                    Icon(
                                        painterResource(
                                            when (index) {
                                                0 -> R.drawable.neo41_nav_calendar
                                                1 -> R.drawable.neo41_nav_shifts
                                                2 -> R.drawable.neo41_nav_finance
                                                3 -> R.drawable.neo41_nav_settings
                                                else -> R.drawable.neo41_nav_tasks
                                            }
                                        ),
                                        contentDescription = null,
                                    )
                            },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight =
                                        if (selectedTab == index) FontWeight.Bold
                                        else FontWeight.Normal,
                                )
                            },
                            alwaysShowLabel = true,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
            }
        }
    }
}
