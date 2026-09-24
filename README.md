# ShipTrack Pro

A full-stack shipment tracking and delivery visibility platform. Businesses, logistics
operators and customers can book shipments, watch them move on a live map, capture proof
of delivery, and pull performance reports — all from one place.

**Stack:** Spring Boot 3.2 (Java 17) · MongoDB · Spring Security + JWT · Spring WebSocket
(STOMP/SockJS) · React 18 + Vite · Tailwind CSS · Chart.js · Leaflet / OpenStreetMap

---

## Quick start

### 1. MongoDB

The backend expects a local MongoDB at `mongodb://localhost:27017/` and creates the
`shiptrackpro` database on first run. Nothing to set up by hand.

```bash
# macOS (Homebrew)
brew services start mongodb-community

# Linux (systemd)
sudo systemctl start mongod

# Or just run it in Docker
docker run -d --name shiptrack-mongo -p 27017:27017 -v shiptrack-mongo:/data/db mongo:7
```

Check it is listening: `mongosh mongodb://localhost:27017/ --eval "db.adminCommand('ping')"`

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Runs on **http://localhost:8080**. Swagger UI is at
**http://localhost:8080/swagger-ui.html**, health at `/actuator/health`.

On first boot the seeder creates five demo accounts and five shipments spread across
Hyderabad, Bengaluru, Mumbai, Delhi and Chennai. A background simulator nudges in-transit
shipments along their route every 8 seconds so the live map has something to show.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on **http://localhost:5173**. Vite proxies `/api` and `/ws` to port 8080, so no
environment file is needed for local development. Copy `.env.example` to `.env` only if
your backend lives somewhere else.

---

## Demo logins

Every account uses the password **`Password123`**.

| Email | Role | What they see |
|---|---|---|
| `admin@shiptrack.dev` | Administrator | Everything: users, network analytics, reports |
| `driver@shiptrack.dev` | Logistics Operator | Live map, status updates, POD capture |
| `support@shiptrack.dev` | Support Agent | Shipment lookup, POD verification queue |
| `business@shiptrack.dev` | Business Client | Their shipments, delivery performance |
| `customer@shiptrack.dev` | Customer | Their parcels, tracking, notifications |

Public tracking needs no login at all: **http://localhost:5173/track** — paste any
tracking number from the seeded data (format `STP-YYMMDD-XXXXXX`).

---

## Running with Docker

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- MongoDB: localhost:27017 (persisted in a named volume)

Compose points the backend at `mongodb://mongo:27017/shiptrackpro`, because inside the
Compose network the database host is the service name rather than `localhost`. Set a real
`JWT_SECRET` before putting this anywhere public.

---

## Modules

1. **Authentication & roles** — JWT sign-in, demo OAuth2 exchange, password reset by token,
   five roles enforced with method-level security.
2. **User management** — customer and business registration, profile and default pickup
   address, account suspension, role changes, activity audit trail.
3. **Shipment management** — booking, package details, lifecycle transitions validated by
   the status machine, cancellation, history.
4. **Tracking** — every status change writes a tracking event; the timeline and public
   tracking page read from it.
5. **Live delivery monitoring** — driver location pushed over STOMP to
   `/topic/shipments/{trackingNumber}` and `/topic/fleet`; the map updates without a refresh.
6. **ETA prediction** — distance, service level, handling time, traffic factor and
   historical delay feed the estimate; anything more than 30 minutes past the promise is
   flagged as delayed.
7. **Route management** — planned corridor with waypoints, nearest-neighbour optimisation,
   distance maths and per-route performance.
8. **Proof of delivery** — signature captured on canvas, photo upload, notes and geotag,
   then a support/admin verification queue.
9. **Notifications** — in-app centre with live push, plus email and SMS hooks (disabled by
   default; flip `app.notifications.*` in `application.yml`).
10. **Analytics** — separate customer, business and admin dashboards with status mix,
    volume trend, on-time rate, destination mix and operator throughput.
11. **Reports & export** — filtered shipment exports as PDF, Excel and CSV.

---

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/{register,login,oauth2,forgot-password,reset-password,change-password}` |
| Users | `GET/PUT /api/users/me` · `GET /api/users` · `PATCH /api/users/{id}/role` · `PATCH /api/users/{id}/status` · `GET /api/users/drivers` · `GET /api/users/activity` |
| Shipments | `POST/GET /api/shipments` · `GET /api/shipments/{id}` · `/timeline` · `/eta` · `PATCH /{id}/status` · `/{id}/location` · `/{id}/driver` · `POST /{id}/cancel` |
| Public tracking | `GET /api/public/track/{trackingNumber}` · `/eta` |
| Routes | `GET /api/routes` · `GET /api/routes/shipment/{id}` · `POST .../plan` · `.../optimize` |
| POD | `POST /api/pod/shipment/{id}` · `GET /api/pod/pending` · `POST /api/pod/{id}/verify` · `/photo` |
| Notifications | `GET /api/notifications` · `/unread-count` · `PATCH /{id}/read` · `/read-all` |
| Analytics | `GET /api/analytics/{customer,business,admin,routes}` |
| Reports | `GET /api/reports/shipments.{pdf,xlsx,csv}` |
| WebSocket | `SockJS /ws` → topics `/topic/shipments/{tn}`, `/topic/fleet`, `/topic/notifications/{userId}` |

---

## Configuration

`backend/src/main/resources/application.yml`, all overridable by environment variable:

| Property | Default | Notes |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/shiptrackpro` | `SPRING_DATA_MONGODB_URI` |
| `app.jwt.secret` | dev value | Set `JWT_SECRET` in production |
| `app.jwt.expiration-ms` | 86400000 (24 h) | |
| `app.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | |
| `app.storage.location` | `./uploads` | POD photos land here |
| `app.seed.enabled` | `true` | Set to `false` to stop demo data |
| `app.simulation.enabled` | `true` | Background driver movement |
| `app.simulation.interval-ms` | `8000` | |
| `app.notifications.email-enabled` | `false` | Needs `MAIL_USERNAME` / `MAIL_PASSWORD` |

## Tests

```bash
cd backend && mvn test
```

---

## Project layout

```
shiptrack-pro/
├── backend/
│   ├── src/main/java/com/shiptrack/
│   │   ├── config/       security, websocket, seeding, OpenAPI
│   │   ├── controller/   REST endpoints
│   │   ├── dto/          request and response records
│   │   ├── exception/    ApiException + global handler
│   │   ├── model/        Mongo documents and enums
│   │   ├── repository/   Spring Data repositories
│   │   ├── security/     JWT filter, principal, token service
│   │   ├── service/      business logic, ETA, routing, reports, simulator
│   │   └── util/         geo maths, tracking number generator
│   └── src/main/resources/application.yml
├── frontend/
│   └── src/
│       ├── api/          axios client and endpoint groups
│       ├── components/   layout, map, timeline, signature pad, badges
│       ├── context/      AuthContext
│       ├── hooks/        STOMP subscription hook
│       ├── pages/        one file per route
│       └── utils/        formatting helpers
└── docker-compose.yml
```

## Troubleshooting

- **Backend exits on startup** — MongoDB is not running. Start it, then retry.
- **Login returns 401 with the demo accounts** — the seeder only runs against an empty
  database. Drop it and restart: `mongosh shiptrackpro --eval "db.dropDatabase()"`.
- **Map tiles do not load** — OpenStreetMap tiles need internet access.
- **Live updates do not arrive** — the browser must be able to reach `/ws`. Check the dev
  proxy in `vite.config.js` and that the backend is on 8080.
- **Port already in use** — change `server.port` in `application.yml`, or the Vite port
  with `npm run dev -- --port 5174`.
