import type { QuestionDifficulty } from '../types';

interface DifficultyBadgeProps {
  difficulty: QuestionDifficulty | string;
  size?: 'sm' | 'md';
}

export function DifficultyBadge({ difficulty, size = 'sm' }: DifficultyBadgeProps) {
  const d = String(difficulty).toUpperCase();
  const pad = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  let style = 'bg-muted text-muted-foreground border-border';
  switch (d) {
    case 'EASY':
      style = 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/20';
      break;
    case 'MEDIUM':
      style = 'bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-500/20';
      break;
    case 'HARD':
      style = 'bg-rose-500/10 text-rose-700 dark:text-rose-300 border-rose-500/20';
      break;
  }

  return (
    <span className={`inline-flex items-center font-semibold rounded-full border ${pad} ${style}`}>
      {d}
    </span>
  );
}

