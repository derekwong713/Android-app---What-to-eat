package hk.whatoeat.feature.result

import android.animation.ValueAnimator
import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.draw.WheelGeometry
import kotlin.math.*

@Composable fun Wheel(session: DrawSession,candidates: List<Restaurant>,reduced: Boolean,onFinish: ()->Unit) {
    val rotation=remember(session.drawId) { Animatable(0f) }
    val finish by rememberUpdatedState(onFinish)
    val count=session.orderedCandidateIds.size
    val title=stringResource(R.string.spinning)
    LaunchedEffect(session.drawId) {
        if(!reduced && ValueAnimator.areAnimatorsEnabled()) rotation.animateTo(WheelGeometry.target(count,session.winnerIndex).toFloat(),tween(3600,easing=CubicBezierEasing(0.12f,0.7f,0.12f,1f)))
        finish()
    }
    Text(title,Modifier.semantics { liveRegion=LiveRegionMode.Polite },style=MaterialTheme.typography.headlineSmall)
    val palette=listOf(Color(0xFFF9BC7D),Color(0xFFFFDDB8),Color(0xFFE5A66D),Color(0xFFF5CF8C))
    val pointer=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).semantics { contentDescription=title }) {
        val diameter=size.minDimension*0.86f
        val origin=Offset((size.width-diameter)/2,(size.height-diameter)/2)
        val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(48,33,22);textSize=if(count>8) 15.dp.toPx() else 12.dp.toPx();textAlign=Paint.Align.CENTER }
        rotate(rotation.value) {
            repeat(count) { i ->
                drawArc(palette[i%palette.size],i*360f/count,360f/count,true,origin,androidx.compose.ui.geometry.Size(diameter,diameter))
                val angle=Math.toRadians(WheelGeometry.center(count,i))
                val label=if(count>8) (i+1).toString() else candidates.getOrNull(i)?.name?.take(8) ?: (i+1).toString()
                drawContext.canvas.nativeCanvas.drawText(label,center.x+cos(angle).toFloat()*diameter*.31f,center.y+sin(angle).toFloat()*diameter*.31f,textPaint)
            }
        }
        val path=androidx.compose.ui.graphics.Path().apply { moveTo(center.x-12.dp.toPx(),4.dp.toPx());lineTo(center.x+12.dp.toPx(),4.dp.toPx());lineTo(center.x,28.dp.toPx());close() }
        drawPath(path,pointer)
    }
    Button(onClick=onFinish,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.skip_animation)) }
    session.orderedCandidateIds.forEachIndexed { i,key ->
        Text(stringResource(R.string.numbered_candidate,i+1,candidates.firstOrNull { it.key==key }?.name ?: stringResource(R.string.saved_placeholder)))
    }
}
