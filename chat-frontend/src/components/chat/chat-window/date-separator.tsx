"use client";

// ==============================
// Date Separator Component
// ==============================

import { isToday, isYesterday, format } from "date-fns";

interface DateSeparatorProps {
  date: Date;
}

export function DateSeparator({ date }: DateSeparatorProps) {
  // Guard against invalid dates (null, undefined, or unparseable strings)
  if (!date || isNaN(date.getTime())) return null;

  let label: string;

  if (isToday(date)) {
    label = "Today";
  } else if (isYesterday(date)) {
    label = "Yesterday";
  } else {
    label = format(date, "MMMM d, yyyy");
  }

  return (
    <div className="flex items-center gap-3 py-3 px-2">
      <div className="flex-1 h-px bg-border" />
      <span className="text-[11px] font-medium text-muted-foreground bg-background px-3 py-1 rounded-full border border-border select-none">
        {label}
      </span>
      <div className="flex-1 h-px bg-border" />
    </div>
  );
}
