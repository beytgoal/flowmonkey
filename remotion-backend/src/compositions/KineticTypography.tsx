import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { z } from 'zod';

export const kineticTypographySchema = z.object({
  title: z.string(),
  subtitle: z.string(),
  themeColor: z.string(),
  accentColor: z.string(),
  animationStyle: z.string()
});

export const KineticTypography: React.FC<z.infer<typeof kineticTypographySchema>> = ({
  title,
  subtitle,
  themeColor,
  accentColor
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const words = title.split(' ');

  const titleSpring = spring({
    frame,
    fps,
    config: { damping: 12, mass: 0.6, stiffness: 120 }
  });

  const subtitleOpacity = interpolate(frame, [25, 45], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp'
  });

  const subtitleTranslateY = interpolate(frame, [25, 45], [40, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp'
  });

  return (
    <div
      style={{
        flex: 1,
        backgroundColor: 'transparent',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
        width: '100%',
        height: '100%'
      }}
    >
      {/* Staggered Animated Words */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'center',
          gap: '24px',
          maxWidth: '85%'
        }}
      >
        {words.map((word, i) => {
          const wordDelay = i * 4;
          const wordSpring = spring({
            frame: frame - wordDelay,
            fps,
            config: { damping: 10, stiffness: 140 }
          });

          const scale = interpolate(wordSpring, [0, 1], [0.3, 1]);
          const opacity = interpolate(wordSpring, [0, 1], [0, 1]);
          const skewX = interpolate(wordSpring, [0, 1], [25, 0]);

          return (
            <span
              key={i}
              style={{
                fontSize: '84px',
                fontWeight: 900,
                color: i % 2 === 0 ? '#FFFFFF' : accentColor,
                textTransform: 'uppercase',
                letterSpacing: '4px',
                transform: `scale(${scale}) skewX(${skewX}deg)`,
                opacity,
                textShadow: `0 0 35px ${themeColor}AA, 0 10px 30px rgba(0,0,0,0.8)`
              }}
            >
              {word}
            </span>
          );
        })}
      </div>

      {/* Subtitle with dynamic underline accent */}
      <div
        style={{
          marginTop: '32px',
          opacity: subtitleOpacity,
          transform: `translateY(${subtitleTranslateY}px)`,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center'
        }}
      >
        <span
          style={{
            fontSize: '28px',
            fontWeight: 700,
            color: '#E2E8F0',
            letterSpacing: '8px',
            textTransform: 'uppercase'
          }}
        >
          {subtitle}
        </span>
        <div
          style={{
            marginTop: '12px',
            height: '4px',
            width: `${titleSpring * 240}px`,
            backgroundColor: themeColor,
            borderRadius: '2px',
            boxShadow: `0 0 16px ${themeColor}`
          }}
        />
      </div>
    </div>
  );
};
