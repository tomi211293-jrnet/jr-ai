const MODEL = "@cf/meta/llama-3.1-8b-instruct-fp8";
const IMAGE_MODEL = "@cf/black-forest-labs/flux-1-schnell";
const MAX_MESSAGES = 20;
const MAX_MESSAGE_CHARS = 4000;
const MAX_IMAGE_PROMPT_CHARS = 1800;
const MAX_EDIT_IMAGE_CHARS = 2000000;

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

async function checkRateLimit(request, env) {
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  const rate = await env.CHAT_RATE_LIMITER.limit({ key: ip });
  return rate.success;
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
        service: "Chat and Image Generation API",
        status: "online",
      });
    }

    if (request.method !== "POST" || !["/chat", "/generate-image", "/edit-photo"].includes(url.pathname)) {
      return json({ success: false, error: "Endpoint tidak ditemukan." }, 404);
    }

    if (!(await checkRateLimit(request, env))) {
      return json({ success: false, error: "Terlalu banyak permintaan. Coba lagi satu menit." }, 429);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.toLowerCase().includes("application/json")) {
      return json({ success: false, error: "Content-Type harus application/json." }, 415);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 2_800_000) {
        return json({ success: false, error: "Permintaan terlalu besar." }, 413);
      }
      body = JSON.parse(raw);
    } catch {
      return json({ success: false, error: "Format JSON tidak valid." }, 400);
    }

    if (url.pathname === "/generate-image") {
      const prompt = typeof body?.prompt === "string" ? body.prompt.trim() : "";
      if (!prompt || prompt.length > MAX_IMAGE_PROMPT_CHARS) {
        return json({
          success: false,
          error: `Deskripsi gambar wajib diisi dan maksimal ${MAX_IMAGE_PROMPT_CHARS} karakter.`,
        }, 400);
      }

      try {
        const result = await env.AI.run(IMAGE_MODEL, {
          prompt,
          steps: 4,
        });
        if (typeof result?.image !== "string" || result.image.length === 0) {
          return json({ success: false, error: "Mesin gambar belum menghasilkan gambar." }, 502);
        }
        return json({
          success: true,
          image: result.image,
          mimeType: "image/jpeg",
        });
      } catch {
        return json({
          success: false,
          error: "Generator gambar sedang tidak tersedia. Periksa kuota Workers AI atau coba lagi nanti.",
        }, 503);
      }
    }


    if (url.pathname === "/edit-photo") {
      const prompt = typeof body?.prompt === "string" ? body.prompt.trim() : "";
      const image = typeof body?.image === "string" ? body.image.trim() : "";
      if (!prompt || prompt.length > 1200) {
        return json({ success: false, error: "Instruksi edit wajib diisi (maksimal 1.200 karakter)." }, 400);
      }
      if (!image || image.length > MAX_EDIT_IMAGE_CHARS) {
        return json({ success: false, error: "Foto tidak valid atau terlalu besar. Pilih foto yang lebih kecil." }, 400);
      }
      try {
        const result = await env.AI.run("@cf/runwayml/stable-diffusion-v1-5-img2img", {
          prompt,
          image_b64: image,
          num_steps: 12,
          strength: 0.65,
          guidance: 7.5,
        });
        const bytes = new Uint8Array(await new Response(result).arrayBuffer());
        if (bytes.length === 0) return json({ success: false, error: "Mesin AI belum menghasilkan foto." }, 502);
        let binary = "";
        for (let offset = 0; offset < bytes.length; offset += 0x8000) {
          binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
        }
        return json({ success: true, image: btoa(binary), mimeType: "image/png" });
      } catch {
        return json({ success: false, error: "Edit foto belum tersedia. Periksa kuota Workers AI atau coba lagi nanti." }, 503);
      }
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
