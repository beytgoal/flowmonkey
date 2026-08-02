# FlowMonkey Video Studio CLI Engine

Versi CLI (Command Line Interface) lengkap dan identik dari **FlowMonkey Video Studio App**, dirancang khusus agar dapat dioperasikan secara terprogram oleh **AI Agent** maupun developer melalui terminal/shell.

---

## 🚀 Fitur Lengkap Identik (100% Parity dengan Android UI)

1. **📁 Pengelolaan Proyek (Project Management)**:
   - Buat, lihat, pilih, dan hapus proyek video.
   - Dukungan rasio aspek (`9:16`, `16:9`, `1:1`, `4:3`, `21:9`) dan resolusi target.

2. **🤖 AI Video Generator (Veo 2 & Highfield)**:
   - Model: `Veo 2`, `Veo Fast`, `Highfield Pro`.
   - Preset Gaya: `Cinematic`, `Cyberpunk`, `Anime`, `3D Render`, `Hyper-Realistic`, `Photorealistic`, `Retro 80s`.
   - Pengaturan durasi, FPS, dan otomatis penambahan ke timeline.

3. **🎬 Storyboard Engine**:
   - Template pre-built: `TikTok Viral`, `Product Showcase`, `Music Video`, `Short Film`.
   - Kompilasi otomatis adegan ke multi-track timeline.

4. **✂️ Multi-Track Timeline Editor**:
   - Track Video Utama, Overlay/B-Roll, Audio, dan Subtitel.
   - Tambah klip, pemotongan (*split*), *trimming*, pengaturan kecepatan (*0.25x - 4.0x*).
   - **📈 Curve Speed Ramping**: Preset kurva `Hero`, `Bullet Time`, `Montage`, `Fast Out`, `Slow In`, dan `Custom Curve`.
   - **💎 Keyframe Animation System**: Kontrol keyframe presisi untuk Posisi X/Y, *Scale*, *Rotation*, *Opacity*, dan Kurva *Easing* (Bezier/EaseInOut).
   - Filter visual (`Cinematic Glow`, `Cyberpunk Neon`, `Vintage Film`, `Black & White`, dll).
   - Efek transisi (`Dissolve`, `Wipe Left/Right`, `Zoom In`, `Fade Black`).
   - Kontrol audio lengkap (*Volume*, *Mute*, *Noise Reduction*, *Vocal Enhance*).

5. **⚡ Low-Resolution Proxy Preview Engine & Background Transcoder**:
   - Sakelar **Proxy Mode** vs **Original 1080p Quality Mode**.
   - Target resolusi proxy (`360p Proxy`, `540p Proxy`, `720p Proxy`).
   - Pengubah otomatis (*Auto-Transcode*) saat impor aset baru.
   - Transcoder latar belakang dengan pemantauan status job & estimasi beban GPU.

6. **📼 Export & Rendering Studio Engine**:
   - Pilihan resolusi export (`1080p FHD`, `4K Ultra HD`, `720p HD`), FPS (`60`, `30`, `24`), format (`MP4`, `WEBM`).
   - Simulasi render multi-pass tanpa kompresi proxy (selalu menggunakan aset original resolusi tinggi).

7. **🤖 AI Agent Automation Runner**:
   - Jalankan tugas otomatisasi langsung dari prompt bahasa alami: `node cli/index.js agent "<Instruksi AI Agent>"`.

---

## 🛠️ Panduan Penggunaan CLI

### 1. Perintah Bantuan & List Proyek
```bash
node cli/index.js help
node cli/index.js project list
node cli/index.js project create "TikTok Viral Trends" 9:16
node cli/index.js project select 1
```

### 2. AI Video Generation
```bash
node cli/index.js generate --prompt "Cyberpunk drone footage over neon city" --style Cyberpunk --model "Veo 2" --duration 5
```

### 3. Storyboard Engine
```bash
node cli/index.js storyboard list
node cli/index.js storyboard compile "TikTok Viral"
```

### 4. Multi-Track Timeline Editing & Keyframes
```bash
node cli/index.js timeline view
node cli/index.js timeline add --title "B-Roll Extra" --duration 4
node cli/index.js timeline filter --clip-id 1 --name "Cinematic Glow"
node cli/index.js timeline speed --clip-id 1 --multiplier 1.5
node cli/index.js timeline curve --clip-id 1 --name "Bullet Time"
node cli/index.js keyframe add --clip-id 1 --time 1.5 --x 20 --y -10 --scale 1.3 --rotation 12 --ease Bezier
node cli/index.js keyframe list --clip-id 1
node cli/index.js timeline split --clip-id 1 --time 2.5
node cli/index.js timeline audio --clip-id 1 --vol 0.8 --noise-red true
```

### 5. Proxy Preview Engine & Background Transcoder
```bash
node cli/index.js proxy status
node cli/index.js proxy toggle --mode on
node cli/index.js proxy res "360p Proxy"
node cli/index.js proxy auto --mode on
node cli/index.js proxy transcode-all
```

### 6. Export Studio
```bash
node cli/index.js export run --res "1080p FHD" --fps 60 --format MP4
node cli/index.js export list
```

### 7. Pengaturan & API
```bash
node cli/index.js settings view
node cli/index.js settings update --gemini-key "AIzaSy..." --theme dark
```

### 8. Otomatisasi AI Agent (Single Execution / Batch)
```bash
node cli/index.js agent "Buat video TikTok 15 detik tentang kuliner malam dengan musik upbeat"
```

### 9. Mode Interaktif REPL untuk AI Agent
```bash
node cli/index.js repl
```

---

## 🤖 Operasi Format Structured Data (JSON Output untuk Agent Parsing)
Tambahkan flag `--json` pada perintah manapun untuk menerima output JSON terstruktur yang mudah diproses oleh skrip atau AI Agent:

```bash
node cli/index.js timeline view --json
node cli/index.js project list --json
node cli/index.js proxy status --json
```
