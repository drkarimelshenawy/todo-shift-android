package com.todoshift.app.data

import java.time.LocalDate
import java.time.YearMonth

enum class PaymentStatus {
    NOT_PAID,
    PARTIALLY_PAID,
    PAID,
    OVERPAID
}

data class FinanceSettlement(
    val month: YearMonth,
    val expected: Double,
    val received: Double,
    val difference: Double,
    val status: PaymentStatus
)

object FinanceV2 {

    fun settlement(
        data: AppData,
        month: YearMonth
    ): FinanceSettlement {

        val report =
            FinanceEngine.month(
                data,
                month
            )

        val status =
            when {
                report.received <= 0.0 &&
                    report.expected > 0.0 ->
                    PaymentStatus.NOT_PAID

                report.received <
                    report.expected ->
                    PaymentStatus.PARTIALLY_PAID

                report.received ==
                    report.expected ->
                    PaymentStatus.PAID

                report.received >
                    report.expected ->
                    PaymentStatus.OVERPAID

                else ->
                    PaymentStatus.PAID
            }

        return FinanceSettlement(
            month = month,
            expected = report.expected,
            received = report.received,
            difference =
                report.received -
                    report.expected,
            status = status
        )
    }

    fun statusArabic(
        status: PaymentStatus
    ): String =
        when (status) {
            PaymentStatus.NOT_PAID ->
                "لم يتم السداد"

            PaymentStatus.PARTIALLY_PAID ->
                "سداد جزئي"

            PaymentStatus.PAID ->
                "تم السداد بالكامل"

            PaymentStatus.OVERPAID ->
                "زيادة في السداد"
        }

    fun payments(
        data: AppData,
        month: YearMonth
    ): List<Payment> =
        data.payments
            .filter {
                YearMonth.from(
                    LocalDate.parse(
                        it.date
                    )
                ) == month
            }
            .sortedByDescending {
                it.date
            }

    fun deletePayment(
        data: AppData,
        paymentId: String
    ): AppData =
        data.copy(
            payments =
                data.payments.filterNot {
                    it.id == paymentId
                }
        )
}
