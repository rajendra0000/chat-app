/**
 * Plays a soft notification ping using the Web Audio API (no audio file needed).
 * Triggers `navigator.vibrate` for haptic on mobile.
 * Both are no-ops when the tab is visible or the browser doesn't support them.
 */
export function notifyUser(): void {
  // Only fire when tab is hidden (background / another window in focus)
  if (typeof document !== "undefined" && !document.hidden) return;

  // ── Haptic (mobile / supported browsers) ─────────────────────────
  if (typeof navigator !== "undefined" && "vibrate" in navigator) {
    navigator.vibrate(80); // 80ms single pulse
  }

  // ── Audio ping (Web Audio API synth — no file required) ──────────
  try {
    const ctx = new AudioContext();

    // Primary: soft sine tone
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.value = 880; // A5 — bright but not harsh
    gain.gain.setValueAtTime(0.0001, ctx.currentTime); // start silent
    gain.gain.exponentialRampToValueAtTime(0.18, ctx.currentTime + 0.01);  // fast attack
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.25); // decay
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + 0.28);

    // Close context after sound finishes to avoid resource leak
    osc.onended = () => ctx.close();
  } catch {
    // AudioContext blocked or unsupported — silently ignore
  }
}
