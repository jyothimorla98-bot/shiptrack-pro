import { useEffect, useMemo, useState } from 'react';
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { analyticsApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/StatCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { formatDuration, statusLabel } from '../utils/format';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Filler,
  Tooltip,
  Legend,
);

const INK = '#0E2233';
const BRAND = '#0B6E5B';
const SIGNAL = '#C77A06';
const DANGER = '#A83A2B';
const EDGE = '#E2E0D8';

const STATUS_COLOURS = {
  CREATED: '#9AA7B1',
  PICKED_UP: '#4F9C8B',
  IN_TRANSIT: SIGNAL,
  OUT_FOR_DELIVERY: '#E0A33C',
  DELIVERED: BRAND,
  FAILED_DELIVERY: DANGER,
  CANCELLED: '#C4C1B6',
};

const baseOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: INK, font: { family: 'Inter, sans-serif', size: 12 }, boxWidth: 12 } },
    tooltip: { backgroundColor: INK, padding: 10, cornerRadius: 6 },
  },
  scales: {
    x: { grid: { display: false }, ticks: { color: 'rgba(14,34,51,0.55)' } },
    y: { beginAtZero: true, grid: { color: EDGE }, ticks: { color: 'rgba(14,34,51,0.55)', precision: 0 } },
  },
};

function Panel({ title, subtitle, children, height = 280 }) {
  return (
    <div className="card p-5">
      <h2 className="font-display text-lg font-semibold">{title}</h2>
      {subtitle && <p className="mt-0.5 text-sm text-harbour/55">{subtitle}</p>}
      <div className="mt-4" style={{ height }}>
        {children}
      </div>
    </div>
  );
}

export default function Analytics() {
  const { isStaff, isAdmin, isBusiness } = useAuth();
  const [summary, setSummary] = useState(null);
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        let data;
        if (isAdmin) data = await analyticsApi.admin();
        else if (isStaff || isBusiness) data = await analyticsApi.business();
        else data = await analyticsApi.customer();
        setSummary(data);
        if (isStaff) setRoutes(await analyticsApi.routes().catch(() => []));
      } catch (err) {
        setError(errorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [isAdmin, isStaff, isBusiness]);

  const trendData = useMemo(() => {
    const trend = summary?.trend || [];
    return {
      labels: trend.map((point) => point.date),
      datasets: [
        {
          label: 'Created',
          data: trend.map((point) => point.created),
          borderColor: SIGNAL,
          backgroundColor: 'rgba(199,122,6,0.12)',
          fill: true,
          tension: 0.35,
          pointRadius: 2,
        },
        {
          label: 'Delivered',
          data: trend.map((point) => point.delivered),
          borderColor: BRAND,
          backgroundColor: 'rgba(11,110,91,0.12)',
          fill: true,
          tension: 0.35,
          pointRadius: 2,
        },
      ],
    };
  }, [summary]);

  const statusData = useMemo(() => {
    const breakdown = summary?.statusBreakdown || {};
    const keys = Object.keys(breakdown);
    return {
      labels: keys.map(statusLabel),
      datasets: [
        {
          data: keys.map((key) => breakdown[key]),
          backgroundColor: keys.map((key) => STATUS_COLOURS[key] || '#9AA7B1'),
          borderWidth: 0,
        },
      ],
    };
  }, [summary]);

  const destinationData = useMemo(() => {
    const list = summary?.topDestinations || summary?.operatorPerformance || [];
    return {
      labels: list.map((item) => item.label),
      datasets: [
        {
          label: summary?.topDestinations ? 'Shipments' : 'Deliveries completed',
          data: list.map((item) => item.count),
          backgroundColor: 'rgba(11,110,91,0.75)',
          borderRadius: 4,
          barThickness: 22,
        },
      ],
    };
  }, [summary]);

  if (loading) return <Spinner label="Crunching the numbers" />;
  if (error) {
    return (
      <EmptyState
        title="Analytics are unavailable"
        body={error}
        actionLabel="Back to dashboard"
        actionTo="/dashboard"
      />
    );
  }

  const hasTrend = (summary?.trend || []).length > 0;
  const hasStatus = Object.keys(summary?.statusBreakdown || {}).length > 0;
  const hasBars = (summary?.topDestinations || summary?.operatorPerformance || []).length > 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Analytics</h1>
        <p className="mt-1 text-sm text-harbour/60">
          {isAdmin
            ? 'Network-wide delivery performance and operator throughput.'
            : isStaff || isBusiness
              ? 'Delivery performance, delay analysis and destination mix.'
              : 'How your shipments have been moving.'}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {isAdmin && (
          <>
            <StatCard label="Users" value={summary.totalUsers} hint="Accounts on the platform" />
            <StatCard label="Shipments" value={summary.totalShipments} />
            <StatCard label="Active now" value={summary.activeShipments} tone="signal" />
            <StatCard
              label="On-time rate"
              value={`${Math.round(summary.onTimeRatePercent)}%`}
              tone="brand"
              hint={`${summary.pendingPodVerifications} POD checks pending`}
            />
          </>
        )}
        {!isAdmin && (isStaff || isBusiness) && (
          <>
            <StatCard label="Total shipments" value={summary.totalShipments} />
            <StatCard label="Delivered" value={summary.delivered} tone="brand" />
            <StatCard label="In flight" value={summary.inFlight} tone="signal" />
            <StatCard
              label="Delayed"
              value={summary.delayed}
              tone={summary.delayed > 0 ? 'danger' : 'default'}
              hint={`${summary.failed} failed deliveries`}
            />
          </>
        )}
        {!isStaff && !isBusiness && (
          <>
            <StatCard label="Active shipments" value={summary.activeShipments} tone="signal" />
            <StatCard label="In transit" value={summary.inTransit} />
            <StatCard label="Delivered" value={summary.deliveredShipments} tone="brand" />
            <StatCard
              label="Running late"
              value={summary.delayed}
              tone={summary.delayed > 0 ? 'danger' : 'default'}
            />
          </>
        )}
      </div>

      {(isStaff || isBusiness) && !isAdmin && (
        <div className="grid gap-4 sm:grid-cols-2">
          <StatCard
            label="On-time rate"
            value={`${Math.round(summary.onTimeRatePercent)}%`}
            tone="brand"
            hint="Delivered on or before the promised window"
          />
          <StatCard
            label="Average door-to-door time"
            value={formatDuration(Math.round((summary.averageDeliveryHours || 0) * 60))}
            hint="From pickup to delivery confirmation"
          />
        </div>
      )}

      <div className="grid gap-5 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Panel title="Volume over time" subtitle="Shipments created against shipments delivered">
            {hasTrend ? (
              <Line data={trendData} options={baseOptions} />
            ) : (
              <p className="text-sm text-harbour/50">Not enough history yet.</p>
            )}
          </Panel>
        </div>
        <Panel title="Status mix" subtitle="Where everything sits right now">
          {hasStatus ? (
            <Doughnut
              data={statusData}
              options={{
                ...baseOptions,
                scales: undefined,
                cutout: '58%',
                plugins: {
                  ...baseOptions.plugins,
                  legend: { ...baseOptions.plugins.legend, position: 'bottom' },
                },
              }}
            />
          ) : (
            <p className="text-sm text-harbour/50">No shipments to break down.</p>
          )}
        </Panel>
      </div>

      {hasBars && (
        <Panel
          title={summary.topDestinations ? 'Top destinations' : 'Operator throughput'}
          subtitle={
            summary.topDestinations
              ? 'Cities receiving the most volume'
              : 'Deliveries completed per logistics operator'
          }
        >
          <Bar
            data={destinationData}
            options={{ ...baseOptions, indexAxis: 'y', plugins: { ...baseOptions.plugins, legend: { display: false } } }}
          />
        </Panel>
      )}

      {isStaff && routes.length > 0 && (
        <div className="card overflow-hidden">
          <div className="border-b border-edge px-5 py-4">
            <h2 className="font-display text-lg font-semibold">Route performance</h2>
            <p className="mt-0.5 text-sm text-harbour/55">
              Planned versus actual transit time on completed runs.
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[680px]">
              <thead className="bg-paper">
                <tr>
                  <th className="table-head">Tracking number</th>
                  <th className="table-head">Operator</th>
                  <th className="table-head">Distance</th>
                  <th className="table-head">Planned</th>
                  <th className="table-head">Actual</th>
                  <th className="table-head">Result</th>
                </tr>
              </thead>
              <tbody>
                {routes.map((route) => (
                  <tr key={route.trackingNumber} className="hover:bg-paper/60">
                    <td className="table-cell font-medium">{route.trackingNumber}</td>
                    <td className="table-cell text-harbour/70">{route.driverName || 'Unassigned'}</td>
                    <td className="table-cell tabular-nums text-harbour/70">
                      {route.distanceKm?.toFixed(1)} km
                    </td>
                    <td className="table-cell text-harbour/70">{formatDuration(route.plannedMinutes)}</td>
                    <td className="table-cell text-harbour/70">{formatDuration(route.actualMinutes)}</td>
                    <td className="table-cell">
                      <span
                        className={`rounded border px-2 py-0.5 text-xs font-medium ${
                          route.delayed
                            ? 'border-danger/30 bg-danger-soft text-danger'
                            : 'border-brand/30 bg-brand-soft text-brand-dark'
                        }`}
                      >
                        {route.delayed ? 'Late' : 'On time'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
