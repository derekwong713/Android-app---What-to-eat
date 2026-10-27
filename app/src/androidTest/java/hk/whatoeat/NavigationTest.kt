package hk.whatoeat
import android.content.*
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.feature.result.ExternalActions
import org.junit.Assert.*
import org.junit.Test
fun testRestaurant()=Restaurant("r","茶記 A&B","香港 商場 #1",Point(22.3,114.1),listOf(Cuisine("a","中式")),null,Business.Unknown,source="test",updatedAt="now",version=1)
class NavigationTest {
 @Test fun coordinatesAndNameWithoutPlaceId() {var launched: Intent?=null;val c=object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {override fun startActivity(intent: Intent) {launched=intent}};assertTrue(ExternalActions.navigate(c,testRestaurant()));assertEquals("22.3,114.1",launched!!.data!!.getQueryParameter("destination"));assertEquals("茶記 A&B",launched!!.getStringExtra(Intent.EXTRA_TITLE));assertNull(launched!!.data!!.getQueryParameter("destination_place_id"));assertEquals("https",launched!!.data!!.scheme)}
 @Test fun encodedAddressAndNoHandler() {val r=testRestaurant().copy(point=null);assertEquals(r.name+" "+r.address,Uri.parse(ExternalActions.navigationUrl(r)).getQueryParameter("destination"));val c=object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {override fun startActivity(intent: Intent) {throw ActivityNotFoundException()}};assertFalse(ExternalActions.navigate(c,r))}
}
