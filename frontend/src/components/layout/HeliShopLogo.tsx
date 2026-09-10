import React from "react";
import { Link } from "react-router-dom";

interface HeliShopLogoProps {
  size?: "sm" | "md" | "lg";
  variant?: "on-dark" | "on-light";
  showBadge?: boolean;
  badgeText?: string;
  className?: string;
}

export const HeliShopLogo: React.FC<HeliShopLogoProps> = ({
  size = "md",
  variant = "on-dark",
  showBadge = true,
  badgeText = "Mall",
  className = "",
}) => {
  const iconSizes = {
    sm: "w-8 h-8 rounded-xl",
    md: "w-11 h-11 rounded-2xl",
    lg: "w-14 h-14 rounded-3xl",
  };

  const textSizes = {
    sm: "text-xl",
    md: "text-2xl sm:text-3xl",
    lg: "text-3xl sm:text-4xl",
  };

  const textColor = variant === "on-dark" ? "text-white" : "text-slate-900";
  const accentColor = variant === "on-dark" ? "text-sky-200" : "text-[#0284C7]";
  const badgeStyle =
    variant === "on-dark"
      ? "bg-sky-400/20 text-sky-100 border border-sky-300/40 shadow-sm"
      : "bg-sky-50 text-[#0284C7] border border-sky-200 font-bold";

  return (
    <Link to="/" className={`flex items-center gap-2.5 shrink-0 group select-none ${className}`}>
      {/* 3D HeliShop Emblem Image */}
      <div
        className={`${iconSizes[size]} bg-white p-1 flex items-center justify-center shadow-lg shadow-sky-950/20 ring-2 ring-white/30 group-hover:scale-105 group-hover:rotate-1 transition-all duration-300 overflow-hidden shrink-0`}
      >
        <img
          src="/images/logo.png"
          alt="HeliShop Logo"
          className="w-full h-full object-cover rounded-xl"
        />
      </div>

      {/* Brand Typography & Badge */}
      <div className="flex items-center gap-1.5">
        <span className={`${textSizes[size]} font-black tracking-tight ${textColor} drop-shadow-sm transition-colors`}>
          Heli<span className={accentColor}>Shop</span>
        </span>
        {showBadge && (
          <span
            className={`text-[10px] font-extrabold px-1.5 py-0.5 rounded uppercase tracking-wider ${badgeStyle}`}
          >
            {badgeText}
          </span>
        )}
      </div>
    </Link>
  );
};
