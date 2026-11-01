package com.todoshift.app.widget

import android.content.Intent
import android.widget.RemoteViewsService

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(
        intent: Intent
    ): RemoteViewsFactory {
        return CalendarWidgetFactory(
            applicationContext,
            intent
        )
    }
}
