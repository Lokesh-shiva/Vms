# VMS — Vendor Mobility System

> **⚠️ PROPRIETARY SOFTWARE — ALL RIGHTS RESERVED**
>
> Copyright © 2026 **SAVIRIGANA LOKESHWARA RAO**. Unauthorized use, reproduction,
> distribution, or marketing of this source code is strictly prohibited and
> subject to legal action. See [LICENSE](./LICENSE) for details.

---

A full-stack **Vendor Mobility System** comprising a FastAPI backend and an Android admin application, designed for managing vendor bookings, payments, and fleet operations.

## Architecture

```
VMS/
├── backend/          # FastAPI REST API with PostgreSQL (Neon)
├── Vmsadminapp/      # Android Admin App (Kotlin, Jetpack Compose)
├── LICENSE           # Proprietary license
└── README.md
```

## Backend — FastAPI

Modular Python backend with a layered architecture:

- **Controller** → API routes (FastAPI routers)
- **Service** → Business logic
- **Repository** → Data access (SQLAlchemy ORM)
- **Database** → PostgreSQL via Neon

### Tech Stack

| Layer       | Technology                 |
|-------------|----------------------------|
| Framework   | FastAPI                    |
| ORM         | SQLAlchemy                 |
| Database    | PostgreSQL (Neon)          |
| Auth        | JWT (HS256)                |
| Server      | Uvicorn                    |
| Testing     | Pytest                     |

### Modules

`user` · `location` · `timeslot` · `cart_type` · `cart` · `item` · `booking` · `booking_item` · `payment` · `auth`

### Quick Start

```bash
# 1. Create virtual environment
python -m venv venv && venv\Scripts\activate

# 2. Install dependencies
pip install -r backend/requirements.txt

# 3. Configure environment
cp backend/.env.example backend/.env
# Set DATABASE_URL in .env

# 4. Run server
cd backend && uvicorn main:app --reload --port 8000
```

API docs: [Swagger UI](http://127.0.0.1:8000/docs) · [ReDoc](http://127.0.0.1:8000/redoc)

### Running Tests

```bash
cd backend && pytest
```

---

## Android Admin App — Jetpack Compose

Native Android admin panel for managing bookings and reviewing payments.

### Tech Stack

| Layer       | Technology                    |
|-------------|-------------------------------|
| Language    | Kotlin                        |
| UI          | Jetpack Compose (Material 3)  |
| Networking  | Retrofit + OkHttp             |
| Auth        | JWT with token interceptor    |
| Architecture| MVVM + StateFlow              |

### Features

- **Dashboard** — Real-time metric cards (Active Bookings, Pending Payments, Completed Today, Total)
- **Bookings Management** — View, Start, Complete, Cancel bookings with confirmation dialogs
- **Payment Review** — Approve/Reject payments with UNDER_REVIEW filtering
- **Pull-to-Refresh** — Swipe down to refresh data on Bookings and Payments
- **Skeleton Loading** — Shimmer effect placeholders during data loading
- **Glassmorphism UI** — Modern translucent card design with gradient borders
- **Animated Entry** — Staggered fade-in and slide-up animations for list items

### Design System

| Token             | Value         |
|-------------------|---------------|
| Screen Title      | 20sp Bold     |
| Section Title     | 16sp SemiBold |
| Primary Text      | 14sp          |
| Metadata Text     | 12sp          |
| Card Padding      | 16dp          |
| Card Spacing      | 12dp          |
| Card Corner Radius| 12dp          |

### Build

Open `Vmsadminapp/` in Android Studio and run on a device or emulator.

---

## Legal Notice

```
Copyright (c) 2026 SAVIRIGANA LOKESHWARA RAO. All rights reserved.

This project is proprietary software. No license is granted for use,
modification, distribution, or reproduction of any part of this software
without explicit written permission from the copyright holder.

Unauthorized use, copying, redistribution, or commercial exploitation of
this source code is strictly prohibited and will be subject to legal
proceedings under applicable intellectual property laws.
```

## Contact

For licensing inquiries or authorized use, contact **SAVIRIGANA LOKESHWARA RAO** directly.
