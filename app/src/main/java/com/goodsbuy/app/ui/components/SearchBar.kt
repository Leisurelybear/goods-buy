package com.goodsbuy.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * 搜索框。聚焦时底色提亮、描边弹性出现、放大镜染色并轻微放大，
 * 清除按钮横向展开入场——让"进入搜索状态"有明确反馈。
 */
@Composable
fun SearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索…",
    onClear: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val containerColor by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = colorAnimSpec,
        label = "search_container_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_border_width"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = colorAnimSpec,
        label = "search_icon_tint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (focused) 1.15f else 1f,
        animationSpec = pressScaleSpring,
        label = "search_icon_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .border(
                border = BorderStroke(borderWidth, MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.text.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
        AnimatedVisibility(
            visible = value.text.isNotEmpty() && onClear != null,
            enter = expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)),
            exit = shrinkHorizontally(tween(160)) + fadeOut(tween(120))
        ) {
            IconButton(onClick = { onClear?.invoke() }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
