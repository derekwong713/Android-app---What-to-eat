package hk.whatoeat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.mascot.*
import hk.whatoeat.feature.result.CuisineWheel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MascotUiTest {
 @get:Rule val ui=createComposeRule()
 private fun loaded(tag: String) {ui.waitUntil(15000) {if(!ui.mainClock.autoAdvance) ui.mainClock.advanceTimeByFrame();ui.onAllNodes(hasTestTag(tag) and SemanticsMatcher.keyIsDefined(MascotFrameKey)).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun actualAtlasFramesAdvanceAndReducedMotionUsesOriginal() {
  val reduced=mutableStateOf(false)
  ui.mainClock.autoAdvance=false
  ui.setContent {EatTheme {AnimatedPigMascot(MascotTheme.Ramen,"拉麵",Modifier.size(196.dp),cycles=1,reducedMotion=reduced.value)}}
  loaded("mascot_ramen")
  ui.mainClock.advanceTimeBy(100)
  val before=ui.onNodeWithTag("mascot_ramen").fetchSemanticsNode().config[MascotFrameKey]
  ui.mainClock.advanceTimeBy(700)
  val after=ui.onNodeWithTag("mascot_ramen").fetchSemanticsNode().config[MascotFrameKey]
  assertNotEquals(before,after)
  ui.mainClock.advanceTimeBy(5000)
  ui.onNodeWithTag("mascot_ramen").assert(SemanticsMatcher.expectValue(MascotFrameKey,7))
  ui.runOnIdle {reduced.value=true};ui.mainClock.advanceTimeByFrame()
  ui.onNodeWithTag("mascot_ramen").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"靜態造型"))
 }
 @Test fun everyBundledCostumeRendersAndChangesWithCuisine() {
 val theme=mutableStateOf(MascotTheme.Base)
 ui.setContent {EatTheme {ResultMascot(theme.value,"測試菜式",theme.value.name)}}
 for(value in MascotTheme.entries) {
 ui.runOnIdle {theme.value=value}
 ui.onNodeWithTag("mascot_${value.name.lowercase()}").assertIsDisplayed()
 loaded("mascot_${value.name.lowercase()}")
 }
 ui.onNodeWithText("抽中菜式：測試菜式").assertIsDisplayed()
 }
 @Test fun wheelMascotAnimationCompletesOnceWithoutChangingSelectedCuisine() {
 val chinese=Cuisine("chinese","中菜");val sushi=Cuisine("sushi","壽司")
 val snapshot=CandidateSnapshot("s","t",10000,listOf(CuisineCount(chinese,1),CuisineCount(sushi,1)),1,0)
 val restaurant=Restaurant("r","測試","",null,listOf(chinese,sushi),null,Business.Unknown,source="test",updatedAt="",version=1)
 val session=DrawSession("d",snapshot,"sushi",1,restaurant,1)
 var reveals=0
 ui.mainClock.autoAdvance=false
 ui.setContent {EatTheme {CuisineWheel(session) {reveals++}}}
 ui.onNodeWithTag("mascot_base").assertIsDisplayed()
 ui.mainClock.advanceTimeBy(1000)
 val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
 ui.onRoot().captureToImage().asAndroidBitmap().let {bitmap ->
 java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"mascot-wheel.png").outputStream().use {
 bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
 }
 }
 ui.mainClock.advanceTimeBy(3000)
 ui.runOnIdle {assertEquals(1,reveals);assertEquals(MascotTheme.Sushi,CuisineMascot.forResult(session))}
 }
}
