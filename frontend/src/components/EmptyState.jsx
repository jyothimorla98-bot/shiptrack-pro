import { Link } from 'react-router-dom';

export default function EmptyState({ title, body, actionLabel, actionTo, onAction }) {
  return (
    <div className="card flex flex-col items-center gap-3 px-6 py-14 text-center">
      <h3 className="text-lg font-semibold">{title}</h3>
      {body && <p className="max-w-sm text-sm text-harbour/60">{body}</p>}
      {actionLabel && actionTo && (
        <Link to={actionTo} className="btn-primary mt-2">
          {actionLabel}
        </Link>
      )}
      {actionLabel && onAction && !actionTo && (
        <button type="button" className="btn-primary mt-2" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
