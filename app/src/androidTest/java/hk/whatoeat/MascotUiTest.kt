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
 @Test fun everyBundledCostumeRendersAndChangesWithCuisine() {
 val theme=mutableStateOf(MascotTheme.Base)
 ui.setContent {EatTheme {ResultMascot(theme.value,"測試菜式",theme.value.name)}}
 for(value in MascotTheme.entries) {
 ui.runOnIdle {theme.value=value}
 ui.onNodeWithTag("mascot_${value.name.lowercase()}").assertIsDisplayed()
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
