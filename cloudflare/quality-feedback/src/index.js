const ALLOWED_CATEGORIES = new Set([
  "incorrect",
  "outdated",
  "unsafe",
  "did_not_follow_request",
  "poor_translation",
  "other",
]);

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

function cleanText(value, maxLength) {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

async function submitFeedback(request, env) {
  const length = Number(request.headers.get("content-length") || 0);
  if (length > 12_000) return json({ error: "Payload too large" }, 413);

  let input;
  try {
    input = await request.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const category = cleanText(input.category, 40);
  if (!ALLOWED_CATEGORIES.has(category)) {
    return json({ error: "Invalid category" }, 400);
  }

  const shareContent = input.shareContent === true;
  const prompt = shareContent ? cleanText(input.prompt, 2_000) : null;
  const response = shareContent ? cleanText(input.response, 4_000) : null;
  const appVersion = cleanText(input.appVersion, 80) || "unknown";
  const language = cleanText(input.language, 24) || "unknown";
  const responseLength = Math.max(0, Math.min(Number(input.responseLength) || 0, 100_000));

  await env.DB.prepare(
    `INSERT INTO quality_feedback
      (category, app_version, language, response_length, shared_prompt, shared_response)
     VALUES (?, ?, ?, ?, ?, ?)`,
  ).bind(category, appVersion, language, responseLength, prompt, response).run();

  return json({ accepted: true }, 202);
}

async function githubProxy(request, env, url) {
  if (!env.GITHUB_TOKEN) return json({ error: "Issue reporting unavailable" }, 503);
  const match = url.pathname.match(
    /^\/repos\/([^/]+)\/([^/]+)\/(issues(?:\/\d+(?:\/comments)?)?|contents\/feedback-assets\/([^/]+))$/,
  );
  if (!match || match[1] !== env.GITHUB_OWNER || match[2] !== env.GITHUB_REPO) {
    return json({ error: "Not found" }, 404);
  }

  const write = request.method === "POST" || request.method === "PUT";
  if (!write && request.method !== "GET") return json({ error: "Method not allowed" }, 405);
  const limiter = write ? env.WRITE_RATE_LIMITER : env.READ_RATE_LIMITER;
  const actor = request.headers.get("cf-connecting-ip") || "unknown";
  const rate = await limiter.limit({ key: `${actor}:${write ? "write" : "read"}` });
  if (!rate.success) return json({ error: "Too many requests" }, 429);

  const bodyLength = Number(request.headers.get("content-length") || 0);
  if (bodyLength > 6_000_000) return json({ error: "Payload too large" }, 413);

  let body;
  if (write) {
    try {
      const input = await request.json();
      if (url.pathname.includes("/contents/")) {
        const filename = match[4] || "";
        if (!/^[a-zA-Z0-9._-]+\.(png|jpg|jpeg)$/.test(filename)) {
          return json({ error: "Invalid attachment name" }, 400);
        }
        body = JSON.stringify({
          message: cleanText(input.message, 160) || "Add feedback attachment",
          content: cleanText(input.content, 5_500_000),
        });
      } else if (/\/issues$/.test(url.pathname)) {
        body = JSON.stringify({
          title: cleanText(input.title, 180),
          body: cleanText(input.body, 50_000),
          labels: ["feedback"],
        });
      } else {
        body = JSON.stringify({ body: cleanText(input.body, 50_000) });
      }
    } catch {
      return json({ error: "Invalid JSON" }, 400);
    }
  }

  const upstream = await fetch(`https://api.github.com${url.pathname}`, {
    method: request.method,
    headers: {
      accept: "application/vnd.github+json",
      authorization: `Bearer ${env.GITHUB_TOKEN}`,
      "content-type": "application/json; charset=utf-8",
      "user-agent": "Aria-Feedback-Worker/1.0",
      "x-github-api-version": "2022-11-28",
    },
    body,
  });
  const responseBody = await upstream.arrayBuffer();
  return new Response(responseBody, {
    status: upstream.status,
    headers: {
      "content-type": upstream.headers.get("content-type") || "application/json",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

async function trends(request, env) {
  const expected = `Bearer ${env.ADMIN_TOKEN || ""}`;
  if (!env.ADMIN_TOKEN || request.headers.get("authorization") !== expected) {
    return json({ error: "Unauthorized" }, 401);
  }

  const url = new URL(request.url);
  const days = Math.max(1, Math.min(Number(url.searchParams.get("days")) || 7, 90));
  const since = `-${days} days`;
  const totals = await env.DB.prepare(
    `SELECT COUNT(*) AS total,
            SUM(CASE WHEN shared_prompt IS NOT NULL OR shared_response IS NOT NULL THEN 1 ELSE 0 END) AS shared_content
       FROM quality_feedback
      WHERE created_at >= datetime('now', ?)`,
  ).bind(since).first();
  const categories = await env.DB.prepare(
    `SELECT category, COUNT(*) AS count
       FROM quality_feedback
      WHERE created_at >= datetime('now', ?)
      GROUP BY category
      ORDER BY count DESC, category ASC`,
  ).bind(since).all();
  const versions = await env.DB.prepare(
    `SELECT app_version AS version, COUNT(*) AS count
       FROM quality_feedback
      WHERE created_at >= datetime('now', ?)
      GROUP BY app_version
      ORDER BY count DESC
      LIMIT 10`,
  ).bind(since).all();

  return json({
    days,
    total: Number(totals?.total || 0),
    sharedContentReports: Number(totals?.shared_content || 0),
    categories: categories.results,
    versions: versions.results,
    generatedAt: new Date().toISOString(),
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ status: "ok" });
    }
    if (request.method === "POST" && url.pathname === "/v1/feedback") {
      const actor = request.headers.get("cf-connecting-ip") || "unknown";
      const rate = await env.WRITE_RATE_LIMITER.limit({ key: `${actor}:quality` });
      if (!rate.success) return json({ error: "Too many requests" }, 429);
      return submitFeedback(request, env);
    }
    if (request.method === "GET" && url.pathname === "/v1/trends") {
      return trends(request, env);
    }
    if (url.pathname.startsWith("/repos/")) {
      return githubProxy(request, env, url);
    }
    return json({ error: "Not found" }, 404);
  },
};
