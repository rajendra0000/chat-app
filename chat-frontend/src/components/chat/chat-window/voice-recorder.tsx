"use client";

// ==============================
// VoiceRecorder — canvas waveform + MediaRecorder + Cloudinary upload
// ==============================

import { useState, useRef, useEffect, useCallback } from "react";
import { Mic, Square, Send, X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { uploadFileWithProgress } from "@/services/file-service";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

interface VoiceRecorderProps {
  onSend: (documentId: number) => void;
  onCancel: () => void;
}

function formatDuration(ms: number) {
  const s = Math.floor(ms / 1000);
  const m = Math.floor(s / 60);
  return `${m}:${String(s % 60).padStart(2, "0")}`;
}

export function VoiceRecorder({ onSend, onCancel }: VoiceRecorderProps) {
  const [phase, setPhase] = useState<"idle" | "recording" | "preview" | "uploading">("idle");
  const [durationMs, setDurationMs] = useState(0);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animFrameRef = useRef<number>(0);
  const streamRef = useRef<MediaStream | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const startTimeRef = useRef<number>(0);
  const timerRef = useRef<ReturnType<typeof setInterval>>(undefined);
  const audioElRef = useRef<HTMLAudioElement | null>(null);

  // Start on mount
  useEffect(() => {
    startRecording();
    return () => cleanup();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const cleanup = () => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    if (timerRef.current) clearInterval(timerRef.current);
    streamRef.current?.getTracks().forEach((t) => t.stop());
    if (audioCtxRef.current && audioCtxRef.current.state !== "closed") {
      audioCtxRef.current.close().catch(() => {});
    }
    if (audioUrl) URL.revokeObjectURL(audioUrl);
  };

  const drawWaveform = useCallback(() => {
    const analyser = analyserRef.current;
    const canvas = canvasRef.current;
    if (!analyser || !canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const buf = new Uint8Array(analyser.frequencyBinCount);
    analyser.getByteTimeDomainData(buf);

    const { width: w, height: h } = canvas;
    ctx.clearRect(0, 0, w, h);

    // Gradient bar waveform
    const gradient = ctx.createLinearGradient(0, 0, w, 0);
    gradient.addColorStop(0, "#7c3aed");
    gradient.addColorStop(1, "#a78bfa");
    ctx.strokeStyle = gradient;
    ctx.lineWidth = 2.5;
    ctx.lineCap = "round";

    ctx.beginPath();
    const sliceWidth = w / buf.length;
    let x = 0;
    for (let i = 0; i < buf.length; i++) {
      const v = buf[i] / 128;
      const y = (v * h) / 2;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
      x += sliceWidth;
    }
    ctx.lineTo(w, h / 2);
    ctx.stroke();

    animFrameRef.current = requestAnimationFrame(drawWaveform);
  }, []);

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      const ctx = new AudioContext();
      audioCtxRef.current = ctx;
      const src = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 256;
      src.connect(analyser);
      analyserRef.current = analyser;

      const mr = new MediaRecorder(stream, { mimeType: "audio/webm;codecs=opus" });
      mediaRecorderRef.current = mr;
      chunksRef.current = [];

      mr.ondataavailable = (e) => { if (e.data.size > 0) chunksRef.current.push(e.data); };
      mr.onstop = () => {
        if (timerRef.current) clearInterval(timerRef.current);
        const blob = new Blob(chunksRef.current, { type: "audio/webm" });
        const url = URL.createObjectURL(blob);
        setAudioBlob(blob);
        setAudioUrl(url);
        setPhase("preview");
      };

      mr.start(100);
      startTimeRef.current = Date.now();
      setPhase("recording");

      // Live duration timer
      timerRef.current = setInterval(() => {
        setDurationMs(Date.now() - startTimeRef.current);
      }, 200);

      drawWaveform();
    } catch {
      toast.error("Microphone access denied.");
      onCancel();
    }
  };

  const stopRecording = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    streamRef.current?.getTracks().forEach((t) => t.stop());
    if (audioCtxRef.current && audioCtxRef.current.state !== "closed") {
      audioCtxRef.current.close().catch(() => {});
    }
    mediaRecorderRef.current?.stop();
  };

  const handleSend = async () => {
    if (!audioBlob) return;
    setPhase("uploading");
    const file = new File([audioBlob], `voice-${Date.now()}.webm`, { type: "audio/webm" });
    const { promise } = uploadFileWithProgress(file, (p) => setUploadProgress(p));
    try {
      const { documentId } = await promise;
      cleanup();
      onSend(documentId);
    } catch {
      toast.error("Failed to upload voice message.");
      setPhase("preview");
    }
  };

  const togglePlay = () => {
    if (!audioUrl) return;
    if (!audioElRef.current) {
      audioElRef.current = new Audio(audioUrl);
      audioElRef.current.onended = () => setIsPlaying(false);
    }
    if (isPlaying) {
      audioElRef.current.pause();
      setIsPlaying(false);
    } else {
      audioElRef.current.play();
      setIsPlaying(true);
    }
  };

  return (
    <div className="flex items-center gap-3 px-4 py-3">
      {/* Cancel */}
      <Button variant="ghost" size="icon" onClick={onCancel} className="shrink-0 text-muted-foreground hover:text-destructive">
        <X className="w-5 h-5" />
      </Button>

      {/* Canvas / playback area */}
      <div className="flex-1 flex items-center gap-3 bg-muted/60 rounded-2xl px-4 py-2.5 min-h-[42px]">
        {phase === "recording" && (
          <>
            {/* Pulsing dot */}
            <span className="w-2.5 h-2.5 rounded-full bg-destructive animate-pulse shrink-0" />
            <canvas ref={canvasRef} width={200} height={32} className="flex-1 h-8" />
            <span className="text-xs text-muted-foreground tabular-nums shrink-0">
              {formatDuration(durationMs)}
            </span>
          </>
        )}

        {phase === "preview" && (
          <>
            <button
              onClick={togglePlay}
              className={cn(
                "w-8 h-8 rounded-full flex items-center justify-center shrink-0 transition-colors",
                "bg-accent hover:bg-accent-hover text-white"
              )}
            >
              {isPlaying ? (
                <Square className="w-3.5 h-3.5 fill-current" />
              ) : (
                <span className="border-l-[10px] border-l-white border-y-[6px] border-y-transparent ml-0.5" />
              )}
            </button>
            {/* Static waveform bars (decorative) */}
            <div className="flex-1 flex items-center gap-px">
              {Array.from({ length: 40 }, (_, i) => (
                <div
                  key={i}
                  className="flex-1 bg-accent/60 rounded-full"
                  style={{ height: `${8 + Math.sin(i * 0.5) * 6 + Math.random() * 8}px` }}
                />
              ))}
            </div>
            <span className="text-xs text-muted-foreground tabular-nums shrink-0">
              {formatDuration(durationMs)}
            </span>
          </>
        )}

        {phase === "uploading" && (
          <div className="flex-1 flex items-center gap-3">
            <Loader2 className="w-4 h-4 animate-spin text-accent shrink-0" />
            <div className="flex-1 h-1.5 bg-muted-foreground/20 rounded-full overflow-hidden">
              <div className="h-full bg-accent rounded-full transition-all" style={{ width: `${uploadProgress}%` }} />
            </div>
            <span className="text-xs text-muted-foreground">{uploadProgress}%</span>
          </div>
        )}
      </div>

      {/* Right action */}
      {phase === "recording" && (
        <Button
          variant="ghost"
          size="icon"
          onClick={stopRecording}
          className="shrink-0 text-destructive hover:bg-destructive/10"
          title="Stop recording"
        >
          <Square className="w-5 h-5 fill-current" />
        </Button>
      )}

      {phase === "preview" && (
        <Button
          size="icon"
          onClick={handleSend}
          className="shrink-0 rounded-xl bg-accent hover:bg-accent-hover"
          title="Send voice message"
          id="send-voice-btn"
        >
          <Send className="w-4 h-4" />
        </Button>
      )}

      {phase === "uploading" && (
        <Button size="icon" disabled className="shrink-0 rounded-xl bg-accent opacity-50">
          <Loader2 className="w-4 h-4 animate-spin" />
        </Button>
      )}
    </div>
  );
}
