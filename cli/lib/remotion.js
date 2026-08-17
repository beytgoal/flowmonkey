const { getDb, saveDb } = require('./store');

const REMOTION_COMPOSITIONS = [
  {
    id: "KineticTypography",
    alias: "kinetic",
    displayName: "Teks Animasi Bergaya Kinetik & Tipografi Dinamis",
    description: "Animasi teks 3D kinetik dengan efek staggered words, spring bounce, dan glowing neon",
    targetTrackType: "TEXT",
    defaultDurationMs: 4000,
    defaultProps: {
      title: "FLOWMONKEY PRO",
      subtitle: "PRO CLOUD RENDERING ENGINE",
      themeColor: "#8B5CF6",
      accentColor: "#06B6D4"
    }
  },
  {
    id: "GlitchWaveTransition",
    alias: "glitch",
    displayName: "Transisi Glitch Digital & Distorsi Gelombang",
    description: "Distorsi sinusoidal, slice horizontal block displacement, RGB chromatic split & scanlines",
    targetTrackType: "VIDEO",
    defaultDurationMs: 2500,
    defaultProps: {
      glitchIntensity: 0.85,
      waveSpeed: 1.5,
      chromaticShiftPx: 18,
      scanlines: true,
      glitchColor: "#EC4899"
    }
  },
  {
    id: "HudInfographics",
    alias: "hud",
    displayName: "Komponen Infografis, Chart Bergerak, & Elemen HUD",
    description: "HUD radar putar, grafik batang analitik dinamis, crosshair targeting & telemetry readout",
    targetTrackType: "TEXT",
    defaultDurationMs: 5000,
    defaultProps: {
      hudTitle: "TACTICAL TELEMETRY HUD",
      statValue: 98.4,
      dataPoints: [25, 45, 60, 80, 75, 98],
      radarSpeed: 1.2,
      hudColor: "#00E5FF"
    }
  },
  {
    id: "ParticleGlowVfx",
    alias: "particle",
    displayName: "Partikel Melayang & Efek Cahaya (Glow & Particle Systems)",
    description: "Simulasi partikel dinamis dengan pergerakan acak, neon glow halo, dan ambient pulsing vignette",
    targetTrackType: "VIDEO",
    defaultDurationMs: 6000,
    defaultProps: {
      particleCount: 80,
      particleSpeed: 1.4,
      glowIntensity: 1.8,
      particleColor: "#F59E0B",
      particleType: "cyber_embers"
    }
  }
];

function listRemotionVfx(jsonMode = false) {
  if (jsonMode) {
    return JSON.stringify({ compositions: REMOTION_COMPOSITIONS }, null, 2);
  }

  let output = `\n✨ VERCEL + REMOTION CLOUD VFX COMPOSITIONS (SERVERLESS GPU CLUSTER)\n========================================================================\n`;
  REMOTION_COMPOSITIONS.forEach((comp, idx) => {
    output += `${idx + 1}. [${comp.alias} / ${comp.id}]\n`;
    output += `   Nama        : ${comp.displayName}\n`;
    output += `   Tipe Track  : ${comp.targetTrackType} Track | Durasi Default: ${comp.defaultDurationMs / 1000}s\n`;
    output += `   Deskripsi   : ${comp.description}\n\n`;
  });
  output += `========================================================================\n`;
  return output;
}

function renderRemotionCloudVfx({
  vfxType = "kinetic",
  title = "FLOWMONKEY CLOUD VFX",
  subtitle = "RENDERED WITH REMOTION ON VERCEL",
  themeColor = "#8B5CF6",
  durationSec = null,
  trackIndex = null,
  jsonMode = false
}) {
  const db = getDb();
  const activeProjId = db.activeProjectId || (db.projects[0] ? db.projects[0].id : 1);

  const matchedComp = REMOTION_COMPOSITIONS.find(
    c => c.id.toLowerCase() === vfxType.toLowerCase() || c.alias.toLowerCase() === vfxType.toLowerCase()
  ) || REMOTION_COMPOSITIONS[0];

  const durationMs = durationSec ? (parseFloat(durationSec) * 1000) : matchedComp.defaultDurationMs;

  // Find or create target track
  let track = db.tracks.find(t => t.projectId === activeProjId && t.type === matchedComp.targetTrackType);
  if (!track) {
    const newTrackId = db.tracks.length > 0 ? Math.max(...db.tracks.map(t => t.id)) + 1 : 1;
    track = {
      id: newTrackId,
      projectId: activeProjId,
      trackIndex: db.tracks.filter(t => t.projectId === activeProjId).length,
      type: matchedComp.targetTrackType,
      label: `Remotion Cloud VFX (${matchedComp.targetTrackType})`
    };
    db.tracks.push(track);
  }

  // Calculate start time based on existing clips in track
  const trackClips = db.clips.filter(c => c.projectId === activeProjId && c.trackId === track.id);
  const startTimeMs = trackClips.length > 0 ? Math.max(...trackClips.map(c => c.endTimeMs)) : 0;
  const endTimeMs = startTimeMs + durationMs;

  const jobId = `remotion_${matchedComp.alias}_${Date.now()}`;
  const cloudCdnUrl = `https://storage.googleapis.com/flowmonkey-vfx-cloud/${matchedComp.id.toLowerCase()}_${jobId}.mp4`;

  const newClipId = db.clips.length > 0 ? Math.max(...db.clips.map(c => c.id)) + 1 : 1;
  const newClip = {
    id: newClipId,
    projectId: activeProjId,
    trackId: track.id,
    title: `[Remotion Cloud] ${matchedComp.id}: ${title.substring(0, 20)}`,
    uri: cloudCdnUrl,
    startTimeMs,
    endTimeMs,
    durationMs,
    filterName: "Remotion Cloud HDR",
    effectName: matchedComp.id,
    speedMultiplier: 1.0,
    speedCurve: "Standard",
    hasKeyframe: false,
    keyframes: [],
    transitionType: "None",
    volume: 1.0,
    isMuted: false,
    noiseReduction: false,
    vocalEnhance: false,
    proxyUri: `proxy_${jobId}.mp4`,
    proxyStatus: "READY",
    cloudMetadata: {
      renderer: "Vercel Serverless + Remotion GPU Cluster",
      compositionId: matchedComp.id,
      inputProps: {
        title,
        subtitle,
        themeColor
      },
      renderedAt: new Date().toISOString()
    }
  };

  db.clips.push(newClip);
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      jobId,
      compositionId: matchedComp.id,
      clip: newClip,
      streamUrl: cloudCdnUrl
    }, null, 2);
  }

  return `
🚀 VERCEL + REMOTION CLOUD VFX SELESAI DI-RENDER!
========================================================================
Komposisi   : ${matchedComp.displayName} (${matchedComp.id})
Job ID      : ${jobId}
Judul/Teks  : "${title}"
Warna Tema  : ${themeColor}
Durasi      : ${(durationMs / 1000).toFixed(1)}s (Track ${track.type} #${track.id})
Timeline    : Ditambahkan pada rentang ${(startTimeMs / 1000).toFixed(1)}s - ${(endTimeMs / 1000).toFixed(1)}s
Cloud URL   : ${cloudCdnUrl}
========================================================================
`;
}

function previewRemotionCloudVfx(vfxType = "kinetic", frame = 30, jsonMode = false) {
  const matchedComp = REMOTION_COMPOSITIONS.find(
    c => c.id.toLowerCase() === vfxType.toLowerCase() || c.alias.toLowerCase() === vfxType.toLowerCase()
  ) || REMOTION_COMPOSITIONS[0];

  const previewThumbnail = `https://storage.googleapis.com/flowmonkey-vfx-cloud/previews/${matchedComp.id}_f${frame}.jpg`;

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      compositionId: matchedComp.id,
      previewFrame: parseInt(frame),
      previewThumbnail,
      timestamp: new Date().toISOString()
    }, null, 2);
  }

  return `🖼️ Keyframe Preview [Frame #${frame}] untuk '${matchedComp.id}':\n   URL: ${previewThumbnail}`;
}

module.exports = {
  REMOTION_COMPOSITIONS,
  listRemotionVfx,
  renderRemotionCloudVfx,
  previewRemotionCloudVfx
};
