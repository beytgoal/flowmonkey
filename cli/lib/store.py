import json
import os
from pathlib import Path
from datetime import datetime

DB_PATH = Path(__file__).parent.parent / "studio_db.json"

DEFAULT_DB = {
    "projects": [
        {
            "id": 1,
            "title": "TikTok Viral Trends 2026",
            "description": "Dynamic AI video sequence created with FlowMonkey",
            "aspectRatio": "9:16",
            "resolution": "1080p FHD",
            "createdAt": datetime.now().isoformat(),
            "updatedAt": datetime.now().isoformat()
        }
    ],
    "activeProjectId": 1,
    "tracks": [
        {"id": 1, "projectId": 1, "trackIndex": 0, "type": "VIDEO", "label": "Utama (Video Track 1)"},
        {"id": 2, "projectId": 1, "trackIndex": 1, "type": "VIDEO", "label": "Overlay / B-Roll Track"},
        {"id": 3, "projectId": 1, "trackIndex": 2, "type": "AUDIO", "label": "Musik & Efek Suara"},
        {"id": 4, "projectId": 1, "trackIndex": 3, "type": "TEXT", "label": "Subtitel & Teks AI"}
    ],
    "clips": [
        {
            "id": 1,
            "projectId": 1,
            "trackId": 1,
            "title": "Opening Cyberpunk Neon",
            "uri": "https://example.com/assets/video1.mp4",
            "startTimeMs": 0,
            "endTimeMs": 5000,
            "durationMs": 5000,
            "filterName": "Cinematic Glow",
            "speedMultiplier": 1.0,
            "speedCurve": "Hero",
            "hasKeyframe": True,
            "keyframes": [
                {"id": 1, "timeSec": 0.0, "posX": 0, "posY": 0, "scale": 1.0, "rotation": 0, "opacity": 1.0, "ease": "EaseInOut"},
                {"id": 2, "timeSec": 2.5, "posX": 40, "posY": -15, "scale": 1.2, "rotation": 8, "opacity": 1.0, "ease": "Bezier"}
            ],
            "transitionType": "Dissolve",
            "volume": 1.0,
            "isMuted": False,
            "audioFadeInSec": 0.5,
            "audioFadeOutSec": 0.5,
            "audioPitch": 1.0,
            "noiseReduction": True,
            "vocalEnhance": False,
            "proxyUri": "proxy_360p_1_1720000000000.mp4",
            "proxyStatus": "READY"
        }
    ],
    "transcodingJobs": [],
    "settings": {
        "isProxyModeEnabled": True,
        "proxyResolution": "360p Proxy",
        "autoTranscodeOnImport": True,
        "isDarkTheme": True,
        "geminiApiKey": "",
        "highfieldApiKey": "",
        "customEndpoint": "https://api.flowmonkey.ai/v1"
    },
    "exports": []
}

def get_db():
    try:
        if not DB_PATH.exists():
            save_db(DEFAULT_DB)
            return DEFAULT_DB
        with open(DB_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return DEFAULT_DB

def save_db(data):
    try:
        with open(DB_PATH, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
    except Exception as e:
        print(f"Error writing database: {e}")
