"use client";

// ==============================
// App Layout — Auth-guarded shell
// Mobile: shows sidebar OR chat exclusively (WhatsApp-style)
// Desktop: side-by-side panels
// ==============================

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { Sidebar } from "@/components/chat/sidebar/sidebar";
import { ErrorBoundary } from "@/components/error-boundary";
import { useAuthStore } from "@/store/auth-store";
import { cn } from "@/lib/utils";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);

  // On mobile: show sidebar (list view) ONLY when at /chat
  // At /chat/[id] or /settings: show main content full-screen
  const showSidebarMobile = pathname === "/chat";

  useEffect(() => {
    if (isInitialized && !isAuthenticated()) {
      router.replace("/login");
    }
  }, [isInitialized, isAuthenticated, router]);

  if (!isInitialized || !isAuthenticated()) {
    return (
      <div className="h-[100dvh] flex items-center justify-center bg-gradient-to-br from-accent to-[#000DFF]">
        <div className="w-10 h-10 border-[3px] border-white border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="h-[100dvh] overflow-hidden bg-gradient-to-br from-accent to-[#000DFF]">
      <div className="h-full p-0 sm:p-2 lg:p-4">
        <div
          className="h-full bg-sidebar sm:rounded-2xl shadow-2xl overflow-hidden flex"
          role="application"
          aria-label="Chat application"
        >
          {/* Sidebar panel
              Mobile: full-width on /chat (list view), hidden everywhere else
              Desktop (lg+): always visible at fixed width */}
          <div
            className={cn(
              "flex flex-col lg:w-80 lg:flex lg:shrink-0 border-r border-border bg-sidebar",
              showSidebarMobile ? "flex w-full" : "hidden"
            )}
          >
            <Sidebar />
          </div>

          {/* Main content panel
              Mobile: full-width when NOT on /chat, hidden at /chat
              Desktop: always visible, fills remaining space */}
          <main
            className={cn(
              "flex-col flex-1 bg-background overflow-hidden",
              showSidebarMobile ? "hidden lg:flex" : "flex"
            )}
            role="main"
            aria-label="Chat content"
          >
            <ErrorBoundary fallbackTitle="Chat error">
              {children}
            </ErrorBoundary>
          </main>
        </div>
      </div>
    </div>
  );
}

