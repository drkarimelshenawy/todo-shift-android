package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Editor-only full-height sheet; application navigation and identity are unchanged. */
@Composable
internal fun NeoNoteEditorFrame(containerColor:Color,textContentColor:Color,onDismissRequest:()->Unit,properties:DialogProperties,
    title:@Composable ()->Unit,text:@Composable ()->Unit,confirmButton:@Composable ()->Unit,dismissButton:@Composable ()->Unit) {
    Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnClickOutside=false,securePolicy=properties.securePolicy)) {
        Surface(Modifier.fillMaxSize(),color=containerColor,contentColor=textContentColor) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal=12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)){title()};confirmButton();dismissButton()
                }
                HorizontalDivider()
                Box(Modifier.weight(1f).fillMaxWidth().padding(top=8.dp)){text()}
            }
        }
    }
}
