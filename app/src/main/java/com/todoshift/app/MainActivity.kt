package com.todoshift.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToDoShiftApp()
            }
        }
    }
}

data class WorkPlace(
    val name: String,
    val color: Color
)

data class DayPart(
    val place: WorkPlace
)

data class ShiftDay(
    val day: Int,
    val parts: List<DayPart> = emptyList()
)

private val matamir = WorkPlace("مطامير", Color(0xFF9ED7B1))
private val shamla = WorkPlace("الشاملة", Color(0xFFFFE58A))
private val banna = WorkPlace("البنا", Color(0xFFD6B4E8))
private val master = WorkPlace("الماستر", Color(0xFF9CC8F5))

private val demoDays = listOf(
    ShiftDay(1, listOf(DayPart(matamir))),
    ShiftDay(2, listOf(DayPart(shamla), DayPart(banna))),
    ShiftDay(3),
    ShiftDay(4, listOf(DayPart(master))),
    ShiftDay(5, listOf(DayPart(matamir), DayPart(shamla), DayPart(banna))),
    ShiftDay(6, listOf(DayPart(matamir))),
    ShiftDay(7, listOf(DayPart(matamir))),
    ShiftDay(8),
    ShiftDay(9, listOf(DayPart(shamla))),
    ShiftDay(10),
    ShiftDay(11, listOf(DayPart(banna), DayPart(master))),
    ShiftDay(12),
    ShiftDay(13, listOf(DayPart(matamir))),
    ShiftDay(14, listOf(DayPart(matamir))),
    ShiftDay(15, listOf(DayPart(master))),
    ShiftDay(16, listOf(DayPart(shamla))),
    ShiftDay(17, listOf(DayPart(matamir), DayPart(banna))),
    ShiftDay(18),
    ShiftDay(19),
    ShiftDay(20, listOf(DayPart(matamir))),
    ShiftDay(21, listOf(DayPart(matamir))),
    ShiftDay(22, listOf(DayPart(shamla), DayPart(master))),
    ShiftDay(23),
    ShiftDay(24),
    ShiftDay(25, listOf(DayPart(banna), DayPart(master), DayPart(matamir))),
    ShiftDay(26),
    ShiftDay(27, listOf(DayPart(matamir))),
    ShiftDay(28, listOf(DayPart(matamir))),
    ShiftDay(29, listOf(DayPart(master))),
    ShiftDay(30, listOf(DayPart(shamla))),
    ShiftDay(31)
)

@Composable
fun ToDoShiftApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F8FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {

            Spacer(Modifier.height(18.dp))

            Text(
                text = "ToDo Shift",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF171A21)
            )

            Text(
                text = "جدول الشيفتات",
                fontSize = 14.sp,
                color = Color(0xFF747982)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {}) {
                    Text("‹", fontSize = 32.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "أكتوبر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "2026",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                TextButton(onClick = {}) {
                    Text("›", fontSize = 32.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            WeekHeader()

            Spacer(Modifier.height(6.dp))

            CalendarGrid()

            Spacer(Modifier.height(14.dp))

            Legend()

            Spacer(Modifier.weight(1f))

            NavigationBar()

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun WeekHeader() {
    val names = listOf("سبت", "أحد", "اثن", "ثلا", "أرب", "خمي", "جمع")

    Row(Modifier.fillMaxWidth()) {
        names.forEach {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF777C85)
                )
            }
        }
    }
}

@Composable
fun CalendarGrid() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        demoDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(7 - week.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: ShiftDay,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(
                1.dp,
                Color(0xFFE2E4E8),
                RoundedCornerShape(10.dp)
            )
    ) {

        if (day.parts.isNotEmpty()) {
            Column(Modifier.fillMaxSize()) {
                day.parts.forEach { part ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(part.place.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = part.place.name,
                            fontSize = if (day.parts.size == 3) 8.sp else 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF20242A)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(5.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.day.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242A)
            )
        }
    }
}

@Composable
fun Legend() {
    Column {
        Text(
            "أماكن العمل",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(matamir)
            LegendItem(shamla)
            LegendItem(banna)
            LegendItem(master)
        }
    }
}

@Composable
fun LegendItem(place: WorkPlace) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(place.color)
        )

        Spacer(Modifier.width(4.dp))

        Text(
            place.name,
            fontSize = 10.sp,
            color = Color(0xFF555A62)
        )
    }
}

@Composable
fun NavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            "التقويم",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF246BFD)
        )

        Text("جداولي", color = Color.Gray)
        Text("المالية", color = Color.Gray)
        Text("المزيد", color = Color.Gray)
    }
}
