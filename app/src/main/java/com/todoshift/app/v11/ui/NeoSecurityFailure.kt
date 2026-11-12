package com.todoshift.app.v11.ui

import com.todoshift.app.v11.V11SecurityStore

internal fun neoSecurityFailure(store: V11SecurityStore, incorrect: String): String {
    val seconds = (store.verificationDelayMillis() + 999L) / 1000L
    return if (seconds > 0L)
        neoLiteral("محاولات كثيرة. انتظر $seconds ثانية ثم أعد المحاولة", "Too many attempts. Wait $seconds seconds and try again")
    else incorrect
}
