# FlowMonkey Remotion Cloud VFX Rendering Engine (Vercel Backend)

Cloud rendering microservice built on **Remotion** and **Vercel Serverless** to offload computationally heavy video effects, particle simulation, and dynamic motion graphics from mobile devices.

## 4 Supported Heavy Cloud VFX Compositions

1. **`KineticTypography`**
   - Staggered word animation, bounce physics, dynamic 3D skew, glowing neon typography overlays.
2. **`GlitchWaveTransition`**
   - Digital slice displacements, chromatic aberration RGB split, sinusoidal wave distortion, CRT scanlines.
3. **`HudInfographics`**
   - Futuristic cyberpunk HUD gauges, rotating radars, live telemetry bar charts, efficiency metrics.
4. **`ParticleGlowVfx`**
   - Floating particle system with customizable speed, size dispersion, gravity, and pulsing neon aura.

## Deploying to Vercel

```bash
cd remotion-backend
npm install
vercel deploy --prod
```

## API Usage

### Cloud Render Dispatch
```http
POST /api/render
Content-Type: application/json

{
  "compositionId": "KineticTypography",
  "inputProps": {
    "title": "NEON CYBER CITY",
    "subtitle": "4K MOTION GRAPHICS",
    "themeColor": "#8B5CF6",
    "accentColor": "#06B6D4"
  },
  "durationInFrames": 120,
  "fps": 30
}
```
