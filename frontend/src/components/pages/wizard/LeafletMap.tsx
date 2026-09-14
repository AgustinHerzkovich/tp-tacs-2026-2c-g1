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

const markerIcon = L.divIcon({
  className: "planazo-map-marker",
  html: '<span aria-hidden="true"></span>',
  iconSize: [30, 40],
  iconAnchor: [15, 40],
});

const draggableMarkerIcon = L.divIcon({
  className: "planazo-map-marker cursor-move",
  html: '<span aria-hidden="true"></span>',
  iconSize: [30, 40],
  iconAnchor: [15, 40],
});

function DraggableMarker({ position, onChange }: { position: [number, number]; onChange: (location: LocationOption) => void }) {
  const [resolving, setResolving] = useState(false);
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

        setResolving(true);
        try {
          const response = await fetch(`/api/geocoding/reverse?lat=${position.lat}&lon=${position.lng}`);
          const body = response.ok ? ((await response.json()) as { label: string }) : null;
          onChange({ 
            label: body?.label ?? `${position.lat.toFixed(5)}, ${position.lng.toFixed(5)}`, 
            latitude: position.lat, 
            longitude: position.lng 
          });
        } finally {
          setResolving(false);
        }
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

function MapController({ latitude, longitude, onChange, onUseCurrentLocation }: { latitude: number | null; longitude: number | null; onChange: (location: LocationOption) => void; onUseCurrentLocation?: () => void }) {
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

export default function LeafletMap({ latitude, longitude, onChange, onUseCurrentLocation }: { 
  latitude: number | null; 
  longitude: number | null; 
  onChange: (location: LocationOption) => void;
  onUseCurrentLocation?: () => void;
}) {
  const center: [number, number] = latitude !== null && longitude !== null ? [latitude, longitude] : DEFAULT_CENTER;
  const hasLocation = latitude !== null && longitude !== null;

  return (
    <div className="h-64 mt-3 rounded-2xl overflow-hidden border-2 relative z-0">
      <MapContainer center={center} zoom={13} scrollWheelZoom className="size-full" aria-label="Mapa de OpenStreetMap - hacé clic para seleccionar ubicación o arrastrá el marcador">
        <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MapController latitude={latitude} longitude={longitude} onChange={onChange} onUseCurrentLocation={onUseCurrentLocation} />
        {hasLocation && (
          <DraggableMarker 
            position={[latitude, longitude]} 
            onChange={onChange} 
          />
        )}
      </MapContainer>
    </div>
  );
}
