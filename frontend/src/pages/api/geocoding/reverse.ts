import type { NextApiRequest, NextApiResponse } from "next";
import { fetchNominatim, NominatimError } from "@/lib/nominatim";

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
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    res.status(400).json({ error: "Coordenadas inválidas." });
    return;
  }

  try {
    const params = new URLSearchParams({ format: "jsonv2", "accept-language": "es", lat: String(latitude), lon: String(longitude) });
    const result = await fetchNominatim<NominatimReverseResult>("/reverse", params);
    res.status(200).json({ label: result.display_name });
  } catch (error) {
    const nominatimError = error instanceof NominatimError ? error : new NominatimError(502, "No se pudo consultar OpenStreetMap.");
    res.status(nominatimError.status).json({ error: nominatimError.message });
  }
}
