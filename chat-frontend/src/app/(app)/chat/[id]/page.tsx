"use client";

// ==============================
// Chat Window — /chat/[id] page
// — optimistic messages, ImageViewer, info panel
// ==============================

import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { ChatHeader } from "@/components/chat/chat-window/chat-header";
import { MessageList } from "@/components/chat/chat-window/message-list";
import { MessageInput } from "@/components/chat/chat-window/message-input";
import { ChatInfoPanel } from "@/components/chat/chat-window/chat-info-panel";
import { ImageViewer } from "@/components/chat/chat-window/image-viewer";
import { useConversations } from "@/hooks/use-conversations";
import { useWebSocket } from "@/hooks/use-websocket";
import { useMessages, useAddMessageToCache } from "@/hooks/use-messages";
import { useChatStore } from "@/store/chat-store";
import { useAuthStore } from "@/store/auth-store";
import { useNotificationStore } from "@/store/notification-store";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import type { Message } from "@/types";

export default function ChatPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const conversationId = Number(params.id);
  const setActiveConversation = useChatStore((s) => s.setActiveConversation);
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const clearUnread = useNotificationStore((s) => s.clearUnread);

  const [infoPanelOpen, setInfoPanelOpen] = useState(false);
  const [viewerImages, setViewerImages] = useState<string[]>([]);
  const [viewerIndex, setViewerIndex] = useState(0);

  const { data: conversations = [] } = useConversations();
  const { isConnected, typingStates, onlineStatuses, sendMessage, sendTyping, sendTypingStop, markRead } = useWebSocket();
  const addMessageToCache = useAddMessageToCache();

  // Fetch messages for collecting image URLs
  const { data: messagesData } = useMessages(conversationId || null);

  const conversation = useMemo(
    () => conversations.find((c) => c.id === conversationId) || null,
    [conversations, conversationId]
  );

  // Collect all image URLs from messages for gallery navigation
  const allImageUrls = useMemo(() => {
    if (!messagesData) return [];
    const urls: string[] = [];
    const pages = messagesData.pages.slice().reverse();
    for (const page of pages) {
      const msgs = page.content.slice().reverse();
      for (const msg of msgs) {
        if (msg.documents) {
          for (const doc of msg.documents) {
            if (doc.fileType?.startsWith("image/")) {
              const url = doc.url;
              urls.push(url);
            }
          }
        }
      }
    }
    return urls;
  }, [messagesData]);

  useEffect(() => {
    if (conversationId) setActiveConversation(conversationId);
    return () => setActiveConversation(null);
  }, [conversationId, setActiveConversation]);

  useEffect(() => {
    if (conversationId && isConnected) {
      markRead(conversationId);
      // Optimistically zero the badge — no need to wait for a WS read-receipt
      clearUnread(conversationId);
      queryClient.setQueryData<import("@/types").Conversation[]>(["conversations"], (old = []) =>
        old.map((c) => c.id === conversationId ? { ...c, unreadCount: 0 } : c)
      );
    }
  }, [conversationId, isConnected, markRead, clearUnread, queryClient]);

  useEffect(() => {
    if (!isAuthenticated()) router.push("/login");
  }, [isAuthenticated, router]);

  // Optimistic message sending
  const handleSend = useCallback((text: string, documentId?: number | null) => {
    if (!conversationId || !user) return;

    const optimisticMsg: Message = {
      id: `pending-${Date.now()}`,
      conversationId,
      senderId: user.id,
      senderName: user.fullName || "You",
      text,
      timestamp: new Date().toISOString(),
      deleted: false,
      edited: false,
      documents: [],
    };

    addMessageToCache(conversationId, optimisticMsg);
    sendMessage(conversationId, text, documentId);
  }, [conversationId, user, addMessageToCache, sendMessage]);

  const handleTypingStart = useCallback(() => {
    if (!conversationId) return;
    sendTyping(conversationId);
  }, [conversationId, sendTyping]);

  const handleTypingStop = useCallback(() => {
    if (!conversationId) return;
    sendTypingStop(conversationId);
  }, [conversationId, sendTypingStop]);

  const handleImageClick = useCallback((url: string) => {
    const index = allImageUrls.indexOf(url);
    setViewerImages(allImageUrls);
    setViewerIndex(index >= 0 ? index : 0);
  }, [allImageUrls]);

  const handleConversationUpdate = () => {
    queryClient.invalidateQueries({ queryKey: ["conversations"] });
  };

  const isBlocked = conversation?.blockedByCurrentUser || conversation?.currentUserBlocked || false;
  const isAiChat = conversation?.chatType === "PRIVATE" && conversation?.name === "Kalori";
  const typingText = typingStates[conversationId] || null;
  const isOtherOnline = conversation?.status === "ONLINE" || conversation?.status === "active";


  if (!conversationId || isNaN(conversationId)) {
    return (
      <div className="flex-1 flex items-center justify-center text-muted-foreground">
        Invalid conversation.
      </div>
    );
  }

  return (
    <div className="flex flex-1 overflow-hidden">
      {/* Main chat area */}
      <div className="flex flex-col flex-1 min-w-0">
        <ChatHeader
          conversation={conversation}
          isConnected={isConnected}
          typingText={typingText}
          onInfoClick={() => setInfoPanelOpen(true)}
        />
        <MessageList
          conversationId={conversationId}
          onImageClick={handleImageClick}
        />
        <MessageInput
          conversationId={conversationId}
          onSend={handleSend}
          onTypingStart={handleTypingStart}
          onTypingStop={handleTypingStop}
          disabled={!isConnected}
          blocked={isBlocked}
          isAiChat={isAiChat}
        />
      </div>

      {/* Info panel */}
      {conversation && (
        <ChatInfoPanel
          conversation={conversation}
          isOpen={infoPanelOpen}
          onClose={() => setInfoPanelOpen(false)}
          onConversationUpdate={handleConversationUpdate}
        />
      )}

      {/* Image viewer */}
      {viewerImages.length > 0 && (
        <ImageViewer
          images={viewerImages}
          initialIndex={viewerIndex}
          onClose={() => setViewerImages([])}
        />
      )}
    </div>
  );
}
