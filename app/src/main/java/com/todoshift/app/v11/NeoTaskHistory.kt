package com.todoshift.app.v11

/** Completion is a durable event, including reopening and every later recompletion. */
fun TaskV11.withCompletionV11(completed: Boolean, at: Long = System.currentTimeMillis()): TaskV11 {
    if ((status == TaskStatusV11.COMPLETED) == completed) return this
    val historical = if(completionHistory.isEmpty() && completedAt != null) listOf(TaskCompletionV11(completedAt,true)) else completionHistory
    return copy(status=if(completed) TaskStatusV11.COMPLETED else TaskStatusV11.TODO,
        completedAt=if(completed) at else null,updatedAt=at,
        completionHistory=historical+TaskCompletionV11(at,completed))
}
