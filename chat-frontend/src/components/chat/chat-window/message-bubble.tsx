"use client";

// ==============================
// Message Bubble — + reactions, reply-quote, pin/unpin, last-seen ticks
// Mobile: long-press (300ms) reveals action bar
// ==============================

import { useState, useRef, useCallback } from "react";
import { cn } from "@/lib/utils";
import {
  Pencil, Trash2, Check, X, FileText, Download, CheckCheck, Bot,
  SmilePlus, Reply, Pin, PinOff,
} from "lucide-react";
import { editMessage, deleteMessage, reactToMessage, pinMessage } from "@/services/message-service";
import { useUpdateMessageInCache } from "@/hooks/use-messages";
import { toast } from "sonner";
import type { Message } from "@/types";

interface MessageBubbleProps {
  message: Message;
  isMine: boolean;
  conversationId: number;
  isRead?: boolean;
  onImageClick?: (url: string) => void;
  onReply?: (message: Message) => void; // reply threading
}

const QUICK_REACTIONS = ["👍", "❤️", "😂", "😮", "😢", "🔥"];
const LONG_PRESS_DURATION = 300; // ms

function formatTime(isoString: string): string {
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function formatAudioTime(s: number) {
  const m = Math.floor(s / 60);
  return `${m}:${String(Math.floor(s % 60)).padStart(2, "0")}`;
}

/** Compact audio player for voice messages */
function AudioBubble({ url, isMine }: { url: string; isMine: boolean }) {
  const [playing, setPlaying] = useState(false);
  const [current, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const toggle = () => {
    if (!audioRef.current) {
      audioRef.current = new Audio(url);
      audioRef.current.onloadedmetadata = () => setDuration(audioRef.current!.duration);
      audioRef.current.ontimeupdate = () => setCurrent(audioRef.current!.currentTime);
      audioRef.current.onended = () => { setPlaying(false); setCurrent(0); };
    }
    if (playing) { audioRef.current.pause(); setPlaying(false); }
    else { audioRef.current.play(); setPlaying(true); }
  };

  const barCount = 20;
  const progress = duration > 0 ? current / duration : 0;

  return (
    <div className={cn("flex items-center gap-2.5 py-1 min-w-[180px] max-w-[240px]")}>
      {/* Play / Pause */}
      <button
        onClick={toggle}
        className={cn(
          "w-8 h-8 rounded-full shrink-0 flex items-center justify-center transition-colors",
          isMine ? "bg-white/20 hover:bg-white/30" : "bg-accent/20 hover:bg-accent/30"
        )}
      >
        {playing ? (
          <span className="flex gap-[3px]">
            <span className={cn("w-[3px] h-3 rounded-full", isMine ? "bg-white" : "bg-accent")} />
            <span className={cn("w-[3px] h-3 rounded-full", isMine ? "bg-white" : "bg-accent")} />
          </span>
        ) : (
          <span className={cn("border-l-[10px] border-y-[6px] border-y-transparent ml-0.5", isMine ? "border-l-white" : "border-l-accent")} />
        )}
      </button>

      {/* Waveform bars */}
      <div className="flex items-center gap-[2px] flex-1">
        {Array.from({ length: barCount }, (_, i) => {
          const filled = i / barCount < progress;
          return (
            <div
              key={i}
              className={cn(
                "rounded-full flex-1 transition-all",
                filled
                  ? isMine ? "bg-white" : "bg-accent"
                  : isMine ? "bg-white/30" : "bg-muted-foreground/30",
                playing && !filled ? "animate-pulse" : ""
              )}
              style={{ height: `${6 + Math.sin(i) * 4 + (i % 3) * 2}px` }}
            />
          );
        })}
      </div>

      {/* Time */}
      <span className={cn("text-[10px] tabular-nums shrink-0", isMine ? "text-white/70" : "text-muted-foreground")}>
        {playing || current > 0 ? formatAudioTime(current) : duration > 0 ? formatAudioTime(duration) : "0:00"}
      </span>
    </div>
  );
}

export function MessageBubble({
  message, isMine, conversationId, isRead, onImageClick, onReply,
}: MessageBubbleProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(message.text);
  const [isActing, setIsActing] = useState(false);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  // showActions: true = hover bar is forced visible (mobile long-press)
  const [showActions, setShowActions] = useState(false);
  const updateMessageInCache = useUpdateMessageInCache();

  // ── Long-press detection (mobile) ─────────────────────────────────────────
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleTouchStart = useCallback(() => {
    longPressTimerRef.current = setTimeout(() => {
      setShowActions(true);
    }, LONG_PRESS_DURATION);
  }, []);

  const handleTouchEnd = useCallback(() => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  }, []);

  // Close action bar when tapping outside
  const handleBubbleClick = useCallback(() => {
    if (showActions) setShowActions(false);
  }, [showActions]);

  const isAI = message.senderName === "Kalori" || message.senderName === "AI" || message.senderName === "AI Assistant";
  const isPending = typeof message.id === "string" && (message.id as string).startsWith("pending-");

  const handleEdit = async () => {
    if (!editText.trim() || editText === message.text) { setIsEditing(false); return; }
    setIsActing(true);
    try {
      await editMessage(message.id as number, editText.trim());
      updateMessageInCache(conversationId, message.id, { text: editText.trim(), edited: true });
      setIsEditing(false);
      toast.success("Message edited.");
    } catch { toast.error("Failed to edit message."); }
    finally { setIsActing(false); }
  };

  const handleDelete = async () => {
    if (!window.confirm("Delete this message?")) return;
    setIsActing(true);
    try {
      await deleteMessage(message.id as number);
      updateMessageInCache(conversationId, message.id, { deleted: true, text: "" });
      toast.success("Message deleted.");
    } catch { toast.error("Failed to delete message."); }
    finally { setIsActing(false); }
  };

  const handleReact = async (emoji: string) => {
    setShowReactionPicker(false);
    try {
      await reactToMessage(message.id as number, emoji);
      // Server broadcasts REACTION_UPDATE which reconciles counts in real-time
    } catch { toast.error("Failed to react."); }
  };

  // ── Pin / Unpin toggle ─────────────────────────────────────────────────────
  // The backend endpoint (PATCH /api/messages/{id}/pin) already toggles the pin
  // state and broadcasts a PIN_TOGGLED WS event. We just need to call it and
  // locally pre-update the cache so the UI feels instant.
  const handleTogglePin = async () => {
    setShowActions(false);
    const wasAlreadyPinned = Boolean(message.pinned);
    try {
      await pinMessage(message.id as number);
      updateMessageInCache(conversationId, message.id, { pinned: !wasAlreadyPinned });
      toast.success(wasAlreadyPinned ? "Message unpinned." : "Message pinned.");
    } catch { toast.error("Failed to toggle pin."); }
  };

  // Deleted message
  if (message.deleted) {
    return (
      <div className={cn("flex mb-1.5", isMine ? "justify-end" : "justify-start")}>
        <div className="max-w-[75%] px-4 py-2 rounded-2xl bg-muted/50 text-muted-foreground text-xs italic">
          🚫 This message was deleted
          <span className="ml-2 text-[10px] opacity-60">{formatTime(message.timestamp)}</span>
        </div>
      </div>
    );
  }

  // AI message variant
  if (isAI) {
    return (
      <div className="flex mb-2 justify-start">
        <div className="max-w-[80%] px-4 py-3 rounded-2xl rounded-bl-md bg-gradient-to-br from-purple-500/15 to-blue-500/10 border border-purple-500/20 text-sm">
          <div className="flex items-center gap-1.5 mb-1.5">
            <Bot className="w-3.5 h-3.5 text-purple-400" />
            <span className="text-[11px] font-semibold text-purple-400">Kalori AI</span>
          </div>
          {message.text && <p className="whitespace-pre-wrap text-foreground">{message.text}</p>}
          <div className="flex items-center mt-1.5">
            <span className="text-[10px] text-muted-foreground">{formatTime(message.timestamp)}</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className={cn("group flex mb-1.5 relative", isMine ? "justify-end" : "justify-start", isPending && "opacity-60")}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      onTouchCancel={handleTouchEnd}
      onClick={handleBubbleClick}
    >

      {/* Pinned banner */}
      {message.pinned && (
        <div className="absolute -top-4 left-0 right-0 flex justify-center">
          <span className="text-[9px] text-muted-foreground flex items-center gap-0.5">
            <Pin className="w-2.5 h-2.5" />pinned
          </span>
        </div>
      )}

      <div className={cn(
        "relative max-w-[75%] px-4 py-2.5 rounded-2xl text-sm break-words",
        isMine ? "bg-sent-bubble text-white rounded-br-md" : "bg-received-bubble text-foreground rounded-bl-md"
      )}>

        {/* Reply quote preview */}
        {message.replyTo && (
          <div className={cn(
            "mb-2 px-2 py-1 rounded-lg border-l-2 text-xs opacity-80",
            isMine ? "border-white/40 bg-white/10" : "border-accent/50 bg-muted/50"
          )}>
            <p className="font-semibold text-[10px] mb-0.5">{message.replyTo.senderName || "Unknown"}</p>
            <p className="truncate">{message.replyTo.text}</p>
          </div>
        )}

        {/* Edit mode */}
        {isEditing ? (
          <div className="space-y-2">
            <textarea
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              className="w-full bg-transparent border border-white/30 rounded-lg p-2 text-sm resize-none focus:outline-none min-h-[60px]"
              autoFocus
            />
            <div className="flex gap-1.5 justify-end">
              <button onClick={() => { setIsEditing(false); setEditText(message.text); }} disabled={isActing} className="p-1 rounded hover:bg-white/20 transition-colors cursor-pointer">
                <X className="w-3.5 h-3.5" />
              </button>
              <button onClick={handleEdit} disabled={isActing} className="p-1 rounded hover:bg-white/20 transition-colors cursor-pointer">
                <Check className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* File attachments */}
            {message.documents && message.documents.length > 0 && (
              <div className="mb-2 space-y-1.5">
                {message.documents.map((doc) => {
                  const url = doc.url;
                  const isImage = doc.fileType?.startsWith("image/");
                  const isAudio = doc.fileType?.startsWith("audio/");
                  if (isAudio) return <AudioBubble key={doc.id} url={url} isMine={isMine} />;
                  return isImage ? (
                    <button key={doc.id} onClick={() => onImageClick?.(url)} className="block cursor-pointer">
                      <img src={url} alt={doc.fileName} className="max-w-full max-h-48 rounded-lg object-cover hover:opacity-90 transition-opacity" loading="lazy" />
                    </button>
                  ) : (
                    <a key={doc.id} href={url} target="_blank" rel="noopener noreferrer"
                      className={cn("flex items-center gap-2 p-2 rounded-lg transition-colors cursor-pointer",
                        isMine ? "bg-white/10 hover:bg-white/20" : "bg-muted hover:bg-muted/80")}>
                      <FileText className="w-4 h-4 shrink-0" />
                      <span className="text-xs truncate flex-1">{doc.fileName}</span>
                      <Download className="w-3.5 h-3.5 shrink-0 opacity-60" />
                    </a>
                  );
                })}
              </div>
            )}

            {/* Message text */}
            {message.text && <p className="whitespace-pre-wrap">{message.text}</p>}

            {/* Reaction bubbles */}
            {message.reactions && Object.keys(message.reactions).length > 0 && (
              <div className="flex flex-wrap gap-1 mt-1.5">
                {(Object.entries(message.reactions) as [string, number][]).map(([emoji, count]) => (
                  <button
                    key={emoji}
                    onClick={() => handleReact(emoji)}
                    className={cn(
                      "flex items-center gap-0.5 text-xs px-1.5 py-0.5 rounded-full border transition-colors",
                      isMine ? "bg-white/10 border-white/20 hover:bg-white/20" : "bg-muted border-border hover:bg-muted/80"
                    )}
                  >
                    {emoji} <span className="text-[10px]">{count}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Meta row: edited + time + read receipt */}
            <div className={cn("flex items-center gap-1 mt-1", isMine ? "justify-end" : "justify-start")}>
              {message.edited && <span className="text-[10px] opacity-50 italic">edited</span>}
              <span className={cn("text-[10px]", isMine ? "opacity-60" : "text-muted-foreground")}>
                {formatTime(message.timestamp)}
              </span>
              {isMine && !isPending && (
                isRead
                  ? <CheckCheck className="w-3.5 h-3.5 text-blue-300" />
                  : <Check className="w-3.5 h-3.5 opacity-50" />
              )}
              {isPending && <span className="text-[10px] opacity-50">sending...</span>}
            </div>
          </>
        )}

        {/* Action bar — shown on desktop :hover OR mobile long-press */}
        {!isEditing && !isPending && (
          <div className={cn(
            "absolute -top-8 flex gap-0.5 bg-card border border-border rounded-xl shadow-lg px-1 py-0.5 z-10",
            // Desktop: opacity-0 until group-hover; Mobile: controlled by showActions state
            showActions
              ? "opacity-100"
              : "opacity-0 group-hover:opacity-100",
            "transition-opacity",
            isMine ? "right-0" : "left-0"
          )}>
            {/* React */}
            <div className="relative">
              <button
                onClick={(e) => { e.stopPropagation(); setShowReactionPicker((v) => !v); }}
                className="p-1.5 rounded-lg hover:bg-muted transition-colors cursor-pointer"
                title="React"
              >
                <SmilePlus className="w-3.5 h-3.5 text-muted-foreground" />
              </button>
              {showReactionPicker && (
                <div className="absolute bottom-full mb-1 left-0 flex gap-1 bg-card border border-border rounded-xl shadow-xl p-1.5 z-50">
                  {QUICK_REACTIONS.map((emoji) => (
                    <button key={emoji} onClick={(e) => { e.stopPropagation(); handleReact(emoji); }} className="text-base hover:scale-125 transition-transform cursor-pointer p-0.5">
                      {emoji}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Reply */}
            {onReply && (
              <button onClick={(e) => { e.stopPropagation(); onReply(message); }} className="p-1.5 rounded-lg hover:bg-muted transition-colors cursor-pointer" title="Reply">
                <Reply className="w-3.5 h-3.5 text-muted-foreground" />
              </button>
            )}

            {/* Pin / Unpin — icon and colour reflect current state */}
            <button
              onClick={(e) => { e.stopPropagation(); handleTogglePin(); }}
              className={cn(
                "p-1.5 rounded-lg transition-colors cursor-pointer",
                message.pinned ? "hover:bg-accent/10" : "hover:bg-muted"
              )}
              title={message.pinned ? "Unpin" : "Pin"}
            >
              {message.pinned ? (
                <PinOff className="w-3.5 h-3.5 text-accent" />
              ) : (
                <Pin className="w-3.5 h-3.5 text-muted-foreground" />
              )}
            </button>

            {/* Edit + Delete (own messages only) */}
            {isMine && (
              <>
                <button onClick={(e) => { e.stopPropagation(); setIsEditing(true); }} className="p-1.5 rounded-lg hover:bg-muted transition-colors cursor-pointer" title="Edit">
                  <Pencil className="w-3.5 h-3.5 text-muted-foreground" />
                </button>
                <button onClick={(e) => { e.stopPropagation(); handleDelete(); }} disabled={isActing} className="p-1.5 rounded-lg hover:bg-destructive/10 transition-colors cursor-pointer" title="Delete">
                  <Trash2 className="w-3.5 h-3.5 text-destructive" />
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
