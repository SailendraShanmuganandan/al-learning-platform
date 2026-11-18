import { useState } from 'react';
import { resendVerification } from '../../../api/authApi';
import { AlertCircle, Mail, Loader2, CheckCircle2 } from 'lucide-react';

interface UnverifiedEmailPromptProps {
  message?: string;
  className?: string;
}

export function UnverifiedEmailPrompt({
  message = 'Email verification is required to take diagnostic assessments and track official records.',
  className = '',
}: UnverifiedEmailPromptProps) {
  const [resending, setResending] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  async function handleResend() {
    setResending(true);
    setResult(null);
    try {
      const res = await resendVerification();
      setResult({
        type: 'success',
        text: res.message || 'Verification link sent! Please check your email.',
      });
    } catch {
      setResult({
        type: 'error',
        text: 'Failed to send verification email. Please try again.',
      });
    } finally {
      setResending(false);
    }
  }

  return (
    <div
      className={`rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-xs text-amber-800 dark:text-amber-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${className}`}
    >
      <div className="flex items-start sm:items-center gap-2.5">
        <AlertCircle size={18} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5 sm:mt-0" />
        <div>
          <span className="font-semibold text-foreground">Verify your email address: </span>
          <span>{message}</span>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2 shrink-0">
        <button
          type="button"
          disabled={resending}
          onClick={handleResend}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-semibold transition disabled:opacity-50 shadow-xs"
        >
          {resending ? (
            <>
              <Loader2 size={13} className="animate-spin" />
              <span>Sending…</span>
            </>
          ) : (
            <>
              <Mail size={13} />
              <span>Resend Verification Email</span>
            </>
          )}
        </button>

        {result && (
          <span
            className={`font-medium text-[11px] flex items-center gap-1 ${
              result.type === 'success' ? 'text-emerald-700 dark:text-emerald-300' : 'text-rose-700 dark:text-rose-300'
            }`}
          >
            {result.type === 'success' && <CheckCircle2 size={12} />}
            {result.text}
          </span>
        )}
      </div>
    </div>
  );
}

