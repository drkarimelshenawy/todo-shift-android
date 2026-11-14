package com.todoshift.app.integration

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoshift.cashbook.ui.CashBookRoute
import com.todoshift.cashbook.ui.CashBookViewModel
import java.time.DayOfWeek

/**
 * Embedded Cash Book destination for the host navigation graph.
 * Keeps the feature's ViewModel scoped to the host ViewModelStoreOwner.
 *
 * Reload on entry so persisted salary postings appear in the retained ViewModel.
 */
@Composable
fun EmbeddedCashBookScreen(
    weekStart: DayOfWeek = DayOfWeek.SATURDAY,
    salaryContent: (@Composable () -> Unit)? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: CashBookViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    LaunchedEffect(vm) { vm.reload() }
    val appearance = com.todoshift.app.v11.ui.LocalNeoAppearance.current
    val hostLocale = when (appearance.language) {
        com.todoshift.app.v11.NeoLanguage.ARABIC -> java.util.Locale("ar")
        com.todoshift.app.v11.NeoLanguage.ENGLISH -> java.util.Locale.ENGLISH
        else -> LocalContext.current.resources.configuration.locales[0]
    }
    CashBookRoute(vm = vm, weekStart = weekStart, hostLocale = hostLocale, salaryContent = salaryContent)
}
