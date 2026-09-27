package com.example.reproductordeaudio.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
import com.example.reproductordeaudio.domain.model.Song

const val DYNAMIC_THEME_TRANSITION_DURATION = 1000

data class DynamicColorScheme(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val iconTint: Color,
    val highlight: Color,
    val textColor: Color
)

val DefaultDynamicColors = DynamicColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1F28),
    accent = Color(0xFFBB86FC),
    iconTint = Color(0xFF03DAC6),
    highlight = Color(0xFF6200EE),
    textColor = Color.White
)

val LocalDynamicColors = compositionLocalOf { DefaultDynamicColors }

@Composable
fun ProvideDynamicColors(
    currentSong: Song?,
    cardIdentities: Map<Long, CardIdentityEntity>,
    content: @Composable () -> Unit
) {
    val targetScheme = remember(currentSong?.id, cardIdentities[currentSong?.id]?.colorsJson) {
        val identity = currentSong?.let { cardIdentities[it.id] }
        val parsedColors = identity?.colorsJson?.split(",")?.mapNotNull { str ->
            str.trim().toIntOrNull()?.let { Color(it) }
        }

        if (parsedColors != null && parsedColors.isNotEmpty()) {
            val bg = parsedColors[0]
            val surf = parsedColors.getOrElse(1) { bg }
            val acc = parsedColors.getOrElse(2) { surf }
            val iconT = parsedColors.getOrElse(3) { acc }
            val high = parsedColors.last()
            val text = Color(identity.textColorArgb)

            DynamicColorScheme(
                background = bg,
                surface = surf,
                accent = acc,
                iconTint = iconT,
                highlight = high,
                textColor = text
            )
        } else {
            DefaultDynamicColors
        }
    }

    val animatedBackground by animateColorAsState(
        targetValue = targetScheme.background,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_bg"
    )
    val animatedSurface by animateColorAsState(
        targetValue = targetScheme.surface,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_surface"
    )
    val animatedAccent by animateColorAsState(
        targetValue = targetScheme.accent,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_accent"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = targetScheme.iconTint,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_icontint"
    )
    val animatedHighlight by animateColorAsState(
        targetValue = targetScheme.highlight,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_highlight"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = targetScheme.textColor,
        animationSpec = tween(DYNAMIC_THEME_TRANSITION_DURATION),
        label = "dynamic_textcolor"
    )

    val animatedScheme = DynamicColorScheme(
        background = animatedBackground,
        surface = animatedSurface,
        accent = animatedAccent,
        iconTint = animatedIconTint,
        highlight = animatedHighlight,
        textColor = animatedTextColor
    )

    CompositionLocalProvider(LocalDynamicColors provides animatedScheme) {
        content()
    }
}
