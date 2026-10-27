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
 Text(s.user?.let {"${it.nickname}"} ?: "遊客：毋須登入即可抽選、閱讀評分及評論、導航。")
 if(s.user==null) Button(onClick={vm.requestAction("profile","")}) {Text("登入 Google／Facebook")}
 else {
 TextButton(onClick={vm.requestAction("profile","")}) {Text("更改暱稱")}
 TextButton(onClick=vm::logout,enabled=!s.busy) {Text("登出")}
 TextButton(onClick={confirm="account"},enabled=!s.busy) {Text("刪除帳戶")}
 }
 Text("刪除帳戶後會移除登入身份、暱稱、評分及評論；已公開的餐廳修改會保留，修改紀錄及檢舉的作者身份會匿名化。已刪除評論的相關檢舉亦會移除。")
 TextButton(onClick=privacy) {Text("廣告同意及私隱選項")}
 if(BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.PRIVACY_POLICY_URL)}) {Text("私隱政策")}
 if(BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick={open(BuildConfig.TERMS_URL)}) {Text("使用條款")}
 Text("餐廳起始資料 © OpenStreetMap contributors，採用 ODbL 授權；用家修改只儲存在本服務，不會寫回 OSM。")
 TextButton(onClick={open("https://www.openstreetmap.org/copyright")}) {Text("OpenStreetMap 署名及授權")}
 TextButton(onClick={confirm="local"},enabled=!s.busy) {Text("清除本機資料及登出")}
 if(s.admin) TextButton(onClick={vm.requestAction("admin","")}) {Text("管理檢舉")}
 }
 if(confirm.isNotEmpty()) AlertDialog(onDismissRequest={confirm=""},title={Text(if(confirm=="account") "永久刪除帳戶？" else "清除本機資料？")},text={Text(if(confirm=="account") "評分及評論會刪除，公開餐廳修改及匿名修改紀錄會保留。此操作無法復原。" else "會清除本機收藏、歷史及待處理抽選，並登出。")},confirmButton={TextButton(onClick={if(confirm=="account") vm.deleteAccount() else vm.clearLocalData();confirm=""}) {Text("確認刪除")}},dismissButton={TextButton(onClick={confirm=""}) {Text("取消")}})
}
