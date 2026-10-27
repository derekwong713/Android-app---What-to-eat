package hk.whatoeat.domain

import hk.whatoeat.domain.draw.WheelGeometry as W
import org.junit.Assert.*
import org.junit.Test

class WheelGeometryTest {
    @Test fun everyWinnerAlignsForOneThroughTwentyCandidates() {
        for(count in 1..64) for(index in 0 until count) for(current in listOf(-750.0,0.0,350.0,720.0,12345.0)) {
            val target=W.target(count,index,current)
            assertEquals("count=$count index=$index rotation=$current",270.0,W.normalize(target+W.center(count,index)),1e-8)
            assertEquals(index,W.pointedIndex(count,target))
            assertTrue(target>=current+1800);assertTrue(target<current+2160)
        }
    }
    @Test fun normalizationCrossesFullRotations() {assertEquals(10.0,W.normalize(730.0),0.0);assertEquals(350.0,W.normalize(-730.0),0.0)}
    @Test(expected=IllegalArgumentException::class) fun invalidCountRejected() {W.target(0,0)}
}
