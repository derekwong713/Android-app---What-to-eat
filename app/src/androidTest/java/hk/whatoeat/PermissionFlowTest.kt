package hk.whatoeat
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
class PermissionFlowTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun deny(node: AccessibilityNodeInfo?): Boolean {
 if(node==null) return false
 if(node.viewIdResourceName?.endsWith("permission_deny_button")==true || (node.isClickable && node.text?.toString() in setOf("Don’t allow","Don't allow","不允許","不允许","拒絕"))) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
 for(i in 0 until node.childCount) if(deny(node.getChild(i))) return true
 return false
 }
 @Test fun realPermissionDenialOrPermanentDenialOffersUnlimitedFallback() {
 assumeTrue(BuildConfig.DEMO)
 ui.waitUntil(15000) {ui.onAllNodes(hasText("首頁") and isEnabled()).fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("首頁").performClick()
 val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
 automation.serviceInfo=automation.serviceInfo.apply {flags=flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
 ui.onNodeWithText("距離及菜式").performScrollTo().performClick();ui.onAllNodes(isSelectable())[1].performClick();ui.onNodeWithText("套用篩選").performScrollTo().performClick();ui.waitUntil(10000) {ui.onAllNodesWithText("500 米內 · 所有已分類菜式").fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("取得／重試目前位置").performScrollTo().performClick()
 ui.waitUntil(30000) {
 deny(automation.rootInActiveWindow) || automation.windows.any {deny(it.root)} || runCatching {ui.onAllNodesWithText("不限距離繼續").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)
 }
 ui.waitUntil(10000) {ui.onAllNodesWithText("不限距離繼續").fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("不限距離繼續").performClick();ui.waitUntil(10000) {ui.onAllNodesWithText("不限距離 · 所有已分類菜式").fetchSemanticsNodes().isNotEmpty()}
 }
}
