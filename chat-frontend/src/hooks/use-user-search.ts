"use client";

// ==============================
// User Search Hook (Debounced)
// ==============================

import { useState, useEffect, useCallback } from "react";
import { searchUsers } from "@/services/user-service";
import { MIN_SEARCH_LENGTH } from "@/lib/constants";
import type { UserSearchResult } from "@/types";

export function useUserSearch() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (query.length < MIN_SEARCH_LENGTH) {
      setResults([]);
      setError(null);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      setError(null);
      try {
        const users = await searchUsers(query);
        setResults(users);
      } catch {
        setError("Error searching users.");
        setResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300); // 300ms debounce

    return () => clearTimeout(timer);
  }, [query]);

  const reset = useCallback(() => {
    setQuery("");
    setResults([]);
    setError(null);
  }, []);

  return { query, setQuery, results, isSearching, error, reset };
}
