import { apiGet, apiPut, apiDelete, apiPost, apiPatch } from "./api";
import type { MessagePage, EditMessageRequest } from "@/types";
import { MESSAGE_PAGE_SIZE } from "@/lib/constants";

export async function fetchMessages(
  conversationId: number,
  page: number = 0,
  size: number = MESSAGE_PAGE_SIZE
): Promise<MessagePage> {
  return apiGet<MessagePage>(
    `/api/conversations/${conversationId}/messages?page=${page}&size=${size}`
  );
}

export async function editMessage(messageId: number, text: string): Promise<void> {
  await apiPut<void>(`/api/chat-message/messages/${messageId}/edit`, { text } as EditMessageRequest);
}

export async function deleteMessage(messageId: number): Promise<void> {
  await apiDelete<void>(`/api/chat-message/messages/${messageId}`);
}

/** Modern feature: react to a message with an emoji */
export async function reactToMessage(messageId: number, emoji: string): Promise<void> {
  await apiPost<void>(`/api/messages/${messageId}/react`, { emoji });
}

/** Modern feature: pin a message */
export async function pinMessage(messageId: number): Promise<void> {
  await apiPatch<void>(`/api/messages/${messageId}/pin`);
}
