import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { trackApi } from '../api/api';
import { errorMessage } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import Timeline from '../components/Timeline';
import MapView from '../components/MapView';
import Spinner from '../components/Spinner';
import useStompSubscription from '../hooks/useLiveShipment';
import { formatDateTime, formatDuration } from '../utils/format';

export default function TrackShipment() {
  const { trackingNumber } = useParams();
  const navigate = useNavigate();
  const [query, setQuery] = useState(trackingNumber || '');
  const [data, setData] = useState(null);
  const [eta, setEta] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(Boolean(trackingNumber));

  const load = useCallback(async (number) => {
    setLoading(true);
    setError('');
    try {
      const [shipment, etaData] = await Promise.all([trackApi.track(number), trackApi.eta(number)]);
      setData(shipment);
      setEta(etaData);
    } catch (err) {
      setData(null);
      setError(errorMessage(err, 'No shipment matches that tracking number'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (trackingNumber) load(trackingNumber);
  }, [trackingNumber, load]);

  useStompSubscription(
    `/topic/shipments/${trackingNumber}`,
    () => load(trackingNumber),
    Boolean(trackingNumber && data),
  );

  const submit = (event) => {
    event.preventDefault();
    const value = query.trim().toUpperCase();
    if (value) navigate(`/track/${encodeURIComponent(value)}`);
  };

  const markers = data
    ? [data.currentLocation && { ...data.currentLocation, label: data.currentLocation.label || 'Current position', kind: 'vehicle' }].filter(
        Boolean,
      )
    : [];

  return (
    <div className="min-h-screen bg-paper">
      <header className="border-b border-edge bg-white px-6 py-4">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <Link to="/" className="font-display text-lg font-semibold">
            ShipTrack <span className="text-brand">Pro</span>
          </Link>
          <Link to="/login" className="text-sm text-brand hover:underline">
            Sign in
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        <h1 className="font-display text-3xl font-semibold">Track a shipment</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Enter the tracking number from your booking confirmation.
        </p>

        <form onSubmit={submit} className="mt-5 flex flex-col gap-2 sm:flex-row">
          <input
            className="input sm:flex-1"
            placeholder="STP-260920-ABC123"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button type="submit" className="btn-primary sm:w-36">
            Track
          </button>
        </form>

        {error && (
          <p className="mt-6 rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        {loading && <Spinner label="Finding your shipment" />}

        {data && !loading && (
          <div className="mt-8 space-y-6">
            <section className="card p-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-display text-xl font-semibold">{data.trackingNumber}</p>
                  <p className="mt-1 text-sm text-harbour/60">
                    {data.origin} to {data.destination}
                  </p>
                </div>
                <StatusBadge status={data.status} />
              </div>

              <dl className="mt-6 grid gap-4 border-t border-edge pt-5 sm:grid-cols-3">
                <div>
                  <dt className="text-sm text-harbour/55">
                    {data.actualDeliveryAt ? 'Delivered' : 'Expected delivery'}
                  </dt>
                  <dd className="mt-1 font-display text-lg">
                    {formatDateTime(data.actualDeliveryAt || data.estimatedDeliveryAt)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-harbour/55">Time remaining</dt>
                  <dd className="mt-1 font-display text-lg">
                    {data.actualDeliveryAt ? 'Complete' : formatDuration(eta?.remainingMinutes)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-harbour/55">Service</dt>
                  <dd className="mt-1 font-display text-lg capitalize">
                    {(data.serviceType || '').toLowerCase().replace('_', ' ')}
                  </dd>
                </div>
              </dl>

              {data.delayed && (
                <p className="mt-4 rounded-md border border-signal/30 bg-signal-soft px-3 py-2 text-sm text-signal">
                  Running about {formatDuration(data.delayMinutes)} behind the original promise.
                </p>
              )}
            </section>

            {markers.length > 0 && <MapView markers={markers} height={320} />}

            <section className="card p-6">
              <h2 className="font-display text-lg font-semibold">Tracking history</h2>
              <div className="mt-5">
                <Timeline events={data.timeline} />
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
