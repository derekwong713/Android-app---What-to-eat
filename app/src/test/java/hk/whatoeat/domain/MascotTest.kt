package hk.whatoeat.domain

import hk.whatoeat.core.model.*
import hk.whatoeat.domain.mascot.*
import org.junit.Assert.*
import org.junit.Test

class MascotTest {
 @Test fun resultUsesDrawnCuisineRatherThanFirstRestaurantCuisine() {
 val chinese=Cuisine("chinese","中菜");val sushi=Cuisine("sushi","壽司")
 val snapshot=CandidateSnapshot("s","t",1000,listOf(CuisineCount(chinese,1),CuisineCount(sushi,1)),1,0)
 val restaurant=row().copy(cuisines=listOf(chinese,sushi))
 val session=DrawSession("d",snapshot,"sushi",1,restaurant,1)
 assertEquals(MascotTheme.Sushi,CuisineMascot.forResult(session))
 assertEquals(MascotTheme.Chinese,CuisineMascot.forCuisine(restaurant.cuisines.first().id))
 }
 @Test fun themedCuisineGroupsAndUnknownFallbackAreStable() {
 assertEquals(48,CuisineMascot.themes.size)
 assertEquals(MascotTheme.Ramen,CuisineMascot.forCuisine("ramen"))
 assertEquals(MascotTheme.Vegetarian,CuisineMascot.forCuisine("vegetarian"))
 assertEquals(MascotTheme.Hotpot,CuisineMascot.forCuisine("hot_pot"))
 assertEquals(MascotTheme.Dessert,CuisineMascot.forCuisine("dessert"))
 assertEquals(MascotTheme.Base,CuisineMascot.forCuisine("not_confirmed"))
 assertEquals(MascotTheme.Base,CuisineMascot.forCuisine(null))
 }
 @Test fun reducedMotionDisablesMascotSwayCompletely() {
 for(i in 0..100) assertEquals(0f,MascotMotion.wheelSway(i/100f,true),0f)
 }
 @Test fun mascotSwayIsBoundedAndStopsWithTheWheel() {
 for(i in 0..1000) assertTrue(kotlin.math.abs(MascotMotion.wheelSway(i/1000f,false))<=4f)
 assertEquals(0f,MascotMotion.wheelSway(0f,false),.0001f)
 assertEquals(0f,MascotMotion.wheelSway(1f,false),.0001f)
 }
}
