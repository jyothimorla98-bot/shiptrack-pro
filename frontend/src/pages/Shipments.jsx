import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { shipmentApi } from '../api/api';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/StatusBadge';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import { STATUSES, formatDateTime, statusLabel } from '../utils/format';

export default function Shipments() {
  const { isStaff } = useAuth();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    shipmentApi
      .list({ page, size: 10, ...(status ? { status } : {}) })
      .then(setData)
      .finally(() => setLoading(false));
  }, [page, status]);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold">Shipments</h1>
          <p className="mt-1 text-sm text-harbour/60">
            {isStaff ? 'Every shipment in the network.' : 'Everything you have booked.'}
          </p>
        </div>
        {!isStaff && (
          <Link to="/shipments/new" className="btn-primary">
            Book a shipment
          </Link>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => {
            setStatus('');
            setPage(0);
          }}
          className={`rounded-full border px-3 py-1 text-sm ${
            status === '' ? 'border-harbour bg-harbour text-white' : 'border-edge bg-white text-harbour/70'
          }`}
        >
          All
        </button>
        {STATUSES.map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => {
              setStatus(value);
              setPage(0);
            }}
            className={`rounded-full border px-3 py-1 text-sm ${
              status === value ? 'border-harbour bg-harbour text-white' : 'border-edge bg-white text-harbour/70'
            }`}
          >
            {statusLabel(value)}
          </button>
        ))}
      </div>

      {loading && <Spinner />}

      {!loading && data?.content.length === 0 && (
        <EmptyState
          title="Nothing matches this filter"
          body="Try a different status, or book a new shipment to get started."
          actionLabel="Book a shipment"
          actionTo="/shipments/new"
        />
      )}

      {!loading && data?.content.length > 0 && (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px]">
              <thead className="bg-paper">
                <tr>
                  <th className="table-head">Tracking number</th>
                  <th className="table-head">Route</th>
                  <th className="table-head">Service</th>
                  <th className="table-head">Status</th>
                  <th className="table-head">Expected</th>
                  <th className="table-head">Driver</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((shipment) => (
                  <tr key={shipment.id} className="hover:bg-paper/60">
                    <td className="table-cell">
                      <Link to={`/shipments/${shipment.id}`} className="font-medium hover:text-brand">
                        {shipment.trackingNumber}
                      </Link>
                      {shipment.delayed && (
                        <span className="ml-2 text-xs font-medium text-danger">late</span>
                      )}
                    </td>
                    <td className="table-cell text-harbour/70">
                      {shipment.sender?.city} to {shipment.receiver?.city}
                    </td>
                    <td className="table-cell capitalize text-harbour/70">
                      {(shipment.serviceType || '').toLowerCase().replace('_', ' ')}
                    </td>
                    <td className="table-cell">
                      <StatusBadge status={shipment.status} />
                    </td>
                    <td className="table-cell text-harbour/70">
                      {formatDateTime(shipment.actualDeliveryAt || shipment.estimatedDeliveryAt)}
                    </td>
                    <td className="table-cell text-harbour/70">
                      {shipment.assignedDriverName || 'Unassigned'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
