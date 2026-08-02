const { createProject } = require('./projects');
const { generateAiVideo } = require('./generate');
const { compileStoryboardToTimeline } = require('./storyboard');
const { toggleProxy, setProxyResolution, transcodeAll } = require('./proxy');
const { exportVideo } = require('./export');
const { viewTimeline, updateFilter } = require('./timeline');

function runAgentTask(agentPrompt, jsonMode = false) {
  let logs = [];
  logs.push(`\n🤖 AI AGENT AUTOMATION EXECUTION RUNNER`);
  logs.push(`=========================================================`);
  logs.push(`Agent Prompt: "${agentPrompt}"\n`);

  // Step 1: Create or select project automatically based on prompt
  const projTitle = agentPrompt.substring(0, 30) || "AI Agent Automated Video";
  const projResult = createProject(projTitle, `Created automatically by AI Agent from prompt: ${agentPrompt}`, "9:16", "1080p FHD");
  logs.push(`[Step 1] Project Initialization:`);
  logs.push(projResult.trim());

  // Step 2: Auto-select Storyboard or Generate Clips
  if (agentPrompt.toLowerCase().includes("storyboard") || agentPrompt.toLowerCase().includes("tiktok")) {
    logs.push(`\n[Step 2] Storyboard Auto-Compiling:`);
    const sbResult = compileStoryboardToTimeline("TikTok Viral");
    logs.push(sbResult.trim());
  } else {
    logs.push(`\n[Step 2] AI Video Clip Generation:`);
    const genResult = generateAiVideo({
      prompt: agentPrompt,
      style: agentPrompt.toLowerCase().includes("cyberpunk") ? "Cyberpunk" : "Cinematic",
      model: "Veo 2",
      ratio: "9:16",
      durationSec: 10
    });
    logs.push(genResult.trim());
  }

  // Step 3: Low-Resolution Proxy Mode Activation for Smooth Timeline Playback
  logs.push(`\n[Step 3] Timeline Low-Res Proxy Mode Activation:`);
  setProxyResolution("360p Proxy");
  const proxyResult = transcodeAll();
  logs.push(proxyResult.trim());

  // Step 4: Render & Export
  logs.push(`\n[Step 4] Final Video Rendering & Export:`);
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

  return logs.join('\n') + `\n\n✅ AI Agent Task Completed Successfully!\n`;
}

module.exports = {
  runAgentTask
};
