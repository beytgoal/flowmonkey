import json
import time
from .store import get_db, save_db

STYLES = ["Cinematic", "Anime", "Cyberpunk", "3D Render", "Hyper-Realistic", "Photorealistic", "Retro 80s"]
MODELS = ["Veo 2", "Veo Fast", "Highfield Pro"]
RATIOS = ["16:9", "9:16", "1:1", "4:3", "21:9"]

def generate_ai_video(options=None, **kwargs):
    if options is None:
        options = kwargs
    elif isinstance(options, dict) and kwargs:
        options = {**options, **kwargs}
    elif not isinstance(options, dict):
        options = kwargs

    prompt = options.get("prompt", "A aesthetic cinematic drone clip over neon cyberpunk metropolis")
    style = options.get("style", "Cinematic")
    model = options.get("model", "Veo 2")
    ratio = options.get("ratio", "9:16")
    duration_sec = int(options.get("durationSec", 5))
    fps = int(options.get("fps", 30))
    json_mode = options.get("jsonMode", False)

    db = get_db()
    active_project_id = db.get("activeProjectId")
    if not active_project_id:
        if json_mode:
            return json.dumps({"error": "No active project"})
        return "❌ Error: Tidak ada proyek aktif. Buat atau pilih proyek dulu."

    tracks = [t for t in db.get("tracks", []) if t.get("projectId") == active_project_id and t.get("trackIndex") == 0]
    main_track = tracks[0] if tracks else db.get("tracks", [{}])[0]
    
    duration_ms = duration_sec * 1000
    existing_clips = [c for c in db.get("clips", []) if c.get("projectId") == active_project_id and c.get("trackId") == main_track.get("id")]
    start_time_ms = max([c.get("endTimeMs", 0) for c in existing_clips], default=0)
    end_time_ms = start_time_ms + duration_ms

    clips = db.get("clips", [])
    new_clip_id = max([c.get("id", 0) for c in clips], default=0) + 1
    
    settings = db.get("settings", {})
    auto_transcode = settings.get("autoTranscodeOnImport", True)
    target_proxy_res = settings.get("proxyResolution", "360p Proxy")

    filter_name = "Cyberpunk Neon" if style == "Cyberpunk" else "Cinematic Glow" if style == "Cinematic" else "None"
    res_tag = target_proxy_res[:4].lower() if target_proxy_res else "360p"

    new_clip = {
        "id": new_clip_id,
        "projectId": active_project_id,
        "trackId": main_track.get("id"),
        "title": f"[AI {model}] {prompt[:25]}...",
        "uri": f"https://generated.flowmonkey.ai/v/{int(time.time() * 1000)}.mp4",
        "startTimeMs": start_time_ms,
        "endTimeMs": end_time_ms,
        "durationMs": duration_ms,
        "filterName": filter_name,
        "speedMultiplier": 1.0,
        "transitionType": "Dissolve",
        "volume": 1.0,
        "isMuted": False,
        "audioFadeInSec": 0.5,
        "audioFadeOutSec": 0.5,
        "audioPitch": 1.0,
        "noiseReduction": True,
        "vocalEnhance": False,
        "proxyUri": f"proxy_{res_tag}_{new_clip_id}.mp4" if auto_transcode else "",
        "proxyStatus": "READY" if auto_transcode else "IDLE"
    }

    clips.append(new_clip)
    db["clips"] = clips

    if auto_transcode:
        job = {
            "id": f"job_{int(time.time() * 1000)}",
            "clipId": new_clip_id,
            "mediaTitle": new_clip["title"],
            "originalResolution": "1080p FHD",
            "targetResolution": target_proxy_res,
            "progressPercent": 100,
            "statusMessage": f"Proxy Low-Res Transcoded ({target_proxy_res})",
            "isCompleted": True
        }
        db.setdefault("transcodingJobs", []).append(job)

    save_db(db)

    if json_mode:
        return json.dumps({
            "success": True,
            "prompt": prompt,
            "model": model,
            "style": style,
            "ratio": ratio,
            "durationSec": duration_sec,
            "clip": new_clip
        }, indent=2)

    log = f"\n🤖 VEO AI VIDEO GENERATOR ENGINE (Python)\n"
    log += f"=========================================\n"
    log += f"Prompt    : \"{prompt}\"\n"
    log += f"Model     : {model}\n"
    log += f"Gaya      : {style}\n"
    log += f"Rasio     : {ratio} | Durasi: {duration_sec}s @ {fps} FPS\n"
    log += f"Status    : ✅ Video berhasil diproses & ditambahkan ke Timeline Track 1!\n"
    log += f"Clip ID   : {new_clip_id} (Durasi: {start_time_ms/1000}s - {end_time_ms/1000}s)\n"
    if auto_transcode:
        log += f"Proxy     : ⚡ Low-Res Proxy ({target_proxy_res}) dibuat otomatis di latar belakang!\n"
    return log
