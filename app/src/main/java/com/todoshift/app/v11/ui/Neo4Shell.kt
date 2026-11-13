package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.graphics.Color
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
    onBack: (() -> Unit)? = null,
) {
    val p = LocalNeoAppearance.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Surface(
        color = NeoPremium.Navy,
        contentColor = NeoPremium.Cream,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(NeoPremium.NavyLight, NeoPremium.Navy)))
                .statusBarsPadding()
                .heightIn(min = 48.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null)
                IconButton(onClick = onBack) {
                    Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                        neoLabel("رجوع", "Back"),
                    )
                }
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val constrained =
                configuration.screenWidthDp < 340 &&
                    androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
            if (p.showBrand) {
                Icon(Icons.Rounded.CalendarMonth, null,
                    Modifier.size(if (constrained) 24.dp else 30.dp), tint = NeoPremium.Gold)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        if (p.showBrand && onBack == null) {
                            withStyle(SpanStyle(color = Color.White)) { append("ToDo ") }
                            withStyle(SpanStyle(color = NeoPremium.Gold)) { append("Shift") }
                        } else append(title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeoPremium.Gold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (calendar) {
                IconButton(onClick = onManage, modifier = Modifier.border(1.dp, Color(0xFF507DA6), CircleShape)) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        neoLabel("إدارة التقاويم", "Manage calendars"),
                    )
                }
            }
            if (overview)
                IconButton(onClick = onOverview, modifier = Modifier.border(1.dp, Color(0xFF507DA6), CircleShape)) {
                    Icon(Icons.Rounded.Search, neoLabel("بحث", "Search"))
                }
            if (destination.isNotEmpty() && destination !in listOf("Settings", "Tasks"))
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
}
