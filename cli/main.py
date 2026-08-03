#!/usr/bin/env python3
import sys
import argparse
import shlex
from lib.projects import list_projects, create_project, select_project, delete_project, create_template, create_project_from_template
from lib.generate import generate_ai_video
from lib.storyboard import list_templates, compile_storyboard_to_timeline
from lib.timeline import view_timeline, add_clip, update_filter, update_speed, set_speed_curve, add_keyframe, list_keyframes, remove_keyframe, clear_keyframes, split_clip, update_audio, delete_clip
from lib.proxy import toggle_proxy, set_proxy_resolution, toggle_auto_transcode, transcode_clip, transcode_all, view_proxy_status
from lib.export import export_video, list_exports
from lib.settings import view_settings, update_settings
from lib.agent import run_agent_task

def print_help():
    print("""
🎬 FLOWMONKEY VIDEO STUDIO PYTHON CLI (AI AGENT OPERABLE ENGINE v2.1)
========================================================================
PENGGUNAAN:
  python3 cli/main.py <command> [subcommand] [flags]
  python3 cli/main.py repl   (Masuk ke mode interaktif REPL untuk AI Agent)

PERINTAH UTAMA:

1. PROYEK & TEMPLATE (PROJECT & TEMPLATES)
   python3 cli/main.py project list                        List semua proyek video & template
   python3 cli/main.py project create "<Title>" [ratio]    Buat proyek baru
   python3 cli/main.py project select <Id>                 Pilih proyek aktif
   python3 cli/main.py project delete <Id>                 Hapus proyek
   python3 cli/main.py project template-create <Id>        Jadikan proyek terpilih sebagai Template Full Tools
   python3 cli/main.py project template-use <TmplId> [Title] [--media <file.mp4>] Buat proyek baru dari Template

2. AI VIDEO GENERATOR (VEO & HIGHFIELD)
   python3 cli/main.py generate --prompt "<text>" [--style Cinematic|Cyberpunk] [--model "Veo 2"] [--duration 5]

3. STORYBOARD ENGINE
   python3 cli/main.py storyboard list                     Lihat daftar template storyboard
   python3 cli/main.py storyboard compile "<TemplateName>" Kompilasi adegan storyboard ke timeline

4. MULTI-TRACK TIMELINE EDITOR
   python3 cli/main.py timeline view                       Lihat visualisasi semua track & klip
   python3 cli/main.py timeline add --title "<Name>" [--duration 5] [--track 0]
   python3 cli/main.py timeline filter --clip-id <id> --name "Cinematic Glow"
   python3 cli/main.py timeline speed --clip-id <id> --multiplier 1.5
   python3 cli/main.py timeline curve --clip-id <id> --name "Hero|Bullet Time" [--multiplier 2.0]
   python3 cli/main.py timeline keyframe add --clip-id <id> [--time 1.2] [--x 10] [--y -5] [--scale 1.2]
   python3 cli/main.py timeline keyframe list --clip-id <id>
   python3 cli/main.py timeline keyframe remove --clip-id <id> --keyframe-id <kId>
   python3 cli/main.py timeline keyframe clear --clip-id <id>
   python3 cli/main.py timeline split --clip-id <id> --time 2.5
   python3 cli/main.py timeline audio --clip-id <id> [--vol 0.8] [--mute true/false]
   python3 cli/main.py timeline delete --clip-id <id>

5. LOW-RESOLUTION PROXY ENGINE & BACKGROUND TRANSCODER
   python3 cli/main.py proxy status                        Lihat status proxy & antrean job
   python3 cli/main.py proxy toggle [--mode on/off]        Ganti mode Proxy dan Full-Res
   python3 cli/main.py proxy res "<360p Proxy|540p Proxy>" Set target resolusi proxy
   python3 cli/main.py proxy auto [--mode on/off]          Aktifkan auto-transcode
   python3 cli/main.py proxy transcode <clipId>            Transcode klip tertentu
   python3 cli/main.py proxy transcode-all                 Transcode seluruh klip timeline

6. EXPORT STUDIO
   python3 cli/main.py export run [--res "1080p FHD"] [--fps 60] [--format MP4]
   python3 cli/main.py export list                         Lihat riwayat hasil export

7. PENGATURAN STUDIO
   python3 cli/main.py settings view
   python3 cli/main.py settings update [--gemini-key <key>] [--theme dark/light]

8. AUTOMATED AI AGENT RUNNER
   python3 cli/main.py agent "<Instructions>"            Jalankan otomatisasi penuh dari prompt

FLAG GLOBAL:
  --json                                                Keluarkan hasil dalam format JSON terstruktur
""")

def parse_args(args):
    flags = {}
    positional = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg.startswith('--'):
            key = arg[2:]
            if i + 1 < len(args) and not args[i + 1].startswith('--'):
                flags[key] = args[i + 1]
                i += 1
            else:
                flags[key] = True
        else:
            positional.append(arg)
        i += 1
    return flags, positional

def handle_command(raw_args):
    flags, positional = parse_args(raw_args)
    json_mode = bool(flags.get('json'))

    main_cmd = positional[0].lower() if positional else 'help'
    sub_cmd = positional[1].lower() if len(positional) > 1 else ''

    if main_cmd in ['help', '-h', '--help']:
        print_help()
    elif main_cmd in ['project', 'template']:
        if sub_cmd == 'list':
            print(list_projects(json_mode))
        elif sub_cmd == 'create':
            title = positional[2] if len(positional) > 2 else "Proyek Baru Studio"
            ratio = positional[3] if len(positional) > 3 else flags.get('ratio', "9:16")
            print(create_project(title, "Dibuat via Python CLI", ratio, "1080p FHD", json_mode))
        elif sub_cmd == 'select':
            target_id = positional[2] if len(positional) > 2 else flags.get('id', 1)
            print(select_project(target_id, json_mode))
        elif sub_cmd == 'delete':
            target_id = positional[2] if len(positional) > 2 else flags.get('id', 0)
            print(delete_project(target_id, json_mode))
        elif sub_cmd in ['template-create', 'make-template']:
            target_id = positional[2] if len(positional) > 2 else flags.get('id', 1)
            print(create_template(target_id, json_mode))
        elif sub_cmd in ['template-use', 'use-template']:
            tmpl_id = positional[2] if len(positional) > 2 else flags.get('id', 1)
            c_title = positional[3] if len(positional) > 3 else flags.get('title', "")
            media = flags.get('media', "")
            print(create_project_from_template(tmpl_id, c_title, media, json_mode))
        else:
            print(list_projects(json_mode))

    elif main_cmd == 'generate':
        prompt = flags.get('prompt') or (' '.join(positional[1:]) if len(positional) > 1 else "Cyberpunk video animation")
        print(generate_ai_video({
            "prompt": prompt,
            "style": flags.get('style', 'Cinematic'),
            "model": flags.get('model', 'Veo 2'),
            "ratio": flags.get('ratio', '9:16'),
            "durationSec": int(flags.get('duration', 5)),
            "fps": int(flags.get('fps', 30)),
            "jsonMode": json_mode
        }))

    elif main_cmd == 'storyboard':
        if sub_cmd == 'list':
            print(list_templates(json_mode))
        elif sub_cmd == 'compile':
            t_name = flags.get('template') or (' '.join(positional[2:]) if len(positional) > 2 else "TikTok Viral")
            print(compile_storyboard_to_timeline(t_name, json_mode))
        else:
            print(list_templates(json_mode))

    elif main_cmd == 'timeline':
        if sub_cmd == 'view':
            print(view_timeline(json_mode))
        elif sub_cmd == 'add':
            title = flags.get('title') or (' '.join(positional[2:]) if len(positional) > 2 else "Klip Baru")
            print(add_clip({
                "title": title,
                "durationSec": float(flags.get('duration', 5)),
                "trackIndex": int(flags.get('track', 0)),
                "jsonMode": json_mode
            }))
        elif sub_cmd == 'filter':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(update_filter(clip_id, flags.get('name', 'Cinematic Glow'), json_mode))
        elif sub_cmd == 'speed':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(update_speed(clip_id, flags.get('multiplier', 1.5), json_mode))
        elif sub_cmd == 'curve':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(set_speed_curve(clip_id, flags.get('name', 'Hero'), flags.get('multiplier'), json_mode))
        elif sub_cmd in ['keyframe', 'kf']:
            action = positional[2].lower() if len(positional) > 2 else 'list'
            clip_id = flags.get('clip-id') or (positional[3] if len(positional) > 3 else 1)
            if action == 'add':
                print(add_keyframe(clip_id, {
                    "timeSec": flags.get('time', 0.0),
                    "posX": flags.get('x', 0),
                    "posY": flags.get('y', 0),
                    "scale": flags.get('scale', 1.0),
                    "rotation": flags.get('rotation', 0),
                    "opacity": flags.get('opacity', 1.0),
                    "ease": flags.get('ease', "EaseInOut"),
                    "jsonMode": json_mode
                }))
            elif action in ['remove', 'delete']:
                print(remove_keyframe(clip_id, flags.get('keyframe-id') or (positional[4] if len(positional) > 4 else 1), json_mode))
            elif action == 'clear':
                print(clear_keyframes(clip_id, json_mode))
            else:
                print(list_keyframes(clip_id, json_mode))
        elif sub_cmd == 'split':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(split_clip(clip_id, flags.get('time', 2.5), json_mode))
        elif sub_cmd == 'audio':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(update_audio(clip_id, {
                "volume": flags.get('vol'),
                "isMuted": str(flags.get('mute')).lower() == 'true' if 'mute' in flags else None,
                "noiseReduction": str(flags.get('noise-red')).lower() == 'true' if 'noise-red' in flags else None,
                "vocalEnhance": str(flags.get('vocal-enhance')).lower() == 'true' if 'vocal-enhance' in flags else None,
                "jsonMode": json_mode
            }))
        elif sub_cmd == 'delete':
            clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
            print(delete_clip(clip_id, json_mode))
        else:
            print(view_timeline(json_mode))

    elif main_cmd in ['keyframe', 'kf']:
        action = sub_cmd or 'list'
        target_clip_id = flags.get('clip-id') or (positional[2] if len(positional) > 2 else 1)
        if action == 'add':
            print(add_keyframe(target_clip_id, {
                "timeSec": flags.get('time', 0.0),
                "posX": flags.get('x', 0),
                "posY": flags.get('y', 0),
                "scale": flags.get('scale', 1.0),
                "rotation": flags.get('rotation', 0),
                "opacity": flags.get('opacity', 1.0),
                "ease": flags.get('ease', "EaseInOut"),
                "jsonMode": json_mode
            }))
        elif action in ['remove', 'delete']:
            print(remove_keyframe(target_clip_id, flags.get('keyframe-id') or (positional[3] if len(positional) > 3 else 1), json_mode))
        elif action == 'clear':
            print(clear_keyframes(target_clip_id, json_mode))
        else:
            print(list_keyframes(target_clip_id, json_mode))

    elif main_cmd == 'proxy':
        if sub_cmd == 'status':
            print(view_proxy_status(json_mode))
        elif sub_cmd == 'toggle':
            mode_val = False if flags.get('mode') == 'off' else (True if flags.get('mode') == 'on' else None)
            print(toggle_proxy(mode_val, json_mode))
        elif sub_cmd == 'res':
            res_val = flags.get('target') or (' '.join(positional[2:]) if len(positional) > 2 else "360p Proxy")
            print(set_proxy_resolution(res_val, json_mode))
        elif sub_cmd == 'auto':
            auto_val = False if flags.get('mode') == 'off' else (True if flags.get('mode') == 'on' else None)
            print(toggle_auto_transcode(auto_val, json_mode))
        elif sub_cmd == 'transcode':
            cid = positional[2] if len(positional) > 2 else flags.get('clip-id', 1)
            print(transcode_clip(cid, json_mode))
        elif sub_cmd == 'transcode-all':
            print(transcode_all(json_mode))
        else:
            print(view_proxy_status(json_mode))

    elif main_cmd == 'export':
        if sub_cmd == 'run':
            print(export_video({
                "resolution": flags.get('res', "1080p FHD"),
                "fps": flags.get('fps', 60),
                "format": flags.get('format', "MP4"),
                "bitrate": flags.get('bitrate', "High (16 Mbps)"),
                "jsonMode": json_mode
            }))
        elif sub_cmd == 'list':
            print(list_exports(json_mode))
        else:
            print(export_video({"jsonMode": json_mode}))

    elif main_cmd == 'settings':
        if sub_cmd == 'view':
            print(view_settings(json_mode))
        elif sub_cmd == 'update':
            print(update_settings({
                "geminiApiKey": flags.get('gemini-key'),
                "highfieldApiKey": flags.get('highfield-key'),
                "customEndpoint": flags.get('endpoint'),
                "isDarkTheme": flags.get('theme') == 'dark' if 'theme' in flags else None,
                "jsonMode": json_mode
            }))
        else:
            print(view_settings(json_mode))

    elif main_cmd == 'agent':
        agent_prompt = ' '.join(positional[1:]) if len(positional) > 1 else (flags.get('prompt') or "Buat video TikTok viral 15 detik")
        print(run_agent_task(agent_prompt, json_mode))

    elif main_cmd in ['repl', 'interactive']:
        start_repl()

    else:
        print(f"❌ Perintah \"{main_cmd}\" tidak dikenali. Ketik \"python3 cli/main.py help\" untuk melihat bantuan.")

def start_repl():
    print("\n🤖 FLOWMONKEY STUDIO INTERACTIVE PYTHON REPL\nKetik 'exit' atau 'quit' untuk keluar.\n")
    while True:
        try:
            line = input('studio-cli> ').strip()
            if line.lower() in ['exit', 'quit']:
                print("REPL ditutup.")
                break
            if line:
                args = shlex.split(line)
                handle_command(args)
        except (KeyboardInterrupt, EOFError):
            print("\nREPL ditutup.")
            break

if __name__ == "__main__":
    user_args = sys.argv[1:]
    if not user_args:
        print_help()
    else:
        handle_command(user_args)
