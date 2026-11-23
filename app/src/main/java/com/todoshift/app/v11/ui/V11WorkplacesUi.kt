package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import com.todoshift.app.v11.ShiftKind
import com.todoshift.app.v11.toMoneyOrNull
import java.util.UUID

private val workplacePalette =
    listOf(
        "#9DD8AE",
        "#FFE28A",
        "#D7ACE8",
        "#91C1EE",
        "#FFB4AB",
        "#B8C4FF",
        "#80CBC4",
        "#FFCC80",
        "#CE93D8",
        "#90CAF9",
        "#A5D6A7",
        "#EF9A9A",
    )

private fun workplaceColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF91C1EE)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun V11WorkplacesScreen(
    workplaces: List<Workplace>,
    onBack: () -> Unit,
    onSaveWorkplaces: (List<Workplace>) -> Unit,
    database: V11Database? = null,
    saveDetails: ((List<Workplace>,V11Database,()->Unit)->Unit)? = null,
    datedRateKeys: Set<String> = emptySet(),
    protectedWorkplaceIds: Set<String> = emptySet(),
) {
    val commit = rememberNeoWorkplaceCommit(onSaveWorkplaces)
    val reject = LocalNeoRejectWrite.current
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = workplaces.firstOrNull { it.id == editingId }

    var adding by rememberSaveable { mutableStateOf(false) }

    var deleting by remember { mutableStateOf<Workplace?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(neoLiteral("أماكن العمل", "Workplaces"), fontWeight = FontWeight.Black)
                        Text(
                            neoLiteral("${workplaces.size} أماكن", "${workplaces.size} workplaces"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = neoLiteral("رجوع", "Back"),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = neoLiteral("إضافة مكان عمل", "Add workplace"),
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 90.dp)
        ) {
            Text(
                neoLiteral(
                    "لون المكان هو اللون المستخدم داخل التقويم. الأسعار مستقلة لكل نوع شيفت.",
                    "Workplace color is used in the calendar. Rates are independent for each shift type.",
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            workplaces.forEach { workplace ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable {
                            editingId = workplace.id
                        },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(workplaceColor(workplace.colorHex)))
                            Text(workplace.name,Modifier.weight(1f).padding(horizontal=10.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            IconButton(onClick={editingId=workplace.id}) {Icon(Icons.Default.Edit,neoLiteral("تعديل","Edit"))}
                            IconButton(onClick={deleting=workplace}) {Icon(Icons.Default.Delete,neoLiteral("حذف","Delete"))}
                        }
                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            listOf(
                                neoLiteral("24س","24h") to workplace.fullDayRate,
                                neoLiteral("طويلة","Long") to workplace.longRate,
                                neoLiteral("ليلية","Night") to workplace.nightRate,
                                neoLiteral("صباحية","Morning") to workplace.morningRate,
                                neoLiteral("مسائية","Evening") to workplace.eveningRate
                            ).forEach { (kind,rate) ->
                                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(kind+" · "+rate.cleanMoney(),Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (workplaces.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            neoLiteral("لا توجد أماكن عمل", "No workplaces yet"),
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            neoLiteral(
                                "اضغط + لإضافة أول مكان عمل.",
                                "Tap + to add your first workplace.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        V11WorkplaceEditor(
            initial = null,
            database = database,
            onDismiss = { adding = false },
            onSave = { value,monthly,effective ->
                if (workplaces.any { it.id == value.id }) {
                    reject(
                        "Workplace request already saved; review the stored workplace before opening a new entry."
                    )
                    return@V11WorkplaceEditor
                }
                if(database!=null&&saveDetails!=null) saveDetails(workplaces+value,NeoWorkplaceEdit.apply(database,null,value,monthly,effective)) {adding=false} else commit(workplaces + value) { adding = false }
            },
        )
    }

    editing?.let { workplace ->
        val unchanged = rememberNeoDraftUnchanged(workplace.id, workplace)
        V11WorkplaceEditor(
            initial = workplace,
            database = database,
            lockedKinds =
                ShiftKind.entries.filter { "${workplace.id}|${it.name}" in datedRateKeys }.toSet(),
            onDismiss = { editingId = null },
            onSave = { value,monthly,effective ->
                if (!unchanged) {
                    reject("Workplace changed. Draft retained; review before reopening.")
                    return@V11WorkplaceEditor
                }
                if(database!=null&&saveDetails!=null) saveDetails(workplaces.map {if(it.id==value.id)value else it},NeoWorkplaceEdit.apply(database,workplace,value,monthly,effective)) {editingId=null} else commit(workplaces.map { if (it.id == value.id) value else it }) { editingId = null }
            },
        )
    }

    deleting?.let { workplace ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(neoLiteral("حذف مكان العمل؟", "Delete workplace?")) },
            text = {
                Text(
                    neoLiteral(
                        "سيتم حذف «${workplace.name}» من قائمة أماكن العمل. ",
                        "Remove «${workplace.name}» from workplaces. ",
                    ) +
                        neoLiteral(
                            "لن نحذف الشيفتات القديمة تلقائيًا.",
                            "Existing shifts will not be deleted automatically.",
                        )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = workplace.id !in protectedWorkplaceIds,
                    onClick = {
                        commit(workplaces.filterNot { it.id == workplace.id }) { deleting = null }
                    },
                ) {
                    Text(neoLiteral("حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(neoLiteral("إلغاء", "Cancel")) }
            },
        )
    }
}

@Composable
private fun V11WorkplaceEditor(
    initial: Workplace?,
    database: V11Database? = null,
    lockedKinds: Set<ShiftKind> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (Workplace,Double?,String) -> Unit,
) {
    val source=initial?.let {place->database?.incomeSources?.firstOrNull {it.workplaceId==place.id}}
    var monthly by rememberSaveable(initial?.id) {mutableStateOf(source?.fixedMonthlyAmount!=null)}
    var salary by rememberSaveable(initial?.id) {mutableStateOf(source?.fixedMonthlyAmount?.toString() ?: "")}
    var effective by rememberSaveable(initial?.id) {mutableStateOf(java.time.LocalDate.now().toString())}
    var periodsOpen by rememberSaveable {mutableStateOf(false)}
    var enabledKinds by rememberSaveable(initial?.id) {mutableStateOf(initial?.enabledShiftKinds ?: ShiftKind.entries.map {it.name}.toSet())}
    var names by rememberSaveable(initial?.id) {mutableStateOf(initial?.shiftNames ?: emptyMap())}
    var colors by rememberSaveable(initial?.id) {mutableStateOf(initial?.shiftColors ?: emptyMap())}
    var kindsOpen by rememberSaveable {mutableStateOf(false)}
    var editError by remember {mutableStateOf<String?>(null)}
    val recordId = rememberSaveable(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }

    var colorHex by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.colorHex ?: workplacePalette.first())
        }

    var fullDay by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.fullDayRate?.editableMoney() ?: "0")
        }

    var longRate by
        rememberSaveable(initial?.id) { mutableStateOf(initial?.longRate?.editableMoney() ?: "0") }

    var night by
        rememberSaveable(initial?.id) { mutableStateOf(initial?.nightRate?.editableMoney() ?: "0") }

    var morning by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.morningRate?.editableMoney() ?: "0")
        }

    var evening by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.eveningRate?.editableMoney() ?: "0")
        }

    val leave =
        rememberNeoEditorExit(
            listOf(name, colorHex, fullDay, longRate, night, morning, evening, monthly,salary,effective,enabledKinds,names,colors),
            onDismiss,
        )
    AlertDialog(
        onDismissRequest = leave,
        title = {
            Text(
                if (initial == null) neoLiteral("إضافة مكان عمل", "Add workplace")
                else neoLiteral("تعديل مكان العمل", "Edit workplace")
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(neoLiteral("اسم مكان العمل", "Workplace name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))

                Text(neoLiteral("لون المكان", "Workplace color"), fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                workplacePalette.chunked(6).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowColors.forEach { hex ->
                            val selected = hex.equals(colorHex, ignoreCase = true)

                            Surface(
                                modifier =
                                    Modifier.size(if (selected) 42.dp else 36.dp).clickable {
                                        colorHex = hex
                                    },
                                shape = CircleShape,
                                color = workplaceColor(hex),
                                border =
                                    if (selected)
                                        androidx.compose.foundation.BorderStroke(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                        )
                                    else null,
                            ) {}
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))

                Text(neoLiteral("أسعار الشيفت", "Shift rates"), fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                if(database!=null) {
                    NeoChoice(neoLabel("طريقة احتساب المستحقات","Compensation model"),listOf(false,true),monthly,{if(it) neoLabel("راتب شهري ثابت","Fixed monthly salary") else neoLabel("بالوردية","Per shift")},{monthly=it})
                    if(monthly) {RateField(neoLabel("الراتب الشهري","Monthly salary"),value=salary,onValueChange={salary=it});Text(neoLabel("يسري الراتب من أول شهر التاريخ المختار؛ لا يُضرب في عدد الورديات","Salary applies from the first of the selected month; it is never multiplied by shift count"),style=MaterialTheme.typography.labelSmall)}
                    NeoDateInput(neoLabel("سريان التغيير؛ الفترات السابقة محفوظة","Effective date; previous periods retained"),effective){effective=it}
                    TextButton(onClick={periodsOpen=!periodsOpen}) {Text(if(monthly) neoLabel("فترات الرواتب","Salary periods") else neoLabel("فترات الأسعار","Rate periods"))}
                    if(periodsOpen) {
                        source?.compensationHistory?.sortedByDescending {it.effectiveMonth}?.forEach {Text("${it.effectiveMonth} • "+if(it.shiftBased) neoLabel("بالوردية","Per shift") else it.amount.toString())}
                        database.rates.filter {it.workplaceId==initial?.id}.groupBy {it.effectiveFrom}.toSortedMap(reverseOrder()).forEach {(date,periods)->
                            Text(date,style=MaterialTheme.typography.titleSmall)
                            Text(periods.map {neoShiftLabel(it.shiftKind)+": "+it.amount}.joinToString(" • "))
                            TextButton(onClick={fullDay=(periods.firstOrNull {it.shiftKind==ShiftKind.FULL_DAY}?.amount ?: fullDay.toMoneyOrNull() ?: 0.0).toString();longRate=(periods.firstOrNull {it.shiftKind==ShiftKind.LONG}?.amount ?: longRate.toMoneyOrNull() ?: 0.0).toString();night=(periods.firstOrNull {it.shiftKind==ShiftKind.NIGHT}?.amount ?: night.toMoneyOrNull() ?: 0.0).toString();morning=(periods.firstOrNull {it.shiftKind==ShiftKind.MORNING}?.amount ?: morning.toMoneyOrNull() ?: 0.0).toString();evening=(periods.firstOrNull {it.shiftKind==ShiftKind.EVENING}?.amount ?: evening.toMoneyOrNull() ?: 0.0).toString()}) {Text(neoLabel("نسخ أسعار الفترة","Copy period rates"))}
                        }
                    }
                }
                TextButton(onClick={kindsOpen=!kindsOpen}) {Text(neoLabel("أنواع الورديات وألوانها","Shift types and colors"))}
                if(kindsOpen) ShiftKind.entries.forEach {kind->
                    NeoSwitch(neoShiftLabel(kind),kind.name in enabledKinds){on->enabledKinds=if(on)enabledKinds+kind.name else enabledKinds-kind.name}
                    if(kind.name in enabledKinds) {
                        OutlinedTextField(names[kind.name].orEmpty(),{names=names+(kind.name to it)},label={Text(neoLabel("اسم مخصص (اختياري)","Custom name (optional)"))})
                        OutlinedTextField(colors[kind.name].orEmpty(),{colors=colors+(kind.name to it)},label={Text(neoLabel("لون النوع #RRGGBB (اختياري)","Type color #RRGGBB (optional)"))})
                    }
                }
                editError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                if(!monthly) Column {
                RateField(
                    enabled = true,
                    label = neoLiteral("24 ساعة", "24 hours"),
                    value = fullDay,
                    onValueChange = { fullDay = it },
                )

                RateField(
                    label = "Long",
                    enabled = true,
                    value = longRate,
                    onValueChange = { longRate = it },
                )

                RateField(
                    label = "Night",
                    enabled = true,
                    value = night,
                    onValueChange = { night = it },
                )

                RateField(
                    label = "Morning",
                    enabled = true,
                    value = morning,
                    onValueChange = { morning = it },
                )

                RateField(
                    label = "Evening",
                    enabled = true,
                    value = evening,
                    onValueChange = { evening = it },
                )
                }
            }
        },
        confirmButton = {
            leave.SaveButton(
                enabled =
                    name.trim().isNotEmpty() && enabledKinds.isNotEmpty() &&
                        colors.values.all { it.isBlank() || it.matches(Regex("#[0-9a-fA-F]{6}")) } &&
                        runCatching { java.time.LocalDate.parse(effective) }.isSuccess &&
                        workplaceCompensationInputsValid(
                            monthly, salary, listOf(fullDay, longRate, night, morning, evening)
                        ),
                onClick = {
                    try {onSave(
                        Workplace(
                            id = recordId,
                            name = name.trim(),
                            colorHex = colorHex,
                            // Monthly-pay edits must not overwrite hidden per-shift values.
                            fullDayRate = if (monthly) initial?.fullDayRate ?: 0.0 else fullDay.toMoneyOrNull() ?: 0.0,
                            longRate = if (monthly) initial?.longRate ?: 0.0 else longRate.toMoneyOrNull() ?: 0.0,
                            nightRate = if (monthly) initial?.nightRate ?: 0.0 else night.toMoneyOrNull() ?: 0.0,
                            morningRate = if (monthly) initial?.morningRate ?: 0.0 else morning.toMoneyOrNull() ?: 0.0,
                            eveningRate = if (monthly) initial?.eveningRate ?: 0.0 else evening.toMoneyOrNull() ?: 0.0,
                            enabledShiftKinds=enabledKinds,shiftNames=names.filterValues {it.isNotBlank()},shiftColors=colors.filterValues {it.isNotBlank()},
                        ),if(monthly) salary.toMoneyOrNull() else null,effective
                    )} catch(failure:Exception) {editError=failure.message}
                },
            ) {
                Text(neoLiteral("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = leave) { Text(neoLiteral("إلغاء", "Cancel")) } },
    )
}

@Composable
private fun RateField(
    label: String,
    enabled: Boolean = true,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        enabled = enabled,
        isError = value.isNotBlank() && value.toMoneyOrNull() == null,
        supportingText = {
            if (value.isNotBlank() && value.toMoneyOrNull() == null)
                Text(neoLiteral("مبلغ غير صالح", "Invalid amount"))
        },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            ),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(neoLiteral("ج", "EGP")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    )
}

private fun Double.cleanMoney(): String =
    if (this % 1.0 == 0.0) toInt().toString()
    else String.format(java.util.Locale.ROOT, "%.2f", this)

private fun Double.editableMoney(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

/** Fixed monthly pay must not depend on hidden per-shift inputs. */
internal fun workplaceCompensationInputsValid(
    monthly: Boolean,
    salary: String,
    shiftRates: List<String>,
): Boolean {
    fun validAmount(input: String): Boolean =
        input.toMoneyOrNull()?.let { it.isFinite() && it >= 0.0 } == true
    return if (monthly) validAmount(salary) else shiftRates.all(::validAmount)
}
