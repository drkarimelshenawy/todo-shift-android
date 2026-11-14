package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.todoshift.app.v11.*

private val LocalTaskEditorAuthorized=staticCompositionLocalOf {false}

/** All task editor entry paths share the same persistent section policy. */
@Composable
internal fun NeoTaskEditorGate(onDismiss:()->Unit, sourceProtected:Boolean=false, content:@Composable ()->Unit) {
    val context=LocalContext.current
    val security=remember {V11SecurityStore(context.applicationContext)}
    val settings=security.load()
    val required=settings.protectTasks||settings.protectApplication||sourceProtected
    val inherited=LocalTaskEditorAuthorized.current
    var authorized by remember(required) {mutableStateOf(!required||inherited)}
    val state=rememberSaveableStateHolder()
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,required) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP&&required)authorized=false}
        lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
    }
    if(!authorized) {
        Dialog(onDismissRequest=onDismiss,properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn)) {
            if(settings.mode==V11LockMode.NONE&&!settings.biometricEnabled) Surface {Text(neoLabel("أعد إعداد المصادقة من الإعدادات","Configure authentication in Settings"))}
            else V11AccountsUnlockScreen(settings,security){authorized=true}
        }
    } else state.SaveableStateProvider("task-editor") {CompositionLocalProvider(LocalTaskEditorAuthorized provides true,content=content)}
}

@Composable
internal fun neoTaskDialogProperties(sourceProtected:Boolean=false):DialogProperties {
    val security=V11SecurityStore(LocalContext.current.applicationContext).load()
    return DialogProperties(securePolicy=if(security.protectTasks||security.protectApplication||sourceProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit)
}
