import json
import time
from datetime import datetime
from .store import get_db, save_db

def list_projects(json_mode=False):
    db = get_db()
    if json_mode:
        return json.dumps({"activeProjectId": db.get("activeProjectId", 1), "projects": db.get("projects", [])}, indent=2)
    
    active_id = db.get("activeProjectId", 1)
    output = f"\n📁 DAFTAR PROYEK VIDEO STUDIO (Active ID: {active_id})\n"
    output += "=======================================================\n"
    for p in db.get("projects", []):
        is_active = " [AKTIF]" if p.get("id") == active_id else ""
        created = p.get("createdAt", "")[:10]
        output += f"ID: {p.get('id')}{is_active} | \"{p.get('title')}\" | Aspect: {p.get('aspectRatio')} | Res: {p.get('resolution')} | Created: {created}\n"
    return output

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
