import type { BulkStatusResponse } from '../types';
import { CheckCircle2, AlertCircle, X } from 'lucide-react';

interface BulkResultDialogProps {
  isOpen: boolean;
  result: BulkStatusResponse | null;
  onClose: () => void;
}

export function BulkResultDialog({ isOpen, result, onClose }: BulkResultDialogProps) {
  if (!isOpen || !result) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-card rounded-3xl border border-border p-6 max-w-lg w-full shadow-2xl animate-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-3 border-b border-border">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <CheckCircle2 size={18} />
            </div>
            <div>
              <h3 className="text-sm font-bold text-foreground">Bulk Status Result</h3>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition"
          >
            <X size={16} />
          </button>
        </div>

        <div className="my-5 grid grid-cols-2 gap-3">
          <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-3.5 text-center">
            <span className="text-2xl font-black text-emerald-700 dark:text-emerald-300">
              {result.updatedIds.length}
            </span>
            <p className="text-xs font-semibold text-emerald-800 dark:text-emerald-200 mt-0.5">Updated Successfully</p>
          </div>
          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-3.5 text-center">
            <span className="text-2xl font-black text-amber-700 dark:text-amber-300">
              {result.skipped.length}
            </span>
            <p className="text-xs font-semibold text-amber-800 dark:text-amber-200 mt-0.5">Skipped</p>
          </div>
        </div>

        {result.skipped.length > 0 && (
          <div className="mb-5 space-y-2">
            <h4 className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
              <AlertCircle size={14} className="text-amber-500" />
              <span>Skipped Questions Breakdown</span>
            </h4>
            <div className="max-h-48 overflow-y-auto rounded-xl border border-border bg-muted/30">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-border bg-muted/50 text-muted-foreground uppercase text-[10px] tracking-wider">
                  <tr>
                    <th className="py-2 px-3 font-semibold">Question ID</th>
                    <th className="py-2 px-3 font-semibold">Reason</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {result.skipped.map((sq) => (
                    <tr key={sq.id} className="hover:bg-muted/40">
                      <td className="py-2 px-3 font-mono text-[11px] font-semibold text-foreground">
                        #{sq.id}
                      </td>
                      <td className="py-2 px-3 text-muted-foreground leading-relaxed">
                        {sq.reason}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        <div className="pt-2">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-2.5 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
