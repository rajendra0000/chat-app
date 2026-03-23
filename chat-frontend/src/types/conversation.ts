// ==============================
// Conversation Types
// ==============================

export type ChatType = "PRIVATE" | "GROUP";

export interface Conversation {
  id: number;
  name: string;
  chatType: ChatType;
  lastMessage: string | null;
  timestamp: string | null;
  unreadCount: number;
  deleted: boolean;
  participants: number[];
  blockedByCurrentUser: boolean;
  currentUserBlocked: boolean;
  status: string | null;
}

export interface CreateConversationRequest {
  userId: number;
}

export interface CreateGroupRequest {
  title: string;
  memberIds: number[];
}
