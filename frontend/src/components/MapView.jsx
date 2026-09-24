import { useEffect } from 'react';
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';

/** Leaflet's default icon URLs break under a bundler, so markers are drawn as small SVG pins. */
function pin(color) {
  return L.divIcon({
    className: '',
    html: `<span style="display:block;width:14px;height:14px;border-radius:50%;background:${color};box-shadow:0 0 0 3px rgba(255,255,255,.9),0 0 0 5px ${color}33"></span>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
  });
}

const PINS = {
  origin: pin('#0E2233'),
  destination: pin('#0B6E5B'),
  vehicle: pin('#C77A06'),
};

function FitBounds({ points }) {
  const map = useMap();
  useEffect(() => {
    const valid = points.filter((p) => p && Number.isFinite(p.latitude) && Number.isFinite(p.longitude));
    if (!valid.length) return;
    if (valid.length === 1) {
      map.setView([valid[0].latitude, valid[0].longitude], 11);
      return;
    }
    map.fitBounds(valid.map((p) => [p.latitude, p.longitude]), { padding: [40, 40] });
  }, [map, points]);
  return null;
}

/**
 * markers: [{ latitude, longitude, label, kind: 'origin' | 'destination' | 'vehicle' }]
 * path:    [{ latitude, longitude }] drawn as a dashed planned corridor
 */
export default function MapView({ markers = [], path = [], height = 380 }) {
  const points = [...markers, ...path];
  const centre = points.find((p) => p && Number.isFinite(p.latitude)) || { latitude: 20.5937, longitude: 78.9629 };

  return (
    <div style={{ height }} className="overflow-hidden rounded-lg border border-edge">
      <MapContainer center={[centre.latitude, centre.longitude]} zoom={5} scrollWheelZoom>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {path.length > 1 && (
          <Polyline
            positions={path.map((p) => [p.latitude, p.longitude])}
            pathOptions={{ color: '#0E2233', weight: 2, opacity: 0.45, dashArray: '6 8' }}
          />
        )}
        {markers
          .filter((m) => m && Number.isFinite(m.latitude) && Number.isFinite(m.longitude))
          .map((marker, index) => (
            <Marker
              key={`${marker.label}-${index}`}
              position={[marker.latitude, marker.longitude]}
              icon={PINS[marker.kind] || PINS.vehicle}
            >
              <Popup>{marker.label || 'Position'}</Popup>
            </Marker>
          ))}
        <FitBounds points={points} />
      </MapContainer>
    </div>
  );
}
