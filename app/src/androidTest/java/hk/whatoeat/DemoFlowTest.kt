package hk.whatoeat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before

/** Run against demoDebug on a clean app install / cleared app data. */
class DemoFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun text(id: Int)=rule.activity.getString(id)
    @Before fun clearDemoData() {
        rule.waitForIdle()
        // Close any result preserved by a prior test before opening settings.
        if(rule.onAllNodesWithText(text(R.string.close)).fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText(text(R.string.close)).performScrollTo().performClick()
        rule.onNodeWithContentDescription(text(R.string.settings)).performClick()
        rule.onNodeWithText(text(R.string.clear_local_data)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.confirm)).performClick()
        rule.onNodeWithText(text(R.string.home)).performClick()
    }
    @Test fun manualDistrictDirectDrawFavoritesAndHistory() {
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.find_nearby)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.district_sha_tin)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.find_nearby)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.pick)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.pick)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.result_title)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.favorite_add)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.close)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.history)).performClick()
        rule.onAllNodesWithText(text(R.string.saved_placeholder)).onFirst().assertExists()
    }
    @Test fun zeroResultsCanRecoverThroughAnotherDistrict() {
        rule.onNodeWithText(text(R.string.district_tsuen_wan)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.find_nearby)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.no_restaurants)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.district_sha_tin)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.pick)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.pick)).assertIsEnabled()
    }
    @Test fun conservativeUnknownIsExcludedWithoutRelaxation() {
        rule.onNodeWithText(text(R.string.district_sha_tin)).performScrollTo().performClick()
        rule.onAllNodesWithText(text(R.string.filters)).onLast().performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.allergen_milk)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.conservative_mode)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.apply)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.find_nearby)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.no_conservative)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.pick)).performScrollTo().assertIsNotEnabled()
    }
    @Test fun wheelSkipRevealsPreparedWinner() {
        rule.onNodeWithText(text(R.string.wheel)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.find_nearby)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.pick)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.pick)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.skip_animation)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.skip_animation)).performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText(text(R.string.result_title)).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(text(R.string.result_title)).assertExists()
    }
}
