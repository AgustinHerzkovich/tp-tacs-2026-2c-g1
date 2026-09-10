import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson } from "@/lib/backendProxy";

// Body is a raw ISO-8601 datetime JSON string (the chosen alternative slot),
// matching the backend's `@RequestBody LocalDateTime`.
export default function handler(req: NextApiRequest, res: NextApiResponse) {
  const { votationId } = req.query;
  return proxyJson(req, res, `/votations/${votationId}/votes/me`, { methods: ["PUT"] });
}
