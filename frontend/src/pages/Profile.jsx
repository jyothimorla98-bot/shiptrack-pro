import { useEffect, useState } from 'react';
import { authApi, userApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/Spinner';
import { formatDate, roleLabel, timeAgo } from '../utils/format';

const emptyAddress = {
  contactName: '',
  contactPhone: '',
  email: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
  latitude: '',
  longitude: '',
};

export default function Profile() {
  const { user, updateUser, logout } = useAuth();

  const [profile, setProfile] = useState({
    fullName: user.fullName || '',
    phone: user.phone || '',
    companyName: user.companyName || '',
    avatarUrl: user.avatarUrl || '',
  });
  const [address, setAddress] = useState({ ...emptyAddress, ...(user.defaultAddress || {}) });
  const [profileState, setProfileState] = useState({ busy: false, message: '', error: '' });

  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [passwordState, setPasswordState] = useState({ busy: false, message: '', error: '' });

  const [activity, setActivity] = useState(null);

  useEffect(() => {
    userApi
      .myActivity({ page: 0, size: 12 })
      .then(setActivity)
      .catch(() => setActivity({ content: [] }));
  }, []);

  const saveProfile = async (event) => {
    event.preventDefault();
    setProfileState({ busy: true, message: '', error: '' });
    const hasAddress = address.addressLine1 && address.city && address.country;
    try {
      const updated = await userApi.updateProfile({
        ...profile,
        defaultAddress: hasAddress
          ? {
              ...address,
              latitude: address.latitude === '' ? null : Number(address.latitude),
              longitude: address.longitude === '' ? null : Number(address.longitude),
            }
          : null,
      });
      updateUser(updated);
      setProfileState({ busy: false, message: 'Profile saved.', error: '' });
    } catch (err) {
      setProfileState({ busy: false, message: '', error: errorMessage(err) });
    }
  };

  const changePassword = async (event) => {
    event.preventDefault();
    if (passwords.newPassword !== passwords.confirm) {
      setPasswordState({ busy: false, message: '', error: 'The two new passwords do not match.' });
      return;
    }
    setPasswordState({ busy: true, message: '', error: '' });
    try {
      await authApi.changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      setPasswords({ currentPassword: '', newPassword: '', confirm: '' });
      setPasswordState({ busy: false, message: 'Password updated.', error: '' });
    } catch (err) {
      setPasswordState({ busy: false, message: '', error: errorMessage(err) });
    }
  };

  const field = (key, label, extra = {}) => (
    <div>
      <label className="label" htmlFor={key}>
        {label}
      </label>
      <input
        id={key}
        className="input"
        value={profile[key]}
        onChange={(event) => setProfile({ ...profile, [key]: event.target.value })}
        {...extra}
      />
    </div>
  );

  const addressField = (key, label, extra = {}) => (
    <div>
      <label className="label" htmlFor={`addr-${key}`}>
        {label}
      </label>
      <input
        id={`addr-${key}`}
        className="input"
        value={address[key] ?? ''}
        onChange={(event) => setAddress({ ...address, [key]: event.target.value })}
        {...extra}
      />
    </div>
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Your account</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Contact details, the address we pre-fill on new shipments, and your sign-in security.
        </p>
      </div>

      <div className="card flex flex-wrap items-center gap-4 p-5">
        <div className="flex h-14 w-14 items-center justify-center overflow-hidden rounded-full bg-harbour text-lg font-semibold text-white">
          {user.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="h-full w-full object-cover" />
          ) : (
            user.fullName.charAt(0).toUpperCase()
          )}
        </div>
        <div>
          <p className="font-display text-lg font-semibold">{user.fullName}</p>
          <p className="text-sm text-harbour/60">{user.email}</p>
        </div>
        <div className="ml-auto grid gap-1 text-right text-sm">
          <span className="rounded border border-edge bg-paper px-2 py-0.5 text-xs font-medium text-harbour/65">
            {roleLabel(user.role)}
          </span>
          <span className="text-xs text-harbour/45">Member since {formatDate(user.createdAt)}</span>
          {user.provider && user.provider !== 'LOCAL' && (
            <span className="text-xs text-harbour/45">Signed in with {user.provider}</span>
          )}
        </div>
      </div>

      <form className="card p-5" onSubmit={saveProfile}>
        <h2 className="font-display text-lg font-semibold">Profile</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {field('fullName', 'Full name', { required: true })}
          {field('phone', 'Phone', { placeholder: '+91 98765 43210' })}
          {field('companyName', 'Company')}
          {field('avatarUrl', 'Avatar URL', { placeholder: 'https://…' })}
        </div>

        <h3 className="mt-6 font-display text-base font-semibold">Default pickup address</h3>
        <p className="mt-0.5 text-sm text-harbour/55">
          Used to pre-fill the sender section when you book a shipment.
        </p>
        <div className="mt-3 grid gap-4 sm:grid-cols-2">
          {addressField('contactName', 'Contact name')}
          {addressField('contactPhone', 'Contact phone')}
          <div className="sm:col-span-2">{addressField('addressLine1', 'Address line 1')}</div>
          <div className="sm:col-span-2">{addressField('addressLine2', 'Address line 2')}</div>
          {addressField('city', 'City')}
          {addressField('state', 'State')}
          {addressField('postalCode', 'Postal code')}
          {addressField('country', 'Country')}
          {addressField('latitude', 'Latitude', { type: 'number', step: 'any' })}
          {addressField('longitude', 'Longitude', { type: 'number', step: 'any' })}
        </div>

        {profileState.error && (
          <p className="mt-4 rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {profileState.error}
          </p>
        )}
        {profileState.message && (
          <p className="mt-4 rounded border border-brand/25 bg-brand-soft px-3 py-2 text-sm text-brand-dark">
            {profileState.message}
          </p>
        )}

        <button type="submit" className="btn-primary mt-5" disabled={profileState.busy}>
          {profileState.busy ? 'Saving…' : 'Save changes'}
        </button>
      </form>

      <form className="card p-5" onSubmit={changePassword}>
        <h2 className="font-display text-lg font-semibold">Password</h2>
        <p className="mt-0.5 text-sm text-harbour/55">At least 6 characters.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <div>
            <label className="label" htmlFor="currentPassword">
              Current password
            </label>
            <input
              id="currentPassword"
              type="password"
              className="input"
              autoComplete="current-password"
              value={passwords.currentPassword}
              onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })}
              required
            />
          </div>
          <div>
            <label className="label" htmlFor="newPassword">
              New password
            </label>
            <input
              id="newPassword"
              type="password"
              className="input"
              autoComplete="new-password"
              minLength={6}
              value={passwords.newPassword}
              onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })}
              required
            />
          </div>
          <div>
            <label className="label" htmlFor="confirm">
              Confirm new password
            </label>
            <input
              id="confirm"
              type="password"
              className="input"
              autoComplete="new-password"
              minLength={6}
              value={passwords.confirm}
              onChange={(event) => setPasswords({ ...passwords, confirm: event.target.value })}
              required
            />
          </div>
        </div>

        {passwordState.error && (
          <p className="mt-4 rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
            {passwordState.error}
          </p>
        )}
        {passwordState.message && (
          <p className="mt-4 rounded border border-brand/25 bg-brand-soft px-3 py-2 text-sm text-brand-dark">
            {passwordState.message}
          </p>
        )}

        <button type="submit" className="btn-primary mt-5" disabled={passwordState.busy}>
          {passwordState.busy ? 'Updating…' : 'Update password'}
        </button>
      </form>

      <div className="card p-5">
        <h2 className="font-display text-lg font-semibold">Recent account activity</h2>
        {!activity && <Spinner />}
        {activity?.content.length === 0 && (
          <p className="mt-3 text-sm text-harbour/55">Nothing recorded yet.</p>
        )}
        {activity?.content.length > 0 && (
          <ul className="mt-3 divide-y divide-edge">
            {activity.content.map((log) => (
              <li key={log.id} className="flex flex-wrap items-baseline gap-x-3 py-2.5">
                <span className="rounded border border-edge bg-paper px-2 py-0.5 text-xs font-medium text-harbour/65">
                  {log.action}
                </span>
                <span className="text-sm text-harbour/75">{log.detail}</span>
                <span className="ml-auto text-xs text-harbour/45">{timeAgo(log.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card p-5">
        <h2 className="font-display text-lg font-semibold">Session</h2>
        <p className="mt-1 text-sm text-harbour/60">
          Signing out clears the token stored in this browser.
        </p>
        <button type="button" className="btn-ghost mt-4" onClick={logout}>
          Sign out
        </button>
      </div>
    </div>
  );
}
