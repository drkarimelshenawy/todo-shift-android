package com.todoshift.cashbook.ui
import com.todoshift.cashbook.R

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import com.todoshift.cashbook.core.*
import java.time.*
import java.util.concurrent.Executors

/** Temporary host adapter. ToDo Shift supplies its unified Host via ViewModel constructor. */
class AndroidDebtReminderHost(private val context:Context):DebtReminders.Host {
    private val manager=context.getSystemService(AlarmManager::class.java)
    private fun alarm()=PendingIntent.getBroadcast(context,411,Intent(context,DebtReminderReceiver::class.java).setAction("cashbook.reminder"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun sync(snapshot:Ledger.Snapshot) = synchronized(schedulingLock) {
        // A receiver and a foreground save may overlap: schedule only the latest committed bytes.
        val current=Ledger(AndroidLedgerStore(context)).load()
        replace(current)
    }
    private fun replace(snapshot:Ledger.Snapshot) {
        CashBookText.initialize(context)
        manager.cancel(alarm())
        val notifications=context.getSystemService(NotificationManager::class.java)
        val prefs=context.getSharedPreferences("cashbook-reminder-delivered",Context.MODE_PRIVATE)
        val desired=DebtReminders.plan(snapshot);val keys=desired.map{it.key}.toSet()
        for(record in notifications.activeNotifications.filter{it.notification.channelId=="cashbook.debt"}){
            val event=desired.find{it.key==record.notification.extras.getString("cashbook.event.key")}
            if(event==null)notifications.cancel(record.tag,record.id)
            else {val body=reminderMessage(event);if(record.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()!=body)notifications.notify(record.tag,record.id,Notification.Builder.recoverBuilder(context,record.notification).setContentTitle(event.title).setContentText(body).setOnlyAlertOnce(true).build())}
        }
        prefs.edit().apply{prefs.all.keys.filter{it !in keys}.forEach{remove(it)}}.commit()
        val now=LocalDateTime.now()
        // An old before-date alert is superseded by its overdue counterpart.
        val next=desired.filter{!prefs.getBoolean(it.key,false)&&!(it.key.endsWith(":before")&&it.at.toLocalDate().isBefore(now.toLocalDate()))}.minByOrNull{it.at}?:return
        val at=if(next.at.isBefore(now))System.currentTimeMillis()+15000 else next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,alarm())
    }
    companion object {internal val schedulingLock=Any();const val OPEN_DEBT="cashbook.open.debt"}
}
class DebtReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        CashBookText.initialize(context)
        val pending=goAsync()
        Executors.newSingleThreadExecutor().also{executor->executor.execute{
            try{synchronized(AndroidDebtReminderHost.schedulingLock){
                val s=Ledger(AndroidLedgerStore(context)).load();val now=LocalDateTime.now();val prefs=context.getSharedPreferences("cashbook-reminder-delivered",Context.MODE_PRIVATE)
                val due=DebtReminders.plan(s).filter{!it.at.isAfter(now)&&!prefs.getBoolean(it.key,false)&&!(it.key.endsWith(":before")&&it.at.toLocalDate().isBefore(now.toLocalDate()))}
                val notifications=context.getSystemService(NotificationManager::class.java)
                notifications.createNotificationChannel(NotificationChannel("cashbook.debt",tr(R.string.cb_229),NotificationManager.IMPORTANCE_DEFAULT))
                val allowed=Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
                if(allowed)for(event in due){
                    val launch=context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply{data=Uri.parse("cashbook://debt/${event.accountId}");putExtra(AndroidDebtReminderHost.OPEN_DEBT,event.accountId);addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)}?:continue
                    val open=PendingIntent.getActivity(context,event.accountId.hashCode(),launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    notifications.notify(event.accountId,411,Notification.Builder(context,"cashbook.debt").setSmallIcon(com.todoshift.cashbook.R.drawable.cb_calendar).setContentTitle(event.title).setContentText(reminderMessage(event)).addExtras(android.os.Bundle().apply{putString("cashbook.event.key",event.key)}).setOnlyAlertOnce(true).setContentIntent(open).setAutoCancel(true).build())
                    prefs.edit().putBoolean(event.key,true).commit()
                }
                if(allowed)AndroidDebtReminderHost(context).sync(s)
            }}catch(_:Exception){/* Never replace failed financial reads with empty data. */}
            finally{pending.finish();executor.shutdown()}
        }}
    }
}

internal fun reminderMessage(event:DebtReminders.Event)=tr(if(event.overdue)R.string.cb_reminder_late else R.string.cb_reminder_before,referenceMoney(event.minor),referenceDayShort(event.date))
