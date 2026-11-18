import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { useCart } from '../../../contexts/CartContext';
import { resendVerification } from '../../../api/authApi';
import { LEARNING_ROUTES } from '../routes';
import {
  BookOpen,
  Library,
  ShoppingBag,
  ShoppingCart,
  User,
  LogOut,
  AlertCircle,
  Mail,
  Loader2,
  Compass,
  ChevronDown,
  BarChart3,
  Award,
  Clock,
  Sparkles,
  History,
} from 'lucide-react';

export function StudentLayout() {
  const { user, isAuthenticated, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const location = useLocation();

  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [blockedPrompt, setBlockedPrompt] = useState<string | null>(null);
  const [learningMenuOpen, setLearningMenuOpen] = useState(false);

  // Global listener for EMAIL_NOT_VERIFIED events from apiFetch
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
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Navbar */}
      <header className="border-b border-border bg-card px-4 sm:px-6 py-3.5 sticky top-0 z-30 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-3">
            <div
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold shadow-xs"
              aria-hidden
            >
              AL
            </div>
            <span className="font-semibold text-foreground text-sm tracking-tight hidden sm:inline">
              AL Learning Platform
            </span>
          </Link>
        </div>

        {/* Navigation links */}
        <nav className="flex items-center gap-2 sm:gap-3">
          <Link
            to="/subjects"
            className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 sm:px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <BookOpen size={13} />
            <span className="hidden md:inline">Subjects</span>
          </Link>

          <Link
            to="/products"
            className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 sm:px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <ShoppingBag size={13} />
            <span className="hidden md:inline">Notes &amp; Videos</span>
          </Link>

          {/* Learning Dropdown for Authenticated Students */}
          {isAuthenticated && (
            <div className="relative">
              <button
                type="button"
                onClick={() => setLearningMenuOpen(prev => !prev)}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  location.pathname.startsWith('/learning') ||
                  location.pathname.startsWith('/quizzes') ||
                  location.pathname.startsWith('/attempts') ||
                  location.pathname.startsWith('/assessments')
                    ? 'bg-primary text-primary-foreground shadow-xs'
                    : 'bg-primary/10 text-primary border border-primary/20 hover:bg-primary/20'
                }`}
              >
                <Compass size={13} />
                <span>Learning</span>
                <ChevronDown size={12} className={`transition-transform ${learningMenuOpen ? 'rotate-180' : ''}`} />
              </button>

              {learningMenuOpen && (
                <div
                  onClick={() => setLearningMenuOpen(false)}
                  className="absolute left-0 sm:right-0 sm:left-auto mt-2 w-56 rounded-2xl border border-border bg-card p-2 shadow-xl z-40 animate-in fade-in zoom-in-95 duration-100"
                  role="menu"
                >
                  <Link
                    to={LEARNING_ROUTES.OVERVIEW}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <Compass size={14} className="text-primary" />
                    <span>Learning Overview</span>
                  </Link>

                  <Link
                    to={LEARNING_ROUTES.PROGRESS}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <BarChart3 size={14} className="text-primary" />
                    <span>Syllabus Progress</span>
                  </Link>

                  <Link
                    to={LEARNING_ROUTES.MISTAKES}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <Award size={14} className="text-primary" />
                    <span>My Mistakes</span>
                  </Link>

                  <Link
                    to={LEARNING_ROUTES.STUDY_SESSION_START}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <Clock size={14} className="text-primary" />
                    <span>Focused Study Session</span>
                  </Link>

                  <div className="my-1 border-t border-border" />

                  <Link
                    to={LEARNING_ROUTES.OVERALL_QUIZ_START}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <Sparkles size={14} className="text-primary" />
                    <span>Take Overall Quiz</span>
                  </Link>

                  <Link
                    to={LEARNING_ROUTES.DIAGNOSTIC_START}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <Compass size={14} className="text-primary" />
                    <span>Diagnostic Assessment</span>
                  </Link>

                  <Link
                    to={LEARNING_ROUTES.ASSESSMENTS_HISTORY}
                    className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground hover:bg-muted rounded-xl transition"
                    role="menuitem"
                  >
                    <History size={14} className="text-primary" />
                    <span>Assessment History</span>
                  </Link>
                </div>
              )}
            </div>
          )}

          {isAuthenticated ? (
            <>
              <Link
                to="/my-videos"
                className="hidden lg:flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                <Library size={13} />
                <span>My Videos</span>
              </Link>

              <Link
                to="/cart"
                className="relative flex items-center gap-1.5 rounded-lg border border-border px-2.5 sm:px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
                title="View cart"
              >
                <ShoppingCart size={13} />
                {itemCount > 0 && (
                  <span className="rounded-full bg-primary text-primary-foreground px-1.5 text-[10px] font-bold">
                    {itemCount}
                  </span>
                )}
              </Link>

              <Link
                to="/dashboard"
                className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 sm:px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                <User size={13} />
                <span className="hidden sm:inline">{user?.fullName?.split(' ')[0]}</span>
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition"
                title="Sign out"
              >
                <LogOut size={14} />
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="px-3 py-1.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs"
            >
              Sign In
            </Link>
          )}
        </nav>
      </header>

      {/* Unverified Email Warning Banner */}
      {isAuthenticated && !isVerified && (
        <div className="border-b border-amber-500/30 bg-amber-500/10 px-4 sm:px-6 py-2.5 text-xs text-amber-800 dark:text-amber-200">
          <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <AlertCircle size={15} className="text-amber-600 shrink-0" />
              <span>
                <strong>Email not verified:</strong> Please verify your email to unlock all assessment features.
              </span>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                disabled={resending}
                onClick={handleResend}
                className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white px-2.5 py-1 text-[11px] font-semibold transition disabled:opacity-50"
              >
                {resending ? <Loader2 size={12} className="animate-spin" /> : <Mail size={12} />}
                <span>Resend verification</span>
              </button>
              {resendStatus && (
                <span className={resendStatus.type === 'success' ? 'text-emerald-700 dark:text-emerald-300 font-medium' : 'text-rose-700 font-medium'}>
                  {resendStatus.message}
                </span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Action Blocked Alert */}
      {blockedPrompt && (
        <div className="max-w-3xl mx-auto mt-4 px-4 w-full">
          <div className="p-4 rounded-2xl border border-rose-500/30 bg-rose-500/10 text-rose-800 dark:text-rose-200 text-xs flex items-start gap-3">
            <AlertCircle size={18} className="text-rose-600 shrink-0 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-bold">Action Blocked: Email Verification Required</h4>
              <p className="mt-0.5 opacity-90">{blockedPrompt}</p>
              <div className="mt-2.5 flex items-center gap-3">
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={resending}
                  className="px-3 py-1 rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-semibold transition"
                >
                  Resend verification email
                </button>
                <button
                  type="button"
                  onClick={() => setBlockedPrompt(null)}
                  className="underline hover:no-underline font-medium"
                >
                  Dismiss
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 px-4 sm:px-6 py-6 max-w-6xl w-full mx-auto">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t border-border bg-card/60 px-6 py-6 text-center text-xs text-muted-foreground mt-auto">
        <p>&copy; {new Date().getFullYear()} AL Learning Platform. Built for Advanced Level students.</p>
      </footer>
    </div>
  );
}
