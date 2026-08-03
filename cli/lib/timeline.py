import json
from .store import get_db, save_db

FILTERS = ["None", "Cinematic Glow", "Cyberpunk Neon", "Vintage Film", "Black & White", "Warm Sunset", "Anamorphic Flare", "Dramatic Contrast"]
TRANSITIONS = ["None", "Dissolve", "Wipe Left", "Wipe Right", "Zoom In", "Fade Black"]
SPEED_CURVES = ["Normal", "Hero", "Bullet Time", "Montage", "Fast Out", "Slow In", "Custom Curve"]

def view_timeline(json_mode=False):
    db = get_db()
    active_project_id = db.get("activeProjectId")
    projects = db.get("projects", [])
    project = next((p for p in projects if p.get("id") == active_project_id), None)
    
    if not project:
        if json_mode:
            return json.dumps({"error": "No active project"})
        return "❌ Error: Tidak ada proyek aktif."

    tracks = [t for t in db.get("tracks", []) if t.get("projectId") == active_project_id]
    clips = [c for c in db.get("clips", []) if c.get("projectId") == active_project_id]
    settings = db.get("settings", {})
    proxy_mode = settings.get("isProxyModeEnabled", True)
    proxy_res = settings.get("proxyResolution", "360p Proxy")

    if json_mode:
        return json.dumps({
            "project": project,
            "proxyMode": {"isEnabled": proxy_mode, "resolution": proxy_res},
            "tracks": tracks,
            "clips": clips
        }, indent=2)

    out = f"\n🎬 TIMELINE MULTI-TRACK EDITOR [Proyek: \"{project.get('title')}\"] (Python)\n"
    out += f"=========================================================\n"
    out += f"⚡ Mode Pratinjau : {'PROXY LOW-RES (' + proxy_res + ') [Beban GPU minimal]' if proxy_mode else '1080p FULL-RES ORIGINAL'}\n\n"

    for track in tracks:
        track_clips = [c for c in clips if c.get("trackId") == track.get("id")]
        out += f"Track {track.get('trackIndex', 0) + 1} ({track.get('type')}) - \"{track.get('label')}\":\n"
        if not track_clips:
            out += "   (Kosong)\n"
        else:
            for c in track_clips:
                start_sec = f"{c.get('startTimeMs', 0) / 1000:.1f}"
                end_sec = f"{c.get('endTimeMs', 0) / 1000:.1f}"
                dur_sec = f"{c.get('durationMs', 0) / 1000:.1f}"
                kfs = c.get("keyframes", [])
                kf_count = len(kfs) if kfs else (1 if c.get("hasKeyframe") else 0)
                curve_info = f" [Curve: {c.get('speedCurve')}]" if c.get("speedCurve") and c.get("speedCurve") != "Normal" else ""
                out += f"   [ID: {c.get('id')}] \"{c.get('title')}\" | {start_sec}s -> {end_sec}s ({dur_sec}s) | Speed: {c.get('speedMultiplier', 1.0)}x{curve_info} | Filter: {c.get('filterName', 'None')} | Keyframes: {kf_count} | Proxy: {c.get('proxyStatus', 'IDLE')}\n"
        out += "\n"

    return out

def add_clip(options):
    title = options.get("title", "Klip Media Baru")
    duration_sec = float(options.get("durationSec", 5))
    track_index = int(options.get("trackIndex", 0))
    json_mode = options.get("jsonMode", False)

    db = get_db()
    active_project_id = db.get("activeProjectId")
    if not active_project_id:
        if json_mode:
            return json.dumps({"error": "No active project"})
        return "❌ Error: Tidak ada proyek aktif."

    tracks = [t for t in db.get("tracks", []) if t.get("projectId") == active_project_id and t.get("trackIndex") == track_index]
    track = tracks[0] if tracks else (db.get("tracks", [{}])[0])

    existing_clips = [c for c in db.get("clips", []) if c.get("projectId") == active_project_id and c.get("trackId") == track.get("id")]
    start_time_ms = max([c.get("endTimeMs", 0) for c in existing_clips], default=0)
    duration_ms = int(duration_sec * 1000)
    end_time_ms = start_time_ms + duration_ms

    clips = db.get("clips", [])
    new_clip_id = max([c.get("id", 0) for c in clips], default=0) + 1
    settings = db.get("settings", {})
    auto_transcode = settings.get("autoTranscodeOnImport", True)
    proxy_res = settings.get("proxyResolution", "360p Proxy")

    safe_title = title.lower().replace(' ', '_')
    new_clip = {
        "id": new_clip_id,
        "projectId": active_project_id,
        "trackId": track.get("id"),
        "title": title,
        "uri": f"file:///sdcard/Movies/{safe_title}.mp4",
        "startTimeMs": start_time_ms,
        "endTimeMs": end_time_ms,
        "durationMs": duration_ms,
        "filterName": "None",
        "speedMultiplier": 1.0,
        "transitionType": "None",
        "volume": 1.0,
        "isMuted": False,
        "audioFadeInSec": 0.0,
        "audioFadeOutSec": 0.0,
        "audioPitch": 1.0,
        "noiseReduction": False,
        "vocalEnhance": False,
        "proxyUri": f"proxy_{new_clip_id}.mp4" if auto_transcode else "",
        "proxyStatus": "READY" if auto_transcode else "IDLE"
    }

    clips.append(new_clip)
    db["clips"] = clips

    if auto_transcode:
        db.setdefault("transcodingJobs", []).append({
            "id": f"job_{new_clip_id}",
            "clipId": new_clip_id,
            "mediaTitle": title,
            "originalResolution": "1080p FHD",
            "targetResolution": proxy_res,
            "progressPercent": 100,
            "statusMessage": f"Proxy Transcoded ({proxy_res})",
            "isCompleted": True
        })

    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "clip": new_clip}, indent=2)
    return f"\n✅ Klip berhasil ditambahkan ke Track {track.get('trackIndex', 0) + 1}: ID {new_clip_id} - \"{title}\" ({duration_sec}s)\n"

def update_filter(clip_id, filter_name, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    clip["filterName"] = filter_name
    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "clipId": clip["id"], "filterName": filter_name}, indent=2)
    return f"\n🎨 Filter visual klip ID {clip_id} diubah menjadi: \"{filter_name}\"\n"

def update_speed(clip_id, speed_multiplier, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
        speed = float(speed_multiplier)
    except (ValueError, TypeError):
        cid = -1
        speed = 1.0

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    clip["speedMultiplier"] = speed
    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "clipId": clip["id"], "speedMultiplier": speed}, indent=2)
    return f"\n⚡ Kecepatan pemutaran klip ID {clip_id} diubah ke: {speed}x\n"

def split_clip(clip_id, split_time_sec, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
        split_sec = float(split_time_sec)
    except (ValueError, TypeError):
        cid = -1
        split_sec = 0.0

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    split_time_ms = clip["startTimeMs"] + int(split_sec * 1000)
    if split_time_ms <= clip["startTimeMs"] or split_time_ms >= clip["endTimeMs"]:
        if json_mode:
            return json.dumps({"error": "Split time out of clip bounds"})
        return f"❌ Error: Posisi pemotongan harus berada di antara {clip['startTimeMs']/1000}s dan {clip['endTimeMs']/1000}s."

    old_end_ms = clip["endTimeMs"]
    clip["endTimeMs"] = split_time_ms
    clip["durationMs"] = split_time_ms - clip["startTimeMs"]

    clips = db.get("clips", [])
    new_clip_id = max([c.get("id", 0) for c in clips], default=0) + 1
    new_clip = dict(clip)
    new_clip["id"] = new_clip_id
    new_clip["title"] = f"{clip['title']} (Part 2)"
    new_clip["startTimeMs"] = split_time_ms
    new_clip["endTimeMs"] = old_end_ms
    new_clip["durationMs"] = old_end_ms - split_time_ms

    clips.append(new_clip)
    db["clips"] = clips
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "clip1": clip, "clip2": new_clip}, indent=2)
    return f"\n✂️ Klip ID {clip_id} berhasil dipotong di detik ke-{split_sec}s! Terbagi menjadi ID {clip_id} dan ID {new_clip_id}.\n"

def update_audio(clip_id, options):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if options.get("jsonMode"):
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    if "volume" in options and options["volume"] is not None:
        clip["volume"] = float(options["volume"])
    if "isMuted" in options and options["isMuted"] is not None:
        clip["isMuted"] = bool(options["isMuted"])
    if "noiseReduction" in options and options["noiseReduction"] is not None:
        clip["noiseReduction"] = bool(options["noiseReduction"])
    if "vocalEnhance" in options and options["vocalEnhance"] is not None:
        clip["vocalEnhance"] = bool(options["vocalEnhance"])

    save_db(db)
    if options.get("jsonMode"):
        return json.dumps({"success": True, "clip": clip}, indent=2)
    return f"\n🎵 Audio klip ID {clip_id} diperbarui (Vol: {clip.get('volume', 1.0) * 100}%, Mute: {clip.get('isMuted')}, NoiseRed: {clip.get('noiseReduction')})\n"

def delete_clip(clip_id, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    exists = any(c.get("id") == cid for c in db.get("clips", []))
    if not exists:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    db["clips"] = [c for c in db.get("clips", []) if c.get("id") != cid]
    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "deletedClipId": cid})
    return f"\n🗑️ Klip ID {cid} berhasil dihapus dari timeline.\n"

def set_speed_curve(clip_id, curve_name, multiplier=1.0, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    clip["speedCurve"] = curve_name or "Hero"
    if multiplier is not None:
        clip["speedMultiplier"] = float(multiplier)

    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "clipId": clip["id"], "speedCurve": clip["speedCurve"], "speedMultiplier": clip["speedMultiplier"]}, indent=2)
    return f"\n📈 Speed Ramping Curve klip ID {clip_id} diatur ke: \"{clip['speedCurve']}\" (Speed Base: {clip['speedMultiplier']}x)\n"

def add_keyframe(clip_id, options):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if options.get("jsonMode"):
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    kfs = clip.setdefault("keyframes", [])
    kf_id = max([k.get("id", 0) for k in kfs], default=0) + 1

    time_sec = float(options.get("timeSec", 0.0))
    pos_x = float(options.get("posX", 0))
    pos_y = float(options.get("posY", 0))
    scale = float(options.get("scale", 1.0))
    rotation = float(options.get("rotation", 0))
    opacity = float(options.get("opacity", 1.0))
    ease = options.get("ease", "EaseInOut")

    new_kf = {
        "id": kf_id,
        "timeSec": time_sec,
        "posX": pos_x,
        "posY": pos_y,
        "scale": scale,
        "rotation": rotation,
        "opacity": opacity,
        "ease": ease
    }

    kfs.append(new_kf)
    clip["hasKeyframe"] = True
    save_db(db)

    if options.get("jsonMode"):
        return json.dumps({"success": True, "clipId": clip["id"], "keyframe": new_kf}, indent=2)
    return f"\n💎 Keyframe baru ditambahkan ke klip ID {clip_id} pada t={time_sec}s [X:{pos_x}, Y:{pos_y}, Scale:{scale}, Rot:{rotation}°, Opacity:{opacity}, Ease:{ease}]\n"

def list_keyframes(clip_id, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    kfs = clip.get("keyframes", [])
    if json_mode:
        return json.dumps({"clipId": clip["id"], "hasKeyframe": clip.get("hasKeyframe", False), "keyframes": kfs}, indent=2)

    out = f"\n💎 LIST KEYFRAMES [Klip ID {clip['id']} - \"{clip['title']}\"]:\n"
    out += f"=========================================================\n"
    if not kfs:
        out += "   (Belum ada keyframe pada klip ini)\n"
    else:
        for k in kfs:
            out += f"   - Keyframe ID {k.get('id')} @ t={k.get('timeSec')}s | Pos: ({k.get('posX')}, {k.get('posY')}) | Scale: {k.get('scale')}x | Rot: {k.get('rotation')}° | Opacity: {k.get('opacity')} | Ease: {k.get('ease')}\n"
    return out

def remove_keyframe(clip_id, keyframe_id, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
        kfid = int(keyframe_id)
    except (ValueError, TypeError):
        cid = -1
        kfid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    kfs = [k for k in clip.get("keyframes", []) if k.get("id") != kfid]
    clip["keyframes"] = kfs
    clip["hasKeyframe"] = len(kfs) > 0
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "clipId": clip["id"], "removedKeyframeId": kfid}, indent=2)
    return f"\n🗑️ Keyframe ID {kfid} dihapus dari klip ID {clip_id}.\n"

def clear_keyframes(clip_id, json_mode=False):
    db = get_db()
    try:
        cid = int(clip_id)
    except (ValueError, TypeError):
        cid = -1

    clip = next((c for c in db.get("clips", []) if c.get("id") == cid), None)
    if not clip:
        if json_mode:
            return json.dumps({"error": "Clip not found"})
        return f"❌ Error: Klip ID {clip_id} tidak ditemukan."

    clip["keyframes"] = []
    clip["hasKeyframe"] = False
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "clipId": clip["id"]}, indent=2)
    return f"\n🗑️ Semua keyframe dibersihkan dari klip ID {clip_id}.\n"
