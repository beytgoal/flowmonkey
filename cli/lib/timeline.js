const { getDb, saveDb } = require('./store');

const FILTERS = ["None", "Cinematic Glow", "Cyberpunk Neon", "Vintage Film", "Black & White", "Warm Sunset", "Anamorphic Flare", "Dramatic Contrast"];
const TRANSITIONS = ["None", "Dissolve", "Wipe Left", "Wipe Right", "Zoom In", "Fade Black"];
const SPEED_CURVES = ["Normal", "Hero", "Bullet Time", "Montage", "Fast Out", "Slow In", "Custom Curve"];

function viewTimeline(jsonMode = false) {
  const db = getDb();
  const activeProjectId = db.activeProjectId;
  const project = db.projects.find(p => p.id === activeProjectId);
  if (!project) {
    if (jsonMode) return JSON.stringify({ error: "No active project" });
    return "❌ Error: Tidak ada proyek aktif.";
  }

  const tracks = db.tracks.filter(t => t.projectId === activeProjectId);
  const clips = db.clips.filter(c => c.projectId === activeProjectId);
  const proxyMode = db.settings.isProxyModeEnabled;
  const proxyRes = db.settings.proxyResolution;

  if (jsonMode) {
    return JSON.stringify({
      project,
      proxyMode: { isEnabled: proxyMode, resolution: proxyRes },
      tracks,
      clips
    }, null, 2);
  }

  let out = `\n🎬 TIMELINE MULTI-TRACK EDITOR [Proyek: "${project.title}"]\n`;
  out += `=========================================================\n`;
  out += `⚡ Mode Pratinjau : ${proxyMode ? `PROXY LOW-RES (${proxyRes}) [Beban GPU minimal]` : "1080p FULL-RES ORIGINAL"}\n\n`;

  tracks.forEach(track => {
    const trackClips = clips.filter(c => c.trackId === track.id);
    out += `Track ${track.trackIndex + 1} (${track.type}) - "${track.label}":\n`;
    if (trackClips.length === 0) {
      out += `   (Kosong)\n`;
    } else {
      trackClips.forEach(c => {
        const startSec = (c.startTimeMs / 1000).toFixed(1);
        const endSec = (c.endTimeMs / 1000).toFixed(1);
        const durSec = (c.durationMs / 1000).toFixed(1);
        const kfCount = c.keyframes ? c.keyframes.length : (c.hasKeyframe ? 1 : 0);
        const curveInfo = c.speedCurve && c.speedCurve !== 'Normal' ? ` [Curve: ${c.speedCurve}]` : '';
        out += `   [ID: ${c.id}] "${c.title}" | ${startSec}s -> ${endSec}s (${durSec}s) | Speed: ${c.speedMultiplier}x${curveInfo} | Filter: ${c.filterName} | Keyframes: ${kfCount} | Proxy: ${c.proxyStatus}\n`;
      });
    }
    out += `\n`;
  });

  return out;
}

function addClip(options) {
  const { title = "Klip Media Baru", durationSec = 5, trackIndex = 0, jsonMode = false } = options;
  const db = getDb();
  const activeProjectId = db.activeProjectId;
  if (!activeProjectId) {
    if (jsonMode) return JSON.stringify({ error: "No active project" });
    return "❌ Error: Tidak ada proyek aktif.";
  }

  let track = db.tracks.find(t => t.projectId === activeProjectId && t.trackIndex === parseInt(trackIndex));
  if (!track) {
    track = db.tracks.find(t => t.projectId === activeProjectId) || db.tracks[0];
  }

  const existingClips = db.clips.filter(c => c.projectId === activeProjectId && c.trackId === track.id);
  const startTimeMs = existingClips.reduce((max, c) => Math.max(max, c.endTimeMs), 0);
  const durationMs = durationSec * 1000;
  const endTimeMs = startTimeMs + durationMs;

  const newClipId = db.clips.length > 0 ? Math.max(...db.clips.map(c => c.id)) + 1 : 1;
  const autoTranscode = db.settings.autoTranscodeOnImport;
  const proxyRes = db.settings.proxyResolution;

  const newClip = {
    id: newClipId,
    projectId: activeProjectId,
    trackId: track.id,
    title,
    uri: `file:///sdcard/Movies/${title.toLowerCase().replace(/\s+/g, '_')}.mp4`,
    startTimeMs,
    endTimeMs,
    durationMs,
    filterName: "None",
    speedMultiplier: 1.0,
    transitionType: "None",
    volume: 1.0,
    isMuted: false,
    audioFadeInSec: 0.0,
    audioFadeOutSec: 0.0,
    audioPitch: 1.0,
    noiseReduction: false,
    vocalEnhance: false,
    proxyUri: autoTranscode ? `proxy_${newClipId}.mp4` : "",
    proxyStatus: autoTranscode ? "READY" : "IDLE"
  };

  db.clips.push(newClip);

  if (autoTranscode) {
    db.transcodingJobs.push({
      id: "job_" + newClipId,
      clipId: newClipId,
      mediaTitle: title,
      originalResolution: "1080p FHD",
      targetResolution: proxyRes,
      progressPercent: 100,
      statusMessage: `Proxy Transcoded (${proxyRes})`,
      isCompleted: true
    });
  }

  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clip: newClip }, null, 2);
  return `\n✅ Klip berhasil ditambahkan ke Track ${track.trackIndex + 1}: ID ${newClipId} - "${title}" (${durationSec}s)\n`;
}

function updateFilter(clipId, filterName, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }
  clip.filterName = filterName;
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id, filterName }, null, 2);
  return `\n🎨 Filter visual klip ID ${clipId} diubah menjadi: "${filterName}"\n`;
}

function updateSpeed(clipId, speedMultiplier, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }
  const speed = parseFloat(speedMultiplier);
  clip.speedMultiplier = speed;
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id, speedMultiplier: speed }, null, 2);
  return `\n⚡ Kecepatan pemutaran klip ID ${clipId} diubah ke: ${speed}x\n`;
}

function splitClip(clipId, splitTimeSec, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  const splitTimeMs = clip.startTimeMs + (parseFloat(splitTimeSec) * 1000);
  if (splitTimeMs <= clip.startTimeMs || splitTimeMs >= clip.endTimeMs) {
    if (jsonMode) return JSON.stringify({ error: "Split time out of clip bounds" });
    return `❌ Error: Posisi pemotongan harus berada di antara ${(clip.startTimeMs/1000)}s dan ${(clip.endTimeMs/1000)}s.`;
  }

  const oldEndMs = clip.endTimeMs;
  clip.endTimeMs = splitTimeMs;
  clip.durationMs = splitTimeMs - clip.startTimeMs;

  const newClipId = Math.max(...db.clips.map(c => c.id)) + 1;
  const newClip = {
    ...clip,
    id: newClipId,
    title: `${clip.title} (Part 2)`,
    startTimeMs: splitTimeMs,
    endTimeMs: oldEndMs,
    durationMs: oldEndMs - splitTimeMs
  };

  db.clips.push(newClip);
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clip1: clip, clip2: newClip }, null, 2);
  return `\n✂️ Klip ID ${clipId} berhasil dipotong di detik ke-${splitTimeSec}s! Terbagi menjadi ID ${clipId} dan ID ${newClipId}.\n`;
}

function updateAudio(clipId, options) {
  const { volume, isMuted, noiseReduction, vocalEnhance, jsonMode = false } = options;
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  if (volume !== undefined) clip.volume = parseFloat(volume);
  if (isMuted !== undefined) clip.isMuted = Boolean(isMuted);
  if (noiseReduction !== undefined) clip.noiseReduction = Boolean(noiseReduction);
  if (vocalEnhance !== undefined) clip.vocalEnhance = Boolean(vocalEnhance);

  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, clip }, null, 2);
  return `\n🎵 Audio klip ID ${clipId} diperbarui (Vol: ${clip.volume * 100}%, Mute: ${clip.isMuted}, NoiseRed: ${clip.noiseReduction}, VocalEnhance: ${clip.vocalEnhance})\n`;
}

function deleteClip(clipId, jsonMode = false) {
  const db = getDb();
  const cId = parseInt(clipId);
  const exists = db.clips.some(c => c.id === cId);
  if (!exists) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }
  db.clips = db.clips.filter(c => c.id !== cId);
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, deletedClipId: cId }, null, 2);
  return `\n🗑️ Klip ID ${cId} berhasil dihapus dari timeline.\n`;
}

function setSpeedCurve(clipId, curveName, multiplier = 1.0, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }
  clip.speedCurve = curveName || "Hero";
  if (multiplier) clip.speedMultiplier = parseFloat(multiplier);
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id, speedCurve: clip.speedCurve, speedMultiplier: clip.speedMultiplier }, null, 2);
  return `\n📈 Speed Ramping Curve klip ID ${clipId} diatur ke: "${clip.speedCurve}" (Speed Base: ${clip.speedMultiplier}x)\n`;
}

function addKeyframe(clipId, options) {
  const { timeSec = 0.0, posX = 0, posY = 0, scale = 1.0, rotation = 0, opacity = 1.0, ease = "EaseInOut", jsonMode = false } = options;
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  if (!clip.keyframes) clip.keyframes = [];
  const kfId = clip.keyframes.length > 0 ? Math.max(...clip.keyframes.map(k => k.id)) + 1 : 1;
  const newKf = {
    id: kfId,
    timeSec: parseFloat(timeSec),
    posX: parseFloat(posX),
    posY: parseFloat(posY),
    scale: parseFloat(scale),
    rotation: parseFloat(rotation),
    opacity: parseFloat(opacity),
    ease
  };

  clip.keyframes.push(newKf);
  clip.hasKeyframe = true;
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id, keyframe: newKf }, null, 2);
  return `\n💎 Keyframe baru ditambahkan ke klip ID ${clipId} pada t=${timeSec}s [X:${posX}, Y:${posY}, Scale:${scale}, Rot:${rotation}°, Opacity:${opacity}, Ease:${ease}]\n`;
}

function listKeyframes(clipId, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  const kfs = clip.keyframes || [];
  if (jsonMode) return JSON.stringify({ clipId: clip.id, hasKeyframe: clip.hasKeyframe, keyframes: kfs }, null, 2);

  let out = `\n💎 LIST KEYFRAMES [Klip ID ${clip.id} - "${clip.title}"]:\n`;
  out += `=========================================================\n`;
  if (kfs.length === 0) {
    out += `   (Belum ada keyframe pada klip ini)\n`;
  } else {
    kfs.forEach(k => {
      out += `   - Keyframe ID ${k.id} @ t=${k.timeSec}s | Pos: (${k.posX}, ${k.posY}) | Scale: ${k.scale}x | Rot: ${k.rotation}° | Opacity: ${k.opacity} | Ease: ${k.ease}\n`;
    });
  }
  return out;
}

function removeKeyframe(clipId, keyframeId, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  const kfId = parseInt(keyframeId);
  clip.keyframes = (clip.keyframes || []).filter(k => k.id !== kfId);
  clip.hasKeyframe = clip.keyframes.length > 0;
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id, removedKeyframeId: kfId }, null, 2);
  return `\n🗑️ Keyframe ID ${kfId} dihapus dari klip ID ${clipId}.\n`;
}

function clearKeyframes(clipId, jsonMode = false) {
  const db = getDb();
  const clip = db.clips.find(c => c.id === parseInt(clipId));
  if (!clip) {
    if (jsonMode) return JSON.stringify({ error: "Clip not found" });
    return `❌ Error: Klip ID ${clipId} tidak ditemukan.`;
  }

  clip.keyframes = [];
  clip.hasKeyframe = false;
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, clipId: clip.id }, null, 2);
  return `\n🗑️ Semua keyframe dibersihkan dari klip ID ${clipId}.\n`;
}

module.exports = {
  viewTimeline,
  addClip,
  updateFilter,
  updateSpeed,
  setSpeedCurve,
  addKeyframe,
  listKeyframes,
  removeKeyframe,
  clearKeyframes,
  splitClip,
  updateAudio,
  deleteClip,
  FILTERS,
  TRANSITIONS,
  SPEED_CURVES
};
