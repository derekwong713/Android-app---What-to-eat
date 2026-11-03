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
 val vm=androidx.lifecycle.ViewModelProvider(ui.activity)[hk.whatoeat.feature.home.HomeViewModel::class.java]
 ui.waitUntil(15000) {vm.state.value.ready}
 if(vm.state.value.restored) ui.waitUntil(10000) {ui.onAllNodes(hasText("抽中菜式：",substring=true)).fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("首頁").performClick()
 ui.onAllNodesWithText("取得／重試目前位置").assertCountEquals(0)
 val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
 automation.serviceInfo=automation.serviceInfo.apply {flags=flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
 ui.onNodeWithText("距離、菜式、評分及地區").performScrollTo().performClick()
 ui.onNodeWithText("500 米內").performScrollTo().performClick()
 automation.takeScreenshot()?.let {bitmap ->
  java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"motion-filter.png").outputStream().use {
   bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
  };bitmap.recycle()
 }
 ui.onNodeWithText("取得／重試目前位置").performScrollTo().performClick()
 ui.waitUntil(30000) {
 deny(automation.rootInActiveWindow) || automation.windows.any {deny(it.root)} || runCatching {ui.onAllNodesWithText("不限距離繼續").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)
 }
 ui.waitUntil(10000) {ui.onAllNodesWithText("不限距離繼續").fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("重試定位").assertExists();ui.onNodeWithText("系統設定").assertExists()
 ui.onNodeWithText("不限距離繼續").performClick()
 ui.onNode(hasText("不限距離") and isSelected()).assertExists()
 ui.onNodeWithText("套用篩選").performScrollTo().performClick()
 ui.onNodeWithText("不限距離 · 所有已分類菜式 · 不限評分 · 所有地區").assertExists()
 ui.onAllNodes(hasText("定位時間",substring=true) or hasText("準確度約",substring=true)).assertCountEquals(0)
 }
}
