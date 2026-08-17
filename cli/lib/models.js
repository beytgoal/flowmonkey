const { getDb, saveDb } = require('./store');

const DEFAULT_MODELS = [
  {
    id: "face_mesh_int8",
    name: "MediaPipe Face Mesh (Beauty Retouch)",
    approxSizeBytes: 2800000,
    sizeDisplay: "2.8 MB",
    status: "CACHED_ON_DISK", // NOT_DOWNLOADED, DOWNLOADING, CACHED_ON_DISK, LOADED_IN_GPU_RAM
    memoryUsageMb: 0.0,
    description: "468 landmark face geometry untuk penghalusan kulit & lighting"
  },
  {
    id: "pose_tracker_int8",
    name: "MediaPipe Body Pose (Silhouette VFX)",
    approxSizeBytes: 3400000,
    sizeDisplay: "3.4 MB",
    status: "LOADED_IN_GPU_RAM",
    memoryUsageMb: 3.4,
    description: "33 3D body keypoints untuk kinetic glow & silhouette vfx"
  },
  {
    id: "bg_segmenter_int8",
    name: "AI Background Cutout (Matting)",
    approxSizeBytes: 2100000,
    sizeDisplay: "2.1 MB",
    status: "CACHED_ON_DISK",
    memoryUsageMb: 0.0,
    description: "High-precision human matting & background removal"
  },
  {
    id: "hand_gesture_tracker",
    name: "MediaPipe Hand Gesture Tracker",
    approxSizeBytes: 1900000,
    sizeDisplay: "1.9 MB",
    status: "NOT_DOWNLOADED",
    memoryUsageMb: 0.0,
    description: "21 keypoint hand tracking untuk stiker interaktif"
  },
  {
    id: "object_detector_int8",
    name: "AI Smart Object Tracker",
    approxSizeBytes: 3800000,
    sizeDisplay: "3.8 MB",
    status: "NOT_DOWNLOADED",
    memoryUsageMb: 0.0,
    description: "Real-time bounding box tracking untuk pinning teks & visual stickers"
  }
];

function listAiModels(jsonMode = false) {
  const db = getDb();
  if (!db.aiModels || db.aiModels.length === 0) {
    db.aiModels = DEFAULT_MODELS;
    saveDb(db);
  }

  const models = db.aiModels;
  if (jsonMode) {
    return JSON.stringify({ models }, null, 2);
  }

  let output = `\n🧠 DYNAMIC AI MODEL LIFECYCLE (DYNAMIC MODEL LOADING)\n======================================================\n`;
  models.forEach((m, idx) => {
    const statusTag = m.status === 'LOADED_IN_GPU_RAM' 
      ? '🟢 LOADED IN GPU' 
      : (m.status === 'CACHED_ON_DISK' ? '💾 DISK CACHE' : '⚪ NOT DOWNLOADED');
    output += `${idx + 1}. [${m.id}] ${m.name}\n`;
    output += `   Status       : ${statusTag} (${m.sizeDisplay}) | RAM: ${m.memoryUsageMb.toFixed(1)} MB\n`;
    output += `   Deskripsi    : ${m.description}\n\n`;
  });
  output += `======================================================\n`;
  return output;
}

function downloadAiModel(modelId, jsonMode = false) {
  const db = getDb();
  if (!db.aiModels) db.aiModels = DEFAULT_MODELS;

  const model = db.aiModels.find(m => m.id.toLowerCase() === modelId.toLowerCase());
  if (!model) {
    const err = `Model AI dengan ID '${modelId}' tidak ditemukan.`;
    if (jsonMode) return JSON.stringify({ success: false, error: err }, null, 2);
    return `❌ ${err}`;
  }

  model.status = "CACHED_ON_DISK";
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      modelId: model.id,
      name: model.name,
      status: model.status,
      size: model.sizeDisplay
    }, null, 2);
  }

  return `✅ Model '${model.name}' (${model.sizeDisplay}) berhasil diunduh on-demand ke disk cache lokal.`;
}

function loadModelToGpu(modelId, jsonMode = false) {
  const db = getDb();
  if (!db.aiModels) db.aiModels = DEFAULT_MODELS;

  const model = db.aiModels.find(m => m.id.toLowerCase() === modelId.toLowerCase());
  if (!model) {
    const err = `Model AI dengan ID '${modelId}' tidak ditemukan.`;
    if (jsonMode) return JSON.stringify({ success: false, error: err }, null, 2);
    return `❌ ${err}`;
  }

  model.status = "LOADED_IN_GPU_RAM";
  model.memoryUsageMb = model.approxSizeBytes / (1024 * 1024);
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      modelId: model.id,
      name: model.name,
      status: model.status,
      memoryUsageMb: parseFloat(model.memoryUsageMb.toFixed(2))
    }, null, 2);
  }

  return `🚀 Model '${model.name}' berhasil dimuat ke GPU/NPU RAM (${model.memoryUsageMb.toFixed(1)} MB). Siap untuk inferensi real-time.`;
}

function unloadModelFromGpu(modelId, jsonMode = false) {
  const db = getDb();
  if (!db.aiModels) db.aiModels = DEFAULT_MODELS;

  const model = db.aiModels.find(m => m.id.toLowerCase() === modelId.toLowerCase());
  if (!model) {
    const err = `Model AI dengan ID '${modelId}' tidak ditemukan.`;
    if (jsonMode) return JSON.stringify({ success: false, error: err }, null, 2);
    return `❌ ${err}`;
  }

  model.status = "CACHED_ON_DISK";
  model.memoryUsageMb = 0.0;
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      modelId: model.id,
      name: model.name,
      status: model.status
    }, null, 2);
  }

  return `💤 Model '${model.name}' berhasil dilepaskan dari memori RAM (RAM dibebaskan).`;
}

function purgeAiModelCaches(jsonMode = false) {
  const db = getDb();
  db.aiModels = DEFAULT_MODELS.map(m => ({
    ...m,
    status: "NOT_DOWNLOADED",
    memoryUsageMb: 0.0
  }));
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      message: "Semua cache file model AI berhasil dibersihkan dari penyimpanan."
    }, null, 2);
  }

  return `🗑️ Semua cache file model AI (~14 MB) berhasil dibersihkan dari penyimpanan.`;
}

module.exports = {
  listAiModels,
  downloadAiModel,
  loadModelToGpu,
  unloadModelFromGpu,
  purgeAiModelCaches
};
