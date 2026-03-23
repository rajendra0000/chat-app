// ==============================
// Chat Management Service
// ==============================

import { apiGet, apiPost, apiPut, apiDelete, apiUpload } from "./api";
import type { ChatMember, Conversation } from "@/types";

/** Get group chat profile picture URL */
export async function getChatProfilePicUrl(chatId: number): Promise<string> {
  const url = await apiGet<string>(`/api/chats/${chatId}/pic-url`, "text");
  return url || "";
}

/** Upload group chat profile picture */
export async function uploadChatProfilePic(chatId: number, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  await apiUpload<void>(`/api/chats/${chatId}/upload-profile`, formData);
}

/** Get all members of a group chat */
export async function fetchMembers(chatId: number): Promise<ChatMember[]> {
  return apiGet<ChatMember[]>(`/api/chats/${chatId}/members`);
}

/** Add a member to a group by phone */
export async function addMember(chatId: number, phone: string): Promise<void> {
  await apiPost<void>(`/api/chats/${chatId}/add-member?phone=${encodeURIComponent(phone)}`);
}

/** Block a member in a group (admin only) */
export async function blockMember(chatId: number, userId: number): Promise<void> {
  await apiPut<void>(`/api/chats/${chatId}/block-member?userId=${userId}`);
}

/** Unblock a member in a group (admin only) */
export async function unblockMember(chatId: number, userId: number): Promise<void> {
  await apiPut<void>(`/api/chats/${chatId}/unblock-member?userId=${userId}`);
}

/** Remove a member from a group (admin only) */
export async function removeMember(chatId: number, userId: number): Promise<void> {
  await apiDelete<void>(`/api/chats/${chatId}/remove-member?userId=${userId}`);
}

/** Block a user in a private chat */
export async function blockUser(chatId: number, targetUserId: number): Promise<Conversation> {
  return apiPut<Conversation>(
    `/api/chat-message/chats/${chatId}/block-user?targetUserId=${targetUserId}`
  );
}

/** Unblock a user in a private chat */
export async function unblockUser(chatId: number, targetUserId: number): Promise<Conversation> {
  return apiPut<Conversation>(
    `/api/chat-message/chats/${chatId}/unblock-user?targetUserId=${targetUserId}`
  );
}
