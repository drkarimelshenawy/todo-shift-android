package com.todoshift.app.v11.ui

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.MainActivity
import com.todoshift.app.R

@Composable
fun NeoShortcutControls() {
    val context=LocalContext.current
    val manager=remember {context.getSystemService(ShortcutManager::class.java)}
    var result by remember {mutableStateOf<String?>(null)}
    Text(neoLabel("اختصارات المشغّل","Launcher shortcuts"),style=MaterialTheme.typography.titleSmall)
    val destinations=listOf(
        Triple("calendar",neoLabel("التقويم والعمل","Calendar & Work"),0),
        Triple("tasks",neoLabel("المهام","Tasks"),4),
        Triple("notes",neoLabel("الملاحظات","Notes"),2),
        Triple("new_note",neoLabel("ملاحظة جديدة","New note"),2),
        Triple("cashbook",neoLabel("الإدارة المالية","Cash Book"),1),
        Triple("settings",neoLabel("الإعدادات","Settings"),3),
    )
    if(manager?.isRequestPinShortcutSupported!=true)
        Text(neoLabel("المشغّل الحالي لا يدعم طلب تثبيت الاختصارات","This launcher does not support shortcut pin requests"))
    destinations.forEach { (id,label,tab) ->
        TextButton(enabled=manager?.isRequestPinShortcutSupported==true,onClick={
            result=try {
                val intent=Intent(context,MainActivity::class.java).setAction(Intent.ACTION_VIEW)
                    .putExtra("neo4_open_tab",tab)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if(id=="tasks") intent.putExtra("todo_shift_task_action","tasks")
                if(id=="notes") intent.putExtra("todo_shift_task_action","notes")
                if(id=="new_note") intent.putExtra("todo_shift_task_action","new_note")
                val icon=when(id){
                    "calendar"->R.drawable.exmore_shortcut_calendar_work
                    "cashbook"->R.drawable.exmore_shortcut_cash_book
                    "settings"->R.drawable.exmore_shortcut_settings
                    "notes", "new_note"->R.drawable.exmore_shortcut_notes
                    else->R.drawable.exmore_shortcut_tasks
                }
                val shortcut=ShortcutInfo.Builder(context,id).setShortLabel(label).setLongLabel(label)
                    .setIcon(Icon.createWithResource(context,icon)).setIntent(intent).build()
                if(manager!!.requestPinShortcut(shortcut,null)) neoLiteral("أُرسل الطلب للمشغّل؛ أكمل التأكيد فيه. لم يتم التحقق من إنشاء الاختصار.","Request sent to the launcher; confirm there. Shortcut creation has not been verified.")
                else neoLiteral("لم يقبل المشغّل الطلب","The launcher did not accept the request")
            } catch(failure:Exception) {neoLiteral("تعذر طلب الاختصار","Unable to request shortcut")}
        }) {Text(neoLabel("إضافة ","Add ")+label)}
    }
    result?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
}
