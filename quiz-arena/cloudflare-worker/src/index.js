const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "content-type, authorization",
  "access-control-allow-methods": "GET, POST, OPTIONS",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: JSON_HEADERS });
}

function cleanText(value, max = 6000) {
  return String(value ?? "").replace(/\s+/g, " ").trim().slice(0, max);
}

async function proxyAppsScript(request, env) {
  if (!env.APPS_SCRIPT_URL) return json({ success: false, error: "APPS_SCRIPT_NOT_CONFIGURED" }, 503);
  const body = await request.text();
  const upstream = await fetch(env.APPS_SCRIPT_URL, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body,
    redirect: "follow",
  });
  return new Response(await upstream.arrayBuffer(), {
    status: upstream.status,
    headers: { ...JSON_HEADERS, "content-type": upstream.headers.get("content-type") || "application/json; charset=utf-8" },
  });
}

async function aiText(request, env) {
  if (!env.OPENAI_API_KEY) return json({ success: false, error: "OPENAI_NOT_CONFIGURED" }, 503);
  let p;
  try { p = await request.json(); } catch { return json({ success: false, error: "INVALID_JSON" }, 400); }
  const input = cleanText(p.input || p.prompt);
  if (!input) return json({ success: false, error: "INPUT_REQUIRED" }, 400);
  const system = cleanText(p.system || "You are the educational AI assistant for Quiz Arena. Reply clearly and concisely in the requested language.", 2000);
  const r = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: { "authorization": `Bearer ${env.OPENAI_API_KEY}`, "content-type": "application/json" },
    body: JSON.stringify({ model: env.OPENAI_TEXT_MODEL || "gpt-5-mini", instructions: system, input }),
  });
  const data = await r.json();
  if (!r.ok) return json({ success: false, error: "OPENAI_ERROR", status: r.status }, 502);
  const text = (data.output || []).flatMap(x => x.content || []).filter(x => x.type === "output_text").map(x => x.text).join("\n").trim();
  return json({ success: true, text, response_id: data.id });
}

async function aiSpeech(request, env) {
  if (!env.OPENAI_API_KEY) return json({ success: false, error: "OPENAI_NOT_CONFIGURED" }, 503);
  let p;
  try { p = await request.json(); } catch { return json({ success: false, error: "INVALID_JSON" }, 400); }
  const input = cleanText(p.input || p.text, 4000);
  if (!input) return json({ success: false, error: "INPUT_REQUIRED" }, 400);
  const r = await fetch("https://api.openai.com/v1/audio/speech", {
    method: "POST",
    headers: { "authorization": `Bearer ${env.OPENAI_API_KEY}`, "content-type": "application/json" },
    body: JSON.stringify({ model: env.OPENAI_TTS_MODEL || "gpt-4o-mini-tts", voice: p.voice || "onyx", input, format: "mp3" }),
  });
  if (!r.ok) return json({ success: false, error: "OPENAI_SPEECH_ERROR", status: r.status }, 502);
  return new Response(await r.arrayBuffer(), { status: 200, headers: { "content-type": "audio/mpeg", "cache-control": "private, max-age=3600", "access-control-allow-origin": "*" } });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: JSON_HEADERS });
    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health")) {
      return json({ success: true, service: "quiz-arena-backend", openai_configured: Boolean(env.OPENAI_API_KEY), apps_script_bridge: Boolean(env.APPS_SCRIPT_URL) });
    }
    if (request.method !== "POST") return json({ success: false, error: "METHOD_NOT_ALLOWED" }, 405);
    if (url.pathname === "/api/ai/respond") return aiText(request, env);
    if (url.pathname === "/api/ai/speech") return aiSpeech(request, env);
    if (url.pathname === "/api/arena" || url.pathname === "/") return proxyAppsScript(request, env);
    return json({ success: false, error: "NOT_FOUND" }, 404);
  },
};
