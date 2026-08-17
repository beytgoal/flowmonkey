# 🎬 FLOWMONKEY VIDEO STUDIO CLI & AI AGENT ENGINE (v3.0)

Sinkronisasi 1:1 identik antara **Aplikasi Android Jetpack Compose Studio** dan **Antarmuka CLI (Python & Node.js)** yang dirancang untuk dioperasikan oleh AI Agent maupun pengguna terminal.

---

## ⚡ Fitur Utama 1:1 Parity

1. **Vercel + Remotion Cloud VFX Rendering Backend**
   - Delegasikan animasi 3D kinetik, transisi glitch digital RGB, HUD infografis cyberpunk, dan partikel neon ke cluster GPU serverless.
   - Hasil render langsung ditautkan secara presisi ke multi-track timeline proyek aktif.

2. **Zero-Copy GPU-Driven Streaming Pipeline (4K Memory Protection)**
   - Eliminasi duplikasi memori antar MediaPipe, OpenCV, GStreamer, dan Surface.
   - Menghemat >400MB RAM pada pemrosesan resolusi tinggi.

3. **Dynamic AI Model Lifecycle (On-Demand Model Loading)**
   - Model AI INT8 (Face Mesh, Body Pose, Human Matting/Cutout, Gesture Tracker, Object Detector) diunduh on-demand dan dimuat/dilepas dari GPU RAM sesuai kebutuhan.

4. **Trimmed FFmpeg Audio Engine & MediaPipe VFX**
   - Ekstraksi audio instan (AAC), AI Voice Changer presets (*Robot, Chipmunk, Deep Monster, Radio Walkie, Alien, Studio Reverb*), Spectral Noise Reduction (Denoise), dan Beat Waveform Analyzer.
   - Quantized Portrait Face Mesh Retouching & Glowing Silhouette VFX.

5. **Multi-Track Timeline & Low-Res Proxy Engine**
   - Dukungan track tak terbatas (Video, Audio, Overlay B-Roll, Teks/VFX).
   - Bézier Keyframing, Dynamic Speed Ramping Curves (*Hero, Bullet Time, Montage*), Split & Trimming.
   - Background Proxy Transcoding (360p / 540p) untuk editing mulus tanpa lag.

6. **Automated AI Agent Runner (`agent`) & Mode Interaktif REPL**
   - Menerima instruksi bahasa alami dan mengeksekusi pipeline end-to-end secara otomatis.

---

## 🚀 Panduan Penggunaan CLI

### 1. Vercel + Remotion Cloud VFX
```bash
# Node.js
node cli/index.js remotion list
node cli/index.js remotion render --vfx kinetic --title "FLOWMONKEY 2026" --color "#8B5CF6"
node cli/index.js remotion render --vfx glitch --title "CYBER GLITCH" --color "#EC4899"
node cli/index.js remotion preview --vfx hud --frame 45

# Python
python3 cli/main.py remotion list
python3 cli/main.py remotion render --vfx particle --title "NEON EMBERS" --color "#00E5FF"
```

### 2. Zero-Copy Pipeline & Dynamic AI Models
```bash
# Status Zero-Copy
node cli/index.js zerocopy status
python3 cli/main.py zerocopy status

# Dynamic AI Models (Download & Muat ke GPU RAM)
node cli/index.js model list
node cli/index.js model download face_mesh_int8
node cli/index.js model load face_mesh_int8
node cli/index.js model unload face_mesh_int8
```

### 3. Audio & VFX Processing
```bash
# Ekstrak audio dari klip
node cli/index.js audio extract --clip-id 1

# Ubah suara & Denoise
node cli/index.js audio voice --clip-id 1 --effect "Robot"
node cli/index.js audio denoise --clip-id 1 --enable true

# Beat Waveform Sync
node cli/index.js audio beat --clip-id 1

# MediaPipe Retouch & Silhouette Glow
node cli/index.js vfx retouch --clip-id 1 --smooth 0.85
node cli/index.js vfx silhouette --clip-id 1 --color "#00E5FF"
```

### 4. Eksekusi Otomatisasi AI Agent (Single-Prompt Automation)
```bash
# Contoh 1: Render Cloud VFX & Rangkai Proyek TikTok
python3 cli/main.py agent "Buatkan video TikTok viral berdurasi 15 detik dengan teks kinetik 'CYBER LAUNCH' dan warna cyan"

# Contoh 2: Proses Audio & Efek Suara
node cli/index.js agent "Ekstrak audio klip 1, ubah suara jadi Robot, hilangkan noise latar, dan ekspor ke MP4 1080p 60fps"
```

### 5. Mode Interaktif REPL
```bash
python3 cli/main.py repl
# atau
node cli/index.js repl
```

### 6. Output Terstruktur JSON untuk AI Agent Tools
Tambahkan flag `--json` pada perintah apapun:
```bash
python3 cli/main.py remotion render --vfx kinetic --title "AI SHOW" --json
node cli/index.js zerocopy status --json
node cli/index.js model list --json
```
