// Every call to the Spring Boot backend goes through a route under
// `src/pages/api`, never directly from client components or hooks. That
// keeps `BACKEND_URL` server-side only, sidesteps CORS, and gives us one
// place to attach auth headers once real login is wired up.

import type { NextApiRequest, NextApiResponse } from "next";

const BACKEND_URL = process.env.BACKEND_URL || "http://localhost:8080";

function rawQueryString(req: NextApiRequest): string {
  const url = req.url ?? "";
  const idx = url.indexOf("?");
  return idx === -1 ? "" : url.slice(idx);
}

function forwardHeaders(req: NextApiRequest): Record<string, string> {
  const headers: Record<string, string> = {};
  const auth = req.headers.authorization;
  if (auth) headers.authorization = auth;
  return headers;
}

async function relayResponse(backendRes: Response, res: NextApiResponse): Promise<void> {
  const contentType = backendRes.headers.get("content-type") || "";
  res.status(backendRes.status);
  if (contentType) res.setHeader("Content-Type", contentType);

  if (contentType.includes("json")) {
    const data: unknown = await backendRes.json().catch(() => null);
    res.send(data === null ? "" : JSON.stringify(data));
    return;
  }
  const text = await backendRes.text();
  res.send(text);
}

function methodNotAllowed(res: NextApiResponse, methods: string[]): void {
  res.setHeader("Allow", methods.join(", "));
  res.status(405).json({ error: "Method not allowed" });
}

interface ProxyOptions {
  methods?: string[];
}

/**
 * Forwards a Next.js API request to the backend as JSON: same method, query
 * string, `Authorization` header, and — for methods other than GET/HEAD/DELETE
 * — the already-parsed `req.body` re-serialized as JSON.
 */
export async function proxyJson(
  req: NextApiRequest,
  res: NextApiResponse,
  backendPath: string,
  { methods }: ProxyOptions = {},
): Promise<void> {
  if (methods && req.method && !methods.includes(req.method)) {
    methodNotAllowed(res, methods);
    return;
  }

  const hasBody = !["GET", "HEAD", "DELETE"].includes(req.method ?? "") && req.body !== undefined && req.body !== "";
  const backendRes = await fetch(`${BACKEND_URL}${backendPath}${rawQueryString(req)}`, {
    method: req.method,
    headers: {
      ...forwardHeaders(req),
      ...(hasBody ? { "content-type": "application/json" } : {}),
    },
    body: hasBody ? JSON.stringify(req.body) : undefined,
  });
  await relayResponse(backendRes, res);
}

/**
 * Same as `proxyJson`, but streams the raw, unparsed request body through —
 * use it for `multipart/form-data` endpoints (activity creation with image
 * uploads). The route file must disable Next's body parser:
 * `export const config = { api: { bodyParser: false } }`.
 */
export async function proxyRaw(
  req: NextApiRequest,
  res: NextApiResponse,
  backendPath: string,
  { methods }: ProxyOptions = {},
): Promise<void> {
  if (methods && req.method && !methods.includes(req.method)) {
    methodNotAllowed(res, methods);
    return;
  }

  const backendRes = await fetch(`${BACKEND_URL}${backendPath}${rawQueryString(req)}`, {
    method: req.method,
    headers: {
      ...forwardHeaders(req),
      "content-type": req.headers["content-type"] ?? "",
    },
    body: req as unknown as ReadableStream,
    duplex: "half",
  } as RequestInit & { duplex: "half" });
  await relayResponse(backendRes, res);
}
