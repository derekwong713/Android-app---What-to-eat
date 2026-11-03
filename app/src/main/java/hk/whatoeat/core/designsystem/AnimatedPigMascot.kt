package hk.whatoeat.core.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import hk.whatoeat.R
import hk.whatoeat.domain.mascot.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val atlasCache=object:LruCache<Int,Bitmap>(20*1024*1024) {
 override fun sizeOf(key: Int,value: Bitmap)=value.allocationByteCount
}
// Test-only semantic data avoids making screen readers announce every animation frame.
val MascotFrameKey=SemanticsPropertyKey<Int>("MascotFrame")

@DrawableRes fun mascotAtlas(theme: MascotTheme,clip: MascotClip): Int=when(clip) {
 MascotClip.Welcome->R.drawable.mascot_motion_welcome
 MascotClip.Wok->R.drawable.mascot_motion_wok
 MascotClip.Drooling->R.drawable.mascot_motion_drooling
 MascotClip.Themed->when(theme) {
  MascotTheme.Base->R.drawable.mascot_motion_welcome
  MascotTheme.Chinese->R.drawable.mascot_motion_chinese
  MascotTheme.Ramen->R.drawable.mascot_motion_ramen
  MascotTheme.Sushi->R.drawable.mascot_motion_sushi
  MascotTheme.Korean->R.drawable.mascot_motion_korean
  MascotTheme.Southeast->R.drawable.mascot_motion_southeast
  MascotTheme.Curry->R.drawable.mascot_motion_curry
  MascotTheme.Western->R.drawable.mascot_motion_western
  MascotTheme.Grill->R.drawable.mascot_motion_grill
  MascotTheme.Vegetarian->R.drawable.mascot_motion_vegetarian
  MascotTheme.Seafood->R.drawable.mascot_motion_seafood
  MascotTheme.Hotpot->R.drawable.mascot_motion_hotpot
  MascotTheme.Dessert->R.drawable.mascot_motion_dessert
  MascotTheme.Wrap->R.drawable.mascot_motion_wrap
 }
}

/** Draws the approved atlas directly; no frame copies, whole-image rotation or frame-list bitmap allocation. */
@Composable fun AnimatedPigMascot(
 theme: MascotTheme,description: String?,modifier: Modifier=Modifier,
 clip: MascotClip=MascotClip.Themed,playKey: String=theme.name,
 cycles: Int?=2,reducedMotion: Boolean=reducedMascotMotion(),onFinished: ()->Unit={}
) {
 val resources=LocalResources.current
 val id=mascotAtlas(theme,clip)
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 val doneCallback by rememberUpdatedState(onFinished)
 val bitmap by key(id,resources,reducedMotion) {produceState<Bitmap?>(null) {
  if(!reducedMotion) value=withContext(Dispatchers.IO) {
   atlasCache.get(id) ?: BitmapFactory.decodeResource(resources,id,BitmapFactory.Options().apply {inScaled=false})?.also {atlasCache.put(id,it)}
  }
 }}
 var frame by remember(id,playKey) {mutableIntStateOf(0)}
 val elapsed=remember(id,playKey) {longArrayOf(0L)}
 var completed by remember(id,playKey) {mutableStateOf(false)}
 LaunchedEffect(id,playKey,cycles,reducedMotion,bitmap,lifecycle) {
  if(reducedMotion || bitmap==null) return@LaunchedEffect
  lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
   var previous: Long?=null
   while(!completed) {
    withFrameMillis {now ->
     previous?.let {elapsed[0]+=(now-it).coerceAtLeast(0)}
     previous=now
     frame=MascotTimeline.frameAt(clip,elapsed[0],cycles)
     if(MascotTimeline.finished(clip,elapsed[0],cycles)) {completed=true;doneCallback()}
    }
   }
  }
 }
 val image=remember(bitmap) {bitmap?.asImageBitmap()}
 if(reducedMotion || image==null) {
  PigMascot(theme,description,modifier.semantics {stateDescription=if(reducedMotion) "靜態造型" else "動畫載入中"})
 } else Canvas(modifier.testTag("mascot_${theme.name.lowercase()}").semantics {
  description?.let {contentDescription=it};this[MascotFrameKey]=frame
 }) {
  // The approved atlases have odd dimensions: calculate both edges to avoid losing the final row/column.
  val left=(frame%4)*image.width/4;val right=(frame%4+1)*image.width/4
  val top=(frame/4)*image.height/2;val bottom=(frame/4+1)*image.height/2
  val side=size.minDimension.roundToInt()
  drawImage(image,srcOffset=IntOffset(left,top),srcSize=IntSize(right-left,bottom-top),
   dstOffset=IntOffset(((size.width-side)/2).roundToInt(),((size.height-side)/2).roundToInt()),dstSize=IntSize(side,side))
 }
}
