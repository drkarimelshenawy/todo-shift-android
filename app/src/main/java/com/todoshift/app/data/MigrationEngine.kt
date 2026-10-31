package com.todoshift.app.data

import android.content.Context

object MigrationEngine {

    private const val PREFS =
        "todo_shift_migrations"

    private const val KEY_VERSION =
        "schema_version"

    const val CURRENT_SCHEMA = 1

    fun migrate(
        context: Context,
        data: AppData
    ): AppData {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val oldVersion =
            prefs.getInt(
                KEY_VERSION,
                0
            )

        var migrated = data

        if (oldVersion < 1) {

            migrated =
                migrated.copy(
                    workplaces =
                        migrated.workplaces
                            .distinctBy {
                                it.id
                            },

                    shifts =
                        migrated.shifts
                            .distinctBy {
                                it.date
                            }
                            .sortedBy {
                                it.date
                            },

                    payments =
                        migrated.payments
                            .distinctBy {
                                it.id
                            },

                    templates =
                        migrated.templates
                            .distinctBy {
                                it.id
                            }
                )
        }

        prefs.edit()
            .putInt(
                KEY_VERSION,
                CURRENT_SCHEMA
            )
            .apply()

        return migrated
    }
}
