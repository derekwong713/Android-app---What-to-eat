package hk.whatoeat.feature.home
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.core.designsystem.filterSummary
import hk.whatoeat.core.model.DrawMode
@Composable fun HomeScreen(s: HomeState,vm: HomeViewModel,filters: ()->Unit,location: ()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text("今日食咩好？",style=MaterialTheme.typography.headlineLarge)
 Text("先等機率抽菜式，再等機率抽餐廳。評分及廣告不影響抽選。")
 Text("服務範圍為香港；資料來自 OpenStreetMap 及用家補充，尚未完整收錄全港餐廳。")
 Text(filterSummary(s.filters));Text("距離為直線距離，不代表步行距離。")
 OutlinedButton(onClick=filters,enabled=!s.locked) {Text("距離及菜式")}
 OutlinedButton(onClick=location,enabled=!s.locked) {Text("取得／重試目前位置")}
 s.fix?.let {Text("定位時間：${java.util.Date(it.timestamp)}；準確度約 ${it.accuracy.toInt()} 米")}
 if(s.filters.radius!=null) TextButton(onClick=vm::noDistance,enabled=!s.locked) {Text("改用不限距離")}
 s.snapshot?.let {snap ->
 Text("符合條件：${snap.restaurantCount} 間餐廳、${snap.cuisines.size} 種菜式")
 if(snap.unclassifiedCount>0) Text("另有 ${snap.unclassifiedCount} 間未分類餐廳，不參與菜式抽選；不會根據店名猜測。")
 if(snap.cuisines.isEmpty()) Text("沒有符合條件的餐廳，請調整距離或菜式。")
 }
 Button(onClick={vm.draw(DrawMode.NewCuisine)},enabled=!s.locked) {Text(if(s.recovery) "重試原有抽選" else "抽菜式，再抽餐廳")}
 OutlinedButton(onClick=vm::refresh,enabled=!s.locked) {Text("重新載入完整候選")}
 if(s.recovery) {
 Text("重試原有抽選會沿用原查詢的位置及候選，並非重新定位。要改用目前位置，請捨棄原抽選再取得位置。")
 TextButton(onClick=vm::discardPending,enabled=!s.busy) {Text("捨棄待處理抽選")}
 }
 if(s.legacyCount>0) Text("已保留 ${s.legacyCount} 項舊版收藏／歷史；舊供應商編號無法在新後台查詢。")
 if(s.busy || !s.ready) LinearProgressIndicator(Modifier.fillMaxWidth())
 }
}
