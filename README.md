# SkibidiChat 💬

A production-ready, real-time chat application built with **Next.js 15** and **Spring Boot 3.5**, featuring WebSocket messaging, AI integration, file sharing, and email OTP authentication.

---

## 📸 Overview

SkibidiChat is a full-stack mono-repo chat platform with:

- ⚡ **Real-time messaging** via STOMP over WebSockets (SockJS)
- 🔐 **Email OTP authentication** — no passwords, just phone + email verification
- 🤖 **AI bot** powered by Groq (Llama 3.3) — mention `@kalori` in any group chat
- 📎 **File sharing** — images, videos, documents via Cloudinary
- 👥 **Group & private chats** — with typing indicators, read receipts, unread badges
- 📱 **Mobile-first responsive** design
- 🔔 **Real-time notifications** via WebSocket

---

## 🏗️ Architecture

```
chat-app/
├── chat-frontend/     # Next.js 15 + TypeScript (Vercel)
└── chat-backend/      # Spring Boot 3.5 + Java 21 (Render)
```

| Layer | Technology |
|---|---|
| Frontend | Next.js 15, TypeScript, TailwindCSS, TanStack Query |
| Backend | Spring Boot 3.5, Java 21, Spring Security + JWT |
| Database | PostgreSQL (Neon) + Flyway migrations |
| Cache/OTP | Redis (Upstash) |
| Message Queue | RabbitMQ (CloudAMQP) |
| File Storage | Cloudinary |
| Email OTP | Gmail SMTP (spring-boot-starter-mail) |
| AI | Groq API (Llama 3.3 70B) |
| Realtime | STOMP over WebSocket (SockJS) |

---

## 🚀 Quick Start

### Prerequisites
- Node.js 20+, npm
- Java 21, Maven
- Docker (for PostgreSQL, Redis, RabbitMQ locally)

### 1. Clone
```bash
git clone <your-repo-url>
cd chat-app
```

### 2. Start Backend
```bash
cd chat-backend
cp .env.example .env      # Fill in your credentials
docker compose up -d      # Start PostgreSQL, Redis, RabbitMQ
mvn spring-boot:run
```

### 3. Start Frontend
```bash
cd chat-frontend
cp .env.example .env.local
# Set NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

---

## 🌐 Deployment

| Service | Platform |
|---|---|
| Frontend | [Vercel](https://vercel.com) |
| Backend | [Render](https://render.com) |
| Database | [Neon](https://neon.tech) (PostgreSQL) |
| Redis | [Upstash](https://upstash.com) |
| RabbitMQ | [CloudAMQP](https://www.cloudamqp.com) |

See individual READMEs for detailed deployment steps:
- [`chat-frontend/README.md`](./chat-frontend/README.md)
- [`chat-backend/README.md`](./chat-backend/README.md)

---

## 📄 License

MIT
