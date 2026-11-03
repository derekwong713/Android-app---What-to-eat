package hk.whatoeat

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.domain.mascot.CuisineMascot
import org.json.JSONArray
import java.io.File
import org.junit.Rule
import org.junit.Test

class MascotFlowTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun screenshot(name: String) {
 val i=InstrumentationRegistry.getInstrumentation()
 val bitmap=requireNotNull(i.uiAutomation.takeScreenshot())
 File(i.targetContext.getExternalFilesDir(null),"mascot-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 bitmap.recycle()
 }
 private fun checkResult(): String {
 ui.waitUntil(20000) {ui.onAllNodes(hasText("抽中菜式：",substring=true)).fetchSemanticsNodes().isNotEmpty()}
 val heading=ui.onNode(hasText("抽中菜式：",substring=true)).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
 val label=heading.removePrefix("抽中菜式：")
 val rows=JSONArray(ui.activity.assets.open("restaurants.json").bufferedReader().use {it.readText()})
 var id: String?=null
 for(n in 0 until rows.length()) {
 val cuisines=rows.getJSONObject(n).getJSONArray("cuisines")
 for(c in 0 until cuisines.length()) {
 val cuisine=cuisines.getJSONObject(c)
 if(cuisine.getString("label")==label) id=cuisine.getString("id")
 }
 }
 requireNotNull(id)
 ui.onNodeWithTag("mascot_${CuisineMascot.forCuisine(id).name.lowercase()}").assertIsDisplayed()
 return label
 }
 @Test fun guestDrawAndBothRerollsShowTheDrawnCuisineCostume() {
 val vm=androidx.lifecycle.ViewModelProvider(ui.activity)[hk.whatoeat.feature.home.HomeViewModel::class.java]
 ui.waitUntil(20000) {vm.state.value.ready}
 if(vm.state.value.restored) checkResult()
 ui.waitUntil(20000) {ui.onAllNodes(hasText("首頁") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithText("首頁").performClick()
 ui.waitUntil(10000) {ui.onAllNodesWithText("抽菜式，再抽餐廳").fetchSemanticsNodes().isNotEmpty()}
 ui.onNodeWithTag("mascot_base").assertIsDisplayed()
 screenshot("home")
 val previousDraw=vm.state.value.session?.drawId
 ui.onNodeWithText("抽菜式，再抽餐廳").performScrollTo().performClick()
 ui.waitUntil(20000) {vm.state.value.session?.let {it.drawId!=previousDraw && it.revealed && !vm.state.value.busy}==true}
 val cuisine=checkResult()
 screenshot("result")
 val beforeReroll=vm.state.value.session?.drawId
 ui.onNodeWithText("重抽餐廳").performScrollTo().performClick()
 ui.waitUntil(20000) {vm.state.value.session?.let {it.drawId!=beforeReroll && it.revealed && !vm.state.value.busy}==true}
 org.junit.Assert.assertEquals(cuisine,checkResult())
 ui.onNodeWithText("重抽菜式").performScrollTo().performClick()
 ui.waitUntil(20000) {ui.onAllNodes(hasText("抽中菜式：",substring=true)).fetchSemanticsNodes().any {
 it.config[SemanticsProperties.Text].first().text!="抽中菜式：$cuisine"
 }}
 checkResult()
 screenshot("reroll")
 }
}
