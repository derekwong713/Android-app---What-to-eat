package hk.whatoeat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.designsystem.EatTheme
import hk.whatoeat.core.model.*
import hk.whatoeat.feature.filter.FilterSheet
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccessibilityTest {
    @get:Rule val rule=createComposeRule()
    @Test fun filterActionsRemainReachableAtLargeFontAndEmitDraftOnlyOnApply() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        var applied: Filters?=null
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) {
                EatTheme {FilterSheet(Filters(),{}, {applied=it})}
            }
        }
        rule.onNodeWithText(context.getString(R.string.allergen_milk)).performScrollTo().performClick()
        assertNull(applied)
        rule.onNodeWithText(context.getString(R.string.apply)).performScrollTo().assertHasClickAction().performClick()
        rule.runOnIdle {assertEquals(setOf(Allergen.Milk),applied?.avoid)}
    }
}
