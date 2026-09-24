import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/api';
import { errorMessage } from '../api/client';

export default function ResetPassword() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ token: '', newPassword: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      await authApi.resetPassword(form);
      navigate('/login?reset=1', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-6">
      <div className="w-full max-w-sm">
        <h1 className="font-display text-2xl font-semibold">Choose a new password</h1>
        <p className="mt-1 text-sm text-harbour/60">Paste the code from your reset email.</p>

        {error && (
          <p className="mt-4 rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        <form onSubmit={submit} className="card mt-6 space-y-4 p-6">
          <div>
            <label className="label" htmlFor="token">
              Reset code
            </label>
            <input
              id="token"
              required
              className="input"
              value={form.token}
              onChange={(event) => setForm({ ...form, token: event.target.value })}
            />
          </div>
          <div>
            <label className="label" htmlFor="newPassword">
              New password
            </label>
            <input
              id="newPassword"
              type="password"
              required
              minLength={6}
              className="input"
              value={form.newPassword}
              onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
            />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={busy}>
            {busy ? 'Updating' : 'Update password'}
          </button>
        </form>

        <Link to="/login" className="mt-4 block text-center text-sm text-harbour/60 hover:underline">
          Back to sign in
        </Link>
      </div>
    </div>
  );
}
