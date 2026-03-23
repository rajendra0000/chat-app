"use client";

// ==============================
// Add Member Dialog
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
import { Loader2, UserPlus } from "lucide-react";
import * as chatService from "@/services/chat-service";
import { toast } from "sonner";

interface AddMemberDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  chatId: number;
  onMemberAdded: () => void;
}

export function AddMemberDialog({ open, onOpenChange, chatId, onMemberAdded }: AddMemberDialogProps) {
  const [phone, setPhone] = useState("");
  const [isAdding, setIsAdding] = useState(false);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phone.trim()) return;

    setIsAdding(true);
    try {
      await chatService.addMember(chatId, phone.trim());
      onMemberAdded();
      onOpenChange(false);
      setPhone("");
      toast.success("Member added!");
    } catch {
      toast.error("Failed to add member. Check the phone number.");
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { onOpenChange(v); if (!v) setPhone(""); }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UserPlus className="w-5 h-5 text-accent" />
            Add Member
          </DialogTitle>
          <DialogDescription>
            Enter the phone number of the user to add.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleAdd} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="add-member-phone">Phone Number</Label>
            <Input
              id="add-member-phone"
              placeholder="Enter 10-digit phone number"
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
              inputMode="numeric"
              className="h-10 rounded-xl"
              autoFocus
              maxLength={10}
            />
          </div>
          <Button
            type="submit"
            className="w-full rounded-xl"
            disabled={phone.length !== 10 || isAdding}
          >
            {isAdding ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Adding...
              </>
            ) : (
              "Add Member"
            )}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
