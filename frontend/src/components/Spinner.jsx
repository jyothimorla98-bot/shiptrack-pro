export default function Spinner({ label = 'Loading' }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-harbour/60">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-edge border-t-brand" />
      {label}
    </div>
  );
}
