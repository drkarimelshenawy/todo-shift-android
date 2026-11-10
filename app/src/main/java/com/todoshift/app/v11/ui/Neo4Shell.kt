package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun Neo4CompactHeader(
    title: String,
    calendar: Boolean,
    showMoney: Boolean,
    moneyVisible: Boolean,
    onMoney: () -> Unit,
    onToday: () -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
    overview: Boolean,
    onOverview: () -> Unit,
    destination: String = "",
) {
    val p = LocalNeoAppearance.current
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = p.cardOpacity)) {
        Row(
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 48.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val constrained =
                configuration.screenWidthDp < 340 &&
                    androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
            if (p.showBrand) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(
                        com.todoshift.app.R.drawable.todo_shift_icon_source
                    ),
                    null,
                    Modifier.size(if (constrained) 20.dp else 28.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (calendar) {
                IconButton(onClick = onManage) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        neoLabel("إدارة التقاويم", "Manage calendars"),
                    )
                }
            }
            if (overview)
                IconButton(onClick = onOverview) {
                    Icon(Icons.Rounded.Search, neoLabel("بحث", "Search"))
                }
            if (destination.isNotEmpty() && destination != "Settings")
                NeoContextActions(destination)
            if (showMoney)
                IconButton(onClick = onMoney) {
                    Icon(
                        if (moneyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        neoLabel("إظهار أو إخفاء المبالغ", "Show or hide amounts"),
                    )
                }
        }
    }
}
