"use client";

// ==============================
// Create Group Dialog
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
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Loader2, Users } from "lucide-react";
import { useCreateGroup } from "@/hooks/use-conversations";
import { useChatStore } from "@/store/chat-store";
import { toast } from "sonner";

interface CreateGroupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateGroupDialog({ open, onOpenChange }: CreateGroupDialogProps) {
  const [title, setTitle] = useState("");
  const createGroup = useCreateGroup();
  const setActiveConversation = useChatStore((s) => s.setActiveConversation);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    try {
      const newGroup = await createGroup.mutateAsync({ title: title.trim() });
      setActiveConversation(newGroup.id);
      onOpenChange(false);
      setTitle("");
      toast.success(`Group "${title.trim()}" created!`);
    } catch {
      toast.error("Failed to create group.");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { onOpenChange(v); if (!v) setTitle(""); }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Users className="w-5 h-5 text-accent" />
            Create Group
          </DialogTitle>
          <DialogDescription>
            Create a new group chat. You can add members later.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="group-name">Group Name</Label>
            <Input
              id="group-name"
              placeholder="Enter group name..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="h-10 rounded-xl"
              maxLength={100}
              autoFocus
            />
          </div>

          <Button
            type="submit"
            className="w-full rounded-xl"
            disabled={!title.trim() || createGroup.isPending}
          >
            {createGroup.isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Creating...
              </>
            ) : (
              "Create Group"
            )}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
