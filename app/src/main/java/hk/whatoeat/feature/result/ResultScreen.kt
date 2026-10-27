package hk.whatoeat.feature.result
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.core.model.*
import hk.whatoeat.core.designsystem.label
import hk.whatoeat.feature.home.*
@Composable fun RestaurantContent(d: Detail,s: HomeState,vm: HomeViewModel,navigate: (Restaurant)->Unit) {
 val r=d.restaurant
 Text(r.name,style=MaterialTheme.typography.headlineMedium);Text(r.address.ifBlank {"地址未知"})
 Text(r.cuisines.joinToString("、") {it.label}.ifBlank {"未分類"});Text("營業時間：${r.openingHours?.takeIf {it.isNotBlank()} ?: "未知"}");Text(r.status.label())
 Text(r.averageRating?.let {"用戶評分：${"%.1f".format(it)}／5（${r.ratingCount} 個評分）"} ?: "暫無評分")
 Text("來源：${r.source} · 更新：${r.updatedAt} · 版本 ${r.version}",style=MaterialTheme.typography.bodySmall)
 Button(onClick={navigate(r)}) {Text("導航：${r.name}")}
 OutlinedButton(onClick={vm.toggleFavorite(r.id)},enabled=!s.busy) {Text(if(s.favorites.any {it.key=="catalog:${r.id}"}) "取消收藏" else "收藏餐廳")}
 listOf("edit" to "修改餐廳資料","rate" to "評分／更新自己的評分","comment" to "發表評論","report" to "檢舉餐廳資料","history" to "修改紀錄／還原").forEach {(kind,label) -> TextButton(onClick={vm.requestAction(kind,r.id)},enabled=!s.busy) {Text(label)}}
 Text("文字評論",style=MaterialTheme.typography.titleLarge)
 if(d.reviews.isEmpty()) Text("暫無評論")
 d.reviews.forEach {review ->
 HorizontalDivider();Text("${review.nickname} · ${review.createdAt}");Text(review.text)
 TextButton(onClick={vm.requestAction("report_comment:${review.id}",r.id)},enabled=!s.busy) {Text("檢舉評論")}
 if(review.own) TextButton(onClick={vm.deleteComment(review.id)},enabled=!s.busy) {Text("刪除自己的評論")}
 }
 if(d.nextReviewTime!=null) TextButton(onClick=vm::moreReviews,enabled=!s.busy) {Text("載入更多評論")}
}
@Composable fun ResultScreen(s: HomeState,vm: HomeViewModel,navigate: (Restaurant)->Unit,reroll: Boolean) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 if(reroll) s.session?.let {pick ->
 Text("抽中菜式："+pick.snapshot.cuisines.first {it.cuisine.id==pick.cuisineId}.cuisine.label)
 if(pick.snapshot.cuisines.size==1) Text("只有一種符合條件的菜式。")
 if(pick.restaurantCount==1) Text("此菜式只有一間符合條件的餐廳，重抽餐廳仍會是同一間。")
 Button(onClick={vm.draw(DrawMode.SameCuisine)},enabled=!s.locked) {Text("重抽餐廳")}
 OutlinedButton(onClick={vm.draw(DrawMode.NewCuisine)},enabled=!s.locked) {Text("重抽菜式")}
 }
 if(s.detailLoading) CircularProgressIndicator()
 s.detail?.let {RestaurantContent(it,s,vm,navigate)}
 if(!s.detailLoading) TextButton(onClick=vm::retryDetail) {Text("重新讀取餐廳資料")}
 }
}
