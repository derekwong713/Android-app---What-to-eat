package hk.whatoeat.feature.filter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hk.whatoeat.core.model.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FilterSheet(current: Filters,cuisines: List<Cuisine>,dismiss: ()->Unit,save: (Filters)->Unit) {
 var radius by remember {mutableStateOf(current.radius)};var selected by remember {mutableStateOf(current.cuisines)}
 ModalBottomSheet(onDismissRequest=dismiss) {
 Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
 Text("距離及菜式",style=MaterialTheme.typography.headlineSmall);Text("直線距離，不代表步行距離。有限距離需要目前裝置位置。")
 DistanceOptions.metres.forEach {v -> Row {RadioButton(selected=radius==v,onClick={radius=v});Text(v?.let {"${it} 米內"} ?: "不限距離")}}
 Text("菜式（不選代表所有已分類菜式）")
 TextButton(onClick={selected=emptySet()}) {Text("所有菜式／清除已選")}
 cuisines.forEach {c -> Row {Checkbox(c.id in selected,{checked -> selected=if(checked) selected+c.id else selected-c.id});Text(c.label)}}
 Button(onClick={save(Filters(radius,selected));dismiss()}) {Text("套用篩選")}
 }
 }
}
