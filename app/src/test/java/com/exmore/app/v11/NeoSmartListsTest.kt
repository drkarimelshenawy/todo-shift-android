package com.exmore.app.v11

import androidx.compose.ui.text.TextRange
import com.exmore.app.v11.ui.NeoSmartLists
import org.junit.Assert.*
import org.junit.Test

class NeoSmartListsTest {
    @Test fun toggleStartsAndStopsActiveNumbering() {
        val on=NeoSmartLists.toggle("",TextRange(0),"number")
        assertEquals("1. ",on.text)
        assertTrue(NeoSmartLists.active(on.text,on.caret,"number"))
        assertEquals("",NeoSmartLists.toggle(on.text,TextRange(on.caret),"number").text)
    }

    @Test fun enterContinuesNumbersAndExitsOnEmptyItem() {
        val first="1. مهمة"
        val next=NeoSmartLists.onKeyboardEdit(first,first+"\n",(first+"\n").length)
        assertEquals("1. مهمة\n2. ",next!!.text)
        val populated=next.text+"مهمة ثانية"
        val third=NeoSmartLists.onKeyboardEdit(populated,populated+"\n",(populated+"\n").length)
        assertEquals("1. مهمة\n2. مهمة ثانية\n3. ",third!!.text)
        val exit=NeoSmartLists.onKeyboardEdit(third.text,third.text+"\n",(third.text+"\n").length)
        assertEquals("1. مهمة\n2. مهمة ثانية\n",exit!!.text)
    }

    @Test fun middleEnterResequencesFollowingRows() {
        val before="1. first\n2. second"
        val edit=NeoSmartLists.onKeyboardEdit(before,"1. first\n\n2. second","1. first\n".length)
        assertEquals("1. first\n2. \n3. second",edit!!.text)
    }

    @Test fun alphaNumbersSupportZToAAAndParentheses() {
        val old="Z. item"
        assertEquals("Z. item\nAA. ",NeoSmartLists.onKeyboardEdit(old,old+"\n",(old+"\n").length)!!.text)
        val par="1) item"
        assertEquals("1) item\n2) ",NeoSmartLists.onKeyboardEdit(par,par+"\n",(par+"\n").length)!!.text)
    }

    @Test fun arabicLettersAndAlternateBulletStylesContinue() {
        assertEquals("أ. أول\nب. ",NeoSmartLists.onKeyboardEdit("أ. أول","أ. أول\n","أ. أول\n".length)!!.text)
        assertEquals("▪ واحد\n▪ ",NeoSmartLists.onKeyboardEdit("▪ واحد","▪ واحد\n","▪ واحد\n".length)!!.text)
        assertEquals("◦ واحد\n◦ ",NeoSmartLists.onKeyboardEdit("◦ واحد","◦ واحد\n","◦ واحد\n".length)!!.text)
    }

    @Test fun newChecklistUsesSquareAndOldChecklistRemainsSupported() {
        val entry=NeoSmartLists.toggle("شراء دواء",TextRange(0),"check")
        assertEquals("☐ شراء دواء",entry.text)
        assertEquals("☑ شراء دواء",NeoSmartLists.toggleChecked(entry.text))
        assertEquals("- [x] بند قديم",NeoSmartLists.toggleChecked("- [ ] بند قديم"))
        assertEquals("- [ ] بند قديم",NeoSmartLists.toggleChecked("- [x] بند قديم"))
        assertEquals("☐ مهمة\n☐ ",NeoSmartLists.onKeyboardEdit("☐ مهمة","☐ مهمة\n","☐ مهمة\n".length)!!.text)
    }

    @Test fun selectionSwitchesOnlySelectedLinesAndKeepsText() {
        val original="السطر الأول\nالسطر الثاني\nبدون تغيير"
        val end=original.indexOf("\nبدون")
        val changed=NeoSmartLists.toggle(original,TextRange(0,end),"number")
        assertEquals("1. السطر الأول\n2. السطر الثاني\nبدون تغيير",changed.text)
        val again=NeoSmartLists.toggle(changed.text,TextRange(0,changed.text.indexOf("\nبدون")),"number")
        assertEquals(original,again.text)
    }

    @Test fun nonListTypingDoesNotGetAutomaticNumbers() {
        assertNull(NeoSmartLists.onKeyboardEdit("مرحبًا","مرحبًا\n","مرحبًا\n".length))
        assertNull(NeoSmartLists.onKeyboardEdit("1. one","1. oneX","1. oneX".length))
    }

    @Test fun completedRowsMoveToBottomOfEachBlockWithoutMovingLinkedTaskIds() {
        val original="☑ Done <!--task:id-done-->\n☐ Pending <!--task:id-pending-->\nParagraph\n- [x] Old task\n- [ ] New task"
        val actual=NeoSmartLists.moveCompletedToEnd(original)
        assertEquals("☐ Pending <!--task:id-pending-->\n☑ Done <!--task:id-done-->\nParagraph\n- [ ] New task\n- [x] Old task",actual)
        assertEquals(actual,NeoSmartLists.moveCompletedToEnd(actual))
        assertEquals(original,NeoSmartLists.toggleChecklistLine(original,100,true))
    }

    @Test fun tickingCheckboxReordersOnlyIfRequested() {
        val source="☐ First\n☐ Second\nNote footer"
        assertEquals("☑ First\n☐ Second\nNote footer",NeoSmartLists.toggleChecklistLine(source,0))
        assertEquals("☐ Second\n☑ First\nNote footer",NeoSmartLists.toggleChecklistLine(source,0,true))
        assertEquals("☐ First\n☐ Second\nNote footer",
            NeoSmartLists.toggleChecklistLine("☑ First\n☐ Second\nNote footer",0,true))
    }
}
