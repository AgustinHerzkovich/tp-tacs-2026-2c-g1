import type { NextApiRequest, NextApiResponse } from "next";
import { fetchNominatim, municipalityOf, NominatimError, type NominatimAddress } from "@/lib/nominatim";

interface NominatimResult {
  display_name: string;
  lat: string;
  lon: string;
  address?: NominatimAddress;
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

  try {
    const params = new URLSearchParams({ format: "jsonv2", limit: "5", "accept-language": "es", addressdetails: "1", q: query });
    const results = await fetchNominatim<NominatimResult[]>("/search", params);
    res.status(200).json(
      results.map((item) => ({
        label: item.display_name,
        latitude: Number(item.lat),
        longitude: Number(item.lon),
        city: municipalityOf(item.address),
      })),
    );
  } catch (error) {
    const nominatimError = error instanceof NominatimError ? error : new NominatimError(502, "No se pudo consultar OpenStreetMap.");
    res.status(nominatimError.status).json({ error: nominatimError.message });
  }
}
