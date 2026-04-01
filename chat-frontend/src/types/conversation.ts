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
  participants: number[] | null;
  blockedByCurrentUser: boolean;
  currentUserBlocked: boolean;
  status: string | null;
  /** Profile picture URL — other user's pic for PRIVATE, group pic for GROUP */
  avatarUrl?: string | null;
}

export interface CreateConversationRequest {
  userId: number;
}

export interface CreateGroupRequest {
  title: string;
  memberIds: number[];
}
