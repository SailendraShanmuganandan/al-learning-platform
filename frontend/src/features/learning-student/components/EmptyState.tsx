import type React from 'react';
import { Link } from 'react-router-dom';

interface EmptyStateProps {
  icon: React.ElementType;
  title: string;
  description: string;
  actionText?: string;
  actionHref?: string;
  onActionClick?: () => void;
  className?: string;
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  actionText,
  actionHref,
  onActionClick,
  className = '',
}: EmptyStateProps) {
  return (
    <div className={`rounded-3xl border border-dashed border-border bg-card/50 p-8 text-center flex flex-col items-center justify-center ${className}`}>
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-3">
        <Icon size={24} aria-hidden />
      </div>
      <h3 className="text-sm font-bold text-foreground">{title}</h3>
      <p className="mt-1 text-xs text-muted-foreground max-w-sm leading-relaxed">
        {description}
      </p>

      {actionText && actionHref && (
        <Link
          to={actionHref}
          className="mt-4 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs"
        >
          {actionText}
        </Link>
      )}

      {actionText && onActionClick && !actionHref && (
        <button
          type="button"
          onClick={onActionClick}
          className="mt-4 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs"
        >
          {actionText}
        </button>
      )}
    </div>
  );
}

