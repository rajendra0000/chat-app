"use client";

// ==============================
// Client Providers
// ==============================

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { Toaster } from "sonner";
import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/auth-store";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      refetchOnWindowFocus: false,
      staleTime: 2 * 60 * 1000, // 2 minutes default
    },
    mutations: {
      retry: 0,
    },
  },
});

/**
 * Initializes auth state from localStorage on mount.
 */
function AuthInitializer({ children }: { children: React.ReactNode }) {
  const initialize = useAuthStore((s) => s.initialize);
  const isInitialized = useAuthStore((s) => s.isInitialized);

  useEffect(() => {
    initialize();
  }, [initialize]);

  if (!isInitialized) {
    return null; // Don't render children until auth state is loaded
  }

  return <>{children}</>;
}

export default function Providers({ children }: { children: React.ReactNode }) {
  // Prevent hydration mismatch with ThemeProvider
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider
        attribute="class"
        defaultTheme="dark"
        enableSystem
        disableTransitionOnChange
      >
        <AuthInitializer>
          {mounted ? children : null}
        </AuthInitializer>
        <Toaster
          position="top-right"
          richColors
          closeButton
          toastOptions={{
            duration: 4000,
          }}
        />
      </ThemeProvider>
    </QueryClientProvider>
  );
}
