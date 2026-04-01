"use client";

// ==============================
// Sidebar Component
// ==============================

import { useState, useMemo, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { ThemeToggle } from "@/components/ui/theme-toggle";
import { ConversationItem } from "./conversation-item";
import { NewChatDialog } from "@/components/chat/modals/new-chat-dialog";
import { CreateGroupDialog } from "@/components/chat/modals/create-group-dialog";
import {
  useConversations,
  useDeleteConversation,
  useLeaveGroup,
} from "@/hooks/use-conversations";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";
import { useKeyboardShortcuts } from "@/hooks/use-keyboard-shortcuts";
import {
  MessageSquarePlus,
  Users,
  UserPen,
  Search,
  LogOut,
} from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { fetchCurrentUser, getMyProfilePicUrl } from "@/services/user-service";
import { useQuery } from "@tanstack/react-query";
import { USER_PROFILE_STALE_TIME } from "@/lib/constants";

export function Sidebar() {
  const router = useRouter();
  const { user, setUser, logout, isAuthenticated } = useAuthStore();
  const { activeConversationId, setActiveConversation, isSidebarOpen, setSidebarOpen } = useChatStore();

  const [searchFilter, setSearchFilter] = useState("");
  const [newChatOpen, setNewChatOpen] = useState(false);
  const [createGroupOpen, setCreateGroupOpen] = useState(false);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Ctrl+K → focus search
  useKeyboardShortcuts({
    onSearch: useCallback(() => {
      setSidebarOpen(true);
      setTimeout(() => searchInputRef.current?.focus(), 100);
    }, [setSidebarOpen]),
  });

  const deleteConversation = useDeleteConversation();
  const leaveGroup = useLeaveGroup();

  // Fetch current user
  const { data: currentUser } = useQuery({
    queryKey: ["currentUser"],
    queryFn: fetchCurrentUser,
    staleTime: USER_PROFILE_STALE_TIME,
    enabled: isAuthenticated(),
  });

  // Sync user to store
  useEffect(() => {
    if (currentUser && !user) {
      setUser(currentUser);
    }
  }, [currentUser, user, setUser]);

  // Fetch profile pic
  const { data: profilePicUrl } = useQuery({
    queryKey: ["myProfilePic"],
    queryFn: getMyProfilePicUrl,
    staleTime: USER_PROFILE_STALE_TIME,
    enabled: isAuthenticated(),
  });

  // Fetch conversations
  const { data: conversations = [], isLoading: isLoadingConvos } = useConversations();

  // Filter conversations by search query
  const filteredConversations = useMemo(() => {
    if (!searchFilter.trim()) return conversations;
    const query = searchFilter.toLowerCase();
    return conversations.filter((c) =>
      c.name.toLowerCase().includes(query)
    );
  }, [conversations, searchFilter]);

  const handleSelectConversation = (id: number) => {
    setActiveConversation(id);
    setSidebarOpen(false); // close on mobile
    router.push(`/chat/${id}`);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this conversation?")) return;
    try {
      await deleteConversation.mutateAsync(id);
      if (activeConversationId === id) {
        setActiveConversation(null);
        router.push("/chat");
      }
      toast.success("Conversation deleted.");
    } catch {
      toast.error("Failed to delete conversation.");
    }
  };

  const handleLeave = async (id: number) => {
    if (!window.confirm("Are you sure you want to leave this group?")) return;
    try {
      await leaveGroup.mutateAsync(id);
      if (activeConversationId === id) {
        setActiveConversation(null);
        router.push("/chat");
      }
      toast.success("Left group successfully.");
    } catch {
      toast.error("Failed to leave group.");
    }
  };

  const userName = currentUser?.fullName || user?.fullName || "User";
  const userInitials = userName
    .split(" ")
    .map((n) => n[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <>
      <aside className="flex flex-col h-full w-full border-r border-border bg-sidebar">
        {/* ============ Header ============ */}
        <div className="p-4 border-b border-border shrink-0">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-3">
              <Avatar className="h-10 w-10">
                <AvatarImage src={profilePicUrl || undefined} alt={userName} />
                <AvatarFallback className="bg-accent/20 text-accent font-semibold text-sm">{userInitials}</AvatarFallback>
              </Avatar>
              <div className="min-w-0">
                <p className="text-sm font-semibold truncate">{userName}</p>
                <p className="text-xs text-success">Online</p>
              </div>
            </div>

          {/* Close button hidden — navigation handled by back arrow in chat header */}
          </div>

          {/* Action buttons */}
          <div className="flex gap-1.5">
            <Button
              variant="ghost"
              size="sm"
              className="flex-1 justify-start gap-2 text-xs h-8"
              onClick={() => setNewChatOpen(true)}
              id="new-chat-btn"
            >
              <MessageSquarePlus className="w-3.5 h-3.5" />
              New Chat
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="flex-1 justify-start gap-2 text-xs h-8"
              onClick={() => setCreateGroupOpen(true)}
              id="create-group-btn"
            >
              <Users className="w-3.5 h-3.5" />
              New Group
            </Button>
          </div>

          {/* Utility row */}
          <div className="flex gap-1.5 mt-1.5">
            <Button
              variant="ghost"
              size="sm"
              className="flex-1 justify-start gap-2 text-xs h-8"
              onClick={() => router.push("/settings")}
              id="edit-profile-btn"
            >
              <UserPen className="w-3.5 h-3.5" />
              Profile
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="flex-1 justify-start gap-2 text-xs h-8 text-destructive hover:text-destructive"
              onClick={logout}
              id="logout-btn"
            >
              <LogOut className="w-3.5 h-3.5" />
              Logout
            </Button>
          </div>
        </div>

        {/* ============ Search ============ */}
        <div className="px-3 py-2 shrink-0">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground" />
            <Input
              ref={searchInputRef}
              placeholder="Search conversations... (Ctrl+K)"
              value={searchFilter}
              onChange={(e) => setSearchFilter(e.target.value)}
              className="pl-9 h-8 text-xs rounded-lg bg-muted/50 border-0 focus-visible:ring-1"
              id="search-conversations"
              aria-label="Search conversations"
            />
          </div>
        </div>

        {/* ============ Conversation List ============ */}
        <ScrollArea className="flex-1">
          <div className="px-2 py-1 space-y-0.5">
            {isLoadingConvos ? (
              // Loading skeletons
              Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-3 py-2.5">
                  <Skeleton className="h-11 w-11 rounded-full shrink-0" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-3.5 w-24" />
                    <Skeleton className="h-3 w-36" />
                  </div>
                </div>
              ))
            ) : filteredConversations.length === 0 ? (
              <p className="text-center text-muted-foreground text-sm py-8">
                {searchFilter ? "No matching conversations." : "No conversations yet."}
              </p>
            ) : (
              filteredConversations.map((conversation) => (
                <ConversationItem
                  key={conversation.id}
                  conversation={conversation}
                  isActive={activeConversationId === conversation.id}
                  avatarUrl={conversation.avatarUrl || undefined}
                  onSelect={handleSelectConversation}
                  onDelete={conversation.chatType === "PRIVATE" ? handleDelete : undefined}
                  onLeave={conversation.chatType === "GROUP" ? handleLeave : undefined}
                />
              ))
            )}
          </div>
        </ScrollArea>

        {/* ============ Footer: Theme Toggle ============ */}
        <div className="px-3 py-2.5 border-t border-border shrink-0">
          <ThemeToggle />
        </div>
      </aside>

      {/* ============ Dialogs ============ */}
      <NewChatDialog
        open={newChatOpen}
        onOpenChange={setNewChatOpen}
        conversations={conversations}
      />
      <CreateGroupDialog
        open={createGroupOpen}
        onOpenChange={setCreateGroupOpen}
      />
    </>
  );
}
