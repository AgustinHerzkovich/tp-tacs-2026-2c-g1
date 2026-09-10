import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson } from "@/lib/backendProxy";

// Requires a JWT with ROLE_ADMIN; the Authorization header is forwarded as-is.
export default function handler(req: NextApiRequest, res: NextApiResponse) {
  return proxyJson(req, res, "/statistics", { methods: ["GET"] });
}
