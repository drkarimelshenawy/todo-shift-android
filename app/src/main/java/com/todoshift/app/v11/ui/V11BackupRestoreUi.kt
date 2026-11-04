package com.todoshift.app.v11.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.V11Storage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun V11BackupRestoreDialog(
    context: Context,
    db: V11Database,
    onDatabaseRestored: (V11Database) -> Unit,
    onDismiss: () -> Unit
) {
    val storage = remember(context) {
        V11Storage(context.applicationContext)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf(false)
    }

    fun writeBackup(uri: Uri) {
        runCatching {
            val raw = storage.exportJson(db)

            context.contentResolver
                .openOutputStream(uri, "wt")
                ?.bufferedWriter()
                ?.use {
                    it.write(raw)
                }
                ?: error(
                    "Unable to open backup destination"
                )
        }.onSuccess {
            error = false
            message =
                neoLiteral("تم إنشاء النسخة الاحتياطية بنجاح", "Backup created successfully")
        }.onFailure {
            error = true
            message =
                neoLiteral("فشل إنشاء النسخة الاحتياطية: ${it.message ?: "خطأ غير معروف"}", "Backup failed: ${it.message ?: "Unknown error"}")
        }
    }

    fun restoreBackup(uri: Uri) {
        runCatching {
            val raw =
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: error(
                        "Unable to read backup file"
                    )

            require(raw.isNotBlank()) {
                neoLiteral("ملف النسخة الاحتياطية فارغ", "Backup file is empty")
            }

            storage.importJson(raw)
            .also { onDatabaseRestored(it) }
        }.onSuccess {
            error = false
            message =
                neoLiteral("تمت استعادة النسخة الاحتياطية بنجاح", "Backup restored successfully")
        }.onFailure {
            error = true
            message =
                neoLiteral("فشل الاستعادة: ${it.message ?: "الملف غير صالح"}", "Restore failed: ${it.message ?: "Invalid file"}")
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/json"
                    )
        ) { uri ->
            if (uri != null) {
                writeBackup(uri)
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) { uri ->
            if (uri != null) {
                restoreBackup(uri)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                neoLiteral("النسخ الاحتياطي والاستعادة", "Backup and restore")
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Schema ${db.schemaVersion}",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    neoLiteral("النسخة تشمل التقاويم والشيفتات والحسابات والمدفوعات والالتزامات والمهام والإعدادات المخزنة داخل قاعدة بيانات ToDo Shift.", "The backup includes calendars, shifts, finances, payments, obligations, tasks and settings stored in the ToDo Shift database.")
                )

                Button(
                    onClick = {
                        val stamp =
                            LocalDateTime.now()
                                .format(
                                    DateTimeFormatter
                                        .ofPattern(
                                            "yyyyMMdd-HHmm"
                                        )
                                )

                        exportLauncher.launch(
                            "ToDo-Shift-$stamp.json"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Upload,
                        null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        neoLiteral("إنشاء نسخة احتياطية", "Create backup")
                    )
                }

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Download,
                        null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        neoLiteral("استعادة نسخة احتياطية", "Restore backup")
                    )
                }

                message?.let {
                    Text(
                        it,
                        color =
                            if (error)
                                MaterialTheme
                                    .colorScheme
                                    .error
                            else
                                MaterialTheme
                                    .colorScheme
                                    .primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(neoLiteral("إغلاق", "Close"))
            }
        }
    )
}
