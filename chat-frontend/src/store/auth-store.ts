// ==============================
// Auth Store (Zustand)
// ==============================

import { create } from "zustand";
import type { User } from "@/types";

interface AuthState {
  /** JWT token */
  token: string | null;
  /** Current logged-in user */
  user: User | null;
  /** Whether the auth state has been initialized */
  isInitialized: boolean;

  /** Set the JWT token (also persists to localStorage) */
  setToken: (token: string) => void;
  /** Set the current user */
  setUser: (user: User) => void;
  /** Initialize auth from localStorage */
  initialize: () => void;
  /** Clear auth state and redirect to login */
  logout: () => void;
  /** Check if the user is authenticated */
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  user: null,
  isInitialized: false,

  setToken: (token: string) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("Authorization", token);
    }
    set({ token });
  },

  setUser: (user: User) => {
    set({ user });
  },

  initialize: () => {
    if (typeof window === "undefined") {
      set({ isInitialized: true });
      return;
    }
    const token = localStorage.getItem("Authorization");
    set({ token, isInitialized: true });
  },

  logout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("Authorization");
      window.location.href = "/login";
    }
    set({ token: null, user: null });
  },

  isAuthenticated: () => {
    return !!get().token;
  },
}));
