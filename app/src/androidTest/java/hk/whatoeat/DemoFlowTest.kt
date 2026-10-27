package hk.whatoeat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
class DemoFlowTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun guestDrawCuisineThenRestaurantAndReroll() {
 assumeTrue(BuildConfig.DEMO)
 ui.waitUntil(15000) {ui.onAllNodes(hasText("首頁") and isEnabled()).fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("首頁").performClick()
 ui.onNodeWithText("距離及菜式").performScrollTo().performClick();ui.onAllNodes(isSelectable())[0].performClick();ui.onNodeWithText("所有菜式／清除已選").performScrollTo().performClick();ui.onNodeWithText("套用篩選").performScrollTo().performClick()
 ui.waitUntil(10000) {ui.onAllNodes(hasText("重新載入完整候選") and isEnabled()).fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("重新載入完整候選").performScrollTo().performClick()
 ui.waitUntil(10000) {ui.onAllNodesWithText("符合條件：3 間餐廳、3 種菜式").fetchSemanticsNodes().isNotEmpty() && ui.onAllNodes(hasText("抽菜式，再抽餐廳") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("抽菜式，再抽餐廳").performScrollTo().performClick();ui.waitUntil(15000) {ui.onAllNodes(hasText("重抽餐廳") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("重抽餐廳").performScrollTo().performClick();ui.waitUntil(10000) {ui.onAllNodes(hasText("重抽菜式") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
 }

 @Test fun guestWriteOpRequestsOnlySocialLogin() {assumeTrue(BuildConfig.DEMO);ui.waitUntil(15000) {ui.onAllNodesWithText("帳戶").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("帳戶").performClick();ui.onNodeWithText("登入 Google／Facebook").performClick();ui.onNodeWithText("模擬 Google 登入").assertExists();ui.onNodeWithText("模擬 Facebook 登入").assertExists();ui.onNodeWithText("取消").performClick()}
}
