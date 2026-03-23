"use client";

// ==============================
// Message List — skeleton loading, reply threading, date separators
// ==============================

import { useRef, useEffect, useCallback, useMemo, useState } from "react";
import { useMessages } from "@/hooks/use-messages";
import { MessageBubble } from "./message-bubble";
import { DateSeparator } from "./date-separator";
import { Loader2, AlertCircle, RefreshCw, X, Reply } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth-store";
import { isSameDay } from "date-fns";
import type { Message } from "@/types";

interface MessageListProps {
  conversationId: number;
  onImageClick?: (url: string) => void;
}

function MessageSkeleton() {
  return (
    <div className="flex flex-col gap-3 px-4 py-2">
      {[false, true, false, true, false, true].map((mine, i) => (
        <div key={i} className={`flex gap-2 ${mine ? "justify-end" : "justify-start"}`}>
          <Skeleton
            className={`h-10 rounded-2xl ${mine ? "rounded-br-md" : "rounded-bl-md"}`}
            style={{ width: `${140 + (i % 3) * 60}px` }}
          />
        </div>
      ))}
    </div>
  );
}

export function MessageList({ conversationId, onImageClick }: MessageListProps) {
  const userId = useAuthStore((s) => s.user?.id);
  const bottomRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const prevScrollHeightRef = useRef<number>(0);
  const isInitialLoadRef = useRef(true);
  const [replyTo, setReplyTo] = useState<Message | null>(null);

  const {
    data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError, refetch,
  } = useMessages(conversationId);

  const messages: Message[] = useMemo(
    () => data?.pages.slice().reverse().flatMap((page) => page.content.slice().reverse()) ?? [],
    [data]
  );

  const parseTimestamp = (ts: string | null | undefined): Date => {
    if (!ts) return new Date();
    const normalized = ts.replace(" ", "T");
    const d = new Date(normalized);
    return isNaN(d.getTime()) ? new Date() : d;
  };

  const messagesWithDates = useMemo(() => {
    const items: Array<{ type: "date"; date: Date; key: string } | { type: "message"; message: Message; key: string | number }> = [];
    let lastDate: Date | null = null;
    for (const msg of messages) {
      const msgDate = parseTimestamp(msg.timestamp);
      if (!lastDate || !isSameDay(lastDate, msgDate)) {
        items.push({ type: "date", date: msgDate, key: `date-${msg.timestamp ?? msg.id}` });
        lastDate = msgDate;
      }
      items.push({ type: "message", message: msg, key: `msg-${msg.id}` });
    }
    return items;
  }, [messages]);

  useEffect(() => {
    if (isInitialLoadRef.current && messages.length > 0) {
      bottomRef.current?.scrollIntoView();
      isInitialLoadRef.current = false;
      return;
    }
    const container = scrollContainerRef.current;
    if (!container) return;
    const distFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
    if (distFromBottom < 120) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  useEffect(() => { isInitialLoadRef.current = true; }, [conversationId]);
  useEffect(() => { setReplyTo(null); }, [conversationId]);

  const handleScroll = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    if (container.scrollTop < 80 && hasNextPage && !isFetchingNextPage) {
      prevScrollHeightRef.current = container.scrollHeight;
      fetchNextPage().then(() => {
        requestAnimationFrame(() => {
          if (scrollContainerRef.current) {
            const newScrollHeight = scrollContainerRef.current.scrollHeight;
            scrollContainerRef.current.scrollTop = newScrollHeight - prevScrollHeightRef.current;
          }
        });
      });
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // ── Skeleton loading (modern feature) ─────────────────────────────────────
  if (isLoading) return <MessageSkeleton />;

  if (isError) {
    return (
      <div className="flex-1 flex items-center justify-center text-center p-8">
        <div>
          <div className="w-16 h-16 rounded-full bg-destructive/10 flex items-center justify-center mx-auto mb-4">
            <AlertCircle className="w-8 h-8 text-destructive" />
          </div>
          <p className="text-muted-foreground text-sm mb-4">
            Failed to load messages. This may be a temporary server issue.
          </p>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-accent text-white hover:bg-accent-hover transition-colors cursor-pointer"
          >
            <RefreshCw className="w-4 h-4" />Retry
          </button>
        </div>
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-center p-8">
        <div>
          <div className="w-16 h-16 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-4">
            <span className="text-3xl">👋</span>
          </div>
          <p className="text-muted-foreground text-sm">No messages yet. Send the first message!</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 min-h-0">
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-4 py-3"
      >
        {isFetchingNextPage && (
          <div className="flex justify-center py-3">
            <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
          </div>
        )}
        {!hasNextPage && messages.length > 0 && (
          <div className="text-center py-3">
            <p className="text-xs text-muted-foreground">Beginning of conversation</p>
          </div>
        )}

        {messagesWithDates.map((item) =>
          item.type === "date" ? (
            <DateSeparator key={item.key} date={item.date} />
          ) : (
            <MessageBubble
              key={item.key}
              message={item.message}
              isMine={item.message.senderId === userId}
              conversationId={conversationId}
              onImageClick={onImageClick}
              onReply={setReplyTo}
            />
          )
        )}

        <div ref={bottomRef} />
      </div>

      {/* Reply quote bar */}
      {replyTo && (
        <div className="mx-4 mb-1 px-3 py-2 rounded-xl bg-muted/70 border-l-2 border-accent flex items-start gap-2 text-sm animate-in slide-in-from-bottom-2">
          <Reply className="w-4 h-4 text-accent shrink-0 mt-0.5" />
          <div className="flex-1 min-w-0">
            <p className="text-[11px] text-accent font-semibold mb-0.5 truncate">
              Replying to {replyTo.senderName || "Unknown"}
            </p>
            <p className="text-xs text-muted-foreground truncate">{replyTo.text}</p>
          </div>
          <button onClick={() => setReplyTo(null)} className="p-0.5 rounded hover:bg-muted transition-colors cursor-pointer shrink-0">
            <X className="w-3.5 h-3.5 text-muted-foreground" />
          </button>
        </div>
      )}
    </div>
  );
}
