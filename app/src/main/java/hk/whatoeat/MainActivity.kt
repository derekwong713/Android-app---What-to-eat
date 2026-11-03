package hk.whatoeat
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.data.ads.*
import hk.whatoeat.feature.home.*
import hk.whatoeat.feature.filter.FilterSheet
import hk.whatoeat.feature.result.*
import hk.whatoeat.feature.favorites.FavoritesScreen
import hk.whatoeat.feature.history.HistoryScreen
import hk.whatoeat.feature.settings.SettingsScreen
import java.util.Locale
@AndroidEntryPoint class MainActivity: ComponentActivity() {
 private val vm: HomeViewModel by viewModels()
 val ads get()=(application as EatApplication).ads
 private var oauthAway=false;private var oauthPaused=false
 override fun attachBaseContext(base: Context) {val c=Configuration(base.resources.configuration);c.setLocales(LocaleList(Locale.forLanguageTag("zh-HK")));super.attachBaseContext(base.createConfigurationContext(c))}
 override fun onCreate(savedInstanceState: Bundle?) {super.onCreate(savedInstanceState);ads.start(this,savedInstanceState==null && intent.data==null);intent.data?.let {vm.authCallback(it.toString())};setContent {EatTheme {EatApp(vm,this)}}}
 override fun onNewIntent(intent: Intent) {super.onNewIntent(intent);setIntent(intent);if(intent.data!=null) {oauthAway=false;oauthPaused=false;vm.authCallback(intent.data.toString())}}
 fun launchOAuth(url: String) {ads.markExternal();try {startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)));oauthAway=true;oauthPaused=false;vm.authLaunched()} catch(_: ActivityNotFoundException) {vm.authLaunched();vm.authReturnedWithoutCallback()}}
 override fun onPause() {super.onPause();if(oauthAway) oauthPaused=true}
 override fun onResume() {super.onResume();if(oauthAway && oauthPaused) {oauthAway=false;oauthPaused=false;vm.authReturnedWithoutCallback()}}
 override fun onStop() {super.onStop();ads.leaveStartup();if(vm.state.value.session?.revealed==false) vm.reveal()}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EatApp(vm: HomeViewModel,activity: MainActivity) {
 val s by vm.state.collectAsStateWithLifecycle();val ad by activity.ads.state.collectAsStateWithLifecycle()
 val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route ?: "home"
 BackHandler(enabled=route=="wheel") {vm.reveal()}
 var filters by remember {mutableStateOf(false)}
 var clearDistanceToken by remember {mutableIntStateOf(0)}
 var locationRequestActive by rememberSaveable {mutableStateOf(false)}
 val cancelLocation: ()->Unit={locationRequestActive=false;vm.cancelLocation()}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {grants ->
  if(locationRequestActive) {
   locationRequestActive=false
   if(grants.values.any {it}) vm.useLocation() else vm.permissionDenied(!ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_COARSE_LOCATION) && !ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_FINE_LOCATION))
  }
 }
 val location: ()->Unit={
  vm.cancelLocation();locationRequestActive=true
  if(ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {locationRequestActive=false;vm.useLocation()}
  else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
 }
 LaunchedEffect(vm,ad.startup) {if(!ad.startup) vm.routes.collect {r -> nav.navigate(r) {if(r=="wheel" || r=="result") popUpTo("home");launchSingleTop=true}}}
 LaunchedEffect(s.ready,ad.startup) {if(!ad.startup && s.ready && s.restored && s.session!=null) nav.navigate("result") {launchSingleTop=true}}
 LaunchedEffect(s.authUrl) {s.authUrl?.let(activity::launchOAuth)}
 val openRestaurant: (hk.whatoeat.core.model.Restaurant)->Unit={r -> activity.ads.markExternal();if(!ExternalActions.open(activity,ExternalActions.restaurantUrl(r))) vm.navigationFailed()}
 if(ad.startup) {Surface(Modifier.fillMaxSize()) {Column(Modifier.padding(32.dp)) {Text("食咩好？",style=MaterialTheme.typography.headlineLarge);Text("準備開始…");LinearProgressIndicator()}};return}
 Scaffold(topBar={TopAppBar(title={Text("食咩好？")},actions={TextButton(onClick={nav.navigate("settings")},enabled=!s.locked) {Text("設定")}})},bottomBar={Column(Modifier.navigationBarsPadding()) {
 AdBanner(ad)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {listOf("home" to "首頁","favorites" to "收藏","history" to "歷史").forEach {(r,label) -> TextButton(onClick={nav.navigate(r) {popUpTo("home");launchSingleTop=true}},enabled=!s.locked) {Text(label)}}}
 }}) {padding -> Column(Modifier.fillMaxSize().padding(padding)) {
 Text("內置餐廳資料 · 毋須登入 · 未完整收錄",Modifier.padding(8.dp),style=MaterialTheme.typography.labelMedium)
 NavHost(nav,"home",Modifier.weight(1f)) {
 composable("home") {HomeScreen(s,vm,{filters=true})}
 composable("wheel") {s.session?.let {CuisineWheel(it,vm::reveal)}}
 composable("result") {ResultScreen(s,vm,openRestaurant,true)}
 composable("detail") {ResultScreen(s,vm,openRestaurant,false)}
 composable("favorites") {FavoritesScreen(s,vm,vm::openDetail)}
 composable("history") {HistoryScreen(s,vm,vm::openDetail)}
 composable("settings") {SettingsScreen(s,vm,{activity.ads.privacy(activity)},{ExternalActions.open(activity,it)})}
 }
 }}
 if(filters) FilterSheet(s.filters,s.cuisines,{filters=false},vm::applyFilters,
  requestLocation=location,locationStatus=if(s.busy) "正在處理…" else if(s.fix?.let {System.currentTimeMillis()-it.timestamp in 0..hk.whatoeat.core.model.DistanceOptions.MAX_FIX_AGE}==true) "已取得目前位置" else "尚未取得目前位置",
  busy=s.busy,clearDistanceToken=clearDistanceToken,cancelLocation=cancelLocation)
 CommunityDialogs(s,vm)
 s.failure?.let {f -> AlertDialog(onDismissRequest=vm::clearFailure,title={Text("未能完成操作")},text={Text(f.message())},confirmButton={TextButton(onClick=vm::clearFailure) {Text("關閉")}},dismissButton={Column {
 if(f in setOf(hk.whatoeat.core.model.Failure.Permission,hk.whatoeat.core.model.Failure.PermissionPermanent,hk.whatoeat.core.model.Failure.LocationDisabled,hk.whatoeat.core.model.Failure.LocationUnavailable,hk.whatoeat.core.model.Failure.LocationTimeout,hk.whatoeat.core.model.Failure.StaleLocation)) {
 TextButton(onClick={vm.clearFailure();location()}) {Text("重試定位")};TextButton(onClick={vm.clearFailure();if(filters) clearDistanceToken++;vm.noDistance()}) {Text("不限距離繼續")}
 TextButton(onClick={vm.clearFailure();activity.startActivity(if(f==hk.whatoeat.core.model.Failure.LocationDisabled) Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${activity.packageName}")))}) {Text("系統設定")}
 }
 if(f==hk.whatoeat.core.model.Failure.Conflict) TextButton(onClick={vm.closeAction();vm.clearFailure();vm.retryDetail()}) {Text("放棄草稿並重新讀取")}
 if(f==hk.whatoeat.core.model.Failure.SnapshotExpired) TextButton(onClick={vm.clearFailure();vm.discardPending();nav.navigate("home")}) {Text("捨棄過期抽選")}
 }})}
 if(s.notice) AlertDialog(onDismissRequest=vm::clearFailure,text={Text("已完成操作。")},confirmButton={TextButton(onClick=vm::clearFailure) {Text("確定")}})
}
