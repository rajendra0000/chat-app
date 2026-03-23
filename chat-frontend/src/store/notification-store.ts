// ==============================
// Notification Store (Zustand)
// ==============================

import { create } from "zustand";

interface NotificationState {
  /** Total unread message count across all chats */
  totalUnread: number;
  /** Per-conversation unread counts */
  unreadCounts: Record<number, number>;

  /** Set unread count for a conversation */
  setUnreadCount: (chatId: number, count: number) => void;
  /** Recalculate total unread from individual counts */
  recalculateTotal: () => void;
  /** Clear unread count for a conversation */
  clearUnread: (chatId: number) => void;
  /** Set all unread counts at once (e.g. from initial load) */
  setAllUnreadCounts: (counts: Record<number, number>) => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  totalUnread: 0,
  unreadCounts: {},

  setUnreadCount: (chatId, count) => {
    set((state) => {
      const newCounts = { ...state.unreadCounts, [chatId]: count };
      const total = Object.values(newCounts).reduce((sum, c) => sum + c, 0);
      return { unreadCounts: newCounts, totalUnread: total };
    });
  },

  recalculateTotal: () => {
    const total = Object.values(get().unreadCounts).reduce((sum, c) => sum + c, 0);
    set({ totalUnread: total });
  },

  clearUnread: (chatId) => {
    set((state) => {
      const newCounts = { ...state.unreadCounts };
      delete newCounts[chatId];
      const total = Object.values(newCounts).reduce((sum, c) => sum + c, 0);
      return { unreadCounts: newCounts, totalUnread: total };
    });
  },

  setAllUnreadCounts: (counts) => {
    const total = Object.values(counts).reduce((sum, c) => sum + c, 0);
    set({ unreadCounts: counts, totalUnread: total });
  },
}));
