const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, '..', 'studio_db.json');

const defaultDb = {
  projects: [
    {
      id: 1,
      title: "TikTok Viral Trends 2026",
      description: "Dynamic AI video sequence created with FlowMonkey",
      aspectRatio: "9:16",
      resolution: "1080p FHD",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  ],
  activeProjectId: 1,
  tracks: [
    { id: 1, projectId: 1, trackIndex: 0, type: "VIDEO", label: "Utama (Video Track 1)" },
    { id: 2, projectId: 1, trackIndex: 1, type: "VIDEO", label: "Overlay / B-Roll Track" },
    { id: 3, projectId: 1, trackIndex: 2, type: "AUDIO", label: "Musik & Efek Suara" },
    { id: 4, projectId: 1, trackIndex: 3, type: "TEXT", label: "Subtitel & Teks AI" }
  ],
  clips: [
    {
      id: 1,
      projectId: 1,
      trackId: 1,
      title: "Opening Cyberpunk Neon",
      uri: "https://example.com/assets/video1.mp4",
      startTimeMs: 0,
      endTimeMs: 5000,
      durationMs: 5000,
      filterName: "Cinematic Glow",
      speedMultiplier: 1.0,
      speedCurve: "Hero",
      hasKeyframe: true,
      keyframes: [
        { id: 1, timeSec: 0.0, posX: 0, posY: 0, scale: 1.0, rotation: 0, opacity: 1.0, ease: "EaseInOut" },
        { id: 2, timeSec: 2.5, posX: 40, posY: -15, scale: 1.2, rotation: 8, opacity: 1.0, ease: "Bezier" }
      ],
      transitionType: "Dissolve",
      volume: 1.0,
      isMuted: false,
      audioFadeInSec: 0.5,
      audioFadeOutSec: 0.5,
      audioPitch: 1.0,
      noiseReduction: true,
      vocalEnhance: false,
      proxyUri: "proxy_360p_1_1720000000000.mp4",
      proxyStatus: "READY"
    }
  ],
  transcodingJobs: [],
  settings: {
    isProxyModeEnabled: true,
    proxyResolution: "360p Proxy",
    autoTranscodeOnImport: true,
    isDarkTheme: true,
    geminiApiKey: "",
    highfieldApiKey: "",
    customEndpoint: "https://api.flowmonkey.ai/v1"
  },
  exports: []
};

function getDb() {
  try {
    if (!fs.existsSync(DB_PATH)) {
      saveDb(defaultDb);
      return defaultDb;
    }
    const data = fs.readFileSync(DB_PATH, 'utf8');
    return JSON.parse(data);
  } catch (err) {
    return defaultDb;
  }
}

function saveDb(data) {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(data, null, 2), 'utf8');
  } catch (err) {
    console.error("Error writing database:", err.message);
  }
}

module.exports = {
  getDb,
  saveDb
};
