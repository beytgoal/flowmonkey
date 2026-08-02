const { getDb, saveDb } = require('./store');

function listProjects(jsonMode = false) {
  const db = getDb();
  if (jsonMode) {
    return JSON.stringify({ activeProjectId: db.activeProjectId, projects: db.projects }, null, 2);
  }
  let output = `\n📁 DAFTAR PROYEK VIDEO STUDIO (Active ID: ${db.activeProjectId})\n`;
  output += "=======================================================\n";
  db.projects.forEach(p => {
    const isActive = p.id === db.activeProjectId ? " [AKTIF]" : "";
    output += `ID: ${p.id}${isActive} | "${p.title}" | Aspect: ${p.aspectRatio} | Res: ${p.resolution} | Created: ${p.createdAt.substring(0, 10)}\n`;
  });
  return output;
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
  deleteProject
};
