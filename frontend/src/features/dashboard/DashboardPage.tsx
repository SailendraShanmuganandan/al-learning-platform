import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { resendVerification } from '../../api/authApi';
import { LogOut, User, Mail, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

/**
 * Dashboard page demonstrating the protected-route pattern and
 * unverified-email status & resend verification action.
 */
export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [blockedPrompt, setBlockedPrompt] = useState<string | null>(null);

  // Global listener for EMAIL_NOT_VERIFIED errors from apiFetch across any future endpoint
  useEffect(() => {
    function handleEmailNotVerified(event: Event) {
      const customEvent = event as CustomEvent<{ message?: string }>;
      setBlockedPrompt(
        customEvent.detail?.message ||
          'Your action was blocked because your email address has not been verified.'
      );
    }

    window.addEventListener('al:email-not-verified', handleEmailNotVerified);
    return () => {
      window.removeEventListener('al:email-not-verified', handleEmailNotVerified);
    };
  }, []);

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  async function handleResend() {
    setResending(true);
    setResendStatus(null);
    try {
      const res = await resendVerification();
      setResendStatus({
        type: 'success',
        message: res.message || 'Verification email has been sent! Check your inbox.',
      });
    } catch {
      setResendStatus({
        type: 'error',
        message: 'Failed to send verification email. Please try again later.',
      });
    } finally {
      setResending(false);
    }
  }

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  return (
    <div className="min-h-screen bg-background">
      {/* Top-bar */}
      <header className="border-b border-border bg-card px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold"
            aria-hidden
          >
            AL
          </div>
          <span className="font-semibold text-foreground">AL Learning Platform</span>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <User size={15} />
            <span id="dashboard-user-name">{user?.fullName}</span>
          </div>
          <button
            id="dashboard-logout-btn"
            type="button"
            onClick={handleLogout}
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <LogOut size={13} />
            Sign out
          </button>
        </div>
      </header>

      {/* Verification Reminder Banner if Unverified */}
      {!isVerified && (
        <div
          id="unverified-email-banner"
          className="border-b border-amber-500/30 bg-amber-500/10 px-6 py-3 text-sm text-amber-800 dark:text-amber-200"
        >
          <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <AlertCircle size={18} className="text-amber-600 shrink-0" />
              <span>
                <strong>Your email is not verified.</strong> Please verify your email to unlock all features (purchases, registrations, quizzes).
              </span>
            </div>
            <button
              id="resend-verification-btn"
              type="button"
              disabled={resending}
              onClick={handleResend}
              className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 text-xs font-semibold shadow-sm transition disabled:opacity-50 shrink-0"
            >
              {resending ? (
                <>
                  <Loader2 size={13} className="animate-spin" />
                  Sending…
                </>
              ) : (
                <>
                  <Mail size={13} />
                  Resend verification email
                </>
              )}
            </button>
          </div>
          {resendStatus && (
            <div className="max-w-4xl mx-auto mt-2 text-xs font-medium">
              <span className={resendStatus.type === 'success' ? 'text-green-700 dark:text-green-300' : 'text-destructive'}>
                {resendStatus.message}
              </span>
            </div>
          )}
        </div>
      )}

      {/* EMAIL_NOT_VERIFIED Interceptor Prompt Modal / Banner */}
      {blockedPrompt && (
        <div
          id="email-blocked-alert"
          role="alert"
          className="mx-auto max-w-2xl mt-6 p-4 rounded-xl border border-destructive/40 bg-destructive/10 text-destructive text-sm flex items-start gap-3"
        >
          <AlertCircle size={20} className="shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="font-semibold">Action Blocked: Email Verification Required</h3>
            <p className="mt-1 text-xs opacity-90">{blockedPrompt}</p>
            <div className="mt-3 flex items-center gap-3">
              <button
                type="button"
                onClick={handleResend}
                disabled={resending}
                className="rounded-lg bg-destructive text-destructive-foreground px-3 py-1.5 text-xs font-medium hover:opacity-90 transition"
              >
                {resending ? 'Sending link…' : 'Resend Verification Link'}
              </button>
              <button
                type="button"
                onClick={() => setBlockedPrompt(null)}
                className="text-xs underline hover:no-underline"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Body */}
      <main className="flex flex-col items-center justify-center min-h-[calc(100vh-120px)] px-6 py-8">
        <div className="max-w-lg w-full text-center flex flex-col gap-4">
          <div
            className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-primary/10 text-primary text-3xl font-bold"
            aria-hidden
          >
            👋
          </div>
          <h1 className="text-3xl font-bold text-foreground">
            Welcome back, {user?.fullName?.split(' ')[0]}!
          </h1>
          <p className="text-muted-foreground">
            You&apos;re successfully authenticated. Your dashboard is coming soon.
          </p>

          {/* User info card */}
          <div className="mt-4 rounded-2xl border border-border bg-card p-6 text-left space-y-3">
            <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
              Session Details
            </h2>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between gap-2">
                <dt className="text-muted-foreground">Name</dt>
                <dd id="dashboard-detail-name" className="font-medium text-foreground">{user?.fullName}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-muted-foreground">Email</dt>
                <dd id="dashboard-detail-email" className="font-medium text-foreground">{user?.email}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-muted-foreground">Role</dt>
                <dd id="dashboard-detail-role" className="font-medium text-foreground">{user?.role}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-muted-foreground">Status</dt>
                <dd id="dashboard-detail-status" className="font-medium text-green-600">{user?.accountStatus}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-muted-foreground">Email Verified</dt>
                <dd id="dashboard-detail-verified" className="font-medium">
                  {isVerified ? (
                    <span className="inline-flex items-center gap-1 text-green-600 font-semibold">
                      <CheckCircle2 size={14} /> Verified
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-amber-600 font-semibold">
                      <AlertCircle size={14} /> Not Verified
                    </span>
                  )}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </main>
    </div>
  );
}
