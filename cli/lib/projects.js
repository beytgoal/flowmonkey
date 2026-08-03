const { getDb, saveDb } = require('./store');

function listProjects(jsonMode = false) {
  const db = getDb();
  const allProjects = db.projects || [];
  const userProjects = allProjects.filter(p => !p.isTemplate);
  const templates = allProjects.filter(p => p.isTemplate);

  if (jsonMode) {
    return JSON.stringify({ activeProjectId: db.activeProjectId, projects: userProjects, templates }, null, 2);
  }
  let output = `\n📁 TAB PROYEK AKTIF (Active ID: ${db.activeProjectId})\n`;
  output += "=======================================================\n";
  if (userProjects.length === 0) {
    output += "(Belum ada proyek aktif. Gunakan 'project create' atau 'template use')\n";
  } else {
    userProjects.forEach(p => {
      const isActive = p.id === db.activeProjectId ? " [AKTIF]" : "";
      output += `ID: ${p.id}${isActive} | "${p.title}" | Aspect: ${p.aspectRatio} | Res: ${p.resolution} | Created: ${p.createdAt.substring(0, 10)}\n`;
    });
  }

  output += `\n🔖 TAB TEMPLATE (FULL TOOLS & PLACEHOLDERS: ${templates.length})\n`;
  output += "=======================================================\n";
  if (templates.length === 0) {
    output += "(Belum ada template. Buat dengan 'project template-create <id>')\n";
  } else {
    templates.forEach(t => {
      output += `Template ID: ${t.id} | "${t.title}" | Aspect: ${t.aspectRatio} | Tools: Multi-Track, Curve 2.0x, Keyframes, Subtitle | Created: ${t.createdAt.substring(0, 10)}\n`;
    });
  }

  return output;
}

function createTemplate(projId, jsonMode = false) {
  const db = getDb();
  const pid = parseInt(projId);
  const source = db.projects.find(p => p.id === pid);

  if (!source) {
    if (jsonMode) return JSON.stringify({ error: "Source project not found" });
    return `❌ Error: Proyek ID ${projId} tidak ditemukan.`;
  }

  const newTemplateId = db.projects.length > 0 ? Math.max(...db.projects.map(p => p.id)) + 1 : 1;
  const tmplTitle = source.title.startsWith("Template -") ? source.title : `Template - ${source.title}`;

  const newTemplate = {
    id: newTemplateId,
    title: tmplTitle,
    description: `Template full tools preset dari proyek ${source.title}`,
    aspectRatio: source.aspectRatio || "9:16",
    resolution: source.resolution || "1080p FHD",
    isTemplate: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  db.projects.push(newTemplate);

  const ts = Date.now();
  const sourceTracks = (db.tracks || []).filter(t => t.projectId === pid);
  const sourceClips = (db.clips || []).filter(c => c.projectId === pid);

  const trackMap = {};
  const newTracks = sourceTracks.map((trk, idx) => {
    const newTrId = ts + idx + 1;
    trackMap[trk.id] = newTrId;
    const labelClean = (trk.label || "").replace("Trek ", "").replace("Trek", "").trim();
    return {
      id: newTrId,
      projectId: newTemplateId,
      trackIndex: trk.trackIndex ?? idx,
      type: trk.type || "VIDEO",
      label: labelClean || trk.type
    };
  });

  const newClips = sourceClips.map((clp, idx) => {
    const newClpId = ts + 100 + idx + 1;
    const targetTrId = trackMap[clp.trackId] || (newTracks[0] ? newTracks[0].id : 0);
    let clipTitle = clp.title || "";
    if (!clipTitle.includes("Placeholder")) {
      clipTitle = `Placeholder - ${clipTitle}`;
    }

    return {
      ...clp,
      id: newClpId,
      projectId: newTemplateId,
      trackId: targetTrId,
      title: clipTitle
    };
  });

  db.tracks = (db.tracks || []).concat(newTracks);
  db.clips = (db.clips || []).concat(newClips);
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({ success: true, template: newTemplate }, null, 2);
  }
  return `\n🔖 Berhasil membuat Template Full Tools dari proyek ID ${pid}: Template ID ${newTemplateId} - "${tmplTitle}"\n`;
}

function createProjectFromTemplate(templateId, customTitle = "", galleryMedia = "", jsonMode = false) {
  const db = getDb();
  const tid = parseInt(templateId);
  const tmpl = db.projects.find(p => p.id === tid && p.isTemplate);

  if (!tmpl) {
    if (jsonMode) return JSON.stringify({ error: "Template not found" });
    return `❌ Error: Template ID ${templateId} tidak ditemukan.`;
  }

  const newId = db.projects.length > 0 ? Math.max(...db.projects.map(p => p.id)) + 1 : 1;
  let titleFinal = customTitle ? customTitle : tmpl.title.replace("Template - ", "");
  if (galleryMedia) {
    titleFinal += ` (${galleryMedia.split('.')[0]})`;
  }

  const newProject = {
    id: newId,
    title: titleFinal,
    description: `Proyek dibuat dari template ${tmpl.title}`,
    aspectRatio: tmpl.aspectRatio || "9:16",
    resolution: tmpl.resolution || "1080p FHD",
    isTemplate: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  db.projects.push(newProject);
  db.activeProjectId = newId;

  const ts = Date.now();
  const tmplTracks = (db.tracks || []).filter(t => t.projectId === tid);
  const tmplClips = (db.clips || []).filter(c => c.projectId === tid);

  const trackMap = {};
  const newTracks = tmplTracks.map((trk, idx) => {
    const newTrId = ts + idx + 1;
    trackMap[trk.id] = newTrId;
    return {
      id: newTrId,
      projectId: newId,
      trackIndex: trk.trackIndex ?? idx,
      type: trk.type || "VIDEO",
      label: trk.label || ""
    };
  });

  const newClips = tmplClips.map((clp, idx) => {
    const newClpId = ts + 100 + idx + 1;
    const targetTrId = trackMap[clp.trackId] || (newTracks[0] ? newTracks[0].id : 0);
    let clipTitle = (clp.title || "").replace("Placeholder - ", "");
    let clipUri = clp.uri || "";

    if (idx === 0 && galleryMedia) {
      clipTitle = `Media Galeri: ${galleryMedia}`;
      clipUri = `file:///sdcard/Gallery/${galleryMedia}`;
    }

    return {
      ...clp,
      id: newClpId,
      projectId: newId,
      trackId: targetTrId,
      title: clipTitle,
      uri: clipUri
    };
  });

  db.tracks = (db.tracks || []).concat(newTracks);
  db.clips = (db.clips || []).concat(newClips);
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({ success: true, project: newProject }, null, 2);
  }
  return `\n🚀 Proyek baru berhasil dibuat dari Template! ID ${newId} - "${titleFinal}" (Proyek Aktif)\n`;
}

function createProject(title, description = "Dibuat via FlowMonkey CLI", aspectRatio = "9:16", resolution = "1080p FHD", jsonMode = false) {
  const db = getDb();
  const newId = db.projects.length > 0 ? Math.max(...db.projects.map(p => p.id)) + 1 : 1;
  const newProject = {
    id: newId,
    title,
    description,
    aspectRatio,
    resolution,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  db.projects.push(newProject);
  db.activeProjectId = newId;

  // Initialize tracks for new project
  const newTracks = [
    { id: Date.now() + 1, projectId: newId, trackIndex: 0, type: "VIDEO", label: "Utama (Video Track 1)" },
    { id: Date.now() + 2, projectId: newId, trackIndex: 1, type: "VIDEO", label: "Overlay Track" },
    { id: Date.now() + 3, projectId: newId, trackIndex: 2, type: "AUDIO", label: "Musik & Efek Suara" },
    { id: Date.now() + 4, projectId: newId, trackIndex: 3, type: "TEXT", label: "Subtitel AI" }
  ];
  db.tracks = db.tracks.concat(newTracks);
  saveDb(db);

  if (jsonMode) {
    return JSON.stringify({ success: true, project: newProject }, null, 2);
  }
  return `\n✅ Proyek baru berhasil dibuat dan diaktifkan: ID ${newId} - "${title}" (${aspectRatio}, ${resolution})\n`;
}

function selectProject(id, jsonMode = false) {
  const db = getDb();
  const target = db.projects.find(p => p.id === parseInt(id));
  if (!target) {
    if (jsonMode) return JSON.stringify({ error: "Project not found" });
    return `❌ Error: Proyek dengan ID ${id} tidak ditemukan.`;
  }
  db.activeProjectId = target.id;
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, activeProjectId: target.id });
  return `\n✅ Proyek aktif diubah ke: ID ${target.id} - "${target.title}"\n`;
}

function deleteProject(id, jsonMode = false) {
  const db = getDb();
  const projId = parseInt(id);
  const target = db.projects.find(p => p.id === projId);
  if (!target) {
    if (jsonMode) return JSON.stringify({ error: "Project not found" });
    return `❌ Error: Proyek ID ${id} tidak ditemukan.`;
  }
  db.projects = db.projects.filter(p => p.id !== projId);
  db.tracks = db.tracks.filter(t => t.projectId !== projId);
  db.clips = db.clips.filter(c => c.projectId !== projId);
  if (db.activeProjectId === projId) {
    db.activeProjectId = db.projects.length > 0 ? db.projects[0].id : 0;
  }
  saveDb(db);
  if (jsonMode) return JSON.stringify({ success: true, deletedId: projId, newActiveId: db.activeProjectId });
  return `\n🗑️ Proyek ID ${projId} ("${target.title}") berhasil dihapus.\n`;
}

module.exports = {
  listProjects,
  createProject,
  selectProject,
  deleteProject,
  createTemplate,
  createProjectFromTemplate
};
