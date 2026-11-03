package com.todoshift.app.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(
        intent: Intent
    ): RemoteViewsFactory =
        TasksWidgetFactory(
            applicationContext,
            intent
        )
}
