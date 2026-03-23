# SkibidiChat — Frontend

Next.js 15 frontend for SkibidiChat — a real-time chat application with WebSocket messaging, file sharing, and email OTP authentication.

---

## 🛠️ Tech Stack

| Tool | Purpose |
|---|---|
| Next.js 15 (App Router) | React framework |
| TypeScript | Type safety |
| TailwindCSS | Styling |
| TanStack Query | Server state & caching |
| Zustand | Client state (auth, UI) |
| STOMP.js + SockJS | WebSocket client |
| React Hook Form + Zod | Form validation |
| Lucide React | Icons |

---

## 📁 Project Structure

```
src/
├── app/
│   ├── (auth)/login/        # Login & OTP flow
│   └── (app)/
│       ├── chat/            # Main chat page
│       └── settings/        # User settings
├── components/
│   ├── auth/                # PhoneInput, OtpInput, RegistrationForm
│   ├── chat/                # MessageList, MessageInput, Sidebar, etc.
│   └── ui/                  # Shared UI primitives (Button, Input, etc.)
├── hooks/                   # useAuth, useWebSocket, useConversations, etc.
├── services/                # API, WebSocket, file service
├── store/                   # Zustand stores (auth, UI)
└── types/                   # TypeScript types
```

---

## ⚙️ Environment Variables

Copy `.env.example` to `.env.local` for local development:

```bash
cp .env.example .env.local
```

| Variable | Description | Example |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Spring Boot backend URL | `http://localhost:8080` |
| `NEXT_PUBLIC_WS_URL` | WebSocket endpoint | `http://localhost:8080/chat` |
| `NEXT_PUBLIC_DEFAULT_AVATAR` | Fallback avatar path | `/default-avatar.jpg` |

> **Note:** All frontend env vars must be prefixed with `NEXT_PUBLIC_` to be exposed to the browser.

---

## 🚀 Local Development

### Prerequisites
- Node.js 20+
- npm or yarn
- Backend running on `http://localhost:8080`

### Steps

```bash
# Install dependencies
npm install

# Start dev server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

---

## 🏗️ Build

```bash
npm run build
npm run start
```

---

## 🌐 Deploy to Vercel

1. Push your code to GitHub
2. Import the project in [Vercel](https://vercel.com)
3. Set `Root Directory` to `chat-frontend`
4. Add environment variables in Vercel dashboard:

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend.onrender.com
NEXT_PUBLIC_WS_URL=https://your-backend.onrender.com/chat
NEXT_PUBLIC_DEFAULT_AVATAR=/default-avatar.jpg
```

5. Deploy — Vercel auto-deploys on every push to `main`

---

## 🔐 Authentication Flow

1. User enters **phone number** (E.164, e.g. `+919876543210`) + **email**
2. If existing user → OTP sent to email → verify → JWT issued → redirect to `/chat`
3. If new user → registration form → OTP sent → verify → redirect to `/chat`
4. JWT stored in `localStorage` as `Authorization` header value

---

## 💬 WebSocket

- Connection: `ws://localhost:8080/chat` (SockJS transport)
- Protocol: STOMP
- Auth: JWT token passed as query param and connect header
- Subscriptions:
  - `/user/queue/messages` — incoming messages
  - `/user/queue/notifications` — notifications
  - `/topic/chat/{chatId}` — group chat messages

---

## 📱 Mobile

The app is fully responsive with:
- Mobile-optimized sidebar navigation
- iOS safe-area insets support
- Touch-friendly tap targets
