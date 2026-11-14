package com.todoshift.cashbook.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.cashbook.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ReferenceShape = RoundedCornerShape(12.dp)
internal val ReferenceBlue = Color(0xFF2196E8)
internal val ReferenceBorder = Color(0xFFD5DEE4)

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun RefIcon(@DrawableRes resource: Int, description: String?, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(painterResource(resource), description, modifier.size(22.dp), tint)
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun ReferencePill(text: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, active: Color = Color.Transparent, selectedText: Color = Color.White, onClick: () -> Unit) {
    Box(modifier.height(40.dp).selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth().height(32.dp), shape = RoundedCornerShape(18.dp), color = if (selected) active else Color(0xFFE7E7E7), border = if (selected && active == Color.Transparent) BorderStroke(1.dp, Color.White) else null) {
            Box(contentAlignment = Alignment.Center) { Text(text, color = if (selected) selectedText else Color(0xFF222222), fontSize = 13.sp, maxLines = 1) }
        }
    }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun ReferenceDateTime(day: String, time: String, enabled: Boolean, dateChanged: (String) -> Unit, timeChanged: (String) -> Unit) {
    val context = CashBookText.dialogContext(LocalContext.current)
    val dateClick = {
        val d = LocalDate.parse(day)
        DatePickerDialog(context, { _, y, m, n -> dateChanged(LocalDate.of(y, m + 1, n).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).apply{setOnShowListener{window?.decorView?.layoutDirection=android.view.View.LAYOUT_DIRECTION_LTR};show()}
        Unit
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.weight(0.64f).height(42.dp).border(1.dp, ReferenceBorder, ReferenceShape), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp).fillMaxHeight().clickable(enabled, role = Role.Button) { dateChanged(LocalDate.parse(day).minusDays(1).toString()) }, contentAlignment = Alignment.Center) { RefIcon(R.drawable.cb_left, tr(R.string.cb_110), tint = Color.DarkGray) }
            Text(LocalDate.parse(day).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy", CashBookText.locale())), Modifier.weight(1f).clickable(enabled, onClick = dateClick), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Clip)
            Box(Modifier.width(26.dp).fillMaxHeight().clickable(enabled, role = Role.Button) { dateChanged(LocalDate.parse(day).plusDays(1).toString()) }, contentAlignment = Alignment.Center) { RefIcon(R.drawable.cb_right, tr(R.string.cb_111), tint = Color.DarkGray) }
            Box(Modifier.width(28.dp).fillMaxHeight().clickable(enabled, onClick = dateClick), contentAlignment = Alignment.Center) { RefIcon(R.drawable.cb_calendar, tr(R.string.cb_112)) }
        }
        Row(Modifier.weight(0.36f).height(42.dp).border(1.dp, ReferenceBorder, ReferenceShape).clickable(enabled, role = Role.Button) {
            val t = time.takeIf { it.isNotEmpty() }?.let(LocalTime::parse) ?: LocalTime.now()
            TimePickerDialog(context, { _, h, m -> timeChanged(String.format(Locale.ROOT, "%02d:%02d", h, m)) }, t.hour, t.minute, false).apply{setOnShowListener{window?.decorView?.layoutDirection=android.view.View.LAYOUT_DIRECTION_LTR};show()}
        }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(time.takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("hh:mm a", CashBookText.locale())) } ?: tr(R.string.cb_113), Modifier.weight(1f), fontSize = 12.sp, maxLines = 1)
            RefIcon(R.drawable.cb_clock, tr(R.string.cb_114), Modifier.size(19.dp))
        }
    }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun ReferenceField(value: String, changed: (String) -> Unit, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "", enabled: Boolean = true, readOnly: Boolean = false, numeric: Boolean = false, trailing: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = if (label == null) 0.dp else 6.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (numeric) 58.dp else 52.dp).border(1.dp, if (numeric) MaterialTheme.colorScheme.primary else ReferenceBorder, ReferenceShape).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value, {if(!numeric||com.todoshift.cashbook.core.WholeMoney.input(it,false))changed(it)}, modifier = modifier.weight(1f).padding(vertical = 10.dp), enabled = enabled, readOnly = readOnly,
                singleLine = numeric, maxLines = if (numeric) 1 else 3,
                textStyle = TextStyle(color = Color(0xFF222222), fontSize = if (numeric) 16.sp else 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 14.sp); inner() } })
            trailing()
        }
        if (label != null) Text(label, Modifier.padding(start = 12.dp).offset(y = (-7).dp).background(Color.White).padding(horizontal = 4.dp), color = Color(0xFF35833B), fontSize = 12.sp)
    }
}

/** Display-only formatting. Storage and editable amount parsing remain unchanged. */
internal fun referenceDecimal(value: String): String = java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(Locale.ENGLISH)).format(java.math.BigDecimal(value))
internal fun referenceMoney(minor: Long): String = referenceDecimal(java.math.BigDecimal.valueOf(minor, 2).toPlainString())
internal fun referenceDay(day: String): String = LocalDate.parse(day).format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", CashBookText.locale()))
internal fun referenceTime(time: String): String = if (time.isBlank()) "—" else LocalTime.parse(time).format(DateTimeFormatter.ofPattern("hh:mm a", CashBookText.locale()))

internal fun referenceDayShort(day:String):String = if(day.isBlank()) "—" else LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMM yyyy",CashBookText.locale()))
