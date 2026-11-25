package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exmore.app.v11.*
import com.exmore.app.data.Workplace
import java.time.YearMonth

@Composable
internal fun NeoOperationalWorkSummary(db:V11Database, places:List<Workplace>, month:YearMonth, change:(YearMonth)->Unit) {
    val visible=db.calendars.filter {it.visible}.map {it.id}.toSet()
    fun rows(m:YearMonth)=db.days.filter {it.calendarId in visible&&it.date.startsWith(m.toString())}
    val days=rows(month);val segments=days.flatMap {it.segments}.filter {it.kind!=ShiftKind.OFF}
    val previous=rows(month.minusMonths(1));val previousCount=previous.sumOf {it.segments.count {s->s.kind!=ShiftKind.OFF}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(neoLabel("ملخص العمل","Work summary"),style=MaterialTheme.typography.titleLarge)
        Row {TextButton(onClick={change(month.minusMonths(1))}){Text("‹")};Text(neoMonthTitle(month,LocalNeoEnglish.current),Modifier.weight(1f));TextButton(onClick={change(month.plusMonths(1))}){Text("›")}}
        Text(neoLabel("${days.filter {d->d.segments.any {it.kind!=ShiftKind.OFF}}.map {it.date}.distinct().size} يوم عمل • ${segments.size} وردية","${days.filter {d->d.segments.any {it.kind!=ShiftKind.OFF}}.map {it.date}.distinct().size} worked days • ${segments.size} shifts"))
        Text(neoLabel("الشهر السابق: $previousCount وردية • الفرق: ${segments.size-previousCount}","Previous month: $previousCount shifts • change: ${segments.size-previousCount}"))
        places.forEach {p->ListItem(headlineContent={Text(p.name)},trailingContent={Text(segments.count {it.workplaceId==p.id}.toString())})}
        ShiftKind.entries.filter {it!=ShiftKind.OFF}.forEach {k->ListItem(headlineContent={Text(neoShiftLabel(k))},trailingContent={Text(segments.count {it.kind==k}.toString())})}
    }
}
