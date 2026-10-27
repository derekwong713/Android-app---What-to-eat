package hk.whatoeat.data.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.*
import hk.whatoeat.BuildConfig
import hk.whatoeat.domain.draw.StartupAdGate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference

data class AdState(val startup: Boolean=true,val canRequest: Boolean=false,val privacyRequired: Boolean=false,val failed: Boolean=false)
class AdsController(private val context: Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mutable=MutableStateFlow(AdState());val state=mutable.asStateFlow()
    private val prefs=context.getSharedPreferences("ad_startup_gate",Context.MODE_PRIVATE)
    private var current=WeakReference<ComponentActivity>(null)
    private var started=false;private var initialized=false;private var initializing=false;private var privacyShowing=false;private var loading=false
    private var gate=StartupAdGate(0)
    private val consent by lazy {UserMessagingPlatform.getConsentInformation(context)}
    fun start(activity: ComponentActivity,eligible: Boolean) {
        current=WeakReference(activity)
        if(started) return
        started=true
        val disabled=BuildConfig.DEBUG && System.getProperty("whatoeat.test.noAds")=="true"
        if(disabled || !BuildConfig.ADS_ENABLED) {mutable.value=AdState(startup=false);return}
        val now=System.currentTimeMillis()
        val allowed=eligible && now>=prefs.getLong("external_until",0) && now-prefs.getLong("last_shown",0)>=3600000
        gate=StartupAdGate(if(allowed) SystemClock.elapsedRealtime()+1800 else 0)
        if(!allowed) {gate.finish();mutable.update {it.copy(startup=false)}}
        scope.launch {delay(1800);gate.timeout();if(gate.home) mutable.update {it.copy(startup=false)}}
        consent.requestConsentInfoUpdate(activity,ConsentRequestParameters.Builder().build(),{
            mutable.update {it.copy(privacyRequired=consent.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)}
            if(consent.canRequestAds()) initialize()
            else UserMessagingPlatform.loadConsentForm(context,{form ->
                val a=current.get()
                if(!gate.home && a!=null && !a.isDestroyed) form.show(a) {initialize()}
                // Late forms are deferred to the explicit privacy entry point, never forced over a draw.
            },{finish()})
        },{if(consent.canRequestAds()) initialize() else finish()})
    }
    private fun initialize() {
        if(!consent.canRequestAds()) {finish();return}
        if(initialized) {mutable.update {it.copy(canRequest=!privacyShowing)};return}
        if(initializing) return
        initializing=true
        MobileAds.initialize(context) {scope.launch {
            initializing=false;initialized=true
            mutable.update {it.copy(canRequest=consent.canRequestAds() && !privacyShowing)}
            if(!privacyShowing) loadStartup()
        }}
    }
    private fun loadStartup() {
        if(gate.home || loading || BuildConfig.ADMOB_APP_OPEN_ID.isBlank()) return
        loading=true
        AppOpenAd.load(context,BuildConfig.ADMOB_APP_OPEN_ID,AdRequest.Builder().build(),object: AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                loading=false
                val a=current.get()
                if(a==null || a.isDestroyed || a.isFinishing || !a.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {finish();return}
                if(!gate.loaded(SystemClock.elapsedRealtime())) {mutable.update {it.copy(startup=false)};return}
                ad.fullScreenContentCallback=object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent()=finish()
                    override fun onAdFailedToShowFullScreenContent(error: AdError)=finish()
                    override fun onAdShowedFullScreenContent() {prefs.edit().putLong("last_shown",System.currentTimeMillis()).apply()}
                }
                ad.show(a)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {loading=false;mutable.update {it.copy(failed=true)};finish()}
        })
    }
    private fun finish() {gate.finish();mutable.update {it.copy(startup=false)}}
    fun leaveStartup() {finish()}
    fun markExternal() {prefs.edit().putLong("external_until",System.currentTimeMillis()+600000).apply();finish()}
    fun privacy(activity: Activity) {
        if(!BuildConfig.ADS_ENABLED || privacyShowing) return
        finish();privacyShowing=true
        // Dispose the old banner before changing consent; future requests use the updated choices.
        mutable.update {it.copy(canRequest=false)}
        fun completed() {
            privacyShowing=false
            mutable.update {it.copy(privacyRequired=consent.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,canRequest=false)}
            if(consent.canRequestAds()) initialize()
        }
        consent.requestConsentInfoUpdate(activity,ConsentRequestParameters.Builder().build(),{
            if(consent.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
                UserMessagingPlatform.showPrivacyOptionsForm(activity) {completed()}
            else UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {completed()}
        },{completed()})
    }
}
