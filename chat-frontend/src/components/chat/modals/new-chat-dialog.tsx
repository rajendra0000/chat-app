"use client";

// ==============================
// New Chat Dialog — with Kalori AI pinned entry
// ==============================

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Loader2, Search, MessageSquarePlus, Sparkles } from "lucide-react";
import { useUserSearch } from "@/hooks/use-user-search";
import { useCreateConversation } from "@/hooks/use-conversations";
import { useChatStore } from "@/store/chat-store";
import { toast } from "sonner";
import type { Conversation } from "@/types";
import { useAuthStore } from "@/store/auth-store";
import { apiGet } from "@/services/api";

interface NewChatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  conversations: Conversation[];
}

interface KaloriUser {
  id: number;
  name: string;
  phone: string;
}

export function NewChatDialog({ open, onOpenChange, conversations }: NewChatDialogProps) {
  const { query, setQuery, results, isSearching, error, reset } = useUserSearch();
  const createConversation = useCreateConversation();
  const setActiveConversation = useChatStore((s) => s.setActiveConversation);
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [isCreating, setIsCreating] = useState(false);
  const [isStartingAiChat, setIsStartingAiChat] = useState(false);

  const handleSelectUser = async (userId: number) => {
    // Check if conversation already exists
    const existing = conversations.find(
      (c) => c.chatType === "PRIVATE" && c.participants.includes(userId)
    );

    if (existing) {
      setActiveConversation(existing.id);
      onOpenChange(false);
      reset();
      return;
    }

    // Create new conversation
    setIsCreating(true);
    try {
      const newConvo = await createConversation.mutateAsync(userId);
      setActiveConversation(newConvo.id);
      onOpenChange(false);
      reset();
      toast.success("Conversation started!");
    } catch {
      toast.error("Failed to start conversation.");
    } finally {
      setIsCreating(false);
    }
  };

  const handleStartKaloriChat = async () => {
    setIsStartingAiChat(true);
    try {
      const kalori = await apiGet<KaloriUser>("/api/users/kalori");
      await handleSelectUser(kalori.id);
    } catch {
      toast.error("Failed to start Kalori AI chat.");
    } finally {
      setIsStartingAiChat(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <MessageSquarePlus className="w-5 h-5 text-accent" />
            New Chat
          </DialogTitle>
          <DialogDescription>
            Search by phone number to start a conversation.
          </DialogDescription>
        </DialogHeader>

        {/* ─── Kalori AI pinned entry ─────────────────────── */}
        <button
          onClick={handleStartKaloriChat}
          disabled={isStartingAiChat || isCreating}
          id="chat-with-kalori-btn"
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl border border-accent/30 bg-accent/5 hover:bg-accent/10 transition-colors text-left cursor-pointer disabled:opacity-50 group"
        >
          <div className="h-9 w-9 rounded-full bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center flex-shrink-0 shadow-sm">
            {isStartingAiChat ? (
              <Loader2 className="w-4 h-4 animate-spin text-white" />
            ) : (
              <Sparkles className="w-4 h-4 text-white" />
            )}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-foreground">Kalori AI</p>
            <p className="text-xs text-muted-foreground">Your personal AI assistant</p>
          </div>
          <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full bg-accent/20 text-accent">AI</span>
        </button>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Enter phone number..."
            value={query}
            onChange={(e) => setQuery(e.target.value.replace(/[^\d+]/g, ""))}
            className="pl-9 h-10 rounded-xl"
            inputMode="numeric"
            autoFocus
            id="search-users-input"
          />
        </div>

        {/* Results */}
        <div className="max-h-64 overflow-y-auto space-y-1">
          {isSearching && (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {error && (
            <p className="text-destructive text-sm text-center py-4">{error}</p>
          )}

          {!isSearching && query.length >= 3 && results.length === 0 && !error && (
            <p className="text-muted-foreground text-sm text-center py-4">
              No users found.
            </p>
          )}

          {query.length > 0 && query.length < 3 && (
            <p className="text-muted-foreground text-xs text-center py-4">
              Enter at least 3 digits to search.
            </p>
          )}

          {results
            .filter((u) => u.id !== currentUserId)
            .map((user) => (
            <button
              key={user.id}
              onClick={() => handleSelectUser(user.id)}
              disabled={isCreating}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-muted/50 transition-colors text-left cursor-pointer disabled:opacity-50"
            >
              <Avatar className="h-9 w-9">
                <AvatarFallback className="bg-accent/20 text-accent text-xs font-semibold">
                  {user.fullName.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium truncate">{user.fullName}</p>
                <p className="text-xs text-muted-foreground">{user.phone}</p>
              </div>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}
