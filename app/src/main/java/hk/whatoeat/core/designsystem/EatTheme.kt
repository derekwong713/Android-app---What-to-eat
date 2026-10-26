package hk.whatoeat.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable fun EatTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if(dark) darkColorScheme(primary=Color(0xFFFFB68D),onPrimary=Color(0xFF572000),
        background=Color(0xFF201A16),surface=Color(0xFF201A16),onBackground=Color(0xFFF3E0D1),
        secondaryContainer=Color(0xFF513324)) else lightColorScheme(primary=Color(0xFFA94310),onPrimary=Color.White,
        background=Color(0xFFFFF8EE),surface=Color(0xFFFFF8EE),onBackground=Color(0xFF302116),
        secondaryContainer=Color(0xFFFFDBBC),onSecondaryContainer=Color(0xFF342010))
    MaterialTheme(colorScheme=colors,typography=Typography(),content=content)
}
