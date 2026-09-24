import { useEffect, useState, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmail, type ApiError } from '../../api/authApi';
import { CheckCircle2, AlertTriangle, Loader2, ArrowRight } from 'lucide-react';

type Status = 'loading' | 'success' | 'error';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [status, setStatus] = useState<Status>('loading');
  const [message, setMessage] = useState<string>('');
  const requestedRef = useRef(false);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('No verification token was provided in the link. Please check your email for the complete link.');
      return;
    }

    if (requestedRef.current) return;
    requestedRef.current = true;

    verifyEmail(token)
      .then((res) => {
        setStatus('success');
        setMessage(res.message || 'Your email address has been successfully verified.');
      })
      .catch((err: ApiError) => {
        setStatus('error');
        setMessage(err.message || 'Verification link is invalid, expired, or has already been used.');
      });
  }, [token]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4">
      {/* Background decorative gradient */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 -z-10 overflow-hidden"
      >
        <div className="absolute -top-40 -right-40 h-[600px] w-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-[500px] w-[500px] rounded-full bg-primary/5 blur-3xl" />
      </div>

      <div className="w-full max-w-md">
        {/* Wordmark */}
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div
            className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground text-xl font-bold shadow-lg"
            aria-hidden
          >
            AL
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            AL Learning Platform
          </h1>
          <p className="text-sm text-muted-foreground">
            Account Email Verification
          </p>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-border bg-card p-8 shadow-sm text-center">
          {status === 'loading' && (
            <div className="flex flex-col items-center py-6 gap-4">
              <Loader2 className="h-12 w-12 animate-spin text-primary" />
              <h2 className="text-lg font-semibold text-foreground">
                Verifying your email...
              </h2>
              <p className="text-sm text-muted-foreground">
                Please wait while we validate your verification token.
              </p>
            </div>
          )}

          {status === 'success' && (
            <div className="flex flex-col items-center py-6 gap-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 text-green-600">
                <CheckCircle2 className="h-10 w-10" />
              </div>
              <h2 className="text-xl font-bold text-foreground">
                Email Verified!
              </h2>
              <p className="text-sm text-muted-foreground">{message}</p>
              <div className="mt-4 w-full">
                <Link
                  to="/login"
                  id="verify-success-login-btn"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90"
                >
                  Proceed to Login
                  <ArrowRight size={16} />
                </Link>
              </div>
            </div>
          )}

          {status === 'error' && (
            <div className="flex flex-col items-center py-6 gap-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-amber-500/10 text-amber-600">
                <AlertTriangle className="h-10 w-10" />
              </div>
              <h2 className="text-xl font-bold text-foreground">
                Verification Failed
              </h2>
              <p className="text-sm text-muted-foreground">{message}</p>
              <div className="mt-4 flex flex-col gap-2 w-full">
                <Link
                  to="/login"
                  id="verify-error-login-btn"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90"
                >
                  Go to Login
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
