package hk.whatoeat

import androidx.compose.ui.test.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.junit4.createComposeRule
import hk.whatoeat.core.designsystem.EatTheme
import hk.whatoeat.core.model.Filters
import hk.whatoeat.feature.filter.FilterSheet
import org.junit.Assert.*
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

class LocalityFilterUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun selectingLocalityClearsFiniteDistanceAndPreservesRating() {
 var saved: Filters?=null
 var hideKeyboard: ()->Unit={}
 compose.setContent {
 val focus=LocalFocusManager.current
 val keyboard=LocalSoftwareKeyboardController.current
 hideKeyboard={focus.clearFocus(force=true);keyboard?.hide()}
 EatTheme {FilterSheet(Filters(radius=500,minRating=4.0),emptyList(),{}, {saved=it})}
 }
 compose.onNodeWithText("搜尋地區／地點，例如旺角、東涌").performScrollTo().performTextInput("太子")
 compose.runOnIdle {hideKeyboard()}
 compose.onNode(hasText("太子") and isToggleable()).performScrollTo().performClick()
 compose.onNodeWithText("套用篩選").performScrollTo().performClick()
 compose.runOnIdle {assertNotNull(saved);assertNull(saved!!.radius);assertEquals(setOf("area_prince_edward"),saved!!.regions);assertEquals(4.0,saved!!.minRating!!,0.0)}
 compose.onNodeWithText("餐廳篩選").performScrollTo()
 compose.waitForIdle()
 val instrumentation=InstrumentationRegistry.getInstrumentation()
 instrumentation.uiAutomation.takeScreenshot()?.let {bitmap ->
 val file=File(instrumentation.targetContext.getExternalFilesDir(null),"morandi-filter.png")
 FileOutputStream(file).use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 bitmap.recycle()
 }

 }
 @Test fun multiDistrictPlaceStaysSelectedWhenOneParentDistrictIsAdded() {
 var saved: Filters?=null
 var hideKeyboard: ()->Unit={}
 compose.setContent {
 val focus=LocalFocusManager.current
 val keyboard=LocalSoftwareKeyboardController.current
 hideKeyboard={focus.clearFocus(force=true);keyboard?.hide()}
 EatTheme {FilterSheet(Filters(radius=500),emptyList(),{}, {saved=it})}
 }
 compose.onNodeWithText("搜尋地區／地點，例如旺角、東涌").performScrollTo().performTextInput("銅鑼灣")
 compose.runOnIdle {hideKeyboard()}
 val places=compose.onAllNodes(hasText("銅鑼灣",substring=true) and isToggleable())
 places.assertCountEquals(2)
 places.onFirst().performScrollTo().performClick()
 compose.onNode(hasText("灣仔區") and isToggleable()).performScrollTo().performClick()
 compose.onNodeWithText("套用篩選").performScrollTo().performClick()
 compose.runOnIdle {
 assertEquals(setOf("area_causeway_bay","wan_chai"),saved!!.regions)
 assertNull(saved!!.radius)
 }
 }

}
