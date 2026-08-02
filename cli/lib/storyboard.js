const { getDb, saveDb } = require('./store');

const STORYBOARD_TEMPLATES = {
  "TikTok Viral": {
    name: "TikTok Viral",
    shots: [
      { id: 1, title: "Hook 3 Detik Pertama", durationSec: 3, prompt: "Fast motion dynamic hook opening with vibrant lighting", camera: "Zoom In" },
      { id: 2, title: "Aksi Utama / B-Roll", durationSec: 5, prompt: "Main subject demonstration in ultra crisp 60fps", camera: "Panning Right" },
      { id: 3, title: "Puncak Masalah / Emosi", durationSec: 4, prompt: "Close up emotional reaction with dramatic lighting", camera: "Static Medium" },
      { id: 4, title: "Call to Action & Outro", durationSec: 3, prompt: "End screen with glowing subscribe branding button", camera: "Zoom Out" }
    ]
  },
  "Product Showcase": {
    name: "Product Showcase",
    shots: [
      { id: 1, title: "Desain Bodi Futuristik", durationSec: 4, prompt: "360 degree slow motion rotation of luxury tech product", camera: "Orbit" },
      { id: 2, title: "Macro Detail Material", durationSec: 3, prompt: "Extreme macro shot highlighting premium texture", camera: "Macro Close Up" },
      { id: 3, title: "Fitur Unggulan Beraksi", durationSec: 5, prompt: "Product feature demonstration in realistic studio lighting", camera: "Tracking" },
      { id: 4, title: "Harga & Logo Branding", durationSec: 3, prompt: "Sleek product graphic overlay with price tag", camera: "Static" }
    ]
  },
  "Music Video": {
    name: "Music Video",
    shots: [
      { id: 1, title: "Intro Visual Ambience", durationSec: 5, prompt: "Wide landscape cinematic atmosphere with fog and neon tint", camera: "Wide Crane" },
      { id: 2, title: "Lip Sync Vocalist Shot", durationSec: 6, prompt: "Artist singing with anamorphic lens flare highlights", camera: "Handheld Motion" },
      { id: 3, title: "Montase Ritme Cepat", durationSec: 4, prompt: "Rhythmic fast cuts synced to bass drop pulses", camera: "Quick Cut" },
      { id: 4, title: "Climax Performance", durationSec: 5, prompt: "Energetic live performance with stage lasers and smoke", camera: "Dolly Zoom" }
    ]
  },
  "Short Film": {
    name: "Short Film",
    shots: [
      { id: 1, title: "Establishing Shot Suasana", durationSec: 6, prompt: "Mysterious rainy city street at twilight", camera: "Wide Static" },
      { id: 2, title: "Karakter Berjalan di Hujan", durationSec: 5, prompt: "Protagonist walking down alley with reflective neon puddles", camera: "Low Angle Track" },
      { id: 3, title: "Konfrontasi & Dialog Utama", durationSec: 7, prompt: "Two figures facing each other under single streetlamp", camera: "Over The Shoulder" },
      { id: 4, title: "Ending Menggantung", durationSec: 4, prompt: "Fade out slowly as silhouette disappears into shadows", camera: "Crane Up" }
    ]
  }
};

function listTemplates(jsonMode = false) {
  if (jsonMode) return JSON.stringify(STORYBOARD_TEMPLATES, null, 2);
  let out = `\n🎬 TEMPLATE STORYBOARD AI STUDIO\n`;
  out += `=================================================\n`;
  Object.keys(STORYBOARD_TEMPLATES).forEach(key => {
    const t = STORYBOARD_TEMPLATES[key];
    const totalDuration = t.shots.reduce((acc, s) => acc + s.durationSec, 0);
    out += `📌 Template: "${t.name}" (${t.shots.length} Shot, Total: ${totalDuration}s)\n`;
    t.shots.forEach(s => {
      out += `   - [${s.title}] (${s.durationSec}s): "${s.prompt}"\n`;
    });
    out += `\n`;
  });
  return out;
}

function compileStoryboardToTimeline(templateName, jsonMode = false) {
  const template = STORYBOARD_TEMPLATES[templateName] || STORYBOARD_TEMPLATES["TikTok Viral"];
  const db = getDb();
  const activeProjectId = db.activeProjectId;
  if (!activeProjectId) {
    if (jsonMode) return JSON.stringify({ error: "No active project" });
    return "❌ Error: Pilih atau buat proyek aktif terlebih dahulu.";
  }

  const mainTrack = db.tracks.find(t => t.projectId === activeProjectId && t.trackIndex === 0) || db.tracks[0];
  let currentStartMs = 0;
  const createdClips = [];
  const autoTranscode = db.settings.autoTranscodeOnImport;
  const proxyRes = db.settings.proxyResolution;

  template.shots.forEach(s => {
    const clipId = db.clips.length > 0 ? Math.max(...db.clips.map(c => c.id)) + 1 : 1;
    const durationMs = s.durationSec * 1000;
    const endMs = currentStartMs + durationMs;

    const newClip = {
      id: clipId,
      projectId: activeProjectId,
      trackId: mainTrack.id,
      title: `[SB] ${s.title}`,
      uri: `https://generated.flowmonkey.ai/sb/${clipId}.mp4`,
      startTimeMs: currentStartMs,
      endTimeMs: endMs,
      durationMs,
      filterName: "Cinematic Glow",
      speedMultiplier: 1.0,
      transitionType: "Dissolve",
      volume: 1.0,
      isMuted: false,
      audioFadeInSec: 0.5,
      audioFadeOutSec: 0.5,
      audioPitch: 1.0,
      noiseReduction: true,
      vocalEnhance: false,
      proxyUri: autoTranscode ? `proxy_sb_${clipId}.mp4` : "",
      proxyStatus: autoTranscode ? "READY" : "IDLE"
    };

    db.clips.push(newClip);
    createdClips.push(newClip);
    currentStartMs = endMs;

    if (autoTranscode) {
      db.transcodingJobs.push({
        id: "job_sb_" + clipId,
        clipId,
        mediaTitle: newClip.title,
        originalResolution: "1080p FHD",
        targetResolution: proxyRes,
        progressPercent: 100,
        statusMessage: "Proxy Low-Res Transcoded",
        isCompleted: true
      });
    }
  });

  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, template: template.name, clips: createdClips }, null, 2);

  let log = `\n🚀 STORYBOARD ENGINE COMPILER\n`;
  log += `=========================================\n`;
  log += `Template Digunakan : "${template.name}"\n`;
  log += `Jumlah Shot        : ${createdClips.length} klip\n`;
  log += `Total Durasi       : ${currentStartMs / 1000} detik\n`;
  log += `Status             : ✅ Semua adegan dikompilasi ke Timeline Track 1!\n`;
  return log;
}

module.exports = {
  STORYBOARD_TEMPLATES,
  listTemplates,
  compileStoryboardToTimeline
};
