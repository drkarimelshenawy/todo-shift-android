package com.todoshift.app.data

import java.util.UUID

object TemplateEngine {

    fun create(
        data: AppData,
        name: String,
        selection: PainterSelection
    ): AppData {

        val cleanName =
            name.trim()

        if (
            cleanName.isBlank()
        ) {
            return data
        }

        val template =
            ShiftTemplate(
                id =
                    UUID.randomUUID()
                        .toString(),
                name =
                    cleanName,
                type =
                    selection.type,
                workplaceIds =
                    selection.workplaceIds,
                customAmount =
                    selection.customAmount
            )

        return data.copy(
            templates =
                data.templates +
                    template
        )
    }

    fun update(
        data: AppData,
        template: ShiftTemplate
    ): AppData {

        return data.copy(
            templates =
                data.templates.map {
                    if (
                        it.id ==
                        template.id
                    ) {
                        template
                    } else {
                        it
                    }
                }
        )
    }

    fun delete(
        data: AppData,
        id: String
    ): AppData {

        return data.copy(
            templates =
                data.templates
                    .filterNot {
                        it.id == id
                    }
        )
    }

    fun selection(
        template: ShiftTemplate
    ): PainterSelection {

        return PainterSelection(
            type =
                template.type,
            workplaceIds =
                template.workplaceIds,
            customAmount =
                template.customAmount
        )
    }
}
