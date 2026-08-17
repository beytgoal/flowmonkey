#!/usr/bin/env python3
import sys
import argparse
import shlex
import json
from lib.projects import list_projects, create_project, select_project, delete_project, create_template, create_project_from_template
from lib.generate import generate_ai_video
from lib.storyboard import list_templates, compile_storyboard_to_timeline
from lib.timeline import view_timeline, add_clip, update_filter, update_speed, set_speed_curve, add_keyframe, list_keyframes, remove_keyframe, clear_keyframes, split_clip, update_audio, delete_clip
from lib.proxy import toggle_proxy, set_proxy_resolution, toggle_auto_transcode, transcode_clip, transcode_all, view_proxy_status
from lib.export import export_video, list_exports
from lib.settings import view_settings, update_settings
from lib.agent import run_agent_task
from lib.remotion import list_remotion_vfx, render_remotion_cloud_vfx, preview_remotion_cloud_vfx
from lib.zerocopy import get_zerocopy_status, acquire_zerocopy_frame, forward_zerocopy_frame, clear_zerocopy_pipeline
from lib.models import list_ai_models, download_ai_model, load_model_to_gpu, unload_model_from_gpu, purge_ai_model_caches
from lib.multimedia import extract_audio_track, apply_voice_changer, apply_audio_denoise, analyze_beat_waveform, apply_mediapipe_retouch, apply_body_silhouette_vfx

def print_help():
    print("""
🎬 FLOWMONKEY VIDEO STUDIO PYTHON CLI (AI AGENT OPERABLE ENGINE v3.0 - 1:1 ANDROID PARITY)
===========================================================================================
PENGGUNAAN:
  python3 cli/main.py <command> [subcommand] [flags]
  python3 cli/main.py repl   (Masuk ke mode interaktif REPL untuk AI Agent)

PERINTAH UTAMA:

1. PROYEK & TEMPLATE (PROJECT & TEMPLATES)
   python3 cli/main.py project list                        List semua proyek video & template
   python3 cli/main.py project create "<Title>" [ratio]    Buat proyek baru (misal: "TikTok Video" 9:16)
   python3 cli/main.py project select <Id>                 Pilih proyek aktif
   python3 cli/main.py project delete <Id>                 Hapus proyek
   python3 cli/main.py project template-create <Id>        Jadikan proyek terpilih sebagai Template Full Tools
   python3 cli/main.py project template-use <TmplId> [Title] [--media <file.mp4>] Buat proyek baru dari Template

2. AI VIDEO GENERATOR (VEO & HIGHFIELD)
   python3 cli/main.py generate --prompt "<text>" [--style Cinematic|Cyberpunk|Anime] [--model "Veo 2"] [--duration 5]

3. STORYBOARD ENGINE
   python3 cli/main.py storyboard list                     Lihat daftar template storyboard
   python3 cli/main.py storyboard compile "<TemplateName>" Kompilasi adegan storyboard ke timeline (misal: "TikTok Viral")

4. MULTI-TRACK TIMELINE EDITOR
   python3 cli/main.py timeline view                       Lihat visualisasi semua track & klip
   python3 cli/main.py timeline add --title "<Name>" [--duration 5] [--track 0]
   python3 cli/main.py timeline filter --clip-id <id> --name "Cinematic Glow"
   python3 cli/main.py timeline speed --clip-id <id> --multiplier 1.5
   python3 cli/main.py timeline curve --clip-id <id> --name "Hero|Bullet Time|Montage" [--multiplier 2.0]
   python3 cli/main.py timeline keyframe add --clip-id <id> [--time 1.2] [--x 10] [--y -5] [--scale 1.2] [--rotation 15] [--opacity 0.9] [--ease Bezier]
   python3 cli/main.py timeline keyframe list --clip-id <id>
   python3 cli/main.py timeline keyframe remove --clip-id <id> --keyframe-id <kId>
   python3 cli/main.py timeline keyframe clear --clip-id <id>
   python3 cli/main.py timeline split --clip-id <id> --time 2.5
   python3 cli/main.py timeline audio --clip-id <id> [--vol 0.8] [--mute true/false]
   python3 cli/main.py timeline delete --clip-id <id>

5. VERCEL + REMOTION SERVERLESS CLOUD VFX RENDERING
   python3 cli/main.py remotion list                       Daftar 4 komposisi cloud VFX (Kinetic, Glitch, HUD, Particle)
   python3 cli/main.py remotion render --vfx <type> [--title "TEXT"] [--color "#8B5CF6"] [--duration 4]
   python3 cli/main.py remotion preview --vfx <type> [--frame 30]

6. ZERO-COPY GPU STREAMING PIPELINE (4K HEAP-PROTECTION)
   python3 cli/main.py zerocopy status                     Telemetry RAM dihemat, frame sharing & buffer pool
   python3 cli/main.py zerocopy acquire [--width 1920] [--height 1080] [--gpu true]
   python3 cli/main.py zerocopy forward --target mediapipe|opencv|gstreamer
   python3 cli/main.py zerocopy clear                      Recycle frame ring buffer

7. DYNAMIC AI MODEL LIFECYCLE (ON-DEMAND GPU LOADING)
   python3 cli/main.py model list                          Status INT8 models (Face Mesh, Pose, Segmenter, Hand, Object)
   python3 cli/main.py model download <modelId>            Download model on-demand ke disk cache
   python3 cli/main.py model load <modelId>                Muat model ke GPU/NPU RAM untuk inferensi
   python3 cli/main.py model unload <modelId>              Bebaskan model dari RAM
   python3 cli/main.py model purge                         Hapus seluruh file cache AI model

8. MULTIMEDIA & NATIVE ENGINES (FFMPEG & MEDIAPIPE)
   python3 cli/main.py audio extract --clip-id <id>        Ekstrak native audio stream (AAC) ke track audio
   python3 cli/main.py audio voice --clip-id <id> --effect "Robot|Chipmunk|Deep Monster|Radio Walkie|Alien|Studio Reverb"
   python3 cli/main.py audio denoise --clip-id <id> [--enable true/false]
   python3 cli/main.py audio beat --clip-id <id>           GStreamer beat waveform & energy peaks detection
   python3 cli/main.py vfx retouch --clip-id <id> [--smooth 0.8] [--sharpen 0.5]
   python3 cli/main.py vfx silhouette --clip-id <id> [--color "#00E5FF"]

9. LOW-RESOLUTION PROXY ENGINE & BACKGROUND TRANSCODER
   python3 cli/main.py proxy status                        Lihat status proxy & antrean job
   python3 cli/main.py proxy toggle [--mode on/off]        Ganti mode antara Proxy Low-Res dan Original 1080p
   python3 cli/main.py proxy res "<360p Proxy|540p Proxy>" Set target resolusi proxy
   python3 cli/main.py proxy auto [--mode on/off]          Aktifkan auto-transcode untuk aset baru
   python3 cli/main.py proxy transcode <clipId>            Transcode klip tertentu ke proxy
   python3 cli/main.py proxy transcode-all                 Transcode seluruh klip timeline ke proxy

10. EXPORT STUDIO
    python3 cli/main.py export run [--res "1080p FHD"] [--fps 60] [--format MP4]
    python3 cli/main.py export list                         Lihat riwayat hasil export

11. PENGATURAN STUDIO
    python3 cli/main.py settings view
    python3 cli/main.py settings update [--gemini-key <key>] [--theme dark/light]

12. AUTOMATED AI AGENT RUNNER
    python3 cli/main.py agent "<Instructions>"            Jalankan otomatisasi penuh 1:1 dari prompt AI agent

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

    try:
        if main_cmd == 'help':
            print_help()

        elif main_cmd == 'project':
            if sub_cmd in ['list', 'ls']:
                print(list_projects(json_mode))
            elif sub_cmd in ['create', 'new']:
                title = positional[2] if len(positional) > 2 else "Proyek Baru"
                ratio = positional[3] if len(positional) > 3 else flags.get('ratio', '16:9')
                res = flags.get('res', '1080p FHD')
                print(create_project(title, "Dibuat via Python CLI", ratio, res, json_mode))
            elif sub_cmd == 'select':
                p_id = positional[2] if len(positional) > 2 else 1
                print(select_project(p_id, json_mode))
            elif sub_cmd in ['delete', 'rm']:
                p_id = positional[2] if len(positional) > 2 else 1
                print(delete_project(p_id, json_mode))
            elif sub_cmd == 'template-create':
                p_id = positional[2] if len(positional) > 2 else 1
                name = positional[3] if len(positional) > 3 else flags.get('name', 'Template Kustom')
                print(create_template(p_id, name, json_mode))
            elif sub_cmd == 'template-use':
                t_id = positional[2] if len(positional) > 2 else 1
                new_title = positional[3] if len(positional) > 3 else flags.get('title', 'Proyek Dari Template')
                media = flags.get('media', 'assets/sample_user_video.mp4')
                print(create_project_from_template(t_id, new_title, media, json_mode))
            else:
                print("Perintah project tidak dikenal. Gunakan: list, create, select, delete, template-create, template-use")

        elif main_cmd in ['generate', 'gen']:
            prompt = flags.get('prompt') or (" ".join(positional[1:]) if len(positional) > 1 else "Cyberpunk Neon City 4K")
            style = flags.get('style', 'Cinematic')
            model = flags.get('model', 'Veo 2')
            dur = flags.get('duration', flags.get('dur', 5))
            print(generate_ai_video(prompt=prompt, style=style, model=model, ratio=flags.get('ratio', '16:9'), duration_sec=dur, json_mode=json_mode))

        elif main_cmd in ['storyboard', 'sb']:
            if sub_cmd == 'list':
                print(list_templates(json_mode))
            elif sub_cmd == 'compile':
                name = positional[2] if len(positional) > 2 else flags.get('name', 'TikTok Viral')
                print(compile_storyboard_to_timeline(name, json_mode))
            else:
                print("Perintah storyboard tidak dikenal. Gunakan: list, compile")

        elif main_cmd in ['timeline', 'tl']:
            if sub_cmd in ['view', 'ls', '']:
                print(view_timeline(json_mode))
            elif sub_cmd == 'add':
                title = flags.get('title') or (positional[2] if len(positional) > 2 else "Klip Video")
                dur = flags.get('duration', 5)
                track = flags.get('track', 0)
                print(add_clip(title=title, duration_sec=dur, track_index=track, json_mode=json_mode))
            elif sub_cmd == 'filter':
                clip_id = flags.get('clip-id', flags.get('clip', positional[2] if len(positional) > 2 else 1))
                name = flags.get('name', positional[3] if len(positional) > 3 else "Vibrant Warm")
                print(update_filter(clip_id, name, json_mode))
            elif sub_cmd == 'speed':
                clip_id = flags.get('clip-id', flags.get('clip', positional[2] if len(positional) > 2 else 1))
                mult = flags.get('multiplier', positional[3] if len(positional) > 3 else 1.0)
                print(update_speed(clip_id, mult, json_mode))
            elif sub_cmd == 'curve':
                clip_id = flags.get('clip-id', flags.get('clip', positional[2] if len(positional) > 2 else 1))
                name = flags.get('name', positional[3] if len(positional) > 3 else "Hero")
                mult = flags.get('multiplier', 1.0)
                print(set_speed_curve(clip_id, name, mult, json_mode))
            elif sub_cmd in ['keyframe', 'kf']:
                action = positional[2] if len(positional) > 2 else 'list'
                clip_id = flags.get('clip-id', flags.get('clip', positional[3] if len(positional) > 3 else 1))
                if action == 'add':
                    print(add_keyframe(
                        clip_id=clip_id,
                        time_sec=flags.get('time', 0.0),
                        pos_x=flags.get('x', 0),
                        pos_y=flags.get('y', 0),
                        scale=flags.get('scale', 1.0),
                        rotation=flags.get('rotation', 0),
                        opacity=flags.get('opacity', 1.0),
                        ease=flags.get('ease', "EaseInOut"),
                        json_mode=json_mode
                    ))
                elif action == 'list':
                    print(list_keyframes(clip_id, json_mode))
                elif action in ['remove', 'rm']:
                    kf_id = flags.get('keyframe-id', positional[4] if len(positional) > 4 else 1)
                    print(remove_keyframe(clip_id, kf_id, json_mode))
                elif action == 'clear':
                    print(clear_keyframes(clip_id, json_mode))
                else:
                    print("Subperintah keyframe: add, list, remove, clear")
            elif sub_cmd == 'split':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                time_sec = flags.get('time', positional[3] if len(positional) > 3 else 2.5)
                print(split_clip(clip_id, time_sec, json_mode))
            elif sub_cmd == 'audio':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                print(update_audio(
                    clip_id=clip_id,
                    volume=flags.get('vol', flags.get('volume')),
                    is_muted=flags.get('mute'),
                    noise_red=flags.get('noise-red'),
                    vocal_enh=flags.get('vocal-enh'),
                    json_mode=json_mode
                ))
            elif sub_cmd in ['delete', 'rm']:
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                print(delete_clip(clip_id, json_mode))
            else:
                print("Perintah timeline tidak dikenal. Gunakan: view, add, filter, speed, curve, keyframe, split, audio, delete")

        elif main_cmd == 'remotion':
            if sub_cmd in ['list', 'ls', '']:
                print(list_remotion_vfx(json_mode))
            elif sub_cmd == 'render':
                vfx_type = flags.get('vfx', positional[2] if len(positional) > 2 else "kinetic")
                title = flags.get('title', positional[3] if len(positional) > 3 else "FLOWMONKEY CLOUD VFX")
                theme_color = flags.get('color', '#8B5CF6')
                duration_sec = flags.get('duration', flags.get('dur'))
                print(render_remotion_cloud_vfx(vfx_type=vfx_type, title=title, theme_color=theme_color, duration_sec=duration_sec, json_mode=json_mode))
            elif sub_cmd == 'preview':
                vfx_type = flags.get('vfx', positional[2] if len(positional) > 2 else "kinetic")
                frame = flags.get('frame', positional[3] if len(positional) > 3 else 30)
                print(preview_remotion_cloud_vfx(vfx_type=vfx_type, frame=frame, json_mode=json_mode))
            else:
                print("Perintah remotion tidak dikenal. Gunakan: list, render, preview")

        elif main_cmd in ['zerocopy', 'zc']:
            if sub_cmd in ['status', '']:
                print(get_zerocopy_status(json_mode))
            elif sub_cmd == 'acquire':
                w = int(flags.get('width', 1920))
                h = int(flags.get('height', 1080))
                gpu = flags.get('gpu') != 'false'
                print(acquire_zerocopy_frame(w, h, gpu, json_mode))
            elif sub_cmd == 'forward':
                target = flags.get('target', positional[2] if len(positional) > 2 else "mediapipe")
                print(forward_zerocopy_frame(target, None, json_mode))
            elif sub_cmd == 'clear':
                print(clear_zerocopy_pipeline(json_mode))
            else:
                print("Perintah zerocopy tidak dikenal. Gunakan: status, acquire, forward, clear")

        elif main_cmd in ['model', 'models']:
            if sub_cmd in ['list', 'ls', '']:
                print(list_ai_models(json_mode))
            elif sub_cmd == 'download':
                m_id = positional[2] if len(positional) > 2 else flags.get('id', 'face_mesh_int8')
                print(download_ai_model(m_id, json_mode))
            elif sub_cmd == 'load':
                m_id = positional[2] if len(positional) > 2 else flags.get('id', 'face_mesh_int8')
                print(load_model_to_gpu(m_id, json_mode))
            elif sub_cmd == 'unload':
                m_id = positional[2] if len(positional) > 2 else flags.get('id', 'face_mesh_int8')
                print(unload_model_from_gpu(m_id, json_mode))
            elif sub_cmd == 'purge':
                print(purge_ai_model_caches(json_mode))
            else:
                print("Perintah model tidak dikenal. Gunakan: list, download, load, unload, purge")

        elif main_cmd == 'audio':
            if sub_cmd == 'extract':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                print(extract_audio_track(clip_id, json_mode))
            elif sub_cmd == 'voice':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                effect = flags.get('effect', positional[3] if len(positional) > 3 else "Robot")
                print(apply_voice_changer(clip_id, effect, json_mode))
            elif sub_cmd == 'denoise':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                enable = flags.get('enable') != 'false'
                print(apply_audio_denoise(clip_id, enable, json_mode))
            elif sub_cmd == 'beat':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                print(analyze_beat_waveform(clip_id, json_mode))
            else:
                print("Perintah audio tidak dikenal. Gunakan: extract, voice, denoise, beat")

        elif main_cmd == 'vfx':
            if sub_cmd == 'retouch':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                smooth = float(flags.get('smooth', 0.8))
                sharpen = float(flags.get('sharpen', 0.5))
                print(apply_mediapipe_retouch(clip_id, smooth, sharpen, json_mode))
            elif sub_cmd == 'silhouette':
                clip_id = flags.get('clip-id', positional[2] if len(positional) > 2 else 1)
                col = flags.get('color', '#00E5FF')
                print(apply_body_silhouette_vfx(clip_id, col, json_mode))
            else:
                print("Perintah vfx tidak dikenal. Gunakan: retouch, silhouette")

        elif main_cmd == 'proxy':
            if sub_cmd in ['status', '']:
                print(view_proxy_status(json_mode))
            elif sub_cmd == 'toggle':
                mode = flags.get('mode', positional[2] if len(positional) > 2 else None)
                print(toggle_proxy(mode, json_mode))
            elif sub_cmd == 'res':
                res_name = positional[2] if len(positional) > 2 else flags.get('res', '360p Proxy')
                print(set_proxy_resolution(res_name, json_mode))
            elif sub_cmd == 'auto':
                mode = flags.get('mode', positional[2] if len(positional) > 2 else None)
                print(toggle_auto_transcode(mode, json_mode))
            elif sub_cmd == 'transcode':
                clip_id = positional[2] if len(positional) > 2 else flags.get('clip-id')
                print(transcode_clip(clip_id, json_mode))
            elif sub_cmd == 'transcode-all':
                print(transcode_all(json_mode))
            else:
                print("Perintah proxy tidak dikenal. Gunakan: status, toggle, res, auto, transcode, transcode-all")

        elif main_cmd == 'export':
            if sub_cmd in ['run', '']:
                res = flags.get('res', flags.get('resolution', '1080p FHD'))
                fps = flags.get('fps', 60)
                fmt = flags.get('format', 'MP4')
                print(export_video(resolution=res, fps=fps, format=fmt, json_mode=json_mode))
            elif sub_cmd in ['list', 'ls']:
                print(list_exports(json_mode))
            else:
                print("Perintah export tidak dikenal. Gunakan: run, list")

        elif main_cmd == 'settings':
            if sub_cmd in ['view', '']:
                print(view_settings(json_mode))
            elif sub_cmd == 'update':
                print(update_settings(
                    gemini_key=flags.get('gemini-key'),
                    highfield_key=flags.get('highfield-key'),
                    endpoint=flags.get('endpoint'),
                    is_dark=flags.get('theme') == 'dark' if flags.get('theme') else None,
                    json_mode=json_mode
                ))
            else:
                print("Perintah settings tidak dikenal. Gunakan: view, update")

        elif main_cmd == 'agent':
            agent_prompt = " ".join(positional[1:]) if len(positional) > 1 else flags.get('prompt', 'Create a cinematic TikTok video with Kinetic Cloud VFX')
            print(run_agent_task(agent_prompt, json_mode))

        elif main_cmd == 'repl':
            start_repl()

        else:
            print(f"Perintah '{main_cmd}' tidak ditemukan. Ketik 'python3 cli/main.py help' untuk bantuan.")

    except Exception as e:
        if json_mode:
            print(json.dumps({'error': str(e)}, indent=2))
        else:
            print(f"❌ Terjadi kesalahan: {str(e)}")

def start_repl():
    print("🎬 Masuk ke Mode Interaktif FlowMonkey REPL (Ketik 'exit' atau 'quit' untuk keluar, 'help' untuk bantuan)\n")
    while True:
        try:
            line = input('flowmonkey-cli> ').strip()
            if line in ['exit', 'quit']:
                print("👋 Keluar dari FlowMonkey REPL.")
                break
            if line:
                parts = shlex.split(line)
                handle_command(parts)
            print()
        except (KeyboardInterrupt, EOFError):
            print("\n👋 Keluar dari FlowMonkey REPL.")
            break

if __name__ == '__main__':
    args = sys.argv[1:]
    if len(args) == 0:
        print_help()
    else:
        handle_command(args)
