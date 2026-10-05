package com.example.reproductordeaudio.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import com.example.reproductordeaudio.ui.theme.LocalDynamicColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onSyncClick: () -> Unit,
    onSortClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null,
    onSphereToggleClick: (() -> Unit)? = null,
    isSphereEffectEnabled: Boolean = true,
    isSyncing: Boolean = false
) {
    val dynamicColors = LocalDynamicColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotationTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SyncRotationAngle"
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = dynamicColors.textColor
            )
        },
        actions = {
            if (onSphereToggleClick != null) {
                IconButton(onClick = onSphereToggleClick) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Efecto 3D Esfera",
                        tint = if (isSphereEffectEnabled) dynamicColors.highlight else dynamicColors.textColor.copy(alpha = 0.38f)
                    )
                }
            }
            if (onShuffleClick != null) {
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Canción Aleatoria",
                        tint = dynamicColors.iconTint
                    )
                }
            }
            if (onSortClick != null) {
                IconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Ordenar",
                        tint = dynamicColors.iconTint
                    )
                }
            }
            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar Biblioteca",
                    tint = if (isSyncing) dynamicColors.textColor.copy(alpha = 0.5f) else dynamicColors.iconTint,
                    modifier = if (isSyncing) Modifier.rotate(rotationAngle) else Modifier
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = dynamicColors.background,
            titleContentColor = dynamicColors.textColor
        )
    )
}
