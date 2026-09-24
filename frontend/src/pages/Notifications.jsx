import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import useStompSubscription from '../hooks/useLiveShipment';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import { timeAgo } from '../utils/format';

const TONE = {
  DELAY_ALERT: 'border-danger/30 bg-danger-soft text-danger',
  DELIVERY_FAILED: 'border-danger/30 bg-danger-soft text-danger',
  SHIPMENT_DELIVERED: 'border-brand/30 bg-brand-soft text-brand-dark',
  ETA_UPDATE: 'border-signal/30 bg-signal-soft text-signal',
  OUT_FOR_DELIVERY: 'border-signal/30 bg-signal-soft text-signal',
};

const typeLabel = (type) => (type || 'UPDATE').toLowerCase().replace(/_/g, ' ');

export default function Notifications() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    notificationApi
      .list({ page, size: 20 })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(load, [load]);

  // A new notification pushed over the socket drops straight into the list.
  useStompSubscription(
    user ? `/topic/notifications/${user.id}` : null,
    (incoming) => {
      setData((current) =>
        current
          ? { ...current, content: [incoming, ...current.content], totalElements: current.totalElements + 1 }
          : current,
      );
    },
    Boolean(user),
  );

  const markRead = async (id) => {
    await notificationApi.markRead(id).catch(() => {});
    setData((current) =>
      current
        ? { ...current, content: current.content.map((n) => (n.id === id ? { ...n, read: true } : n)) }
        : current,
    );
  };

  const markAllRead = async () => {
    await notificationApi.markAllRead().catch(() => {});
    setData((current) =>
      current ? { ...current, content: current.content.map((n) => ({ ...n, read: true })) } : current,
    );
  };

  const remove = async (id) => {
    await notificationApi.remove(id).catch(() => {});
    setData((current) =>
      current ? { ...current, content: current.content.filter((n) => n.id !== id) } : current,
    );
  };

  const items = (data?.content || []).filter((n) => (filter === 'unread' ? !n.read : true));
  const unread = (data?.content || []).filter((n) => !n.read).length;

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold">Notifications</h1>
          <p className="mt-1 text-sm text-harbour/60">
            Status changes, ETA revisions and delay warnings, newest first.
          </p>
        </div>
        {unread > 0 && (
          <button type="button" className="btn-ghost" onClick={markAllRead}>
            Mark all as read
          </button>
        )}
      </div>

      <div className="flex gap-2">
        {[
          { value: 'all', label: 'All' },
          { value: 'unread', label: `Unread${unread ? ` (${unread})` : ''}` },
        ].map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => setFilter(tab.value)}
            className={`rounded-full border px-3 py-1 text-sm ${
              filter === tab.value
                ? 'border-harbour bg-harbour text-white'
                : 'border-edge bg-white text-harbour/70'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <p className="rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <Spinner />}

      {!loading && items.length === 0 && (
        <EmptyState
          title={filter === 'unread' ? 'Nothing unread' : 'No notifications yet'}
          body="Once a shipment moves through the network, every update lands here."
          actionLabel="View shipments"
          actionTo="/shipments"
        />
      )}

      {!loading && items.length > 0 && (
        <div className="card divide-y divide-edge">
          {items.map((item) => (
            <div
              key={item.id}
              className={`flex flex-wrap items-start gap-3 px-4 py-4 ${item.read ? '' : 'bg-paper/70'}`}
            >
              <span
                className={`mt-0.5 rounded border px-2 py-0.5 text-xs font-medium capitalize ${
                  TONE[item.type] || 'border-edge bg-paper text-harbour/65'
                }`}
              >
                {typeLabel(item.type)}
              </span>

              <div className="min-w-[200px] flex-1">
                <p className="font-medium">{item.title}</p>
                <p className="mt-0.5 text-sm text-harbour/65">{item.message}</p>
                <p className="mt-1 text-xs text-harbour/45">
                  {timeAgo(item.createdAt)}
                  {item.channel && item.channel !== 'IN_APP' ? ` · sent via ${item.channel}` : ''}
                </p>
              </div>

              <div className="flex items-center gap-3 text-sm">
                {item.shipmentId && (
                  <Link to={`/shipments/${item.shipmentId}`} className="text-brand hover:underline">
                    Open
                  </Link>
                )}
                {!item.read && (
                  <button
                    type="button"
                    className="text-harbour/55 hover:text-harbour"
                    onClick={() => markRead(item.id)}
                  >
                    Mark read
                  </button>
                )}
                <button
                  type="button"
                  className="text-harbour/45 hover:text-danger"
                  onClick={() => remove(item.id)}
                >
                  Remove
                </button>
              </div>
            </div>
          ))}
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
