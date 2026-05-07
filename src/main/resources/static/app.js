const tabs = document.querySelectorAll(".tab");
const panels = document.querySelectorAll(".panel");

const userIdEl = document.getElementById("userId");
const sessionIdEl = document.getElementById("sessionId");
const newSessionBtn = document.getElementById("newSessionBtn");
const reloadSessionsBtn = document.getElementById("reloadSessionsBtn");

const ingestModeEl = document.getElementById("ingestMode");
const sourceNameWrap = document.getElementById("sourceNameWrap");
const textWrap = document.getElementById("textWrap");
const captionWrap = document.getElementById("captionWrap");
const fileWrap = document.getElementById("fileWrap");
const ingestForm = document.getElementById("ingestForm");
const ingestLog = document.getElementById("ingestLog");
const clearIngestLogBtn = document.getElementById("clearIngestLogBtn");

const chatForm = document.getElementById("chatForm");
const chatTimeline = document.getElementById("chatTimeline");
const clearChatBtn = document.getElementById("clearChatBtn");
const sessionListEl = document.getElementById("sessionList");
const deleteSessionBtn = document.getElementById("deleteSessionBtn");

const memoryForm = document.getElementById("memoryForm");
const memoryLog = document.getElementById("memoryLog");
const fillMemoryBtn = document.getElementById("fillMemoryBtn");

let selectedSessionId = "";

tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
        tabs.forEach((x) => x.classList.remove("active"));
        panels.forEach((p) => p.classList.remove("active"));
        tab.classList.add("active");
        document.getElementById(tab.dataset.tab).classList.add("active");
    });
});

newSessionBtn.addEventListener("click", async () => {
    try {
        const userId = (userIdEl.value || "").trim() || "u-demo";
        const data = await post(`/api/sessions/new?userId=${encodeURIComponent(userId)}`);
        userIdEl.value = data.userId || userId;
        sessionIdEl.value = data.sessionId || `s-${Date.now()}`;
        selectedSessionId = sessionIdEl.value;
        chatTimeline.innerHTML = "";
        await loadSessions();
    } catch (err) {
        alert(err.message || String(err));
    }
});

reloadSessionsBtn.addEventListener("click", async () => {
    await loadSessions();
});

userIdEl.addEventListener("change", loadSessions);
userIdEl.addEventListener("blur", loadSessions);

deleteSessionBtn.addEventListener("click", async () => {
    const userId = (userIdEl.value || "").trim();
    const sessionId = (sessionIdEl.value || "").trim();
    if (!userId || !sessionId) {
        alert("请先指定 userId 和 sessionId");
        return;
    }
    if (!confirm(`确认删除会话 ${sessionId} 吗？`)) {
        return;
    }
    try {
        await del(`/api/sessions/${encodeURIComponent(userId)}/${encodeURIComponent(sessionId)}`);
        if (selectedSessionId === sessionId) {
            chatTimeline.innerHTML = "";
            selectedSessionId = "";
        }
        await loadSessions();
    } catch (err) {
        alert(err.message || String(err));
    }
});

ingestModeEl.addEventListener("change", syncIngestModeUI);
syncIngestModeUI();

clearIngestLogBtn.addEventListener("click", () => {
    ingestLog.textContent = "";
});

ingestForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const mode = ingestModeEl.value;
        const metadata = collectDocMeta();
        let result;
        if (mode === "text") {
            const body = {
                docId: value("docId"),
                sourceName: value("sourceName"),
                content: value("textContent"),
                docType: metadata.docType,
                project: metadata.project,
                tags: metadata.tags
            };
            if (!body.content || !body.content.trim()) {
                throw new Error("纯文本模式下 content 不能为空");
            }
            result = await postJson("/api/ingest/text", body);
        } else if (mode === "file") {
            const file = document.getElementById("uploadFile").files[0];
            if (!file) {
                throw new Error("请选择要上传的文件");
            }
            const form = new FormData();
            form.append("file", file);
            appendIfNotBlank(form, "docId", value("docId"));
            appendIfNotBlank(form, "docType", metadata.docType);
            appendIfNotBlank(form, "project", metadata.project);
            appendIfNotBlank(form, "tags", metadata.tags);
            result = await postForm("/api/ingest/file", form);
        } else {
            const image = document.getElementById("uploadFile").files[0];
            if (!image) {
                throw new Error("图片模式下必须上传文件");
            }
            const form = new FormData();
            form.append("file", image);
            appendIfNotBlank(form, "docId", value("docId"));
            appendIfNotBlank(form, "caption", value("caption"));
            appendIfNotBlank(form, "docType", metadata.docType);
            appendIfNotBlank(form, "project", metadata.project);
            appendIfNotBlank(form, "tags", metadata.tags);
            result = await postForm("/api/ingest/image", form);
        }
        appendLog(ingestLog, "INGEST OK", result);
    } catch (error) {
        appendLog(ingestLog, "INGEST ERROR", error.message || String(error));
    }
});

chatForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const question = value("question");
        if (!question || !question.trim()) {
            throw new Error("问题不能为空");
        }
        const payload = {
            userId: (userIdEl.value || "").trim() || "u-demo",
            sessionId: (sessionIdEl.value || "").trim() || "s-demo",
            question,
            topK: Number(value("topK") || 6)
        };
        selectedSessionId = payload.sessionId;
        addMessage("user", question);
        const result = await postJson("/api/chat", payload);
        addAssistantMessage(result);
        chatForm.reset();
        document.getElementById("topK").value = "6";
        await loadSessions();
    } catch (error) {
        addSystemError(error.message || String(error));
    }
});

clearChatBtn.addEventListener("click", () => {
    chatTimeline.innerHTML = "";
});

memoryForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const content = value("memoryContent");
        if (!content || !content.trim()) {
            throw new Error("长期记忆内容不能为空");
        }
        const payload = {
            userId: (userIdEl.value || "").trim() || "u-demo",
            content
        };
        const result = await postJson("/api/memory/remember", payload);
        appendLog(memoryLog, "MEMORY OK", result);
    } catch (error) {
        appendLog(memoryLog, "MEMORY ERROR", error.message || String(error));
    }
});

fillMemoryBtn.addEventListener("click", () => {
    document.getElementById("memoryContent").value = "我是Java开发者，代码示例默认使用Java。";
});

async function loadSessions() {
    const userId = (userIdEl.value || "").trim();
    if (!userId) {
        sessionListEl.innerHTML = "";
        return;
    }
    try {
        const sessions = await getJson(`/api/sessions/${encodeURIComponent(userId)}?limit=50`);
        renderSessionList(Array.isArray(sessions) ? sessions : []);
    } catch (err) {
        sessionListEl.innerHTML = `<li class="session-sub" style="padding:12px;">加载失败: ${escapeHtml(err.message || String(err))}</li>`;
    }
}

function renderSessionList(sessions) {
    sessionListEl.innerHTML = "";
    if (!sessions.length) {
        sessionListEl.innerHTML = `<li class="session-sub" style="padding:12px;">暂无会话记录</li>`;
        return;
    }
    sessions.forEach((s) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = `session-item ${s.sessionId === selectedSessionId ? "active" : ""}`;
        btn.innerHTML = `
          <div class="session-title">${escapeHtml(s.sessionId || "")}</div>
          <div class="session-sub">轮次: ${s.turnCount ?? 0}</div>
          <div class="session-sub">${escapeHtml(shortText(s.lastUserMessage || "(无用户消息)", 42))}</div>
        `;
        btn.addEventListener("click", async () => {
            await replaySession(s.sessionId);
        });
        const li = document.createElement("li");
        li.appendChild(btn);
        sessionListEl.appendChild(li);
    });
}

async function replaySession(sessionId) {
    const userId = (userIdEl.value || "").trim();
    if (!userId || !sessionId) {
        return;
    }
    try {
        const data = await getJson(`/api/sessions/${encodeURIComponent(userId)}/${encodeURIComponent(sessionId)}/turns`);
        sessionIdEl.value = sessionId;
        selectedSessionId = sessionId;
        chatTimeline.innerHTML = "";
        const turns = Array.isArray(data.turns) ? data.turns : [];
        turns.forEach((t) => addHistoryMessage(t.role, t.content, t.createdAt));
        await loadSessions();
    } catch (err) {
        addSystemError(err.message || String(err));
    }
}

function syncIngestModeUI() {
    const mode = ingestModeEl.value;
    const isText = mode === "text";
    const isImage = mode === "image";
    sourceNameWrap.classList.toggle("hidden", !isText);
    textWrap.classList.toggle("hidden", !isText);
    fileWrap.classList.toggle("hidden", isText);
    captionWrap.classList.toggle("hidden", !isImage);
}

function collectDocMeta() {
    return {
        docType: value("docType"),
        project: value("project"),
        tags: value("tags")
    };
}

function value(id) {
    const el = document.getElementById(id);
    return el ? el.value : "";
}

function appendIfNotBlank(formData, key, val) {
    if (val && val.trim()) {
        formData.append(key, val.trim());
    }
}

async function postJson(url, body) {
    const res = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json; charset=UTF-8"},
        body: JSON.stringify(body)
    });
    return parseResponse(res);
}

async function postForm(url, formData) {
    const res = await fetch(url, {method: "POST", body: formData});
    return parseResponse(res);
}

async function post(url) {
    const res = await fetch(url, {method: "POST"});
    return parseResponse(res);
}

async function del(url) {
    const res = await fetch(url, {method: "DELETE"});
    return parseResponse(res);
}

async function getJson(url) {
    const res = await fetch(url, {method: "GET"});
    return parseResponse(res);
}

async function parseResponse(res) {
    const text = await res.text();
    const data = text ? tryJson(text) : {};
    if (!res.ok) {
        if (data && data.message) {
            throw new Error(data.message);
        }
        throw new Error(`HTTP ${res.status}: ${text || "request failed"}`);
    }
    return data;
}

function tryJson(text) {
    try {
        return JSON.parse(text);
    } catch (_) {
        return {raw: text};
    }
}

function appendLog(el, title, payload) {
    const prefix = `[${new Date().toLocaleString()}] ${title}\n`;
    const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
    el.textContent = `${prefix}${body}\n\n${el.textContent}`.trim();
}

function addMessage(role, content) {
    const item = document.createElement("article");
    item.className = `bubble ${role}`;
    item.innerHTML = `
      <div class="role">${role === "user" ? "用户" : "助手"}</div>
      <div>${escapeHtml(content)}</div>
    `;
    chatTimeline.prepend(item);
}

function addHistoryMessage(role, content, createdAt) {
    const item = document.createElement("article");
    const normalizedRole = (role || "").toLowerCase();
    item.className = `bubble ${normalizedRole === "assistant" ? "assistant" : "user"}`;
    item.innerHTML = `
      <div class="role">${escapeHtml(role || "unknown")} · ${escapeHtml(formatTime(createdAt))}</div>
      <div>${escapeHtml(content || "")}</div>
    `;
    chatTimeline.prepend(item);
}

function addAssistantMessage(result) {
    const item = document.createElement("article");
    item.className = "bubble assistant";
    const chunkIds = Array.isArray(result.citedChunkIds) ? result.citedChunkIds.join(", ") : "";
    const memoryHints = Array.isArray(result.memoryHints) ? result.memoryHints.join(" | ") : "";
    item.innerHTML = `
      <div class="role">助手</div>
      <div>${escapeHtml(result.answer || "")}</div>
      <div class="meta">
        <div><strong>改写问句:</strong> ${escapeHtml(result.rewrittenQuery || "")}</div>
        <div><strong>命中 Chunk:</strong> ${escapeHtml(chunkIds || "(无)")}</div>
        <div><strong>长期记忆召回:</strong> ${escapeHtml(memoryHints || "(无)")}</div>
      </div>
    `;
    chatTimeline.prepend(item);
}

function addSystemError(msg) {
    const item = document.createElement("article");
    item.className = "bubble";
    item.innerHTML = `
      <div class="role">系统</div>
      <div class="error">请求失败：${escapeHtml(msg)}</div>
    `;
    chatTimeline.prepend(item);
}

function shortText(text, maxLen) {
    if (!text || text.length <= maxLen) {
        return text;
    }
    return `${text.substring(0, maxLen)}...`;
}

function formatTime(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleString();
}

function escapeHtml(s) {
    return String(s)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;")
        .replaceAll("\n", "<br>");
}

loadSessions().catch(() => undefined);
