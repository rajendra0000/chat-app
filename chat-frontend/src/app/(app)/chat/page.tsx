"use client";

/**
 * Chat welcome screen — shown on desktop when no conversation is selected.
 * On mobile this pane is hidden by the layout; the sidebar (conversation list) is shown instead.
 */
export default function ChatPage() {
  return (
    <div className="flex items-center justify-center h-full text-center p-8">
      <div>
        <div className="w-28 h-28 rounded-full bg-accent/10 text-accent flex items-center justify-center text-5xl mx-auto mb-8">
          💬
        </div>
        <h2 className="text-2xl font-semibold text-foreground mb-2">
          Welcome to SkibidiChat
        </h2>
        <p className="text-muted-foreground max-w-sm mx-auto">
          Select a conversation from the sidebar to start messaging, or create a new chat.
        </p>
      </div>
    </div>
  );
}
