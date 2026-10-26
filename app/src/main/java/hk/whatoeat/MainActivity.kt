package hk.whatoeat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.feature.filter.FilterSheet
import hk.whatoeat.feature.home.*
import hk.whatoeat.feature.result.*
import hk.whatoeat.feature.favorites.FavoritesScreen
import hk.whatoeat.feature.history.HistoryScreen
import hk.whatoeat.feature.settings.SettingsScreen

@AndroidEntryPoint class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EatTheme { EatApp(vm,this) } }
    }
    override fun onStop() { super.onStop(); if(vm.state.value.session?.status == hk.whatoeat.core.model.SessionStatus.Prepared) vm.reveal() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EatApp(vm: HomeViewModel,activity: ComponentActivity) {
    val s by vm.state.collectAsStateWithLifecycle()
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "home"
    var showFilters by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if(grants.values.any { it }) vm.useLocation()
        else vm.permissionDenied(!ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_COARSE_LOCATION) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_FINE_LOCATION))
    }
    val useLocation: ()->Unit = {
        if(ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) vm.useLocation()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
    }
    val summary=filterSummary(s.preferences.filters)
    Scaffold(topBar={ TopAppBar(title={Text(stringResource(R.string.app_name))},actions={
        IconButton(onClick={nav.navigate("settings")},enabled=!s.locked) { Icon(Icons.Outlined.Settings,stringResource(R.string.settings)) }
    }) }, bottomBar={ NavigationBar {
        listOf(Triple("home",R.string.home,Icons.Outlined.Restaurant),Triple("favorites",R.string.favorites,Icons.Outlined.FavoriteBorder),Triple("history",R.string.history,Icons.Outlined.History)).forEach { (r,label,icon) ->
            NavigationBarItem(selected=route==r,onClick={nav.navigate(r) { popUpTo("home");launchSingleTop=true }},enabled=!s.locked,
                icon={Icon(icon,null)},label={Text(stringResource(label))})
        }
    } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.fillMaxWidth()) {
                Text(stringResource(if(BuildConfig.DEMO) R.string.demo_indicator else R.string.live_indicator),Modifier.padding(horizontal=20.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge)
            }
            NavHost(navController=nav,startDestination="home",modifier=Modifier.weight(1f)) {
                composable("home") { HomeScreen(s,vm,summary,{showFilters=true},useLocation) { key -> vm.openDetail(key);nav.navigate("detail") } }
                composable("favorites") { FavoritesScreen(s,vm) { key -> vm.openDetail(key);nav.navigate("detail") } }
                composable("history") { HistoryScreen(s,vm) { key -> vm.openDetail(key);nav.navigate("detail") } }
                composable("settings") { SettingsScreen(s,vm,{showFilters=true}) }
                composable("detail") { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick={nav.popBackStack()}) { Text(stringResource(R.string.back)) }
                    if(s.detailLoading) CircularProgressIndicator()
                    s.detail?.let { RestaurantContent(it,s,vm,true) }
                    if(!s.detailLoading && s.detail==null) Text(stringResource(R.string.error_details))
                } }
            }
        }
    }
    if(showFilters) FilterSheet(s.preferences.filters,{showFilters=false},vm::applyFilters)
    s.session?.let { ResultDialog(s,vm,summary) }
    s.failure?.let { f ->
        AlertDialog(onDismissRequest=vm::clearFailure,title={Text(stringResource(f.labelId()))},
            confirmButton={TextButton(onClick=vm::clearFailure){Text(stringResource(R.string.close))}},
            dismissButton={ Row {
                if(f==hk.whatoeat.core.model.Failure.StaleLocation) TextButton(onClick=vm::acceptStaleLocation){Text(stringResource(R.string.use_stale))}
                if(f in setOf(hk.whatoeat.core.model.Failure.PermissionPermanent,hk.whatoeat.core.model.Failure.LocationDisabled)) TextButton(onClick={
                    val intent=if(f==hk.whatoeat.core.model.Failure.LocationDisabled) Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${activity.packageName}"))
                    activity.startActivity(intent);vm.clearFailure()
                }){Text(stringResource(R.string.open_system_settings))}
                if(f==hk.whatoeat.core.model.Failure.Persistence && s.session!=null) TextButton(onClick={vm.clearFailure();vm.reveal()}){Text(stringResource(R.string.retry))}
            } })
    }
    if(s.needsAcknowledgment) AlertDialog(onDismissRequest={vm.acknowledgment(false)},title={Text(stringResource(R.string.ack_title))},
        text={Column { Text(stringResource(R.string.allergy_warning));Text(stringResource(R.string.ack_body)) }},
        confirmButton={TextButton(onClick={vm.acknowledgment(true)}) { Text(stringResource(R.string.acknowledge)) }},
        dismissButton={TextButton(onClick={vm.acknowledgment(false)}) { Text(stringResource(R.string.cancel)) }})
    if(s.exhausted) AlertDialog(onDismissRequest={vm.restartRound(false)},title={Text(stringResource(R.string.round_exhausted))},
        confirmButton={TextButton(onClick={vm.restartRound(true)}){Text(stringResource(R.string.restart_round))}},
        dismissButton={TextButton(onClick={vm.restartRound(false)}){Text(stringResource(R.string.cancel))}})
}
