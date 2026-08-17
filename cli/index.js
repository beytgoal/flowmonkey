#!/usr/bin/env node

const readline = require('readline');
const { listProjects, createProject, selectProject, deleteProject, createTemplate, createProjectFromTemplate } = require('./lib/projects');
const { generateAiVideo } = require('./lib/generate');
const { listTemplates, compileStoryboardToTimeline } = require('./lib/storyboard');
const { viewTimeline, addClip, updateFilter, updateSpeed, setSpeedCurve, addKeyframe, listKeyframes, removeKeyframe, clearKeyframes, splitClip, updateAudio, deleteClip } = require('./lib/timeline');
const { toggleProxy, setProxyResolution, toggleAutoTranscode, transcodeClip, transcodeAll, viewProxyStatus } = require('./lib/proxy');
const { exportVideo, listExports } = require('./lib/export');
const { viewSettings, updateSettings } = require('./lib/settings');
const { runAgentTask } = require('./lib/agent');
const { listRemotionVfx, renderRemotionCloudVfx, previewRemotionCloudVfx } = require('./lib/remotion');
const { getZeroCopyStatus, acquireZeroCopyFrame, forwardZeroCopyFrame, clearZeroCopyPipeline } = require('./lib/zerocopy');
const { listAiModels, downloadAiModel, loadModelToGpu, unloadModelFromGpu, purgeAiModelCaches } = require('./lib/models');
const { extractAudioTrack, applyVoiceChanger, applyAudioDenoise, analyzeBeatWaveform, applyMediaPipeRetouch, applyBodySilhouetteVfx } = require('./lib/multimedia');

function printHelp() {
  console.log(`
🎬 FLOWMONKEY VIDEO STUDIO CLI (AI AGENT OPERABLE ENGINE v3.0 - 1:1 ANDROID PARITY)
===================================================================================
PENGGUNAAN:
  node cli/index.js <command> [subcommand] [flags]
  node cli/index.js repl   (Masuk ke mode interaktif REPL untuk AI Agent)

PERINTAH UTAMA:

1. PROYEK & TEMPLATE (PROJECT & TEMPLATES)
   node cli/index.js project list                        List semua proyek video & template
   node cli/index.js project create "<Title>" [ratio]    Buat proyek baru (misal: "TikTok Video" 9:16)
   node cli/index.js project select <Id>                 Pilih proyek aktif
   node cli/index.js project delete <Id>                 Hapus proyek
   node cli/index.js project template-create <Id>        Jadikan proyek terpilih sebagai Template Full Tools
   node cli/index.js project template-use <TmplId> [Title] [--media <file.mp4>] Buat proyek baru dari Template

2. AI VIDEO GENERATOR (VEO & HIGHFIELD)
   node cli/index.js generate --prompt "<text>" [--style Cinematic|Cyberpunk|Anime] [--model "Veo 2"] [--duration 5]

3. STORYBOARD ENGINE
   node cli/index.js storyboard list                     Lihat daftar template storyboard
   node cli/index.js storyboard compile "<TemplateName>" Kompilasi adegan storyboard ke timeline (misal: "TikTok Viral")

4. MULTI-TRACK TIMELINE EDITOR
   node cli/index.js timeline view                       Lihat visualisasi semua track & klip
   node cli/index.js timeline add --title "<Name>" [--duration 5] [--track 0]
   node cli/index.js timeline filter --clip-id <id> --name "Cinematic Glow"
   node cli/index.js timeline speed --clip-id <id> --multiplier 1.5
   node cli/index.js timeline curve --clip-id <id> --name "Hero|Bullet Time|Montage" [--multiplier 2.0]
   node cli/index.js timeline keyframe add --clip-id <id> [--time 1.2] [--x 10] [--y -5] [--scale 1.2] [--rotation 15] [--opacity 0.9] [--ease Bezier]
   node cli/index.js timeline keyframe list --clip-id <id>
   node cli/index.js timeline keyframe remove --clip-id <id> --keyframe-id <kId>
   node cli/index.js timeline keyframe clear --clip-id <id>
   node cli/index.js timeline split --clip-id <id> --time 2.5
   node cli/index.js timeline audio --clip-id <id> [--vol 0.8] [--mute true/false]
   node cli/index.js timeline delete --clip-id <id>

5. VERCEL + REMOTION SERVERLESS CLOUD VFX RENDERING
   node cli/index.js remotion list                       Daftar 4 komposisi cloud VFX (Kinetic, Glitch, HUD, Particle)
   node cli/index.js remotion render --vfx <type> [--title "TEXT"] [--color "#8B5CF6"] [--duration 4]
   node cli/index.js remotion preview --vfx <type> [--frame 30]

6. ZERO-COPY GPU STREAMING PIPELINE (4K HEAP-PROTECTION)
   node cli/index.js zerocopy status                     Telemetry RAM dihemat, frame sharing & buffer pool
   node cli/index.js zerocopy acquire [--width 1920] [--height 1080] [--gpu true]
   node cli/index.js zerocopy forward --target mediapipe|opencv|gstreamer
   node cli/index.js zerocopy clear                      Recycle frame ring buffer

7. DYNAMIC AI MODEL LIFECYCLE (ON-DEMAND GPU LOADING)
   node cli/index.js model list                          Status INT8 models (Face Mesh, Pose, Segmenter, Hand, Object)
   node cli/index.js model download <modelId>            Download model on-demand ke disk cache
   node cli/index.js model load <modelId>                Muat model ke GPU/NPU RAM untuk inferensi
   node cli/index.js model unload <modelId>              Bebaskan model dari RAM
   node cli/index.js model purge                         Hapus seluruh file cache AI model

8. MULTIMEDIA & NATIVE ENGINES (FFMPEG & MEDIAPIPE)
   node cli/index.js audio extract --clip-id <id>        Ekstrak native audio stream (AAC) ke track audio
   node cli/index.js audio voice --clip-id <id> --effect "Robot|Chipmunk|Deep Monster|Radio Walkie|Alien|Studio Reverb"
   node cli/index.js audio denoise --clip-id <id> [--enable true/false]
   node cli/index.js audio beat --clip-id <id>           GStreamer beat waveform & energy peaks detection
   node cli/index.js vfx retouch --clip-id <id> [--smooth 0.8] [--sharpen 0.5]
   node cli/index.js vfx silhouette --clip-id <id> [--color "#00E5FF"]

9. LOW-RESOLUTION PROXY ENGINE & BACKGROUND TRANSCODER
   node cli/index.js proxy status                        Lihat status proxy & antrean job
   node cli/index.js proxy toggle [--mode on/off]        Ganti mode antara Proxy Low-Res dan Original 1080p
   node cli/index.js proxy res "<360p Proxy|540p Proxy>" Set target resolusi proxy
   node cli/index.js proxy auto [--mode on/off]          Aktifkan auto-transcode untuk aset baru
   node cli/index.js proxy transcode <clipId>            Transcode klip tertentu ke proxy
   node cli/index.js proxy transcode-all                 Transcode seluruh klip timeline ke proxy

10. EXPORT STUDIO
    node cli/index.js export run [--res "1080p FHD"] [--fps 60] [--format MP4]
    node cli/index.js export list                         Lihat riwayat hasil export

11. PENGATURAN STUDIO
    node cli/index.js settings view
    node cli/index.js settings update [--gemini-key <key>] [--theme dark/light]

12. AUTOMATED AI AGENT RUNNER
    node cli/index.js agent "<Instructions>"            Jalankan otomatisasi penuh 1:1 dari prompt AI agent

FLAG GLOBAL:
  --json                                                Keluarkan hasil dalam format JSON terstruktur
`);
}

function parseArgs(args) {
  const flags = {};
  const positional = [];
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg.startsWith('--')) {
      const key = arg.substring(2);
      if (i + 1 < args.length && !args[i + 1].startsWith('--')) {
        flags[key] = args[i + 1];
        i++;
      } else {
        flags[key] = true;
      }
    } else {
      positional.push(arg);
    }
  }
  return { flags, positional };
}

function handleCommand(rawArgs) {
  const { flags, positional } = parseArgs(rawArgs);
  const jsonMode = Boolean(flags.json);

  const mainCmd = positional[0] ? positional[0].toLowerCase() : 'help';
  const subCmd = positional[1] ? positional[1].toLowerCase() : '';

  try {
    switch (mainCmd) {
      case 'help':
        printHelp();
        break;

      case 'project':
        if (subCmd === 'list' || subCmd === 'ls') {
          console.log(listProjects(jsonMode));
        } else if (subCmd === 'create' || subCmd === 'new') {
          const title = positional[2] || "Proyek Baru";
          const ratio = positional[3] || flags.ratio || "16:9";
          const res = flags.res || "1080p FHD";
          console.log(createProject(title, "Dibuat via Studio CLI", ratio, res, jsonMode));
        } else if (subCmd === 'select') {
          const id = positional[2];
          console.log(selectProject(id, jsonMode));
        } else if (subCmd === 'delete' || subCmd === 'rm') {
          const id = positional[2];
          console.log(deleteProject(id, jsonMode));
        } else if (subCmd === 'template-create') {
          const id = positional[2];
          const name = positional[3] || flags.name || "Template Kustom";
          console.log(createTemplate(id, name, jsonMode));
        } else if (subCmd === 'template-use') {
          const tmplId = positional[2];
          const newTitle = positional[3] || flags.title || "Proyek Dari Template";
          const media = flags.media || "assets/sample_user_video.mp4";
          console.log(createProjectFromTemplate(tmplId, newTitle, media, jsonMode));
        } else {
          console.log("Perintah project tidak dikenal. Gunakan: list, create, select, delete, template-create, template-use");
        }
        break;

      case 'generate':
      case 'gen':
        const prompt = flags.prompt || positional.slice(1).join(" ") || "Cyberpunk Neon City 4K";
        const style = flags.style || "Cinematic";
        const model = flags.model || "Veo 2";
        const dur = flags.duration || flags.dur || 5;
        console.log(generateAiVideo({ prompt, style, model, ratio: flags.ratio || "16:9", durationSec: dur, jsonMode }));
        break;

      case 'storyboard':
      case 'sb':
        if (subCmd === 'list') {
          console.log(listTemplates(jsonMode));
        } else if (subCmd === 'compile') {
          const name = positional[2] || flags.name || "TikTok Viral";
          console.log(compileStoryboardToTimeline(name, jsonMode));
        } else {
          console.log("Perintah storyboard tidak dikenal. Gunakan: list, compile");
        }
        break;

      case 'timeline':
      case 'tl':
        if (subCmd === 'view' || subCmd === 'ls' || subCmd === '') {
          console.log(viewTimeline(jsonMode));
        } else if (subCmd === 'add') {
          const title = flags.title || positional[2] || "Klip Video";
          const dur = flags.duration || 5;
          const track = flags.track || 0;
          console.log(addClip({ title, durationSec: dur, trackIndex: track, jsonMode }));
        } else if (subCmd === 'filter') {
          const clipId = flags['clip-id'] || flags.clip || positional[2];
          const name = flags.name || positional[3] || "Vibrant Warm";
          console.log(updateFilter(clipId, name, jsonMode));
        } else if (subCmd === 'speed') {
          const clipId = flags['clip-id'] || flags.clip || positional[2];
          const mult = flags.multiplier || positional[3] || 1.0;
          console.log(updateSpeed(clipId, mult, jsonMode));
        } else if (subCmd === 'curve') {
          const clipId = flags['clip-id'] || flags.clip || positional[2];
          const name = flags.name || positional[3] || "Hero";
          const mult = flags.multiplier || 1.0;
          console.log(setSpeedCurve(clipId, name, mult, jsonMode));
        } else if (subCmd === 'keyframe' || subCmd === 'kf') {
          const action = positional[2];
          const clipId = flags['clip-id'] || flags.clip || positional[3];
          if (action === 'add') {
            console.log(addKeyframe({
              clipId,
              timeSec: flags.time || 0.0,
              posX: flags.x || 0,
              posY: flags.y || 0,
              scale: flags.scale || 1.0,
              rotation: flags.rotation || 0,
              opacity: flags.opacity || 1.0,
              ease: flags.ease || "EaseInOut",
              jsonMode
            }));
          } else if (action === 'list') {
            console.log(listKeyframes(clipId, jsonMode));
          } else if (action === 'remove' || action === 'rm') {
            const kfId = flags['keyframe-id'] || positional[4];
            console.log(removeKeyframe(clipId, kfId, jsonMode));
          } else if (action === 'clear') {
            console.log(clearKeyframes(clipId, jsonMode));
          } else {
            console.log("Subperintah keyframe: add, list, remove, clear");
          }
        } else if (subCmd === 'split') {
          const clipId = flags['clip-id'] || positional[2];
          const timeSec = flags.time || positional[3] || 2.5;
          console.log(splitClip(clipId, timeSec, jsonMode));
        } else if (subCmd === 'audio') {
          const clipId = flags['clip-id'] || positional[2];
          console.log(updateAudio({
            clipId,
            volume: flags.vol || flags.volume,
            isMuted: flags.mute,
            noiseReduction: flags['noise-red'],
            vocalEnhance: flags['vocal-enh'],
            jsonMode
          }));
        } else if (subCmd === 'delete' || subCmd === 'rm') {
          const clipId = flags['clip-id'] || positional[2];
          console.log(deleteClip(clipId, jsonMode));
        } else {
          console.log("Perintah timeline tidak dikenal. Gunakan: view, add, filter, speed, curve, keyframe, split, audio, delete");
        }
        break;

      case 'remotion':
        if (subCmd === 'list' || subCmd === 'ls' || subCmd === '') {
          console.log(listRemotionVfx(jsonMode));
        } else if (subCmd === 'render') {
          const vfxType = flags.vfx || positional[2] || "kinetic";
          const title = flags.title || positional[3] || "FLOWMONKEY CLOUD VFX";
          const themeColor = flags.color || "#8B5CF6";
          const durationSec = flags.duration || flags.dur || null;
          console.log(renderRemotionCloudVfx({ vfxType, title, themeColor, durationSec, jsonMode }));
        } else if (subCmd === 'preview') {
          const vfxType = flags.vfx || positional[2] || "kinetic";
          const frame = flags.frame || positional[3] || 30;
          console.log(previewRemotionCloudVfx(vfxType, frame, jsonMode));
        } else {
          console.log("Perintah remotion tidak dikenal. Gunakan: list, render, preview");
        }
        break;

      case 'zerocopy':
      case 'zc':
        if (subCmd === 'status' || subCmd === '') {
          console.log(getZeroCopyStatus(jsonMode));
        } else if (subCmd === 'acquire') {
          const w = parseInt(flags.width || 1920);
          const h = parseInt(flags.height || 1080);
          const gpu = flags.gpu !== 'false';
          console.log(acquireZeroCopyFrame(w, h, gpu, jsonMode));
        } else if (subCmd === 'forward') {
          const target = flags.target || positional[2] || "mediapipe";
          console.log(forwardZeroCopyFrame(target, null, jsonMode));
        } else if (subCmd === 'clear') {
          console.log(clearZeroCopyPipeline(jsonMode));
        } else {
          console.log("Perintah zerocopy tidak dikenal. Gunakan: status, acquire, forward, clear");
        }
        break;

      case 'model':
      case 'models':
        if (subCmd === 'list' || subCmd === 'ls' || subCmd === '') {
          console.log(listAiModels(jsonMode));
        } else if (subCmd === 'download') {
          const mId = positional[2] || flags.id || "face_mesh_int8";
          console.log(downloadAiModel(mId, jsonMode));
        } else if (subCmd === 'load') {
          const mId = positional[2] || flags.id || "face_mesh_int8";
          console.log(loadModelToGpu(mId, jsonMode));
        } else if (subCmd === 'unload') {
          const mId = positional[2] || flags.id || "face_mesh_int8";
          console.log(unloadModelFromGpu(mId, jsonMode));
        } else if (subCmd === 'purge') {
          console.log(purgeAiModelCaches(jsonMode));
        } else {
          console.log("Perintah model tidak dikenal. Gunakan: list, download, load, unload, purge");
        }
        break;

      case 'audio':
        if (subCmd === 'extract') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          console.log(extractAudioTrack(clipId, jsonMode));
        } else if (subCmd === 'voice') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          const effect = flags.effect || positional[3] || "Robot";
          console.log(applyVoiceChanger(clipId, effect, jsonMode));
        } else if (subCmd === 'denoise') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          const enable = flags.enable !== 'false';
          console.log(applyAudioDenoise(clipId, enable, jsonMode));
        } else if (subCmd === 'beat') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          console.log(analyzeBeatWaveform(clipId, jsonMode));
        } else {
          console.log("Perintah audio tidak dikenal. Gunakan: extract, voice, denoise, beat");
        }
        break;

      case 'vfx':
        if (subCmd === 'retouch') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          const smooth = parseFloat(flags.smooth || 0.8);
          const sharpen = parseFloat(flags.sharpen || 0.5);
          console.log(applyMediaPipeRetouch(clipId, smooth, sharpen, jsonMode));
        } else if (subCmd === 'silhouette') {
          const clipId = flags['clip-id'] || positional[2] || 1;
          const col = flags.color || "#00E5FF";
          console.log(applyBodySilhouetteVfx(clipId, col, jsonMode));
        } else {
          console.log("Perintah vfx tidak dikenal. Gunakan: retouch, silhouette");
        }
        break;

      case 'proxy':
        if (subCmd === 'status' || subCmd === '') {
          console.log(viewProxyStatus(jsonMode));
        } else if (subCmd === 'toggle') {
          const mode = flags.mode || positional[2];
          console.log(toggleProxy(mode, jsonMode));
        } else if (subCmd === 'res') {
          const resName = positional[2] || flags.res || "360p Proxy";
          console.log(setProxyResolution(resName, jsonMode));
        } else if (subCmd === 'auto') {
          const mode = flags.mode || positional[2];
          console.log(toggleAutoTranscode(mode, jsonMode));
        } else if (subCmd === 'transcode') {
          const clipId = positional[2] || flags['clip-id'];
          console.log(transcodeClip(clipId, jsonMode));
        } else if (subCmd === 'transcode-all') {
          console.log(transcodeAll(jsonMode));
        } else {
          console.log("Perintah proxy tidak dikenal. Gunakan: status, toggle, res, auto, transcode, transcode-all");
        }
        break;

      case 'export':
        if (subCmd === 'run' || subCmd === '') {
          const res = flags.res || flags.resolution || "1080p FHD";
          const fps = flags.fps || 60;
          const format = flags.format || "MP4";
          console.log(exportVideo({ resolution: res, fps, format, jsonMode }));
        } else if (subCmd === 'list' || subCmd === 'ls') {
          console.log(listExports(jsonMode));
        } else {
          console.log("Perintah export tidak dikenal. Gunakan: run, list");
        }
        break;

      case 'settings':
        if (subCmd === 'view' || subCmd === '') {
          console.log(viewSettings(jsonMode));
        } else if (subCmd === 'update') {
          console.log(updateSettings({
            geminiKey: flags['gemini-key'],
            highfieldKey: flags['highfield-key'],
            endpoint: flags.endpoint,
            isDark: flags.theme ? flags.theme === 'dark' : undefined,
            jsonMode
          }));
        } else {
          console.log("Perintah settings tidak dikenal. Gunakan: view, update");
        }
        break;

      case 'agent':
        const agentPrompt = positional.slice(1).join(" ") || flags.prompt || "Create a cinematic TikTok video with Kinetic Cloud VFX";
        console.log(runAgentTask(agentPrompt, jsonMode));
        break;

      case 'repl':
        startRepl();
        break;

      default:
        console.log(`Perintah '${mainCmd}' tidak ditemukan. Ketik 'node cli/index.js help' untuk bantuan.`);
    }
  } catch (err) {
    if (jsonMode) {
      console.error(JSON.stringify({ error: err.message }, null, 2));
    } else {
      console.error(`❌ Terjadi kesalahan: ${err.message}`);
    }
  }
}

function startRepl() {
  console.log("🎬 Masuk ke Mode Interaktif FlowMonkey REPL (Ketik 'exit' atau 'quit' untuk keluar, 'help' untuk bantuan)\n");
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: 'flowmonkey-cli> '
  });

  rl.prompt();

  rl.on('line', (line) => {
    const trimmed = line.trim();
    if (trimmed === 'exit' || trimmed === 'quit') {
      console.log("👋 Keluar dari FlowMonkey REPL.");
      process.exit(0);
    }
    if (trimmed) {
      const parts = trimmed.match(/(?:[^\s"]+|"[^"]*")+/g) || [];
      const cleaned = parts.map(p => p.startsWith('"') && p.endsWith('"') ? p.slice(1, -1) : p);
      handleCommand(cleaned);
    }
    console.log();
    rl.prompt();
  }).on('close', () => {
    console.log("👋 Selesai.");
    process.exit(0);
  });
}

if (require.main === module) {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    printHelp();
  } else {
    handleCommand(args);
  }
}
