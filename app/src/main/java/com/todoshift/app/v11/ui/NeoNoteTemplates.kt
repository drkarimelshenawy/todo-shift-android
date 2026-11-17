package com.todoshift.app.v11.ui

/**
 * Optional starting content, not separate note types. Templates always create
 * the same regular rich note; no template ID is stored in the user's database.
 */
internal object NeoNoteTemplates {
    data class Template(
        val id:String,
        val titleAr:String,
        val titleEn:String,
        val bodyAr:String,
        val bodyEn:String
    ) {
        fun title(ar:Boolean)=if(ar) titleAr else titleEn
        fun body(ar:Boolean)=if(ar) bodyAr else bodyEn
    }

    val builtIn:List<Template> = listOf(
        Template(
            "handover","تسليم واستلام شيفت","Shift handover",
            "# تسليم واستلام الشيفت\nالتاريخ:\nمكان العمل:\n\n## أهم الحالات والمستجدات\n\n## إجراءات مطلوبة\n☐ \n\n## ملاحظات للزميل التالي\n",
            "# Shift handover\nDate:\nWorkplace:\n\n## Key updates\n\n## Follow-up actions\n☐ \n\n## Notes for the next shift\n"
        ),
        Template(
            "work","متابعة العمل","Work follow-up",
            "# متابعة العمل\nالمكان:\n\n## المطلوب\n☐ \n\n## ما تم إنجازه\n\n## ملاحظات\n",
            "# Work follow-up\nLocation:\n\n## To do\n☐ \n\n## Completed\n\n## Notes\n"
        ),
        Template(
            "shopping","قائمة مشتريات","Shopping list",
            "# قائمة المشتريات\n☐ \n\n## ملاحظات وأسعار\n",
            "# Shopping list\n☐ \n\n## Notes and prices\n"
        ),
        Template(
            "meeting","محضر اجتماع","Meeting notes",
            "# اجتماع\nالتاريخ:\nالحضور:\n\n## الموضوعات\n\n## القرارات\n\n## إجراءات المتابعة\n☐ \n",
            "# Meeting\nDate:\nAttendees:\n\n## Agenda\n\n## Decisions\n\n## Action items\n☐ \n"
        ),
        Template(
            "daily","مذكرة يومية","Daily reflection",
            "# يومي\n\n## أهم ما حدث\n\n## أفكار\n\n## للمتابعة\n☐ \n",
            "# My day\n\n## Highlights\n\n## Ideas\n\n## Follow-up\n☐ \n"
        ),
        Template(
            "finance","ملاحظات مالية","Finance notes",
            "# ملاحظات مالية\nالفترة:\n\n## بنود تحتاج مراجعة\n☐ \n\n## تفاصيل وملاحظات\n\n## متابعة\n",
            "# Finance notes\nPeriod:\n\n## Items to review\n☐ \n\n## Details and notes\n\n## Follow-up\n"
        )
    )

    fun byId(id:String?):Template?=builtIn.firstOrNull {it.id==id}
}
