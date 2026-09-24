import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    phone: '',
    companyName: '',
    role: 'CUSTOMER',
  });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (field) => (event) => setForm({ ...form, [field]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      await register(form);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'That account could not be created'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-6 py-12">
      <div className="w-full max-w-md">
        <Link to="/login" className="font-display text-lg font-semibold">
          ShipTrack <span className="text-brand">Pro</span>
        </Link>

        <h1 className="mt-6 font-display text-2xl font-semibold">Create your account</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Personal accounts ship parcels. Business accounts get analytics across every shipment they book.
        </p>

        {error && (
          <p className="mt-4 rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        <form onSubmit={submit} className="card mt-6 space-y-4 p-6">
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setForm({ ...form, role: 'CUSTOMER' })}
              className={`rounded-md border px-3 py-2 text-sm ${
                form.role === 'CUSTOMER' ? 'border-brand bg-brand-soft text-brand-dark' : 'border-edge bg-white'
              }`}
            >
              Personal
            </button>
            <button
              type="button"
              onClick={() => setForm({ ...form, role: 'BUSINESS_CLIENT' })}
              className={`rounded-md border px-3 py-2 text-sm ${
                form.role === 'BUSINESS_CLIENT' ? 'border-brand bg-brand-soft text-brand-dark' : 'border-edge bg-white'
              }`}
            >
              Business
            </button>
          </div>

          <div>
            <label className="label" htmlFor="fullName">
              Full name
            </label>
            <input id="fullName" required className="input" value={form.fullName} onChange={set('fullName')} />
          </div>

          {form.role === 'BUSINESS_CLIENT' && (
            <div>
              <label className="label" htmlFor="companyName">
                Company name
              </label>
              <input id="companyName" className="input" value={form.companyName} onChange={set('companyName')} />
            </div>
          )}

          <div>
            <label className="label" htmlFor="email">
              Email
            </label>
            <input id="email" type="email" required className="input" value={form.email} onChange={set('email')} />
          </div>

          <div>
            <label className="label" htmlFor="phone">
              Mobile number
            </label>
            <input id="phone" className="input" value={form.phone} onChange={set('phone')} />
          </div>

          <div>
            <label className="label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={6}
              className="input"
              value={form.password}
              onChange={set('password')}
            />
            <p className="mt-1 text-xs text-harbour/50">At least 6 characters.</p>
          </div>

          <button type="submit" className="btn-primary w-full" disabled={busy}>
            {busy ? 'Creating account' : 'Create account'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-harbour/60">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
