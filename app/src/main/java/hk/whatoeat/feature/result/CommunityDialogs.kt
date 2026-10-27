package hk.whatoeat.feature.result
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.BuildConfig
import hk.whatoeat.core.model.*
import hk.whatoeat.core.designsystem.label
import hk.whatoeat.feature.home.*
import java.util.UUID
@Composable fun CommunityDialogs(s: HomeState,vm: HomeViewModel) {
 if(s.login) {
 AlertDialog(onDismissRequest=vm::cancelLogin,title={Text("登入後繼續原有操作")},text={Column {
 Text(if(BuildConfig.DEMO) "示範模式：以下只建立虛構本機身份，不會登入真實帳戶。" else "只支援 Google 及 Facebook。公開評論只顯示暱稱，不公開電郵。")
 Provider.entries.forEach {p -> Button(onClick={vm.signIn(p)},enabled=!s.busy) {Text((if(BuildConfig.DEMO) "模擬 " else "使用 ")+p.name+" 登入")}}
 }},confirmButton={TextButton(onClick=vm::cancelLogin,enabled=!s.busy) {Text("取消")}})
 return
 }
 val action=s.action ?: return
 val r=s.detail?.restaurant
 var text by remember(action) {mutableStateOf(if(action.kind=="profile") s.user?.nickname.orEmpty() else "")}
 var stars by remember(action) {mutableIntStateOf(s.detail?.ownRating ?: 5)}
 val requestId=remember(action) {UUID.randomUUID().toString()}
 var name by remember(action) {mutableStateOf(r?.name.orEmpty())};var address by remember(action) {mutableStateOf(r?.address.orEmpty())}
 var lat by remember(action) {mutableStateOf(r?.point?.latitude?.toString().orEmpty())};var lon by remember(action) {mutableStateOf(r?.point?.longitude?.toString().orEmpty())}
 var hours by remember(action) {mutableStateOf(r?.openingHours.orEmpty())};var cuisines by remember(action) {mutableStateOf(r?.cuisines?.map {it.id}?.toSet() ?: emptySet())}
 var status by remember(action) {mutableStateOf(r?.status ?: Business.Unknown)}
 val version=remember(action) {r?.version ?: 0L}
 var restore by remember(action) {mutableStateOf<Revision?>(null)}
 val title=when(action.kind) {"edit"->"修改餐廳資料";"rate"->"評分";"comment"->"發表評論";"profile"->"設定公開暱稱";"history"->"修改紀錄";"admin"->"管理檢舉";else->"檢舉"}
 AlertDialog(onDismissRequest={if(!s.busy) vm.closeAction()},title={Text(title)},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
 when(action.kind) {
 "edit" -> {
 OutlinedTextField(name,{if(it.length<=120) name=it},label={Text("名稱")});OutlinedTextField(address,{if(it.length<=500) address=it},label={Text("地址（商場、樓層、舖號）")})
 OutlinedTextField(lat,{lat=it},label={Text("緯度（可留空）")});OutlinedTextField(lon,{lon=it},label={Text("經度（可留空）")});OutlinedTextField(hours,{if(it.length<=1000) hours=it},label={Text("營業時間（留空代表未知）")})
 s.cuisines.forEach {c -> Row {Checkbox(c.id in cuisines,{checked -> cuisines=if(checked) cuisines+c.id else cuisines-c.id});Text(c.label)}}
 Business.entries.forEach {v -> Row {RadioButton(v==status,{status=v});Text(v.label())}}
 Text("根據版本 $version 修改；若有衝突，草稿不會自動覆蓋新資料。關閉草稿後可重新讀取餐廳。")
 }
 "rate" -> {Text("每個帳戶每間餐廳只保留一個評分；提交會更新自己的評分。");Row {(1..5).forEach {v -> TextButton(onClick={stars=v}) {Text(if(stars==v) "★$v" else "$v")}}}}
 "history" -> {s.revisions.forEach {rev -> Text("版本 ${rev.version} · ${rev.nickname} · ${rev.createdAt}");Text("修改前：${rev.before}");Text("修改後：${rev.after}");TextButton(onClick={if(s.user==null) vm.requestAction("restore:${rev.id}",action.restaurantId) else restore=rev},enabled=!s.busy) {Text("還原至這次修改前")};HorizontalDivider()};TextButton(onClick=vm::moreRevisions,enabled=!s.busy) {Text("載入較早紀錄")}}
 "admin" -> {s.reports.forEach {report -> Text(report.reason);Text("餐廳：${report.restaurantId ?: "—"}；評論：${report.commentId ?: "—"}");TextButton(onClick={vm.resolve(report.id,"dismiss")},enabled=!s.busy) {Text("完成處理／不作變更")};TextButton(onClick={vm.resolve(report.id,if(report.commentId!=null) "hide_comment" else "disable_restaurant")},enabled=!s.busy) {Text(if(report.commentId!=null) "隱藏被檢舉評論" else "停用被檢舉餐廳")};HorizontalDivider()}}
 else -> if(action.kind.startsWith("restore:")) Text("還原會新增一個修改版本，不會移除原有紀錄。") else OutlinedTextField(text,{if(it.length<=(if(action.kind=="profile") 40 else if(action.kind=="comment") 2000 else 1000)) text=it},label={Text(if(action.kind=="profile") "暱稱（不可填寫電郵）" else if(action.kind=="comment") "文字評論" else "檢舉原因")})
 }
 }},confirmButton={if(action.kind !in setOf("history","admin")) TextButton(enabled=!s.busy,onClick={
 when(action.kind) {
 "edit" -> {val point=if(lat.isBlank() && lon.isBlank()) null else lat.toDoubleOrNull()?.let {a -> lon.toDoubleOrNull()?.let {b -> Point(a,b)}};if((lat.isBlank() && lon.isBlank()) || point!=null) vm.edit(action.restaurantId,version,EditPatch(name.trim(),address.trim(),point,cuisines,hours.trim().ifBlank {null},status)) else vm.invalidInput()}
 "rate" -> vm.rate(action.restaurantId,stars)
 "comment" -> vm.comment(action.restaurantId,text,requestId)
 "profile" -> vm.nickname(text.trim())
 else -> if(action.kind.startsWith("restore:")) vm.restoreRevision(action.restaurantId,action.kind.substringAfter(':').toLong(),version) else vm.report(if(action.kind=="report") action.restaurantId else null,action.kind.takeIf {it.startsWith("report_comment:")}?.substringAfter(':'),text)
 }
 }) {Text("提交")}},dismissButton={TextButton(onClick=vm::closeAction,enabled=!s.busy) {Text("關閉草稿")}})
 restore?.let {rev -> AlertDialog(onDismissRequest={restore=null},title={Text("確認還原？")},text={Text("會按目前版本 ${r?.version} 還原至所選修改前資料。")},confirmButton={TextButton(onClick={vm.restoreRevision(action.restaurantId,rev.id,r?.version ?: 0);restore=null},enabled=!s.busy) {Text("確認還原")}},dismissButton={TextButton(onClick={restore=null}) {Text("取消")}})}
}
