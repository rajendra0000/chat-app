"use client";

// ==============================
// Conversation Item Component
// ==============================

import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Trash2, LogOut } from "lucide-react";
import { cn } from "@/lib/utils";
import { useNotificationStore } from "@/store/notification-store";
import type { Conversation } from "@/types";

interface ConversationItemProps {
  conversation: Conversation;
  isActive: boolean;
  avatarUrl?: string;
  onSelect: (id: number) => void;
  onDelete?: (id: number) => void;
  onLeave?: (id: number) => void;
}

function formatTimestamp(isoString: string | null): string {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return "";
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  if (date >= startOfToday) {
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }
  return date.toLocaleDateString([], { day: "2-digit", month: "short" });
}

export function ConversationItem({
  conversation,
  isActive,
  avatarUrl,
  onSelect,
  onDelete,
  onLeave,
}: ConversationItemProps) {
  const initials = conversation.name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  // Use the live notification store count (updated instantly by WS),
  // falling back to the value from the conversations API response.
  const liveCount = useNotificationStore((s) => s.unreadCounts[conversation.id] ?? conversation.unreadCount ?? 0);
  const badgeLabel = liveCount > 9 ? "9+" : liveCount > 0 ? String(liveCount) : null;

  const lastMessage = conversation.deleted
    ? "Message deleted"
    : conversation.lastMessage || "";

  return (
    <div
      className={cn(
        "group flex items-center gap-3 px-3 py-2.5 cursor-pointer rounded-xl transition-all duration-150 hover:bg-muted/50",
        isActive && "bg-accent/10 border-l-3 border-accent"
      )}
      onClick={() => onSelect(conversation.id)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === "Enter" && onSelect(conversation.id)}
      id={`conversation-${conversation.id}`}
    >
      {/* Avatar */}
      <div className="relative shrink-0">
        <Avatar className="h-11 w-11">
          <AvatarImage src={avatarUrl} alt={conversation.name} />
          <AvatarFallback className="bg-accent/20 text-accent text-sm font-semibold">
            {initials}
          </AvatarFallback>
        </Avatar>
        {conversation.chatType === "PRIVATE" && conversation.status === "ONLINE" && (
          <span className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-success border-2 border-sidebar" />
        )}
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 min-w-0">
            <span className="text-sm font-semibold text-foreground truncate">
              {conversation.name}
            </span>
            {conversation.chatType === "GROUP" && conversation.participants?.length > 0 && (
              <span className="text-[10px] text-muted-foreground/70 shrink-0 font-normal">
                ({conversation.participants.length})
              </span>
            )}
          </div>
          <span className="text-[11px] text-muted-foreground shrink-0 ml-2">
            {formatTimestamp(conversation.timestamp)}
          </span>
        </div>
        <div className="flex items-center justify-between mt-0.5">
          <p
            className={cn(
              "text-xs truncate pr-2",
              conversation.deleted
                ? "italic text-muted-foreground"
                : "text-muted-foreground"
            )}
          >
            {lastMessage || (conversation.chatType === "GROUP"
              ? `${conversation.participants?.length ?? 0} members`
              : "No messages yet")}
          </p>
          <div className="flex items-center gap-1.5 shrink-0">
            {conversation.chatType === "GROUP" && conversation.participants?.length > 0 && !lastMessage && (
              <span className="text-[10px] text-muted-foreground/60 shrink-0" />
            )}
            {badgeLabel && (
              <span
                className="inline-flex items-center justify-center h-5 min-w-5 px-1.5 rounded-full bg-destructive text-white text-[10px] font-bold leading-none"
                aria-label={`${liveCount} unread messages`}
              >
                {badgeLabel}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Action button (visible on hover) */}
      <div className="opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
        {conversation.chatType === "PRIVATE" && onDelete && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDelete(conversation.id);
            }}
            className="p-1.5 rounded-lg hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-colors cursor-pointer"
            title="Delete chat"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        )}
        {conversation.chatType === "GROUP" && onLeave && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onLeave(conversation.id);
            }}
            className="p-1.5 rounded-lg hover:bg-orange-500/10 text-muted-foreground hover:text-orange-500 transition-colors cursor-pointer"
            title="Leave group"
          >
            <LogOut className="w-3.5 h-3.5" />
          </button>
        )}
      </div>
    </div>
  );
}
