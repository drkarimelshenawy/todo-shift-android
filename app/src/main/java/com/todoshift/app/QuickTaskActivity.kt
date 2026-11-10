package com.todoshift.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import com.todoshift.app.v11.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Launcher/widget entry point: a floating task editor, never opens MainActivity. */
class QuickTaskActivity : FragmentActivity() {
    private val notificationPermission =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) {
            finish()
        }

    private fun finishAfterSave(task: TaskV11) {
        if (
            task.reminderAt != null &&
                android.os.Build.VERSION.SDK_INT >= 33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        )
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        setContent {
            val scope = rememberCoroutineScope()
            var state by remember { mutableStateOf<V11Database?>(null) }
            var workplaces by remember {
                mutableStateOf(emptyList<com.todoshift.app.data.Workplace>())
            }
            var failure by remember { mutableStateOf<String?>(null) }
            var busy by remember { mutableStateOf(false) }
            androidx.activity.compose.BackHandler(enabled = busy) {}
            val appearance = remember {
                runCatching { NeoAppearanceStore(applicationContext).load() }
                    .getOrDefault(NeoAppearance())
            }
            LaunchedEffect(Unit) {
                try {
                    withContext(Dispatchers.IO) {
                            val confirmed = V11Runtime(applicationContext).start()
                            runCatching {
                                NeoCommittedEffects(applicationContext).reconcileCurrent()
                            }
                            confirmed to AppStorage(applicationContext).load().workplaces
                        }
                        .let {
                            state = it.first
                            workplaces = it.second
                        }
                } catch (e: Exception) {
                    failure = e.message ?: "Unable to load data safely"
                }
            }
            NeoEnvironment(appearance, true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    if (failure != null && state == null)
                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text("تعذر الحفظ / Save unavailable") },
                            text = { Text(failure!!) },
                            confirmButton = {
                                TextButton(onClick = { finish() }) { Text("إغلاق / Close") }
                            },
                        )
                    else if (state != null)
                        CompositionLocalProvider(
                            LocalNeoEditorWriteState provides NeoEditorWriteState(busy, failure)
                        ) {
                            TaskEditorDialogV11(null, state!!, workplaces, { finish() }) { task ->
                                if (!busy) {
                                    busy = true
                                    failure = null
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val runtime = V11Runtime(applicationContext)
                                                when (
                                                    val result =
                                                        NeoTaskCommit(
                                                                runtime::start,
                                                                { expected, next ->
                                                                    com.todoshift.app.backup
                                                                        .Neo4LocalBackup(
                                                                            applicationContext
                                                                        )
                                                                        .commitDatabase(
                                                                            expected,
                                                                            next,
                                                                        )
                                                                },
                                                                {
                                                                    com.todoshift.app.reminders
                                                                        .ToDoShiftReminder
                                                                        .schedule(
                                                                            applicationContext,
                                                                            it,
                                                                        )
                                                                },
                                                            )
                                                            .submit(task)
                                                ) {
                                                    NeoTaskCommit.Result.Conflict ->
                                                        error(
                                                            "Task ID already exists with different content; reopen before editing"
                                                        )
                                                    NeoTaskCommit.Result.Retry ->
                                                        error(
                                                            "Save not confirmed; draft retained. Retry safely."
                                                        )
                                                    is NeoTaskCommit.Result.Committed ->
                                                        check(!result.servicesPending) {
                                                            "Task saved; reminder refresh pending. Retry to refresh without creating another task."
                                                        }
                                                }
                                            }

                                            finishAfterSave(task)
                                        } catch (e: Exception) {
                                            failure = e.message ?: "Save failed"
                                            busy = false
                                        }
                                    }
                                }
                            }
                        }
                    else Surface { Box(Modifier.padding(24.dp)) { CircularProgressIndicator() } }
                }
            }
        }
    }
}
