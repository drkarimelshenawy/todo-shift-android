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
    val records=remember(db) {
        db.tasks.filter {it.reminderAt!=null && it.status!=TaskStatusV11.COMPLETED}.map {it.id to it.title} +
            db.dayItems.filter {it.reminderAt!=null && !it.completed && it.noteVisible && !it.notePrivate}.map {it.id to it.title}
    }
    if(records.isEmpty()) return
    var id by remember(records) {mutableStateOf(records.first().first)}
    NeoChoice(neoLabel("التذكير","Reminder"),records.map {it.first},id,{ selected -> records.firstOrNull {it.first==selected}?.second ?: selected },{id=it})
    NeoItemReminderRecurrenceControls(id)
}

/** Only call from an authorized record editor; this control contains no content preview. */
@Composable
fun NeoItemReminderRecurrenceControls(id:String) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val rules=remember {ReminderPolicy(context.applicationContext)}
    var kind by remember(id) {mutableStateOf(rules.rule(id).first)}
    var days by remember(id) {mutableStateOf(rules.rule(id).second.toString())}
    var error by remember(id) {mutableStateOf<String?>(null)}
    var saved by remember(id) {mutableStateOf(false)}
    var saving by remember(id) {mutableStateOf(false)}
    Text(neoLabel("تكرار التذكير (لا يغير تاريخ العنصر)","Reminder recurrence (keeps the item date)"))
    NeoChoice(neoLabel("التكرار","Recurrence"),ReminderPolicy.kinds,kind,{when(it) {
        "DAILY"->neoLiteral("يومي","Daily");"WEEKLY"->neoLiteral("أسبوعي","Weekly");"MONTHLY"->neoLiteral("شهري","Monthly")
        "YEARLY"->neoLiteral("سنوي","Yearly");"CUSTOM"->neoLiteral("كل عدد من الأيام","Custom interval in days");else->neoLiteral("مرة واحدة","One time")
    }},{kind=it;saved=false})
    if(kind=="CUSTOM") OutlinedTextField(days,{days=it.filter(Char::isDigit).take(4);saved=false},label={Text(neoLabel("عدد الأيام","Days"))},singleLine=true)
    TextButton(enabled=!saving,onClick={
        val interval=if(kind=="CUSTOM") days.toIntOrNull() else 1
        if(interval==null || interval !in 1..3660) error=neoLiteral("أدخل ١ إلى ٣٦٦٠ يومًا","Enter 1 to 3660 days")
        else {
            saving=true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        rules.configure(id,kind,interval)
                        val current=V11Runtime(context.applicationContext).start()
                        current.tasks.firstOrNull {it.id==id}?.let {ToDoShiftReminder.schedule(context,it)}
                        current.dayItems.firstOrNull {it.id==id}?.let {ToDoShiftReminder.schedule(context,it)}
                    }
                    error=null;saved=true
                } catch(failure:Exception) {error=failure.message ?: "Unable to save reminder"}
                finally {saving=false}
            }
        }
    }) {Text(neoLabel("حفظ التكرار","Save recurrence"))}
    if(saved) Text(neoLabel("تم الحفظ؛ يُستخدم عند حفظ العنصر أيضًا","Saved; also used when the item is saved"),style=MaterialTheme.typography.bodySmall)
    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}
