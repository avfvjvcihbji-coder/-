package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.motion.MotionProject
import com.example.motion.MotionScene
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray
import com.example.viewmodel.GenerationUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenesScreen(
    generationState: GenerationUiState,
    onPreviewScene: (Int?) -> Unit,
    onRegenerateScene: (Int) -> Unit,
    onChangeSceneDuration: (Int, Int) -> Unit,
    onRenderSingleScene: (Int) -> Unit,
    onRenderUnifiedProject: () -> Unit,
    onGoToStudio: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (generationState !is GenerationUiState.Ready) {
            EmptyScenesState(onGoToStudio = onGoToStudio)
            return@Box
        }

        val project: MotionProject = generationState.project
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceStudioGray),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدیریت صحنه‌های مستقل (${project.scenes.size} صحنه)",
                                style = MaterialTheme.typography.titleLarge,
                                color = CharcoalBlack
                            )
                            Text(
                                text = "مجموع: ${project.totalDurationSeconds}s · ${project.totalFrames}f",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricBlue
                            )
                        }
                        Text(
                            text = "هر پاراگراف با استعاره بصری، ترکیب‌بندی، آبجکت‌ها، حرکت دوربین و ترنزیشن متمایز طراحی شده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onPreviewScene(null) },
                                modifier = Modifier.height(48.dp).testTag("scenes_preview_all_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("پیش‌نمایش یکپارچه")
                            }
                            OutlinedButton(
                                onClick = onRenderUnifiedProject,
                                modifier = Modifier.height(48.dp).testTag("scenes_render_all_button")
                            ) {
                                Icon(Icons.Default.VideoFile, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("رندر کل ویدیو")
                            }
                        }
                    }
                }
            }

            itemsIndexed(project.scenes, key = { _, s -> s.id }) { index, scene ->
                SceneDetailCard(
                    scene = scene,
                    index = index,
                    onPreview = { onPreviewScene(index) },
                    onRegenerate = { onRegenerateScene(index) },
                    onDurationDelta = { delta ->
                        onChangeSceneDuration(index, scene.durationSeconds + delta)
                    },
                    onRenderSingle = { onRenderSingleScene(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SceneDetailCard(
    scene: MotionScene,
    index: Int,
    onPreview: () -> Unit,
    onRegenerate: () -> Unit,
    onDurationDelta: (Int) -> Unit,
    onRenderSingle: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scene.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = CharcoalBlack
                    )
                    Text(
                        text = scene.layoutMode.labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepPurple
                    )
                }

                // Palette swatches
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        scene.palette.primaryColor,
                        scene.palette.secondaryColor,
                        scene.palette.accentColor
                    ).forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(1.dp, SurfaceCardBorder, CircleShape)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceStudioGray
            ) {
                Text(
                    text = scene.sourceText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalBlack,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }

            // Duration stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدت صحنه: ${scene.durationSeconds} ثانیه (${scene.totalFrames} فریم)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalBlack
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDurationDelta(-1) },
                        modifier = Modifier.size(48.dp).testTag("scene_${index}_dec_duration")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کاهش مدت")
                    }
                    IconButton(
                        onClick = { onDurationDelta(1) },
                        modifier = Modifier.size(48.dp).testTag("scene_${index}_inc_duration")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزایش مدت")
                    }
                }
            }

            // Full 8-part AI Scenario Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScenarioLine("پیام اصلی", scene.scenario.coreMessage)
                ScenarioLine("استعاره بصری", scene.scenario.visualMetaphor)
                ScenarioLine("کانسپت بصری", scene.scenario.visualConcept)
                ScenarioLine("طراحی آبجکت‌ها", scene.scenario.objectDesign)
                ScenarioLine("کارگردانی انیمیشن", scene.scenario.animationDirection)
                ScenarioLine("حرکت دوربین", scene.scenario.cameraMovement)
                ScenarioLine("ترکیب‌بندی", scene.scenario.sceneComposition)
                ScenarioLine("ترنزیشن", scene.scenario.transitionDesign)
                ScenarioLine("زمان‌بندی", scene.scenario.timing)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.height(48.dp).testTag("scene_${index}_preview_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("پخش صحنه")
                }
                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier.height(48.dp).testTag("scene_${index}_regenerate_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("سناریوی جدید")
                }
                OutlinedButton(
                    onClick = onRenderSingle,
                    modifier = Modifier.height(48.dp).testTag("scene_${index}_render_button")
                ) {
                    Icon(Icons.Default.Movie, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("رندر جداگانه")
                }
            }
        }
    }
}

@Composable
private fun ScenarioLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelLarge,
            color = ElectricBlue
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyScenesState(onGoToStudio: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = SlateMuted,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "هنوز صحنه‌ای تولید نشده است",
            style = MaterialTheme.typography.titleLarge,
            color = CharcoalBlack
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "متن یا فیلمنامه خود را در استودیو وارد کنید تا برای هر پاراگراف یک سناریوی موشن‌گرافیک اختصاصی ساخته شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateMuted
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGoToStudio,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            Text("رفتن به استودیو")
        }
    }
}
