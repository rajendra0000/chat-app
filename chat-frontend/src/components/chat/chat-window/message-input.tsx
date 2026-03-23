"use client";

// ==============================
// Message Input — emoji picker + drag-and-drop + upload + voice recorder
// ==============================

import { useState, useRef, useCallback, useEffect, type KeyboardEvent, type DragEvent } from "react";
import { Button } from "@/components/ui/button";
import { Send, Paperclip, X, FileText, Loader2, Smile, Mic } from "lucide-react";
import EmojiPicker, { Theme } from "emoji-picker-react";
import { uploadFileWithProgress, validateFile, FileValidationError } from "@/services/file-service";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { VoiceRecorder } from "./voice-recorder";

interface MessageInputProps {
  conversationId: number;
  onSend: (text: string, documentId?: number | null) => void;
  onTypingStart: () => void;
  onTypingStop: () => void;
  disabled?: boolean;
  blocked?: boolean;
  /** When true (chatting with Kalori AI), hides file and voice buttons */
  isAiChat?: boolean;
}

export function MessageInput({
  conversationId,
  onSend,
  onTypingStart,
  onTypingStop,
  disabled,
  blocked,
  isAiChat = false,
}: MessageInputProps) {
  const [text, setText] = useState("");
  const [attachment, setAttachment] = useState<{
    file: File;
    documentId: number;
    isImage: boolean;
    previewUrl?: string;
  } | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [showVoice, setShowVoice] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const abortUploadRef = useRef<(() => void) | null>(null);

  // Typing throttle refs
  const isTypingRef = useRef(false);
  const stopTypingTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  // Reset conversation-level typing state when conversation changes
  useEffect(() => {
    isTypingRef.current = false;
    if (stopTypingTimerRef.current) clearTimeout(stopTypingTimerRef.current);
  }, [conversationId]);

  const handleSend = useCallback(() => {
    const trimmed = text.trim();
    if (!trimmed && !attachment) return;

    // Clear typing state on send
    if (isTypingRef.current) {
      isTypingRef.current = false;
      if (stopTypingTimerRef.current) clearTimeout(stopTypingTimerRef.current);
      onTypingStop();
    }

    onSend(trimmed, attachment?.documentId || null);
    setText("");
    setAttachment(null);
    setShowEmojiPicker(false);

    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.focus();
    }
  }, [text, attachment, onSend, onTypingStop]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (value: string) => {
    setText(value);

    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }

    // Typing indicator: START on first keystroke, STOP after 3s idle
    if (!isTypingRef.current) {
      isTypingRef.current = true;
      onTypingStart();
    }

    // Reset the 3s stop timer on every keystroke
    if (stopTypingTimerRef.current) clearTimeout(stopTypingTimerRef.current);
    stopTypingTimerRef.current = setTimeout(() => {
      isTypingRef.current = false;
      onTypingStop();
    }, 3000);
  };

  const processFile = async (file: File) => {
    try {
      validateFile(file);
    } catch (err) {
      if (err instanceof FileValidationError) toast.error(err.message);
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);

    const { promise, abort } = uploadFileWithProgress(file, (percent) => {
      setUploadProgress(percent);
    });
    abortUploadRef.current = abort;

    try {
      const { documentId } = await promise;
      const isImage = file.type.startsWith("image/");
      const previewUrl = isImage ? URL.createObjectURL(file) : undefined;
      setAttachment({ file, documentId, isImage, previewUrl });
      toast.success("File attached!");
    } catch (err) {
      if ((err as Error).message !== "Upload cancelled") {
        toast.error("Failed to upload file.");
      }
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      abortUploadRef.current = null;
    }
  };

  const handleCancelUpload = () => {
    abortUploadRef.current?.();
    toast.info("Upload cancelled.");
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (fileInputRef.current) fileInputRef.current.value = "";
    await processFile(file);
  };

  const handleDragOver = (e: DragEvent) => { e.preventDefault(); if (!isAiChat) setIsDragging(true); };
  const handleDragLeave = (e: DragEvent) => { e.preventDefault(); setIsDragging(false); };
  const handleDrop = async (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (isAiChat) return; // AI chats don't accept file drops
    const file = e.dataTransfer.files?.[0];
    if (file) await processFile(file);
  };

  const handleEmojiClick = (emojiData: { emoji: string }) => {
    setText((prev) => prev + emojiData.emoji);
    textareaRef.current?.focus();
  };

  const removeAttachment = () => {
    if (attachment?.previewUrl) URL.revokeObjectURL(attachment.previewUrl);
    setAttachment(null);
  };

  // Voice message sent (documentId from Cloudinary upload)
  const handleVoiceSend = useCallback((documentId: number) => {
    setShowVoice(false);
    onSend("🎤 Voice message", documentId);
  }, [onSend]);

  if (blocked) {
    return (
      <div className="px-4 py-3 border-t border-border bg-surface/80 text-center">
        <p className="text-muted-foreground text-sm">🚫 You cannot send messages in this conversation.</p>
      </div>
    );
  }

  if (showVoice) {
    return (
      <div className="border-t border-border bg-surface/80 backdrop-blur-sm shrink-0">
        <VoiceRecorder
          onSend={handleVoiceSend}
          onCancel={() => setShowVoice(false)}
        />
      </div>
    );
  }

  return (
    <div
      className={cn(
        "border-t border-border bg-surface/80 backdrop-blur-sm shrink-0 relative safe-bottom",
        isDragging && "ring-2 ring-accent ring-inset"
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Drag overlay */}
      {isDragging && (
        <div className="absolute inset-0 bg-accent/10 z-10 flex items-center justify-center pointer-events-none">
          <p className="text-accent text-sm font-medium">Drop file to attach</p>
        </div>
      )}

      {/* Emoji picker */}
      {showEmojiPicker && (
        <div className="absolute bottom-full left-4 mb-2 z-20">
          <EmojiPicker
            onEmojiClick={handleEmojiClick}
            theme={Theme.DARK}
            width={320}
            height={380}
            searchPlaceholder="Search emoji..."
            lazyLoadEmojis
          />
        </div>
      )}

      {/* Upload progress bar */}
      {isUploading && (
        <div className="px-4 pt-3">
          <div className="flex items-center gap-3 p-2.5 bg-muted rounded-xl">
            <div className="flex-1 min-w-0">
              <p className="text-xs text-muted-foreground mb-1.5">Uploading... {uploadProgress}%</p>
              <div className="h-1.5 bg-muted-foreground/20 rounded-full overflow-hidden">
                <div
                  className="h-full bg-accent rounded-full transition-all duration-200 ease-out"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
            </div>
            <button onClick={handleCancelUpload} className="p-1.5 rounded-lg hover:bg-muted-foreground/10 transition-colors cursor-pointer shrink-0" title="Cancel upload">
              <X className="w-4 h-4 text-destructive" />
            </button>
          </div>
        </div>
      )}

      {/* Attachment preview */}
      {attachment && !isUploading && (
        <div className="px-4 pt-3">
          <div className="flex items-center gap-3 p-2.5 bg-muted rounded-xl">
            {attachment.isImage ? (
              <img src={attachment.previewUrl} alt="Attachment" className="w-12 h-12 rounded-lg object-cover" />
            ) : (
              <div className="w-12 h-12 rounded-lg bg-accent/10 flex items-center justify-center">
                <FileText className="w-5 h-5 text-accent" />
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{attachment.file.name}</p>
              <p className="text-xs text-muted-foreground">{(attachment.file.size / 1024).toFixed(1)} KB</p>
            </div>
            <button onClick={removeAttachment} className="p-1.5 rounded-lg hover:bg-muted-foreground/10 transition-colors cursor-pointer">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Input row */}
      <div className="flex items-end gap-2 px-4 py-3">
        <Button type="button" variant="ghost" size="icon" onClick={() => setShowEmojiPicker((v) => !v)} className="shrink-0 mb-0.5">
          <Smile className={cn("w-5 h-5", showEmojiPicker && "text-accent")} />
        </Button>

        {/* File attach — hidden for AI chats */}
        {isAiChat ? (
          <span
            title="Kalori AI only supports text messages"
            className="shrink-0 mb-0.5 w-9 h-9 flex items-center justify-center opacity-30 cursor-not-allowed"
          >
            <Paperclip className="w-5 h-5" />
          </span>
        ) : (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || isUploading}
            className="shrink-0 mb-0.5"
            id="attach-file-btn"
          >
            {isUploading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Paperclip className="w-5 h-5" />}
          </Button>
        )}
        <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileSelect} accept="image/*,.pdf,.doc,.docx,.txt,.xlsx,.csv,.zip" />

        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={text}
            onChange={(e) => handleInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setShowEmojiPicker(false)}
            placeholder="Type a message..."
            disabled={disabled}
            className={cn(
              "w-full resize-none rounded-2xl border border-input bg-muted/50 px-4 py-2.5 text-sm",
              "placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-accent/30",
              "max-h-[120px] min-h-[42px] scrollbar-thin"
            )}
            rows={1}
            id="message-input"
          />
        </div>

        {/* Voice recorder toggle — hidden for AI chats; shown only when no text/attachment */}
        {!text.trim() && !attachment ? (
          isAiChat ? (
            <span
              title="Kalori AI only supports text messages"
              className="shrink-0 mb-0.5 w-9 h-9 flex items-center justify-center opacity-30 cursor-not-allowed"
            >
              <Mic className="w-5 h-5" />
            </span>
          ) : (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => setShowVoice(true)}
              disabled={disabled}
              className="shrink-0 mb-0.5"
              id="voice-record-btn"
              title="Record voice message"
            >
              <Mic className="w-5 h-5" />
            </Button>
          )
        ) : (
          <Button
            onClick={handleSend}
            disabled={disabled || (!text.trim() && !attachment)}
            size="icon"
            className="shrink-0 rounded-xl bg-accent hover:bg-accent-hover mb-0.5"
            id="send-message-btn"
          >
            <Send className="w-4 h-4" />
          </Button>
        )}
      </div>
    </div>
  );
}
