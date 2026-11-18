import type { QuestionStatus, QuizStatus, QuestionReportStatus, TopicStatus } from '../types';

interface StatusBadgeProps {
  status: QuestionStatus | QuizStatus | QuestionReportStatus | TopicStatus | string;
  size?: 'sm' | 'md';
}

export function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  const s = String(status).toUpperCase();
  const pad = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  let style = 'bg-muted text-muted-foreground border-border';
  const label = s;

  switch (s) {
    case 'APPROVED':
    case 'PUBLISHED':
    case 'ACTIVE':
    case 'RESOLVED':
      style = 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/20';
      break;
    case 'PENDING':
    case 'DRAFT':
    case 'OPEN':
      style = 'bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-500/20';
      break;
    case 'ARCHIVED':
    case 'INACTIVE':
    case 'DISMISSED':
      style = 'bg-zinc-500/10 text-zinc-600 dark:text-zinc-400 border-zinc-500/20';
      break;
  }

  return (
    <span
      className={`inline-flex items-center gap-1 font-semibold rounded-full border ${style} ${pad} uppercase tracking-wider`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
      <span>{label}</span>
    </span>
  );
}
