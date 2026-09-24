import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { notificationApi } from '../api/api';
import useStompSubscription from '../hooks/useLiveShipment';

const NAV = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/shipments', label: 'Shipments' },
  { to: '/shipments/new', label: 'Book a shipment', hideFor: ['LOGISTICS_OPERATOR', 'SUPPORT_AGENT'] },
  { to: '/live', label: 'Live map', staffOnly: true },
  { to: '/pod', label: 'Proof of delivery', staffOnly: true },
  { to: '/analytics', label: 'Analytics' },
  { to: '/reports', label: 'Reports' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/admin/users', label: 'People', roles: ['ADMIN', 'SUPPORT_AGENT'] },
];

export default function AppLayout() {
  const { user, logout, isStaff } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);
  const [query, setQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  const refreshUnread = () =>
    notificationApi
      .unreadCount()
      .then((data) => setUnread(data.count))
      .catch(() => {});

  useEffect(() => {
    refreshUnread();
  }, []);

  useStompSubscription(`/topic/notifications/${user?.id}`, () => refreshUnread(), Boolean(user?.id));

  const visibleNav = NAV.filter((item) => {
    if (item.staffOnly && !isStaff) return false;
    if (item.roles && !item.roles.includes(user.role)) return false;
    if (item.hideFor && item.hideFor.includes(user.role)) return false;
    return true;
  });

  const search = (event) => {
    event.preventDefault();
    const value = query.trim();
    if (value) navigate(`/track/${encodeURIComponent(value.toUpperCase())}`);
  };

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 flex-col bg-harbour text-white lg:flex">
        <div className="px-6 py-6">
          <Link to="/dashboard" className="font-display text-lg font-semibold tracking-tight">
            ShipTrack <span className="text-brand-soft">Pro</span>
          </Link>
          <p className="mt-1 text-xs text-harbour-300">Delivery visibility</p>
        </div>

        <nav className="flex-1 space-y-0.5 px-3">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/shipments'}
              className={({ isActive }) =>
                `flex items-center justify-between rounded-md px-3 py-2 text-sm transition-colors ${
                  isActive ? 'bg-harbour-600 text-white' : 'text-harbour-300 hover:bg-harbour-700 hover:text-white'
                }`
              }
            >
              {item.label}
              {item.to === '/notifications' && unread > 0 && (
                <span className="rounded-full bg-signal px-1.5 py-0.5 text-[11px] font-semibold text-white">
                  {unread}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-white/10 px-6 py-4 text-xs text-harbour-300">
          <p className="text-white">{user.fullName}</p>
          <p className="mt-0.5">{user.role.toLowerCase().replace(/_/g, ' ')}</p>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex flex-wrap items-center gap-3 border-b border-edge bg-white px-4 py-3 lg:px-8">
          <Link to="/dashboard" className="font-display font-semibold lg:hidden">
            ShipTrack Pro
          </Link>

          <form onSubmit={search} className="order-3 w-full lg:order-none lg:max-w-sm lg:flex-1">
            <label htmlFor="global-track" className="sr-only">
              Find a shipment by tracking number
            </label>
            <input
              id="global-track"
              className="input"
              placeholder="Find a tracking number, e.g. STP-260920-ABC123"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </form>

          <div className="ml-auto flex items-center gap-2">
            <Link to="/notifications" className="btn-ghost relative px-3 py-2">
              Alerts
              {unread > 0 && (
                <span className="absolute -right-1 -top-1 rounded-full bg-signal px-1.5 text-[11px] font-semibold text-white">
                  {unread}
                </span>
              )}
            </Link>

            <div className="relative">
              <button
                type="button"
                className="btn-ghost px-3 py-2"
                onClick={() => setMenuOpen((open) => !open)}
                aria-expanded={menuOpen}
              >
                {user.fullName.split(' ')[0]}
              </button>
              {menuOpen && (
                <div className="absolute right-0 z-20 mt-1 w-44 rounded-md border border-edge bg-white py-1 shadow-lift">
                  <Link
                    to="/profile"
                    className="block px-4 py-2 text-sm hover:bg-paper"
                    onClick={() => setMenuOpen(false)}
                  >
                    Profile and settings
                  </Link>
                  <button
                    type="button"
                    className="block w-full px-4 py-2 text-left text-sm text-danger hover:bg-paper"
                    onClick={() => {
                      logout();
                      navigate('/login');
                    }}
                  >
                    Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <nav className="flex gap-1 overflow-x-auto border-b border-edge bg-white px-4 py-2 lg:hidden">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/shipments'}
              className={({ isActive }) =>
                `whitespace-nowrap rounded px-3 py-1.5 text-sm ${
                  isActive ? 'bg-harbour text-white' : 'text-harbour/70'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main className="flex-1 px-4 py-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
