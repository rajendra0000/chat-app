// ==============================
// Constants & Environment Config
// ==============================

/** Backend API base URL — proxied through Next.js rewrites in dev */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

/** WebSocket STOMP endpoint */
export const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080/chat";

/** Default avatar fallback path */
export const DEFAULT_AVATAR = process.env.NEXT_PUBLIC_DEFAULT_AVATAR || "/default-avatar.jpg";

/** Message pagination size */
export const MESSAGE_PAGE_SIZE = 20;

/** User search minimum query length */
export const MIN_SEARCH_LENGTH = 3;

/** OTP length */
export const OTP_LENGTH = 6;

/** OTP countdown seconds */
export const OTP_COUNTDOWN_SECONDS = 60;

/** Max file upload size in bytes (10MB) */
export const MAX_FILE_SIZE = 10 * 1024 * 1024;

/** WebSocket reconnect delay in ms */
export const WS_RECONNECT_DELAY = 5000;

/** WebSocket heartbeat interval in ms */
export const WS_HEARTBEAT_INTERVAL = 4000;

/** TanStack Query stale time for conversations (5 minutes) */
export const CONVERSATIONS_STALE_TIME = 5 * 60 * 1000;

/** TanStack Query stale time for user profile (10 minutes) */
export const USER_PROFILE_STALE_TIME = 10 * 60 * 1000;
