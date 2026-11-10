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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V11WorkplacesScreen(
    workplaces: List<Workplace>,
    onBack: () -> Unit,
    onSaveWorkplaces: (List<Workplace>) -> Unit,
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier.size(44.dp)
                                    .clip(CircleShape)
                                    .background(workplaceColor(workplace.colorHex))
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                workplace.name,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                neoLiteral(
                                    "24س ${workplace.fullDayRate.cleanMoney()}  •  ",
                                    "24h ${workplace.fullDayRate.cleanMoney()}  •  ",
                                ) +
                                    "Long ${workplace.longRate.cleanMoney()}  •  " +
                                    "Night ${workplace.nightRate.cleanMoney()}",
                                style = MaterialTheme.typography.bodySmall,
                            )

                            Text(
                                "Morning ${workplace.morningRate.cleanMoney()}  •  " +
                                    "Evening ${workplace.eveningRate.cleanMoney()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        IconButton(onClick = { editingId = workplace.id }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = neoLiteral("تعديل", "Edit"),
                            )
                        }

                        IconButton(onClick = { deleting = workplace }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = neoLiteral("حذف", "Delete"),
                            )
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
            onDismiss = { adding = false },
            onSave = { value ->
                if (workplaces.any { it.id == value.id }) {
                    reject(
                        "Workplace request already saved; review the stored workplace before opening a new entry."
                    )
                    return@V11WorkplaceEditor
                }
                commit(workplaces + value) { adding = false }
            },
        )
    }

    editing?.let { workplace ->
        val unchanged = rememberNeoDraftUnchanged(workplace.id, workplace)
        V11WorkplaceEditor(
            initial = workplace,
            lockedKinds =
                ShiftKind.entries.filter { "${workplace.id}|${it.name}" in datedRateKeys }.toSet(),
            onDismiss = { editingId = null },
            onSave = { value ->
                if (!unchanged) {
                    reject("Workplace changed. Draft retained; review before reopening.")
                    return@V11WorkplaceEditor
                }
                commit(workplaces.map { if (it.id == value.id) value else it }) { editingId = null }
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
    lockedKinds: Set<ShiftKind> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (Workplace) -> Unit,
) {
    val recordId = rememberSaveable(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }

    var colorHex by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.colorHex ?: workplacePalette.first())
        }

    var fullDay by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.fullDayRate?.editableMoney() ?: "")
        }

    var longRate by
        rememberSaveable(initial?.id) { mutableStateOf(initial?.longRate?.editableMoney() ?: "") }

    var night by
        rememberSaveable(initial?.id) { mutableStateOf(initial?.nightRate?.editableMoney() ?: "") }

    var morning by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.morningRate?.editableMoney() ?: "")
        }

    var evening by
        rememberSaveable(initial?.id) {
            mutableStateOf(initial?.eveningRate?.editableMoney() ?: "")
        }

    val leave =
        rememberNeoEditorExit(
            listOf(name, colorHex, fullDay, longRate, night, morning, evening),
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

                if (lockedKinds.isNotEmpty())
                    Text(
                        neoLiteral(
                            "الأسعار المؤرخة تُعدّل من الأدوات ← أسعار مؤرخة",
                            "Manage dated rates in Tools → Dated rates",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                RateField(
                    enabled = ShiftKind.FULL_DAY !in lockedKinds,
                    label = neoLiteral("24 ساعة", "24 hours"),
                    value = fullDay,
                    onValueChange = { fullDay = it },
                )

                RateField(
                    label = "Long",
                    enabled = ShiftKind.LONG !in lockedKinds,
                    value = longRate,
                    onValueChange = { longRate = it },
                )

                RateField(
                    label = "Night",
                    enabled = ShiftKind.NIGHT !in lockedKinds,
                    value = night,
                    onValueChange = { night = it },
                )

                RateField(
                    label = "Morning",
                    enabled = ShiftKind.MORNING !in lockedKinds,
                    value = morning,
                    onValueChange = { morning = it },
                )

                RateField(
                    label = "Evening",
                    enabled = ShiftKind.EVENING !in lockedKinds,
                    value = evening,
                    onValueChange = { evening = it },
                )
            }
        },
        confirmButton = {
            Button(
                enabled =
                    name.trim().isNotEmpty() &&
                        listOf(fullDay, longRate, night, morning, evening).all {
                            it.toMoneyOrNull() != null
                        },
                onClick = {
                    onSave(
                        Workplace(
                            id = recordId,
                            name = name.trim(),
                            colorHex = colorHex,
                            fullDayRate = fullDay.toMoneyOrNull() ?: 0.0,
                            longRate = longRate.toMoneyOrNull() ?: 0.0,
                            nightRate = night.toMoneyOrNull() ?: 0.0,
                            morningRate = morning.toMoneyOrNull() ?: 0.0,
                            eveningRate = evening.toMoneyOrNull() ?: 0.0,
                        )
                    )
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
    if (this == 0.0) "" else if (this % 1.0 == 0.0) toInt().toString() else toString()
