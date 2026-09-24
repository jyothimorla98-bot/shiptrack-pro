import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { analyticsApi, shipmentApi } from '../api/api';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { formatDateTime, formatDuration } from '../utils/format';

export default function Dashboard() {
  const { user, isStaff } = useAuth();
  const [summary, setSummary] = useState(null);
  const [recent, setRecent] = useState([]);
  const [delayed, setDelayed] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [summaryData, page] = await Promise.all([
          isStaff ? analyticsApi.admin().catch(() => analyticsApi.business()) : analyticsApi.customer(),
          shipmentApi.list({ page: 0, size: 6 }),
        ]);
        setSummary(summaryData);
        setRecent(page.content);
        if (isStaff) {
          setDelayed(await shipmentApi.delayed().catch(() => []));
        }
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [isStaff]);

  if (loading) return <Spinner label="Building your dashboard" />;

  const firstName = user.fullName.split(' ')[0];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold">Good to see you, {firstName}</h1>
          <p className="mt-1 text-sm text-harbour/60">
            {isStaff
              ? 'Network status across every shipment in the system.'
              : 'Where your parcels are right now.'}
          </p>
        </div>
        {!isStaff && (
          <Link to="/shipments/new" className="btn-primary">
            Book a shipment
          </Link>
        )}
      </div>

      {isStaff ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Shipments in flight" value={summary.activeShipments} tone="signal" />
          <StatCard label="Delivered" value={summary.delivered} tone="brand" />
          <StatCard
            label="Running late"
            value={summary.delayed}
            tone={summary.delayed > 0 ? 'danger' : 'default'}
          />
          <StatCard
            label="On-time rate"
            value={`${summary.onTimeRatePercent}%`}
            hint="Against the promise given at booking"
          />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Active shipments" value={summary.activeShipments} tone="signal" />
          <StatCard label="In transit" value={summary.inTransit} />
          <StatCard label="Delivered" value={summary.deliveredShipments} tone="brand" />
          <StatCard
            label="Needs attention"
            value={summary.delayed}
            tone={summary.delayed > 0 ? 'danger' : 'default'}
            hint={summary.delayed > 0 ? 'Running behind schedule' : 'Everything on schedule'}
          />
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <section className="card overflow-hidden">
          <header className="flex items-center justify-between px-4 py-3">
            <h2 className="font-display font-semibold">Recent shipments</h2>
            <Link to="/shipments" className="text-sm text-brand hover:underline">
              View all
            </Link>
          </header>

          {recent.length === 0 ? (
            <div className="px-4 pb-6">
              <EmptyState
                title="No shipments yet"
                body="Book your first shipment and it will show up here with live tracking."
                actionLabel="Book a shipment"
                actionTo="/shipments/new"
              />
            </div>
          ) : (
            <table className="w-full">
              <thead className="bg-paper">
                <tr>
                  <th className="table-head">Tracking</th>
                  <th className="table-head">Destination</th>
                  <th className="table-head">Status</th>
                  <th className="table-head">Expected</th>
                </tr>
              </thead>
              <tbody>
                {recent.map((shipment) => (
                  <tr key={shipment.id} className="hover:bg-paper/60">
                    <td className="table-cell font-medium">
                      <Link to={`/shipments/${shipment.id}`} className="hover:text-brand">
                        {shipment.trackingNumber}
                      </Link>
                    </td>
                    <td className="table-cell">{shipment.receiver?.city}</td>
                    <td className="table-cell">
                      <StatusBadge status={shipment.status} />
                    </td>
                    <td className="table-cell text-harbour/70">
                      {formatDateTime(shipment.actualDeliveryAt || shipment.estimatedDeliveryAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="card p-4">
          <h2 className="font-display font-semibold">
            {isStaff ? 'Delay watchlist' : 'What happens next'}
          </h2>

          {isStaff ? (
            delayed.length === 0 ? (
              <p className="mt-3 text-sm text-harbour/60">
                Nothing is running behind. The watchlist fills up when an ETA slips past the promise.
              </p>
            ) : (
              <ul className="mt-3 divide-y divide-edge">
                {delayed.slice(0, 6).map((shipment) => (
                  <li key={shipment.id} className="py-3">
                    <Link to={`/shipments/${shipment.id}`} className="text-sm font-medium hover:text-brand">
                      {shipment.trackingNumber}
                    </Link>
                    <p className="mt-0.5 text-xs text-danger">
                      {formatDuration(shipment.delayMinutes)} behind, heading to {shipment.receiver?.city}
                    </p>
                  </li>
                ))}
              </ul>
            )
          ) : (
            <ol className="mt-3 space-y-3 text-sm text-harbour/70">
              <li>
                <span className="font-medium text-harbour">Book</span> a shipment with pickup and delivery
                addresses.
              </li>
              <li>
                <span className="font-medium text-harbour">Follow</span> the driver on the live map once the
                parcel is picked up.
              </li>
              <li>
                <span className="font-medium text-harbour">Collect</span> the signed proof of delivery from
                the shipment page.
              </li>
            </ol>
          )}
        </section>
      </div>
    </div>
  );
}
