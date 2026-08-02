#!/usr/bin/env node

const readline = require('readline');
const { listProjects, createProject, selectProject, deleteProject } = require('./lib/projects');
const { generateAiVideo } = require('./lib/generate');
const { listTemplates, compileStoryboardToTimeline } = require('./lib/storyboard');
const { viewTimeline, addClip, updateFilter, updateSpeed, setSpeedCurve, addKeyframe, listKeyframes, removeKeyframe, clearKeyframes, splitClip, updateAudio, deleteClip } = require('./lib/timeline');
const { toggleProxy, setProxyResolution, toggleAutoTranscode, transcodeClip, transcodeAll, viewProxyStatus } = require('./lib/proxy');
const { exportVideo, listExports } = require('./lib/export');
const { viewSettings, updateSettings } = require('./lib/settings');
const { runAgentTask } = require('./lib/agent');

function printHelp() {
  console.log(`
🎬 FLOWMONKEY VIDEO STUDIO CLI (AI AGENT OPERABLE ENGINE v1.0)
========================================================================
PENGGUNAAN:
  node cli/index.js <command> [subcommand] [flags]
  node cli/index.js repl   (Masuk ke mode interaktif REPL untuk AI Agent)

PERINTAH UTAMA:

1. PROYEK (PROJECT)
   node cli/index.js project list                        List semua proyek video
   node cli/index.js project create "<Title>" [ratio]    Buat proyek baru (misal: "TikTok Video" 9:16)
   node cli/index.js project select <Id>                 Pilih proyek aktif
   node cli/index.js project delete <Id>                 Hapus proyek

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
   node cli/index.js timeline audio --clip-id <id> [--vol 0.8] [--mute true/false] [--noise-red true/false]
   node cli/index.js timeline delete --clip-id <id>

5. LOW-RESOLUTION PROXY ENGINE & BACKGROUND TRANSCODER
   node cli/index.js proxy status                        Lihat status proxy & antrean job
   node cli/index.js proxy toggle [--mode on/off]        Ganti mode antara Proxy Low-Res dan Original 1080p
   node cli/index.js proxy res "<360p Proxy|540p Proxy>" Set target resolusi proxy
   node cli/index.js proxy auto [--mode on/off]          Aktifkan auto-transcode untuk aset baru
   node cli/index.js proxy transcode <clipId>            Transcode klip tertentu ke proxy
   node cli/index.js proxy transcode-all                 Transcode seluruh klip timeline ke proxy

6. EXPORT STUDIO
   node cli/index.js export run [--res "1080p FHD"] [--fps 60] [--format MP4]
   node cli/index.js export list                         Lihat riwayat hasil export

7. PENGATURAN STUDIO
   node cli/index.js settings view
   node cli/index.js settings update [--gemini-key <key>] [--theme dark/light]

8. AUTOMATED AI AGENT RUNNER
   node cli/index.js agent "<Instructions>"            Jalankan otomatisasi penuh dari prompt instruksi AI agent

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

  switch (mainCmd) {
    case 'help':
    case '-h':
    case '--help':
      printHelp();
      break;

    case 'project':
      if (subCmd === 'list') console.log(listProjects(jsonMode));
      else if (subCmd === 'create') {
        const title = positional[2] || "Proyek Baru Studio";
        const ratio = positional[3] || flags.ratio || "9:16";
        console.log(createProject(title, "Dibuat via CLI", ratio, "1080p FHD", jsonMode));
      } else if (subCmd === 'select') console.log(selectProject(positional[2], jsonMode));
      else if (subCmd === 'delete') console.log(deleteProject(positional[2], jsonMode));
      else console.log(listProjects(jsonMode));
      break;

    case 'generate':
      console.log(generateAiVideo({
        prompt: flags.prompt || positional.slice(1).join(' ') || "Cyberpunk video animation",
        style: flags.style || "Cinematic",
        model: flags.model || "Veo 2",
        ratio: flags.ratio || "9:16",
        durationSec: parseInt(flags.duration || 5),
        fps: parseInt(flags.fps || 30),
        jsonMode
      }));
      break;

    case 'storyboard':
      if (subCmd === 'list') console.log(listTemplates(jsonMode));
      else if (subCmd === 'compile') {
        const tName = flags.template || positional.slice(2).join(' ') || "TikTok Viral";
        console.log(compileStoryboardToTimeline(tName, jsonMode));
      } else console.log(listTemplates(jsonMode));
      break;

    case 'timeline':
      if (subCmd === 'view') console.log(viewTimeline(jsonMode));
      else if (subCmd === 'add') {
        console.log(addClip({
          title: flags.title || positional.slice(2).join(' ') || "Klip Baru",
          durationSec: parseFloat(flags.duration || 5),
          trackIndex: parseInt(flags.track || 0),
          jsonMode
        }));
      } else if (subCmd === 'filter') {
        console.log(updateFilter(flags['clip-id'] || positional[2], flags.name || "Cinematic Glow", jsonMode));
      } else if (subCmd === 'speed') {
        console.log(updateSpeed(flags['clip-id'] || positional[2], flags.multiplier || 1.5, jsonMode));
      } else if (subCmd === 'curve') {
        console.log(setSpeedCurve(flags['clip-id'] || positional[2], flags.name || "Hero", flags.multiplier, jsonMode));
      } else if (subCmd === 'keyframe' || subCmd === 'kf') {
        const action = positional[2] ? positional[2].toLowerCase() : 'list';
        const clipId = flags['clip-id'] || positional[3] || 1;
        if (action === 'add') {
          console.log(addKeyframe(clipId, {
            timeSec: flags.time || 0.0,
            posX: flags.x || 0,
            posY: flags.y || 0,
            scale: flags.scale || 1.0,
            rotation: flags.rotation || 0,
            opacity: flags.opacity || 1.0,
            ease: flags.ease || "EaseInOut",
            jsonMode
          }));
        } else if (action === 'remove' || action === 'delete') {
          console.log(removeKeyframe(clipId, flags['keyframe-id'] || positional[4], jsonMode));
        } else if (action === 'clear') {
          console.log(clearKeyframes(clipId, jsonMode));
        } else {
          console.log(listKeyframes(clipId, jsonMode));
        }
      } else if (subCmd === 'split') {
        console.log(splitClip(flags['clip-id'] || positional[2], flags.time || 2.5, jsonMode));
      } else if (subCmd === 'audio') {
        console.log(updateAudio(flags['clip-id'] || positional[2], {
          volume: flags.vol,
          isMuted: flags.mute === 'true',
          noiseReduction: flags['noise-red'] === 'true',
          vocalEnhance: flags['vocal-enhance'] === 'true',
          jsonMode
        }));
      } else if (subCmd === 'delete') {
        console.log(deleteClip(flags['clip-id'] || positional[2], jsonMode));
      } else console.log(viewTimeline(jsonMode));
      break;

    case 'keyframe':
    case 'kf':
      const kfAction = subCmd || 'list';
      const targetClipId = flags['clip-id'] || positional[2] || 1;
      if (kfAction === 'add') {
        console.log(addKeyframe(targetClipId, {
          timeSec: flags.time || 0.0,
          posX: flags.x || 0,
          posY: flags.y || 0,
          scale: flags.scale || 1.0,
          rotation: flags.rotation || 0,
          opacity: flags.opacity || 1.0,
          ease: flags.ease || "EaseInOut",
          jsonMode
        }));
      } else if (kfAction === 'remove' || kfAction === 'delete') {
        console.log(removeKeyframe(targetClipId, flags['keyframe-id'] || positional[3], jsonMode));
      } else if (kfAction === 'clear') {
        console.log(clearKeyframes(targetClipId, jsonMode));
      } else {
        console.log(listKeyframes(targetClipId, jsonMode));
      }
      break;

    case 'proxy':
      if (subCmd === 'status') console.log(viewProxyStatus(jsonMode));
      else if (subCmd === 'toggle') {
        const modeVal = flags.mode === 'off' ? false : flags.mode === 'on' ? true : undefined;
        console.log(toggleProxy(modeVal, jsonMode));
      } else if (subCmd === 'res') {
        const resVal = flags.target || positional.slice(2).join(' ') || "360p Proxy";
        console.log(setProxyResolution(resVal, jsonMode));
      } else if (subCmd === 'auto') {
        const autoVal = flags.mode === 'off' ? false : flags.mode === 'on' ? true : undefined;
        console.log(toggleAutoTranscode(autoVal, jsonMode));
      } else if (subCmd === 'transcode') {
        console.log(transcodeClip(positional[2] || flags['clip-id'], jsonMode));
      } else if (subCmd === 'transcode-all') {
        console.log(transcodeAll(jsonMode));
      } else console.log(viewProxyStatus(jsonMode));
      break;

    case 'export':
      if (subCmd === 'run') {
        console.log(exportVideo({
          resolution: flags.res || "1080p FHD",
          fps: flags.fps || 60,
          format: flags.format || "MP4",
          bitrate: flags.bitrate || "High (16 Mbps)",
          jsonMode
        }));
      } else if (subCmd === 'list') {
        console.log(listExports(jsonMode));
      } else {
        console.log(exportVideo({ jsonMode }));
      }
      break;

    case 'settings':
      if (subCmd === 'view') console.log(viewSettings(jsonMode));
      else if (subCmd === 'update') {
        console.log(updateSettings({
          geminiApiKey: flags['gemini-key'],
          highfieldApiKey: flags['highfield-key'],
          customEndpoint: flags.endpoint,
          isDarkTheme: flags.theme ? flags.theme === 'dark' : undefined,
          jsonMode
        }));
      } else console.log(viewSettings(jsonMode));
      break;

    case 'agent':
      const agentPrompt = positional.slice(1).join(' ') || flags.prompt || "Buat video TikTok viral 15 detik";
      console.log(runAgentTask(agentPrompt, jsonMode));
      break;

    case 'repl':
    case 'interactive':
      startRepl();
      break;

    default:
      console.log(`❌ Perintah "${mainCmd}" tidak dikenali. Ketik "node cli/index.js help" untuk melihat bantuan.`);
      break;
  }
}

function startRepl() {
  console.log(`\n🤖 FLOWMONKEY STUDIO INTERACTIVE AGENT REPL\nType 'exit' or 'quit' to exit.\n`);
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: 'studio-cli> '
  });

  rl.prompt();

  rl.on('line', (line) => {
    const str = line.trim();
    if (str === 'exit' || str === 'quit') {
      rl.close();
      return;
    }
    if (str.length > 0) {
      const args = str.match(/(?:[^\s"]+|"[^"]*")+/g).map(arg => arg.replace(/^"|"$/g, ''));
      handleCommand(args);
    }
    rl.prompt();
  }).on('close', () => {
    console.log('REPL closed.');
    process.exit(0);
  });
}

// Execute CLI
if (require.main === module) {
  const userArgs = process.argv.slice(2);
  if (userArgs.length === 0) {
    printHelp();
  } else {
    handleCommand(userArgs);
  }
}
