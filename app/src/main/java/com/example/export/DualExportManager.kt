package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.motion.ExportFormat
import com.example.motion.MotionFrameRenderer
import com.example.motion.MotionProject
import com.example.motion.MotionScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream

data class ExportArtifact(
    val label: String,
    val file: File,
    val mimeType: String,
    val codecInfo: String,
    val sizeBytes: Long,
    val isAlphaChannel: Boolean
)

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Rendering(
        val stepTitle: String,
        val currentFrame: Int,
        val totalFrames: Int,
        val overallProgress: Float
    ) : ExportUiState
    data class Ready(
        val artifacts: List<ExportArtifact>,
        val resolutionLabel: String,
        val fps: Int
    ) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

class DualExportManager(
    private val context: Context,
    private val renderer: MotionFrameRenderer
) {
    private val mp4Encoder = WhiteMp4Encoder()
    private val pngZipExporter = PngSequenceZipExporter()

    suspend fun exportProject(
        project: MotionProject,
        singleScene: MotionScene?,
        formatOverride: ExportFormat? = null,
        onProgress: (ExportUiState.Rendering) -> Unit
    ): Result<ExportUiState.Ready> = withContext(Dispatchers.Default) {
        try {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val targetProject = if (singleScene != null) {
                project.copy(scenes = listOf(singleScene))
            } else {
                project
            }
            val totalFrames = targetProject.totalFrames
            val width = targetProject.settings.resolution.width
            val height = targetProject.settings.resolution.height
            val fps = targetProject.settings.fps
            val activeFormat = formatOverride ?: targetProject.settings.exportFormat
            val stamp = System.currentTimeMillis()
            val prefix = if (singleScene != null) "scene_${singleScene.paragraphIndex + 1}" else "motion_full"

            val artifacts = mutableListOf<ExportArtifact>()

            val needMp4 = activeFormat == ExportFormat.DUAL_MOV_AND_MP4 || activeFormat == ExportFormat.WHITE_MP4_ONLY
            val needMov = activeFormat == ExportFormat.DUAL_MOV_AND_MP4 || activeFormat == ExportFormat.TRANSPARENT_MOV_ONLY
            val needZip = activeFormat == ExportFormat.PNG_SEQUENCE_ZIP

            val totalSteps = listOf(needMp4, needMov, needZip).count { it }.coerceAtLeast(1)
            var completedSteps = 0

            // Step A: Render White Background (#FFFFFF) H.264 MP4
            if (needMp4) {
                val mp4File = File(exportDir, "${prefix}_white_${stamp}.mp4")
                onProgress(
                    ExportUiState.Rendering(
                        stepTitle = "رندر ویدیوی MP4 با پس‌زمینه سفید (#FFFFFF)...",
                        currentFrame = 0,
                        totalFrames = totalFrames,
                        overallProgress = completedSteps / totalSteps.toFloat()
                    )
                )
                mp4Encoder.encode(
                    outputFile = mp4File,
                    width = width,
                    height = height,
                    fps = fps,
                    totalFrames = totalFrames,
                    renderFrameToBitmap = { frameIndex, bitmap ->
                        val canvas = Canvas(bitmap)
                        renderer.renderProjectFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            project = targetProject,
                            globalFrame = frameIndex,
                            transparentBackground = false
                        )
                    },
                    onProgress = { cur, tot ->
                        val stepFrac = cur / tot.toFloat()
                        onProgress(
                            ExportUiState.Rendering(
                                stepTitle = "در حال کدگذاری فریم‌های H.264 MP4 ($cur / $tot)",
                                currentFrame = cur,
                                totalFrames = tot,
                                overallProgress = (completedSteps + stepFrac) / totalSteps.toFloat()
                            )
                        )
                    }
                )
                verifyNonEmptyFile(mp4File, "MP4")
                artifacts += ExportArtifact(
                    label = "White Background MP4",
                    file = mp4File,
                    mimeType = "video/mp4",
                    codecInfo = "H.264 / AVC · #FFFFFF · ${width}×${height} · ${fps}FPS",
                    sizeBytes = mp4File.length(),
                    isAlphaChannel = false
                )
                completedSteps++
            }

            // Step B: Render Transparent Alpha MOV (32-bit RGBA PNG-in-MOV)
            if (needMov) {
                val movFile = File(exportDir, "${prefix}_alpha_${stamp}.mov")
                onProgress(
                    ExportUiState.Rendering(
                        stepTitle = "آماده‌سازی کانال آلفای واقعی (32-bit RGBA MOV)...",
                        currentFrame = 0,
                        totalFrames = totalFrames,
                        overallProgress = completedSteps / totalSteps.toFloat()
                    )
                )
                val movWriter = QuickTimeAlphaMovWriter(
                    outputFile = movFile,
                    width = width,
                    height = height,
                    fps = fps
                )
                val alphaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(alphaBitmap)
                    for (frame in 0 until totalFrames) {
                        yield()
                        renderer.renderProjectFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            project = targetProject,
                            globalFrame = frame,
                            transparentBackground = true
                        )
                        movWriter.writeFrame(alphaBitmap)
                        val stepFrac = (frame + 1) / totalFrames.toFloat()
                        onProgress(
                            ExportUiState.Rendering(
                                stepTitle = "ساخت فریم‌های شفاف Alpha MOV (${frame + 1} / $totalFrames)",
                                currentFrame = frame + 1,
                                totalFrames = totalFrames,
                                overallProgress = (completedSteps + stepFrac) / totalSteps.toFloat()
                            )
                        )
                    }
                    movWriter.finish()
                } finally {
                    alphaBitmap.recycle()
                }
                verifyNonEmptyFile(movFile, "Transparent MOV")
                artifacts += ExportArtifact(
                    label = "Transparent Motion MOV",
                    file = movFile,
                    mimeType = "video/quicktime",
                    codecInfo = "QuickTime 32-bit RGBA Alpha · ${width}×${height} · ${fps}FPS",
                    sizeBytes = movFile.length(),
                    isAlphaChannel = true
                )
                completedSteps++
            }

            // Step C: Optional PNG Sequence Alpha ZIP + Remotion TSX Blueprint
            if (needZip) {
                val zipFile = File(exportDir, "${prefix}_png_alpha_${stamp}.zip")
                pngZipExporter.export(
                    outputZip = zipFile,
                    project = targetProject,
                    width = width,
                    height = height,
                    totalFrames = totalFrames,
                    renderTransparentFrame = { frameIndex, bitmap ->
                        val canvas = Canvas(bitmap)
                        renderer.renderProjectFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            project = targetProject,
                            globalFrame = frameIndex,
                            transparentBackground = true
                        )
                    },
                    onProgress = { cur, tot ->
                        val stepFrac = cur / tot.toFloat()
                        onProgress(
                            ExportUiState.Rendering(
                                stepTitle = "بسته‌بندی سکانس PNG شفاف + کد Remotion ($cur / $tot)",
                                currentFrame = cur,
                                totalFrames = tot,
                                overallProgress = (completedSteps + stepFrac) / totalSteps.toFloat()
                            )
                        )
                    }
                )
                verifyNonEmptyFile(zipFile, "PNG Sequence ZIP")
                artifacts += ExportArtifact(
                    label = "PNG Sequence Alpha + Remotion TSX",
                    file = zipFile,
                    mimeType = "application/zip",
                    codecInfo = "ARGB_8888 PNG + Composition.tsx · ${width}×${height}",
                    sizeBytes = zipFile.length(),
                    isAlphaChannel = true
                )
            }

            Result.success(
                ExportUiState.Ready(
                    artifacts = artifacts,
                    resolutionLabel = "${width}×${height}",
                    fps = fps
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveArtifactToDownloads(artifact: ExportArtifact): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, artifact.file.name)
                    put(MediaStore.Downloads.MIME_TYPE, artifact.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MotionStudio")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("خطا در ایجاد فایل در پوشه Downloads")
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(artifact.file).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("خطا در نوشتن خروجی در Downloads")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("در پوشه Downloads/MotionStudio/${artifact.file.name} ذخیره شد.")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MotionStudio"
                ).apply { mkdirs() }
                val dest = File(downloadsDir, artifact.file.name)
                artifact.file.copyTo(dest, overwrite = true)
                Result.success("در ${dest.absolutePath} ذخیره شد.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createShareIntent(artifact: ExportArtifact): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            artifact.file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = artifact.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun verifyNonEmptyFile(file: File, label: String) {
        if (!file.exists() || file.length() < 128L) {
            throw IllegalStateException("فایل خروجی $label ناقص یا خالی تولید شد؛ لطفاً رزولوشن یا مدت را بررسی و دوباره رندر کنید.")
        }
    }
}
