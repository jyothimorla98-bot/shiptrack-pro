import { useCallback, useEffect, useState } from 'react';
import { userApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import { formatDateTime, roleLabel, timeAgo } from '../utils/format';

const ROLES = ['CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMIN'];

export default function AdminUsers() {
  const { user: me, isAdmin } = useAuth();
  const [tab, setTab] = useState('users');

  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [activity, setActivity] = useState(null);
  const [activityPage, setActivityPage] = useState(0);

  const load = useCallback(() => {
    setLoading(true);
    userApi
      .list({ page, size: 15, ...(search ? { query: search } : {}) })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [page, search]);

  useEffect(load, [load]);

  useEffect(() => {
    if (tab !== 'activity') return;
    userApi
      .activity({ page: activityPage, size: 30 })
      .then(setActivity)
      .catch((err) => setError(errorMessage(err)));
  }, [tab, activityPage]);

  const patch = (updated) =>
    setData((current) =>
      current
        ? { ...current, content: current.content.map((u) => (u.id === updated.id ? updated : u)) }
        : current,
    );

  const changeRole = async (id, role) => {
    setError('');
    try {
      patch(await userApi.updateRole(id, role));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const toggleActive = async (target) => {
    setError('');
    try {
      patch(await userApi.updateStatus(target.id, !target.active));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async (target) => {
    if (!window.confirm(`Remove ${target.fullName}? This cannot be undone.`)) return;
    setError('');
    try {
      await userApi.remove(target.id);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div className="space-y-5">
      <div>
        <h1 className="font-display text-2xl font-semibold">User management</h1>
        <p className="mt-1 text-sm text-harbour/60">
          Accounts, roles and the audit trail of what everyone has been doing.
        </p>
      </div>

      <div className="flex gap-2">
        {[
          { value: 'users', label: 'Accounts' },
          { value: 'activity', label: 'Activity log' },
        ].map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setTab(item.value)}
            className={`rounded-full border px-3 py-1 text-sm ${
              tab === item.value
                ? 'border-harbour bg-harbour text-white'
                : 'border-edge bg-white text-harbour/70'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {error && (
        <p className="rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      {tab === 'users' && (
        <>
          <div className="flex flex-wrap gap-2">
            <input
              className="input max-w-xs"
              placeholder="Search by name, email or company"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  setPage(0);
                  setSearch(query.trim());
                }
              }}
            />
            <button
              type="button"
              className="btn-ghost"
              onClick={() => {
                setPage(0);
                setSearch(query.trim());
              }}
            >
              Search
            </button>
            {search && (
              <button
                type="button"
                className="btn-ghost"
                onClick={() => {
                  setQuery('');
                  setSearch('');
                  setPage(0);
                }}
              >
                Reset
              </button>
            )}
          </div>

          {loading && <Spinner />}

          {!loading && data?.content.length === 0 && (
            <EmptyState title="No accounts match" body="Try a different name, email or company." />
          )}

          {!loading && data?.content.length > 0 && (
            <div className="card overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[880px]">
                  <thead className="bg-paper">
                    <tr>
                      <th className="table-head">Name</th>
                      <th className="table-head">Email</th>
                      <th className="table-head">Company</th>
                      <th className="table-head">Role</th>
                      <th className="table-head">Last login</th>
                      <th className="table-head">Status</th>
                      {isAdmin && <th className="table-head">Actions</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((account) => {
                      const self = account.id === me.id;
                      return (
                        <tr key={account.id} className="hover:bg-paper/60">
                          <td className="table-cell">
                            <span className="font-medium">{account.fullName}</span>
                            {self && <span className="ml-2 text-xs text-harbour/45">you</span>}
                            <p className="text-xs text-harbour/45">
                              Joined {formatDateTime(account.createdAt)}
                            </p>
                          </td>
                          <td className="table-cell text-harbour/70">{account.email}</td>
                          <td className="table-cell text-harbour/70">{account.companyName || '--'}</td>
                          <td className="table-cell">
                            {isAdmin && !self ? (
                              <select
                                className="input py-1 text-sm"
                                value={account.role}
                                onChange={(event) => changeRole(account.id, event.target.value)}
                              >
                                {ROLES.map((role) => (
                                  <option key={role} value={role}>
                                    {roleLabel(role)}
                                  </option>
                                ))}
                              </select>
                            ) : (
                              <span className="text-harbour/70">{roleLabel(account.role)}</span>
                            )}
                          </td>
                          <td className="table-cell text-harbour/70">
                            {account.lastLoginAt ? timeAgo(account.lastLoginAt) : 'Never'}
                          </td>
                          <td className="table-cell">
                            <span
                              className={`rounded border px-2 py-0.5 text-xs font-medium ${
                                account.active
                                  ? 'border-brand/30 bg-brand-soft text-brand-dark'
                                  : 'border-edge bg-paper text-harbour/50'
                              }`}
                            >
                              {account.active ? 'Active' : 'Suspended'}
                            </span>
                          </td>
                          {isAdmin && (
                            <td className="table-cell">
                              {self ? (
                                <span className="text-xs text-harbour/40">--</span>
                              ) : (
                                <div className="flex gap-3 text-sm">
                                  <button
                                    type="button"
                                    className="text-harbour/60 hover:text-harbour"
                                    onClick={() => toggleActive(account)}
                                  >
                                    {account.active ? 'Suspend' : 'Reinstate'}
                                  </button>
                                  <button
                                    type="button"
                                    className="text-harbour/45 hover:text-danger"
                                    onClick={() => remove(account)}
                                  >
                                    Remove
                                  </button>
                                </div>
                              )}
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
            </div>
          )}
        </>
      )}

      {tab === 'activity' && (
        <div className="card overflow-hidden">
          {!activity && <Spinner />}
          {activity?.content.length === 0 && (
            <p className="px-5 py-10 text-center text-sm text-harbour/55">Nothing logged yet.</p>
          )}
          {activity?.content.length > 0 && (
            <>
              <div className="divide-y divide-edge">
                {activity.content.map((log) => (
                  <div key={log.id} className="flex flex-wrap items-baseline gap-x-3 gap-y-1 px-4 py-3">
                    <span className="rounded border border-edge bg-paper px-2 py-0.5 text-xs font-medium text-harbour/65">
                      {log.action}
                    </span>
                    <span className="text-sm">{log.detail}</span>
                    <span className="ml-auto text-xs text-harbour/45">
                      {log.userEmail || 'system'} · {timeAgo(log.createdAt)}
                    </span>
                  </div>
                ))}
              </div>
              <Pagination
                page={activity.page}
                totalPages={activity.totalPages}
                onChange={setActivityPage}
              />
            </>
          )}
        </div>
      )}
    </div>
  );
}
