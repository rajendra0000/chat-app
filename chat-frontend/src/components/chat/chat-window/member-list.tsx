"use client";

// ==============================
// Member List — #3: click member to open private chat
// ==============================

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ShieldCheck, Ban, UserX, Unlock, MessageSquare } from "lucide-react";
import { useAuthStore } from "@/store/auth-store";
import * as chatService from "@/services/chat-service";
import { toast } from "sonner";
import type { ChatMember } from "@/types";

interface MemberListProps {
  chatId: number;
  members: ChatMember[];
  onRefresh: () => void;
  isAdmin: boolean;
  onChatWithMember?: (userId: number) => void; // #3
}

export function MemberList({ chatId, members, onRefresh, isAdmin, onChatWithMember }: MemberListProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);

  const handleBlock = async (userId: number) => {
    try { await chatService.blockMember(chatId, userId); onRefresh(); toast.success("Member blocked."); }
    catch { toast.error("Failed to block member."); }
  };

  const handleUnblock = async (userId: number) => {
    try { await chatService.unblockMember(chatId, userId); onRefresh(); toast.success("Member unblocked."); }
    catch { toast.error("Failed to unblock member."); }
  };

  const handleRemove = async (userId: number) => {
    if (!window.confirm("Remove this member from the group?")) return;
    try { await chatService.removeMember(chatId, userId); onRefresh(); toast.success("Member removed."); }
    catch { toast.error("Failed to remove member."); }
  };

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium text-muted-foreground px-1 mb-2">
        {members.length} member{members.length !== 1 ? "s" : ""}
      </p>
      {members.map((member) => {
        const isSelf = member.userId === currentUserId;
        const initials = member.name.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase();

        return (
          <div
            key={member.userId}
            className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-muted/50 transition-colors group"
          >
            <Avatar className="h-9 w-9 shrink-0">
              <AvatarFallback className="bg-accent/20 text-accent text-xs font-semibold">{initials}</AvatarFallback>
            </Avatar>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium truncate">{member.name}{isSelf ? " (You)" : ""}</span>
                {member.role === "ADMIN" && (
                  <Badge variant="secondary" className="text-[10px] h-4 px-1.5 gap-0.5">
                    <ShieldCheck className="w-2.5 h-2.5" />Admin
                  </Badge>
                )}
              </div>
              {member.blocked && <p className="text-xs text-destructive">Blocked</p>}
            </div>

            {/* #3 — Message button for non-self members */}
            {!isSelf && onChatWithMember && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs text-accent opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={() => onChatWithMember(member.userId)}
                title="Start private chat"
              >
                <MessageSquare className="w-3 h-3" />
              </Button>
            )}

            {/* Admin actions (not on self) */}
            {isAdmin && !isSelf && (
              <div className="flex gap-1 shrink-0">
                {member.blocked ? (
                  <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-success hover:text-success" onClick={() => handleUnblock(member.userId)}>
                    <Unlock className="w-3 h-3 mr-1" />Unblock
                  </Button>
                ) : (
                  <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-orange-500 hover:text-orange-500" onClick={() => handleBlock(member.userId)}>
                    <Ban className="w-3 h-3 mr-1" />Block
                  </Button>
                )}
                <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-destructive hover:text-destructive" onClick={() => handleRemove(member.userId)}>
                  <UserX className="w-3 h-3 mr-1" />Remove
                </Button>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
