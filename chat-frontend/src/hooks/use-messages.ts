"use client";

// ==============================
// Messages Hook (TanStack Query + Infinite)
// ==============================

import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { fetchMessages } from "@/services/message-service";
import { useAuthStore } from "@/store/auth-store";
import type { Message, MessagePage } from "@/types";
import { MESSAGE_PAGE_SIZE } from "@/lib/constants";
import { useCallback } from "react";

export function messagesKey(conversationId: number) {
  return ["messages", conversationId] as const;
}

export function useMessages(conversationId: number | null) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return useInfiniteQuery<MessagePage>({
    queryKey: messagesKey(conversationId!),
    queryFn: async ({ pageParam }) => {
      return fetchMessages(conversationId!, pageParam as number, MESSAGE_PAGE_SIZE);
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, _allPages, lastPageParam) => {
      if (lastPage.last) return undefined;
      return (lastPageParam as number) + 1;
    },
    enabled: !!conversationId && isAuthenticated(),
    staleTime: 60_000,
  });
}

/**
 * Hook to add a new message to the query cache optimistically.
 * Used by the WebSocket handler when a new message arrives.
 */
export function useAddMessageToCache() {
  const queryClient = useQueryClient();

  return useCallback(
    (conversationId: number, message: Message) => {
      queryClient.setQueryData(
        messagesKey(conversationId),
        (old: { pages: MessagePage[]; pageParams: unknown[] } | undefined) => {
          if (!old) return old;

          // Check if this exact message ID already exists (dedup for re-deliveries)
          const exists = old.pages.some((page) =>
            page.content.some((m) => String(m.id) === String(message.id))
          );
          if (exists) return old;

          const isRealMessage = typeof message.id === "number";

          // For real (server-confirmed) messages: evict any pending optimistic messages
          // with the same text so we don't show the message twice
          const newPages = old.pages.map((page) => ({
            ...page,
            content: isRealMessage
              ? page.content.filter(
                  (m) =>
                    !(
                      typeof m.id === "string" &&
                      m.id.startsWith("pending-") &&
                      m.text === message.text &&
                      m.conversationId === message.conversationId
                    )
                )
              : page.content,
          }));

          // PREPEND to pages[0] (newest-first order) so after the display reversal
          // the new message appears at the bottom of the chat, not the top
          newPages[0] = {
            ...newPages[0],
            content: [message, ...newPages[0].content],
          };

          return { ...old, pages: newPages };
        }
      );
    },
    [queryClient]
  );
}

/**
 * Hook to update a message in the query cache (e.g. edit or delete).
 */
export function useUpdateMessageInCache() {
  const queryClient = useQueryClient();

  return useCallback(
    (conversationId: number, messageId: number | string, updates: Partial<Message>) => {
      queryClient.setQueryData(
        messagesKey(conversationId),
        (old: { pages: MessagePage[]; pageParams: unknown[] } | undefined) => {
          if (!old) return old;

          const newPages = old.pages.map((page) => ({
            ...page,
            content: page.content.map((m) =>
              m.id === messageId ? { ...m, ...updates } : m
            ),
          }));

          return { ...old, pages: newPages };
        }
      );
    },
    [queryClient]
  );
}
