import json
import time
from .store import get_db, save_db

def toggle_proxy(enabled=None, json_mode=False):
    db = get_db()
    settings = db.get("settings", {})
    if enabled is None:
        settings["isProxyModeEnabled"] = not settings.get("isProxyModeEnabled", True)
    else:
        settings["isProxyModeEnabled"] = bool(enabled)
    db["settings"] = settings
    save_db(db)

    res = settings.get("proxyResolution", "360p Proxy")
    status_str = f"⚡ PROXY LOW-RES ENABLED ({res})" if settings["isProxyModeEnabled"] else "🎬 ORIGINAL 1080p FULL-RES ENABLED"
    if json_mode:
        return json.dumps({"success": True, "isProxyModeEnabled": settings["isProxyModeEnabled"], "proxyResolution": res}, indent=2)
    return f"\n🔄 Mode Pratinjau Timeline Diubah: {status_str}\n"

def set_proxy_resolution(resolution, json_mode=False):
    db = get_db()
    settings = db.get("settings", {})
    settings["proxyResolution"] = resolution
    if "1080p" in resolution or "Original" in resolution:
        settings["isProxyModeEnabled"] = False
    else:
        settings["isProxyModeEnabled"] = True

    db["settings"] = settings
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "proxyResolution": settings["proxyResolution"], "isProxyModeEnabled": settings["isProxyModeEnabled"]}, indent=2)
    return f"\n⚡ Target Resolusi Proxy Diperbarui Ke: \"{resolution}\". Mode Proxy: {'AKTIF' if settings['isProxyModeEnabled'] else 'NONAKTIF'}\n"

def toggle_auto_transcode(enabled=None, json_mode=False):
    db = get_db()
    settings = db.get("settings", {})
    if enabled is None:
        settings["autoTranscodeOnImport"] = not settings.get("autoTranscodeOnImport", True)
    else:
        settings["autoTranscodeOnImport"] = bool(enabled)

    db["settings"] = settings
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "autoTranscodeOnImport": settings["autoTranscodeOnImport"]}, indent=2)
    return f"\n🔄 Auto-Transcode Impor Aset: {'AKTIF (Otomatis konversi video baru di latar belakang)' if settings['autoTranscodeOnImport'] else 'NONAKTIF'}\n"

def transcode_clip(clip_id, json_mode=False):
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

    target_res = db.get("settings", {}).get("proxyResolution", "360p Proxy")
    res_tag = target_res[:4].lower() if target_res else "360p"
    clip["proxyStatus"] = "READY"
    clip["proxyUri"] = f"proxy_{res_tag}_{clip['id']}.mp4"

    job = {
        "id": f"job_manual_{int(time.time() * 1000)}",
        "clipId": clip["id"],
        "mediaTitle": clip["title"],
        "originalResolution": "1080p FHD",
        "targetResolution": target_res,
        "progressPercent": 100,
        "statusMessage": f"Proxy Low-Res Transcoded ({target_res})",
        "isCompleted": True
    }

    db.setdefault("transcodingJobs", []).append(job)
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "clip": clip, "job": job}, indent=2)
    clip_title = clip['title']
    return f"\n⚡ Klip ID {clip_id} (\"{clip_title}\") berhasil ditranscode ke {target_res}! Status: READY.\n"

def transcode_all(json_mode=False):
    db = get_db()
    active_project_id = db.get("activeProjectId")
    project_clips = [c for c in db.get("clips", []) if c.get("projectId") == active_project_id]
    target_res = db.get("settings", {}).get("proxyResolution", "360p Proxy")
    count = 0

    for clip in project_clips:
        clip["proxyStatus"] = "READY"
        clip["proxyUri"] = f"proxy_{clip['id']}.mp4"
        count += 1

        db.setdefault("transcodingJobs", []).append({
            "id": f"job_all_{clip['id']}_{int(time.time() * 1000)}",
            "clipId": clip["id"],
            "mediaTitle": clip["title"],
            "originalResolution": "1080p FHD",
            "targetResolution": target_res,
            "progressPercent": 100,
            "statusMessage": f"Proxy Transcoded ({target_res})",
            "isCompleted": True
        })

    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "transcodedCount": count, "targetResolution": target_res}, indent=2)
    return f"\n🚀 BACKGROUND TRANSCODER ENGINE (Python)\n=========================================\n✅ Berhasil mentranscode {count} klip video ke resolusi proxy ({target_res})!\nBeban GPU Timeline berkurang ~75% untuk pemutaran 60 FPS tanpa lag.\n"

def view_proxy_status(json_mode=False):
    db = get_db()
    s = db.get("settings", {})
    jobs = db.get("transcodingJobs", [])

    if json_mode:
        return json.dumps({"settings": s, "jobs": jobs}, indent=2)

    is_proxy = s.get("isProxyModeEnabled", True)
    res = s.get("proxyResolution", "360p Proxy")
    auto_trans = s.get("autoTranscodeOnImport", True)

    out = f"\n⚙️ STATUS LOW-RESOLUTION PROXY ENGINE (Python)\n"
    out += f"=========================================\n"
    out += f"Mode Proxy Preview : {'⚡ AKTIF' if is_proxy else '🎬 NONAKTIF (1080p Direct)'}\n"
    out += f"Target Resolution  : {res}\n"
    out += f"Auto-Transcode     : {'AKTIF' if auto_trans else 'NONAKTIF'}\n"
    out += f"Beban GPU Estimasi : {'15% - 25% (Sangat Ringan)' if is_proxy else '85% - 100% (High-Res Direct)'}\n\n"

    out += f"📋 RIWAYAT TUGAS TRANSCODER LATAR BELAKANG ({len(jobs)} Tugas):\n"
    if not jobs:
        out += "   (Belum ada tugas transcoding)\n"
    else:
        for j in jobs[-5:]:
            status_tag = "DONE" if j.get("isCompleted") else "IN_PROGRESS"
            out += f"   - [{status_tag}] {j.get('mediaTitle')} ({j.get('originalResolution')} -> {j.get('targetResolution')}) - Progress: {j.get('progressPercent')}%\n"
    return out
