import React from 'react';
import { interpolate, useCurrentFrame } from 'remotion';
import { z } from 'zod';

export const glitchWaveSchema = z.object({
  glitchIntensity: z.number(),
  waveSpeed: z.number(),
  chromaticShiftPx: z.number(),
  scanlines: z.boolean(),
  glitchColor: z.string()
});

export const GlitchWaveTransition: React.FC<z.infer<typeof glitchWaveSchema>> = ({
  glitchIntensity,
  waveSpeed,
  chromaticShiftPx,
  scanlines,
  glitchColor
}) => {
  const frame = useCurrentFrame();

  // Wave sine math displacement
  const waveOffset = Math.sin((frame * waveSpeed * 0.2)) * (25 * glitchIntensity);
  const glitchActive = Math.sin(frame * 0.8) > 0.3;
  const rgbShift = glitchActive ? chromaticShiftPx * glitchIntensity : 2;

  // Digital horizontal slice blocks
  const sliceCount = 8;
  const slices = Array.from({ length: sliceCount }).map((_, i) => {
    const shift = glitchActive ? ((Math.sin(frame + i * 2.5) * 40 * glitchIntensity)) : 0;
    return { id: i, shift };
  });

  const progress = interpolate(frame, [0, 60], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp'
  });

  return (
    <div
      style={{
        flex: 1,
        width: '100%',
        height: '100%',
        backgroundColor: 'transparent',
        overflow: 'hidden',
        position: 'relative'
      }}
    >
      {/* Glitch Slices Simulation */}
      {slices.map((slice, i) => (
        <div
          key={slice.id}
          style={{
            position: 'absolute',
            top: `${(100 / sliceCount) * i}%`,
            height: `${100 / sliceCount}%`,
            width: '100%',
            transform: `translateX(${slice.shift}px)`,
            backgroundColor: glitchActive && i % 2 === 0 ? `${glitchColor}22` : 'transparent',
            borderBottom: glitchActive ? `1px solid ${glitchColor}88` : 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        />
      ))}

      {/* Chromatic Aberration RGB Cyan/Red Edge Split */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          mixBlendMode: 'screen',
          transform: `translate(${rgbShift}px, ${waveOffset * 0.3}px)`,
          opacity: glitchIntensity * 0.7,
          background: `radial-gradient(circle, transparent 40%, #00FFFF33 100%)`
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 0,
          mixBlendMode: 'screen',
          transform: `translate(-${rgbShift}px, -${waveOffset * 0.3}px)`,
          opacity: glitchIntensity * 0.7,
          background: `radial-gradient(circle, transparent 40%, #FF005533 100%)`
        }}
      />

      {/* Cyber Wave Distortion Central Badge */}
      <div
        style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: `translate(-50%, -50%) scale(${1 + Math.sin(frame * 0.3) * 0.08}) skewX(${waveOffset * 0.4}deg)`,
          padding: '16px 36px',
          border: `2px solid ${glitchColor}`,
          boxShadow: `0 0 30px ${glitchColor}`,
          backgroundColor: 'rgba(10, 10, 18, 0.75)',
          borderRadius: '8px'
        }}
      >
        <span
          style={{
            color: '#FFFFFF',
            fontSize: '24px',
            fontWeight: 800,
            letterSpacing: '6px',
            fontFamily: 'monospace'
          }}
        >
          GLITCH DISTORTION // {(progress * 100).toFixed(0)}%
        </span>
      </div>

      {/* Scanline CRT Grid Filter */}
      {scanlines && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            backgroundImage:
              'repeating-linear-gradient(0deg, rgba(0, 0, 0, 0.35), rgba(0, 0, 0, 0.35) 2px, transparent 2px, transparent 4px)',
            pointerEvents: 'none'
          }}
        />
      )}
    </div>
  );
};
