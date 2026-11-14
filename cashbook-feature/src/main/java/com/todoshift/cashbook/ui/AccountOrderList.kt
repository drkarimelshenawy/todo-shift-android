package com.todoshift.cashbook.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.todoshift.cashbook.R
import com.todoshift.cashbook.core.Ledger
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Local drag preview; a complete order is committed once on release, never per pixel. */
@OptIn(ExperimentalFoundationApi::class)
@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun AccountOrderList(
    accounts: List<Ledger.Account>, active: String?, busy: Boolean, error: String?, editing: Boolean, query: String,
    modifier: Modifier = Modifier, select: (String) -> Unit, rename: (Ledger.Account) -> Unit,
    reorder: (List<String>) -> Unit
) {
    var order by remember(accounts) { mutableStateOf(accounts) }
    var dragged by remember { mutableStateOf<String?>(null) }
    var dragTop by remember { mutableFloatStateOf(0f) }
    var dragHeight by remember { mutableFloatStateOf(0f) }
    val list = rememberLazyListState()
    val density = LocalDensity.current
    val edge = with(density) { 48.dp.toPx() }
    val maxStep = with(density) { 12.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val canDrag = editing && !busy && query.isBlank()
    val visible = order.filter { (it.name + it.icon).contains(query.trim(), ignoreCase = true) }
    // A failed disk write leaves the VM's snapshot untouched and restores that order here.
    LaunchedEffect(accounts, busy, error, editing, query) {
        if (!busy && dragged == null) order = accounts
    }
    fun move(from: Int, to: Int) {
        if (from == to || from !in order.indices || to !in order.indices) return
        val first = list.firstVisibleItemIndex
        val offset = list.firstVisibleItemScrollOffset
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        if (from == first || to == first) list.requestScrollToItem(first, offset)
    }
    fun crossRow() {
        val id = dragged ?: return
        val info = list.layoutInfo
        val from = order.indexOfFirst { it.id == id }
        // Wait for the last move to be measured before considering another crossing.
        if (info.visibleItemsInfo.firstOrNull { it.key == id }?.index != from) return
        if (info.viewportEndOffset <= info.viewportStartOffset) return
        val center = (dragTop + dragHeight / 2).coerceIn(info.viewportStartOffset + 1f, info.viewportEndOffset - 1f)
        val target = info.visibleItemsInfo.firstOrNull {
            it.key != id && center >= it.offset && center < it.offset + it.size
        } ?: return
        move(from, order.indexOfFirst { it.id == target.key })
    }
    fun finish(cancel: Boolean) {
        if (dragged == null) return
        dragged = null
        if (cancel) order = accounts
        else if (order.map { it.id } != accounts.map { it.id }) reorder(order.map { it.id })
    }
    fun accessibleMove(id: String, step: Int): Boolean {
        if (!canDrag || dragged != null) return false
        val from = order.indexOfFirst { it.id == id }
        val to = from + step
        if (from !in order.indices || to !in order.indices) return false
        move(from, to)
        reorder(order.map { it.id })
        return true
    }
    // Continue scrolling while held at either edge, including when the finger stops moving.
    LaunchedEffect(dragged) {
        if (dragged == null) return@LaunchedEffect
        while (isActive) {
            val info = list.layoutInfo
            val center = dragTop + dragHeight / 2
            val delta = when {
                center < info.viewportStartOffset + edge -> -((info.viewportStartOffset + edge - center) / edge).coerceIn(0f, 1f) * maxStep
                center > info.viewportEndOffset - edge -> ((center - info.viewportEndOffset + edge) / edge).coerceIn(0f, 1f) * maxStep
                else -> 0f
            }
            if ((delta < 0f && list.canScrollBackward) || (delta > 0f && list.canScrollForward)) { list.scrollBy(delta); crossRow() }
            delay(16)
        }
    }
    LazyColumn(modifier.testTag("accounts_list"), state = list) {
        if (visible.isEmpty()) item { Text(tr(R.string.cb_167), Modifier.padding(12.dp)) }
        items(visible, key = { it.id }) { account ->
            val moving = dragged == account.id
            val position = order.indexOfFirst { it.id == account.id }
            val animation = if (moving) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(130))
            Column(animation.zIndex(if (moving) 1f else 0f).graphicsLayer {
                translationY = if (moving) dragTop - (list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == account.id }?.offset ?: dragTop.toInt()) else 0f
            }.then(if (moving) Modifier.shadow(3.dp) else Modifier).background(if (moving) Color(0xFFE3F2FD) else Color.White)) {
                Row(Modifier.testTag("account_row_" + account.id).fillMaxWidth().heightIn(min = if (editing) 48.dp else 42.dp)
                    .semantics { stateDescription = tr(R.string.cb_168 ,position + 1,order.size) }
                    .clickable(enabled = !busy) { if (dragged == null) { if (editing) rename(account) else select(account.id) } },
                    verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        Box(Modifier.size(48.dp).testTag("account_drag_" + account.id).semantics {
                            contentDescription = tr(R.string.cb_169 ,account.name)
                            if (canDrag) customActions = buildList {
                                if (position > 0) add(CustomAccessibilityAction(tr(R.string.cb_170)) { accessibleMove(account.id, -1) })
                                if (position < order.lastIndex) add(CustomAccessibilityAction(tr(R.string.cb_171)) { accessibleMove(account.id, 1) })
                            }
                        }.pointerInput(account.id, canDrag) {
                            if (canDrag) detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == account.id }?.let { info ->
                                        dragged = account.id; dragTop = info.offset.toFloat(); dragHeight = info.size.toFloat()
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, amount -> change.consume(); dragTop += amount.y; crossRow() },
                                onDragEnd = { finish(false) }, onDragCancel = { finish(true) }
                            )
                        }, contentAlignment = Alignment.Center) {
                            RefIcon(R.drawable.cb_drag, null, tint = if (canDrag) ReferenceBlue else Color.LightGray)
                        }
                    }
                    if(account.id==active)Text("✓",Modifier.padding(horizontal=6.dp).testTag("active_account"),color=ReferenceBlue)
                    AccountName(account,Modifier.weight(1f).padding(horizontal=6.dp,vertical=10.dp))
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE8E8E8))
            }
        }
    }
}
