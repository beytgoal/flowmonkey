const { getDb, saveDb } = require('./store');

function viewSettings(jsonMode = false) {
  const db = getDb();
  const s = db.settings;

  if (jsonMode) return JSON.stringify(s, null, 2);

  let out = `\n⚙️ PENGATURAN STUDIO & KONFIGURASI API (v2.1.0)\n`;
  out += `=========================================================\n`;
  out += `CLI Engine Version: v2.1.0\n`;
  out += `Gemini API Key    : ${s.geminiApiKey ? "••••••••" + s.geminiApiKey.slice(-4) : "Belum diatur (Menggunakan Fallback Engine)"}\n`;
  out += `Highfield Key     : ${s.highfieldApiKey ? "••••••••" + s.highfieldApiKey.slice(-4) : "Terhubung"}\n`;
  out += `Custom Endpoint   : ${s.customEndpoint}\n`;
  out += `Mode Theme        : ${s.isDarkTheme ? "🌙 Dark Theme" : "☀️ Light Theme"}\n`;
  out += `Mode Proxy Active : ${s.isProxyModeEnabled ? "⚡ ON (" + s.proxyResolution + ")" : "🎬 OFF (1080p Original)"}\n`;
  out += `Auto-Transcode    : ${s.autoTranscodeOnImport ? "ON" : "OFF"}\n`;
  return out;
}

function updateSettings(options) {
  const { geminiApiKey, highfieldApiKey, customEndpoint, isDarkTheme, jsonMode = false } = options;
  const db = getDb();

  if (geminiApiKey !== undefined) db.settings.geminiApiKey = geminiApiKey;
  if (highfieldApiKey !== undefined) db.settings.highfieldApiKey = highfieldApiKey;
  if (customEndpoint !== undefined) db.settings.customEndpoint = customEndpoint;
  if (isDarkTheme !== undefined) db.settings.isDarkTheme = Boolean(isDarkTheme);

  saveDb(db);

  if (jsonMode) return JSON.stringify({ success: true, settings: db.settings }, null, 2);
  return `\n✅ Pengaturan Studio berhasil diperbarui!\n`;
}

module.exports = {
  viewSettings,
  updateSettings
};
