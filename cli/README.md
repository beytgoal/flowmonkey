# 🎬 FlowMonkey Video Studio CLI Engine (v2.1.0)

Framework CLI FlowMonkey Studio telah ditingkatkan ke **v2.1.0 (Python 3.10+ & Node.js)** untuk dukungan pustaka AI, manipulasi JSON, dan integrasi otomatisasi agent yang jauh lebih baik.

## 🚀 Fitur Utama Python CLI
- **Proyek Video**: `python3 cli/main.py project list|create|select|delete`
- **AI Video Generator (Veo & Highfield)**: `python3 cli/main.py generate --prompt "Text" --style "Cinematic" --duration 10`
- **Storyboard Engine**: `python3 cli/main.py storyboard list|compile "TikTok Viral"`
- **Timeline Multi-Track Editor**: `python3 cli/main.py timeline view|add|filter|speed|curve|keyframe|split|audio|delete`
- **Low-Resolution Proxy Engine**: `python3 cli/main.py proxy status|toggle|res|auto|transcode|transcode-all`
- **Export & Render Studio**: `python3 cli/main.py export run|list`
- **Studio Settings**: `python3 cli/main.py settings view|update`
- **AI Agent Automated Runner**: `python3 cli/main.py agent "Instruksi AI Agent"`
- **Interactive REPL**: `python3 cli/main.py repl`

## 🛠️ Cara Menggunakan
```bash
# Jalankan perintah langsung
python3 cli/main.py timeline view

# Atau masuk ke REPL interaktif
python3 cli/main.py repl
```
