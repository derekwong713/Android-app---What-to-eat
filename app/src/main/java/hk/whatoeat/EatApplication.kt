package hk.whatoeat

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hk.whatoeat.core.model.*
import hk.whatoeat.data.local.*
import hk.whatoeat.data.location.FusedLocationRepository
import hk.whatoeat.data.places.*
import hk.whatoeat.domain.draw.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import java.util.UUID

@HiltAndroidApp class EatApplication : Application()
@Module @InstallIn(SingletonComponent::class) object AppModule {
    @Provides @Singleton fun time(): TimeSource = TimeSource { System.currentTimeMillis() }
    @Provides fun dispatcher(): CoroutineDispatcher = Dispatchers.IO
    @Provides @Singleton fun random(): RandomSource = RandomSource { kotlin.random.Random.nextInt(it) }
    @Provides @Singleton fun ids(): IdSource = IdSource { UUID.randomUUID().toString() }
    @Provides @Singleton fun database(@ApplicationContext c: Context): EatDatabase = Room.databaseBuilder(c,EatDatabase::class.java,"eat.db").build()
    @Provides @Singleton fun local(db: EatDatabase) = LocalStore(db)
    @Provides fun favorites(s: LocalStore): FavoritesRepository = s
    @Provides fun history(s: LocalStore): HistoryRepository = s
    @Provides fun rounds(s: LocalStore): RoundStore = s
    @Provides @Singleton fun preferences(@ApplicationContext c: Context): PreferencesRepository = UserPreferences(c)
    @Provides @Singleton fun places(@ApplicationContext c: Context,t: TimeSource): RestaurantRepository =
        if(BuildConfig.DEMO) DemoRestaurantRepository(c,t) else GoogleRestaurantRepository(c,BuildConfig.PLACES_API_KEY,t)
    @Provides @Singleton fun allergens(r: RestaurantRepository): AllergenInfoRepository =
        if(BuildConfig.DEMO) r as DemoRestaurantRepository else UnknownAllergens()
    @Provides @Singleton fun location(@ApplicationContext c: Context,t: TimeSource): LocationRepository = FusedLocationRepository(c,t)
    @Provides @Singleton fun coordinator(store: RoundStore,r: RandomSource,t: TimeSource,ids: IdSource) = DrawCoordinator(store,DrawEngine(r,t,ids),t)
}
