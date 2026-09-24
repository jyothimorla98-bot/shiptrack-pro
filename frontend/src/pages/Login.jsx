import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

const DEMO = [
  { label: 'Administrator', email: 'admin@shiptrack.dev' },
  { label: 'Logistics operator', email: 'driver@shiptrack.dev' },
  { label: 'Business client', email: 'business@shiptrack.dev' },
  { label: 'Customer', email: 'customer@shiptrack.dev' },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(params.get('expired') ? 'Your session ended. Sign in again.' : '');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(form);
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Email or password is incorrect'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <section className="hidden flex-col justify-between bg-harbour px-12 py-12 text-white lg:flex">
        <p className="font-display text-lg font-semibold">
          ShipTrack <span className="text-brand-soft">Pro</span>
        </p>
        <div>
          <h1 className="max-w-md font-display text-4xl font-semibold leading-tight">
            Every parcel, every scan, on one screen.
          </h1>
          <p className="mt-4 max-w-md text-harbour-300">
            Book shipments, follow drivers on a live map, capture signatures at the door and see where
            time is being lost across your delivery network.
          </p>
        </div>
        <dl className="grid grid-cols-3 gap-6 border-t border-white/10 pt-6 text-sm">
          <div>
            <dt className="text-harbour-300">Live positions</dt>
            <dd className="mt-1 font-display text-xl">Every 8s</dd>
          </div>
          <div>
            <dt className="text-harbour-300">Delivery stages</dt>
            <dd className="mt-1 font-display text-xl">7</dd>
          </div>
          <div>
            <dt className="text-harbour-300">Roles</dt>
            <dd className="mt-1 font-display text-xl">5</dd>
          </div>
        </dl>
      </section>

      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <h2 className="font-display text-2xl font-semibold">Sign in</h2>
          <p className="mt-1 text-sm text-harbour/60">Use your ShipTrack Pro account.</p>

          {error && (
            <p className="mt-4 rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
              {error}
            </p>
          )}

          <form onSubmit={submit} className="mt-6 space-y-4">
            <div>
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                required
                className="input"
                value={form.email}
                onChange={(event) => setForm({ ...form, email: event.target.value })}
              />
            </div>
            <div>
              <label className="label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                className="input"
                value={form.password}
                onChange={(event) => setForm({ ...form, password: event.target.value })}
              />
            </div>

            <button type="submit" className="btn-primary w-full" disabled={busy}>
              {busy ? 'Signing in' : 'Sign in'}
            </button>
          </form>

          <div className="mt-4 flex items-center justify-between text-sm">
            <Link to="/forgot-password" className="text-brand hover:underline">
              Forgot password
            </Link>
            <Link to="/track" className="text-harbour/60 hover:underline">
              Track without signing in
            </Link>
          </div>

          <p className="mt-6 text-sm text-harbour/60">
            New here?{' '}
            <Link to="/register" className="font-medium text-brand hover:underline">
              Create an account
            </Link>
          </p>

          <div className="mt-8 rounded-md border border-edge bg-white p-4">
            <p className="text-xs font-semibold text-harbour/60">Demo accounts, password Password123</p>
            <div className="mt-2 grid gap-1">
              {DEMO.map((account) => (
                <button
                  key={account.email}
                  type="button"
                  className="flex items-center justify-between rounded px-2 py-1 text-left text-sm hover:bg-paper"
                  onClick={() => setForm({ email: account.email, password: 'Password123' })}
                >
                  <span>{account.label}</span>
                  <span className="text-xs text-harbour/50">{account.email}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
