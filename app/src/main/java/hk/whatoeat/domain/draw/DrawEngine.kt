package hk.whatoeat.domain.draw
import hk.whatoeat.core.model.*

/** Same unweighted two-stage algorithm as the server RPC. No ratings or ads are inputs. */
class DrawEngine(private val random: RandomSource) {
    private fun <T> choose(values: List<T>,previous: T?): T {
        require(values.isNotEmpty())
        val available=if(values.size>1) values.filterNot {it==previous} else values
        return available[if(available.size==1) 0 else random.nextInt(available.size)]
    }
    fun prepare(request: DrawRequest,rows: List<Restaurant>): DrawSession? {
        val cuisines=request.snapshot.cuisines.map {it.cuisine.id}.distinct().sorted()
        if(cuisines.isEmpty()) return null
        val cuisine=if(request.mode==DrawMode.SameCuisine) request.previousCuisine?.takeIf {it in cuisines} ?: return null else choose(cuisines,request.previousCuisine)
        val pool=rows.filter {it.recommendable && it.cuisines.any {c -> c.id==cuisine}}.distinctBy {it.id}.sortedBy {it.id}
        if(pool.isEmpty()) return null
        val winnerId=choose(pool.map {it.id},request.previousRestaurant)
        return DrawSession(request.id,request.snapshot,cuisine,cuisines.indexOf(cuisine),pool.first {it.id==winnerId},pool.size)
    }
}
