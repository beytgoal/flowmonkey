const { getDb, saveDb } = require('./store');

const STYLES = ["Cinematic", "Anime", "Cyberpunk", "3D Render", "Hyper-Realistic", "Photorealistic", "Retro 80s"];
const MODELS = ["Veo 2", "Veo Fast", "Highfield Pro"];
const RATIOS = ["16:9", "9:16", "1:1", "4:3", "21:9"];

function generateAiVideo(options) {
  const {
    prompt = "A aesthetic cinematic drone clip over neon cyberpunk metropolis",
    style = "Cinematic",
    model = "Veo 2",
    ratio = "9:16",
    durationSec = 5,
    fps = 30,
    jsonMode = false
  } = options;

  const db = getDb();
  const activeProjectId = db.activeProjectId;
  if (!activeProjectId) {
    if (jsonMode) return JSON.stringify({ error: "No active project" });
    return "❌ Error: Tidak ada proyek aktif. Buat atau pilih proyek dulu.";
  }

  const mainTrack = db.tracks.find(t => t.projectId === activeProjectId && t.trackIndex === 0) || db.tracks[0];
  const durationMs = durationSec * 1000;

  // Calculate timeline start time based on existing clips on this track
  const existingClips = db.clips.filter(c => c.projectId === activeProjectId && c.trackId === mainTrack.id);
  const startTimeMs = existingClips.reduce((max, c) => Math.max(max, c.endTimeMs), 0);
  const endTimeMs = startTimeMs + durationMs;

  const newClipId = db.clips.length > 0 ? Math.max(...db.clips.map(c => c.id)) + 1 : 1;
  const autoTranscode = db.settings.autoTranscodeOnImport;
  const targetProxyRes = db.settings.proxyResolution;

  const newClip = {
    id: newClipId,
    projectId: activeProjectId,
    trackId: mainTrack.id,
    title: `[AI ${model}] ${prompt.substring(0, 25)}...`,
    uri: `https://generated.flowmonkey.ai/v/${Date.now()}.mp4`,
    startTimeMs,
    endTimeMs,
    durationMs,
    filterName: style === "Cyberpunk" ? "Cyberpunk Neon" : style === "Cinematic" ? "Cinematic Glow" : "None",
    speedMultiplier: 1.0,
    transitionType: "Dissolve",
    volume: 1.0,
    isMuted: false,
    audioFadeInSec: 0.5,
    audioFadeOutSec: 0.5,
    audioPitch: 1.0,
    noiseReduction: true,
    vocalEnhance: false,
    proxyUri: autoTranscode ? `proxy_${targetProxyRes.take ? targetProxyRes.take(4).toLowerCase() : '360p'}_${newClipId}.mp4` : "",
    proxyStatus: autoTranscode ? "READY" : "IDLE"
  };

  db.clips.push(newClip);

  if (autoTranscode) {
    const job = {
      id: "job_" + Date.now(),
      clipId: newClipId,
      mediaTitle: newClip.title,
      originalResolution: "1080p FHD",
      targetResolution: targetProxyRes,
      progressPercent: 100,
      statusMessage: `Proxy Low-Res Transcoded (${targetProxyRes})`,
      isCompleted: true
    };
    db.transcodingJobs.push(job);
  }

  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      prompt,
      model,
      style,
      ratio,
      durationSec,
      clip: newClip
    }, null, 2);
  }

  let log = `\n🤖 VEO AI VIDEO GENERATOR ENGINE\n`;
  log += `=========================================\n`;
  log += `Prompt    : "${prompt}"\n`;
  log += `Model     : ${model}\n`;
  log += `Gaya      : ${style}\n`;
  log += `Rasio     : ${ratio} | Durasi: ${durationSec}s @ ${fps} FPS\n`;
  log += `Status    : ✅ Video berhasil diproses & ditambahkan ke Timeline Track 1!\n`;
  log += `Clip ID   : ${newClipId} (Durasi: ${startTimeMs/1000}s - ${endTimeMs/1000}s)\n`;
  if (autoTranscode) {
    log += `Proxy     : ⚡ Low-Res Proxy (${targetProxyRes}) dibuat otomatis di latar belakang!\n`;
  }
  return log;
}

module.exports = {
  generateAiVideo,
  STYLES,
  MODELS,
  RATIOS
};
