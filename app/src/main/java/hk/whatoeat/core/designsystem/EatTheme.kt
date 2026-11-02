package hk.whatoeat.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared muted sage, dusty rose, blue-grey and warm stone design tokens. */
object MorandiPalette {
 val wheel=listOf(0xFFBCC8BC,0xFFD1B9B5,0xFFB8C5CE,0xFFD5C8B5,0xFFC5BED0,0xFFB5C8C3).map {Color(it)}
 val wheelInk=Color(0xFF303630)
}
private val LightColors=lightColorScheme(
 primary=Color(0xFF526657),onPrimary=Color(0xFFFFFFFF),primaryContainer=Color(0xFFDCE5D9),onPrimaryContainer=Color(0xFF263D2B),
 secondary=Color(0xFF765E5D),onSecondary=Color.White,secondaryContainer=Color(0xFFEADAD7),onSecondaryContainer=Color(0xFF493635),
 tertiary=Color(0xFF536675),onTertiary=Color.White,tertiaryContainer=Color(0xFFDCE4EA),onTertiaryContainer=Color(0xFF283B48),
 background=Color(0xFFF6F3EE),onBackground=Color(0xFF30332F),surface=Color(0xFFF6F3EE),onSurface=Color(0xFF30332F),
 surfaceVariant=Color(0xFFE4E5DC),onSurfaceVariant=Color(0xFF4D544B),outline=Color(0xFF767C72),outlineVariant=Color(0xFFC7CBC0),
 surfaceContainer=Color(0xFFEBEAE2),surfaceContainerLow=Color(0xFFF0EEE7),surfaceContainerHigh=Color(0xFFE4E4DB),surfaceContainerHighest=Color(0xFFDEDFD5),surfaceContainerLowest=Color(0xFFFCFAF6),
 error=Color(0xFF914F4B),onError=Color.White,errorContainer=Color(0xFFF2DAD6),onErrorContainer=Color(0xFF612E2A)
)
private val DarkColors=darkColorScheme(
 primary=Color(0xFFB5C7B5),onPrimary=Color(0xFF23382A),primaryContainer=Color(0xFF3C5141),onPrimaryContainer=Color(0xFFDCE5D9),
 secondary=Color(0xFFD7BEBB),onSecondary=Color(0xFF422E2E),secondaryContainer=Color(0xFF5C4443),onSecondaryContainer=Color(0xFFEADAD7),
 tertiary=Color(0xFFB9C9D6),onTertiary=Color(0xFF253A47),tertiaryContainer=Color(0xFF3C5260),onTertiaryContainer=Color(0xFFDCE4EA),
 background=Color(0xFF1F2420),onBackground=Color(0xFFE2E4DB),surface=Color(0xFF1F2420),onSurface=Color(0xFFE2E4DB),
 surfaceVariant=Color(0xFF434B42),onSurfaceVariant=Color(0xFFC7CDBF),outline=Color(0xFF929B8D),outlineVariant=Color(0xFF434B42),
 surfaceContainer=Color(0xFF2A302A),surfaceContainerLow=Color(0xFF252B25),surfaceContainerHigh=Color(0xFF343A33),surfaceContainerHighest=Color(0xFF3E453C),surfaceContainerLowest=Color(0xFF171C18),
 error=Color(0xFFE3B4AD),onError=Color(0xFF542A26),errorContainer=Color(0xFF713C36),onErrorContainer=Color(0xFFF2DAD6)
)
@Composable fun EatTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme=if(isSystemInDarkTheme()) DarkColors else LightColors,
 typography=Typography(),shapes=Shapes(small=RoundedCornerShape(10.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp)),content=content)
}
