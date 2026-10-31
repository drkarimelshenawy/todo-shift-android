package com.todoshift.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupEngine {

    const val BACKUP_VERSION = 1

    fun export(
        data: AppData
    ): String {

        val root = JSONObject()

        root.put(
            "backupVersion",
            BACKUP_VERSION
        )

        root.put(
            "app",
            "ToDo Shift"
        )

        val workplaces =
            JSONArray()

        data.workplaces.forEach { p ->
            workplaces.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put(
                        "colorHex",
                        p.colorHex
                    )
                    .put(
                        "fullDayRate",
                        p.fullDayRate
                    )
                    .put(
                        "longRate",
                        p.longRate
                    )
                    .put(
                        "nightRate",
                        p.nightRate
                    )
                    .put(
                        "morningRate",
                        p.morningRate
                    )
                    .put(
                        "eveningRate",
                        p.eveningRate
                    )
            )
        }

        root.put(
            "workplaces",
            workplaces
        )

        val shifts =
            JSONArray()

        data.shifts.forEach { sh ->

            val ids =
                JSONArray()

            sh.workplaceIds
                .forEach {
                    ids.put(
                        it ?: JSONObject.NULL
                    )
                }

            val obj =
                JSONObject()
                    .put(
                        "date",
                        sh.date
                    )
                    .put(
                        "type",
                        sh.type.name
                    )
                    .put(
                        "workplaceIds",
                        ids
                    )
                    .put(
                        "note",
                        sh.note
                    )

            if (
                sh.customAmount != null
            ) {
                obj.put(
                    "customAmount",
                    sh.customAmount
                )
            }

            shifts.put(obj)
        }

        root.put(
            "shifts",
            shifts
        )

        val payments =
            JSONArray()

        data.payments.forEach { pay ->
            payments.put(
                JSONObject()
                    .put("id", pay.id)
                    .put(
                        "date",
                        pay.date
                    )
                    .put(
                        "amount",
                        pay.amount
                    )
                    .put(
                        "note",
                        pay.note
                    )
            )
        }

        root.put(
            "payments",
            payments
        )

        val templates =
            JSONArray()

        data.templates.forEach { t ->

            val ids =
                JSONArray()

            t.workplaceIds
                .forEach {
                    ids.put(
                        it ?: JSONObject.NULL
                    )
                }

            val obj =
                JSONObject()
                    .put(
                        "id",
                        t.id
                    )
                    .put(
                        "name",
                        t.name
                    )
                    .put(
                        "type",
                        t.type.name
                    )
                    .put(
                        "workplaceIds",
                        ids
                    )

            if (
                t.customAmount != null
            ) {
                obj.put(
                    "customAmount",
                    t.customAmount
                )
            }

            templates.put(obj)
        }

        root.put(
            "templates",
            templates
        )

        return root.toString(2)
    }

    fun saveInternalBackup(
        context: Context,
        data: AppData
    ): String {

        val file =
            context.filesDir.resolve(
                "todo_shift_backup.json"
            )

        file.writeText(
            export(data)
        )

        return file.absolutePath
    }
}
