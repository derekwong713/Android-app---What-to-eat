package hk.whatoeat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import hk.whatoeat.core.designsystem.EatTheme
import hk.whatoeat.core.model.Filters
import hk.whatoeat.core.model.Cuisine
import hk.whatoeat.feature.filter.FilterSheet
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LocationFilterUiTest {
 @get:Rule val ui=createComposeRule()
 @Test fun requestingPositionClearsAllDistrictsAndPlacesEvenWithoutReturnedFix() {
  var saved: Filters?=null;var requests=0
  ui.setContent {EatTheme {FilterSheet(Filters(cuisines=setOf("sushi"),minRating=4.0,regions=setOf("yau_tsim_mong","area_prince_edward")),listOf(Cuisine("sushi","壽司")),{}, {saved=it},requestLocation={requests++})}}
  ui.onNodeWithText("取得／重試目前位置").performScrollTo().performClick()
  ui.onNodeWithText("500 米內").performScrollTo().performClick()
  ui.onNodeWithText("套用篩選").performScrollTo().performClick()
  assertEquals(1,requests);assertTrue(saved!!.regions.isEmpty());assertEquals(500,saved!!.radius)
  assertEquals(setOf("sushi"),saved!!.cuisines);assertEquals(4.0,saved!!.minRating!!,0.0)
 }
 @Test fun selectingDistrictCancelsPositionAndDoesNotRestoreItWhenDistrictCleared() {
  var saved: Filters?=null;var cancellations=0
  val status=mutableStateOf("已取得目前位置")
  ui.setContent {EatTheme {FilterSheet(Filters(radius=500,cuisines=setOf("sushi"),minRating=4.0),listOf(Cuisine("sushi","壽司")),{}, {saved=it},
   locationStatus=status.value,cancelLocation={cancellations++;status.value="尚未取得目前位置"})}}
  ui.onNode(hasText("油尖旺區") and isToggleable()).performScrollTo().performClick()
  ui.onNodeWithText("已選地區，未使用目前位置").assertExists()
  ui.onNode(hasText("油尖旺區") and isToggleable()).performScrollTo().performClick()
  ui.onNodeWithText("尚未取得目前位置").assertExists()
  ui.onAllNodesWithText("已取得目前位置").assertCountEquals(0)
  ui.onNodeWithText("套用篩選").performScrollTo().performClick()
  assertEquals(1,cancellations);assertNull(saved!!.radius);assertTrue(saved!!.regions.isEmpty())
  assertEquals(setOf("sushi"),saved!!.cuisines);assertEquals(4.0,saved!!.minRating!!,0.0)
 }
 @Test fun selectingLocalityClearsCurrentPositionAndSavesOnlyLocality() {
  var saved: Filters?=null;var cancellations=0
  var hideKeyboard: ()->Unit={}
  ui.setContent {
   val focus=androidx.compose.ui.platform.LocalFocusManager.current
   val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
   hideKeyboard={focus.clearFocus(force=true);keyboard?.hide()}
   EatTheme {FilterSheet(Filters(radius=1000),emptyList(),{}, {saved=it},locationStatus="已取得目前位置",cancelLocation={cancellations++})}
  }
  ui.onNodeWithText("搜尋地區／地點，例如旺角、東涌").performScrollTo().performTextInput("太子")
  ui.runOnIdle {hideKeyboard()}
  ui.onNode(hasText("太子") and isToggleable()).performScrollTo().performClick()
  ui.onNodeWithText("已選地區，未使用目前位置").assertExists()
  ui.onNodeWithText("套用篩選").performScrollTo().performClick()
  assertEquals(1,cancellations);assertNull(saved!!.radius);assertEquals(setOf("area_prince_edward"),saved!!.regions)
 }
 @Test fun locationRequestStaysInsideSheetAndKeepsDraftDistance() {
  var requests=0;var saved: Filters?=null
  ui.setContent {EatTheme {FilterSheet(Filters(radius=500),emptyList(),{}, {saved=it},
   requestLocation={requests++},locationStatus="已取得目前位置")}}
  ui.onNodeWithText("取得／重試目前位置").performScrollTo().performClick()
  assertEquals(1,requests)
  ui.onNodeWithText("已取得目前位置").assertExists()
  ui.onAllNodes(hasText("定位時間",substring=true) or hasText("準確度",substring=true)).assertCountEquals(0)
  ui.onNodeWithText("套用篩選").performScrollTo().performClick()
  assertEquals(500,saved?.radius)
 }
 @Test fun unlimitedRecoveryClearsOnlyDraftDistanceAndPreservesRating() {
  val token=mutableIntStateOf(0);var saved: Filters?=null
  ui.setContent {EatTheme {FilterSheet(Filters(radius=500,minRating=4.0),emptyList(),{}, {saved=it},clearDistanceToken=token.intValue)}}
  ui.runOnIdle {token.intValue++}
  ui.onNodeWithText("套用篩選").performScrollTo().performClick()
  assertNull(saved?.radius);assertEquals(4.0,saved?.minRating!!,0.0)
 }
}
