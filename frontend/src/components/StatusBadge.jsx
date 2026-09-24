import { statusLabel } from '../utils/format';

const TONES = {
  CREATED: 'bg-paper text-harbour/70 border-edge',
  PICKED_UP: 'bg-brand-soft text-brand-dark border-brand/25',
  IN_TRANSIT: 'bg-signal-soft text-signal border-signal/30',
  OUT_FOR_DELIVERY: 'bg-signal-soft text-signal border-signal/40',
  DELIVERED: 'bg-brand-soft text-brand-dark border-brand/35',
  FAILED_DELIVERY: 'bg-danger-soft text-danger border-danger/30',
  CANCELLED: 'bg-paper text-harbour/45 border-edge',
};

export default function StatusBadge({ status, className = '' }) {
  const tone = TONES[status] || TONES.CREATED;
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded border px-2 py-0.5 text-xs font-medium ${tone} ${className}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
      {statusLabel(status)}
    </span>
  );
}
