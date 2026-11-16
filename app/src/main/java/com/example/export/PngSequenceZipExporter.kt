package com.example.export

import android.graphics.Bitmap
import com.example.motion.MotionProject
import kotlinx.coroutines.yield
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PngSequenceZipExporter {

    suspend fun export(
        outputZip: File,
        project: MotionProject,
        width: Int,
        height: Int,
        totalFrames: Int,
        renderTransparentFrame: (frameIndex: Int, bitmap: Bitmap) -> Unit,
        onProgress: suspend (currentFrame: Int, totalFrames: Int) -> Unit
    ) {
        outputZip.parentFile?.mkdirs()
        if (outputZip.exists()) outputZip.delete()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
                // 1. Write Remotion TSX composition blueprint
                val tsxEntry = ZipEntry("RemotionComposition.tsx")
                zos.putNextEntry(tsxEntry)
                zos.write(RemotionCodeGenerator.generateTsxForProject(project).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Write scenario manifest JSON
                val manifestEntry = ZipEntry("scenario_manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(buildManifestJson(project, width, height).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. Write transparent ARGB_8888 PNG sequence
                for (frame in 0 until totalFrames) {
                    yield()
                    renderTransparentFrame(frame, bitmap)
                    val entryName = "frames/frame_%04d.png".format(frame)
                    zos.putNextEntry(ZipEntry(entryName))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                    onProgress(frame + 1, totalFrames)
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun buildManifestJson(project: MotionProject, width: Int, height: Int): String {
        val root = JSONObject()
        root.put("width", width)
        root.put("height", height)
        root.put("fps", project.settings.fps)
        root.put("totalFrames", project.totalFrames)
        val scenesArr = JSONArray()
        for (scene in project.scenes) {
            val s = JSONObject()
            s.put("index", scene.paragraphIndex)
            s.put("title", scene.title)
            s.put("archetype", scene.archetype.name)
            s.put("durationSeconds", scene.durationSeconds)
            s.put("visualConcept", scene.scenario.visualConcept)
            s.put("visualMetaphor", scene.scenario.visualMetaphor)
            scenesArr.put(s)
        }
        root.put("scenes", scenesArr)
        return root.toString(2)
    }
}
