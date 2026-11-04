package com.todoshift.app.backup

import com.todoshift.app.data.AppData
import com.todoshift.app.v11.*

data class Neo4BootState(val database: V11Database, val legacy: AppData, val ui: V11UiSettings,
    val appearance: NeoAppearance, val security: V11SecuritySettings, val activeCalendarId: String?)
