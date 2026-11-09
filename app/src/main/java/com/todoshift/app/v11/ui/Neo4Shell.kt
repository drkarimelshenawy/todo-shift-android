package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Neo4CompactHeader(title: String, calendar: Boolean, showMoney: Boolean, moneyVisible: Boolean,
    onMoney: () -> Unit, onToday: () -> Unit, onAdd: () -> Unit, onManage: () -> Unit,
    overview: Boolean, onOverview: () -> Unit) {
    val p = LocalNeoAppearance.current
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = p.cardOpacity)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 48.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p.showBrand) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.todoshift.app.R.drawable.neo41_brand_emblem), null, Modifier.size(44.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (p.showBrand) Text("ToDo Shift", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (calendar) {
                IconButton(onClick = onToday) { Icon(Icons.Rounded.Today, neoLabel("اليوم", "Today")) }
                IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, neoLabel("إضافة", "Add")) }
                IconButton(onClick = onManage) { Icon(Icons.Rounded.MoreVert, neoLabel("إدارة التقاويم", "Manage calendars")) }
            }
            if (overview) IconButton(onClick = onOverview) { Icon(Icons.Rounded.Search, neoLabel("البحث والأدوات", "Search and tools")) }
            if (showMoney) IconButton(onClick = onMoney) { Icon(if (moneyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, neoLabel("إظهار أو إخفاء المبالغ", "Show or hide amounts")) }
        }
    }
}
