import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { shipmentApi } from '../api/api';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SERVICE_TYPES } from '../utils/format';

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

/** Coordinates for the cities in the seed data, so a new booking lands on the map straight away. */
const CITY_COORDS = {
  hyderabad: [17.385, 78.4867],
  bengaluru: [12.9716, 77.5946],
  bangalore: [12.9716, 77.5946],
  mumbai: [19.076, 72.8777],
  delhi: [28.6139, 77.209],
  'new delhi': [28.6139, 77.209],
  chennai: [13.0827, 80.2707],
  kolkata: [22.5726, 88.3639],
  pune: [18.5204, 73.8567],
  ahmedabad: [23.0225, 72.5714],
  jaipur: [26.9124, 75.7873],
  kochi: [9.9312, 76.2673],
};

function AddressFields({ title, hint, value, onChange }) {
  const set = (field) => (event) => {
    const next = { ...value, [field]: event.target.value };
    if (field === 'city') {
      const match = CITY_COORDS[event.target.value.trim().toLowerCase()];
      if (match) {
        next.latitude = match[0];
        next.longitude = match[1];
      }
    }
    onChange(next);
  };

  return (
    <fieldset className="card p-5">
      <legend className="px-1 font-display font-semibold">{title}</legend>
      <p className="mb-4 text-sm text-harbour/55">{hint}</p>

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="label">Contact name</label>
          <input required className="input" value={value.contactName} onChange={set('contactName')} />
        </div>
        <div>
          <label className="label">Phone</label>
          <input className="input" value={value.contactPhone} onChange={set('contactPhone')} />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Email</label>
          <input type="email" className="input" value={value.email} onChange={set('email')} />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Street address</label>
          <input required className="input" value={value.addressLine1} onChange={set('addressLine1')} />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Apartment, floor, landmark</label>
          <input className="input" value={value.addressLine2} onChange={set('addressLine2')} />
        </div>
        <div>
          <label className="label">City</label>
          <input required className="input" value={value.city} onChange={set('city')} />
        </div>
        <div>
          <label className="label">State</label>
          <input className="input" value={value.state} onChange={set('state')} />
        </div>
        <div>
          <label className="label">Postal code</label>
          <input className="input" value={value.postalCode} onChange={set('postalCode')} />
        </div>
        <div>
          <label className="label">Country</label>
          <input required className="input" value={value.country} onChange={set('country')} />
        </div>
        <div>
          <label className="label">Latitude</label>
          <input className="input" value={value.latitude} onChange={set('latitude')} placeholder="17.3850" />
        </div>
        <div>
          <label className="label">Longitude</label>
          <input className="input" value={value.longitude} onChange={set('longitude')} placeholder="78.4867" />
        </div>
      </div>
      <p className="mt-3 text-xs text-harbour/50">
        Coordinates fill in automatically for well-known cities and drive the map and ETA.
      </p>
    </fieldset>
  );
}

export default function NewShipment() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [sender, setSender] = useState({
    ...emptyAddress,
    contactName: user.fullName,
    contactPhone: user.phone || '',
    email: user.email,
    ...(user.defaultAddress || {}),
  });
  const [receiver, setReceiver] = useState(emptyAddress);
  const [pkg, setPkg] = useState({
    description: '',
    quantity: 1,
    weightKg: 1,
    lengthCm: 30,
    widthCm: 20,
    heightCm: 15,
    declaredValue: 0,
    fragile: false,
    requiresSignature: true,
  });
  const [serviceType, setServiceType] = useState('STANDARD');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const numeric = (address) => ({
    ...address,
    latitude: address.latitude === '' ? null : Number(address.latitude),
    longitude: address.longitude === '' ? null : Number(address.longitude),
  });

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const shipment = await shipmentApi.create({
        sender: numeric(sender),
        receiver: numeric(receiver),
        packageDetails: {
          ...pkg,
          quantity: Number(pkg.quantity),
          weightKg: Number(pkg.weightKg),
          lengthCm: Number(pkg.lengthCm),
          widthCm: Number(pkg.widthCm),
          heightCm: Number(pkg.heightCm),
          declaredValue: Number(pkg.declaredValue),
        },
        serviceType,
        notes,
      });
      navigate(`/shipments/${shipment.id}`);
    } catch (err) {
      setError(errorMessage(err, 'The shipment could not be booked'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Book a shipment</h1>
        <p className="mt-1 text-sm text-harbour/60">
          We generate the tracking number and first ETA as soon as you book.
        </p>
      </div>

      {error && (
        <p className="rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <AddressFields
        title="Collect from"
        hint="Where the driver picks the parcel up."
        value={sender}
        onChange={setSender}
      />
      <AddressFields
        title="Deliver to"
        hint="Where the parcel needs to arrive."
        value={receiver}
        onChange={setReceiver}
      />

      <fieldset className="card p-5">
        <legend className="px-1 font-display font-semibold">What is in the box</legend>

        <div className="mt-2 grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="label">Contents</label>
            <input
              required
              className="input"
              placeholder="Two cartons of packaged food"
              value={pkg.description}
              onChange={(event) => setPkg({ ...pkg, description: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Pieces</label>
            <input
              type="number"
              min="1"
              className="input"
              value={pkg.quantity}
              onChange={(event) => setPkg({ ...pkg, quantity: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Weight (kg)</label>
            <input
              type="number"
              step="0.1"
              min="0.1"
              className="input"
              value={pkg.weightKg}
              onChange={(event) => setPkg({ ...pkg, weightKg: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Length (cm)</label>
            <input
              type="number"
              className="input"
              value={pkg.lengthCm}
              onChange={(event) => setPkg({ ...pkg, lengthCm: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Width (cm)</label>
            <input
              type="number"
              className="input"
              value={pkg.widthCm}
              onChange={(event) => setPkg({ ...pkg, widthCm: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Height (cm)</label>
            <input
              type="number"
              className="input"
              value={pkg.heightCm}
              onChange={(event) => setPkg({ ...pkg, heightCm: event.target.value })}
            />
          </div>
          <div>
            <label className="label">Declared value</label>
            <input
              type="number"
              className="input"
              value={pkg.declaredValue}
              onChange={(event) => setPkg({ ...pkg, declaredValue: event.target.value })}
            />
          </div>
        </div>

        <div className="mt-4 flex flex-wrap gap-6">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={pkg.fragile}
              onChange={(event) => setPkg({ ...pkg, fragile: event.target.checked })}
            />
            Fragile, handle with care
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={pkg.requiresSignature}
              onChange={(event) => setPkg({ ...pkg, requiresSignature: event.target.checked })}
            />
            Signature required on delivery
          </label>
        </div>
      </fieldset>

      <fieldset className="card p-5">
        <legend className="px-1 font-display font-semibold">Service level</legend>
        <div className="mt-2 grid gap-3 sm:grid-cols-3">
          {SERVICE_TYPES.map((service) => (
            <button
              key={service.value}
              type="button"
              onClick={() => setServiceType(service.value)}
              className={`rounded-md border p-3 text-left ${
                serviceType === service.value ? 'border-brand bg-brand-soft' : 'border-edge bg-white'
              }`}
            >
              <span className="block text-sm font-medium">{service.label}</span>
              <span className="mt-0.5 block text-xs text-harbour/55">{service.hint}</span>
            </button>
          ))}
        </div>

        <div className="mt-4">
          <label className="label">Delivery instructions</label>
          <textarea
            className="input min-h-[80px]"
            placeholder="Leave with building security if nobody answers"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
        </div>
      </fieldset>

      <div className="flex justify-end gap-2">
        <button type="button" className="btn-ghost" onClick={() => navigate('/shipments')}>
          Cancel
        </button>
        <button type="submit" className="btn-primary" disabled={busy}>
          {busy ? 'Booking' : 'Book shipment'}
        </button>
      </div>
    </form>
  );
}
