import type React from 'react';
import { CheckCircle2, AlertCircle, X } from 'lucide-react';

interface FeedbackAlertProps {
  type: 'success' | 'error' | 'info';
  message: React.ReactNode;
  onClose?: () => void;
  className?: string;
}

export function FeedbackAlert({ type, message, onClose, className = '' }: FeedbackAlertProps) {
  if (!message) return null;

  let style = 'bg-muted/80 text-foreground border-border';
  let Icon = AlertCircle;

  if (type === 'success') {
    style = 'bg-emerald-500/10 text-emerald-800 dark:text-emerald-200 border-emerald-500/20';
    Icon = CheckCircle2;
  } else if (type === 'error') {
    style = 'bg-rose-500/10 text-rose-800 dark:text-rose-200 border-rose-500/20';
    Icon = AlertCircle;
  }

  return (
    <div className={`rounded-xl border p-3.5 text-xs flex items-start gap-2.5 ${style} ${className}`}>
      <Icon size={16} className="shrink-0 mt-0.5" />
      <div className="flex-1 leading-relaxed">{message}</div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="text-current opacity-60 hover:opacity-100 transition p-0.5"
          title="Dismiss"
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}
