# SkibidiChat — Backend

Spring Boot 3.5 backend for SkibidiChat — a production-ready real-time chat API with WebSocket (STOMP), email OTP authentication, AI integration, file uploads, and async message processing.

---

## 🛠️ Tech Stack

| Tool | Purpose |
|---|---|
| Spring Boot 3.5 (Java 21) | Core framework |
| Spring Security + JWT | Authentication & authorization |
| Spring WebSocket + STOMP | Real-time messaging |
| Spring Data JPA + Hibernate | Database ORM |
| PostgreSQL | Primary database |
| Flyway | Database migrations |
| Redis | OTP storage, rate limiting, presence tracking |
| RabbitMQ | Async message processing queue |
| Cloudinary | File/image/video storage |
| Gmail SMTP (JavaMail) | Email OTP delivery |
| Groq API | AI bot (Llama 3.3 70B) |
| Spring Actuator | Health checks & metrics |

---

## 📁 Project Structure

```
src/main/java/com/chatapp/backend/
├── controller/          # REST controllers (Auth, Chat, User, File, AI)
├── service/             # Business logic interfaces
│   └── impl/            # Implementations (OtpServiceImpl, etc.)
├── model/               # JPA entities (User, Chat, Message, Document)
├── dto/                 # Request/Response DTOs
├── repository/          # Spring Data JPA repositories
├── security/            # JWT, CustomUserDetailsService, SecurityConfig
├── config/              # WebSocket, CORS, RabbitMQ, Cloudinary, WebClient configs
├── mapper/              # Entity ↔ DTO mappers
├── ai/                  # AI bot integration (Groq)
├── seeder/              # DB seeders (roles)
└── enums/               # ChatType, etc.

src/main/resources/
├── application.properties   # All config (env-var backed)
└── db/migration/
    └── V1__init_schema.sql  # Single consolidated schema migration
```

---

## ⚙️ Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

| Variable | Description | Example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/chatapp` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `chatuser` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `chatpassword` |
| `SPRING_REDIS_URL` | Redis URL | `redis://localhost:6379` |
| `RABBITMQ_URI` | RabbitMQ URI | `amqp://guest:guest@localhost:5672` |
| `JWT_SECRET` | JWT signing secret (64+ chars) | `8f3c2b1a...` |
| `JWT_EXPIRATION` | JWT expiry in ms | `1296000000` (15 days) |
| `MAIL_USERNAME` | Gmail address for OTP | `yourapp@gmail.com` |
| `MAIL_PASSWORD` | Gmail App Password | `xxxx xxxx xxxx xxxx` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | `davfq7v7a` |
| `CLOUDINARY_API_KEY` | Cloudinary API key | `123456789` |
| `CLOUDINARY_API_SECRET` | Cloudinary secret | `abc123...` |
| `GROK_API_KEY` | Groq API key | `gsk_...` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `https://yourapp.vercel.app` |
| `APP_NAME` | App name in emails | `SkibidiChat` |

### Gmail App Password Setup
1. Enable 2FA on your Google account
2. Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
3. Generate App Password for **Mail**
4. Use that 16-char password as `MAIL_PASSWORD`

---

## 🚀 Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

### Steps

```bash
# 1. Start infrastructure services
docker compose up -d
# This starts: PostgreSQL (5432), Redis (6379), RabbitMQ (5672)

# 2. Copy and fill env vars
cp .env.example .env
# Edit .env with your credentials

# 3. Run the application
mvn spring-boot:run
```

API runs on: `http://localhost:8080`

### Verify startup
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

## 🌐 Deploy to Render

1. Push code to GitHub
2. Create a new **Web Service** on [Render](https://render.com)
3. Set:
   - **Root Directory**: `chat-backend`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/chat-backend-0.0.1-SNAPSHOT.jar`
4. Add all environment variables from the table above
5. First deploy: set `CORS_ALLOWED_ORIGINS=*`, then tighten after getting your Vercel URL

### Managed Services (free tiers)
| Service | Provider | Free Tier |
|---|---|---|
| PostgreSQL | [Neon](https://neon.tech) | 0.5 GB storage |
| Redis | [Upstash](https://upstash.com) | 10K commands/day |
| RabbitMQ | [CloudAMQP](https://www.cloudamqp.com) | Little Lemur (1M msgs/month) |

---

## 🔐 Auth Flow

```
POST /auth/check-user   { phone }              → { userExists }
POST /auth/send-otp     { phone, email }        → OTP sent to email
POST /auth/verify-otp   { phone, otp }          → { token, userExists }
POST /auth/register     { phone, fullName, ... } → { success }
```

All other endpoints require `Authorization: Bearer <jwt>` header.

---

## 📡 WebSocket (STOMP)

**Connection endpoint:** `ws://localhost:8080/chat`

| Destination | Direction | Purpose |
|---|---|---|
| `/app/chat.sendMessage` | Client → Server | Send message |
| `/app/chat.typing` | Client → Server | Typing indicator |
| `/topic/chat/{chatId}` | Server → Client | Group messages |
| `/user/queue/messages` | Server → Client | Private messages |
| `/user/queue/notifications` | Server → Client | Notifications |

---

## 🗄️ Database

Single Flyway migration: `V1__init_schema.sql`

Key tables:
- `users` — `phone` (unique, E.164), `email` (unique), `full_name`
- `chats` — `chat_type` (PRIVATE/GROUP)
- `chat_members` — user ↔ chat mapping
- `chat_messages` — messages with `pinned`, `hidden_for_user`
- `message_reactions` — emoji reactions
- `documents` — file attachments (Cloudinary)
- `roles` — USER / ADMIN

---

## 🤖 AI Bot

Mention `@kalori` in any group chat to trigger the AI bot.

- Model: Groq Llama 3.3 70B
- Rate limited: 20 requests/minute
- Async via RabbitMQ — won't block chat
- Dead-letter queue for failed messages