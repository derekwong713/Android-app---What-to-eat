package hk.whatoeat

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import hk.whatoeat.core.model.*
import hk.whatoeat.feature.result.ExternalActions
import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    private val restaurant=Restaurant("google","place-123","Example",Point(22.3,114.1),fetchedAt=0)
    @Test fun directionsUseDestinationPlaceIdAndFallbackToBrowser() {
        val launches=mutableListOf<Intent>()
        val c=object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun startActivity(intent: Intent) {
                launches+=intent
                if(intent.`package`!=null) throw ActivityNotFoundException()
            }
        }
        assertTrue(ExternalActions.navigate(c,restaurant))
        assertEquals(2,launches.size)
        assertEquals("place-123",launches.last().data?.getQueryParameter("destination_place_id"))
        assertEquals("https",launches.last().data?.scheme)
        assertNull(launches.last().`package`)
    }
    @Test fun noHandlerReturnsRecoverableFailureAndFictionalIdsAreOmitted() {
        val c=object: ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun startActivity(intent: Intent) {throw ActivityNotFoundException()}
        }
        assertFalse(ExternalActions.navigate(c,restaurant))
        val demoUrl=ExternalActions.navigationUrl(restaurant.copy(provider="demo"))!!
        assertNull(Uri.parse(demoUrl).getQueryParameter("destination_place_id"))
        assertNull(ExternalActions.navigationUrl(restaurant.copy(point=null,address=null,name=null)))
    }
}
