package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.export.ExportArtifact
import com.example.export.ExportUiState
import com.example.motion.ExportFormat
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.ErrorRedInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenInk
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray
import com.example.viewmodel.GenerationUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    generationState: GenerationUiState,
    selectedSceneIndex: Int?,
    exportState: ExportUiState,
    remotionTsxCode: String,
    onSelectSceneScope: (Int?) -> Unit,
    onStartExport: (ExportFormat) -> Unit,
    onSaveToDownloads: (ExportArtifact) -> Unit,
    onShareArtifact: (ExportArtifact) -> Unit,
    onGoToStudio: () -> Unit
) {
    var showTsxDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (generationState !is GenerationUiState.Ready) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.MovieFilter, null, tint = SlateMuted, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "ابتدا در استودیو یک موشن‌گرافیک تولید کنید",
                    style = MaterialTheme.typography.titleLarge,
                    color = CharcoalBlack
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGoToStudio,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("بازگشت به استودیو")
                }
            }
            return@Box
        }

        val project = generationState.project
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "مرکز رندر و خروجی دوگانه",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CharcoalBlack
                        )
                        Text(
                            text = "رندر واقعی فریم‌به‌فریم با حفظ دقیق هماهنگی موقعیت عناصر، حرکت دوربین و زمان‌بندی میان نسخه شفاف و نسخه زمینه سفید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )

                        // Scope selector
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSceneIndex == null,
                                onClick = { onSelectSceneScope(null) },
                                label = { Text("کل ویدیو (${project.totalDurationSeconds}s)") }
                            )
                            project.scenes.forEachIndexed { idx, scene ->
                                FilterChip(
                                    selected = selectedSceneIndex == idx,
                                    onClick = { onSelectSceneScope(idx) },
                                    label = { Text("صحنه ${idx + 1} (${scene.durationSeconds}s)") }
                                )
                            }
                        }

                        // Export Actions
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onStartExport(ExportFormat.DUAL_MOV_AND_MP4) },
                                enabled = exportState !is ExportUiState.Rendering,
                                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("export_screen_dual_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.MovieFilter, null)
                                Spacer(Modifier.width(8.dp))
                                Text("تولید هم‌زمان هر دو خروجی (Transparent MOV + White MP4)")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onStartExport(ExportFormat.TRANSPARENT_MOV_ONLY) },
                                    enabled = exportState !is ExportUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(50.dp).testTag("export_screen_mov_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                                ) {
                                    Text("فقط MOV شفاف")
                                }
                                Button(
                                    onClick = { onStartExport(ExportFormat.WHITE_MP4_ONLY) },
                                    enabled = exportState !is ExportUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(50.dp).testTag("export_screen_mp4_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalBlack)
                                ) {
                                    Text("فقط MP4 سفید")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onStartExport(ExportFormat.PNG_SEQUENCE_ZIP) },
                                    enabled = exportState !is ExportUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_screen_png_zip_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("سکانس PNG شفاف (ZIP)")
                                }
                                OutlinedButton(
                                    onClick = { showTsxDialog = true },
                                    modifier = Modifier.height(48.dp).testTag("view_remotion_tsx_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Code, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("کد Remotion")
                                }
                            }
                        }
                    }
                }
            }

            // Render Progress / Error / Ready Cards
            item {
                when (exportState) {
                    is ExportUiState.Idle -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = SurfaceStudioGray,
                            border = BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "برای دریافت فایل‌های نهایی آماده دانلود، یکی از گزینه‌های رندر بالا را انتخاب کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    is ExportUiState.Rendering -> {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = exportState.stepTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ElectricBlue
                                )
                                LinearProgressIndicator(
                                    progress = { exportState.overallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "${(exportState.overallProgress * 100).toInt()}% تکمیل شده",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    is ExportUiState.Error -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = ErrorRedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = exportState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRedInk,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    is ExportUiState.Ready -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = SuccessGreenBg,
                                modifier = Modifier.fillMaxWidth().testTag("export_verified_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreenInk)
                                    Text(
                                        text = "رندر و بررسی سلامت فایل‌ها با موفقیت انجام شد (${exportState.resolutionLabel} · ${exportState.fps}FPS).",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SuccessGreenInk
                                    )
                                }
                            }

                            exportState.artifacts.forEachIndexed { idx, artifact ->
                                ArtifactDownloadCard(
                                    artifact = artifact,
                                    index = idx,
                                    onSave = { onSaveToDownloads(artifact) },
                                    onShare = { onShareArtifact(artifact) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTsxDialog) {
        AlertDialog(
            onDismissRequest = { showTsxDialog = false },
            title = { Text("کد Composition اختصاصی Remotion") },
            text = {
                Box(
                    modifier = Modifier
                        .height(340.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = remotionTsxCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = CharcoalBlack
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTsxDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }
}

@Composable
private fun ArtifactDownloadCard(
    artifact: ExportArtifact,
    index: Int,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val sizeMb = artifact.sizeBytes / (1024f * 1024f)
    val sizeText = if (sizeMb >= 1f) "%.2f MB".format(sizeMb) else "${artifact.sizeBytes / 1024} KB"

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artifact.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = CharcoalBlack
                    )
                    Text(
                        text = artifact.codecInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (artifact.isAlphaChannel) Color(0xFFEDE9FE) else SurfaceStudioGray
                ) {
                    Text(
                        text = if (artifact.isAlphaChannel) "ALPHA · $sizeText" else sizeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (artifact.isAlphaChannel) DeepPurple else CharcoalBlack,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_downloads_$index"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (artifact.isAlphaChannel) DeepPurple else CharcoalBlack
                    )
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ذخیره در Downloads")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.height(48.dp).testTag("share_artifact_$index")
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری")
                }
            }
        }
    }
}
