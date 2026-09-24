import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { podApi } from '../api/api';
import { errorMessage } from '../api/client';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import { formatDateTime, timeAgo } from '../utils/format';

function PodCard({ pod, onVerify, busy }) {
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');

  return (
    <div className="card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link
            to={`/shipments/${pod.shipmentId}`}
            className="font-display text-lg font-semibold hover:text-brand"
          >
            {pod.trackingNumber}
          </Link>
          <p className="mt-1 text-sm text-harbour/60">
            Received by {pod.receivedByName}
            {pod.relationship ? ` (${pod.relationship})` : ''} · {timeAgo(pod.deliveredAt)}
          </p>
        </div>
        <span className="rounded border border-signal/30 bg-signal-soft px-2 py-0.5 text-xs font-medium text-signal">
          Awaiting verification
        </span>
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-[220px_1fr]">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-harbour/45">Signature</p>
          {pod.signatureImage ? (
            <img
              src={pod.signatureImage}
              alt={`Signature of ${pod.receivedByName}`}
              className="mt-2 w-full rounded border border-edge bg-white"
            />
          ) : (
            <p className="mt-2 text-sm text-harbour/50">Not captured</p>
          )}
        </div>

        <div className="space-y-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-harbour/45">
              Delivery evidence
            </p>
            {pod.photoUrls?.length ? (
              <div className="mt-2 flex flex-wrap gap-2">
                {pod.photoUrls.map((url) => (
                  <a key={url} href={url} target="_blank" rel="noreferrer">
                    <img
                      src={url}
                      alt="Delivery evidence"
                      className="h-20 w-20 rounded border border-edge object-cover"
                    />
                  </a>
                ))}
              </div>
            ) : (
              <p className="mt-2 text-sm text-harbour/50">No photos uploaded</p>
            )}
          </div>

          <dl className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
            <div className="flex justify-between gap-3 sm:block">
              <dt className="text-harbour/50">Delivered at</dt>
              <dd>{formatDateTime(pod.deliveredAt)}</dd>
            </div>
            <div className="flex justify-between gap-3 sm:block">
              <dt className="text-harbour/50">Captured by</dt>
              <dd>{pod.capturedBy || '--'}</dd>
            </div>
            {pod.deliveryLocation && (
              <div className="flex justify-between gap-3 sm:block">
                <dt className="text-harbour/50">Location</dt>
                <dd className="tabular-nums">
                  {pod.deliveryLocation.latitude?.toFixed(4)}, {pod.deliveryLocation.longitude?.toFixed(4)}
                </dd>
              </div>
            )}
          </dl>

          {pod.notes && <p className="text-sm text-harbour/70">Driver note: {pod.notes}</p>}
        </div>
      </div>

      {rejecting ? (
        <div className="mt-4 space-y-2 border-t border-edge pt-4">
          <label className="label" htmlFor={`reason-${pod.id}`}>
            Why is this record being rejected?
          </label>
          <textarea
            id={`reason-${pod.id}`}
            className="input"
            rows={2}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Signature does not match the recipient on file"
          />
          <div className="flex gap-2">
            <button
              type="button"
              className="btn-danger"
              disabled={busy || !reason.trim()}
              onClick={() => onVerify(pod.id, false, reason.trim())}
            >
              Confirm rejection
            </button>
            <button type="button" className="btn-ghost" onClick={() => setRejecting(false)}>
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div className="mt-4 flex gap-2 border-t border-edge pt-4">
          <button
            type="button"
            className="btn-primary"
            disabled={busy}
            onClick={() => onVerify(pod.id, true, null)}
          >
            Approve delivery
          </button>
          <button type="button" className="btn-ghost" onClick={() => setRejecting(true)}>
            Reject
          </button>
        </div>
      )}
    </div>
  );
}

export default function PodQueue() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [flash, setFlash] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    podApi
      .pending({ page, size: 10 })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(load, [load]);

  const verify = async (podId, approved, rejectionReason) => {
    setBusy(true);
    setError('');
    try {
      await podApi.verify(podId, { approved, rejectionReason });
      setFlash(approved ? 'Delivery approved.' : 'Record sent back to the operator.');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-5">
      <div>
        <h1 className="font-display text-2xl font-semibold">Proof of delivery queue</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Check the signature and photos against the shipment before you approve.
        </p>
      </div>

      {flash && (
        <p className="rounded border border-brand/25 bg-brand-soft px-3 py-2 text-sm text-brand-dark">
          {flash}
        </p>
      )}
      {error && (
        <p className="rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <Spinner label="Loading pending records" />}

      {!loading && data?.content.length === 0 && (
        <EmptyState
          title="Queue is clear"
          body="Every captured proof of delivery has been verified. New records appear here as drivers complete deliveries."
        />
      )}

      {!loading && data?.content.length > 0 && (
        <>
          <div className="space-y-4">
            {data.content.map((pod) => (
              <PodCard key={pod.id} pod={pod} onVerify={verify} busy={busy} />
            ))}
          </div>
          <div className="card">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        </>
      )}
    </div>
  );
}
