const dateTime = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});

const dateOnly = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

export const formatDateTime = (value) => (value ? dateTime.format(new Date(value)) : '--');
export const formatDate = (value) => (value ? dateOnly.format(new Date(value)) : '--');

export const statusLabel = (status) =>
  (status || '').toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());

/** "2 days 4 hrs", "3 hrs 20 min", "18 min" */
export function formatDuration(minutes) {
  if (minutes == null) return '--';
  if (minutes <= 0) return 'Arriving now';
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const mins = Math.round(minutes % 60);
  if (days > 0) return `${days} day${days > 1 ? 's' : ''} ${hours} hr`;
  if (hours > 0) return `${hours} hr ${mins} min`;
  return `${mins} min`;
}

export function timeAgo(value) {
  if (!value) return '--';
  const diff = Math.round((Date.now() - new Date(value).getTime()) / 60000);
  if (diff < 1) return 'just now';
  if (diff < 60) return `${diff} min ago`;
  if (diff < 1440) return `${Math.floor(diff / 60)} hr ago`;
  return `${Math.floor(diff / 1440)} d ago`;
}

export const roleLabel = (role) => statusLabel(role);

export const SERVICE_TYPES = [
  { value: 'STANDARD', label: 'Standard', hint: 'Best value, 1 to 3 days' },
  { value: 'EXPRESS', label: 'Express', hint: 'Priority handling, next day' },
  { value: 'SAME_DAY', label: 'Same day', hint: 'Within the city, today' },
];

export const STATUSES = [
  'CREATED',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'FAILED_DELIVERY',
  'CANCELLED',
];
