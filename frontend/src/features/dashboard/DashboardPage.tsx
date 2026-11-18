import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { resendVerification } from '../../api/authApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  Mail,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ArrowRight,
} from 'lucide-react';

/**
 * Student Dashboard page
 */
export default function DashboardPage() {
  const { user } = useAuth();

  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [blockedPrompt, setBlockedPrompt] = useState<string | null>(null);

  // Global listener for EMAIL_NOT_VERIFIED errors from apiFetch
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

  async function handleResend() {
    setResending(true);
    setResendStatus(null);
    try {
      const res = await resendVerification();
      setResendStatus({
        type: 'success',
        message: res.message || 'Verification link sent to your email!',
      });
    } catch {
      setResendStatus({
        type: 'error',
        message: 'Failed to send verification email. Please try again.',
      });
    } finally {
      setResending(false);
    }
  }

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  return (
    <PageContainer>
      <div className="space-y-8">
        <PageHeader
          title={`Welcome back, ${user?.fullName?.split(' ')[0] || 'Student'}!`}
          description="Your personal A/L ICT learning and revision dashboard."
        />

        {/* Verification Reminder Banner if Unverified */}
        {!isVerified && (
          <div
            id="unverified-email-banner"
            className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-xs text-amber-800 dark:text-amber-200"
          >
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
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
                className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 text-xs font-semibold shadow-xs transition disabled:opacity-50 shrink-0"
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
              <div className="mt-2 text-xs font-medium">
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
            className="p-4 rounded-2xl border border-destructive/40 bg-destructive/10 text-destructive text-xs flex items-start gap-3"
          >
            <AlertCircle size={20} className="shrink-0 mt-0.5" />
            <div className="flex-1">
              <h3 className="font-semibold text-sm">Action Blocked: Email Verification Required</h3>
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

        {/* Learning Hub Quick Banner */}
        {user?.role === 'STUDENT' && (
          <div className="rounded-3xl border border-primary/20 bg-gradient-to-br from-card via-card to-primary/5 p-6 text-left shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <span className="text-[10px] font-bold text-primary uppercase tracking-wide">
                Learning Hub
              </span>
              <h2 className="text-base font-bold text-foreground">
                A/L ICT Practice &amp; Assessments
              </h2>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Master chapter quizzes, practise your mistakes, and map your syllabus progress.
              </p>
            </div>
            <Link
              to="/learning-overview"
              className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 shrink-0 self-start sm:self-auto"
            >
              <span>Open Learning Hub</span>
              <ArrowRight size={13} />
            </Link>
          </div>
        )}

        {/* User Info & Session Details */}
        <div className="rounded-2xl border border-border bg-card p-6 text-left space-y-4">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            Account Details
          </h2>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="flex justify-between sm:justify-start sm:gap-4 p-3 rounded-xl bg-muted/30 border border-border/40">
              <dt className="text-muted-foreground">Name</dt>
              <dd id="dashboard-detail-name" className="font-semibold text-foreground">{user?.fullName}</dd>
            </div>
            <div className="flex justify-between sm:justify-start sm:gap-4 p-3 rounded-xl bg-muted/30 border border-border/40">
              <dt className="text-muted-foreground">Email</dt>
              <dd id="dashboard-detail-email" className="font-semibold text-foreground">{user?.email}</dd>
            </div>
            <div className="flex justify-between sm:justify-start sm:gap-4 p-3 rounded-xl bg-muted/30 border border-border/40">
              <dt className="text-muted-foreground">Role</dt>
              <dd id="dashboard-detail-role" className="font-semibold text-foreground">{user?.role}</dd>
            </div>
            <div className="flex justify-between sm:justify-start sm:gap-4 p-3 rounded-xl bg-muted/30 border border-border/40">
              <dt className="text-muted-foreground">Status</dt>
              <dd id="dashboard-detail-status" className="font-semibold text-green-600">{user?.accountStatus}</dd>
            </div>
            <div className="sm:col-span-2 flex justify-between sm:justify-start sm:gap-4 p-3 rounded-xl bg-muted/30 border border-border/40">
              <dt className="text-muted-foreground">Email Verification</dt>
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
    </PageContainer>
  );
}
