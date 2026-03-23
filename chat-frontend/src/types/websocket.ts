// ==============================
// WebSocket Payload Types
// ==============================

export interface WsNotification {
  chatId: number;
  unreadCount: number;
  latestMessage: string | null;
  timestamp: string | null;
}

export interface WsReadReceipt {
  chatId: number;
  unreadCount: number;
}

export interface WsStatusUpdate {
  userId: number;
  status: string;
}

export interface WsTypingIndicator {
  userId?: number;
  isTyping?: boolean;
  groupId?: number;
  typingMessage?: string;
}

export interface WsErrorPayload {
  error: string;
}

export interface WsRefreshPayload {
  chatId: number;
}

export interface WsUserStatus {
  status: "active" | "inactive";
}

export interface WsMarkRead {
  chatId: number;
}
