import type { NextApiRequest, NextApiResponse } from "next";

interface NominatimResult {
  display_name: string;
  lat: string;
  lon: string;
}

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const query = typeof req.query.q === "string" ? req.query.q.trim() : "";
  if (query.length < 3) {
    res.status(400).json({ error: "La búsqueda debe tener al menos 3 caracteres." });
    return;
  }

  const response = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&accept-language=es&q=${encodeURIComponent(query)}`, {
    headers: { "User-Agent": "Planazo-TACS/1.0 (educational project)" },
  });
  if (!response.ok) {
    res.status(502).json({ error: "No se pudo consultar OpenStreetMap." });
    return;
  }

  const results = (await response.json()) as NominatimResult[];
  res.status(200).json(results.map((item) => ({ label: item.display_name, latitude: Number(item.lat), longitude: Number(item.lon) })));
}
