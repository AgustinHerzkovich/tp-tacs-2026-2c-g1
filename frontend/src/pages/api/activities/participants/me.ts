import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson } from "@/lib/backendProxy";

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  return proxyJson(req, res, "/activities/participants/me", { methods: ["GET"] });
}
