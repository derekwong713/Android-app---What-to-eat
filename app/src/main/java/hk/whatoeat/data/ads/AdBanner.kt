package hk.whatoeat.data.ads
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.*
import hk.whatoeat.BuildConfig
import hk.whatoeat.R
@Composable fun AdBanner(state: AdState) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    Column(Modifier.fillMaxWidth().padding(top=12.dp,bottom=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        HorizontalDivider();Text(stringResource(R.string.ad_label),style=MaterialTheme.typography.labelSmall)
        Box(Modifier.fillMaxWidth().height(50.dp),contentAlignment=Alignment.Center) {
            if(state.canRequest && BuildConfig.ADMOB_BANNER_ID.isNotBlank()) {
                val view=remember {AdView(context).apply {setAdSize(AdSize.BANNER);adUnitId=BuildConfig.ADMOB_BANNER_ID}}
                DisposableEffect(view,owner) {
                    view.loadAd(AdRequest.Builder().build())
                    val observer=LifecycleEventObserver {_,event -> when(event) {Lifecycle.Event.ON_RESUME->view.resume();Lifecycle.Event.ON_PAUSE->view.pause();else->Unit}}
                    owner.lifecycle.addObserver(observer)
                    onDispose {owner.lifecycle.removeObserver(observer);view.destroy()}
                }
                AndroidView(factory={view},modifier=Modifier.width(320.dp).height(50.dp))
            } else Text(stringResource(R.string.ad_unavailable),style=MaterialTheme.typography.bodySmall)
        }
    }
}
