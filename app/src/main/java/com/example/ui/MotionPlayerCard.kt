package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.motion.MotionFrameRenderer
import com.example.motion.MotionProject
import com.example.motion.PreviewBackground
import com.example.motion.VideoResolution
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionPlayerCard(
    project: MotionProject,
    selectedSceneIndex: Int?,
    currentFrame: Int,
    isPlaying: Boolean,
    previewBackground: PreviewBackground,
    previewResolution: VideoResolution,
    renderer: MotionFrameRenderer,
    onSelectScene: (Int?) -> Unit,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onSeekFrame: (Int) -> Unit,
    onChangeBackground: (PreviewBackground) -> Unit,
    onChangeResolution: (VideoResolution) -> Unit,
    onRegenerateActiveScene: () -> Unit,
    onEditScript: () -> Unit,
    onRenderAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProject = remember(project, selectedSceneIndex) {
        if (selectedSceneIndex != null && selectedSceneIndex in project.scenes.indices) {
            project.copy(scenes = listOf(project.scenes[selectedSceneIndex]))
        } else {
            project
        }
    }
    val totalFrames = activeProject.totalFrames.coerceAtLeast(1)
    val clampedFrame = currentFrame.coerceIn(0, totalFrames - 1)
    val fps = project.settings.fps.coerceAtLeast(1)

    // Render offscreen preview bitmap at chosen preview resolution so scaling & Safe Area stay 1:1 with export
    val previewBitmap = remember(previewResolution) {
        Bitmap.createBitmap(previewResolution.width, previewResolution.height, Bitmap.Config.ARGB_8888)
    }
    DisposableEffect(previewBitmap) {
        onDispose { previewBitmap.recycle() }
    }

    val transparentMode = previewBackground == PreviewBackground.CHECKERBOARD
    val customBgInt = when (previewBackground) {
        PreviewBackground.WHITE -> android.graphics.Color.WHITE
        PreviewBackground.GRAY -> 0xFFE2E8F0.toInt()
        PreviewBackground.CHECKERBOARD -> null
    }

    val imageBitmap = remember(activeProject, clampedFrame, previewBackground, previewResolution) {
        val canvas = AndroidCanvas(previewBitmap)
        renderer.renderProjectFrame(
            canvas = canvas,
            width = previewBitmap.width,
            height = previewBitmap.height,
            project = activeProject,
            globalFrame = clampedFrame,
            transparentBackground = transparentMode,
            customPreviewBgColor = customBgInt
        )
        previewBitmap.asImageBitmap()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row + Scene Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "پیش‌نمایش فریم‌محور (16:9)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CharcoalBlack
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceStudioGray
                ) {
                    Text(
                        text = "${previewResolution.width}×${previewResolution.height} · ${fps}FPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Scene selector pills if multi-paragraph
            if (project.scenes.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSceneIndex == null,
                        onClick = { onSelectScene(null) },
                        label = { Text("کل ویدیو (${project.scenes.size} صحنه)") },
                        modifier = Modifier.testTag("preview_all_scenes_chip")
                    )
                    project.scenes.forEachIndexed { idx, _ ->
                        FilterChip(
                            selected = selectedSceneIndex == idx,
                            onClick = { onSelectScene(idx) },
                            label = { Text("صحنه ${idx + 1}") },
                            modifier = Modifier.testTag("preview_scene_${idx}_chip")
                        )
                    }
                }
            }

            // 16:9 Preview Stage with optional Checkerboard background (strictly UI-only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (previewBackground == PreviewBackground.CHECKERBOARD) {
                        val cell = 20.dp.toPx()
                        val cols = (size.width / cell).toInt() + 1
                        val rows = (size.height / cell).toInt() + 1
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val isDark = (r + c) % 2 == 1
                                drawRect(
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell, cell)
                                )
                            }
                        }
                    }
                    // Draw rendered frame scaled smoothly into 16:9 box
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        previewBitmap,
                        null,
                        android.graphics.RectF(0f, 0f, size.width, size.height),
                        null
                    )
                }
            }

            // Frame Timeline Scrubber & Timecode
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = clampedFrame.toFloat(),
                    onValueChange = { onSeekFrame(it.toInt()) },
                    valueRange = 0f..(totalFrames - 1).coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("timeline_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val curSec = clampedFrame / fps.toFloat()
                    val totSec = totalFrames / fps.toFloat()
                    Text(
                        text = "FRAME %04d / %04d".format(clampedFrame, totalFrames),
                        style = MaterialTheme.typography.labelMedium,
                        color = CharcoalBlack
                    )
                    Text(
                        text = "%.2fs / %.2fs".format(curSec, totSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                }
            }

            // Transport Bar: Prev Frame, Play/Pause, Next Frame, Restart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(48.dp).testTag("restart_button")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "شروع مجدد", tint = SlateMuted)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onStepFrame(-1) },
                    modifier = Modifier.size(48.dp).testTag("prev_frame_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "فریم قبل",
                        tint = CharcoalBlack
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = { onStepFrame(1) },
                    modifier = Modifier.size(48.dp).testTag("next_frame_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "فریم بعد",
                        tint = CharcoalBlack
                    )
                }
            }

            // Preview Background Switcher (White / Gray / Alpha Checkerboard)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "پس‌زمینه پیش‌نمایش (شطرنجی فقط جهت بررسی شفافیت است)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PreviewBackground.entries.forEach { bg ->
                        FilterChip(
                            selected = previewBackground == bg,
                            onClick = { onChangeBackground(bg) },
                            label = { Text(bg.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.testTag("preview_bg_${bg.name.lowercase()}")
                        )
                    }
                }
            }

            // Preview Resolution Switcher
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoResolution.entries.forEach { res ->
                    FilterChip(
                        selected = previewResolution == res,
                        onClick = { onChangeResolution(res) },
                        label = { Text("${res.height}p") },
                        modifier = Modifier.testTag("preview_res_${res.height}")
                    )
                }
            }

            // Quick Actions: Regenerate Scene, Edit Script, Render Again
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRegenerateActiveScene,
                    modifier = Modifier.height(48.dp).testTag("player_regenerate_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("بازطراحی صحنه")
                }
                OutlinedButton(
                    onClick = onEditScript,
                    modifier = Modifier.height(48.dp).testTag("player_edit_script_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ویرایش فیلمنامه")
                }
                OutlinedButton(
                    onClick = onRenderAgain,
                    modifier = Modifier.height(48.dp).testTag("player_render_again_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("رندر مجدد")
                }
            }
        }
    }
}
