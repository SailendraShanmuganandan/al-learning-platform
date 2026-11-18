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
    <div className="text-center">
      <div className="mb-6">
        <h2 className="text-xl font-bold tracking-tight text-foreground">Account Email Verification</h2>
        <p className="text-xs text-muted-foreground mt-1">Validating your email address</p>
      </div>

      {status === 'loading' && (
        <div className="flex flex-col items-center py-6 gap-4">
          <Loader2 className="h-12 w-12 animate-spin text-primary" />
          <h3 className="text-base font-semibold text-foreground">
            Verifying your email...
          </h3>
          <p className="text-xs text-muted-foreground">
            Please wait while we validate your verification token.
          </p>
        </div>
      )}

      {status === 'success' && (
        <div className="flex flex-col items-center py-4 gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 text-green-600">
            <CheckCircle2 className="h-10 w-10" />
          </div>
          <h3 className="text-xl font-bold text-foreground">
            Email Verified!
          </h3>
          <p className="text-xs text-muted-foreground">{message}</p>
          <div className="mt-4 w-full">
            <Link
              to="/login"
              id="verify-success-login-btn"
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground shadow transition hover:opacity-90"
            >
              Proceed to Login
              <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      )}

      {status === 'error' && (
        <div className="flex flex-col items-center py-4 gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-amber-500/10 text-amber-600">
            <AlertTriangle className="h-10 w-10" />
          </div>
          <h3 className="text-xl font-bold text-foreground">
            Verification Failed
          </h3>
          <p className="text-xs text-muted-foreground">{message}</p>
          <div className="mt-4 flex flex-col gap-2 w-full">
            <Link
              to="/login"
              id="verify-error-login-btn"
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-semibold text-primary-foreground shadow transition hover:opacity-90"
            >
              Go to Login
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
