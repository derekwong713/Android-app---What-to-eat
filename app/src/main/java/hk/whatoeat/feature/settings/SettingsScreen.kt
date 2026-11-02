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
 Text("帳戶及私隱",style=MaterialTheme.typography.headlineLarge)
 Text("內置餐廳資料，毋須登入。不提供評論或資料修改。")
 TextButton(onClick=privacy) {Text("廣告同意及私隱選項")}
 if(BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.PRIVACY_POLICY_URL)}) {Text("私隱政策")}
 if(BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.TERMS_URL)}) {Text("使用條款")}
 Text("資料隨 App 更新，來源及日期見餐廳詳情；未完整收錄香港餐廳。")
 TextButton(onClick={confirm="local"},enabled=!s.busy) {Text("清除本機資料及登出")}
 if(s.admin) TextButton(onClick={vm.requestAction("admin","")}) {Text("管理檢舉")}
 }
 if(confirm.isNotEmpty()) AlertDialog(onDismissRequest={confirm=""},title={Text(if(confirm=="account") "永久刪除帳戶？" else "清除本機資料？")},text={Text(if(confirm=="account") "評分及評論會刪除，公開餐廳修改及匿名修改紀錄會保留。此操作無法復原。" else "會清除本機收藏、歷史及待處理抽選，並登出。")},confirmButton={TextButton(onClick={if(confirm=="account") vm.deleteAccount() else vm.clearLocalData();confirm=""}) {Text("確認刪除")}},dismissButton={TextButton(onClick={confirm=""}) {Text("取消")}})
}
