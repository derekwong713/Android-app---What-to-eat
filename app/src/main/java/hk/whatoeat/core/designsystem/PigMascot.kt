package hk.whatoeat.core.designsystem

import android.provider.Settings
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.domain.mascot.MascotTheme
import hk.whatoeat.domain.mascot.MascotClip

@DrawableRes fun mascotDrawable(theme: MascotTheme): Int=when(theme) {
 MascotTheme.Base->R.drawable.mascot_base
 MascotTheme.Chinese->R.drawable.mascot_chinese
 MascotTheme.Ramen->R.drawable.mascot_ramen
 MascotTheme.Sushi->R.drawable.mascot_sushi
 MascotTheme.Korean->R.drawable.mascot_korean
 MascotTheme.Southeast->R.drawable.mascot_southeast
 MascotTheme.Curry->R.drawable.mascot_curry
 MascotTheme.Western->R.drawable.mascot_western
 MascotTheme.Grill->R.drawable.mascot_grill
 MascotTheme.Vegetarian->R.drawable.mascot_vegetarian
 MascotTheme.Seafood->R.drawable.mascot_seafood
 MascotTheme.Hotpot->R.drawable.mascot_hotpot
 MascotTheme.Dessert->R.drawable.mascot_dessert
 MascotTheme.Wrap->R.drawable.mascot_wrap
}
@Composable fun reducedMascotMotion(): Boolean {
 val context=LocalContext.current
 return Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
}
@Composable fun PigMascot(theme: MascotTheme,description: String?,modifier: Modifier=Modifier) {
 val resources=LocalResources.current
 val bitmap=remember(theme,resources) {
 val id=mascotDrawable(theme)
 val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true;inScaled=false}
 BitmapFactory.decodeResource(resources,id,bounds)
 var sample=1
 while(maxOf(bounds.outWidth,bounds.outHeight)/(sample*2)>=512) sample*=2
 val options=BitmapFactory.Options().apply {inSampleSize=sample;inScaled=false}
 requireNotNull(BitmapFactory.decodeResource(resources,id,options)).asImageBitmap()
 }
 Image(painter=remember(bitmap) {BitmapPainter(bitmap)},contentDescription=description,
 modifier=modifier.testTag("mascot_${theme.name.lowercase()}"),contentScale=ContentScale.Fit)
}
@Composable fun MascotWelcome() {
 var clip by remember {mutableStateOf(MascotClip.Welcome)}
 Surface(color=MaterialTheme.colorScheme.surfaceContainer,shape=MaterialTheme.shapes.large) {
 Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
 AnimatedPigMascot(MascotTheme.Base,"豬仔陪你揀今餐",Modifier.size(108.dp),clip=clip,cycles=1,
  onFinished={if(clip==MascotClip.Welcome) clip=MascotClip.Drooling})
 Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
 Text("豬仔陪你揀今餐",style=MaterialTheme.typography.titleMedium)
 Text("揀好心水條件，轉一轉就有靈感。",style=MaterialTheme.typography.bodyMedium)
 }
 }
 }
}
@Composable fun ResultMascot(theme: MascotTheme,cuisineName: String,appearanceKey: String,modifier: Modifier=Modifier,isDraw: Boolean=true) {
 val reduced=reducedMascotMotion()
 val progress=remember(appearanceKey) {Animatable(if(reduced) 1f else 0f)}
 LaunchedEffect(appearanceKey,reduced) {if(reduced) progress.snapTo(1f) else progress.animateTo(1f,tween(320))}
 Column(modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
 AnimatedPigMascot(theme,"豬仔的${cuisineName}主題造型",Modifier.height(196.dp).fillMaxWidth().graphicsLayer {
 alpha=progress.value
 scaleX=.9f+.1f*progress.value;scaleY=scaleX
 },playKey=appearanceKey)
 Text(if(isDraw) "抽中菜式：$cuisineName" else "菜式：$cuisineName",style=MaterialTheme.typography.titleLarge)
 }
}
