package hk.whatoeat.domain.mascot

import kotlin.math.PI
import kotlin.math.sin

/** A small, finite sway synchronized with the real wheel progress; never random. */
object MascotMotion {
 fun wheelSway(progress: Float,reduced: Boolean): Float =
  if(reduced) 0f else (sin(progress.coerceIn(0f,1f)*10*PI)*4).toFloat()
}
