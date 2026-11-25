package com.exmore.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class WorkplaceCompensationValidationTest {
    @Test fun monthly8400RemainsValidEvenWithBlankHiddenRates() {
        assertTrue(workplaceCompensationInputsValid(true,"8400",listOf("","","","","")))
    }

    @Test fun monthlySalaryMustBeAValidNonNegativeAmount() {
        listOf("","-1","invalid").forEach {
            assertFalse(workplaceCompensationInputsValid(true,it,listOf("","","","","")))
        }
    }

    @Test fun perShiftRequiresAllVisibleRatesButNotASalary() {
        assertFalse(workplaceCompensationInputsValid(false,"",listOf("200","","100","50","50")))
        assertTrue(workplaceCompensationInputsValid(false,"",listOf("200","0","100","50","50")))
    }
}
