package com.todoshift.app.v11.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NeoModernHub(
    db: V11Database,
    workplaces: List<Workplace>,
    financeAuthorized: Boolean,
    moneyVisible: Boolean,
    onChange: (V11Database) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onRevealMoney: () -> Unit = {},
    initialSection: String = "SEARCH",
    focused: Boolean = false,
    initialOwner: String? = null,
    searchScope: String = "CALENDAR",
    initialMonth: YearMonth? = null,
) {
    val commit = rememberNeoCommit(onChange)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { NeoSmartStore(context.applicationContext) }
    var receiveMonths by remember {mutableStateOf<List<YearMonth>?>(null)}
    var section by remember { mutableStateOf(initialSection) }
    receiveMonths?.let {months -> NeoReceiveEntitlementsDialog(db,workplaces,initialOwner,months,{receiveMonths=null}) {next->commit(next) {receiveMonths=null}}}
    val searchSecurity = remember { V11SecurityStore(context.applicationContext) }
    val searchSettings = remember { searchSecurity.load() }
    var searchUnlocked by remember { mutableStateOf(false) }
    if (section == "SEARCH" && searchScope == "TASKS" && (searchSettings.protectTasks||searchSettings.protectTasksOnly||searchSettings.protectNotes) && !searchUnlocked) {
        V11AccountsUnlockScreen(searchSettings, searchSecurity) { searchUnlocked = true }
        return
    }
    if(section == "TASK_TEMPLATES") {
        NeoTaskTemplatesScreen(db, workplaces, onChange, { if(focused) onBack() else section="SEARCH" })
        return
    }

    var error by remember { mutableStateOf<String?>(null) }
    var confirmationBase by remember { mutableStateOf(db) }
    var confirmation by remember { mutableStateOf<Pair<String, V11Database>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable(initialMonth) {
        mutableStateOf((initialMonth ?: YearMonth.now()).atDay(1).toString())
    }
    var to by rememberSaveable(initialMonth) {
        mutableStateOf((initialMonth ?: YearMonth.now()).atEndOfMonth().toString())
    }
    // Open historical salary rate reviews on the inspected accrual month by default;
    // any backdated rate is still explicit, reviewed and manually saved.
    var destination by rememberSaveable(initialSection, initialMonth) {
        mutableStateOf(
            if (initialSection == "RATES") (initialMonth ?: YearMonth.now()).atDay(1).toString()
            else LocalDate.now().plusDays(7).toString()
        )
    }
    var monthA by rememberSaveable(initialMonth) {
        mutableStateOf((initialMonth ?: YearMonth.now()).toString())
    }
    var monthB by rememberSaveable { mutableStateOf(YearMonth.now().minusMonths(1).toString()) }
    var workplace by rememberSaveable(initialOwner) {
        mutableStateOf(initialOwner?.takeIf { selected -> workplaces.any { it.id == selected } }
            ?: workplaces.firstOrNull()?.id)
    }
    var type by rememberSaveable { mutableStateOf(ShiftKind.FULL_DAY) }
    var name by rememberSaveable { mutableStateOf("") }
    var titles by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val parsedAmount = MoneyInput.parse(amount)
    var editingTemplate by rememberSaveable { mutableStateOf<String?>(null) }
    var templateKind by rememberSaveable { mutableStateOf<ShiftKind?>(null) }
    var templateCalendar by rememberSaveable { mutableStateOf<String?>(null) }
    var shiftTemplates by remember {
        mutableStateOf(store.snapshot().optJSONArray("shiftTemplates") ?: org.json.JSONArray())
    }
    var templates by remember { mutableStateOf(store.templates()) }
    var payment by rememberSaveable {
        mutableStateOf(initialOwner ?: db.payments.firstOrNull()?.id)
    }
    var csv by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var detachedReview by remember { mutableStateOf<Set<String>?>(null) }
    var storageRevision by remember { mutableIntStateOf(0) }
    val latestFinancial by rememberUpdatedState(financeAuthorized && moneyVisible)
    val rootFinance = LocalNeoFinanceAccess.current
    fun canUseFinance() = latestFinancial && (rootFinance?.invoke() ?: true)
    LaunchedEffect(financeAuthorized, moneyVisible) {
        if (!canUseFinance()) {
            csv = ""
            if (section in listOf("RATES", "PAYMENTS", "MONTH_REVIEW")) confirmation = null
        }
    }
    val exporter =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            if (uri != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            NeoFinancialExport.write(
                                ::canUseFinance,
                                { context.contentResolver.openOutputStream(uri) },
                                csv,
                            )
                        }
                        error = neoLiteral("تم التصدير", "Exported")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            }
        }
    val owners =
        remember(db.tasks, db.dayItems,searchSettings,searchUnlocked) {
            db.tasks.filter { !it.linkedNotePrivateV11(db) && (searchUnlocked || (!searchSettings.protectTasks && !searchSettings.protectTasksOnly && !(searchSettings.protectNotes&&it.linkedNoteId!=null))) }.map { it.id to it.title } +
                db.dayItems.filter { it.type == DayItemTypeV11.NOTE && !it.notePrivate && it.noteVisible && (searchUnlocked || (!searchSettings.protectTasks&&!searchSettings.protectNotes)) }.map { it.id to it.title }
        }
    var owner by rememberSaveable { mutableStateOf(initialOwner ?: owners.firstOrNull()?.first) }
    val attachments = remember { NeoAttachmentStore(context.applicationContext) }
    var attachmentList by remember { mutableStateOf(emptyList<NeoAttachmentStore.Attachment>()) }
    var storageInventory by remember { mutableStateOf<Pair<Set<String>, Long>?>(null) }
    LaunchedEffect(section, storageRevision) {
        if (section == "ATTACHMENTS" || section == "STORAGE") {
            busy = true
            try {
                val inventory = withContext(Dispatchers.IO) {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        Triple(attachments.list(), attachments.detachedNames(), attachments.storedBytes())
                    }
                }
                attachmentList = inventory.first
                storageInventory = inventory.second to inventory.third
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Unable to read attachment inventory"
            } finally { busy = false }
        }
    }
    fun attachmentWrite(action: () -> Unit, completed: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                attachmentList = withContext(Dispatchers.IO) {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        action()
                        attachments.list()
                    }
                }
                storageInventory = null
                storageRevision++
                completed()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Attachment operation failed"
            } finally { busy = false }
        }
    }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val submittedOwner = owner
            if (uri != null && submittedOwner != null) {
                attachmentWrite({ attachments.import(submittedOwner, uri) })
            }
        }
    fun attempt(action: () -> Unit) {
        try {
            error = null
            action()
        } catch (e: Exception) {
            error = e.message ?: "Operation failed"
        }
    }
    LaunchedEffect(section) {
        if (store.snapshot().optBoolean("usageEnabled", false))
            runCatching {
                store.update { o ->
                    val usage = o.optJSONObject("usage") ?: org.json.JSONObject()
                    usage.put(section, usage.optInt(section) + 1)
                    o.put("usage", usage)
                }
            }
    }
    detachedReview?.let { names ->
        AlertDialog(
            onDismissRequest = { detachedReview = null },
            title = { Text(neoLabel("حذف ملفات غير مرتبطة؟", "Delete detached files?")) },
            text = {
                Text(
                    neoLabel(
                        "سيُحذف ${names.size} ملفًا غير مرتبط نهائيًا؛ لن تتغير السجلات",
                        "Permanently delete ${names.size} detached files; records are unchanged",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        attachmentWrite({ attachments.removeDetached(names) }) {
                            detachedReview = null
                        }
                    }
                ) {
                    Text(neoLabel("حذف الملفات غير المرتبطة", "Delete detached files"))
                }
            },
            dismissButton = {
                TextButton(onClick = { detachedReview = null }) {
                    Text(neoLabel("إلغاء", "Cancel"))
                }
            },
        )
    }
    val financial = canUseFinance()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(neoSpacing()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (focused)
            NeoPageHeader(
                NeoFeatureCatalog.get(section)?.let { neoLabel(it.ar, it.en) }
                    ?: neoToolLabel(section),
                onBack,
            )
        else TextButton(onClick = onBack) { Text(neoLabel("رجوع", "Back")) }
        if (!focused)
            Text(
                neoLabel("أدوات التنظيم والمراجعة", "Organize and review"),
                style = MaterialTheme.typography.titleLarge,
            )
        if (!focused)
            NeoChoice(
                neoLabel("الأداة", "Tool"),
                listOf(
                    "SEARCH",
                    "COPY",
                    "SHIFT_TEMPLATE",
                    "TASK_TEMPLATES",
                    "RATES",
                    "COMPARE",
                    "STATEMENT",
                    "PAYMENTS",
                    "MONTH_REVIEW",
                    "HEALTH",
                    "NOTIFICATIONS",
                    "ATTACHMENTS",
                    "FOCUS",
                    "USAGE",
                ),
                section,
                { neoToolLabel(it) },
                {
                    section = it
                    error = null
                },
            )
        if (
            section in listOf("RATES", "COMPARE", "STATEMENT", "PAYMENTS", "MONTH_REVIEW") &&
                !financial
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(neoLabel("عرض المبالغ مخفي أو محمي", "Amounts are hidden or protected"),
                        style = MaterialTheme.typography.titleMedium)
                    if (financeAuthorized && !moneyVisible) {
                        Text(neoLabel("الحسابات مفتوحة، لكنك أخفيت المبالغ. يمكنك إظهارها من هنا.",
                            "Finance is unlocked, but the amounts are hidden. Reveal them here."))
                        Button(onClick = onRevealMoney) {
                            Text(neoLabel("إظهار المبالغ وفتح الصفحة", "Show amounts and open page"))
                        }
                    } else {
                        Text(neoLabel("ارجع للإدارة المالية وافتح الحسابات أولًا، ثم أظهر المبالغ.",
                            "Return to Finance and unlock it before showing amounts."))
                        OutlinedButton(onClick = onBack) {
                            Text(neoLabel("العودة للإدارة المالية", "Back to Finance"))
                        }
                    }
                }
            }
        }
        when (section) {
            "SEARCH" -> {
                val navigator = LocalNeoFeatures.current
                OutlinedTextField(
                    query,
                    { query = it },
                    label = { Text(neoLabel("بحث في القسم", "Search this section")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                (if (searchScope == "SETTINGS") NeoFeatureCatalog.search(query).take(12) else emptyList()).forEach { feature ->
                    NeoSettingsRow(
                        neoLabel(feature.ar, feature.en),
                        neoLabel("إعداد أو إجراء", "Setting or action"),
                    ) {
                        navigator.open(feature.id)
                    }
                }
                val result =
                    ModernOperations.search(db, query, financial).filter { row ->
                        (!when(row.type) {
                            "TASK" -> searchSettings.protectTasks || searchSettings.protectTasksOnly || (searchSettings.protectNotes && db.tasks.firstOrNull {it.id==row.id}?.linkedNoteId!=null)
                            "NOTE" -> searchSettings.protectTasks || searchSettings.protectNotes
                            else -> false
                        } || searchUnlocked) && (searchScope!="CALENDAR" || row.type!="NOTE" || db.dayItems.firstOrNull {it.id==row.id}?.noteCalendarLinked==true) && row.type in when (searchScope) {
                            "TASKS" -> setOf("TASK", "NOTE")
                            "SALARY" -> setOf("PAYMENT")
                            "CASHBOOK" -> emptySet()
                            "SETTINGS" -> emptySet()
                            else -> setOf("SHIFT", "EVENT", "NOTE")
                        }
                    } + workplaces
                            .filter { searchScope in setOf("CALENDAR", "SALARY") && query.isNotBlank() && it.name.contains(query, true) }
                            .map {
                                ModernOperations.SearchResult("WORKPLACE", it.id, null, it.name)
                            }
                result.take(100).forEach { row ->
                    TextButton(
                        onClick = {
                            if (row.type in setOf("TASK", "NOTE")) navigator.open("TASK_DETAIL", row.id)
                            else if (row.type == "WORKPLACE") navigator.open("WORKPLACES")
                            else if (row.type == "PAYMENT") navigator.open("PAYMENTS", row.id)
                            else row.date?.let { onOpenDate(LocalDate.parse(it)) }
                        }
                    ) {
                        Text(
                            "${neoSearchCategory(row.type)} • ${row.date.orEmpty()} • ${row.text.take(160)}"
                        )
                    }
                }
            }
            "COPY" -> {
                Text(
                    neoLabel(
                        "نسخ شيفتات فقط؛ الملاحظات مستقلة ولا تُنسخ",
                        "Copy shifts only; independent notes are never copied",
                    )
                )
                NeoDateInput(neoLabel("من", "From"), from) { from = it }
                NeoDateInput(neoLabel("إلى", "Through"), to) { to = it }
                NeoDateInput(neoLabel("بداية الوجهة", "Destination start"), destination) {
                    destination = it
                }
                Button(
                    onClick = {
                        attempt {
                            val p =
                                ModernOperations.previewCopy(
                                    db,
                                    LocalDate.parse(from),
                                    LocalDate.parse(to),
                                    LocalDate.parse(destination),
                                )
                            confirmationBase = db
                            confirmation =
                                (neoLiteral(
                                    "سيُنسخ ${p.copied} شيفت. المتعارضات لن تُنسخ: ",
                                    "Copy ${p.copied} shifts. Conflicts are skipped: ",
                                ) + p.conflicts.joinToString()) to p.candidate
                        }
                    }
                ) {
                    Text(neoLabel("مراجعة النسخ", "Review copy"))
                }
            }
            "SHIFT_TEMPLATE",
            "PAINT_SHORTCUTS" -> {
                NeoChoice(
                    neoLabel("مكان العمل", "Workplace"),
                    workplaces.map { it.id },
                    workplace,
                    { id -> workplaces.firstOrNull { it.id == id }?.name.orEmpty() },
                    { workplace = it },
                )
                NeoChoice(
                    neoLabel("نوع الشيفت", "Shift type"),
                    ShiftKind.entries,
                    type,
                    { neoShiftLabel(it) },
                    { type = it },
                )
                NeoDateInput(neoLabel("التاريخ", "Date"), destination) { destination = it }
                Button(
                    onClick = {
                        attempt {
                            val cal =
                                db.calendars.firstOrNull { it.visible }?.id
                                    ?: error("Choose a visible calendar")
                            val p =
                                NeoPaintOperations.preview(
                                    db,
                                    setOf(LocalDate.parse(destination)),
                                    cal,
                                    type,
                                    workplace,
                                    false,
                                )
                            confirmationBase = db
                            confirmation =
                                (p.conflicts.joinToString("\n").ifBlank {
                                    neoLiteral("تأكيد إضافة الشيفت؟", "Confirm shift creation?")
                                }) to p.candidate
                        }
                    }
                ) {
                    Text(neoLabel("مراجعة", "Review"))
                }
                if (section == "SHIFT_TEMPLATE") {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text(neoLabel("اسم قالب الشيفت", "Shift template name")) },
                    )
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            attempt {
                                store.update { o ->
                                    val a = o.optJSONArray("shiftTemplates") ?: org.json.JSONArray()
                                    require(a.length() < 30)
                                    a.put(
                                        org.json
                                            .JSONObject()
                                            .put("id", UUID.randomUUID().toString())
                                            .put("name", name.take(100))
                                            .put("workplace", workplace.orEmpty())
                                            .put("kind", type.name)
                                    )
                                    o.put("shiftTemplates", a)
                                }
                                shiftTemplates = store.snapshot().getJSONArray("shiftTemplates")
                                name = ""
                            }
                        },
                    ) {
                        Text(neoLabel("حفظ قالب مستقل", "Save reusable shift template"))
                    }
                    for (index in 0 until shiftTemplates.length()) {
                        val template = shiftTemplates.getJSONObject(index)
                        TextButton(
                            onClick = {
                                workplace = template.getString("workplace")
                                type = ShiftKind.valueOf(template.getString("kind"))
                            }
                        ) {
                            Text(template.getString("name"))
                        }
                    }
                }
                if (section == "PAINT_SHORTCUTS") {
                    TextButton(
                        onClick = {
                            attempt {
                                val latest =
                                    db.days.map { LocalDate.parse(it.date) }.maxOrNull()
                                        ?: LocalDate.now()
                                destination = latest.plusDays(1).toString()
                            }
                        }
                    ) {
                        Text(neoLabel("اقتراح اليوم التالي فقط", "Suggest next date only"))
                    }
                    TextButton(
                        onClick = {
                            attempt {
                                store.update {
                                    it.put("favoriteWorkplace", workplace.orEmpty())
                                        .put("favoriteKind", type.name)
                                }
                                error = neoLiteral("حُفظ المفضل", "Favorite saved")
                            }
                        }
                    ) {
                        Text(neoLabel("حفظ كمفضل للرسم", "Save paint favorite"))
                    }
                }
            }
            "RATES" ->
                if (financial) {
                    Text(
                        neoLabel("اختر مكان العمل ونوع الشيفت وتاريخ بدء سريان السعر ثم احفظه بنفسك.",
                            "Select the workplace, shift type, effective date and amount, then save explicitly."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (workplaces.isEmpty()) Text(
                        neoLabel("لا توجد أماكن عمل. أضف مكان عمل من التقويم أولًا.",
                            "No workplaces found. Add a workplace in Calendar first."),
                    )
                    NeoChoice(
                        neoLabel("مكان العمل", "Workplace"),
                        workplaces.map { it.id },
                        workplace,
                        { id -> workplaces.firstOrNull { it.id == id }?.name.orEmpty() },
                        { workplace = it },
                    )
                    NeoChoice(
                        neoLabel("النوع", "Type"),
                        ShiftKind.entries.filter { it != ShiftKind.OFF },
                        type,
                        { neoShiftLabel(it) },
                        { type = it },
                    )
                    NeoDateInput(neoLabel("ساري من", "Effective from"), destination) {
                        destination = it
                    }
                    NeoMoneyInput(amount, { amount = it })
                    Button(
                        enabled = parsedAmount.amount != null && workplace != null,
                        onClick = {
                            attempt {
                                val date = LocalDate.parse(destination)
                                require(
                                    db.monthSnapshots.none {
                                        it.reopenedAt == null && it.month >= date.toString().take(7)
                                    }
                                ) {
                                    "Historical closed month is protected"
                                }
                                val candidate =
                                    NeoRatePolicy.add(
                                        db,
                                        workplace!!,
                                        type,
                                        date,
                                        parsedAmount.amount!!,
                                    )
                                confirmationBase = db
                                confirmation =
                                    neoLiteral(
                                        "إضافة سعر مؤرخ؛ لن تُحذف الأسعار السابقة",
                                        "Add dated rate; previous rates are retained",
                                    ) to candidate
                            }
                        },
                    ) {
                        Text(neoLabel("مراجعة السعر", "Review rate"))
                    }
                }
            "COMPARE" ->
                if (financial) {
                    OutlinedTextField(
                        monthA,
                        { monthA = it },
                        label = { Text(neoLabel("الشهر الأول YYYY-MM", "First month YYYY-MM")) },
                    )
                    OutlinedTextField(
                        monthB,
                        { monthB = it },
                        label = { Text(neoLabel("الشهر الثاني", "Second month")) },
                    )
                    listOf(monthA, monthB).forEach { month ->
                        runCatching { ModernOperations.stats(db, YearMonth.parse(month)) }
                            .getOrNull()
                            ?.let { stats ->
                                Text(
                                    "$month • ${stats.shifts} ${neoLiteral("شيفت", "shifts")} • ${stats.workedDays} ${neoLiteral("يوم عمل", "worked days")} • ${stats.off} OFF • ${stats.earnings}"
                                )
                            }
                    }
                }
            "STATEMENT" ->
                if (financial) {
                    Text(neoLabel("كشف حساب مكان العمل", "Workplace statement"),
                        style = MaterialTheme.typography.titleMedium)
                    Text(neoLabel("اختر مكان العمل والفترة؛ الدفعات العامة غير المنسوبة لا تُوزع تلقائيًا.",
                        "Choose workplace and dates; unattributed receipts are not allocated automatically."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    NeoChoice(
                        neoLabel("مكان العمل", "Workplace"),
                        workplaces.map { it.id },
                        workplace,
                        { id -> workplaces.firstOrNull { it.id == id }?.name.orEmpty() },
                        { workplace = it },
                    )
                    Surface(color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            NeoDateInput(neoLabel("من", "From"), from) { from = it }
                            NeoDateInput(neoLabel("إلى", "Through"), to) { to = it }
                        }
                    }
                    val period = remember(from, to) {
                        runCatching {
                            val first = LocalDate.parse(from)
                            val last = LocalDate.parse(to)
                            require(!last.isBefore(first))
                            first to last
                        }.getOrNull()
                    }
                    val months = remember(period) {
                        period?.let { (first, last) ->
                            generateSequence(YearMonth.from(first)) { it.plusMonths(1) }
                                .takeWhile { !it.isAfter(YearMonth.from(last)) }
                                .take(25).toList()
                        }.orEmpty()
                    }
                    val validPeriod = period != null && months.isNotEmpty() &&
                        months.size <= 24 &&
                        !YearMonth.from(period.second).isAfter(months.last())
                    val fullMonths = validPeriod && period != null &&
                        period.first.dayOfMonth == 1 &&
                        period.second.dayOfMonth == period.second.lengthOfMonth()
                    if (!validPeriod) Text(
                        neoLabel("اختر نطاقًا صحيحًا لا يتجاوز 24 شهرًا.",
                            "Choose a valid range of at most 24 months."),
                        color = MaterialTheme.colorScheme.error)
                    if (workplace != null && fullMonths) {
                        val placeId = requireNotNull(workplace)
                        val earned = months.sumOf { activeMonth ->
                            V11FinanceEngine.month(db, activeMonth).byWorkplace
                                .firstOrNull { it.workplaceId == placeId }?.amount ?: 0.0
                        }
                        val paidCents = months.sumOf { activeMonth ->
                            NeoSalaryDashboardLogic.workplaceReceivedCents(db, placeId, activeMonth)
                        }
                        val outstandingCents = months.sumOf { activeMonth ->
                            remainingWorkEntitlement(db, placeId, activeMonth)
                        }
                        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
                            .apply { maximumFractionDigits = 2; minimumFractionDigits = 0 }
                        Surface(color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                ListItem(headlineContent = { Text(neoLabel("المستحق", "Earned")) },
                                    trailingContent = { Text(formatter.format(earned) + " " + neoLabel("ج.م", "EGP")) })
                                ListItem(headlineContent = { Text(neoLabel("المستلم", "Received")) },
                                    trailingContent = { Text(formatter.format(LocalizedNumbers.amount(paidCents)) + " " + neoLabel("ج.م", "EGP")) })
                                ListItem(headlineContent = { Text(neoLabel("المتبقي", "Remaining")) },
                                    trailingContent = { Text(formatter.format(LocalizedNumbers.amount(outstandingCents)) + " " + neoLabel("ج.م", "EGP")) })
                            }
                        }
                        if (outstandingCents <= 0L) Text(
                            neoLabel("تم استلام المستحقات بالكامل", "All entitlements received"),
                            color = MaterialTheme.colorScheme.primary)
                    } else if (workplace != null && validPeriod) {
                        Text(neoLabel("النطاق جزئي؛ سيُصدَّر الكشف بالتواريخ الفعلية. ملخص المستحقات الشهري يظهر عند اختيار أشهر كاملة.",
                            "Partial date range: export uses actual dates. Monthly totals appear for complete months."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val placeKey = workplace
                    // The quick receipt flow allocates complete earning months.
                    // Partial date statements may export exact dates but must not
                    // silently request whole-month receipts.
                    val canReceive = placeKey != null && fullMonths &&
                        months.any { remainingWorkEntitlement(db, placeKey, it) > 0L }
                    if (canReceive) {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            receiveMonths = months
                        }) {
                            Text(neoLabel("استلام مستحقات الكشف", "Receive statement entitlements"))
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = placeKey != null && validPeriod,
                        onClick = {
                            attempt {
                                csv = ModernOperations.statementCsv(
                                    db, requireNotNull(placeKey),
                                    LocalDate.parse(from), LocalDate.parse(to)
                                )
                                exporter.launch("ExMore-statement-$from-$to.csv")
                            }
                        },
                    ) {
                        Text(neoLabel("تصدير كشف الاستحقاقات والمستلم CSV",
                            "Export earned and received CSV"))
                    }
                }
            "PAYMENTS" ->
                if (financial) {
                    NeoChoice(
                        neoLabel("الدفعة", "Payment"),
                        db.payments.map { it.id },
                        payment,
                        { id ->
                            db.payments
                                .firstOrNull { it.id == id }
                                ?.let { "${it.receivedDate} • ${it.amount}" }
                                .orEmpty()
                        },
                        { payment = it },
                    )
                    OutlinedTextField(
                        monthA,
                        { monthA = it },
                        label = {
                            Text(neoLabel("أشهر مفصولة بفاصلة", "Comma-separated accrual months"))
                        },
                    )
                    Button(
                        enabled = payment != null,
                        onClick = {
                            attempt {
                                val months = monthA.split(',').map { YearMonth.parse(it.trim()) }
                                confirmationBase = db
                                confirmation =
                                    neoLiteral(
                                        "توزيع المبلغ بالسنت دون تغيير إجماليه",
                                        "Allocate in cents without changing the total",
                                    ) to ModernOperations.distributePayment(db, payment!!, months)
                            }
                        },
                    ) {
                        Text(neoLabel("مراجعة التوزيع", "Review allocation"))
                    }
                }
            "MONTH_REVIEW" ->
                if (financial) {
                    Text(neoLabel("مراجعة وإقفال الشهر", "Monthly review & closing"),
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(monthA, { monthA = it },
                        label = { Text(neoLabel("شهر الاستحقاق YYYY-MM", "Accrual month YYYY-MM")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val selectedMonth = runCatching { YearMonth.parse(monthA) }.getOrNull()
                    if (selectedMonth == null) {
                        Text(neoLabel("صيغة الشهر غير صحيحة", "Invalid month"),
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        val summary = remember(db, selectedMonth) {
                            V11FinanceEngine.month(db, selectedMonth)
                        }
                        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
                            .apply { minimumFractionDigits = 0; maximumFractionDigits = 2 }
                        val activeSnapshot = db.monthSnapshots.firstOrNull {
                            it.month == monthA && it.reopenedAt == null
                        }
                        if (activeSnapshot != null) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium) {
                                Text(neoLabel("هذا الشهر مُقفل رسميًا", "This month is closed"),
                                    modifier = Modifier.fillMaxWidth().padding(12.dp))
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                listOf(
                                    neoLabel("عدد الورديات", "Shift count") to summary.shiftCount.toString(),
                                    neoLabel("المستحق", "Earned") to formatter.format(summary.expected),
                                    neoLabel("المستلم", "Received") to formatter.format(summary.received),
                                    neoLabel("التسويات", "Adjustments") to formatter.format(summary.settlements),
                                    neoLabel("المتبقي", "Remaining") to formatter.format(summary.difference),
                                ).forEach { (label, value) ->
                                    ListItem(headlineContent = { Text(label) },
                                        trailingContent = { Text(value) })
                                }
                            }
                        }
                        val findings = remember(db) { ModernOperations.health(db) }
                        if (findings.isNotEmpty()) Text(
                            neoLabel("توجد مشكلات سلامة بيانات يجب علاجها قبل الإغلاق.",
                                "Data-health findings must be resolved before closing."),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        if (activeSnapshot == null) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = findings.isEmpty(),
                                onClick = {
                                    attempt {
                                        require(ModernOperations.health(db).isEmpty()) {
                                            "Resolve data-health findings before closing"
                                        }
                                        confirmationBase = db
                                        confirmation = neoLiteral(
                                            "إغلاق الشهر مع لقطة مالية وسجل تدقيق؟",
                                            "Close month with an auditable snapshot?",
                                        ) to V11SettlementEngine.closeMonth(
                                            db, monthA, summary.expected,
                                            summary.received, summary.settlements, summary.difference,
                                        )
                                    }
                                },
                            ) {
                                Text(neoLabel("مراجعة الإغلاق", "Review month closing"))
                            }
                        } else {
                            OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    confirmationBase = db
                                    confirmation = neoLiteral(
                                        "إعادة فتح الشهر رسميًا لتصحيح السجلات؟",
                                        "Explicitly reopen this month for corrections?",
                                    ) to db.copy(
                                        monthSnapshots = db.monthSnapshots.map {
                                            if (it.id == activeSnapshot.id)
                                                it.copy(reopenedAt = System.currentTimeMillis())
                                            else it
                                        },
                                        auditLog = db.auditLog + AuditEntryV11(
                                            UUID.randomUUID().toString(),
                                            "MONTH_REOPEN", "MONTH", activeSnapshot.month,
                                            "User requested audited correction",
                                        ),
                                    )
                                },
                            ) {
                                Text(neoLabel("إعادة فتح الشهر للتصحيح", "Reopen for correction"))
                            }
                        }
                        Text(neoLabel("إقفال الشهر لا ينشئ دفعات، ولا يمس حسابات Cash Book، والتصحيح يتطلب إعادة فتح موثّقة.",
                            "Closing creates no receipts and does not change Cash Book balances. Reopening is audited."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            "HEALTH" -> {
                val withSources = NeoWorkplaceSources.ensure(db, workplaces)
                if (withSources != db)
                    OutlinedButton(
                        onClick = {
                            confirmationBase = db
                            confirmation =
                                neoLiteral(
                                    "إنشاء روابط مصادر دخل لأماكن العمل فقط؛ تُحفظ السجلات السابقة",
                                    "Create missing workplace income-source links only; existing records are preserved",
                                ) to withSources
                        }
                    ) {
                        Text(neoLabel("مراجعة روابط أماكن العمل", "Review workplace source links"))
                    }

                var confirmRepair by remember { mutableStateOf(false) }
                TextButton(onClick = { confirmRepair = true }) {
                    Text(
                        neoLabel(
                            "إعادة تسجيل التذكيرات من البيانات",
                            "Repair reminder registrations from records",
                        )
                    )
                }
                if (confirmRepair)
                    AlertDialog(
                        onDismissRequest = { confirmRepair = false },
                        title = { Text(neoLabel("تأكيد الإصلاح الآمن", "Confirm safe repair")) },
                        text = {
                            Text(
                                neoLabel(
                                    "لا تتغير السجلات أو المبالغ أو التواريخ",
                                    "Records, amounts and dates are unchanged",
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    confirmRepair = false
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                com.todoshift.app.reminders.ToDoShiftReminder
                                                    .rescheduleAll(context, db)
                                            }
                                            error =
                                                neoLiteral(
                                                    "أُعيد تسجيل التذكيرات",
                                                    "Reminder registrations rebuilt",
                                                )
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }
                            ) {
                                Text(neoLabel("تأكيد", "Confirm"))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmRepair = false }) {
                                Text(neoLabel("إلغاء", "Cancel"))
                            }
                        },
                    )
                if (
                    financial &&
                        ModernOperations.repairPaidStatuses(db).obligations != db.obligations
                )
                    TextButton(
                        onClick = {
                            confirmationBase = db
                            confirmation =
                                neoLiteral(
                                    "تعيين الالتزامات المسددة بالكامل كمدفوعة؛ لا تتغير المبالغ",
                                    "Mark fully settled obligations paid; amounts are unchanged",
                                ) to ModernOperations.repairPaidStatuses(db)
                        }
                    ) {
                        Text(neoLabel("مراجعة إصلاح حالات السداد", "Review paid-status repair"))
                    }
                val issues = ModernOperations.health(db)
                Text(
                    if (issues.isEmpty())
                        neoLabel(
                            "لم تُكتشف مشكلة بالقواعد المفحوصة",
                            "No issues found by these checks",
                        )
                    else issues.joinToString("\n")
                )
                Text(
                    neoLabel(
                        "لا إعادة تعيين أو حذف تلقائي؛ راجع السجلات المتأثرة",
                        "No automatic reassignment or deletion; review affected records",
                    )
                )
            }
            "NOTIFICATIONS" -> NeoNotificationControls(db)
            "ATTACHMENTS" -> {
                NeoChoice(
                    neoLabel("المهمة أو الملاحظة", "Task or note"),
                    owners.map { it.first },
                    owner,
                    { id -> owners.firstOrNull { it.first == id }?.second.orEmpty() },
                    { owner = it },
                )
                Button(
                    enabled = owner != null && !busy,
                    onClick = {
                        importer.launch(
                            arrayOf("image/png", "image/jpeg", "application/pdf", "text/plain")
                        )
                    },
                ) {
                    Text(neoLabel("إرفاق ملف أو صورة حتى ٤ ميجا", "Attach image/file up to 4 MiB"))
                }
                attachmentList
                    .filter { it.owner == owner }
                    .forEach { a ->
                        Row {
                            TextButton(
                                onClick = {
                                    attempt {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW)
                                                .setDataAndType(attachments.uri(a.name), a.mime)
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        )
                                    }
                                }
                            ) {
                                Text(a.name.takeLast(18))
                            }
                            TextButton(
                                onClick = {
                                    attachmentWrite({ attachments.detach(a.name) })
                                }
                            ) {
                                Text(neoLabel("فك الارتباط", "Detach"))
                            }
                        }
                    }
            }
            "STORAGE" -> {
                val unused = storageInventory?.first.orEmpty()
                Text(
                    neoLabel("مساحة الملفات الخاصة: ", "Private attachment bytes: ") +
                        (storageInventory?.second ?: 0L)
                )
                Text(neoLabel("ملفات غير مرتبطة: ", "Detached files: ") + unused.size)
                Text(
                    neoLabel(
                        "لا تُحذف المرفقات المرتبطة أو الأرشيفات",
                        "Attached files and safety archives are preserved",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    enabled = !busy && storageInventory != null && unused.isNotEmpty(),
                    onClick = { detachedReview = unused },
                ) {
                    Text(
                        neoLabel(
                            "مراجعة تنظيف الملفات غير المرتبطة",
                            "Review detached-file cleanup",
                        )
                    )
                }
            }
            "FOCUS" -> NeoFocusControls()
            "USAGE" -> {
                val o = store.snapshot()
                Text(
                    neoLabel(
                        "إحصاءات محلية اختيارية، دون إرسال",
                        "Optional on-device counts; never uploaded",
                    )
                )
                NeoSwitch(
                    neoLabel("تسجيل عدد مرات الاستخدام", "Count feature visits"),
                    o.optBoolean("usageEnabled"),
                ) { enabled ->
                    attempt { store.update { it.put("usageEnabled", enabled) } }
                }
                Text(o.optJSONObject("usage")?.toString() ?: "0")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    confirmation
        ?.takeIf { section !in listOf("RATES", "PAYMENTS", "MONTH_REVIEW") || canUseFinance() }
        ?.let { (description, candidate) ->
            AlertDialog(
                onDismissRequest = {
                    if (!busy) {
                        confirmationBase = db
                        confirmation = null
                    }
                },
                title = { Text(neoLabel("تأكيد التعديل", "Confirm change")) },
                text = { Text(description) },
                confirmButton = {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (busy) return@Button
                            val expected = confirmationBase
                            busy = true
                            scope.launch {
                                try {
                                    if (section in listOf("RATES", "PAYMENTS", "MONTH_REVIEW"))
                                        check(canUseFinance()) { "Financial verification required" }
                                    check(db == expected) { "Data changed; review again" }
                                    withContext(Dispatchers.IO) {
                                        com.todoshift.app.backup.createNeoSafetyCopy(
                                            context.applicationContext,
                                            expected,
                                        )
                                    }
                                    if (section in listOf("RATES", "PAYMENTS", "MONTH_REVIEW"))
                                        check(canUseFinance()) { "Financial verification required" }
                                    commit(candidate) { confirmation = null }
                                } catch (e: Exception) {
                                    error = e.message
                                    confirmation = null
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) {
                        Text(neoLabel("تأكيد", "Confirm"))
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            if (!busy) {
                                confirmationBase = db
                                confirmation = null
                            }
                        },
                    ) {
                        Text(neoLabel("إلغاء", "Cancel"))
                    }
                },
            )
        }
}

@Composable
fun NeoDateInput(label: String, value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val d = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
            android.app
                .DatePickerDialog(
                    context,
                    { _, y, m, day -> onChange(LocalDate.of(y, m + 1, day).toString()) },
                    d.year,
                    d.monthValue - 1,
                    d.dayOfMonth,
                )
                .show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("$label • $value")
    }
}

@Composable
fun NeoMoneyInput(value: String, onChange: (String) -> Unit) {
    val parsed = MoneyInput.parse(value)
    OutlinedTextField(
        value,
        onChange,
        label = { Text(neoLabel("المبلغ", "Amount")) },
        isError = !parsed.optionalValid,
        supportingText = {
            if (!parsed.optionalValid) Text(neoLabel("مبلغ غير صالح", "Invalid amount"))
        },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            ),
    )
}

@Composable
fun neoToolLabel(s: String): String =
    when (s) {
        "SEARCH" -> neoLabel("بحث", "Search")
        "COPY" -> neoLabel("نسخ شيفتات", "Copy shifts")
        "SHIFT_TEMPLATE" -> neoLabel("شيفت مفضل", "Shift favorite")
        "TASK_TEMPLATES" -> neoLabel("قوائم جاهزة", "Task templates")
        "RATES" -> neoLabel("أسعار مؤرخة", "Dated rates")
        "COMPARE" -> neoLabel("مقارنة شهرين", "Compare months")
        "STATEMENT" -> neoLabel("كشف مكان العمل", "Workplace statement")
        "PAYMENTS" -> neoLabel("توزيع الدفعات", "Payment allocation")
        "MONTH_REVIEW" -> neoLabel("مراجعة الشهر", "Month review")
        "HEALTH" -> neoLabel("سلامة البيانات", "Data health")
        "NOTIFICATIONS" -> neoLabel("الإشعارات", "Notifications")
        "ATTACHMENTS" -> neoLabel("المرفقات", "Attachments")
        "FOCUS" -> neoLabel("التركيز", "Focus")
        else -> neoLabel("إحصاءات محلية", "Local counts")
    }

