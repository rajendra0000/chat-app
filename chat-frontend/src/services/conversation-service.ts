import { apiGet, apiPost, apiDelete, apiPatch } from "./api";
import type {
  Conversation,
  CreateConversationRequest,
  CreateGroupRequest,
} from "@/types";

export async function fetchConversations(): Promise<Conversation[]> {
  return apiGet<Conversation[]>("/api/conversations");
}

export async function createConversation(userId: number): Promise<Conversation> {
  return apiPost<Conversation>("/api/conversations", { userId } as CreateConversationRequest);
}

export async function createGroup(title: string, memberIds: number[] = []): Promise<Conversation> {
  return apiPost<Conversation>("/api/chats/groups", { title, memberIds } as CreateGroupRequest);
}

/** #7 — Delete (hide) private chat for me */
export async function hideConversation(chatId: number): Promise<void> {
  await apiDelete<void>(`/api/conversations/${chatId}`);
}

/** #4 — Rename group (admin only) */
export async function renameGroup(chatId: number, name: string): Promise<void> {
  await apiPatch<void>(`/api/conversations/${chatId}/rename`, { name });
}

/** #5 — Delete group entirely (only when sole member) */
export async function deleteGroup(chatId: number): Promise<void> {
  await apiDelete<void>(`/api/conversations/${chatId}/group`);
}

/** Leave a group chat */
export async function leaveGroup(chatId: number): Promise<void> {
  await apiPost<void>(`/api/chats/groups/${chatId}/leave`);
}

/** Legacy deleteConversation alias used by sidebar */
export async function deleteConversation(chatId: number): Promise<void> {
  await hideConversation(chatId);
}
