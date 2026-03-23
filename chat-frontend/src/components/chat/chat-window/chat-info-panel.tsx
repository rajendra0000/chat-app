"use client";

// ==============================
// Chat Info Panel (Slide-out)
// — Added: #4 group rename, #5 delete group, #7 delete private chat
// ==============================

import { useEffect, useState, useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { MemberList } from "./member-list";
import { AddMemberDialog } from "@/components/chat/modals/add-member-dialog";
import { useAuthStore } from "@/store/auth-store";
import * as chatService from "@/services/chat-service";
import { blockUser, unblockUser } from "@/services/chat-service";
import {
  X, Camera, UserPlus, Ban, Unlock, Loader2, Pencil, Check, Trash2,
} from "lucide-react";
import type { Conversation, ChatMember } from "@/types";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { useChatStore } from "@/store/chat-store";
import {
  useRenameGroup, useDeleteGroup, useHideConversation,
} from "@/hooks/use-conversations";
import { useCreateConversation } from "@/hooks/use-conversations";

interface ChatInfoPanelProps {
  conversation: Conversation;
  isOpen: boolean;
  onClose: () => void;
  onConversationUpdate?: () => void;
}

export function ChatInfoPanel({
  conversation, isOpen, onClose, onConversationUpdate,
}: ChatInfoPanelProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const queryClient = useQueryClient();
  const router = useRouter();
  const { setActiveConversation } = useChatStore();
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [isUploadingPic, setIsUploadingPic] = useState(false);
  const [isBlocking, setIsBlocking] = useState(false);

  // #4 — Group rename state
  const [isRenaming, setIsRenaming] = useState(false);
  const [draftName, setDraftName] = useState(conversation.name);
  const renameGroup = useRenameGroup();

  // #5 — Delete group
  const deleteGroup = useDeleteGroup();

  // #7 — Delete private chat
  const hideConversation = useHideConversation();

  // #3 — Start private chat with a group member
  const createConversation = useCreateConversation();

  const isGroup = conversation.chatType === "GROUP";

  const { data: members = [], refetch: refetchMembers } = useQuery<ChatMember[]>({
    queryKey: ["chatMembers", conversation.id],
    queryFn: () => chatService.fetchMembers(conversation.id),
    enabled: isGroup && isOpen,
    staleTime: 60_000,
  });

  const { data: chatPicUrl } = useQuery({
    queryKey: ["chatPic", conversation.id],
    queryFn: () => chatService.getChatProfilePicUrl(conversation.id),
    enabled: isOpen,
    staleTime: 60_000,
  });

  const myMember = members.find((m) => m.userId === currentUserId);
  const isAdmin = myMember?.role === "ADMIN";
  const isSoleMember = isGroup && members.length === 1 && members[0]?.userId === currentUserId;

  const handleGroupPicUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploadingPic(true);
    try {
      await chatService.uploadChatProfilePic(conversation.id, file);
      queryClient.invalidateQueries({ queryKey: ["chatPic", conversation.id] });
      toast.success("Group picture updated!");
    } catch { toast.error("Failed to upload group picture."); }
    finally { setIsUploadingPic(false); }
  };

  // #4 — Commit rename
  const handleRename = async () => {
    const name = draftName.trim();
    if (!name || name === conversation.name) { setIsRenaming(false); return; }
    try {
      await renameGroup.mutateAsync({ chatId: conversation.id, name });
      toast.success("Group renamed!");
    } catch { toast.error("Failed to rename group."); }
    finally { setIsRenaming(false); }
  };

  // #5 — Delete group
  const handleDeleteGroup = async () => {
    if (!window.confirm("Delete this group permanently? This cannot be undone.")) return;
    try {
      await deleteGroup.mutateAsync(conversation.id);
      setActiveConversation(null);
      router.push("/chat");
      onClose();
      toast.success("Group deleted.");
    } catch { toast.error("Failed to delete group."); }
  };

  // #7 — Delete private chat for me
  const handleDeletePrivateChat = async () => {
    if (!window.confirm("Delete this conversation for you? The other person will still see it.")) return;
    try {
      await hideConversation.mutateAsync(conversation.id);
      setActiveConversation(null);
      router.push("/chat");
      onClose();
      toast.success("Conversation deleted.");
    } catch { toast.error("Failed to delete conversation."); }
  };

  // #3 — Open/create private chat with a group member
  const handleChatWithMember = async (userId: number) => {
    if (userId === currentUserId) return;
    try {
      const convo = await createConversation.mutateAsync(userId);
      setActiveConversation(convo.id);
      router.push(`/chat/${convo.id}`);
      onClose();
    } catch { toast.error("Failed to open chat."); }
  };

  // Block/Unblock for private chats
  const handleBlockToggle = async () => {
    if (!currentUserId) return;
    setIsBlocking(true);
    try {
      const targetUserId = conversation.participants.find((id) => id !== currentUserId);
      if (!targetUserId) return;
      if (conversation.blockedByCurrentUser) {
        await unblockUser(conversation.id, targetUserId);
        toast.success("User unblocked.");
      } else {
        await blockUser(conversation.id, targetUserId);
        toast.success("User blocked.");
      }
      onConversationUpdate?.();
      queryClient.invalidateQueries({ queryKey: ["conversations"] });
    } catch { toast.error("Failed to update block status."); }
    finally { setIsBlocking(false); }
  };

  const handleMemberRefresh = useCallback(() => refetchMembers(), [refetchMembers]);

  const initials = conversation.name.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase();

  return (
    <>
      {isOpen && (
        <div className="fixed inset-0 bg-black/30 z-40 lg:hidden" onClick={onClose} />
      )}

      <aside className={cn(
        "flex flex-col w-80 border-l border-border bg-sidebar shrink-0 transition-all duration-300 z-50 overflow-hidden",
        "max-lg:fixed max-lg:inset-y-0 max-lg:right-0 max-lg:shadow-2xl",
        isOpen ? "max-lg:translate-x-0 lg:w-80" : "max-lg:translate-x-full lg:w-0"
      )}>
        {/* Header */}
        <div className="h-16 px-4 flex items-center justify-between border-b border-border shrink-0">
          <h3 className="text-sm font-semibold">{isGroup ? "Group Info" : "Chat Info"}</h3>
          <Button variant="ghost" size="icon" onClick={onClose}><X className="w-4 h-4" /></Button>
        </div>

        <ScrollArea className="flex-1">
          <div className="p-5 space-y-6">
            {/* Avatar */}
            <div className="flex flex-col items-center">
              <div className="relative group">
                <Avatar className="h-20 w-20">
                  <AvatarImage src={chatPicUrl || undefined} alt={conversation.name} />
                  <AvatarFallback className="bg-accent/20 text-accent text-xl font-bold">{initials}</AvatarFallback>
                </Avatar>
                {isGroup && isAdmin && (
                  <label className="absolute inset-0 flex items-center justify-center bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
                    {isUploadingPic ? <Loader2 className="w-5 h-5 text-white animate-spin" /> : <Camera className="w-5 h-5 text-white" />}
                    <input type="file" className="hidden" accept="image/*" onChange={handleGroupPicUpload} disabled={isUploadingPic} />
                  </label>
                )}
              </div>

              {/* #4 — Editable group name for admins */}
              {isGroup && isAdmin && isRenaming ? (
                <div className="flex items-center gap-2 mt-3 w-full px-4">
                  <Input
                    value={draftName}
                    onChange={(e) => setDraftName(e.target.value)}
                    onKeyDown={(e) => { if (e.key === "Enter") handleRename(); if (e.key === "Escape") setIsRenaming(false); }}
                    className="h-8 text-sm text-center"
                    autoFocus
                  />
                  <button onClick={handleRename} disabled={renameGroup.isPending} className="p-1 rounded-full hover:bg-muted transition-colors cursor-pointer">
                    {renameGroup.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4 text-success" />}
                  </button>
                  <button onClick={() => setIsRenaming(false)} className="p-1 rounded-full hover:bg-muted transition-colors cursor-pointer">
                    <X className="w-4 h-4 text-muted-foreground" />
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-1.5 mt-3">
                  <h3 className="text-base font-semibold">{conversation.name}</h3>
                  {isGroup && isAdmin && (
                    <button onClick={() => { setDraftName(conversation.name); setIsRenaming(true); }} className="p-1 rounded-full hover:bg-muted transition-colors cursor-pointer opacity-60 hover:opacity-100">
                      <Pencil className="w-3.5 h-3.5 text-muted-foreground" />
                    </button>
                  )}
                </div>
              )}
              <p className="text-xs text-muted-foreground mt-0.5">
                {isGroup ? `${conversation.participants.length} members` : conversation.status || ""}
              </p>
            </div>

            {/* Private chat actions */}
            {!isGroup && (
              <div className="space-y-2">
                <Button
                  variant="outline" size="sm"
                  className={cn("w-full rounded-xl",
                    conversation.blockedByCurrentUser ? "text-success hover:text-success" : "text-destructive hover:text-destructive"
                  )}
                  onClick={handleBlockToggle}
                  disabled={isBlocking}
                >
                  {isBlocking ? <Loader2 className="w-3.5 h-3.5 animate-spin mr-2" /> :
                    conversation.blockedByCurrentUser ? <Unlock className="w-3.5 h-3.5 mr-2" /> : <Ban className="w-3.5 h-3.5 mr-2" />}
                  {conversation.blockedByCurrentUser ? "Unblock User" : "Block User"}
                </Button>
                {/* #7 — Delete private chat for me */}
                <Button
                  variant="outline" size="sm"
                  className="w-full rounded-xl text-destructive hover:text-destructive"
                  onClick={handleDeletePrivateChat}
                  disabled={hideConversation.isPending}
                >
                  {hideConversation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin mr-2" /> : <Trash2 className="w-3.5 h-3.5 mr-2" />}
                  Delete Chat
                </Button>
                {conversation.currentUserBlocked && (
                  <p className="text-xs text-destructive text-center">You have been blocked by this user.</p>
                )}
              </div>
            )}

            {/* Group members */}
            {isGroup && (
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-sm font-semibold">Members</h4>
                  {isAdmin && (
                    <Button variant="ghost" size="sm" className="h-7 text-xs text-accent" onClick={() => setAddMemberOpen(true)}>
                      <UserPlus className="w-3 h-3 mr-1" />Add
                    </Button>
                  )}
                </div>
                <MemberList
                  chatId={conversation.id}
                  members={members}
                  onRefresh={handleMemberRefresh}
                  isAdmin={isAdmin}
                  onChatWithMember={handleChatWithMember} // #3
                />
              </div>
            )}

            {/* #5 — Delete group if sole member */}
            {isSoleMember && (
              <Button
                variant="outline" size="sm"
                className="w-full rounded-xl text-destructive hover:text-destructive border-destructive/30"
                onClick={handleDeleteGroup}
                disabled={deleteGroup.isPending}
              >
                {deleteGroup.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin mr-2" /> : <Trash2 className="w-3.5 h-3.5 mr-2" />}
                Delete Group
              </Button>
            )}
          </div>
        </ScrollArea>
      </aside>

      {isGroup && (
        <AddMemberDialog
          open={addMemberOpen}
          onOpenChange={setAddMemberOpen}
          chatId={conversation.id}
          onMemberAdded={handleMemberRefresh}
        />
      )}
    </>
  );
}
