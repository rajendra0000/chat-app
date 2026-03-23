"use client";

// ==============================
// Notification Sound Hook
// — Play a sound for incoming messages when tab is not focused
// ==============================

import { useEffect, useRef } from "react";

/**
 * Hook that plays a notification sound when a new message arrives
 * and the page is not focused (tab in background).
 */
export function useNotificationSound(messageCount: number) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const prevCountRef = useRef(messageCount);
  const hasFocusRef = useRef(true);

  // Track window focus state
  useEffect(() => {
    const onFocus = () => { hasFocusRef.current = true; };
    const onBlur = () => { hasFocusRef.current = false; };

    window.addEventListener("focus", onFocus);
    window.addEventListener("blur", onBlur);
    return () => {
      window.removeEventListener("focus", onFocus);
      window.removeEventListener("blur", onBlur);
    };
  }, []);

  // Create audio element (using a Web Audio API beep since we don't have a sound file)
  useEffect(() => {
    // Create a small beep using AudioContext
    const createBeep = () => {
      try {
        const audioCtx = new AudioContext();
        const oscillator = audioCtx.createOscillator();
        const gainNode = audioCtx.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(audioCtx.destination);

        oscillator.frequency.value = 800;
        oscillator.type = "sine";
        gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.3);

        oscillator.start(audioCtx.currentTime);
        oscillator.stop(audioCtx.currentTime + 0.3);
      } catch {
        // AudioContext not available
      }
    };

    // Store the beep function for reuse
    audioRef.current = { play: createBeep } as unknown as HTMLAudioElement;
  }, []);

  // Play sound when new messages arrive and tab is blurred
  useEffect(() => {
    if (messageCount > prevCountRef.current && !hasFocusRef.current) {
      try {
        if (audioRef.current) {
          (audioRef.current as unknown as { play: () => void }).play();
        }
      } catch {
        // Ignore autoplay restrictions
      }
    }
    prevCountRef.current = messageCount;
  }, [messageCount]);
}
