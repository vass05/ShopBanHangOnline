import * as React from "react";
import { cn } from "@/lib/utils";

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "secondary" | "destructive" | "outline" | "ocean" | "shopee" | "success" | "mall" | "warning";
}

function Badge({ className, variant = "default", ...props }: BadgeProps) {
  const variantStyles = {
    default: "border-transparent bg-slate-900 text-slate-50",
    secondary: "border-transparent bg-slate-100 text-slate-800",
    destructive: "border-transparent bg-red-100 text-red-700 font-medium",
    outline: "text-slate-800 border-slate-200",
    ocean: "border-transparent bg-sky-100 text-[#0284C7] font-semibold",
    shopee: "border-transparent bg-sky-100 text-[#0284C7] font-semibold",
    success: "border-transparent bg-emerald-100 text-emerald-700 font-medium",
    mall: "border-transparent bg-[#D0011B] text-white font-bold tracking-wider text-[10px]",
    warning: "border-transparent bg-amber-100 text-amber-800 font-medium",
  };

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
        variantStyles[variant],
        className
      )}
      {...props}
    />
  );
}

export { Badge };
