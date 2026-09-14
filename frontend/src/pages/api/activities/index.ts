import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson, proxyRaw } from "@/lib/backendProxy";

// POST is multipart/form-data (activity JSON part + optional image parts),
// so Next's default body parser must stay off for this route.
export const config = { api: { bodyParser: false } };

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === "POST") return proxyRaw(req, res, "/activities", { methods: ["POST"] });
  return proxyJson(req, res, "/activities", { methods: ["GET", "POST"] });
}
