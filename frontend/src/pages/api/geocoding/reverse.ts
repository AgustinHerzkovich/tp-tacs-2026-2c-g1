import type { NextApiRequest, NextApiResponse } from "next";

interface NominatimReverseResult {
  display_name: string;
}

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const latitude = Number(req.query.lat);
  const longitude = Number(req.query.lon);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    res.status(400).json({ error: "Coordenadas inválidas." });
    return;
  }

  const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&accept-language=es&lat=${latitude}&lon=${longitude}`, {
    headers: { "User-Agent": "Planazo-TACS/1.0 (educational project)" },
  });
  if (!response.ok) {
    res.status(502).json({ error: "No se pudo consultar OpenStreetMap." });
    return;
  }

  const result = (await response.json()) as NominatimReverseResult;
  res.status(200).json({ label: result.display_name });
}
