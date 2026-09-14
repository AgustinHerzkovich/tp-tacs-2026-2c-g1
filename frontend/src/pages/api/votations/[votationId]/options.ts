import type { NextApiRequest, NextApiResponse } from "next";
import { proxyJson } from "@/lib/backendProxy";

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  const { votationId } = req.query;
  return proxyJson(req, res, `/votations/${votationId}/options`, { methods: ["PUT"] });
}
