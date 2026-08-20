package com.goodsbuy.app.ui.collectible.form

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.util.FadeShape
import kotlin.math.ceil
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeFadeEditScreen(
    sourcePath: String,
    onCancel: () -> Unit,
    onDone: (String) -> Unit,
    viewModel: EdgeFadeEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preview by viewModel.previewBitmap.collectAsState()
    var consumed by remember { mutableStateOf(false) }

    LaunchedEffect(sourcePath) { viewModel.initialize(sourcePath) }

    LaunchedEffect(Unit) {
        viewModel.done.collect { resultPath ->
            if (!consumed) {
                consumed = true
                onDone(resultPath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片渐隐") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "取消")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::confirm, enabled = !uiState.isProcessing) {
                        Text(if (uiState.isProcessing) "处理中…" else "确认")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FadeShape.entries.forEach { shape ->
                    FilterChip(
                        selected = uiState.shape == shape,
                        onClick = { viewModel.onShapeChange(shape) },
                        label = { Text(shape.displayName) }
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(12.dp)).checkerboard(),
                contentAlignment = Alignment.Center
            ) {
                preview?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "渐隐预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                if (uiState.isPreviewLoading) {
                    CircularProgressIndicator()
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("渐隐强度", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.intensity,
                    onValueChange = viewModel::onIntensityChange,
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..1f
                )
                Text(
                    "${(uiState.intensity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                onClick = viewModel::reset,
                enabled = !uiState.isProcessing && uiState.intensity > 0f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复原图")
            }
        }
    }
}

private val FadeShape.displayName: String
    get() = when (this) {
        FadeShape.ELLIPSE -> "椭圆"
        FadeShape.CIRCLE -> "圆形"
        FadeShape.ROUNDED_RECT -> "圆角矩形"
        FadeShape.RECT -> "矩形"
    }

private fun Modifier.checkerboard(): Modifier = drawWithContent {
    val cell = 8.dp.toPx()
    val cols = ceil(size.width / cell).toInt()
    val rows = ceil(size.height / cell).toInt()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            drawRect(
                color = if ((row + col) % 2 == 0) Color(0xFFE0E0E0) else Color(0xFFF5F5F5),
                topLeft = Offset(col * cell, row * cell),
                size = Size(cell, cell)
            )
        }
    }
    drawContent()
}