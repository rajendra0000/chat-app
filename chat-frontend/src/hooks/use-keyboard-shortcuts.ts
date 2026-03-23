"use client";

// ==============================
// Keyboard Shortcuts Hook
// ==============================

import { useEffect } from "react";

interface ShortcutConfig {
  onSearch?: () => void;    // Ctrl+K
  onEscape?: () => void;    // Escape
}

export function useKeyboardShortcuts({ onSearch, onEscape }: ShortcutConfig) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      // Ctrl+K / Cmd+K → open search
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        onSearch?.();
      }

      // Escape → close
      if (e.key === "Escape") {
        onEscape?.();
      }
    };

    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [onSearch, onEscape]);
}
