// ==============================
// Message Types
// ==============================

export interface MessageDocument {
  id: number;
  fileName: string;
  fileType: string;
  url: string;
}

export interface Message {
  id: number | string;
  senderId: number;
  senderName?: string;
  conversationId: number;
  text: string;
  timestamp: string;
  deleted: boolean;
  edited: boolean;
  read?: boolean;
  documents?: MessageDocument[];
  unreadCount?: number;
  /** Emoji reactions: emoji → count */
  reactions?: Record<string, number>;
  /** Whether this message is pinned in the chat */
  pinned?: boolean;
  /** The message this is replying to (for quote-reply) */
  replyTo?: {
    id: number | string;
    senderName?: string;
    text: string;
  };
}

export interface MessagePage {
  content: Message[];
  last: boolean;
  totalPages?: number;
  totalElements?: number;
  number?: number;
}

export interface SendMessageRequest {
  conversationId: number;
  text: string;
  documentId?: number | null;
}

export interface EditMessageRequest {
  text: string;
}

export interface UploadResponse {
  documentId: number;
  url: string;
  fileName: string;
}

export interface DocumentUrlResponse {
  url: string;
}
