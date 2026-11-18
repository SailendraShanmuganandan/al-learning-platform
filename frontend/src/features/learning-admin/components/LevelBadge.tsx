import type { PerformanceLevel } from '../types';

interface LevelBadgeProps {
  level: PerformanceLevel | string | null | undefined;
  size?: 'sm' | 'md';
}

export function LevelBadge({ level, size = 'sm' }: LevelBadgeProps) {
  const l = level ? String(level).toUpperCase() : 'INSUFFICIENT_DATA';
  const pad = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  if (l === 'INSUFFICIENT_DATA' || !level) {
    return (
      <span
        title="Student has fewer than 5 graded answers in this chapter"
        className={`inline-flex items-center gap-1 font-medium rounded-full border bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 ${pad}`}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-zinc-400" />
        <span>Not enough data</span>
      </span>
    );
  }

  if (l === 'WEAK') {
    return (
      <span
        className={`inline-flex items-center gap-1 font-semibold rounded-full border bg-rose-500/10 text-rose-700 dark:text-rose-300 border-rose-500/20 ${pad}`}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-rose-500" />
        <span>Weak</span>
      </span>
    );
  }

  if (l === 'DEVELOPING') {
    return (
      <span
        className={`inline-flex items-center gap-1 font-semibold rounded-full border bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-500/20 ${pad}`}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
        <span>Developing</span>
      </span>
    );
  }

  if (l === 'STRONG') {
    return (
      <span
        className={`inline-flex items-center gap-1 font-semibold rounded-full border bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/20 ${pad}`}
      >
        <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
        <span>Strong</span>
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center gap-1 font-medium rounded-full border bg-muted text-muted-foreground border-border ${pad}`}>
      {level}
    </span>
  );
}
