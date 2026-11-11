package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
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

private class NeoReloadSlot {
    var action: (() -> Unit)? = null
}

@Composable
fun V11Home(
    initialDate: LocalDate? = null,
    initialTab: Int = 0,
    initialTaskAction: String? = null,
    initialTaskId: String? = null,
    entry: NeoEntry? = null,
    onEntryConsumed: () -> Unit = {},
) {
    val context = LocalContext.current

    val runtime = remember { V11Runtime(context.applicationContext) }

    val oldStorage = remember { AppStorage(context.applicationContext) }

    val uiStore = remember { V11UiSettingsStore(context.applicationContext) }

    val securityStore = remember { V11SecurityStore(context.applicationContext) }

    val activeCalendarStore = remember { V11ActiveCalendarStore(context.applicationContext) }

    val writeScope = rememberCoroutineScope()
    var effectsRefreshJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var writeBusy by remember { mutableStateOf(false) }
    var reloadBusy by remember { mutableStateOf(false) }
    val reloadSlot = remember { NeoReloadSlot() }
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
                    val confirmed = runtime.start()
                    // Never block initial screen rendering on reminders or widget Binder calls.
                    com.todoshift.app.backup.Neo4BootState(
                        confirmed,
                        oldStorage.load(),
                        uiStore.load(),
                        NeoAppearanceStore(context.applicationContext).load(),
                        securityStore.load(),
                        activeCalendarStore.load(),
                    )
                }
            // Journal remains durable; retry side effects only after the UI has loaded.
            writeScope.launch {
                kotlinx.coroutines.delay(1500)
                withContext(Dispatchers.IO) {
                    runCatching { NeoCommittedEffects(context).reconcileCurrent() }
                }
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
    if (writeFailure != null)
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties =
                androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
        ) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                        Text(
                            "حالة الحفظ / Save status",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (writeFailure!!.startsWith("Data saved;"))
                                "حُفظت البيانات؛ تحديث الخدمات يحتاج إعادة محاولة. / Data saved; service refresh needs retry."
                            else
                                "لم يتأكد الحفظ؛ المسودة باقية للمراجعة والمحاولة. / Save not confirmed; your draft remains for review and retry."
                        )
                        Text(writeFailure!!, style = MaterialTheme.typography.bodySmall)
                        if (boot != null)
                            TextButton(
                                onClick = { reloadSlot.action?.invoke() },
                                enabled = !writeBusy,
                            ) {
                                Text("إعادة التحميل مع حفظ المسودة / Reload and keep draft")
                            }
                        TextButton(onClick = { writeFailure = null }) {
                            Text("العودة للمحرر / Return to editor")
                        }
                }
            }
        }
    var showWriteProgress by remember { mutableStateOf(false) }
    LaunchedEffect(writeBusy) {
        showWriteProgress = false
        if (writeBusy) {
            kotlinx.coroutines.delay(400)
            showWriteProgress = true
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

    // Account authentication controls access to finance, not screen capture.
    // Do not apply FLAG_SECURE to the shared window: it also hosts calendar and tasks.

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
                withContext(Dispatchers.IO) { activeCalendarStore.save(fallback) }
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
        val repaired = withContext(Dispatchers.Default) {
            NeoRatePolicy.reconcile(db, oldData.workplaces)
        }

        if (repaired != db) {
            writeBusy = true
            try {
                withContext(Dispatchers.IO) {
                    com.todoshift.app.backup
                        .Neo4LocalBackup(context.applicationContext)
                        .commitDatabase(db, repaired)
                }
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
    val navigation = rememberSaveable(saver = NeoNavigator.saver) { NeoNavigator(initialTab) }
    val leaveGate = remember { NeoLeaveGate() }
    val routeState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    fun go(action: () -> Unit) {
        if (!writeBusy && writeFailure == null) leaveGate.request(action)
    }
    var pendingTaskAction by rememberSaveable { mutableStateOf(initialTaskAction) }
    var pendingTaskId by rememberSaveable { mutableStateOf(initialTaskId) }
    var temporaryFocus by remember { mutableStateOf(false) }
    var calendarAction by remember { mutableStateOf<NeoQuickAction?>(null) }
    var calendarActionId by remember { mutableIntStateOf(0) }
    var calendarManagerRequest by remember { mutableIntStateOf(0) }

    val contentAlpha = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(
        navigation.tab,
        uiSettings.experience,
        neoAppearance.motion,
        neoAppearance.reduceMotion,
        neoAppearance.palette,
        neoAppearance.brightness,
        neoAppearance.accentHex,
        neoAppearance.transition,
        neoAppearance.transitionSpeed,
        neoAppearance.optionalTransitions,
        neoAppearance.performance,
        temporaryFocus,
    ) {
        if (
            uiSettings.experience == V11Experience.PREMIUM &&
                neoAppearance.motionMillis > 0 &&
                !temporaryFocus &&
                !(neoAppearance.performance == NeoPerformance.AUTO &&
                    (context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
                            as android.app.ActivityManager)
                        .isLowRamDevice)
        ) {
            contentAlpha.snapTo(0f)
            contentAlpha.animateTo(
                1f,
                androidx.compose.animation.core.tween(neoAppearance.motionMillis),
            )
        } else contentAlpha.snapTo(1f)
    }

    var launchDate by
        rememberSaveable(
            stateSaver =
                androidx.compose.runtime.saveable.Saver<LocalDate, String>(
                    save = { it.toString() },
                    restore = { LocalDate.parse(it) },
                )
        ) {
            mutableStateOf(initialDate ?: LocalDate.now())
        }

    var month by
        rememberSaveable(
            stateSaver =
                androidx.compose.runtime.saveable.Saver<YearMonth, String>(
                    save = { it.toString() },
                    restore = { YearMonth.parse(it) },
                )
        ) {
            mutableStateOf(YearMonth.from(launchDate))
        }

    LaunchedEffect(entry?.token, writeBusy, writeFailure) {
        val request = entry ?: return@LaunchedEffect
        if (writeBusy || writeFailure != null) return@LaunchedEffect
        // Consume delivery once when offered to the guard. Keep editing cancels the route;
        // a later save must not unexpectedly replay a rejected warm link.
        onEntryConsumed()
        go {
            navigation.selectTab(request.tab)
            request.date?.let {
                launchDate = it
                month = YearMonth.from(it)
            }
            pendingTaskAction = request.taskAction
            pendingTaskId = request.taskId
            calendarAction = null
        }
    }
    var moneyVisible by remember { mutableStateOf(!securitySettings.hideAmounts) }

    LaunchedEffect(moneyVisible, securitySettings.protectAccounts, securitySettings.hideAmounts) {
        // Finance privacy is persisted and widget IPC delivered off the Compose main thread.
        withContext(Dispatchers.IO) {
            com.todoshift.app.widget.Neo4WidgetPrivacy.update(
                context.applicationContext,
                !moneyVisible || securitySettings.protectAccounts || securitySettings.hideAmounts,
            )
        }
    }

    val undoHistory = remember { mutableStateListOf<V11Database>() }
    val displayedDb = db
    val displayedLegacy = oldData
    fun saveDb(value: V11Database, recordUndo: Boolean = true, completed: (Boolean) -> Unit = {}) {
        if (writeBusy || writeFailure != null) {
            completed(false)
            return
        }
        val expected = displayedDb
        if (expected != db) {
            writeFailure = "Data changed; reopen before editing. No overwrite was made."
            completed(false)
            return
        }
        writeBusy = true
        writeScope.launch {
            var committed = false
            try {
                withContext(Dispatchers.IO) {
                    com.todoshift.app.backup
                        .Neo4LocalBackup(context.applicationContext)
                        .commitDatabase(expected, value)
                }
                committed = true
                if (recordUndo) {
                    undoHistory.add(expected)
                    if (undoHistory.size > 20) undoHistory.removeAt(0)
                } else if (undoHistory.isNotEmpty()) undoHistory.removeAt(undoHistory.lastIndex)
                db = value

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Storage failure"
            } finally {
                writeBusy = false
                completed(committed)
                if (committed) {
                    effectsRefreshJob?.cancel()
                    effectsRefreshJob = writeScope.launch {
                        // Coalesce rapid strokes. The durable journal also schedules OS retries.
                        kotlinx.coroutines.delay(350)
                        withContext(Dispatchers.IO) {
                            runCatching { NeoCommittedEffects(context).reconcileCurrent() }
                        }
                    }
                }
            }
        }
    }

    fun saveWorkplaces(
        workplaces: List<com.todoshift.app.data.Workplace>,
        completed: (Boolean) -> Unit = {},
    ) {
        if (writeBusy || writeFailure != null) {
            completed(false)
            return
        }
        val expectedLegacy = displayedLegacy
        val expected = displayedDb
        writeBusy = true
        writeScope.launch {
            var committed = false
            try {
                val (savedLegacy, savedDb) =
                    withContext(Dispatchers.IO) {
                        com.todoshift.app.backup
                            .Neo4LocalBackup(context.applicationContext)
                            .updateWorkplaces(expectedLegacy, expected, workplaces)
                    }
                committed = true
                oldData = savedLegacy
                db = savedDb
                withContext(Dispatchers.IO) {
                    runCatching { NeoCommittedEffects(context).reconcileCurrent() }
                        .onFailure {
                            writeFailure =
                                "Data saved; reminder/widget refresh is pending and will retry on reopening."
                        }
                }
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Workplace save not confirmed; draft retained"
            } finally {
                writeBusy = false
                completed(committed)
            }
        }
    }

    fun adoptRestore(state: com.todoshift.app.backup.Neo4RestoredState) {
        db = state.database
        oldData = state.legacy
        uiSettings = state.ui
        neoAppearance = state.appearance
        activeCalendarId = state.activeCalendarId
        undoHistory.clear()
        accountsUnlocked =
            securitySettings.mode == V11LockMode.NONE && !securitySettings.biometricEnabled
    }

    fun reloadEditorData() {
        if (writeBusy) return
        writeBusy = true
        reloadBusy = true
        writeFailure = null
        writeScope.launch {
            try {
                val confirmed =
                    withContext(Dispatchers.IO) {
                        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                            runtime.start() to oldStorage.load()
                        }
                    }
                if (confirmed.first != db || confirmed.second != oldData) undoHistory.clear()
                db = confirmed.first
                oldData = confirmed.second
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Reload failed; draft retained"
            } finally {
                writeBusy = false
                reloadBusy = false
            }
        }
    }
    SideEffect { reloadSlot.action = { reloadEditorData() } }
    var resumeRefreshPending by remember { mutableStateOf(false) }
    LaunchedEffect(resumeRefreshPending, writeBusy, writeFailure) {
        if (resumeRefreshPending && !writeBusy && writeFailure == null) {
            resumeRefreshPending = false
            reloadEditorData()
        }
    }
    DisposableEffect(lifecycleOwner) {
        // Initial boot already loads the current committed database. Android delivers
        // the first ON_RESUME when we register this observer, even on initial launch.
        // Avoid immediately repeating the expensive recovery and full DB parse.
        var initialResumeObserved = false
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    if (!initialResumeObserved) {
                        initialResumeObserved = true
                    } else if (!writeBusy && writeFailure == null) {
                        resumeRefreshPending = false
                        reloadEditorData()
                    } else resumeRefreshPending = true
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun saveAppearance(value: NeoAppearance, done: () -> Unit = {}) {
        if (writeBusy || writeFailure != null) return
        writeBusy = true
        writeScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        NeoCommittedEffects(context).prepareWidgets()
                        neoStore.save(value)
                    }
                }
                neoAppearance = value
                done()
                withContext(Dispatchers.IO) {
                    runCatching { NeoCommittedEffects(context).reconcileCurrent() }
                }
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Appearance save failed; preview retained"
            } finally {
                writeBusy = false
            }
        }
    }

    fun saveUi(value: V11UiSettings) {
        if (writeBusy || writeFailure != null) return
        val expected = uiSettings
        writeBusy = true
        writeScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        check(uiStore.load() == expected) { "Settings changed; reopen before editing" }
                        NeoCommittedEffects(context).prepareWidgets()
                        uiStore.save(value)
                    }
                }
                uiSettings = value
                withContext(Dispatchers.IO) {
                    runCatching { NeoCommittedEffects(context).reconcileCurrent() }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                writeFailure = failure.message ?: "Settings save failed"
            } finally {
                writeBusy = false
            }
        }
    }

    BackHandler(enabled = navigation.canBack) { go { navigation.back() } }

    val sectionTitle =
        if (uiSettings.experience == V11Experience.PREMIUM) {
            when {
                navigation.workplacesOpen -> stringResource(R.string.premium_title_workplaces)
                navigation.tab == 0 -> stringResource(R.string.premium_nav_calendar)
                navigation.tab == 1 -> stringResource(R.string.premium_nav_schedules)
                navigation.tab == 2 -> stringResource(R.string.premium_nav_finance)
                navigation.tab == 3 -> stringResource(R.string.premium_nav_settings)
                navigation.tab == 4 -> stringResource(R.string.premium_nav_tasks)
                else -> stringResource(R.string.premium_nav_calendar)
            }
        } else {
            when {
                navigation.workplacesOpen -> "أماكن العمل"
                navigation.tab == 0 -> "التقويم"
                navigation.tab == 1 -> "جداولي"
                navigation.tab == 2 -> "الحسابات"
                navigation.tab == 3 -> "الإعدادات"
                navigation.tab == 4 -> "المهام"
                else -> "التقويم"
            }
        }

    NeoEnvironment(
        appearance = neoAppearance,
        enabled = uiSettings.experience == V11Experience.PREMIUM,
        onChange = { value -> saveAppearance(value) },
    ) {
        V11AppThemeProvider(
            theme = uiSettings.appTheme,
            experience = uiSettings.experience,
            premiumBrightness = uiSettings.premiumBrightness,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalNeoShellPageHeader provides
                    (uiSettings.experience == V11Experience.PREMIUM && navigation.featureOpen),
                LocalNeoLeaveGate provides leaveGate,
                LocalNeoAppearanceCommit provides { value, done -> saveAppearance(value, done) },
                LocalNeoNoteCommitted provides
                    { next ->
                        undoHistory.add(db)
                        if (undoHistory.size > 20) undoHistory.removeAt(0)
                        db = next
                    },
                LocalNeoFinanceAccess provides { accountsUnlocked },
                LocalNeoRestoreCommitted provides { state -> adoptRestore(state) },
                LocalNeoWorkplaceCommitRequest provides
                    { places, completed ->
                        saveWorkplaces(places, completed)
                    },
                LocalNeoRejectWrite provides { message -> writeFailure = message },
                LocalNeoCommitRequest provides
                    { next, completed ->
                        saveDb(next, completed = completed)
                    },
                LocalNeoEditorWriteState provides NeoEditorWriteState(writeBusy, writeFailure, showWriteProgress),
                LocalV11NeoEnabled provides (uiSettings.experience == V11Experience.PREMIUM),
                LocalNeoFeatures provides
                    NeoFeatureNavigator { id, owner ->
                        go {
                            when (id) {
                                "TASK_DETAIL" -> {
                                    navigation.back()
                                    navigation.selectTab(4)
                                    pendingTaskAction = "open"
                                    pendingTaskId = owner
                                }
                                "WORKPLACES" -> {
                                    navigation.openWorkplaces()
                                }
                                "CALENDAR_MANAGER" -> {
                                    navigation.openCalendarManager()
                                    calendarManagerRequest++
                                }
                                else -> {
                                    navigation.openFeature(id, owner)
                                }
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
                        navigation.selectTab(0)
                        calendarAction = null
                    },
            ) {
                val navigationFrame =
                    neoNavigationFrame(
                        neoAppearance,
                        contentAlpha.value,
                        androidx.compose.ui.platform.LocalLayoutDirection.current ==
                            androidx.compose.ui.unit.LayoutDirection.Rtl,
                        LocalNeoRuntimeReduced.current || temporaryFocus,
                    )
                Scaffold(
                    containerColor =
                        if (uiSettings.experience == V11Experience.PREMIUM) Color.Transparent
                        else MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column {
                            V11GlobalHeader(
                                onBack =
                                    if (navigation.featureOpen) ({ go { navigation.back() } })
                                    else null,
                                calendarActions = navigation.tab == 0 && navigation.isRoot,
                                onToday = {
                                    calendarAction = NeoQuickAction.TODAY
                                    calendarActionId++
                                },
                                onAdd = {
                                    calendarAction = NeoQuickAction.ADD_SHIFT
                                    calendarActionId++
                                },
                                onManage = { calendarManagerRequest++ },
                                onOverview = { go { navigation.openFeature("SEARCH") } },
                                featureDestination =
                                    if (!navigation.isRoot) ""
                                    else
                                        when (navigation.tab) {
                                            0 -> "Calendar"
                                            1 -> "Schedules"
                                            4 -> "Tasks"
                                            2 -> "Finances"
                                            else -> "Settings"
                                        },
                                title =
                                    if (uiSettings.experience == V11Experience.PREMIUM)
                                        when {
                                            navigation.featureOpen ->
                                                NeoFeatureCatalog.get(navigation.feature)?.let {
                                                    neoLabel(it.ar, it.en)
                                                } ?: neoLabel("أدوات", "Tools")
                                            navigation.workplacesOpen ->
                                                neoLabel("أماكن العمل", "Workplaces")
                                            navigation.tab == 0 -> neoLabel("التقويم", "Calendar")
                                            navigation.tab == 1 -> neoLabel("جداولي", "Schedules")
                                            navigation.tab == 2 -> neoLabel("الحسابات", "Finance")
                                            navigation.tab == 3 -> neoLabel("الإعدادات", "Settings")
                                            else -> neoLabel("المهام", "Tasks")
                                        }
                                    else sectionTitle,
                                showMoneyAction =
                                    navigation.tab == 2 && navigation.isRoot && accountsUnlocked,
                                moneyVisible = moneyVisible,
                                onToggleMoney = { moneyVisible = !moneyVisible },
                            )
                        }
                    },
                    bottomBar = {
                        Column {
                            if (showWriteProgress) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                        if (uiSettings.experience == V11Experience.PREMIUM) {
                            V11PremiumBottomBar(
                                selectedTab = navigation.tab,
                                onSelectTab = { destination ->
                                    go {
                                        pendingTaskAction = null
                                        pendingTaskId = null
                                        calendarAction = null
                                        navigation.selectTab(destination)
                                    }
                                },
                            )
                        } else {
                            V11HeroBottomBar(
                                selectedTab = navigation.tab,
                                onSelectTab = { destination ->
                                    go {
                                        pendingTaskAction = null
                                        pendingTaskId = null
                                        calendarAction = null
                                        navigation.selectTab(destination)
                                    }
                                },
                            )
                        }
                        }
                    },
                ) { padding ->
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(padding)
                                .then(
                                    if (uiSettings.experience == V11Experience.PREMIUM)
                                        Modifier.graphicsLayer {
                                            alpha = navigationFrame.alpha
                                            translationX = navigationFrame.x * size.width
                                            translationY = navigationFrame.y * size.height
                                            scaleX = navigationFrame.scale
                                            scaleY = navigationFrame.scale
                                            rotationY = navigationFrame.rotationY
                                            cameraDistance = 12f * density
                                        }
                                    else Modifier
                                )
                    ) {
                        if (uiSettings.experience == V11Experience.PREMIUM)
                            Neo4Atmosphere(Modifier.matchParentSize())
                        routeState.SaveableStateProvider(navigation.stateKey) {
                            if (navigation.featureOpen) {
                                BackHandler { navigation.back() }
                                key(navigation.feature, navigation.owner) {
                                    if (
                                        navigation.feature in
                                            NeoFeatureCatalog.entries
                                                .filter { it.group.isNotEmpty() }
                                                .map { it.id } &&
                                            navigation.feature !in
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
                                            { state -> adoptRestore(state) },
                                            initialPage = navigation.feature,
                                            onBack = { navigation.back() },
                                        )
                                    } else
                                        NeoModernHub(
                                            db,
                                            oldData.workplaces,
                                            accountsUnlocked,
                                            moneyVisible,
                                            { saveDb(it) },
                                            { date ->
                                                navigation.back()
                                                launchDate = date
                                                month = YearMonth.from(date)
                                                navigation.selectTab(0)
                                                calendarAction = null
                                            },
                                            { navigation.back() },
                                            initialSection = navigation.feature,
                                            focused = true,
                                            initialOwner = navigation.owner,
                                        )
                                }
                            } else if (navigation.workplacesOpen) {
                                V11WorkplacesScreen(
                                    workplaces = NeoRatePolicy.display(db, oldData.workplaces),
                                    datedRateKeys = NeoRatePolicy.ownedKeys(db),
                                    protectedWorkplaceIds =
                                        (db.days
                                                .flatMap { it.segments }
                                                .mapNotNull { it.workplaceId } +
                                                db.tasks.mapNotNull { it.workplaceId })
                                            .toSet(),
                                    onBack = { navigation.back() },
                                    onSaveWorkplaces = { saveWorkplaces(it) },
                                )
                            } else {
                                when (navigation.tab) {
                                    0 ->
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                V11CalendarScreen(
                                                    managerRequest = calendarManagerRequest,
                                                    onManagerHandled = {
                                                        calendarManagerRequest = 0
                                                    },
                                                    onOpenWorkplaces = {
                                                        navigation.openWorkplaces()
                                                    },
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
                                            onNeoRestored = { state -> adoptRestore(state) },
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
                                            onOpenWorkplaces = { navigation.openWorkplaces() },
                                        )

                                    4 ->
                                        V11TasksScreen(
                                            financeAuthorized = accountsUnlocked,
                                            moneyVisible = moneyVisible,
                                            onQuickAction = { action ->
                                                navigation.selectTab(0)
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
                                            onManagerDismiss = {
                                                if (navigation.hasParent) navigation.back()
                                            },
                                            onOpenWorkplaces = { navigation.openWorkplaces() },
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
    onBack: (() -> Unit)? = null,
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
            onBack,
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
                painter = painterResource(R.drawable.neo41_brand_emblem),
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
internal fun V11PremiumBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
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
                            neoLabel("جداولي", "Schedules"),
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
                                val width =
                                    androidx.compose.ui.platform.LocalConfiguration.current
                                        .screenWidthDp
                                        .dp / 5 - 2.dp
                                val large =
                                    androidx.compose.ui.platform.LocalDensity.current.fontScale >
                                        1.3f
                                Text(
                                    label,
                                    modifier = Modifier.requiredWidth(width),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontSize = if (large) 10.sp else 11.sp
                                        ),
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

