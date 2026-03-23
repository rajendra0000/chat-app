"use client";

// ==============================
// Image Viewer — Fullscreen Lightbox
// — zoom, pan, keyboard navigation, Escape close
// ==============================

import { useState, useRef, useCallback, useEffect, type WheelEvent, type MouseEvent } from "react";
import { X, ZoomIn, ZoomOut, RotateCcw, Download, ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface ImageViewerProps {
  images: string[];
  initialIndex?: number;
  onClose: () => void;
}

export function ImageViewer({ images, initialIndex = 0, onClose }: ImageViewerProps) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStartRef = useRef({ x: 0, y: 0 });
  const posStartRef = useRef({ x: 0, y: 0 });

  const currentImage = images[currentIndex];
  const hasMultiple = images.length > 1;

  // Keyboard navigation
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      switch (e.key) {
        case "Escape":
          onClose();
          break;
        case "ArrowLeft":
          if (hasMultiple) goToPrev();
          break;
        case "ArrowRight":
          if (hasMultiple) goToNext();
          break;
        case "+":
        case "=":
          zoomIn();
          break;
        case "-":
          zoomOut();
          break;
        case "0":
          resetView();
          break;
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [hasMultiple, onClose]);

  const zoomIn = () => setScale((s) => Math.min(s * 1.3, 5));
  const zoomOut = () => setScale((s) => Math.max(s / 1.3, 0.5));
  const resetView = () => { setScale(1); setPosition({ x: 0, y: 0 }); };

  const goToNext = useCallback(() => {
    setCurrentIndex((i) => (i + 1) % images.length);
    resetView();
  }, [images.length]);

  const goToPrev = useCallback(() => {
    setCurrentIndex((i) => (i - 1 + images.length) % images.length);
    resetView();
  }, [images.length]);

  // Scroll to zoom
  const handleWheel = (e: WheelEvent) => {
    e.preventDefault();
    if (e.deltaY < 0) zoomIn();
    else zoomOut();
  };

  // Drag to pan
  const handleMouseDown = (e: MouseEvent) => {
    if (scale <= 1) return;
    setIsDragging(true);
    dragStartRef.current = { x: e.clientX, y: e.clientY };
    posStartRef.current = { ...position };
  };

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging) return;
    setPosition({
      x: posStartRef.current.x + (e.clientX - dragStartRef.current.x),
      y: posStartRef.current.y + (e.clientY - dragStartRef.current.y),
    });
  };

  const handleMouseUp = () => setIsDragging(false);

  const handleDownload = () => {
    const a = document.createElement("a");
    a.href = currentImage;
    a.download = `image-${Date.now()}.jpg`;
    a.target = "_blank";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/95 flex flex-col"
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      {/* Toolbar */}
      <div className="flex items-center justify-between px-4 py-3 shrink-0">
        <div className="flex items-center gap-1">
          {hasMultiple && (
            <span className="text-sm text-white/60 mr-3">
              {currentIndex + 1} / {images.length}
            </span>
          )}
          <button onClick={zoomIn} className="p-2 rounded-lg hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer" title="Zoom in (+)">
            <ZoomIn className="w-5 h-5" />
          </button>
          <button onClick={zoomOut} className="p-2 rounded-lg hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer" title="Zoom out (-)">
            <ZoomOut className="w-5 h-5" />
          </button>
          <button onClick={resetView} className="p-2 rounded-lg hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer" title="Reset (0)">
            <RotateCcw className="w-5 h-5" />
          </button>
          <span className="text-xs text-white/40 ml-2">{Math.round(scale * 100)}%</span>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={handleDownload} className="p-2 rounded-lg hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer" title="Download">
            <Download className="w-5 h-5" />
          </button>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer" title="Close (Esc)">
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Image area */}
      <div
        className={cn(
          "flex-1 flex items-center justify-center overflow-hidden relative",
          scale > 1 ? "cursor-grab" : "cursor-default",
          isDragging && "cursor-grabbing"
        )}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onClick={(e) => {
          // Close on backdrop click (not on image)
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <img
          src={currentImage}
          alt={`Image ${currentIndex + 1}`}
          className="max-w-[90vw] max-h-[85vh] object-contain select-none transition-transform duration-150"
          style={{
            transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
          }}
          draggable={false}
        />
      </div>

      {/* Navigation arrows */}
      {hasMultiple && (
        <>
          <button
            onClick={goToPrev}
            className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors cursor-pointer"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
          <button
            onClick={goToNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors cursor-pointer"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        </>
      )}
    </div>
  );
}
