package com.todoshift.app.v11.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.v11.*
import com.todoshift.app.reminders.*
import kotlinx.coroutines.*

@Composable
fun NeoReminderRecurrenceControls(db:V11Database) {
    val context=LocalContext.current
    if(com.todoshift.app.widget.neoWidgetProtected(context,true)) {
        Text(neoLabel("افتح قسم المهام المحمي لإدارة تذكيراته","Open the protected Tasks section to manage its reminders"))
        return
    }
    val scope=rememberCoroutineScope()
    val rules=remember {ReminderPolicy(context.applicationContext)}
    val records=remember(db) {
        db.tasks.filter {it.reminderAt!=null && it.status!=TaskStatusV11.COMPLETED}.map {it.id to it.title} +
            db.dayItems.filter {it.reminderAt!=null && !it.completed && it.noteVisible && !it.notePrivate}.map {it.id to it.title}
    }
    if(records.isEmpty()) return
    var id by remember(records) {mutableStateOf(records.first().first)}
    var kind by remember(id) {mutableStateOf(rules.rule(id).first)}
    var days by remember(id) {mutableStateOf(rules.rule(id).second.toString())}
    var error by remember {mutableStateOf<String?>(null)}
    var saving by remember {mutableStateOf(false)}
    Text(neoLabel("تكرار التذكير (لا يغير تاريخ العنصر)","Reminder recurrence (keeps the item date)"))
    NeoChoice(neoLabel("التذكير","Reminder"),records.map {it.first},id,{ selected -> records.firstOrNull {it.first==selected}?.second ?: selected },{id=it})
    NeoChoice(neoLabel("التكرار","Recurrence"),ReminderPolicy.kinds,kind,{when(it) {
        "DAILY"->neoLiteral("يومي","Daily");"WEEKLY"->neoLiteral("أسبوعي","Weekly");"MONTHLY"->neoLiteral("شهري","Monthly")
        "YEARLY"->neoLiteral("سنوي","Yearly");"CUSTOM"->neoLiteral("كل عدد من الأيام","Custom interval in days");else->neoLiteral("مرة واحدة","One time")
    }},{kind=it})
    if(kind=="CUSTOM") OutlinedTextField(days,{days=it.filter(Char::isDigit).take(4)},label={Text(neoLabel("عدد الأيام","Days"))},singleLine=true)
    TextButton(enabled=!saving,onClick={
        val interval=if(kind=="CUSTOM") days.toIntOrNull() else 1
        if(interval==null || interval !in 1..3660) error=neoLiteral("أدخل ١ إلى ٣٦٦٠ يومًا","Enter 1 to 3660 days")
        else {
            saving=true
            scope.launch {
                try {withContext(Dispatchers.IO) {rules.configure(id,kind,interval);ToDoShiftReminder.rescheduleAll(context,db)};error=null}
                catch(failure:Exception) {error=failure.message ?: "Unable to save reminder"}
                finally {saving=false}
            }
        }
    }) {Text(neoLabel("حفظ التكرار","Save recurrence"))}
    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}
