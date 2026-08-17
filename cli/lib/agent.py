import json
from lib.projects import create_project
from lib.generate import generate_ai_video
from lib.storyboard import compile_storyboard_to_timeline
from lib.proxy import set_proxy_resolution, transcode_all
from lib.export import export_video
from lib.remotion import render_remotion_cloud_vfx
from lib.models import download_ai_model, load_model_to_gpu
from lib.zerocopy import acquire_zerocopy_frame
from lib.multimedia import apply_voice_changer, apply_audio_denoise

def run_agent_task(agent_prompt, json_mode=False):
    logs = []
    logs.append("\n🤖 AI AGENT AUTOMATION EXECUTION RUNNER (1:1 PARITY)")
    logs.append("=========================================================")
    logs.append(f'Agent Prompt: "{agent_prompt}"\n')

    prompt_lower = agent_prompt.lower()

    # Step 1: Project Initialization
    proj_title = agent_prompt[:32] or "AI Agent Studio Video"
    ratio = "9:16" if ("tiktok" in prompt_lower or "reels" in prompt_lower or "9:16" in prompt_lower) else "16:9"
    proj_result = create_project(proj_title, f"Created automatically by AI Agent from: {agent_prompt}", ratio, "1080p FHD")
    logs.append(f"[Step 1] Project Initialization ({ratio}):")
    logs.append(proj_result.strip())

    # Step 2: Zero-Copy GPU Streaming Pipeline
    logs.append("\n[Step 2] Zero-Copy Streaming Pipeline Allocation:")
    zc_result = acquire_zerocopy_frame(1920, 1080, True)
    logs.append(zc_result.strip())

    # Step 3: Remotion Cloud VFX Delegation
    if any(k in prompt_lower for k in ["remotion", "kinetik", "kinetic", "glitch", "hud", "infografis", "particle", "partikel"]):
        logs.append("\n[Step 3] Vercel + Remotion Cloud VFX Delegation:")
        vfx_type = "kinetic"
        if "glitch" in prompt_lower or "wave" in prompt_lower:
            vfx_type = "glitch"
        elif "hud" in prompt_lower or "chart" in prompt_lower or "infografis" in prompt_lower:
            vfx_type = "hud"
        elif "particle" in prompt_lower or "partikel" in prompt_lower or "cahaya" in prompt_lower:
            vfx_type = "particle"

        theme_color = "#EC4899" if "pink" in prompt_lower else ("#00E5FF" if "cyan" in prompt_lower else "#8B5CF6")
        remotion_res = render_remotion_cloud_vfx(
            vfx_type=vfx_type,
            title=agent_prompt[:24].upper(),
            theme_color=theme_color,
            duration_sec=4
        )
        logs.append(remotion_res.strip())

    # Step 4: Dynamic AI Model Loading
    if any(k in prompt_lower for k in ["face mesh", "retouch", "beauty"]):
        logs.append("\n[Step 4] Dynamic AI Model On-Demand Loading (Face Mesh):")
        download_ai_model("face_mesh_int8")
        load_res = load_model_to_gpu("face_mesh_int8")
        logs.append(load_res.strip())
    elif any(k in prompt_lower for k in ["silhouette", "pose", "glow"]):
        logs.append("\n[Step 4] Dynamic AI Model On-Demand Loading (Pose Tracker):")
        download_ai_model("pose_tracker_int8")
        load_res = load_model_to_gpu("pose_tracker_int8")
        logs.append(load_res.strip())

    # Step 5: Storyboard vs Video Generation
    if "storyboard" in prompt_lower or "tiktok viral" in prompt_lower:
        logs.append("\n[Step 5] Storyboard Auto-Compiling:")
        sb_result = compile_storyboard_to_timeline("TikTok Viral")
        logs.append(sb_result.strip())
    else:
        logs.append("\n[Step 5] AI Video Clip Generation (Veo 2):")
        gen_result = generate_ai_video(
            prompt=agent_prompt,
            style="Cyberpunk" if "cyberpunk" in prompt_lower else "Cinematic",
            model="Veo 2",
            ratio=ratio,
            duration_sec=5
        )
        logs.append(gen_result.strip())

    # Step 6: Audio Processing
    if any(k in prompt_lower for k in ["voice", "suara", "robot", "denoise"]):
        logs.append("\n[Step 6] FFmpeg Audio Processing:")
        effect = "Chipmunk" if "chipmunk" in prompt_lower else ("Deep Monster" if "monster" in prompt_lower else "Robot")
        vc_res = apply_voice_changer(1, effect)
        logs.append(vc_res.strip())
        if "denoise" in prompt_lower or "noise" in prompt_lower:
            denoise_res = apply_audio_denoise(1, True)
            logs.append(denoise_res.strip())

    # Step 7: Low-Resolution Proxy Mode
    logs.append("\n[Step 7] Timeline Low-Res Proxy Mode Activation:")
    set_proxy_resolution("360p Proxy")
    proxy_result = transcode_all()
    logs.append(proxy_result.strip())

    # Step 8: Render & Export
    logs.append("\n[Step 8] Final Video Rendering & Export:")
    exp_result = export_video(resolution="1080p FHD", fps=60, format="MP4")
    logs.append(exp_result.strip())

    if json_mode:
        return json.dumps({
            "success": True,
            "agentPrompt": agent_prompt,
            "logs": logs
        }, indent=2)

    return "\n".join(logs) + "\n\n✅ AI Agent Task Completed Successfully with 1:1 Parity!\n"
