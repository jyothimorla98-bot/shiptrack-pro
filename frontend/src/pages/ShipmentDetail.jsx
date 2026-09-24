import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { podApi, routeApi, shipmentApi, userApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/StatusBadge';
import Timeline from '../components/Timeline';
import MapView from '../components/MapView';
import Spinner from '../components/Spinner';
import SignaturePad from '../components/SignaturePad';
import useStompSubscription from '../hooks/useLiveShipment';
import { formatDateTime, formatDuration, statusLabel } from '../utils/format';

export default function ShipmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isStaff, isAdmin } = useAuth();

  const [shipment, setShipment] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [eta, setEta] = useState(null);
  const [route, setRoute] = useState(null);
  const [pod, setPod] = useState(null);
  const [drivers, setDrivers] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [podForm, setPodForm] = useState({ receivedByName: '', relationship: '', notes: '', signatureImage: '' });
  const [showPodForm, setShowPodForm] = useState(false);

  const load = useCallback(async () => {
    const [shipmentData, timelineData, etaData] = await Promise.all([
      shipmentApi.get(id),
      shipmentApi.timeline(id),
      shipmentApi.eta(id).catch(() => null),
    ]);
    setShipment(shipmentData);
    setTimeline(timelineData);
    setEta(etaData);
    setRoute(await routeApi.forShipment(id).catch(() => null));
    setPod(await podApi.byShipment(id).catch(() => null));
  }, [id]);

  useEffect(() => {
    load().finally(() => setLoading(false));
  }, [load]);

  useEffect(() => {
    if (isStaff) userApi.drivers().then(setDrivers).catch(() => {});
  }, [isStaff]);

  useStompSubscription(
    shipment ? `/topic/shipments/${shipment.trackingNumber}` : null,
    () => load(),
    Boolean(shipment),
  );

  const run = async (action, successMessage) => {
    setError('');
    setMessage('');
    try {
      await action();
      await load();
      setMessage(successMessage);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (loading) return <Spinner label="Opening shipment" />;
  if (!shipment) return null;

  const markers = [
    shipment.sender?.latitude && {
      latitude: shipment.sender.latitude,
      longitude: shipment.sender.longitude,
      label: `Pickup: ${shipment.sender.city}`,
      kind: 'origin',
    },
    shipment.receiver?.latitude && {
      latitude: shipment.receiver.latitude,
      longitude: shipment.receiver.longitude,
      label: `Delivery: ${shipment.receiver.city}`,
      kind: 'destination',
    },
    shipment.currentLocation && {
      ...shipment.currentLocation,
      label: shipment.currentLocation.label || 'Current position',
      kind: 'vehicle',
    },
  ].filter(Boolean);

  const capturePod = async (event) => {
    event.preventDefault();
    await run(
      () =>
        podApi.capture(shipment.id, {
          ...podForm,
          latitude: shipment.receiver?.latitude ?? null,
          longitude: shipment.receiver?.longitude ?? null,
        }),
      'Proof of delivery saved and the shipment is now delivered',
    );
    setShowPodForm(false);
    setPodForm({ receivedByName: '', relationship: '', notes: '', signatureImage: '' });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-harbour/55">Tracking number</p>
          <h1 className="font-display text-2xl font-semibold">{shipment.trackingNumber}</h1>
          <p className="mt-1 text-sm text-harbour/60">
            {shipment.sender?.shortText || shipment.sender?.city} to{' '}
            {shipment.receiver?.shortText || shipment.receiver?.city}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={shipment.status} />
          {shipment.delayed && (
            <span className="rounded border border-danger/30 bg-danger-soft px-2 py-0.5 text-xs text-danger">
              {formatDuration(shipment.delayMinutes)} behind
            </span>
          )}
        </div>
      </div>

      {message && (
        <p className="rounded-md border border-brand/30 bg-brand-soft px-3 py-2 text-sm text-brand-dark">
          {message}
        </p>
      )}
      {error && (
        <p className="rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="card px-4 py-4">
          <p className="text-sm text-harbour/55">
            {shipment.actualDeliveryAt ? 'Delivered at' : 'Expected delivery'}
          </p>
          <p className="mt-1 font-display text-lg">
            {formatDateTime(shipment.actualDeliveryAt || shipment.estimatedDeliveryAt)}
          </p>
        </div>
        <div className="card px-4 py-4">
          <p className="text-sm text-harbour/55">Time remaining</p>
          <p className="mt-1 font-display text-lg">
            {shipment.actualDeliveryAt ? 'Complete' : formatDuration(eta?.remainingMinutes)}
          </p>
        </div>
        <div className="card px-4 py-4">
          <p className="text-sm text-harbour/55">Distance</p>
          <p className="mt-1 font-display text-lg">{shipment.distanceKm ?? 0} km</p>
        </div>
        <div className="card px-4 py-4">
          <p className="text-sm text-harbour/55">Driver</p>
          <p className="mt-1 font-display text-lg">{shipment.assignedDriverName || 'Unassigned'}</p>
        </div>
      </div>

      <MapView markers={markers} path={route?.waypoints || []} height={340} />

      {eta && (
        <p className="text-sm text-harbour/60">
          {eta.explanation} Confidence {Math.round(eta.confidence * 100)}%.
        </p>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
        <section className="card p-6">
          <h2 className="font-display text-lg font-semibold">Tracking history</h2>
          <div className="mt-5">
            <Timeline events={timeline} />
          </div>
        </section>

        <div className="space-y-6">
          <section className="card p-6">
            <h2 className="font-display text-lg font-semibold">Shipment details</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-harbour/55">Contents</dt>
                <dd className="text-right">{shipment.packageDetails?.description}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-harbour/55">Pieces and weight</dt>
                <dd className="text-right">
                  {shipment.packageDetails?.quantity} piece(s), {shipment.packageDetails?.weightKg} kg
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-harbour/55">Service</dt>
                <dd className="text-right capitalize">
                  {(shipment.serviceType || '').toLowerCase().replace('_', ' ')}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-harbour/55">Recipient</dt>
                <dd className="text-right">
                  {shipment.receiver?.contactName}
                  <br />
                  <span className="text-harbour/55">{shipment.receiver?.contactPhone}</span>
                </dd>
              </div>
              {shipment.notes && (
                <div className="flex justify-between gap-4">
                  <dt className="text-harbour/55">Instructions</dt>
                  <dd className="text-right">{shipment.notes}</dd>
                </div>
              )}
            </dl>
          </section>

          {pod && (
            <section className="card p-6">
              <h2 className="font-display text-lg font-semibold">Proof of delivery</h2>
              <p className="mt-2 text-sm">
                Signed for by {pod.receivedByName}
                {pod.relationship ? ` (${pod.relationship})` : ''} on {formatDateTime(pod.deliveredAt)}.
              </p>
              <p className="mt-1 text-sm text-harbour/60">
                {pod.verified ? `Verified by ${pod.verifiedBy}` : 'Awaiting verification by support'}
              </p>
              {pod.signatureImage && (
                <img
                  src={pod.signatureImage}
                  alt="Recipient signature"
                  className="mt-4 rounded border border-edge bg-white"
                />
              )}
              {pod.photoUrls?.length > 0 && (
                <div className="mt-3 grid grid-cols-3 gap-2">
                  {pod.photoUrls.map((url) => (
                    <img key={url} src={url} alt="Delivery" className="rounded border border-edge" />
                  ))}
                </div>
              )}
            </section>
          )}

          {isStaff && (
            <section className="card p-6">
              <h2 className="font-display text-lg font-semibold">Operations</h2>

              {shipment.allowedNextStatuses?.length > 0 && (
                <div className="mt-4">
                  <p className="label">Move to next stage</p>
                  <div className="flex flex-wrap gap-2">
                    {shipment.allowedNextStatuses.map((next) => (
                      <button
                        key={next}
                        type="button"
                        className="btn-ghost px-3 py-1.5"
                        onClick={() =>
                          run(
                            () => shipmentApi.updateStatus(shipment.id, { status: next }),
                            `Shipment marked ${statusLabel(next).toLowerCase()}`,
                          )
                        }
                      >
                        {statusLabel(next)}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-5">
                <label className="label" htmlFor="driver">
                  Assign a driver
                </label>
                <select
                  id="driver"
                  className="input"
                  value={shipment.assignedDriverId || ''}
                  onChange={(event) =>
                    run(
                      () => shipmentApi.assignDriver(shipment.id, event.target.value),
                      'Driver assigned',
                    )
                  }
                >
                  <option value="" disabled>
                    Choose an operator
                  </option>
                  {drivers.map((driver) => (
                    <option key={driver.id} value={driver.id}>
                      {driver.fullName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="mt-5 flex flex-wrap gap-2">
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => run(() => routeApi.optimize(shipment.id), 'Route optimised')}
                >
                  Optimise route
                </button>
                {!pod && shipment.status !== 'CANCELLED' && (
                  <button type="button" className="btn-primary" onClick={() => setShowPodForm((open) => !open)}>
                    {showPodForm ? 'Close' : 'Record delivery'}
                  </button>
                )}
              </div>

              {showPodForm && (
                <form onSubmit={capturePod} className="mt-5 space-y-3 border-t border-edge pt-5">
                  <div>
                    <label className="label">Received by</label>
                    <input
                      required
                      className="input"
                      value={podForm.receivedByName}
                      onChange={(event) => setPodForm({ ...podForm, receivedByName: event.target.value })}
                    />
                  </div>
                  <div>
                    <label className="label">Relationship to recipient</label>
                    <input
                      className="input"
                      placeholder="Self, neighbour, building security"
                      value={podForm.relationship}
                      onChange={(event) => setPodForm({ ...podForm, relationship: event.target.value })}
                    />
                  </div>
                  <div>
                    <label className="label">Signature</label>
                    <SignaturePad onChange={(value) => setPodForm({ ...podForm, signatureImage: value })} />
                  </div>
                  <div>
                    <label className="label">Notes</label>
                    <textarea
                      className="input"
                      value={podForm.notes}
                      onChange={(event) => setPodForm({ ...podForm, notes: event.target.value })}
                    />
                  </div>
                  <button type="submit" className="btn-primary w-full">
                    Save proof of delivery
                  </button>
                </form>
              )}
            </section>
          )}

          <section className="card p-6">
            <h2 className="font-display text-lg font-semibold">Manage</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              {!['DELIVERED', 'CANCELLED'].includes(shipment.status) && (
                <button
                  type="button"
                  className="btn-danger"
                  onClick={() => {
                    const reason = window.prompt('Why is this shipment being cancelled?');
                    if (reason !== null) {
                      run(() => shipmentApi.cancel(shipment.id, reason), 'Shipment cancelled');
                    }
                  }}
                >
                  Cancel shipment
                </button>
              )}
              {isAdmin && (
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={async () => {
                    if (window.confirm('Remove this shipment and its history for good?')) {
                      await shipmentApi.remove(shipment.id);
                      navigate('/shipments');
                    }
                  }}
                >
                  Delete permanently
                </button>
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
