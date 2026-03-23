-- =============================================================================
-- V1: Complete baseline schema for chat-backend (production)
-- =============================================================================
-- Single consolidated migration — covers everything from local V1–V7.
-- Safe to run on a fresh database (Neon or any PostgreSQL instance).
-- =============================================================================

-- ── Roles ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL      PRIMARY KEY,
    name VARCHAR(255)
);

-- ── Users ────────────────────────────────────────────────────────────────────
-- phone: E.164 international format e.g. +919876543210 (up to +15 digits + '+' = 16 chars)
-- email: stored for OTP delivery and profile; unique to avoid duplicate accounts
CREATE TABLE IF NOT EXISTS users (
    id             SERIAL       PRIMARY KEY,
    phone          VARCHAR(20)  UNIQUE NOT NULL,   -- E.164 e.g. +919876543210
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) UNIQUE,            -- OTP delivery + profile
    date_of_birth  VARCHAR(20),
    gender         VARCHAR(10),
    address        TEXT,
    profile_pic_id INTEGER,
    status         VARCHAR(20),
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ── User ↔ Role join ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id INTEGER NOT NULL REFERENCES users(id),
    role_id INTEGER NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- ── Chats ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chats (
    id            SERIAL       PRIMARY KEY,
    title         VARCHAR(255),
    chat_type     VARCHAR(20),
    initiated_by  INTEGER,
    profile_pic_id INTEGER,
    unread_count  INTEGER      DEFAULT 0,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ── Chat Members ─────────────────────────────────────────────────────────────
-- hidden_for_user: supports "delete chat for me" without removing the other member
CREATE TABLE IF NOT EXISTS chat_members (
    id              SERIAL    PRIMARY KEY,
    chat_id         INTEGER   NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id         INTEGER   NOT NULL REFERENCES users(id),
    is_admin        BOOLEAN   NOT NULL DEFAULT FALSE,
    blocked         BOOLEAN   NOT NULL DEFAULT FALSE,
    hidden_for_user BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_chat_members_chat_user UNIQUE (chat_id, user_id)
);

-- ── Chat Messages ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_messages (
    id             SERIAL        PRIMARY KEY,
    chat_member_id INTEGER       REFERENCES chat_members(id),
    content        TEXT,
    edited         BOOLEAN       DEFAULT FALSE,
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    read           BOOLEAN       NOT NULL DEFAULT FALSE,
    pinned         BOOLEAN       NOT NULL DEFAULT FALSE,
    message_uuid   VARCHAR(36)   UNIQUE,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ── Documents ────────────────────────────────────────────────────────────────
-- path: stores Cloudinary permanent secure_url (replaces MinIO presigned URLs)
CREATE TABLE IF NOT EXISTS documents (
    id          SERIAL        PRIMARY KEY,
    external_id VARCHAR(255),
    storage_env VARCHAR(255),
    path        VARCHAR(1000),            -- Cloudinary secure_url can be long
    mime_type   VARCHAR(100),
    file_name   VARCHAR(500),
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ── Chat Message Attachments ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_message_attachments (
    id              SERIAL    PRIMARY KEY,
    chat_message_id INTEGER   REFERENCES chat_messages(id) ON DELETE CASCADE,
    document_id     INTEGER   REFERENCES documents(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Message Reactions ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS message_reactions (
    id         SERIAL       PRIMARY KEY,
    message_id INTEGER      NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    user_id    INTEGER      NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
    emoji      VARCHAR(8)   NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reaction_message_user_emoji UNIQUE (message_id, user_id, emoji)
);

-- =============================================================================
-- Indexes
-- =============================================================================

-- Users
CREATE INDEX IF NOT EXISTS idx_users_phone         ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_email         ON users(email)   WHERE email IS NOT NULL;

-- Chat members
CREATE INDEX IF NOT EXISTS idx_chat_members_chat_user ON chat_members(chat_id, user_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_user_id   ON chat_members(user_id);

-- Messages
CREATE INDEX IF NOT EXISTS idx_chat_messages_member_created ON chat_messages(chat_member_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_read           ON chat_messages(read)         WHERE read = FALSE;
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at     ON chat_messages(created_at    DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_messages_message_uuid_unique
    ON chat_messages(message_uuid) WHERE message_uuid IS NOT NULL;

-- Documents
CREATE INDEX IF NOT EXISTS idx_documents_external_id ON documents(external_id);

-- Reactions
CREATE INDEX IF NOT EXISTS idx_reactions_message_id ON message_reactions(message_id);

-- =============================================================================
-- Seed data
-- =============================================================================
INSERT INTO roles (name) VALUES ('USER')  ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT DO NOTHING;
