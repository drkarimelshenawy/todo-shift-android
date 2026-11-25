package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One + action replaces separate permanent image/audio/document buttons. */
@Composable
internal fun NeoNoteInsertMenu(
    enabled:Boolean,
    insertTable:()->Unit,
    insertImage:()->Unit,
    insertCamera:()->Unit,
    insertVoice:()->Unit,
    insertFile:()->Unit,
    insertPageBreak:()->Unit,
    openAttachments:()->Unit
) {
    var expanded by remember {mutableStateOf(false)}
    Box {
        IconButton(enabled=enabled,onClick={expanded=true},modifier=Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Add,neoLabel("إدراج داخل الملاحظة","Insert into note"))
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text(neoLabel("جدول قابل للتحرير","Editable table"))},
                onClick={expanded=false;insertTable()})
            DropdownMenuItem(text={Text(neoLabel("صورة داخل النص","Inline photo"))},
                onClick={expanded=false;insertImage()})
            DropdownMenuItem(text={Text(neoLabel("صورة بالكاميرا","Camera photo"))},
                onClick={expanded=false;insertCamera()})
            DropdownMenuItem(text={Text(neoLabel("تسجيل صوتي","Voice recording"))},
                onClick={expanded=false;insertVoice()})
            DropdownMenuItem(text={Text(neoLabel("ملف PDF أو مستند","File attachment"))},
                onClick={expanded=false;insertFile()})
            HorizontalDivider()
            DropdownMenuItem(text={Text(neoLabel("فاصل صفحة A4","A4 page break"))},
                onClick={expanded=false;insertPageBreak()})
            DropdownMenuItem(text={Text(neoLabel("مرفقات الملاحظة","Manage attachments"))},
                onClick={expanded=false;openAttachments()})
        }
    }
}
