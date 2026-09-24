export default function StatCard({ label, value, hint, tone = 'default' }) {
  const accent = {
    default: 'text-harbour',
    brand: 'text-brand-dark',
    signal: 'text-signal',
    danger: 'text-danger',
  }[tone];

  return (
    <div className="card px-4 py-4">
      <p className="text-sm text-harbour/55">{label}</p>
      <p className={`mt-2 font-display text-3xl font-semibold tabular-nums ${accent}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-harbour/50">{hint}</p>}
    </div>
  );
}
