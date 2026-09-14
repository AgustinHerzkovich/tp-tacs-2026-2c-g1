import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson } from "@/lib/backendProxy";

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  const { id } = req.query;
  return proxyJson(req, res, `/activities/${id}/weather`, { methods: ["GET"] });
}
