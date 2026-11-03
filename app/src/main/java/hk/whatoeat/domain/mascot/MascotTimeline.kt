package hk.whatoeat.domain.mascot

enum class MascotClip {Themed,Welcome,Wok,Drooling}

/** Frame timing only. Never consumes random numbers or changes a draw result. */
object MascotTimeline {
 fun durations(clip: MascotClip): List<Long> = when(clip) {
  MascotClip.Themed->listOf(450,180,180,240,220,400,250,400)
  MascotClip.Welcome->listOf(600,120,100,140,140,140,140,500)
  MascotClip.Wok->listOf(180,140,120,120,150,140,140,180)
  MascotClip.Drooling->listOf(500,240,350,250,350,230,450,450)
 }.map(Int::toLong)
 fun duration(clip: MascotClip)=durations(clip).sum()
 fun finished(clip: MascotClip,elapsed: Long,cycles: Int?)=cycles!=null && elapsed>=duration(clip)*cycles.coerceAtLeast(1)
 fun frameAt(clip: MascotClip,elapsed: Long,cycles: Int?=2): Int {
  if(finished(clip,elapsed,cycles)) return 7
  var within=elapsed.coerceAtLeast(0)%duration(clip)
  durations(clip).forEachIndexed {i,time -> if(within<time) return i;within-=time}
  return 7
 }
}
