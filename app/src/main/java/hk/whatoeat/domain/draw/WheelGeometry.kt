package hk.whatoeat.domain.draw

/** Canvas 0 degrees points right, positive clockwise; pointer at -90 / 270 degrees.
 * Segment i spans [i*sweep, (i+1)*sweep]; its center is (i+.5)*sweep.
 * Wheel rotates clockwise; target puts that center at 270 modulo 360.
 */
object WheelGeometry {
    fun normalize(degrees: Double): Double = ((degrees % 360) + 360) % 360
    fun center(count: Int, index: Int): Double {
        require(count in 1..20 && index in 0 until count)
        return (index + 0.5) * 360 / count
    }
    fun target(count: Int, winner: Int, current: Double = 0.0, turns: Int = 5): Double {
        require(turns in 4..6)
        val alignment = normalize(270 - center(count, winner))
        return current + turns * 360 + normalize(alignment - normalize(current))
    }
    fun pointedIndex(count: Int, rotation: Double): Int {
        require(count in 1..20)
        return (normalize(270 - rotation) / (360.0/count)).toInt().coerceIn(0,count-1)
    }
}
