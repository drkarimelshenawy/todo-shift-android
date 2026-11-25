package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.MaterialTheme

/**
 * One canonical rich text state per paragraph. Multiple A4 views are only
 * clipped windows into the same paragraph, not independently saved fields.
 * This avoids splitting IME composition or writing synthetic page markers.
 */
internal class NeoA4EditingSession(source:String) {
    var state by mutableStateOf(NeoRichEditorState.open(source))
    var committed by mutableStateOf(state)
    var activeFragment by mutableIntStateOf(-1)
    var pendingHandoff by mutableStateOf(false)
}

@Composable
internal fun NeoA4SharedEditableSlice(
    block:NeoDocumentBlocks.Block.Paragraph,
    session:NeoA4EditingSession,
    direction:TextDirection,
    ruled:Boolean,
    arabicFont:String,
    englishFont:String,
    onEdited:(Int,String)->Unit,
    onActivated:((NeoRichEditorState,(NeoRichEditorState)->Unit)->Unit)?,
    onError:(String)->Unit
) {
    val edited by rememberUpdatedState(onEdited)
    val activated by rememberUpdatedState(onActivated)
    val errors by rememberUpdatedState(onError)
    val requester=remember(block.startLine,block.pageFragment){FocusRequester()}
    val index=block.pageFragment
    val last=block.sliceBottomPx>=block.wholeHeightPx
    val caret=session.state.value.selection.start
    val caretOnThisSheet=caret>=block.sliceStartOffset &&
        (caret<block.sliceEndOffset || (last && caret<=block.sliceEndOffset))

    fun apply(next:NeoRichEditorState) {
        val before=session.state
        session.state=next
        if(next.value.composition==null) {
            session.committed=next
            session.pendingHandoff=(next.value.text!=before.value.text ||
                before.value.composition!=null) && next.value.selection.collapsed
            runCatching {edited(block.startLine,next.stored())}
                .onFailure {errors(it.message ?: "Could not save paragraph")}
        } else {
            // Freeze layout throughout keyboard-owned Arabic composition.
            session.pendingHandoff=false
        }
    }

    LaunchedEffect(block.source) {
        if(session.activeFragment<0 && session.state.value.composition==null &&
            session.state.stored()!=block.source) {
            val fresh=NeoRichEditorState.open(block.source)
            session.state=fresh
            session.committed=fresh
        }
    }

    LaunchedEffect(session.activeFragment,session.state) {
        if(session.activeFragment==index)
            activated?.invoke(session.state) { updated->apply(updated) }
    }

    LaunchedEffect(session.pendingHandoff,caretOnThisSheet,
        session.state.value.composition,session.activeFragment) {
        if(session.pendingHandoff && caretOnThisSheet &&
            session.state.value.composition==null && session.activeFragment!=index)
            requester.requestFocus()
    }

    val density=LocalDensity.current
    val height=with(density) { (block.sliceBottomPx-block.sliceTopPx)
        .coerceAtLeast(1).toDp() }
    val fullHeight=with(density) { (block.wholeHeightPx+8).coerceAtLeast(18).toDp() }
    val rtl=direction!=TextDirection.Ltr
    var measuredLines by remember {mutableStateOf<List<Float>>(emptyList())}

    CompositionLocalProvider(LocalLayoutDirection provides
        if(rtl) androidx.compose.ui.unit.LayoutDirection.Rtl
        else androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
            BasicTextField(
                value=session.state.value,
                onValueChange={next->apply(session.state.input(next))},
                modifier=Modifier.fillMaxWidth()
                    .testTag("a4_note_slice_${block.startLine}_${block.pageFragment}")
                    // The field must be top-aligned when taller than the
                    // clipped sheet; default requiredHeight centers overflow.
                    .wrapContentHeight(align=Alignment.Top,unbounded=true)
                    .offset{IntOffset(0,-block.sliceTopPx)}
                    .requiredHeight(fullHeight)
                    .focusRequester(requester)
                    .onFocusChanged { focus ->
                        if(focus.isFocused) {
                            session.activeFragment=index
                            session.pendingHandoff=false
                        } else if(session.activeFragment==index) {
                            session.activeFragment=-1
                            if(session.state.stored()!=block.source) {
                                runCatching { edited(block.startLine,session.state.stored()) }
                                    .onFailure { errors(it.message ?: "Could not save paragraph") }
                            }
                        }
                    }
                    .drawBehind {
                        if(ruled) measuredLines.forEach { bottom ->
                            if(bottom<=size.height) drawLine(
                                Color.Gray.copy(alpha=0.27f),
                                androidx.compose.ui.geometry.Offset(0f,bottom),
                                androidx.compose.ui.geometry.Offset(size.width,bottom),
                                strokeWidth=0.7.dp.toPx()
                            )
                        }
                    },
                onTextLayout={result->
                    val next=(0 until result.lineCount).map{result.getLineBottom(it)}
                    if(measuredLines!=next)measuredLines=next
                },
                textStyle=MaterialTheme.typography.bodyLarge.copy(
                    color=Color(0xFF202126),fontSize=NeoA4PrintMetrics.BODY_PT.sp,
                    lineHeight=NeoA4PrintMetrics.BODY_LINE_PT.sp,
                    textDirection=direction,
                    textAlign=if(rtl)TextAlign.Right else TextAlign.Left),
                visualTransformation=NeoNoteTypefaces.richVisual(
                    session.state.characters,arabicFont,englishFont),
                cursorBrush=androidx.compose.ui.graphics.SolidColor(Color(0xFF174EA6))
            )
        }
    }
}
