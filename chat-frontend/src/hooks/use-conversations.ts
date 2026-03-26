"use client";

// ==============================
// Conversations Hook (TanStack Query)
// ==============================

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import * as conversationService from "@/services/conversation-service";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";
import { useNotificationStore } from "@/store/notification-store";
import { CONVERSATIONS_STALE_TIME } from "@/lib/constants";
import type { Conversation } from "@/types";

const CONVERSATIONS_KEY = ["conversations"] as const;

export function useConversations() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const setConversations = useChatStore((s) => s.setConversations);
  const setAllUnreadCounts = useNotificationStore((s) => s.setAllUnreadCounts);

  return useQuery<Conversation[]>({
    queryKey: CONVERSATIONS_KEY,
    queryFn: async () => {
      const convos = await conversationService.fetchConversations();
      convos.sort(
        (a, b) =>
          new Date(b.timestamp || 0).getTime() -
          new Date(a.timestamp || 0).getTime()
      );
      setConversations(convos);
      // Seed notification store so badges show correct counts on first load
      const counts: Record<number, number> = {};
      convos.forEach((c) => { if (c.unreadCount > 0) counts[c.id] = c.unreadCount; });
      setAllUnreadCounts(counts);
      return convos;
    },
    staleTime: CONVERSATIONS_STALE_TIME,
    enabled: isAuthenticated(),
  });
}

export function useCreateConversation() {
  const queryClient = useQueryClient();
  const upsertConversation = useChatStore((s) => s.upsertConversation);

  return useMutation({
    mutationFn: (userId: number) => conversationService.createConversation(userId),
    onSuccess: (newConvo) => {
      // 1. Inject straight into TQ cache so sidebar re-renders immediately
      queryClient.setQueryData<Conversation[]>(CONVERSATIONS_KEY, (old = []) => {
        if (old.some((c) => c.id === newConvo.id)) return old;
        return [newConvo, ...old];
      });
      // 2. Sync to Zustand so WS handlers can reference it
      upsertConversation(newConvo);
    },
  });
}

export function useCreateGroup() {
  const queryClient = useQueryClient();
  const upsertConversation = useChatStore((s) => s.upsertConversation);

  return useMutation({
    mutationFn: ({ title, memberIds }: { title: string; memberIds?: number[] }) =>
      conversationService.createGroup(title, memberIds),
    onSuccess: (newGroup) => {
      // 1. Inject straight into TQ cache so sidebar re-renders immediately
      queryClient.setQueryData<Conversation[]>(CONVERSATIONS_KEY, (old = []) => {
        if (old.some((c) => c.id === newGroup.id)) return old;
        return [newGroup, ...old];
      });
      // 2. Sync to Zustand so WS handlers can reference it
      upsertConversation(newGroup);
    },
  });
}

export function useDeleteConversation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (chatId: number) => conversationService.deleteConversation(chatId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY });
    },
  });
}

export function useLeaveGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (chatId: number) => conversationService.leaveGroup(chatId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

export function useRenameGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ chatId, name }: { chatId: number; name: string }) =>
      conversationService.renameGroup(chatId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

export function useDeleteGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (chatId: number) => conversationService.deleteGroup(chatId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

export function useHideConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (chatId: number) => conversationService.hideConversation(chatId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

/** Invalidate conversations from outside React (e.g. WebSocket handler) */
export function invalidateConversationsKey() {
  return CONVERSATIONS_KEY;
}
