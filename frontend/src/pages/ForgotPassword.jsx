import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/api';
import { errorMessage } from '../api/client';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const response = await authApi.forgotPassword({ email });
      setMessage(response.message);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-6">
      <div className="w-full max-w-sm">
        <h1 className="font-display text-2xl font-semibold">Reset your password</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Enter the email on your account and we will send a reset code.
        </p>

        {message && (
          <p className="mt-4 rounded-md border border-brand/30 bg-brand-soft px-3 py-2 text-sm text-brand-dark">
            {message} In development the code is printed in the backend console.
          </p>
        )}
        {error && (
          <p className="mt-4 rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        <form onSubmit={submit} className="card mt-6 space-y-4 p-6">
          <div>
            <label className="label" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              className="input"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={busy}>
            {busy ? 'Sending' : 'Send reset code'}
          </button>
        </form>

        <div className="mt-4 flex justify-between text-sm">
          <Link to="/login" className="text-harbour/60 hover:underline">
            Back to sign in
          </Link>
          <Link to="/reset-password" className="text-brand hover:underline">
            I have a code
          </Link>
        </div>
      </div>
    </div>
  );
}
