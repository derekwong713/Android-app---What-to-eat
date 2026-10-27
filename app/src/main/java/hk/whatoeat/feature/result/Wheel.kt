package hk.whatoeat.feature.result
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hk.whatoeat.core.model.DrawSession
import hk.whatoeat.domain.draw.WheelGeometry
@Composable fun CuisineWheel(s: DrawSession,reveal: ()->Unit) {
 val rotation=remember(s.drawId) {Animatable(0f)};val context=LocalContext.current
 LaunchedEffect(s.drawId) {
 val reduced=Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
 rotation.animateTo(WheelGeometry.target(s.snapshot.cuisines.size,s.cuisineIndex).toFloat(),tween(if(reduced) 1 else 3200,easing=FastOutSlowInEasing));reveal()
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text("抽選菜式",style=MaterialTheme.typography.headlineLarge);Text("▼ 指針",Modifier.align(Alignment.CenterHorizontally))
 Canvas(Modifier.fillMaxWidth().height(260.dp)) {
 val count=s.snapshot.cuisines.size;val d=size.minDimension;val top=Offset((size.width-d)/2,0f)
 repeat(count) {i ->
 drawArc(Color.hsv(i*360f/count,.55f,.9f),rotation.value+i*360f/count,360f/count,true,top,androidx.compose.ui.geometry.Size(d,d))
 if(count<=24) {
 val angle=Math.toRadians((rotation.value+(i+.5)*360/count).toDouble())
 val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.BLACK;textSize=14.dp.toPx();textAlign=android.graphics.Paint.Align.CENTER}
 drawContext.canvas.nativeCanvas.drawText((i+1).toString(),(size.width/2+cos(angle)*d*.35).toFloat(),(d/2+sin(angle)*d*.35).toFloat()-(paint.ascent()+paint.descent())/2,paint)
 }
 }
 }
 Text("菜式順序（從右側起順時針）："+s.snapshot.cuisines.mapIndexed {i,c -> "${i+1}. ${c.cuisine.label}"}.joinToString("、"))
 Text("結果已安全儲存；略過動畫亦會顯示同一抽選結果。")
 TextButton(onClick=reveal) {Text("略過動畫，查看結果")}
 }
}
