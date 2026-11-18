import { useState } from 'react';
import type { QuestionReportReason } from '../types';
import { reportQuestion } from '../api';
import { mapStudentError } from '../errorMapper';
import { Flag, Loader2, X, CheckCircle2 } from 'lucide-react';

interface QuestionReportDialogProps {
  questionId: number;
  isOpen: boolean;
  onClose: () => void;
  onReportSuccess: (questionId: number) => void;
}

export function QuestionReportDialog({
  questionId,
  isOpen,
  onClose,
  onReportSuccess,
}: QuestionReportDialogProps) {
  const [reason, setReason] = useState<QuestionReportReason>('WRONG_ANSWER');
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await reportQuestion(questionId, {
        reason,
        comment: comment.trim() || undefined,
      });
      setSuccess(true);
      onReportSuccess(questionId);
      setTimeout(() => {
        onClose();
        setSuccess(false);
      }, 1200);
    } catch (err: unknown) {
      const mapped = mapStudentError(err);
      setError(mapped.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-3 border-b border-border">
          <div className="flex items-center gap-2 text-foreground font-bold text-sm">
            <Flag size={16} className="text-primary" />
            <span>Report Question #{questionId}</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition"
          >
            <X size={16} />
          </button>
        </div>

        {success ? (
          <div className="py-8 text-center space-y-2">
            <CheckCircle2 size={36} className="text-emerald-500 mx-auto" />
            <h4 className="text-sm font-bold text-foreground">Thank you for reporting</h4>
            <p className="text-xs text-muted-foreground">
              Our academic team will review this question promptly.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4 pt-4">
            {error && (
              <div className="p-3 rounded-xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
                {error}
              </div>
            )}

            <div>
              <label htmlFor="report-reason" className="block text-xs font-semibold text-foreground mb-1">
                Reason for report:
              </label>
              <select
                id="report-reason"
                value={reason}
                onChange={e => setReason(e.target.value as QuestionReportReason)}
                className="w-full rounded-xl border border-border bg-card px-3 py-2 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              >
                <option value="WRONG_ANSWER">Wrong Answer / Key Mismatch</option>
                <option value="UNCLEAR">Ambiguous or Unclear Question</option>
                <option value="OTHER">Typo or Other Issue</option>
              </select>
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <label htmlFor="report-comment" className="text-xs font-semibold text-foreground">
                  Additional Details (Optional):
                </label>
                <span className="text-[10px] text-muted-foreground">
                  {comment.length}/500
                </span>
              </div>
              <textarea
                id="report-comment"
                rows={3}
                maxLength={500}
                value={comment}
                onChange={e => setComment(e.target.value)}
                placeholder="Explain why this question or answer is problematic…"
                className="w-full rounded-xl border border-border bg-card p-3 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden resize-none"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <Loader2 size={13} className="animate-spin" />
                    <span>Submitting…</span>
                  </>
                ) : (
                  <span>Submit Report</span>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

