import type React from 'react';
import type { PerformanceLevel } from '../types';
import { CheckCircle2, TrendingUp, AlertTriangle, HelpCircle, MinusCircle } from 'lucide-react';

interface LevelBadgeProps {
  level: PerformanceLevel | 'NOT_ENOUGH_DATA' | 'NO_QUESTIONS' | string | null | undefined;
  size?: 'sm' | 'md';
  className?: string;
}

export function LevelBadge({ level, size = 'sm', className = '' }: LevelBadgeProps) {
  const norm = (level || 'NOT_ENOUGH_DATA').toUpperCase();

  let style: string;
  let text: string;
  let Icon: React.ElementType;

  switch (norm) {
    case 'STRONG':
      style = 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20';
      text = 'Strong';
      Icon = CheckCircle2;
      break;

    case 'DEVELOPING':
      style = 'bg-sky-500/10 text-sky-700 dark:text-sky-400 border-sky-500/20';
      text = 'Developing';
      Icon = TrendingUp;
      break;

    case 'WEAK':
      style = 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20';
      text = 'Needs Practice';
      Icon = AlertTriangle;
      break;

    case 'NO_QUESTIONS':
      style = 'bg-muted/80 text-muted-foreground border-border/80';
      text = 'No quiz yet';
      Icon = MinusCircle;
      break;

    case 'INSUFFICIENT_DATA':
    case 'NOT_ENOUGH_DATA':
    default:
      // Neutral wording & neutral gray/zinc styling - NEVER styled or worded like WEAK
      style = 'bg-zinc-500/10 text-zinc-600 dark:text-zinc-400 border-zinc-500/20';
      text = 'Not enough data yet';
      Icon = HelpCircle;
      break;
  }

  const pad = size === 'sm' ? 'px-2 py-0.5 text-[11px] gap-1' : 'px-2.5 py-1 text-xs gap-1.5';
  const iconSize = size === 'sm' ? 12 : 14;

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full border shadow-2xs whitespace-nowrap ${pad} ${style} ${className}`}
      title={text}
    >
      <Icon size={iconSize} aria-hidden />
      <span>{text}</span>
    </span>
  );
}

export const ProgressStateBadge = LevelBadge;
