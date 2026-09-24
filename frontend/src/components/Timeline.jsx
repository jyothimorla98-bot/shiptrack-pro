import StatusBadge from './StatusBadge';
import { formatDateTime } from '../utils/format';

export default function Timeline({ events = [] }) {
  if (!events.length) {
    return <p className="text-sm text-harbour/55">No tracking scans have been recorded yet.</p>;
  }

  const ordered = [...events].sort((a, b) => new Date(b.occurredAt) - new Date(a.occurredAt));

  return (
    <ol className="relative space-y-6 border-l border-edge pl-6">
      {ordered.map((event, index) => (
        <li key={event.id || index} className="relative">
          <span
            className={`absolute -left-[31px] top-1 h-3 w-3 rounded-full border-2 border-white ${
              index === 0 ? 'bg-brand' : 'bg-harbour/25'
            }`}
            aria-hidden="true"
          />
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={event.status} />
            <time className="text-xs text-harbour/50">{formatDateTime(event.occurredAt)}</time>
          </div>
          <p className="mt-1.5 text-sm">{event.description}</p>
          {event.location?.label && (
            <p className="text-xs text-harbour/50">{event.location.label}</p>
          )}
        </li>
      ))}
    </ol>
  );
}
