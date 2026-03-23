// ==============================
// Chat Store (Zustand)
// ==============================

import { create } from "zustand";
import type { Conversation } from "@/types";

interface ChatState {
  /** Currently active conversation ID */
  activeConversationId: number | null;
  /** Cached conversation list */
  conversations: Conversation[];
  /** Whether the mobile sidebar is open */
  isSidebarOpen: boolean;

  /** Set the active conversation */
  setActiveConversation: (id: number | null) => void;
  /** Set the conversation list */
  setConversations: (conversations: Conversation[]) => void;
  /** Update a single conversation in the list */
  updateConversation: (id: number, updates: Partial<Conversation>) => void;
  /** Move the updated conversation to the top of the list (for real-time sort) */
  bumpConversationToTop: (id: number, updates: Partial<Conversation>) => void;
  /** Upsert: insert at top if not present, update in-place if present */
  upsertConversation: (conversation: Conversation) => void;
  /** Toggle the mobile sidebar */
  toggleSidebar: () => void;
  /** Set the sidebar open state */
  setSidebarOpen: (open: boolean) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  activeConversationId: null,
  conversations: [],
  isSidebarOpen: false,

  setActiveConversation: (id) => set({ activeConversationId: id }),

  setConversations: (conversations) => set({ conversations }),

  updateConversation: (id, updates) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === id ? { ...c, ...updates } : c
      ),
    })),

  bumpConversationToTop: (id, updates) =>
    set((state) => {
      const list = state.conversations.map((c) =>
        c.id === id ? { ...c, ...updates } : c
      );
      const idx = list.findIndex((c) => c.id === id);
      if (idx > 0) {
        const [item] = list.splice(idx, 1);
        list.unshift(item);
      }
      return { conversations: list };
    }),

  upsertConversation: (conversation) =>
    set((state) => {
      const exists = state.conversations.some((c) => c.id === conversation.id);
      if (exists) {
        // Update in-place
        return {
          conversations: state.conversations.map((c) =>
            c.id === conversation.id ? { ...c, ...conversation } : c
          ),
        };
      }
      // Insert at top (newest first)
      return { conversations: [conversation, ...state.conversations] };
    }),

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),

  setSidebarOpen: (open) => set({ isSidebarOpen: open }),
}));
