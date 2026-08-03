import json
import time
from datetime import datetime
from .store import get_db, save_db

def list_projects(json_mode=False):
    db = get_db()
    all_p = db.get("projects", [])
    user_projects = [p for p in all_p if not p.get("isTemplate")]
    templates = [p for p in all_p if p.get("isTemplate")]
    active_id = db.get("activeProjectId", 1)

    if json_mode:
        return json.dumps({
            "activeProjectId": active_id,
            "projects": user_projects,
            "templates": templates
        }, indent=2)
    
    output = f"\n📁 TAB PROYEK AKTIF (Active ID: {active_id})\n"
    output += "=======================================================\n"
    if not user_projects:
        output += "(Belum ada proyek aktif. Gunakan 'project create' atau 'template use')\n"
    else:
        for p in user_projects:
            is_active = " [AKTIF]" if p.get("id") == active_id else ""
            created = p.get("createdAt", "")[:10]
            output += f"ID: {p.get('id')}{is_active} | \"{p.get('title')}\" | Aspect: {p.get('aspectRatio')} | Res: {p.get('resolution')} | Created: {created}\n"

    output += f"\n🔖 TAB TEMPLATE (FULL TOOLS & PLACEHOLDERS: {len(templates)})\n"
    output += "=======================================================\n"
    if not templates:
        output += "(Belum ada template. Buat dengan 'project template-create <id>')\n"
    else:
        for t in templates:
            created = t.get("createdAt", "")[:10]
            output += f"Template ID: {t.get('id')} | \"{t.get('title')}\" | Aspect: {t.get('aspectRatio')} | Tools: Multi-Track, Curve 2.0x, Keyframes, Subtitle | Created: {created}\n"

    return output

def create_template(proj_id, json_mode=False):
    db = get_db()
    try:
        pid = int(proj_id)
    except (ValueError, TypeError):
        pid = -1

    source = next((p for p in db.get("projects", []) if p.get("id") == pid), None)
    if not source:
        if json_mode:
            return json.dumps({"error": "Source project not found"})
        return f"❌ Error: Proyek ID {proj_id} tidak ditemukan."

    projects = db.get("projects", [])
    new_template_id = max([p.get("id", 0) for p in projects], default=0) + 1
    tmpl_title = source.get("title") if source.get("title", "").startswith("Template -") else f"Template - {source.get('title')}"

    new_template = {
        "id": new_template_id,
        "title": tmpl_title,
        "description": f"Template full tools preset dari proyek {source.get('title')}",
        "aspectRatio": source.get("aspectRatio", "9:16"),
        "resolution": source.get("resolution", "1080p FHD"),
        "isTemplate": True,
        "createdAt": datetime.now().isoformat(),
        "updatedAt": datetime.now().isoformat()
    }
    projects.append(new_template)

    # Copy tracks and clips with placeholder labels
    ts = int(time.time() * 1000)
    source_tracks = [t for t in db.get("tracks", []) if t.get("projectId") == pid]
    source_clips = [c for c in db.get("clips", []) if c.get("projectId") == pid]

    track_map = {}
    new_tracks = []
    for idx, trk in enumerate(source_tracks):
        new_tr_id = ts + idx + 1
        track_map[trk.get("id")] = new_tr_id
        label_clean = trk.get("label", "").replace("Trek ", "").replace("Trek", "").strip()
        new_tracks.append({
            "id": new_tr_id,
            "projectId": new_template_id,
            "trackIndex": trk.get("trackIndex", idx),
            "type": trk.get("type", "VIDEO"),
            "label": label_clean if label_clean else trk.get("type")
        })

    new_clips = []
    for idx, clp in enumerate(source_clips):
        new_clp_id = ts + 100 + idx + 1
        target_tr_id = track_map.get(clp.get("trackId"), new_tracks[0]["id"] if new_tracks else 0)
        clip_title = clp.get("title", "")
        if "Placeholder" not in clip_title:
            clip_title = f"Placeholder - {clip_title}"

        clp_copy = dict(clp)
        clp_copy["id"] = new_clp_id
        clp_copy["projectId"] = new_template_id
        clp_copy["trackId"] = target_tr_id
        clp_copy["title"] = clip_title
        new_clips.append(clp_copy)

    db["projects"] = projects
    db["tracks"] = db.get("tracks", []) + new_tracks
    db["clips"] = db.get("clips", []) + new_clips
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "template": new_template}, indent=2)
    return f"\n🔖 Berhasil membuat Template Full Tools dari proyek ID {pid}: Template ID {new_template_id} - \"{tmpl_title}\"\n"

def create_project_from_template(template_id, custom_title="", gallery_media="", json_mode=False):
    db = get_db()
    try:
        tid = int(template_id)
    except (ValueError, TypeError):
        tid = -1

    tmpl = next((p for p in db.get("projects", []) if p.get("id") == tid and p.get("isTemplate")), None)
    if not tmpl:
        if json_mode:
            return json.dumps({"error": "Template not found"})
        return f"❌ Error: Template ID {template_id} tidak ditemukan."

    projects = db.get("projects", [])
    new_id = max([p.get("id", 0) for p in projects], default=0) + 1
    
    title_final = custom_title if custom_title else tmpl.get("title", "").replace("Template - ", "")
    if gallery_media:
        title_final += f" ({gallery_media.split('.')[0]})"

    new_project = {
        "id": new_id,
        "title": title_final,
        "description": f"Proyek dibuat dari template {tmpl.get('title')}",
        "aspectRatio": tmpl.get("aspectRatio", "9:16"),
        "resolution": tmpl.get("resolution", "1080p FHD"),
        "isTemplate": False,
        "createdAt": datetime.now().isoformat(),
        "updatedAt": datetime.now().isoformat()
    }
    projects.append(new_project)
    db["activeProjectId"] = new_id

    # Copy template tracks & clips
    ts = int(time.time() * 1000)
    tmpl_tracks = [t for t in db.get("tracks", []) if t.get("projectId") == tid]
    tmpl_clips = [c for c in db.get("clips", []) if c.get("projectId") == tid]

    track_map = {}
    new_tracks = []
    for idx, trk in enumerate(tmpl_tracks):
        new_tr_id = ts + idx + 1
        track_map[trk.get("id")] = new_tr_id
        new_tracks.append({
            "id": new_tr_id,
            "projectId": new_id,
            "trackIndex": trk.get("trackIndex", idx),
            "type": trk.get("type", "VIDEO"),
            "label": trk.get("label", "")
        })

    new_clips = []
    for idx, clp in enumerate(tmpl_clips):
        new_clp_id = ts + 100 + idx + 1
        target_tr_id = track_map.get(clp.get("trackId"), new_tracks[0]["id"] if new_tracks else 0)
        clip_title = clp.get("title", "").replace("Placeholder - ", "")
        clip_uri = clp.get("uri", "")

        if idx == 0 and gallery_media:
            clip_title = f"Media Galeri: {gallery_media}"
            clip_uri = f"file:///sdcard/Gallery/{gallery_media}"

        clp_copy = dict(clp)
        clp_copy["id"] = new_clp_id
        clp_copy["projectId"] = new_id
        clp_copy["trackId"] = target_tr_id
        clp_copy["title"] = clip_title
        clp_copy["uri"] = clip_uri
        new_clips.append(clp_copy)

    db["projects"] = projects
    db["tracks"] = db.get("tracks", []) + new_tracks
    db["clips"] = db.get("clips", []) + new_clips
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "project": new_project}, indent=2)
    return f"\n🚀 Proyek baru berhasil dibuat dari Template! ID {new_id} - \"{title_final}\" (Proyek Aktif)\n"

def create_project(title, description="Dibuat via FlowMonkey CLI", aspect_ratio="9:16", resolution="1080p FHD", json_mode=False):
    db = get_db()
    projects = db.get("projects", [])
    new_id = max([p.get("id", 0) for p in projects], default=0) + 1
    new_project = {
        "id": new_id,
        "title": title,
        "description": description,
        "aspectRatio": aspect_ratio,
        "resolution": resolution,
        "createdAt": datetime.now().isoformat(),
        "updatedAt": datetime.now().isoformat()
    }
    projects.append(new_project)
    db["projects"] = projects
    db["activeProjectId"] = new_id

    # Initialize tracks
    ts = int(time.time() * 1000)
    new_tracks = [
        {"id": ts + 1, "projectId": new_id, "trackIndex": 0, "type": "VIDEO", "label": "Utama (Video Track 1)"},
        {"id": ts + 2, "projectId": new_id, "trackIndex": 1, "type": "VIDEO", "label": "Overlay Track"},
        {"id": ts + 3, "projectId": new_id, "trackIndex": 2, "type": "AUDIO", "label": "Musik & Efek Suara"},
        {"id": ts + 4, "projectId": new_id, "trackIndex": 3, "type": "TEXT", "label": "Subtitel AI"}
    ]
    db["tracks"] = db.get("tracks", []) + new_tracks
    save_db(db)

    if json_mode:
        return json.dumps({"success": True, "project": new_project}, indent=2)
    return f"\n✅ Proyek baru berhasil dibuat dan diaktifkan: ID {new_id} - \"{title}\" ({aspect_ratio}, {resolution})\n"

def select_project(proj_id, json_mode=False):
    db = get_db()
    try:
        pid = int(proj_id)
    except (ValueError, TypeError):
        pid = -1
    
    target = next((p for p in db.get("projects", []) if p.get("id") == pid), None)
    if not target:
        if json_mode:
            return json.dumps({"error": "Project not found"})
        return f"❌ Error: Proyek dengan ID {proj_id} tidak ditemukan."
    
    db["activeProjectId"] = target["id"]
    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "activeProjectId": target["id"]})
    return f"\n✅ Proyek aktif diubah ke: ID {target['id']} - \"{target['title']}\"\n"

def delete_project(proj_id, json_mode=False):
    db = get_db()
    try:
        pid = int(proj_id)
    except (ValueError, TypeError):
        pid = -1

    target = next((p for p in db.get("projects", []) if p.get("id") == pid), None)
    if not target:
        if json_mode:
            return json.dumps({"error": "Project not found"})
        return f"❌ Error: Proyek ID {proj_id} tidak ditemukan."

    db["projects"] = [p for p in db.get("projects", []) if p.get("id") != pid]
    db["tracks"] = [t for t in db.get("tracks", []) if t.get("projectId") != pid]
    db["clips"] = [c for c in db.get("clips", []) if c.get("projectId") != pid]

    if db.get("activeProjectId") == pid:
        remaining = db.get("projects", [])
        db["activeProjectId"] = remaining[0]["id"] if remaining else 0

    save_db(db)
    if json_mode:
        return json.dumps({"success": True, "deletedId": pid, "newActiveId": db.get("activeProjectId")})
    title_str = target['title']
    return f"\n🗑️ Proyek ID {pid} (\"{title_str}\") berhasil dihapus.\n"
