package hk.whatoeat.feature.settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.BuildConfig
import hk.whatoeat.feature.home.*
@Composable fun SettingsScreen(s: HomeState,vm: HomeViewModel,privacy: ()->Unit,open: (String)->Unit) {
 var confirm by remember {mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text("設定及私隱",style=MaterialTheme.typography.headlineLarge)
 Text("內置餐廳資料，毋須登入。不提供評論或資料修改。")
 TextButton(onClick=privacy) {Text("廣告同意及私隱選項")}
 if(BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.PRIVACY_POLICY_URL)}) {Text("私隱政策")}
 if(BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.TERMS_URL)}) {Text("使用條款")}
 Text("資料隨 App 更新，來源及日期見餐廳詳情；未完整收錄香港餐廳。")
 Text("行政區邊界資料：香港特別行政區政府民政事務總署。")
 TextButton(onClick={open("https://data.gov.hk/en-data/dataset/hk-had-json1-hong-kong-administrative-boundaries")}) {Text("行政區邊界資料來源")}
 TextButton(onClick={confirm="local"},enabled=!s.busy) {Text("清除本機資料")}
 }
 if(confirm.isNotEmpty()) AlertDialog(onDismissRequest={confirm=""},title={Text("清除本機資料？")},text={Text("會清除本機收藏、歷史、篩選及待處理抽選。此操作無法復原。")},confirmButton={TextButton(onClick={vm.clearLocalData();confirm=""}) {Text("確認清除")}},dismissButton={TextButton(onClick={confirm=""}) {Text("取消")}})
}
