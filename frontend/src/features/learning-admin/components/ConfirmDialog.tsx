import type React from 'react';
import { AlertTriangle, AlertCircle, Loader2 } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  description: React.ReactNode;
  warning?: React.ReactNode;
  confirmLabel?: string;
  confirmVariant?: 'primary' | 'destructive' | 'amber' | 'emerald';
  isLoading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  isOpen,
  title,
  description,
  warning,
  confirmLabel = 'Confirm',
  confirmVariant = 'primary',
  isLoading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!isOpen) return null;

  let btnColor = 'bg-primary text-primary-foreground hover:bg-primary/90';
  let iconColor = 'bg-primary/10 text-primary';

  switch (confirmVariant) {
    case 'destructive':
      btnColor = 'bg-destructive text-destructive-foreground hover:bg-destructive/90';
      iconColor = 'bg-destructive/10 text-destructive';
      break;
    case 'amber':
      btnColor = 'bg-amber-600 text-white hover:bg-amber-700';
      iconColor = 'bg-amber-500/10 text-amber-600';
      break;
    case 'emerald':
      btnColor = 'bg-emerald-600 text-white hover:bg-emerald-700';
      iconColor = 'bg-emerald-500/10 text-emerald-600';
      break;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-150">
        <div className={`flex h-12 w-12 items-center justify-center rounded-2xl mb-4 mx-auto ${iconColor}`}>
          <AlertTriangle size={24} />
        </div>

        <h3 className="text-base font-bold text-center text-foreground">
          {title}
        </h3>

        <div className="text-xs text-center text-muted-foreground mt-2 leading-relaxed">
          {description}
        </div>

        {warning && (
          <div className="mt-4 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-900 dark:text-amber-200 flex items-start gap-2">
            <AlertCircle size={16} className="shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
            <div className="leading-relaxed">{warning}</div>
          </div>
        )}

        <div className="mt-6 flex flex-col gap-2">
          <button
            type="button"
            disabled={isLoading}
            onClick={onConfirm}
            className={`w-full py-2.5 px-4 rounded-xl text-xs font-semibold disabled:opacity-50 transition flex items-center justify-center gap-1.5 shadow-xs ${btnColor}`}
          >
            {isLoading ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                <span>Processing…</span>
              </>
            ) : (
              <span>{confirmLabel}</span>
            )}
          </button>
          <button
            type="button"
            disabled={isLoading}
            onClick={onCancel}
            className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
