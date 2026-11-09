package hk.whatoeat.feature.filter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.core.model.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun FilterSheet(current: Filters,cuisines: List<Cuisine>,dismiss: ()->Unit,save: (Filters)->Unit,
 requestLocation: ()->Unit={},locationStatus: String="尚未取得目前位置",busy: Boolean=false,clearDistanceToken: Int=0,
 cancelLocation: ()->Unit={}) {
 var radius by remember {mutableStateOf(current.effectiveRadius)}
 var minRating by remember {mutableStateOf(current.minRating)}
 var regions by remember {mutableStateOf(current.regions)}
 var selected by remember {mutableStateOf(current.cuisines)}
 var regionsExpanded by remember {mutableStateOf(false)}
 var cuisinesExpanded by remember {mutableStateOf(false)}
 var expandedDistricts by remember {mutableStateOf(emptySet<String>())}
 var search by remember {mutableStateOf("")}
 val initialClearToken=remember {clearDistanceToken}
 LaunchedEffect(clearDistanceToken) {if(clearDistanceToken!=initialClearToken) radius=null}
 val changeRegions: (Set<String>)->Unit={values ->
  regions=values.filterNot {it in LocalityOptions.labels && LocalityOptions.covered(it,values)}.toSet()
  if(regions.isNotEmpty()) {radius=null;cancelLocation()}
 }
 ModalBottomSheet(onDismissRequest=dismiss) {
 Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text("餐廳篩選",style=MaterialTheme.typography.headlineSmall)
 Text("距離",style=MaterialTheme.typography.titleMedium)
 Text(if(regions.isNotEmpty()) "已選地區，自動使用不限距離；清除地區後可重新選距離。" else "直線距離，不代表步行距離。有限距離需要目前位置。")
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
 DistanceOptions.metres.forEach {v -> FilterChip(selected=radius==v,onClick={radius=v},enabled=regions.isEmpty() || v==null,label={Text(v?.let {"${it} 米內"} ?: "不限距離")})}
 }
 Text(if(regions.isNotEmpty()) "已選地區，未使用目前位置" else locationStatus,style=MaterialTheme.typography.bodySmall)
 OutlinedButton(onClick={regions=emptySet();requestLocation()},enabled=!busy) {Text("取得／重試目前位置")}
 Text("目前位置與地區擇一：取得位置會清除地區，選地區會取消目前位置。",style=MaterialTheme.typography.bodySmall)
 HorizontalDivider()
 Text("最低評分",style=MaterialTheme.typography.titleMedium)
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
 RatingOptions.values.forEach {v -> FilterChip(selected=minRating==v,onClick={minRating=v},label={Text(v?.let {"${it} 分或以上"} ?: "不限評分")})}
 }
 Text("評分為資料快照；選了門檻後，暫無評分的餐廳不參與抽選。",style=MaterialTheme.typography.bodySmall)
 HorizontalDivider()
 FilterSectionHeader("地區及地點", regionsExpanded, regions.size) {regionsExpanded=!regionsExpanded}
 if(regionsExpanded) {
  Text("可多選 18 區或更細地點，選取後自動不限距離。選整區會包含區內全部地點。",style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(value=search,onValueChange={search=it},label={Text("搜尋地區／地點，例如旺角、東涌")},singleLine=true,modifier=Modifier.fillMaxWidth())
  TextButton(onClick={changeRegions(emptySet())}) {Text("全港／清除已選地區")}
  RegionOptions.groups.forEach {(group,ids) ->
   Text(RegionOptions.groupLabels.getValue(group),style=MaterialTheme.typography.titleMedium)
   val descendants=LocalityOptions.memberships.filterValues {parents -> parents.isNotEmpty() && parents.all {it in ids}}.keys
   TextButton(onClick={changeRegions(if(ids.all {it in regions}) regions-ids.toSet()-descendants else regions+ids)}) {Text(if(ids.all {it in regions}) "取消此大區" else "全選此大區")}
   ids.forEach {id ->
    val localities=LocalityOptions.labels.filterKeys {id in LocalityOptions.memberships[it].orEmpty()}
    val matches=localities.filter {(_,label) -> label.contains(search.trim(),ignoreCase=true)}
    if(search.isBlank() || RegionOptions.labels.getValue(id).contains(search.trim()) || matches.isNotEmpty()) {
     Surface(color=MaterialTheme.colorScheme.surfaceContainer,shape=MaterialTheme.shapes.medium) {
      Column(Modifier.fillMaxWidth().padding(8.dp)) {
       FilterOptionCheck(id,RegionOptions.labels.getValue(id),regions,true) {checked -> changeRegions(if(checked) regions+id else regions-id)}
       if(localities.isNotEmpty()) TextButton(onClick={expandedDistricts=if(id in expandedDistricts) expandedDistricts-id else expandedDistricts+id}) {Text(if(id in expandedDistricts) "收起地點" else "選細地點")}
       if(id in expandedDistricts || search.isNotBlank()) (if(search.isBlank() || RegionOptions.labels.getValue(id).contains(search.trim())) localities else matches).forEach {(key,_) ->
        FilterOptionCheck(key,LocalityOptions.displayLabel(key),if(LocalityOptions.covered(key,regions)) regions+key else regions,!LocalityOptions.covered(key,regions)) {checked -> changeRegions(if(checked) regions+key else regions-key)}
       }
      }
     }
    }
   }
  }
  val otherLocalities=LocalityOptions.labels.filterKeys {LocalityOptions.memberships[it].orEmpty().isEmpty()}
  if(otherLocalities.isNotEmpty()) Text("其他地點",style=MaterialTheme.typography.titleMedium)
  otherLocalities.filter {(_,label) -> search.isBlank() || label.contains(search.trim())}.forEach {(id,label) ->
  FilterOptionCheck(id,label,regions,true) {checked -> changeRegions(if(checked) regions+id else regions-id)}
  }
  Text("行政區按來源座標配對；來源資料可能有誤。同一地點有多區資料時，會在相關區顯示；選地點會包含其全部候選。",style=MaterialTheme.typography.bodySmall)
 }
 HorizontalDivider()
 FilterSectionHeader("菜式", cuisinesExpanded, selected.size) {cuisinesExpanded=!cuisinesExpanded}
 if(cuisinesExpanded) {
  TextButton(onClick={selected=emptySet()}) {Text("所有菜式／清除已選")}
  cuisines.forEach {c ->
   FilterOptionCheck(c.id,c.label,selected,true) {checked -> selected=if(checked) selected+c.id else selected-c.id}
  }
 }
 Button(onClick={save(Filters(radius,selected,minRating,regions).normalized());dismiss()},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("套用篩選")}
 }
 }
}
@Composable
private fun FilterSectionHeader(title: String, expanded: Boolean, selectedCount: Int, onClick: () -> Unit) {
 Row(
  Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 48.dp),
  verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.spacedBy(12.dp)
 ) {
  Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
  if (selectedCount > 0) Text("已選 ${selectedCount} 項", style = MaterialTheme.typography.bodySmall)
  Text(if (expanded) "收起" else "展開", color = MaterialTheme.colorScheme.primary)
 }
}

@Composable private fun FilterOptionCheck(id: String,label: String,selected: Set<String>,enabled: Boolean,change: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().toggleable(value=id in selected,enabled=enabled,role=Role.Checkbox,onValueChange=change)) {Checkbox(checked=id in selected,onCheckedChange=null,enabled=enabled);Text(label,modifier=Modifier.padding(top=12.dp))}
}
