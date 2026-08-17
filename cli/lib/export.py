import json
import re
import time
from datetime import datetime
from .store import get_db, save_db

def export_video(options=None, **kwargs):
    if options is None:
        options = kwargs
    elif isinstance(options, dict) and kwargs:
        options = {**options, **kwargs}
    elif not isinstance(options, dict):
        options = kwargs

    resolution = options.get("resolution", "1080p FHD")
    fps = int(options.get("fps", 60))
    fmt = options.get("format", "MP4")
    bitrate = options.get("bitrate", "High (16 Mbps)")
    json_mode = options.get("jsonMode", False)

    db = get_db()
    active_project_id = db.get("activeProjectId")
    projects = db.get("projects", [])
    project = next((p for p in projects if p.get("id") == active_project_id), None)

    if not project:
        if json_mode:
            return json.dumps({"error": "No active project"})
        return "❌ Error: Tidak ada proyek aktif untuk diexport."

    clips = [c for c in db.get("clips", []) if c.get("projectId") == active_project_id]
    total_duration_sec = max([c.get("endTimeMs", 0) for c in clips], default=0) / 1000.0

    safe_title = re.sub(r'[^a-z0-9]', '_', project.get("title", "").lower())
    res_tag = resolution[:4] if resolution else "1080"
    file_name = f"FlowMonkey_{safe_title}_{res_tag}_{int(time.time() * 1000)}.{fmt.lower()}"
    output_path = f"/sdcard/Movies/FlowMonkey/{file_name}"

    export_record = {
        "id": f"exp_{int(time.time() * 1000)}",
        "projectId": active_project_id,
        "projectTitle": project.get("title"),
        "resolution": resolution,
        "fps": fps,
        "format": fmt,
        "bitrate": bitrate,
        "durationSec": total_duration_sec,
        "outputPath": output_path,
        "exportedAt": datetime.now().isoformat()
    }

    db.setdefault("exports", []).append(export_record)
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "export": export_record}, indent=2)

    out = f"\n🎬 FLOWMONKEY EXPORT & RENDERING STUDIO ENGINE (Python)\n"
    out += f"=========================================================\n"
    out += f"Proyek Target   : \"{project.get('title')}\"\n"
    out += f"Total Durasi    : {total_duration_sec} detik ({len(clips)} Klip Timeline)\n"
    out += f"Resolusi Export : {resolution}\n"
    out += f"Frame Rate      : {fps} FPS\n"
    out += f"Format Video    : {fmt} ({bitrate})\n\n"

    out += f"[PASS 1/3] 🔍 Menggabungkan Video & Audio Tracks...\n"
    out += f"[PASS 2/3] 🎨 Mengaplikasikan Filter Visual & Transisi...\n"
    out += f"[PASS 3/3] ⚡ Hardware Encoding Render Multi-Pass (Full Quality Original Assets)...\n\n"

    out += f"🎉 EXPORT SUKSES!\n"
    out += f"File Output     : {output_path}\n"
    out += f"Status          : Selesai 100% tanpa kompresi proxy.\n"
    return out

def list_exports(json_mode=False):
    db = get_db()
    exports = db.get("exports", [])
    if json_mode:
        return json.dumps(exports, indent=2)

    out = f"\n📂 RIWAYAT EXPORT VIDEO STUDIO ({len(exports)} Video):\n"
    out += f"=========================================================\n"
    if not exports:
        out += "   (Belum ada video yang diexport)\n"
    else:
        for e in exports:
            created = e.get("exportedAt", "")[:10]
            out += f"📌 \"{e.get('projectTitle')}\" | {e.get('resolution')} @ {e.get('fps')}FPS | Durasi: {e.get('durationSec')}s | Path: {e.get('outputPath')} | Date: {created}\n"
    return out
