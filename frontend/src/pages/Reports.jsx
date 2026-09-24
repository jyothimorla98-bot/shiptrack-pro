import { useState } from 'react';
import { reportApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { STATUSES, statusLabel } from '../utils/format';

const FORMATS = [
  {
    value: 'pdf',
    label: 'PDF',
    blurb: 'Formatted document with a summary header. Good for sharing or printing.',
  },
  {
    value: 'xlsx',
    label: 'Excel',
    blurb: 'One row per shipment with typed columns, ready for pivots and filters.',
  },
  {
    value: 'csv',
    label: 'CSV',
    blurb: 'Plain text export for importing into another system.',
  },
];

const PRESETS = [
  { label: 'Last 7 days', days: 7 },
  { label: 'Last 30 days', days: 30 },
  { label: 'Last 90 days', days: 90 },
];

const isoDate = (date) => date.toISOString().slice(0, 10);

export default function Reports() {
  const { isStaff } = useAuth();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [status, setStatus] = useState('');
  const [title, setTitle] = useState('Shipment report');
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');

  const applyPreset = (days) => {
    const end = new Date();
    const start = new Date(Date.now() - days * 86400000);
    setFrom(isoDate(start));
    setTo(isoDate(end));
  };

  const download = async (format) => {
    setBusy(format);
    setError('');
    try {
      await reportApi.download(format, {
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
        ...(status ? { status } : {}),
        ...(format === 'csv' ? {} : { title }),
      });
    } catch (err) {
      setError(errorMessage(err, 'The report could not be generated.'));
    } finally {
      setBusy('');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Reports &amp; export</h1>
        <p className="mt-1 text-sm text-harbour/60">
          {isStaff
            ? 'Export shipment, delivery and delay data across the whole network.'
            : 'Export your own shipment and delivery history.'}
        </p>
      </div>

      <div className="card p-5">
        <h2 className="font-display text-lg font-semibold">Filters</h2>
        <p className="mt-0.5 text-sm text-harbour/55">
          Leave a field blank to include everything. Dates are inclusive.
        </p>

        <div className="mt-4 flex flex-wrap gap-2">
          {PRESETS.map((preset) => (
            <button
              key={preset.days}
              type="button"
              className="rounded-full border border-edge bg-white px-3 py-1 text-sm text-harbour/70 hover:bg-paper"
              onClick={() => applyPreset(preset.days)}
            >
              {preset.label}
            </button>
          ))}
          <button
            type="button"
            className="rounded-full border border-edge bg-white px-3 py-1 text-sm text-harbour/70 hover:bg-paper"
            onClick={() => {
              setFrom('');
              setTo('');
              setStatus('');
            }}
          >
            Clear
          </button>
        </div>

        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="label" htmlFor="from">
              From
            </label>
            <input
              id="from"
              type="date"
              className="input"
              value={from}
              max={to || undefined}
              onChange={(event) => setFrom(event.target.value)}
            />
          </div>
          <div>
            <label className="label" htmlFor="to">
              To
            </label>
            <input
              id="to"
              type="date"
              className="input"
              value={to}
              min={from || undefined}
              onChange={(event) => setTo(event.target.value)}
            />
          </div>
          <div>
            <label className="label" htmlFor="status">
              Status
            </label>
            <select
              id="status"
              className="input"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="">All statuses</option>
              {STATUSES.map((value) => (
                <option key={value} value={value}>
                  {statusLabel(value)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="title">
              Report title
            </label>
            <input
              id="title"
              className="input"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Shipment report"
            />
          </div>
        </div>
      </div>

      {error && (
        <p className="rounded border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        {FORMATS.map((format) => (
          <div key={format.value} className="card flex flex-col p-5">
            <h3 className="font-display text-lg font-semibold">{format.label}</h3>
            <p className="mt-1 flex-1 text-sm text-harbour/60">{format.blurb}</p>
            <button
              type="button"
              className="btn-primary mt-4"
              disabled={Boolean(busy)}
              onClick={() => download(format.value)}
            >
              {busy === format.value ? 'Preparing…' : `Download ${format.label}`}
            </button>
          </div>
        ))}
      </div>

      <div className="card p-5">
        <h2 className="font-display text-lg font-semibold">What is included</h2>
        <ul className="mt-3 space-y-1.5 text-sm text-harbour/70">
          <li>Tracking number, service type and current status</li>
          <li>Sender and receiver cities with the full delivery address</li>
          <li>Package weight, dimensions and declared value</li>
          <li>Created, picked up, promised and actual delivery timestamps</li>
          <li>Assigned operator and whether the shipment ran late</li>
        </ul>
      </div>
    </div>
  );
}
