"use client";

import { useEffect, useState } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from "react-leaflet";
import type { LocationOption } from "@/components/pages/wizard/LocationMap";
import "leaflet/dist/leaflet.css";

const DEFAULT_CENTER: [number, number] = [-34.6037, -58.3816];
const markerIcon = L.divIcon({
  className: "planazo-map-marker",
  html: '<span aria-hidden="true"></span>',
  iconSize: [30, 40],
  iconAnchor: [15, 40],
});

function MapController({ latitude, longitude, onChange }: { latitude: number | null; longitude: number | null; onChange: (location: LocationOption) => void }) {
  const map = useMap();
  const [resolving, setResolving] = useState(false);

  useEffect(() => {
    if (latitude !== null && longitude !== null) map.flyTo([latitude, longitude], 15, { duration: 0.7 });
  }, [latitude, longitude, map]);

  useMapEvents({
    async click(event) {
      if (resolving) return;
      setResolving(true);
      const { lat, lng } = event.latlng;
      try {
        const response = await fetch(`/api/geocoding/reverse?lat=${lat}&lon=${lng}`);
        const body = response.ok ? ((await response.json()) as { label: string }) : null;
        onChange({ label: body?.label ?? `${lat.toFixed(5)}, ${lng.toFixed(5)}`, latitude: lat, longitude: lng });
      } finally {
        setResolving(false);
      }
    },
  });

  return null;
}

export default function LeafletMap({ latitude, longitude, onChange }: { latitude: number | null; longitude: number | null; onChange: (location: LocationOption) => void }) {
  const center: [number, number] = latitude !== null && longitude !== null ? [latitude, longitude] : DEFAULT_CENTER;

  return (
    <div className="h-64 mt-3 rounded-2xl overflow-hidden border-2 relative z-0">
      <MapContainer center={center} zoom={13} scrollWheelZoom className="size-full" aria-label="Mapa de OpenStreetMap">
        <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MapController latitude={latitude} longitude={longitude} onChange={onChange} />
        {latitude !== null && longitude !== null && <Marker position={[latitude, longitude]} icon={markerIcon} />}
      </MapContainer>
    </div>
  );
}
