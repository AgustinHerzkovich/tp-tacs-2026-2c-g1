"use client";

import { useEffect, useState, useRef } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from "react-leaflet";
import type { LocationOption } from "@/components/pages/wizard/LocationMap";
import "leaflet/dist/leaflet.css";

const DEFAULT_CENTER: [number, number] = [-34.6037, -58.3816];
const MIN_LAT = -90;
const MAX_LAT = 90;
const MIN_LON = -180;
const MAX_LON = 180;

const draggableMarkerIcon = L.divIcon({
  className: "planazo-map-marker cursor-move",
  html: '<span aria-hidden="true"></span>',
  iconSize: [30, 40],
  iconAnchor: [15, 40],
});

function DraggableMarker({ position, onChange }: { position: [number, number]; onChange: (location: LocationOption) => void }) {
  const markerRef = useRef<L.Marker | null>(null);

  useEffect(() => {
    const marker = markerRef.current;
    if (marker) {
      marker.on("dragend", async (event: L.LeafletEvent) => {
        const leafletMarker = event.target as L.Marker;
        const position = leafletMarker.getLatLng();
        
        // Validate coordinates
        if (position.lat < MIN_LAT || position.lat > MAX_LAT || position.lng < MIN_LON || position.lng > MAX_LON) {
          return; // Invalid coordinates, ignore
        }

        const response = await fetch(`/api/geocoding/reverse?lat=${position.lat}&lon=${position.lng}`);
        const body = response.ok ? ((await response.json()) as { label: string }) : null;
        onChange({
          label: body?.label ?? `${position.lat.toFixed(5)}, ${position.lng.toFixed(5)}`,
          latitude: position.lat,
          longitude: position.lng
        });
      });
    }
    return () => {
      if (marker) {
        marker.off("dragend");
      }
    };
  }, [markerRef, onChange]);

  return (
    <Marker
      ref={markerRef}
      position={position}
      icon={draggableMarkerIcon}
      draggable={true}
      autoPan={true}
      aria-label="Marcador arrastrable - arrastrá para ajustar la ubicación"
    />
  );
}

function MapController({ latitude, longitude, onChange }: { latitude: number | null; longitude: number | null; onChange: (location: LocationOption) => void }) {
  const map = useMap();
  const [resolving, setResolving] = useState(false);

  useEffect(() => {
    if (latitude !== null && longitude !== null) map.flyTo([latitude, longitude], 15, { duration: 0.7 });
  }, [latitude, longitude, map]);

  useMapEvents({
    async click(event) {
      if (resolving) return;
      const { lat, lng } = event.latlng;
      
      // Validate coordinates
      if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LON || lng > MAX_LON) {
        return;
      }

      setResolving(true);
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

export default function LeafletMap({ latitude, longitude, onChange }: {
  latitude: number | null; 
  longitude: number | null; 
  onChange: (location: LocationOption) => void;
}) {
  const center: [number, number] = latitude !== null && longitude !== null ? [latitude, longitude] : DEFAULT_CENTER;
  const hasLocation = latitude !== null && longitude !== null;

  const moveMarker = async (latitudeDelta: number, longitudeDelta: number) => {
    if (latitude === null || longitude === null) return;
    const nextLatitude = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude + latitudeDelta));
    const nextLongitude = Math.max(MIN_LON, Math.min(MAX_LON, longitude + longitudeDelta));
    const response = await fetch(`/api/geocoding/reverse?lat=${nextLatitude}&lon=${nextLongitude}`);
    const body = response.ok ? ((await response.json()) as { label: string }) : null;
    onChange({ label: body?.label ?? `${nextLatitude.toFixed(5)}, ${nextLongitude.toFixed(5)}`, latitude: nextLatitude, longitude: nextLongitude });
  };

  return (
    <div className="h-64 mt-3 rounded-2xl overflow-hidden border-2 relative z-0">
      <MapContainer center={center} zoom={13} scrollWheelZoom className="size-full" aria-label="Mapa de OpenStreetMap - hacé clic para seleccionar ubicación o arrastrá el marcador">
        <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MapController latitude={latitude} longitude={longitude} onChange={onChange} />
        {hasLocation && (
          <DraggableMarker 
            position={[latitude, longitude]} 
            onChange={onChange} 
          />
        )}
      </MapContainer>
      {hasLocation && (
        <div className="absolute bottom-2 left-2 z-[500] grid grid-cols-3 gap-1 rounded-xl bg-white/95 p-1 shadow" aria-label="Ajustar marcador con teclado">
          <button type="button" className="col-start-2 size-8 rounded-lg font-bold transition-colors hover:bg-[var(--secondary)] hover:text-[var(--secondary-foreground)]" onClick={() => void moveMarker(0.0005, 0)} aria-label="Mover marcador al norte">↑</button>
          <button type="button" className="size-8 rounded-lg font-bold transition-colors hover:bg-[var(--secondary)] hover:text-[var(--secondary-foreground)]" onClick={() => void moveMarker(0, -0.0005)} aria-label="Mover marcador al oeste">←</button>
          <button type="button" className="size-8 rounded-lg font-bold transition-colors hover:bg-[var(--secondary)] hover:text-[var(--secondary-foreground)]" onClick={() => void moveMarker(-0.0005, 0)} aria-label="Mover marcador al sur">↓</button>
          <button type="button" className="size-8 rounded-lg font-bold transition-colors hover:bg-[var(--secondary)] hover:text-[var(--secondary-foreground)]" onClick={() => void moveMarker(0, 0.0005)} aria-label="Mover marcador al este">→</button>
        </div>
      )}
    </div>
  );
}
