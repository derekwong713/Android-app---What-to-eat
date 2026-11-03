package hk.whatoeat.domain.mascot

import hk.whatoeat.core.model.DrawSession

/** Illustration selection only: never feeds back into candidate filtering or random draws. */
enum class MascotTheme {Base,Chinese,Ramen,Sushi,Korean,Southeast,Curry,Western,Grill,Vegetarian,Seafood,Hotpot,Dessert,Wrap}
object CuisineMascot {
    val themes=mapOf(
        "chinese" to MascotTheme.Chinese,
        "cantonese" to MascotTheme.Chinese,
        "shanghai" to MascotTheme.Chinese,
        "beijing" to MascotTheme.Chinese,
        "sichuan" to MascotTheme.Chinese,
        "hunan" to MascotTheme.Chinese,
        "hakka" to MascotTheme.Chinese,
        "fujian" to MascotTheme.Chinese,
        "zhejiang" to MascotTheme.Chinese,
        "taiwanese" to MascotTheme.Chinese,
        "dim_sum" to MascotTheme.Chinese,
        "noodles" to MascotTheme.Chinese,
        "congee" to MascotTheme.Chinese,
        "cha_chaan_teng" to MascotTheme.Chinese,
        "ramen" to MascotTheme.Ramen,
        "japanese" to MascotTheme.Ramen,
        "izakaya" to MascotTheme.Ramen,
        "sushi" to MascotTheme.Sushi,
        "korean" to MascotTheme.Korean,
        "thai" to MascotTheme.Southeast,
        "vietnamese" to MascotTheme.Southeast,
        "singaporean" to MascotTheme.Southeast,
        "malaysian" to MascotTheme.Southeast,
        "indian" to MascotTheme.Curry,
        "nepalese" to MascotTheme.Curry,
        "pakistani" to MascotTheme.Curry,
        "bangladeshi" to MascotTheme.Curry,
        "western" to MascotTheme.Western,
        "french" to MascotTheme.Western,
        "italian" to MascotTheme.Western,
        "spanish" to MascotTheme.Western,
        "american" to MascotTheme.Western,
        "british" to MascotTheme.Western,
        "german" to MascotTheme.Western,
        "polish" to MascotTheme.Western,
        "australian" to MascotTheme.Western,
        "steak" to MascotTheme.Grill,
        "barbecue" to MascotTheme.Grill,
        "japanese_grill" to MascotTheme.Grill,
        "argentinian" to MascotTheme.Grill,
        "vegetarian" to MascotTheme.Vegetarian,
        "seafood" to MascotTheme.Seafood,
        "hot_pot" to MascotTheme.Hotpot,
        "dessert" to MascotTheme.Dessert,
        "turkish" to MascotTheme.Wrap,
        "mediterranean" to MascotTheme.Wrap,
        "mexican" to MascotTheme.Wrap,
        "latin_american" to MascotTheme.Wrap
    )
    fun forCuisine(id: String?): MascotTheme=themes[id] ?: MascotTheme.Base
    fun forResult(session: DrawSession): MascotTheme=forCuisine(session.cuisineId)
}
