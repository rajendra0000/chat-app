// ==============================
// WebSocket Service (STOMP + SockJS)
// — Uses absolute backend URL (not Next.js proxy)
// ==============================

import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_RECONNECT_DELAY, WS_HEARTBEAT_INTERVAL, API_BASE_URL } from "@/lib/constants";

type MessageHandler = (message: IMessage) => void;

let stompClient: Client | null = null;

/**
 * Get the JWT token from localStorage.
 * Returns the raw token without "Bearer " prefix.
 */
function getCleanToken(): string | null {
  if (typeof window === "undefined") return null;
  const token = localStorage.getItem("Authorization");
  if (!token) return null;
  return token.replace("Bearer ", "");
}

/**
 * Build the SockJS WebSocket URL.
 * Uses the backend URL directly (not via Next.js proxy) because
 * SockJS transport paths (xhr-streaming, etc.) conflict with Next.js page routes.
 */
function getWsUrl(): string {
  // API_BASE_URL is http://localhost:8080 — append /chat for the STOMP endpoint
  return `${API_BASE_URL}/chat`;
}

/**
 * Create and activate a STOMP client connection.
 */
export function connectWebSocket(
  onConnect: (client: Client) => void,
  onError?: (error: string) => void
): Client | null {
  const token = getCleanToken();
  if (!token) {
    onError?.("No authentication token found.");
    return null;
  }

  // Prevent duplicate connections
  if (stompClient?.connected) {
    onConnect(stompClient);
    return stompClient;
  }

  const wsUrl = `${getWsUrl()}?token=${encodeURIComponent(token)}`;
  const socket = new SockJS(wsUrl);

  const client = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    debug: () => {
      // STOMP debug logging disabled in production
    },
    reconnectDelay: WS_RECONNECT_DELAY,
    heartbeatIncoming: WS_HEARTBEAT_INTERVAL,
    heartbeatOutgoing: WS_HEARTBEAT_INTERVAL,
  });

  client.onConnect = () => {
    stompClient = client;
    onConnect(client);
  };

  client.onWebSocketError = (error) => {
    console.error("WebSocket error:", error);
    onError?.("WebSocket connection failed. Retrying...");
  };

  client.onStompError = (frame) => {
    console.error("STOMP error:", frame);
    const errorMsg = frame.headers["message"] || "STOMP connection error";
    if (errorMsg.includes("Unauthorized")) {
      onError?.("Authentication failed. Please log in again.");
      localStorage.removeItem("Authorization");
      window.location.href = "/login";
    } else {
      onError?.(errorMsg);
    }
  };

  client.activate();
  stompClient = client;
  return client;
}

/**
 * Disconnect the STOMP client.
 */
export function disconnectWebSocket(): void {
  if (stompClient?.connected) {
    stompClient.deactivate();
  }
  stompClient = null;
}

/**
 * Get the current STOMP client instance.
 */
export function getStompClient(): Client | null {
  return stompClient;
}

/**
 * Subscribe to a STOMP destination.
 * Returns an unsubscribe function.
 */
export function subscribe(
  destination: string,
  handler: MessageHandler
): (() => void) | null {
  if (!stompClient?.connected) {
    console.warn(`Cannot subscribe to ${destination}: not connected`);
    return null;
  }

  const subscription = stompClient.subscribe(destination, handler);
  return () => subscription.unsubscribe();
}

/**
 * Publish a message to a STOMP destination.
 */
export function publish(destination: string, body: unknown): void {
  if (!stompClient?.connected) {
    console.warn(`Cannot publish to ${destination}: not connected`);
    return;
  }

  stompClient.publish({
    destination,
    body: JSON.stringify(body),
  });
}
