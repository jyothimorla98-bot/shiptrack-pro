import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { shipmentApi } from '../api/api';
import MapView from '../components/MapView';
import StatusBadge from '../components/StatusBadge';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import useStompSubscription from '../hooks/useLiveShipment';
import { formatDuration, timeAgo } from '../utils/format';

export default function LiveMap() {
  const [shipments, setShipments] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    shipmentApi
      .active()
      .then((data) => {
        setShipments(data);
        setSelectedId(data[0]?.id || null);
      })
      .finally(() => setLoading(false));
  }, []);

  const onFleetUpdate = useCallback((updated) => {
    setShipments((current) => {
      const index = current.findIndex((s) => s.id === updated.id);
      if (index === -1) return [...current, updated];
      const next = [...current];
      next[index] = updated;
      return next;
    });
  }, []);

  useStompSubscription('/topic/fleet', onFleetUpdate);

  if (loading) return <Spinner label="Loading the fleet" />;

  const selected = shipments.find((s) => s.id === selectedId);
  const markers = selected
    ? [
        selected.sender?.latitude && {
          latitude: selected.sender.latitude,
          longitude: selected.sender.longitude,
          label: `Pickup: ${selected.sender.city}`,
          kind: 'origin',
        },
        selected.receiver?.latitude && {
          latitude: selected.receiver.latitude,
          longitude: selected.receiver.longitude,
          label: `Delivery: ${selected.receiver.city}`,
          kind: 'destination',
        },
        selected.currentLocation && {
          ...selected.currentLocation,
          label: `${selected.trackingNumber} here`,
          kind: 'vehicle',
        },
      ].filter(Boolean)
    : shipments
        .filter((s) => s.currentLocation)
        .map((s) => ({ ...s.currentLocation, label: s.trackingNumber, kind: 'vehicle' }));

  return (
    <div className="space-y-5">
      <div>
        <h1 className="font-display text-2xl font-semibold">Live map</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Positions stream in over a websocket as drivers move. Pick a shipment to follow it.
        </p>
      </div>

      {shipments.length === 0 ? (
        <EmptyState
          title="Nothing is moving right now"
          body="Shipments appear here once they have been picked up."
          actionLabel="View all shipments"
          actionTo="/shipments"
        />
      ) : (
        <div className="grid gap-5 xl:grid-cols-[1fr_340px]">
          <MapView markers={markers} height={520} />

          <div className="card max-h-[520px] overflow-y-auto">
            <header className="border-b border-edge px-4 py-3">
              <h2 className="font-display font-semibold">On the road ({shipments.length})</h2>
            </header>
            <ul className="divide-y divide-edge">
              {shipments.map((shipment) => (
                <li key={shipment.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(shipment.id)}
                    className={`w-full px-4 py-3 text-left transition-colors ${
                      shipment.id === selectedId ? 'bg-brand-soft' : 'hover:bg-paper'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-medium">{shipment.trackingNumber}</span>
                      <StatusBadge status={shipment.status} />
                    </div>
                    <p className="mt-1 text-xs text-harbour/60">
                      {shipment.currentLocation?.label || 'Position pending'} to {shipment.receiver?.city}
                    </p>
                    <p className="mt-0.5 text-xs text-harbour/45">
                      Updated {timeAgo(shipment.updatedAt)}
                      {shipment.delayed ? ` - ${formatDuration(shipment.delayMinutes)} behind` : ''}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {selected && (
        <p className="text-sm">
          <Link to={`/shipments/${selected.id}`} className="text-brand hover:underline">
            Open {selected.trackingNumber}
          </Link>
        </p>
      )}
    </div>
  );
}
