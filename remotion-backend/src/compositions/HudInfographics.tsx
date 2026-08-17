import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { z } from 'zod';

export const hudInfographicsSchema = z.object({
  hudTitle: z.string(),
  statValue: z.number(),
  dataPoints: z.array(z.number()),
  radarSpeed: z.number(),
  hudColor: z.string()
});

export const HudInfographics: React.FC<z.infer<typeof hudInfographicsSchema>> = ({
  hudTitle,
  statValue,
  dataPoints,
  radarSpeed,
  hudColor
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const rotation = (frame * radarSpeed * 3) % 360;
  const barAnimation = spring({
    frame,
    fps,
    config: { damping: 14, stiffness: 100 }
  });

  const liveStat = interpolate(frame, [0, 60], [0, statValue], {
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
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '60px 80px',
        boxSizing: 'border-box',
        fontFamily: 'monospace',
        color: hudColor
      }}
    >
      {/* 1. Left Radar & Reticle HUD Ring */}
      <div
        style={{
          position: 'relative',
          width: '260px',
          height: '260px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        {/* Outer Ring */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            border: `2px dashed ${hudColor}88`,
            borderRadius: '50%',
            transform: `rotate(${rotation}deg)`
          }}
        />
        {/* Inner Ring */}
        <div
          style={{
            position: 'absolute',
            inset: '30px',
            border: `1px solid ${hudColor}`,
            borderRadius: '50%',
            transform: `rotate(-${rotation * 1.5}deg)`
          }}
        />
        {/* Radar Crosshair */}
        <div
          style={{
            position: 'absolute',
            width: '100%',
            height: '1px',
            backgroundColor: `${hudColor}66`
          }}
        />
        <div
          style={{
            position: 'absolute',
            height: '100%',
            width: '1px',
            backgroundColor: `${hudColor}66`
          }}
        />
        {/* Center Target Indicator */}
        <div
          style={{
            width: '12px',
            height: '12px',
            borderRadius: '50%',
            backgroundColor: hudColor,
            boxShadow: `0 0 16px ${hudColor}`
          }}
        />
        <div
          style={{
            position: 'absolute',
            bottom: '-28px',
            fontSize: '12px',
            letterSpacing: '2px',
            fontWeight: 'bold'
          }}
        >
          ACQ://POS_{rotation.toFixed(0)}°
        </div>
      </div>

      {/* 2. Center Live Bar Chart */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          backgroundColor: 'rgba(10, 15, 25, 0.7)',
          padding: '24px 36px',
          borderRadius: '12px',
          border: `1px solid ${hudColor}44`,
          boxShadow: `0 0 24px rgba(0, 229, 255, 0.15)`
        }}
      >
        <span style={{ fontSize: '18px', fontWeight: 'bold', letterSpacing: '4px', marginBottom: '18px' }}>
          {hudTitle}
        </span>

        {/* Dynamic Animated Chart Bars */}
        <div
          style={{
            display: 'flex',
            alignItems: 'flex-end',
            gap: '16px',
            height: '140px',
            width: '320px',
            borderBottom: `2px solid ${hudColor}`,
            paddingBottom: '4px'
          }}
        >
          {dataPoints.map((val, idx) => {
            const barHeight = (val * barAnimation) * 1.2;
            return (
              <div
                key={idx}
                style={{
                  flex: 1,
                  height: `${barHeight}px`,
                  backgroundColor: hudColor,
                  opacity: 0.85,
                  borderRadius: '3px 3px 0 0',
                  boxShadow: `0 0 10px ${hudColor}`,
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'flex-start'
                }}
              >
                <span style={{ fontSize: '10px', color: '#000', fontWeight: 'bold', marginTop: '2px' }}>
                  {val}
                </span>
              </div>
            );
          })}
        </div>

        {/* Metrics Footer */}
        <div
          style={{
            marginTop: '16px',
            display: 'flex',
            justifyContent: 'space-between',
            width: '100%',
            fontSize: '13px'
          }}
        >
          <span>EFFICIENCY INDEX</span>
          <span style={{ fontWeight: 'bold' }}>{liveStat.toFixed(1)}%</span>
        </div>
      </div>

      {/* 3. Right Status Readout */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          fontSize: '13px',
          letterSpacing: '1px'
        }}
      >
        <div style={{ padding: '8px 14px', borderLeft: `3px solid ${hudColor}`, background: 'rgba(0,0,0,0.4)' }}>
          <div>SYS_FPS: {fps} // GPU_CORE</div>
          <div style={{ color: '#A0AEC0' }}>BUFFER_STREAM: 0-COPY ACTIVE</div>
        </div>
        <div style={{ padding: '8px 14px', borderLeft: `3px solid ${hudColor}`, background: 'rgba(0,0,0,0.4)' }}>
          <div>LATENCY: 1.2ms // VERCEL CLOUD</div>
          <div style={{ color: '#A0AEC0' }}>STATUS: OPTIMAL ONLINE</div>
        </div>
      </div>
    </div>
  );
};
