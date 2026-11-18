import type { PerformanceTrend } from '../types';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface TrendIconProps {
  trend?: PerformanceTrend | null;
  size?: number;
  className?: string;
  showText?: boolean;
}

export function TrendIcon({
  trend,
  size = 14,
  className = '',
  showText = false,
}: TrendIconProps) {
  if (!trend) return null;

  if (trend === 'IMPROVING') {
    return (
      <span
        className={`inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-semibold text-xs ${className}`}
        title="Improving performance trend"
        aria-label="Improving trend"
      >
        <TrendingUp size={size} aria-hidden />
        {showText && <span>Improving</span>}
      </span>
    );
  }

  if (trend === 'DECLINING') {
    return (
      <span
        className={`inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 font-semibold text-xs ${className}`}
        title="Declining performance trend"
        aria-label="Declining trend"
      >
        <TrendingDown size={size} aria-hidden />
        {showText && <span>Declining</span>}
      </span>
    );
  }

  return (
    <span
      className={`inline-flex items-center gap-1 text-muted-foreground font-semibold text-xs ${className}`}
      title="Steady performance trend"
      aria-label="Steady trend"
    >
      <Minus size={size} aria-hidden />
      {showText && <span>Steady</span>}
    </span>
  );
}

