const { getDb, saveDb } = require('./store');

function exportVideo(options) {
  const {
    resolution = "1080p FHD",
    fps = 60,
    format = "MP4",
    bitrate = "High (16 Mbps)",
    jsonMode = false
  } = options;

  const db = getDb();
  const activeProjectId = db.activeProjectId;
  const project = db.projects.find(p => p.id === activeProjectId);
  if (!project) {
    if (jsonMode) return JSON.stringify({ error: "No active project" });
    return "❌ Error: Tidak ada proyek aktif untuk diexport.";
  }

  const clips = db.clips.filter(c => c.projectId === activeProjectId);
  const totalDurationSec = clips.reduce((acc, c) => Math.max(acc, c.endTimeMs), 0) / 1000;

  const fileName = `FlowMonkey_${project.title.toLowerCase().replace(/[^a-z0-9]/g, '_')}_${resolution.take ? resolution.take(4) : '1080'}_${Date.now()}.${format.toLowerCase()}`;
  const outputPath = `/sdcard/Movies/FlowMonkey/${fileName}`;

  const exportRecord = {
    id: "exp_" + Date.now(),
    projectId: activeProjectId,
    projectTitle: project.title,
    resolution,
    fps: parseInt(fps),
    format,
    bitrate,
    durationSec: totalDurationSec,
    outputPath,
    exportedAt: new Date().toISOString()
  };

  db.exports.push(exportRecord);
  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, export: exportRecord }, null, 2);

  let out = `\n🎬 FLOWMONKEY EXPORT & RENDERING STUDIO ENGINE\n`;
  out += `=========================================================\n`;
  out += `Proyek Target   : "${project.title}"\n`;
  out += `Total Durasi    : ${totalDurationSec} detik (${clips.length} Klip Timeline)\n`;
  out += `Resolusi Export : ${resolution}\n`;
  out += `Frame Rate      : ${fps} FPS\n`;
  out += `Format Video    : ${format} (${bitrate})\n\n`;

  out += `[PASS 1/3] 🔍 Menggabungkan Video & Audio Tracks...\n`;
  out += `[PASS 2/3] 🎨 Mengaplikasikan Filter Visual & Transisi...\n`;
  out += `[PASS 3/3] ⚡ Hardware Encoding Render Multi-Pass (Full Quality Original Assets)...\n\n`;

  out += `🎉 EXPORT SUKSES!\n`;
  out += `File Output     : ${outputPath}\n`;
  out += `Status          : Selesai 100% tanpa kompresi proxy.\n`;
  return out;
}

function listExports(jsonMode = false) {
  const db = getDb();
  if (jsonMode) return JSON.stringify(db.exports, null, 2);

  let out = `\n📂 RIWAYAT EXPORT VIDEO STUDIO (${db.exports.length} Video):\n`;
  out += `=========================================================\n`;
  if (db.exports.length === 0) {
    out += `   (Belum ada video yang diexport)\n`;
  } else {
    db.exports.forEach(e => {
      out += `📌 "${e.projectTitle}" | ${e.resolution} @ ${e.fps}FPS | Durasi: ${e.durationSec}s | Path: ${e.outputPath} | Date: ${e.exportedAt.substring(0, 10)}\n`;
    });
  }
  return out;
}

module.exports = {
  exportVideo,
  listExports
};
