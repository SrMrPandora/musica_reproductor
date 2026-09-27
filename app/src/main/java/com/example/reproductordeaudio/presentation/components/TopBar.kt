package com.example.reproductordeaudio.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
    isSphereEffectEnabled: Boolean = true
) {
    val dynamicColors = LocalDynamicColors.current

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
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
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
            IconButton(onClick = onSyncClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar Biblioteca",
                    tint = dynamicColors.iconTint
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = dynamicColors.background,
            titleContentColor = dynamicColors.textColor
        )
    )
}
