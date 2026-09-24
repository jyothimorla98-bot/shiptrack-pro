import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-paper px-6 text-center">
      <p className="font-display text-5xl font-semibold text-harbour/20">404</p>
      <h1 className="font-display text-2xl font-semibold">That page is not on the route</h1>
      <p className="max-w-sm text-sm text-harbour/60">
        The link may be out of date. Head back to your dashboard or track a shipment by number.
      </p>
      <div className="mt-2 flex gap-2">
        <Link to="/dashboard" className="btn-primary">
          Go to dashboard
        </Link>
        <Link to="/track" className="btn-ghost">
          Track a shipment
        </Link>
      </div>
    </div>
  );
}
