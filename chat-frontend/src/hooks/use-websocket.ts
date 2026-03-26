"use client";

// ==============================
// WebSocket Hook — Fixed for backend integration
// — Correct STOMP destinations, topic-based typing, exponential backoff
// ==============================

import { useEffect, useRef, useCallback, useState } from "react";
import { connectWebSocket, disconnectWebSocket, publish } from "@/services/websocket-service";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";
import { useNotificationStore } from "@/store/notification-store";
import { useAddMessageToCache } from "./use-messages";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { notifyUser } from "@/lib/notification-sound";
import type { Client, IMessage } from "@stomp/stompjs";
import type { Message, WsNotification, WsReadReceipt } from "@/types";

interface TypingState {
  [conversationId: number]: string | null;
}

/**
 * Parse an incoming WebSocket frame body as a Message.
 * Returns null if the payload is not a real chat message
 * (e.g. AI_THINKING or SYSTEM system events have no numeric `id`).
 */
function parseMessage(body: string): Message | null {
  try {
    const data = JSON.parse(body);
    // Only accept payloads that have a numeric/string message id
    // AI_THINKING and SYSTEM payloads sent by the backend have a `type` field but no `id`
    if (data.id == null) return null;
    return data as Message;
  } catch {
    return null;
  }
}

export function useWebSocket() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const activeConversationId = useChatStore((s) => s.activeConversationId);
  const updateConversation = useChatStore((s) => s.updateConversation);
  const bumpConversationToTop = useChatStore((s) => s.bumpConversationToTop);
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount);
  const addMessageToCache = useAddMessageToCache();
  const queryClient = useQueryClient();

  const [typingStates, setTypingStates] = useState<TypingState>({});
  const [isConnected, setIsConnected] = useState(false);
  const [onlineStatuses, setOnlineStatuses] = useState<Record<string, string>>({});
  const unsubscribesRef = useRef<(() => void)[]>([]);
  const typingSubRef = useRef<(() => void) | null>(null);
  const messageTopicSubRef = useRef<(() => void) | null>(null);
  const reconnectAttemptRef = useRef(0);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const heartbeatTimerRef = useRef<ReturnType<typeof setInterval>>(undefined);
  const clientRef = useRef<Client | null>(null);

  // Keep a ref so subscription callbacks always see the latest value
  const activeConversationIdRef = useRef<number | null>(null);
  const addMessageToCacheRef = useRef(addMessageToCache);
  const bumpConversationToTopRef = useRef(bumpConversationToTop);
  // Ref to always-current conversations for "is it new?" check
  const conversationsRef = useRef<import("@/types").Conversation[]>([]);
  const upsertConversation = useChatStore((s) => s.upsertConversation);
  const conversations = useChatStore((s) => s.conversations);
  // Keep conversationsRef in sync
  useEffect(() => { conversationsRef.current = conversations; }, [conversations]);

  useEffect(() => {
    activeConversationIdRef.current = activeConversationId;
  }, [activeConversationId]);

  useEffect(() => {
    addMessageToCacheRef.current = addMessageToCache;
    bumpConversationToTopRef.current = bumpConversationToTop;
  }, [addMessageToCache, bumpConversationToTop]);

  // ─── Helper: subscribe to the per-conversation message topic ───────────────
  const subscribeToMessageTopic = useCallback((client: Client, conversationId: number) => {
    // Unsubscribe from previous conversation
    messageTopicSubRef.current?.();
    messageTopicSubRef.current = null;

    if (!client.connected || !conversationId) return;

    const sub = client.subscribe(`/topic/messages-${conversationId}`, (frame: IMessage) => {
      try {
        const data = JSON.parse(frame.body);

        // ── System events (non-message payloads) ───────────────────────────
        if (data.type === "GROUP_RENAMED" && data.chatId && data.name) {
          updateConversation(data.chatId, { name: data.name });
          queryClient.invalidateQueries({ queryKey: ["conversations"] });
          return;
        }

        if (data.type === "REACTION_UPDATE" && data.messageId != null) {
          // Update reaction counts on the cached message for this conversation
          queryClient.setQueryData(
            ["messages", conversationId],
            (old: import("@tanstack/react-query").InfiniteData<import("@/types").MessagePage> | undefined) => {
              if (!old) return old;
              return {
                ...old,
                pages: old.pages.map((page) => ({
                  ...page,
                  content: page.content.map((msg) =>
                    msg.id === data.messageId ? { ...msg, reactions: data.reactions } : msg
                  ),
                })),
              };
            }
          );
          return;
        }

        if (data.type === "PIN_TOGGLED" && data.messageId != null) {
          queryClient.setQueryData(
            ["messages", conversationId],
            (old: import("@tanstack/react-query").InfiniteData<import("@/types").MessagePage> | undefined) => {
              if (!old) return old;
              return {
                ...old,
                pages: old.pages.map((page) => ({
                  ...page,
                  content: page.content.map((msg) =>
                    msg.id === data.messageId ? { ...msg, pinned: data.pinned } : msg
                  ),
                })),
              };
            }
          );
          return;
        }

        // ── Real chat message payload ──────────────────────────────────────
        const message = parseMessage(frame.body);
        if (!message) return;
        const targetConvId = message.conversationId ?? conversationId;
        addMessageToCacheRef.current(targetConvId, message);

        // Bump this conversation to top for BOTH the sender (who sends via WS
        // and receives the echo here) and any recipient actively viewing the chat.
        // For recipients NOT in this chat, the /queue/notifications handler does it.
        bumpConversationToTopRef.current(targetConvId, {
          lastMessage: message.text || "",
          timestamp: message.timestamp,
        });
        // Keep TQ cache sorted too
        queryClient.setQueryData<import("@/types").Conversation[]>(["conversations"], (old = []) => {
          const updated = old.map((c) =>
            c.id === targetConvId
              ? { ...c, lastMessage: message.text || c.lastMessage, timestamp: message.timestamp ?? c.timestamp }
              : c
          );
          return updated.sort(
            (a, b) => new Date(b.timestamp || 0).getTime() - new Date(a.timestamp || 0).getTime()
          );
        });
      } catch {
        // ignore malformed frames
      }
    });
    messageTopicSubRef.current = () => sub.unsubscribe();
  }, [updateConversation, queryClient]);

  // ─── On connect: set up all static subscriptions + current topic ───────────
  const handleConnect = useCallback(
    (client: Client) => {
      setIsConnected(true);
      clientRef.current = client;
      reconnectAttemptRef.current = 0;
      const unsubs: (() => void)[] = [];

      // 1. Personal message queue (fallback / direct DMs)
      const msgSub = client.subscribe("/user/queue/messages", (frame: IMessage) => {
        const message = parseMessage(frame.body);
        if (!message) return;
        addMessageToCacheRef.current(message.conversationId, message);
      });
      unsubs.push(() => msgSub.unsubscribe());

      // 2. Notifications
      const notifSub = client.subscribe("/user/queue/notifications", (frame: IMessage) => {
        try {
          const notif: WsNotification = JSON.parse(frame.body);
          const isKnown = conversationsRef.current.some((c) => c.id === notif.chatId);

          if (isKnown) {
            // Fast path: update + re-sort in Zustand
            bumpConversationToTop(notif.chatId, {
              lastMessage: notif.latestMessage,
              timestamp: notif.timestamp,
              unreadCount: notif.unreadCount,
            });
            // Keep TQ cache in sync and sorted
            queryClient.setQueryData<import("@/types").Conversation[]>(["conversations"], (old = []) => {
              const updated = old.map((c) =>
                c.id === notif.chatId
                  ? { ...c, lastMessage: notif.latestMessage ?? c.lastMessage, timestamp: notif.timestamp ?? c.timestamp, unreadCount: notif.unreadCount }
                  : c
              );
              return updated.sort(
                (a, b) => new Date(b.timestamp || 0).getTime() - new Date(a.timestamp || 0).getTime()
              );
            });
          } else {
            // Unknown chat — someone started a brand-new conversation with me.
            // Force an immediate network refetch so the full DTO (with name, type, etc.)
            // is fetched from the backend and inserted into the sidebar.
            queryClient.refetchQueries({ queryKey: ["conversations"] });
          }

          setUnreadCount(notif.chatId, notif.unreadCount);
          // Fire sound + haptic only when the message is NOT in the currently open chat
          if (notif.unreadCount > 0 && notif.chatId !== activeConversationIdRef.current) {
            notifyUser();
          }
        } catch (e) {
          console.error("Failed to parse notification:", e);
        }
      });
      unsubs.push(() => notifSub.unsubscribe());

      // 3. Read receipts — handles both bulk markRead (chatId+unreadCount) and per-message SEEN events
      const readSub = client.subscribe("/user/queue/read-receipt", (frame: IMessage) => {
        try {
          const receipt: WsReadReceipt = JSON.parse(frame.body);
          if (receipt.chatId != null && receipt.unreadCount != null) {
            setUnreadCount(receipt.chatId, receipt.unreadCount);
            updateConversation(receipt.chatId, { unreadCount: receipt.unreadCount });
            // Sync TQ cache so sidebar badge clears immediately without a round-trip
            queryClient.setQueryData<import("@/types").Conversation[]>(["conversations"], (old = []) =>
              old.map((c) =>
                c.id === receipt.chatId ? { ...c, unreadCount: receipt.unreadCount } : c
              )
            );
          }
        } catch (e) {
          console.error("Failed to parse read receipt:", e);
        }
      });
      unsubs.push(() => readSub.unsubscribe());

      // 4. Errors → toast notifications
      const errSub = client.subscribe("/user/queue/errors", (frame: IMessage) => {
        try {
          const payload = JSON.parse(frame.body);
          toast.error(payload.error || "A WebSocket error occurred.");
        } catch {
          toast.error(frame.body || "A WebSocket error occurred.");
        }
      });
      unsubs.push(() => errSub.unsubscribe());

      // 5. Refresh events
      const refreshSub = client.subscribe("/user/queue/refresh", () => {
        queryClient.invalidateQueries({ queryKey: ["conversations"] });
      });
      unsubs.push(() => refreshSub.unsubscribe());

      // 6. Global presence / status updates (/topic/status-updates)
      const statusSub = client.subscribe("/topic/status-updates", (frame: IMessage) => {
        try {
          const data = JSON.parse(frame.body) as { userId: string; status: string };
          if (data.userId && data.status) {
            setOnlineStatuses((prev) => ({ ...prev, [data.userId]: data.status }));
          }
        } catch { /* ignore */ }
      });
      unsubs.push(() => statusSub.unsubscribe());

      unsubscribesRef.current = unsubs;

      // 6. Subscribe to the currently active conversation topic immediately on connect.
      // This covers the case where we navigate to a chat before WS connects.
      const currentConvId = activeConversationIdRef.current;
      if (currentConvId) {
        subscribeToMessageTopic(client, currentConvId);
      }
    },
    [updateConversation, setUnreadCount, queryClient, subscribeToMessageTopic]
  );

  // ─── Resubscribe to message topic when active conversation changes ──────────
  useEffect(() => {
    const client = clientRef.current;
    if (!client?.connected || !activeConversationId) {
      messageTopicSubRef.current?.();
      messageTopicSubRef.current = null;
      return;
    }
    subscribeToMessageTopic(client, activeConversationId);

    return () => {
      messageTopicSubRef.current?.();
      messageTopicSubRef.current = null;
    };
  }, [activeConversationId, isConnected, subscribeToMessageTopic]);

  // ─── Dynamic typing subscription ────────────────────────────────────────────
  useEffect(() => {
    const client = clientRef.current;
    if (!client?.connected || !activeConversationId) {
      typingSubRef.current?.();
      typingSubRef.current = null;
      return;
    }

    typingSubRef.current?.();

    const sub = client.subscribe(`/topic/typing-${activeConversationId}`, (frame: IMessage) => {
      try {
        const data = JSON.parse(frame.body);
        const typingMsg = data.typingMessage || null;
        setTypingStates((prev) => ({
          ...prev,
          [activeConversationId]: typingMsg || null,
        }));
        if (typingMsg) {
          setTimeout(() => {
            setTypingStates((prev) => {
              const next = { ...prev };
              if (next[activeConversationId] === typingMsg) {
                delete next[activeConversationId];
              }
              return next;
            });
          }, 3000);
        }
      } catch (e) {
        console.error("Failed to parse typing:", e);
      }
    });
    typingSubRef.current = () => sub.unsubscribe();

    return () => {
      typingSubRef.current?.();
      typingSubRef.current = null;
    };
  }, [activeConversationId, isConnected]);

  // ─── Exponential backoff reconnection ───────────────────────────────────────
  const handleError = useCallback((errorMsg: string) => {
    console.error("WebSocket error:", errorMsg);
    setIsConnected(false);
    clientRef.current = null;

    const attempt = reconnectAttemptRef.current;
    const delay = Math.min(1000 * Math.pow(2, attempt), 30000);
    reconnectAttemptRef.current += 1;

    if (attempt < 10) {
      toast.error(`Connection lost. Reconnecting in ${Math.round(delay / 1000)}s...`);
      reconnectTimerRef.current = setTimeout(() => {
        connectWebSocket(handleConnect, handleError);
      }, delay);
    } else {
      toast.error("Unable to connect. Please refresh the page.");
    }
  }, [handleConnect]);

  // ─── Connect on mount when authenticated ────────────────────────────────────
  useEffect(() => {
    if (!isAuthenticated()) return;

    connectWebSocket(handleConnect, handleError);

    // Heartbeat every 30s to keep presence alive
    heartbeatTimerRef.current = setInterval(() => {
      if (clientRef.current?.connected) {
        publish("/app/ping", {});
      }
    }, 30_000);

    return () => {
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
      if (heartbeatTimerRef.current) clearInterval(heartbeatTimerRef.current);
      typingSubRef.current?.();
      messageTopicSubRef.current?.();
      unsubscribesRef.current.forEach((unsub) => unsub());
      unsubscribesRef.current = [];
      disconnectWebSocket();
      setIsConnected(false);
      clientRef.current = null;
    };
  }, [isAuthenticated, handleConnect, handleError]);

  // ─── Outbound actions ────────────────────────────────────────────────────────

  // Send typing indicator
  const sendTyping = useCallback(
    (conversationId: number) => {
      if (!user) return;
      publish("/app/typing", { groupId: conversationId, userId: user.id, isTyping: true });
    },
    [user]
  );

  // Stop typing indicator
  const sendTypingStop = useCallback(
    (conversationId: number) => {
      if (!user) return;
      publish("/app/typing-stop", { groupId: conversationId, userId: user.id });
    },
    [user]
  );

  // Send message via WebSocket
  const sendMessage = useCallback(
    (conversationId: number, text: string, documentId?: number | null) => {
      publish("/app/sendMessage", {
        conversationId,
        text,
        documentId: documentId || null,
      });
    },
    []
  );

  // Mark messages as read
  const markRead = useCallback((chatId: number) => {
    publish("/app/markRead", { chatId });
  }, []);

  return {
    isConnected,
    typingStates,
    onlineStatuses,
    sendMessage,
    sendTyping,
    sendTypingStop,
    markRead,
  };
}
