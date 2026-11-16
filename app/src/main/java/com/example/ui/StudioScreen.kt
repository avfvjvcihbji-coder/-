package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.export.ExportUiState
import com.example.motion.ExportFormat
import com.example.motion.MotionFrameRenderer
import com.example.motion.PreviewBackground
import com.example.motion.VideoResolution
import com.example.motion.VideoSettings
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NoticeAmberBg
import com.example.ui.theme.NoticeAmberBorder
import com.example.ui.theme.NoticeAmberInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray
import com.example.viewmodel.GenerationUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    scriptText: String,
    videoSettings: VideoSettings,
    generationState: GenerationUiState,
    selectedSceneIndex: Int?,
    currentFrame: Int,
    isPlaying: Boolean,
    previewBackground: PreviewBackground,
    previewResolution: VideoResolution,
    exportState: ExportUiState,
    isSampleAiMode: Boolean,
    renderer: MotionFrameRenderer,
    onScriptChange: (String) -> Unit,
    onApplySampleScript: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onGenerateMotion: () -> Unit,
    onSelectScene: (Int?) -> Unit,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onSeekFrame: (Int) -> Unit,
    onChangeBackground: (PreviewBackground) -> Unit,
    onChangeResolution: (VideoResolution) -> Unit,
    onRegenerateScene: (Int) -> Unit,
    onStartExport: (ExportFormat) -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scriptFocusRequester = androidx.compose.runtime.remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Firebase AI Notice Banner when Firebase setup was declined
            if (isSampleAiMode) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = NoticeAmberBg,
                        border = BorderStroke(1.dp, NoticeAmberBorder),
                        modifier = Modifier.fillMaxWidth().testTag("ai_notice_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NoticeAmberInk,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_firebase_notice_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoticeAmberInk
                                )
                                Text(
                                    text = stringResource(R.string.ai_firebase_notice_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoticeAmberInk
                                )
                            }
                        }
                    }
                }
            }

            // Hero Studio Banner (shows prominently before first generation, compact after)
            if (generationState !is GenerationUiState.Ready) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_studio_hero_1794843547449),
                                contentDescription = "تصویر استودیوی موشن‌گرافیک",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp)
                                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Remotion Motion Studio",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = CharcoalBlack
                                )
                                Text(
                                    text = "تحلیل مفهومی فیلمنامه، طراحی استعاره بصری اختصاصی و تولید هم‌زمان خروجی Transparent Alpha و White MP4.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }

            // Script Input & Duration Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "متن یا فیلمنامه ویدیو",
                                style = MaterialTheme.typography.titleLarge,
                                color = CharcoalBlack
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceStudioGray,
                                modifier = Modifier.testTag("settings_shortcut_badge")
                            ) {
                                Text(
                                    text = "${videoSettings.resolution.width}×${videoSettings.resolution.height} · ${videoSettings.fps}FPS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SlateMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = scriptText,
                            onValueChange = onScriptChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.script_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMuted
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 136.dp)
                                .focusRequester(scriptFocusRequester)
                                .testTag("script_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = SurfaceStudioGray,
                                unfocusedContainerColor = SurfaceStudioGray
                            ),
                            maxLines = 8
                        )

                        // Quick Concept Chips (multi-paragraph examples)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    onApplySampleScript(
                                        "هوش مصنوعی داده‌های پراکنده را در یک شبکه عصبی هوشمند به هم متصل می‌کند.\n\n" +
                                            "نتیجه این معماری، رشد تصاعدی بهره‌وری و سرعت تصمیم‌گیری در مقیاس جهانی است."
                                    )
                                },
                                label = { Text("هوش مصنوعی + رشد") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    onApplySampleScript(
                                        "سرعت پردازش در نسل جدید زیرساخت‌ها مرزهای تأخیر را از بین برده است.\n\n" +
                                            "در مقایسه با سیستم‌های سنتی، معماری ماژولار جدید ۳ برابر پایدارتر عمل می‌کند."
                                    )
                                },
                                label = { Text("سرعت + مقایسه") },
                                leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    onApplySampleScript(
                                        "دقت مهندسی در پایش لحظه‌ای سیستم، امنیت کامل جریان اطلاعات را تضمین می‌کند."
                                    )
                                },
                                label = { Text("دقت و امنیت HUD") },
                                leadingIcon = { Icon(Icons.Default.Layers, null, Modifier.size(16.dp)) }
                            )
                        }

                        // Duration Selector per scene
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "مدت زمان هر صحنه",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CharcoalBlack
                                )
                                Text(
                                    text = "${videoSettings.defaultDurationSec} ثانیه (${videoSettings.defaultDurationSec * videoSettings.fps} فریم)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricBlue
                                )
                            }
                            Slider(
                                value = videoSettings.defaultDurationSec.toFloat(),
                                onValueChange = { onDurationChange(it.toInt()) },
                                valueRange = 3f..15f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricBlue,
                                    activeTrackColor = ElectricBlue
                                ),
                                modifier = Modifier.testTag("duration_slider")
                            )
                        }

                        // Primary Generate Motion CTA + Advanced Settings button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onGenerateMotion,
                                enabled = generationState !is GenerationUiState.Analyzing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("generate_motion_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                if (generationState is GenerationUiState.Analyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("در حال تحلیل و طراحی سناریو...")
                                } else {
                                    Icon(Icons.Default.MovieCreation, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.generate_motion))
                                }
                            }
                            OutlinedButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.height(52.dp).testTag("advanced_settings_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("تنظیمات پیشرفته")
                            }
                        }
                    }
                }
            }

            // Error state
            if (generationState is GenerationUiState.Error) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generationState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Live Remotion-Style Preview & Dual Export Panel
            if (generationState is GenerationUiState.Ready) {
                val project = generationState.project
                val activeScene = selectedSceneIndex?.let { project.scenes.getOrNull(it) } ?: project.scenes.first()

                item {
                    MotionPlayerCard(
                        project = project,
                        selectedSceneIndex = selectedSceneIndex,
                        currentFrame = currentFrame,
                        isPlaying = isPlaying,
                        previewBackground = previewBackground,
                        previewResolution = previewResolution,
                        renderer = renderer,
                        onSelectScene = onSelectScene,
                        onTogglePlayPause = onTogglePlayPause,
                        onRestart = onRestart,
                        onStepFrame = onStepFrame,
                        onSeekFrame = onSeekFrame,
                        onChangeBackground = onChangeBackground,
                        onChangeResolution = onChangeResolution,
                        onRegenerateActiveScene = {
                            onRegenerateScene(selectedSceneIndex ?: 0)
                        },
                        onEditScript = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                                scriptFocusRequester.requestFocus()
                            }
                        },
                        onRenderAgain = {
                            onStartExport(ExportFormat.DUAL_MOV_AND_MP4)
                            onNavigateToExport()
                        }
                    )
                }

                // Active Scenario Summary Pill Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = SurfaceStudioGray),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "سناریوی طراحی‌شده: ${activeScene.archetype.labelFa}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepPurple
                            )
                            Text(
                                text = "استعاره بصری: ${activeScene.scenario.visualMetaphor}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalBlack
                            )
                            Text(
                                text = "حرکت دوربین: ${activeScene.scenario.cameraMovement}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                        }
                    }
                }

                // Dual Export Quick Panel
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "خروجی دوگانه هماهنگ (Dual Export)",
                                style = MaterialTheme.typography.titleLarge,
                                color = CharcoalBlack
                            )
                            Text(
                                text = "هر دو خروجی از یک Composition مشترک، با زمان‌بندی و حرکت دوربین یکسان رندر می‌شوند.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )

                            AnimatedVisibility(visible = exportState is ExportUiState.Rendering) {
                                val rendering = exportState as? ExportUiState.Rendering
                                if (rendering != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = rendering.stepTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ElectricBlue
                                        )
                                        LinearProgressIndicator(
                                            progress = { rendering.overallProgress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = ElectricBlue
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onStartExport(ExportFormat.TRANSPARENT_MOV_ONLY)
                                        onNavigateToExport()
                                    },
                                    enabled = exportState !is ExportUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(52.dp).testTag("export_mov_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                                ) {
                                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("MOV شفاف", maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        onStartExport(ExportFormat.WHITE_MP4_ONLY)
                                        onNavigateToExport()
                                    },
                                    enabled = exportState !is ExportUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(52.dp).testTag("export_mp4_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalBlack)
                                ) {
                                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("MP4 سفید", maxLines = 1)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onStartExport(ExportFormat.DUAL_MOV_AND_MP4)
                                    onNavigateToExport()
                                },
                                enabled = exportState !is ExportUiState.Rendering,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_dual_both_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("رندر هم‌زمان هر دو خروجی (Dual Export) + PNG ZIP")
                            }
                        }
                    }
                }
            }
        }
    }
}
