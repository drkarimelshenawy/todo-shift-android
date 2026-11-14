package com.todoshift.app.reminders

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import com.todoshift.app.v11.V11Runtime
import kotlinx.coroutines.*

/** Contains no record preview and never changes a source record's date or reminder. */
class ReminderSnoozeActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        val id=intent.getStringExtra(ToDoShiftReminder.EXTRA_ID) ?: return finish()
        val options=arrayOf("5 min / دقائق","10 min / دقائق","15 min / دقائق","30 min / دقائق","60 min / دقائق","Custom / مخصص")
        AlertDialog.Builder(this).setTitle("غفوة / Snooze").setItems(options) { _, index ->
            if(index<5) snooze(id,listOf(5,10,15,30,60)[index]) else {
                val input=EditText(this).apply {inputType=InputType.TYPE_CLASS_NUMBER;hint="Minutes / دقائق"}
                val dialog=AlertDialog.Builder(this).setTitle("Custom / مخصص").setView(input)
                    .setPositiveButton("OK",null).setNegativeButton("Cancel") {_,_->finish()}.create()
                dialog.setOnShowListener {dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val minutes=input.text.toString().toIntOrNull()
                    if(minutes==null || minutes !in 1..10080) input.error="1–10080" else {dialog.dismiss();snooze(id,minutes)}
                }}
                dialog.setOnCancelListener {finish()};dialog.show()
            }
        }.setNegativeButton("Cancel") {_,_->finish()}.setOnCancelListener {finish()}.show()
    }
    private fun snooze(id:String,minutes:Int) {
        scope.launch {
            val result=withContext(Dispatchers.IO) {runCatching {
                val db=V11Runtime(applicationContext).start()
                val whenMs=System.currentTimeMillis()+minutes*60_000L
                val task=db.tasks.firstOrNull {it.id==id}
                val item=db.dayItems.firstOrNull {it.id==id}
                require(task!=null || item!=null || id.startsWith("shift-"))
                ReminderPolicy(applicationContext).snooze(id,whenMs)
                when {
                    task!=null -> ToDoShiftReminder.schedule(applicationContext,task)
                    item!=null -> ToDoShiftReminder.schedule(applicationContext,item)
                    else -> ToDoShiftReminder.rescheduleAll(applicationContext,db)
                }
            }}
            if(result.isSuccess) finish() else AlertDialog.Builder(this@ReminderSnoozeActivity)
                .setMessage("تعذر حفظ الغفوة / Unable to save snooze").setPositiveButton("OK") {_,_->finish()}.show()
        }
    }
    override fun onDestroy() {scope.cancel();super.onDestroy()}
}
