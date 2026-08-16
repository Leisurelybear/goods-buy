package com.goodsbuy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goodsbuy.app.domain.model.Collectible
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectibleCard(
    collectible: Collectible,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 140.dp,
    showName: Boolean = true,
    showPrice: Boolean = true,
    showStatus: Boolean = true,
    fontSize: Int = 1,
    homeImageAutoRotate: Boolean = false,
    homeImageRotationIntervalSeconds: Int = 3,
    onLongPress: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    batchMode: Boolean = false
) {
    val nameSize = when (fontSize) { 0 -> 11f; 1 -> 12f; else -> 14f }
    val priceSize = when (fontSize) { 0 -> 9f; 1 -> 10f; else -> 12f }
    val statusSize = when (fontSize) { 0 -> 9f; 1 -> 10f; else -> 11f }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = pressScaleSpring,
        label = "card_scale"
    )

    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = fadeAnimSpec,
        label = "card_selection"
    )

    val statusColor by animateColorAsState(
        targetValue = Color(collectible.status.colorHex),
        animationSpec = colorAnimSpec,
        label = "card_status_color"
    )

    Card(
        modifier = modifier
            .width(cardSize)
            .aspectRatio(0.75f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = { if (batchMode) onSelect?.invoke() else onClick() },
                onLongClick = { if (onLongPress != null) onLongPress() },
                enabled = !batchMode || onSelect != null
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (collectible.imagePaths.isNotEmpty()) {
                CardImage(
                    collectible = collectible,
                    autoRotate = homeImageAutoRotate,
                    rotationIntervalSeconds = homeImageRotationIntervalSeconds
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (showStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(statusColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = collectible.status.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = statusSize.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectionAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000).copy(alpha = 0.5f * selectionAlpha)),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { onSelect?.invoke() },
                        modifier = Modifier.padding(6.dp).size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = selectionAlpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(52.dp)
                    .background(
                        Color.Black.copy(alpha = 0.55f),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (showName) {
                            Text(
                                text = collectible.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = nameSize.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (showPrice) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\u00a5${collectible.purchasePrice}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = priceSize.sp),
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                    if (collectible.imagePaths.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "+${collectible.imagePaths.size - 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardImage(
    collectible: Collectible,
    autoRotate: Boolean,
    rotationIntervalSeconds: Int
) {
    val imagePaths = collectible.imagePaths
    if (!autoRotate || imagePaths.size <= 1) {
        // Avoid an animation node and a coroutine for the common, static-card case.
        AsyncImage(
            model = imagePaths.first(),
            contentDescription = collectible.name,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        return
    }

    var imageIndex by remember(collectible.id, imagePaths) { mutableIntStateOf(0) }
    LaunchedEffect(collectible.id, imagePaths, rotationIntervalSeconds) {
        imageIndex = 0
        val intervalMillis = rotationIntervalSeconds.coerceIn(1, 60) * 1_000L
        while (true) {
            delay(intervalMillis)
            imageIndex = (imageIndex + 1) % imagePaths.size
        }
    }

    AnimatedContent(
        targetState = imagePaths[imageIndex],
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "card_image_rotation"
    ) { path ->
        AsyncImage(
            model = path,
            contentDescription = collectible.name,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
