import json
from .projects import create_project
from .generate import generate_ai_video
from .storyboard import compile_storyboard_to_timeline
from .proxy import set_proxy_resolution, transcode_all
from .export import export_video

def run_agent_task(agent_prompt, json_mode=False):
    logs = []
    logs.append("\n🤖 AI AGENT AUTOMATION EXECUTION RUNNER (Python Engine)")
    logs.append("=========================================================")
    logs.append(f"Agent Prompt: \"{agent_prompt}\"\n")

    # Step 1: Create or select project automatically based on prompt
    proj_title = agent_prompt[:30] if agent_prompt else "AI Agent Automated Video"
    proj_result = create_project(proj_title, f"Created automatically by AI Agent from prompt: {agent_prompt}", "9:16", "1080p FHD")
    logs.append("[Step 1] Project Initialization:")
    logs.append(proj_result.strip())

    # Step 2: Auto-select Storyboard or Generate Clips
    prompt_lower = agent_prompt.lower()
    if "storyboard" in prompt_lower or "tiktok" in prompt_lower:
        logs.append("\n[Step 2] Storyboard Auto-Compiling:")
        sb_result = compile_storyboard_to_timeline("TikTok Viral")
        logs.append(sb_result.strip())
    else:
        logs.append("\n[Step 2] AI Video Clip Generation:")
        gen_result = generate_ai_video({
            "prompt": agent_prompt,
            "style": "Cyberpunk" if "cyberpunk" in prompt_lower else "Cinematic",
            "model": "Veo 2",
            "ratio": "9:16",
            "durationSec": 10
        })
        logs.append(gen_result.strip())

    # Step 3: Low-Resolution Proxy Mode Activation
    logs.append("\n[Step 3] Timeline Low-Res Proxy Mode Activation:")
    set_proxy_resolution("360p Proxy")
    proxy_result = transcode_all()
    logs.append(proxy_result.strip())

    # Step 4: Render & Export
    logs.append("\n[Step 4] Final Video Rendering & Export:")
    exp_result = export_video({
        "resolution": "1080p FHD",
        "fps": 60,
        "format": "MP4"
    })
    logs.append(exp_result.strip())

    if json_mode:
        return json.dumps({
            "success": True,
            "agentPrompt": agent_prompt,
            "logs": logs
        }, indent=2)

    return "\n".join(logs) + "\n\n✅ AI Agent Task Completed Successfully!\n"
