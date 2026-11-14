package com.todoshift.app.integration

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoshift.cashbook.ui.CashBookRoute
import com.todoshift.cashbook.ui.CashBookViewModel
import java.time.DayOfWeek

/**
 * Embedded Cash Book destination for the host navigation graph.
 * Keeps the feature's ViewModel scoped to the host ViewModelStoreOwner.
 *
 * Wired to the Cash Book tab. Salary posting remains disabled until the
 * two-store transaction and backup/recovery contracts are implemented.
 */
@Composable
fun EmbeddedCashBookScreen(
    weekStart: DayOfWeek = DayOfWeek.SATURDAY,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: CashBookViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    CashBookRoute(vm = vm, weekStart = weekStart)
}
