package hk.whatoeat.domain

import hk.whatoeat.domain.mascot.*
import org.junit.Assert.*
import org.junit.Test

class MascotTimelineTest {
 @Test fun chewingSequenceHoldsAnticipationBeforeLiftingFood() {
  assertEquals(0,MascotTimeline.frameAt(MascotClip.Themed,449))
  assertEquals(1,MascotTimeline.frameAt(MascotClip.Themed,450))
  assertEquals(2,MascotTimeline.frameAt(MascotClip.Themed,630))
 }
 @Test fun finiteResultStopsAtLastPoseAndUnlimitedWheelLoops() {
  val time=MascotTimeline.duration(MascotClip.Themed)*2
  assertTrue(MascotTimeline.finished(MascotClip.Themed,time,2))
  assertEquals(7,MascotTimeline.frameAt(MascotClip.Themed,time+10000,2))
  assertEquals(0,MascotTimeline.frameAt(MascotClip.Wok,MascotTimeline.duration(MascotClip.Wok),null))
  assertFalse(MascotTimeline.finished(MascotClip.Wok,100000,null))
 }
 @Test fun everyClipHasEightNonzeroHoldsAndNegativeTimeUsesFirstPose() {
  MascotClip.entries.forEach {clip ->
   assertEquals(8,MascotTimeline.durations(clip).size)
   assertTrue(MascotTimeline.durations(clip).all {it>0})
   assertEquals(0,MascotTimeline.frameAt(clip,-10))
  }
 }
}
