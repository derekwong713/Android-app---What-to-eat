package hk.whatoeat
import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hk.whatoeat.core.model.*
import hk.whatoeat.data.local.*
import hk.whatoeat.data.auth.*
import hk.whatoeat.data.backend.*
import hk.whatoeat.data.ads.AdsController
import hk.whatoeat.data.location.FusedLocationRepository
import hk.whatoeat.data.places.*
import kotlinx.coroutines.*
import javax.inject.Singleton
import java.util.UUID
@HiltAndroidApp class EatApplication: Application() {val ads by lazy {AdsController(this)}}
@Module @InstallIn(SingletonComponent::class) object AppModule {
 @Provides @Singleton fun time(): TimeSource=TimeSource {System.currentTimeMillis()}
 @Provides fun dispatcher(): CoroutineDispatcher=Dispatchers.IO
 @Provides @Singleton fun random(): RandomSource=RandomSource {kotlin.random.Random.nextInt(it)}
 @Provides @Singleton fun ids(): IdSource=IdSource {UUID.randomUUID().toString()}
 @Provides @Singleton fun db(@ApplicationContext c: Context): EatDatabase=Room.databaseBuilder(c,EatDatabase::class.java,"eat.db").addMigrations(EatDatabase.MIGRATION_1_2).build()
 @Provides @Singleton fun local(db: EatDatabase)=LocalStore(db)
 @Provides @Singleton fun prefs(@ApplicationContext c: Context)=UserPreferences(c)
 @Provides @Singleton fun http(): JsonTransport=SupabaseHttp(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLIC_KEY)
 @Provides @Singleton fun auth(@ApplicationContext c: Context,http: JsonTransport,t: TimeSource): AuthRepository=if(BuildConfig.DEMO) DemoAuthRepository() else SupabaseAuthRepository(http,BuildConfig.SUPABASE_URL,c.packageName,EncryptedAuthVault(c),t)
 @Provides @Singleton fun restaurants(http: JsonTransport,a: AuthRepository,t: TimeSource,r: RandomSource): RestaurantRepository=if(BuildConfig.DEMO) DemoRestaurantRepository(a,t,r) else SupabaseRestaurantRepository(http,a)
 @Provides @Singleton fun location(@ApplicationContext c: Context,t: TimeSource): LocationRepository=FusedLocationRepository(c,t)
}
