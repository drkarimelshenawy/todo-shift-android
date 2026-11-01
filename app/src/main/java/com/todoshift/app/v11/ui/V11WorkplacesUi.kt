package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import java.util.UUID

private val workplacePalette = listOf(
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
    "#EF9A9A"
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
    onSaveWorkplaces: (List<Workplace>) -> Unit
) {
    var editing by remember {
        mutableStateOf<Workplace?>(null)
    }

    var adding by remember {
        mutableStateOf(false)
    }

    var deleting by remember {
        mutableStateOf<Workplace?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "أماكن العمل",
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${workplaces.size} أماكن",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    adding = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "إضافة مكان عمل"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = 90.dp
                )
        ) {
            Text(
                "لون المكان هو اللون المستخدم داخل التقويم. الأسعار مستقلة لكل نوع شيفت.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    vertical = 8.dp
                )
            )

            workplaces.forEach { workplace ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable {
                            editing = workplace
                        },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    workplaceColor(
                                        workplace.colorHex
                                    )
                                )
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                workplace.name,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                "24س ${workplace.fullDayRate.cleanMoney()}  •  " +
                                    "Long ${workplace.longRate.cleanMoney()}  •  " +
                                    "Night ${workplace.nightRate.cleanMoney()}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                "Morning ${workplace.morningRate.cleanMoney()}  •  " +
                                    "Evening ${workplace.eveningRate.cleanMoney()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(
                            onClick = {
                                editing = workplace
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل"
                            )
                        }

                        IconButton(
                            onClick = {
                                deleting = workplace
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف"
                            )
                        }
                    }
                }
            }

            if (workplaces.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "لا توجد أماكن عمل",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Text(
                            "اضغط + لإضافة أول مكان عمل.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        V11WorkplaceEditor(
            initial = null,
            onDismiss = {
                adding = false
            },
            onSave = { value ->
                onSaveWorkplaces(
                    workplaces + value
                )
                adding = false
            }
        )
    }

    editing?.let { workplace ->
        V11WorkplaceEditor(
            initial = workplace,
            onDismiss = {
                editing = null
            },
            onSave = { value ->
                onSaveWorkplaces(
                    workplaces.map {
                        if (it.id == value.id) value else it
                    }
                )
                editing = null
            }
        )
    }

    deleting?.let { workplace ->
        AlertDialog(
            onDismissRequest = {
                deleting = null
            },
            title = {
                Text("حذف مكان العمل؟")
            },
            text = {
                Text(
                    "سيتم حذف «${workplace.name}» من قائمة أماكن العمل. " +
                        "لن نحذف الشيفتات القديمة تلقائيًا."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveWorkplaces(
                            workplaces.filterNot {
                                it.id == workplace.id
                            }
                        )
                        deleting = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleting = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun V11WorkplaceEditor(
    initial: Workplace?,
    onDismiss: () -> Unit,
    onSave: (Workplace) -> Unit
) {
    var name by remember(initial?.id) {
        mutableStateOf(initial?.name ?: "")
    }

    var colorHex by remember(initial?.id) {
        mutableStateOf(
            initial?.colorHex ?: workplacePalette.first()
        )
    }

    var fullDay by remember(initial?.id) {
        mutableStateOf(
            initial?.fullDayRate?.editableMoney() ?: ""
        )
    }

    var longRate by remember(initial?.id) {
        mutableStateOf(
            initial?.longRate?.editableMoney() ?: ""
        )
    }

    var night by remember(initial?.id) {
        mutableStateOf(
            initial?.nightRate?.editableMoney() ?: ""
        )
    }

    var morning by remember(initial?.id) {
        mutableStateOf(
            initial?.morningRate?.editableMoney() ?: ""
        )
    }

    var evening by remember(initial?.id) {
        mutableStateOf(
            initial?.eveningRate?.editableMoney() ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null)
                    "إضافة مكان عمل"
                else
                    "تعديل مكان العمل"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("اسم مكان العمل")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    "لون المكان",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                workplacePalette
                    .chunked(6)
                    .forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { hex ->

                                val selected =
                                    hex.equals(
                                        colorHex,
                                        ignoreCase = true
                                    )

                                Surface(
                                    modifier = Modifier
                                        .size(
                                            if (selected)
                                                42.dp
                                            else
                                                36.dp
                                        )
                                        .clickable {
                                            colorHex = hex
                                        },
                                    shape = CircleShape,
                                    color = workplaceColor(hex),
                                    border =
                                        if (selected)
                                            androidx.compose.foundation.BorderStroke(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        else
                                            null
                                ) {}
                            }
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )
                    }

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "أسعار الشيفت",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                RateField(
                    label = "24 ساعة",
                    value = fullDay,
                    onValueChange = {
                        fullDay = it
                    }
                )

                RateField(
                    label = "Long",
                    value = longRate,
                    onValueChange = {
                        longRate = it
                    }
                )

                RateField(
                    label = "Night",
                    value = night,
                    onValueChange = {
                        night = it
                    }
                )

                RateField(
                    label = "Morning",
                    value = morning,
                    onValueChange = {
                        morning = it
                    }
                )

                RateField(
                    label = "Evening",
                    value = evening,
                    onValueChange = {
                        evening = it
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        Workplace(
                            id = initial?.id
                                ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            colorHex = colorHex,
                            fullDayRate =
                                fullDay.toDoubleOrNull() ?: 0.0,
                            longRate =
                                longRate.toDoubleOrNull() ?: 0.0,
                            nightRate =
                                night.toDoubleOrNull() ?: 0.0,
                            morningRate =
                                morning.toDoubleOrNull() ?: 0.0,
                            eveningRate =
                                evening.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun RateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (
                newValue.isEmpty() ||
                newValue.matches(
                    Regex("""\d*\.?\d*""")
                )
            ) {
                onValueChange(newValue)
            }
        },
        label = {
            Text(label)
        },
        suffix = {
            Text("ج")
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    )
}

private fun Double.cleanMoney(): String =
    if (this % 1.0 == 0.0)
        toInt().toString()
    else
        String.format("%.2f", this)

private fun Double.editableMoney(): String =
    if (this == 0.0)
        ""
    else if (this % 1.0 == 0.0)
        toInt().toString()
    else
        toString()
