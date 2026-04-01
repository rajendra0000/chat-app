"use client";

import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Menu, MoreVertical, Trash2 } from "lucide-react";
import { useChatStore } from "@/store/chat-store";
import type { Conversation } from "@/types";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/lib/utils";

interface ChatHeaderProps {
  conversation: Conversation | null;
  avatarUrl?: string;
  isConnected: boolean;
  typingText?: string | null;
  onInfoClick?: () => void;
  onDeleteChat?: () => void;
}

export function ChatHeader({ conversation, avatarUrl, isConnected, typingText, onInfoClick, onDeleteChat }: ChatHeaderProps) {
  const router = useRouter();
  const { toggleSidebar, setActiveConversation } = useChatStore();
  const [menuOpen, setMenuOpen] = useState(false);

  if (!conversation) return null;

  const initials = conversation.name.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase();
  const isOnline = conversation.status === "ONLINE" || conversation.status === "online";
  const isGroup = conversation.chatType === "GROUP";

  const statusText = typingText
    ? typingText
    : isGroup
    ? `${conversation.participants?.length ?? 0} members`
    : isOnline
    ? "Online"
    : conversation.status
    ? `Last seen recently`
    : "Offline";

  const handleBack = () => {
    setActiveConversation(null);
    router.push("/chat");
  };

  return (
    <div className="h-16 px-4 flex items-center gap-3 border-b border-border bg-surface/80 backdrop-blur-sm shrink-0 relative">
      <Button variant="ghost" size="icon" className="lg:hidden shrink-0" onClick={handleBack} id="back-btn">
        <ArrowLeft className="w-5 h-5" />
      </Button>
      <Button variant="ghost" size="icon" className="hidden lg:flex shrink-0" onClick={toggleSidebar}>
        <Menu className="w-5 h-5" />
      </Button>

      {/* Avatar with online pulse */}
      <div className="relative shrink-0">
        <Avatar className="h-10 w-10">
          <AvatarImage src={avatarUrl} alt={conversation.name} />
          <AvatarFallback className="bg-accent/20 text-accent text-sm font-semibold">{initials}</AvatarFallback>
        </Avatar>
        {/* Online pulse indicator on avatar */}
        {isOnline && !isGroup && (
          <span className="absolute bottom-0 right-0 w-3 h-3 bg-success border-2 border-background rounded-full" />
        )}
      </div>

      {/* Name + status */}
      <div className="flex-1 min-w-0">
        <h2 className="text-sm font-semibold truncate">{conversation.name}</h2>
        <p className={cn("text-xs truncate", typingText ? "text-accent italic" : isOnline && !isGroup ? "text-success" : "text-muted-foreground")}>
          {statusText}
        </p>
      </div>

      {/* Right side actions */}
      <div className="flex items-center gap-2 shrink-0">
        <span
          className={cn("w-2 h-2 rounded-full", isConnected ? "bg-success" : "bg-destructive animate-pulse")}
          title={isConnected ? "Connected" : "Reconnecting..."}
        />
        <Button variant="ghost" size="icon" id="chat-options-btn" onClick={() => { onInfoClick?.(); setMenuOpen(false); }}>
          <MoreVertical className="w-5 h-5" />
        </Button>
      </div>

      {/* Dropdown for delete-chat etc. */}
      {menuOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
          <div className="absolute top-14 right-4 bg-card border border-border rounded-xl shadow-xl p-1 z-50 min-w-[160px]">
            {!isGroup && onDeleteChat && (
              <button
                onClick={() => { setMenuOpen(false); onDeleteChat(); }}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-destructive hover:bg-destructive/10 rounded-lg transition-colors cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />Delete Chat
              </button>
            )}
          </div>
        </>
      )}
    </div>
  );
}
