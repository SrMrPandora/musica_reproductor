package com.example.reproductordeaudio.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onTestClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    isSphereEffectEnabled: Boolean = true,
    isSyncing: Boolean = false
) {
    val dynamicColors = LocalDynamicColors.current
    var showOverflowMenu by remember { mutableStateOf(false) }

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

    val hasOverflowItems = onSortClick != null || onSphereToggleClick != null || onTestClick != null || onSettingsClick != null

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = dynamicColors.textColor
            )
        },
        actions = {
            if (onShuffleClick != null) {
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Canción Aleatoria",
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
            if (hasOverflowItems) {
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = dynamicColors.iconTint
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = dynamicColors.surface
                    ) {
                        if (onSortClick != null) {
                            DropdownMenuItem(
                                text = { Text("Ordenar lista", color = dynamicColors.textColor) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null,
                                        tint = dynamicColors.iconTint
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSortClick()
                                }
                            )
                        }
                        if (onSphereToggleClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isSphereEffectEnabled) "Efecto 3D: Activado" else "Efecto 3D: Desactivado",
                                        color = dynamicColors.textColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (isSphereEffectEnabled) dynamicColors.highlight else dynamicColors.textColor.copy(alpha = 0.38f)
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSphereToggleClick()
                                }
                            )
                        }
                        if (onTestClick != null) {
                            DropdownMenuItem(
                                text = { Text("Prototipo yt-dlp", color = dynamicColors.textColor) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = dynamicColors.iconTint
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onTestClick()
                                }
                            )
                        }
                        if (onSettingsClick != null) {
                            DropdownMenuItem(
                                text = { Text("Ajustes", color = dynamicColors.textColor) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = dynamicColors.iconTint
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSettingsClick()
                                }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = dynamicColors.background,
            titleContentColor = dynamicColors.textColor
        )
    )
}
