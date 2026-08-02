const { getDb, saveDb } = require('./store');

function toggleProxy(enabled, jsonMode = false) {
  const db = getDb();
  if (enabled === undefined) {
    db.settings.isProxyModeEnabled = !db.settings.isProxyModeEnabled;
  } else {
    db.settings.isProxyModeEnabled = Boolean(enabled);
  }
  saveDb(db);

  const statusStr = db.settings.isProxyModeEnabled ? `⚡ PROXY LOW-RES ENABLED (${db.settings.proxyResolution})` : "🎬 ORIGINAL 1080p FULL-RES ENABLED";
  if (jsonMode) return JSON.stringify({ success: true, isProxyModeEnabled: db.settings.isProxyModeEnabled, proxyResolution: db.settings.proxyResolution }, null, 2);
  return `\n🔄 Mode Pratinjau Timeline Diubah: ${statusStr}\n`;
}

function setProxyResolution(resolution, jsonMode = false) {
  const db = getDb();
  db.settings.proxyResolution = resolution;
  if (resolution.includes("1080p") || resolution.includes("Original")) {
    db.settings.isProxyModeEnabled = false;
  } else {
    db.settings.isProxyModeEnabled = true;
  }
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, proxyResolution: db.settings.proxyResolution, isProxyModeEnabled: db.settings.isProxyModeEnabled }, null, 2);
  return `\n⚡ Target Resolusi Proxy Diperbarui Ke: "${resolution}". Mode Proxy: ${db.settings.isProxyModeEnabled ? "AKTIF" : "NONAKTIF"}\n`;
}

function toggleAutoTranscode(enabled, jsonMode = false) {
  const db = getDb();
  if (enabled === undefined) {
    db.settings.autoTranscodeOnImport = !db.settings.autoTranscodeOnImport;
  } else {
    db.settings.autoTranscodeOnImport = Boolean(enabled);
  }
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, autoTranscodeOnImport: db.settings.autoTranscodeOnImport }, null, 2);
  return `\n🔄 Auto-Transcode Impor Aset: ${db.settings.autoTranscodeOnImport ? "AKTIF (Otomatis konversi video baru di latar belakang)" : "NONAKTIF"}\n`;
}

function transcodeClip(clipId, jsonMode = false) {
  const db = getDb();
  const cId = parseInt(clipId);
  const clip = db.clips.find(c => c.id === cId);
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  const targetRes = db.settings.proxyResolution;
  clip.proxyStatus = "READY";
  clip.proxyUri = `proxy_${targetRes.take ? targetRes.take(4).toLowerCase() : '360p'}_${clip.id}.mp4`;

  const job = {
    id: "job_manual_" + Date.now(),
    clipId: clip.id,
    mediaTitle: clip.title,
    originalResolution: "1080p FHD",
    targetResolution: targetRes,
    progressPercent: 100,
    statusMessage: `Proxy Low-Res Transcoded (${targetRes})`,
    isCompleted: true
  };

  db.transcodingJobs.push(job);
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clip, job }, null, 2);
  return `\n⚡ Klip ID ${clipId} ("${clip.title}") berhasil ditranscode ke ${targetRes}! Status: READY.\n`;
}

function transcodeAll(jsonMode = false) {
  const db = getDb();
  const activeProjectId = db.activeProjectId;
  const projectClips = db.clips.filter(c => c.projectId === activeProjectId);

  const targetRes = db.settings.proxyResolution;
  let count = 0;

  projectClips.forEach(clip => {
    clip.proxyStatus = "READY";
    clip.proxyUri = `proxy_${clip.id}.mp4`;
    count++;

    db.transcodingJobs.push({
      id: "job_all_" + clip.id + "_" + Date.now(),
      clipId: clip.id,
      mediaTitle: clip.title,
      originalResolution: "1080p FHD",
      targetResolution: targetRes,
      progressPercent: 100,
      statusMessage: `Proxy Transcoded (${targetRes})`,
      isCompleted: true
    });
  });

  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, transcodedCount: count, targetResolution: targetRes }, null, 2);
  return `\n🚀 BACKGROUND TRANSCODER ENGINE\n=========================================\n✅ Berhasil mentranscode ${count} klip video ke resolusi proxy (${targetRes})!\nBeban GPU Timeline berkurang ~75% untuk pemutaran 60 FPS tanpa lag.\n`;
}

function viewProxyStatus(jsonMode = false) {
  const db = getDb();
  const s = db.settings;
  const jobs = db.transcodingJobs;

  if (jsonMode) return JSON.stringify({ settings: s, jobs }, null, 2);

  let out = `\n⚙️ STATUS LOW-RESOLUTION PROXY ENGINE\n`;
  out += `=========================================\n`;
  out += `Mode Proxy Preview : ${s.isProxyModeEnabled ? "⚡ AKTIF" : "🎬 NONAKTIF (1080p Direct)"}\n`;
  out += `Target Resolution  : ${s.proxyResolution}\n`;
  out += `Auto-Transcode     : ${s.autoTranscodeOnImport ? "AKTIF" : "NONAKTIF"}\n`;
  out += `Beban GPU Estimasi : ${s.isProxyModeEnabled ? "15% - 25% (Sangat Ringan)" : "85% - 100% (High-Res Direct)"}\n\n`;

  out += `📋 RIWAYAT TUGAS TRANSCODER LATAR BELAKANG (${jobs.length} Tugas):\n`;
  if (jobs.length === 0) {
    out += `   (Belum ada tugas transcoding)\n`;
  } else {
    jobs.slice(-5).forEach(j => {
      out += `   - [${j.isCompleted ? "DONE" : "IN_PROGRESS"}] ${j.mediaTitle} (${j.originalResolution} -> ${j.targetResolution}) - Progress: ${j.progressPercent}%\n`;
    });
  }
  return out;
}

module.exports = {
  toggleProxy,
  setProxyResolution,
  toggleAutoTranscode,
  transcodeClip,
  transcodeAll,
  viewProxyStatus
};
