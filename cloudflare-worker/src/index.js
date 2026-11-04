const MODEL = "@cf/meta/llama-3.1-8b-instruct-fp8";
const MAX_MESSAGES = 20;
const MAX_MESSAGE_CHARS = 4000;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
  "X-Content-Type-Options": "nosniff",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: corsHeaders,
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    if (request.method === "GET" && url.pathname === "/") {
      return json({
        success: true,
        app: "JR AI",
        service: "Chat API",
        status: "online",
      });
    }

    if (request.method !== "POST" || url.pathname !== "/chat") {
      return json({ success: false, error: "Endpoint tidak ditemukan." }, 404);
    }

    // Baseline abuse protection: limit requests by source IP.
    // Cloudflare's rate limiter is approximate and local to each edge location.
    const ip = request.headers.get("cf-connecting-ip") || "unknown";
    const rate = await env.CHAT_RATE_LIMITER.limit({ key: ip });
    if (!rate.success) {
      return json({ success: false, error: "Terlalu banyak permintaan. Coba lagi satu menit." }, 429);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.toLowerCase().includes("application/json")) {
      return json({ success: false, error: "Content-Type harus application/json." }, 415);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 100_000) {
        return json({ success: false, error: "Pesan terlalu besar." }, 413);
      }
      body = JSON.parse(raw);
    } catch {
      return json({ success: false, error: "Format JSON tidak valid." }, 400);
    }

    if (!body || !Array.isArray(body.messages) || body.messages.length === 0) {
      return json({ success: false, error: "Pesan percakapan belum diisi." }, 400);
    }

    if (body.messages.length > MAX_MESSAGES) {
      return json({ success: false, error: "Terlalu banyak pesan dalam satu permintaan." }, 400);
    }

    const messages = [];
    for (const item of body.messages) {
      if (
        !item ||
        !["user", "assistant"].includes(item.role) ||
        typeof item.content !== "string" ||
        item.content.trim().length === 0 ||
        item.content.length > MAX_MESSAGE_CHARS
      ) {
        return json({ success: false, error: "Ada pesan yang formatnya tidak valid." }, 400);
      }
      messages.push({ role: item.role, content: item.content.trim() });
    }

    const systemMessage = {
      role: "system",
      content:
        "Kamu adalah JR AI, asisten pribadi yang ramah, cerdas, jujur, dan membantu. " +
        "Jawab dalam bahasa Indonesia secara alami kecuali pengguna meminta bahasa lain. " +
        "Berikan jawaban jelas dan praktis. Jangan mengaku telah melakukan tindakan yang belum dilakukan. " +
        "Jika tidak tahu, katakan dengan jujur.",
    };

    try {
      const result = await env.AI.run(MODEL, {
        messages: [systemMessage, ...messages],
        max_tokens: 1024,
        temperature: 0.7,
      });

      const reply = result?.response;
      if (typeof reply !== "string" || reply.trim().length === 0) {
        return json({ success: false, error: "Mesin AI belum memberikan jawaban." }, 502);
      }

      return json({ success: true, reply: reply.trim() });
    } catch {
      return json({
        success: false,
        error: "Layanan AI sedang tidak tersedia. Coba lagi beberapa saat.",
      }, 503);
    }
  },
};
