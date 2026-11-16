package com.example.export

import com.example.motion.MotionProject
import com.example.motion.MotionScene

object RemotionCodeGenerator {

    fun generateTsxForProject(project: MotionProject): String {
        val scenesCode = project.scenes.joinToString("\n\n") { scene ->
            generateSceneComponent(scene)
        }
        val sequenceItems = project.scenes.mapIndexed { idx, scene ->
            val from = project.scenes.take(idx).sumOf { it.totalFrames }
            """      <Series.Sequence durationInFrames={${scene.totalFrames}}>
        <Scene${idx + 1} transparentBackground={transparentBackground} />
      </Series.Sequence>"""
        }.joinToString("\n")

        return """import React from 'react';
import { AbsoluteFill, Series, useCurrentFrame, useVideoConfig, spring, interpolate } from 'remotion';

export interface MotionStudioProps {
  transparentBackground: boolean;
}

export const MotionStudioComposition: React.FC<MotionStudioProps> = ({ transparentBackground }) => {
  return (
    <AbsoluteFill style={{ backgroundColor: transparentBackground ? 'transparent' : '#FFFFFF' }}>
      <Series>
$sequenceItems
      </Series>
    </AbsoluteFill>
  );
};

$scenesCode
""".trimIndent()
    }

    private fun generateSceneComponent(scene: MotionScene): String {
        val idx = scene.paragraphIndex + 1
        return """const Scene$idx: React.FC<{ transparentBackground: boolean }> = ({ transparentBackground }) => {
  const frame = useCurrentFrame();
  const { fps, durationInFrames } = useVideoConfig();
  const scale = interpolate(frame, [0, durationInFrames], [${scene.cameraRig.zoomStart}f, ${scene.cameraRig.zoomEnd}f]);
  const enter = spring({ frame, fps, config: { damping: 14, stiffness: 135 } });
  // Archetype: ${scene.archetype.name} | Palette: ${scene.palette.name}
  // Metaphor: ${scene.scenario.visualMetaphor}
  return (
    <AbsoluteFill style={{
      backgroundColor: transparentBackground ? 'transparent' : '#FFFFFF',
      transform: `scale(${'$'}{scale})`,
      opacity: enter
    }}>
      {/* Procedural Layer Composition (${scene.elements.size} layers) */}
    </AbsoluteFill>
  );
};"""
    }
}
