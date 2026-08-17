const { createProject } = require('./projects');
const { generateAiVideo } = require('./generate');
const { compileStoryboardToTimeline } = require('./storyboard');
const { toggleProxy, setProxyResolution, transcodeAll } = require('./proxy');
const { exportVideo } = require('./export');
const { renderRemotionCloudVfx } = require('./remotion');
const { downloadAiModel, loadModelToGpu } = require('./models');
const { acquireZeroCopyFrame, forwardZeroCopyFrame } = require('./zerocopy');
const { applyVoiceChanger, applyAudioDenoise, applyMediaPipeRetouch, applyBodySilhouetteVfx } = require('./multimedia');

/**
 * 1:1 Intelligent AI Agent Task Runner.
 * Executes natural language automated workflows covering all video studio features.
 */
function runAgentTask(agentPrompt, jsonMode = false) {
  let logs = [];
  logs.push(`\n🤖 AI AGENT AUTOMATION EXECUTION RUNNER (1:1 PARITY)`);
  logs.push(`=========================================================`);
  logs.push(`Agent Prompt: "${agentPrompt}"\n`);

  const promptLower = agentPrompt.toLowerCase();

  // Step 1: Project Initialization
  const projTitle = agentPrompt.substring(0, 32) || "AI Agent Studio Video";
  const ratio = promptLower.includes("tiktok") || promptLower.includes("reels") || promptLower.includes("9:16") ? "9:16" : "16:9";
  const projResult = createProject(projTitle, `Created automatically by AI Agent from: ${agentPrompt}`, ratio, "1080p FHD");
  logs.push(`[Step 1] Project Initialization (${ratio}):`);
  logs.push(projResult.trim());

  // Step 2: Zero-Copy GPU Streaming Pipeline Activation
  logs.push(`\n[Step 2] Zero-Copy Streaming Pipeline Allocation:`);
  const zcResult = acquireZeroCopyFrame(1920, 1080, true);
  logs.push(zcResult.trim());

  // Step 3: Check for Remotion Cloud VFX Request
  if (
    promptLower.includes("remotion") ||
    promptLower.includes("kinetik") ||
    promptLower.includes("kinetic") ||
    promptLower.includes("glitch") ||
    promptLower.includes("hud") ||
    promptLower.includes("infografis") ||
    promptLower.includes("particle") ||
    promptLower.includes("partikel")
  ) {
    logs.push(`\n[Step 3] Vercel + Remotion Cloud VFX Delegation:`);
    let vfxType = "kinetic";
    if (promptLower.includes("glitch") || promptLower.includes("wave")) vfxType = "glitch";
    else if (promptLower.includes("hud") || promptLower.includes("chart") || promptLower.includes("infografis")) vfxType = "hud";
    else if (promptLower.includes("particle") || promptLower.includes("partikel") || promptLower.includes("cahaya")) vfxType = "particle";

    const themeColor = promptLower.includes("pink") ? "#EC4899" : (promptLower.includes("cyan") ? "#00E5FF" : "#8B5CF6");
    const remotionResult = renderRemotionCloudVfx({
      vfxType,
      title: agentPrompt.substring(0, 24).toUpperCase(),
      themeColor,
      durationSec: 4
    });
    logs.push(remotionResult.trim());
  }

  // Step 4: Check for Dynamic AI Model & Vision VFX
  if (promptLower.includes("face mesh") || promptLower.includes("retouch") || promptLower.includes("beauty")) {
    logs.push(`\n[Step 4] Dynamic AI Model On-Demand Loading (Face Mesh):`);
    downloadAiModel("face_mesh_int8");
    const loadRes = loadModelToGpu("face_mesh_int8");
    logs.push(loadRes.trim());
  } else if (promptLower.includes("silhouette") || promptLower.includes("pose") || promptLower.includes("glow")) {
    logs.push(`\n[Step 4] Dynamic AI Model On-Demand Loading (Pose Tracker):`);
    downloadAiModel("pose_tracker_int8");
    const loadRes = loadModelToGpu("pose_tracker_int8");
    logs.push(loadRes.trim());
  }

  // Step 5: Check for Storyboard vs AI Video Generation
  if (promptLower.includes("storyboard") || promptLower.includes("tiktok viral")) {
    logs.push(`\n[Step 5] Storyboard Auto-Compiling:`);
    const sbResult = compileStoryboardToTimeline("TikTok Viral");
    logs.push(sbResult.trim());
  } else {
    logs.push(`\n[Step 5] AI Video Clip Generation (Veo 2):`);
    const genResult = generateAiVideo({
      prompt: agentPrompt,
      style: promptLower.includes("cyberpunk") ? "Cyberpunk" : "Cinematic",
      model: "Veo 2",
      ratio,
      durationSec: 5
    });
    logs.push(genResult.trim());
  }

  // Step 6: Audio Processing (Voice Changer / Denoise)
  if (promptLower.includes("voice") || promptLower.includes("suara") || promptLower.includes("robot") || promptLower.includes("denoise")) {
    logs.push(`\n[Step 6] FFmpeg Audio Processing:`);
    const effect = promptLower.includes("chipmunk") ? "Chipmunk" : (promptLower.includes("monster") ? "Deep Monster" : "Robot");
    const vcRes = applyVoiceChanger(1, effect);
    logs.push(vcRes.trim());
    if (promptLower.includes("denoise") || promptLower.includes("noise")) {
      const denoiseRes = applyAudioDenoise(1, true);
      logs.push(denoiseRes.trim());
    }
  }

  // Step 7: Low-Resolution Proxy Mode Activation for Smooth Timeline Playback
  logs.push(`\n[Step 7] Timeline Low-Res Proxy Mode Activation:`);
  setProxyResolution("360p Proxy");
  const proxyResult = transcodeAll();
  logs.push(proxyResult.trim());

  // Step 8: Render & Export
  logs.push(`\n[Step 8] Final Video Rendering & Export:`);
  const expResult = exportVideo({
    resolution: "1080p FHD",
    fps: 60,
    format: "MP4"
  });
  logs.push(expResult.trim());

  if (jsonMode) {
    return JSON.stringify({
      success: true,
      agentPrompt,
      logs
    }, null, 2);
  }

  return logs.join('\n') + `\n\n✅ AI Agent Task Completed Successfully with 1:1 Parity!\n`;
}

module.exports = {
  runAgentTask
};
