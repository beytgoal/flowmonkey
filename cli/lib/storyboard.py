import json
import time
from .store import get_db, save_db

STORYBOARD_TEMPLATES = {
    "TikTok Viral": {
        "name": "TikTok Viral",
        "shots": [
            {"id": 1, "title": "Hook 3 Detik Pertama", "durationSec": 3, "prompt": "Fast motion dynamic hook opening with vibrant lighting", "camera": "Zoom In"},
            {"id": 2, "title": "Aksi Utama / B-Roll", "durationSec": 5, "prompt": "Main subject demonstration in ultra crisp 60fps", "camera": "Panning Right"},
            {"id": 3, "title": "Puncak Masalah / Emosi", "durationSec": 4, "prompt": "Close up emotional reaction with dramatic lighting", "camera": "Static Medium"},
            {"id": 4, "title": "Call to Action & Outro", "durationSec": 3, "prompt": "End screen with glowing subscribe branding button", "camera": "Zoom Out"}
        ]
    },
    "Product Showcase": {
        "name": "Product Showcase",
        "shots": [
            {"id": 1, "title": "Desain Bodi Futuristik", "durationSec": 4, "prompt": "360 degree slow motion rotation of luxury tech product", "camera": "Orbit"},
            {"id": 2, "title": "Macro Detail Material", "durationSec": 3, "prompt": "Extreme macro shot highlighting premium texture", "camera": "Macro Close Up"},
            {"id": 3, "title": "Fitur Unggulan Beraksi", "durationSec": 5, "prompt": "Product feature demonstration in realistic studio lighting", "camera": "Tracking"},
            {"id": 4, "title": "Harga & Logo Branding", "durationSec": 3, "prompt": "Sleek product graphic overlay with price tag", "camera": "Static"}
        ]
    },
    "Music Video": {
        "name": "Music Video",
        "shots": [
            {"id": 1, "title": "Intro Visual Ambience", "durationSec": 5, "prompt": "Wide landscape cinematic atmosphere with fog and neon tint", "camera": "Wide Crane"},
            {"id": 2, "title": "Lip Sync Vocalist Shot", "durationSec": 6, "prompt": "Artist singing with anamorphic lens flare highlights", "camera": "Handheld Motion"},
            {"id": 3, "title": "Montase Ritme Cepat", "durationSec": 4, "prompt": "Rhythmic fast cuts synced to bass drop pulses", "camera": "Quick Cut"},
            {"id": 4, "title": "Climax Performance", "durationSec": 5, "prompt": "Energetic live performance with stage lasers and smoke", "camera": "Dolly Zoom"}
        ]
    },
    "Short Film": {
        "name": "Short Film",
        "shots": [
            {"id": 1, "title": "Establishing Shot Suasana", "durationSec": 6, "prompt": "Mysterious rainy city street at twilight", "camera": "Wide Static"},
            {"id": 2, "title": "Karakter Berjalan di Hujan", "durationSec": 5, "prompt": "Protagonist walking down alley with reflective neon puddles", "camera": "Low Angle Track"},
            {"id": 3, "title": "Konfrontasi & Dialog Utama", "durationSec": 7, "prompt": "Two figures facing each other under single streetlamp", "camera": "Over The Shoulder"},
            {"id": 4, "title": "Ending Menggantung", "durationSec": 4, "prompt": "Fade out slowly as silhouette disappears into shadows", "camera": "Crane Up"}
        ]
    }
}

def list_templates(json_mode=False):
    if json_mode:
        return json.dumps(STORYBOARD_TEMPLATES, indent=2)
    out = f"\n🎬 TEMPLATE STORYBOARD AI STUDIO (Python)\n"
    out += f"=================================================\n"
    for name, t in STORYBOARD_TEMPLATES.items():
        total_duration = sum(s["durationSec"] for s in t["shots"])
        out += f"📌 Template: \"{t['name']}\" ({len(t['shots'])} Shot, Total: {total_duration}s)\n"
        for s in t["shots"]:
            out += f"   - [{s['title']}] ({s['durationSec']}s): \"{s['prompt']}\"\n"
        out += "\n"
    return out

def compile_storyboard_to_timeline(template_name, json_mode=False):
    template = STORYBOARD_TEMPLATES.get(template_name, STORYBOARD_TEMPLATES["TikTok Viral"])
    db = get_db()
    active_project_id = db.get("activeProjectId")
    if not active_project_id:
        if json_mode:
            return json.dumps({"error": "No active project"})
        return "❌ Error: Pilih atau buat proyek aktif terlebih dahulu."

    tracks = [t for t in db.get("tracks", []) if t.get("projectId") == active_project_id and t.get("trackIndex") == 0]
    main_track = tracks[0] if tracks else db.get("tracks", [{}])[0]

    current_start_ms = 0
    created_clips = []
    settings = db.get("settings", {})
    auto_transcode = settings.get("autoTranscodeOnImport", True)
    proxy_res = settings.get("proxyResolution", "360p Proxy")

    clips = db.get("clips", [])
    for s in template["shots"]:
        clip_id = max([c.get("id", 0) for c in clips], default=0) + 1
        duration_ms = s["durationSec"] * 1000
        end_ms = current_start_ms + duration_ms

        new_clip = {
            "id": clip_id,
            "projectId": active_project_id,
            "trackId": main_track.get("id"),
            "title": f"[SB] {s['title']}",
            "uri": f"https://generated.flowmonkey.ai/sb/{clip_id}.mp4",
            "startTimeMs": current_start_ms,
            "endTimeMs": end_ms,
            "durationMs": duration_ms,
            "filterName": "Cinematic Glow",
            "speedMultiplier": 1.0,
            "transitionType": "Dissolve",
            "volume": 1.0,
            "isMuted": False,
            "audioFadeInSec": 0.5,
            "audioFadeOutSec": 0.5,
            "audioPitch": 1.0,
            "noiseReduction": True,
            "vocalEnhance": False,
            "proxyUri": f"proxy_sb_{clip_id}.mp4" if auto_transcode else "",
            "proxyStatus": "READY" if auto_transcode else "IDLE"
        }

        clips.append(new_clip)
        created_clips.append(new_clip)
        current_start_ms = end_ms

        if auto_transcode:
            db.setdefault("transcodingJobs", []).append({
                "id": f"job_sb_{clip_id}",
                "clipId": clip_id,
                "mediaTitle": new_clip["title"],
                "originalResolution": "1080p FHD",
                "targetResolution": proxy_res,
                "progressPercent": 100,
                "statusMessage": "Proxy Low-Res Transcoded",
                "isCompleted": True
            })

    db["clips"] = clips
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "template": template["name"], "clips": created_clips}, indent=2)

    log = f"\n🚀 STORYBOARD ENGINE COMPILER (Python)\n"
    log += f"=========================================\n"
    log += f"Template Digunakan : \"{template['name']}\"\n"
    log += f"Jumlah Shot        : {len(created_clips)} klip\n"
    log += f"Total Durasi       : {current_start_ms / 1000} detik\n"
    log += f"Status             : ✅ Semua adegan dikompilasi ke Timeline Track 1!\n"
    return log
