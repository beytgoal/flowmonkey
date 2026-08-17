import json
from lib.store import get_db, save_db

DEFAULT_MODELS = [
    {
        "id": "face_mesh_int8",
        "name": "MediaPipe Face Mesh (Beauty Retouch)",
        "approxSizeBytes": 2800000,
        "sizeDisplay": "2.8 MB",
        "status": "CACHED_ON_DISK",
        "memoryUsageMb": 0.0,
        "description": "468 landmark face geometry untuk penghalusan kulit & lighting"
    },
    {
        "id": "pose_tracker_int8",
        "name": "MediaPipe Body Pose (Silhouette VFX)",
        "approxSizeBytes": 3400000,
        "sizeDisplay": "3.4 MB",
        "status": "LOADED_IN_GPU_RAM",
        "memoryUsageMb": 3.4,
        "description": "33 3D body keypoints untuk kinetic glow & silhouette vfx"
    },
    {
        "id": "bg_segmenter_int8",
        "name": "AI Background Cutout (Matting)",
        "approxSizeBytes": 2100000,
        "sizeDisplay": "2.1 MB",
        "status": "CACHED_ON_DISK",
        "memoryUsageMb": 0.0,
        "description": "High-precision human matting & background removal"
    },
    {
        "id": "hand_gesture_tracker",
        "name": "MediaPipe Hand Gesture Tracker",
        "approxSizeBytes": 1900000,
        "sizeDisplay": "1.9 MB",
        "status": "NOT_DOWNLOADED",
        "memoryUsageMb": 0.0,
        "description": "21 keypoint hand tracking untuk stiker interaktif"
    },
    {
        "id": "object_detector_int8",
        "name": "AI Smart Object Tracker",
        "approxSizeBytes": 3800000,
        "sizeDisplay": "3.8 MB",
        "status": "NOT_DOWNLOADED",
        "memoryUsageMb": 0.0,
        "description": "Real-time bounding box tracking untuk pinning teks & visual stickers"
    }
]

def list_ai_models(json_mode=False):
    db = get_db()
    if 'aiModels' not in db or not db['aiModels']:
        db['aiModels'] = DEFAULT_MODELS
        save_db(db)

    models = db['aiModels']
    if json_mode:
        return json.dumps({'models': models}, indent=2)

    output = "\n🧠 DYNAMIC AI MODEL LIFECYCLE (DYNAMIC MODEL LOADING)\n======================================================\n"
    for idx, m in enumerate(models):
        status_tag = '🟢 LOADED IN GPU' if m.get('status') == 'LOADED_IN_GPU_RAM' else ('💾 DISK CACHE' if m.get('status') == 'CACHED_ON_DISK' else '⚪ NOT DOWNLOADED')
        output += f"{idx + 1}. [{m.get('id')}] {m.get('name')}\n"
        output += f"   Status       : {status_tag} ({m.get('sizeDisplay')}) | RAM: {m.get('memoryUsageMb', 0):.1f} MB\n"
        output += f"   Deskripsi    : {m.get('description')}\n\n"
    output += "======================================================\n"
    return output

def download_ai_model(model_id, json_mode=False):
    db = get_db()
    if 'aiModels' not in db:
        db['aiModels'] = DEFAULT_MODELS

    model = next((m for m in db['aiModels'] if m.get('id', '').lower() == model_id.lower()), None)
    if not model:
        err = f"Model AI dengan ID '{model_id}' tidak ditemukan."
        if json_mode:
            return json.dumps({'success': False, 'error': err}, indent=2)
        return f"❌ {err}"

    model['status'] = "CACHED_ON_DISK"
    save_db(db)

    if json_mode:
        return json.dumps({
            'success': True,
            'modelId': model['id'],
            'name': model['name'],
            'status': model['status'],
            'size': model['sizeDisplay']
        }, indent=2)

    return f"✅ Model '{model['name']}' ({model['sizeDisplay']}) berhasil diunduh on-demand ke disk cache lokal."

def load_model_to_gpu(model_id, json_mode=False):
    db = get_db()
    if 'aiModels' not in db:
        db['aiModels'] = DEFAULT_MODELS

    model = next((m for m in db['aiModels'] if m.get('id', '').lower() == model_id.lower()), None)
    if not model:
        err = f"Model AI dengan ID '{model_id}' tidak ditemukan."
        if json_mode:
            return json.dumps({'success': False, 'error': err}, indent=2)
        return f"❌ {err}"

    model['status'] = "LOADED_IN_GPU_RAM"
    model['memoryUsageMb'] = model.get('approxSizeBytes', 3000000) / (1024 * 1024)
    save_db(db)

    if json_mode:
        return json.dumps({
            'success': True,
            'modelId': model['id'],
            'name': model['name'],
            'status': model['status'],
            'memoryUsageMb': round(model['memoryUsageMb'], 2)
        }, indent=2)

    return f"🚀 Model '{model['name']}' berhasil dimuat ke GPU/NPU RAM ({model['memoryUsageMb']:.1f} MB). Siap untuk inferensi real-time."

def unload_model_from_gpu(model_id, json_mode=False):
    db = get_db()
    if 'aiModels' not in db:
        db['aiModels'] = DEFAULT_MODELS

    model = next((m for m in db['aiModels'] if m.get('id', '').lower() == model_id.lower()), None)
    if not model:
        err = f"Model AI dengan ID '{model_id}' tidak ditemukan."
        if json_mode:
            return json.dumps({'success': False, 'error': err}, indent=2)
        return f"❌ {err}"

    model['status'] = "CACHED_ON_DISK"
    model['memoryUsageMb'] = 0.0
    save_db(db)

    if json_mode:
        return json.dumps({
            'success': True,
            'modelId': model['id'],
            'name': model['name'],
            'status': model['status']
        }, indent=2)

    return f"💤 Model '{model['name']}' berhasil dilepaskan dari memori RAM (RAM dibebaskan)."

def purge_ai_model_caches(json_mode=False):
    db = get_db()
    db['aiModels'] = [{
        **m,
        'status': "NOT_DOWNLOADED",
        'memoryUsageMb': 0.0
    } for m in DEFAULT_MODELS]
    save_db(db)

    if json_mode:
        return json.dumps({
            'success': True,
            'message': 'Semua cache file model AI berhasil dibersihkan dari penyimpanan.'
        }, indent=2)

    return "🗑️ Semua cache file model AI (~14 MB) berhasil dibersihkan dari penyimpanan."
