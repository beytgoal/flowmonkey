import React from 'react';
import { Composition } from 'remotion';
import { KineticTypography, kineticTypographySchema } from './compositions/KineticTypography';
import { GlitchWaveTransition, glitchWaveSchema } from './compositions/GlitchWaveTransition';
import { HudInfographics, hudInfographicsSchema } from './compositions/HudInfographics';
import { ParticleGlowVfx, particleGlowSchema } from './compositions/ParticleGlowVfx';

export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* 1. Kinetic Typography & Dynamic Kinetic Animated Text */}
      <Composition
        id="KineticTypography"
        component={KineticTypography}
        durationInFrames={120}
        fps={30}
        width={1920}
        height={1080}
        schema={kineticTypographySchema}
        defaultProps={{
          title: "FLOWMONKEY STUDIO",
          subtitle: "PRO CLOUD RENDERING ENGINE",
          themeColor: "#8B5CF6",
          accentColor: "#06B6D4",
          animationStyle: "stagger_bounce"
        }}
      />

      {/* 2. Digital Glitch Transition & Wave Distortion */}
      <Composition
        id="GlitchWaveTransition"
        component={GlitchWaveTransition}
        durationInFrames={60}
        fps={30}
        width={1920}
        height={1080}
        schema={glitchWaveSchema}
        defaultProps={{
          glitchIntensity: 0.85,
          waveSpeed: 1.5,
          chromaticShiftPx: 18,
          scanlines: true,
          glitchColor: "#EC4899"
        }}
      />

      {/* 3. Infographic Components, Moving Analytics Charts & Cyberpunk HUD */}
      <Composition
        id="HudInfographics"
        component={HudInfographics}
        durationInFrames={150}
        fps={30}
        width={1920}
        height={1080}
        schema={hudInfographicsSchema}
        defaultProps={{
          hudTitle: "TACTICAL TELEMETRY HUD",
          statValue: 98.4,
          dataPoints: [25, 45, 60, 80, 75, 98],
          radarSpeed: 1.2,
          hudColor: "#00E5FF"
        }}
      />

      {/* 4. Floating Particles & Neon Glow Systems */}
      <Composition
        id="ParticleGlowVfx"
        component={ParticleGlowVfx}
        durationInFrames={180}
        fps={30}
        width={1920}
        height={1080}
        schema={particleGlowSchema}
        defaultProps={{
          particleCount: 80,
          particleSpeed: 1.4,
          glowIntensity: 1.8,
          particleColor: "#F59E0B",
          particleType: "cyber_embers"
        }}
      />
    </>
  );
};
