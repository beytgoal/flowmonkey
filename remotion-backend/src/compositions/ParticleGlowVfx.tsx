import React, { useMemo } from 'react';
import { interpolate, useCurrentFrame, useVideoConfig } from 'remotion';
import { z } from 'zod';

export const particleGlowSchema = z.object({
  particleCount: z.number(),
  particleSpeed: z.number(),
  glowIntensity: z.number(),
  particleColor: z.string(),
  particleType: z.string()
});

interface Particle {
  id: number;
  initialX: number;
  initialY: number;
  size: number;
  speedMultiplier: number;
  wobbleFreq: number;
  wobbleAmp: number;
  alpha: number;
}

export const ParticleGlowVfx: React.FC<z.infer<typeof particleGlowSchema>> = ({
  particleCount,
  particleSpeed,
  glowIntensity,
  particleColor
}) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();

  // Generate deterministic particles
  const particles = useMemo(() => {
    const list: Particle[] = [];
    for (let i = 0; i < particleCount; i++) {
      list.push({
        id: i,
        initialX: (i * 37) % width,
        initialY: (i * 71) % height,
        size: 3 + ((i * 13) % 12),
        speedMultiplier: 0.5 + ((i * 7) % 10) / 10,
        wobbleFreq: 0.05 + ((i * 3) % 5) * 0.02,
        wobbleAmp: 10 + (i % 25),
        alpha: 0.4 + ((i * 11) % 6) / 10
      });
    }
    return list;
  }, [particleCount, width, height]);

  return (
    <div
      style={{
        flex: 1,
        width: '100%',
        height: '100%',
        backgroundColor: 'transparent',
        position: 'relative',
        overflow: 'hidden'
      }}
    >
      {/* Dynamic Floating Particles */}
      {particles.map((p) => {
        const totalDistance = height + 100;
        const currentY =
          (p.initialY - (frame * particleSpeed * p.speedMultiplier * 2.5)) % totalDistance;
        const normalizedY = currentY < -50 ? totalDistance + currentY : currentY;
        const currentX =
          p.initialX + Math.sin(frame * p.wobbleFreq) * p.wobbleAmp;

        const pulseScale = 1 + Math.sin(frame * 0.1 + p.id) * 0.2;

        return (
          <div
            key={p.id}
            style={{
              position: 'absolute',
              left: `${currentX}px`,
              top: `${normalizedY}px`,
              width: `${p.size * pulseScale}px`,
              height: `${p.size * pulseScale}px`,
              borderRadius: '50%',
              backgroundColor: particleColor,
              opacity: p.alpha,
              boxShadow: `0 0 ${p.size * 2 * glowIntensity}px ${particleColor}, 0 0 ${p.size * 4 * glowIntensity}px ${particleColor}AA`,
              transform: 'translate(-50%, -50%)',
              filter: `blur(${p.size > 8 ? '1px' : '0px'})`
            }}
          />
        );
      })}

      {/* Ambient Pulsing Corner Glow Vignette */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: `radial-gradient(circle at 50% 50%, transparent 60%, ${particleColor}22 100%)`,
          opacity: interpolate(Math.sin(frame * 0.05), [-1, 1], [0.3, 0.7]),
          pointerEvents: 'none'
        }}
      />
    </div>
  );
};
