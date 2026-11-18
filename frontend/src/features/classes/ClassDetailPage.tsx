import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getPublicClassById,
  registerForClass,
  getMyClassRegistrations,
  type PublicClassResponse,
  type StudentRegistrationResponse,
  type ClassType,
} from '../../api/classApi';
import { resendVerification, type ApiError } from '../../api/authApi';
import {
  GraduationCap,
  Calendar,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Mail,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';

function formatLKR(fee: number): string {
  return `LKR ${Number(fee).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function getClassTypeBadge(type: ClassType) {
  switch (type) {
    case 'PHYSICAL':
      return {
        label: 'Physical Class',
        desc: 'Classroom learning with direct in-person teacher interaction.',
        style: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
      };
    case 'ONLINE':
      return {
        label: 'Online Class',
        desc: 'Live interactive classes delivered directly over Zoom/Web.',
        style: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20',
      };
    case 'HYBRID':
      return {
        label: 'Hybrid Class',
        desc: 'Flexible attendance supporting both physical classroom and online stream.',
        style: 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20',
      };
    default:
      return {
        label: type,
        desc: '',
        style: 'bg-muted text-muted-foreground border-border',
      };
  }
}

export default function ClassDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAuthenticated } = useAuth();

  const [cls, setCls] = useState<PublicClassResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [existingReg, setExistingReg] = useState<StudentRegistrationResponse | null>(null);
  const [registering, setRegistering] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [regError, setRegError] = useState<string | null>(null);

  // Email verification modal / banner
  const [showEmailUnverifiedModal, setShowEmailUnverifiedModal] = useState(false);
  const [resendingEmail, setResendingEmail] = useState(false);
  const [resendStatus, setResendStatus] = useState<string | null>(null);

  const classId = id ? parseInt(id, 10) : NaN;

  // Listen for global email-not-verified event
  useEffect(() => {
    function handleEmailNotVerified() {
      setShowEmailUnverifiedModal(true);
    }
    window.addEventListener('al:email-not-verified', handleEmailNotVerified);
    return () => {
      window.removeEventListener('al:email-not-verified', handleEmailNotVerified);
    };
  }, []);

  useEffect(() => {
    if (isNaN(classId)) {
      setError('Invalid class ID.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getPublicClassById(classId)
      .then((data) => {
        setCls(data);
      })
      .catch((err: unknown) => {
        const e = err as { message?: string; status?: number };
        if (e?.status === 404) {
          setError('Class not found or is currently not active.');
        } else {
          setError(e?.message || 'Failed to load class details.');
        }
      })
      .finally(() => {
        setLoading(false);
      });
  }, [classId]);

  // Check if student is already registered for this class
  useEffect(() => {
    if (isAuthenticated && user?.role === 'STUDENT' && !isNaN(classId)) {
      getMyClassRegistrations()
        .then((regs) => {
          const match = regs.find(
            (r) => r.classId === classId && (r.status === 'PENDING' || r.status === 'CONFIRMED')
          );
          if (match) {
            setExistingReg(match);
          }
        })
        .catch(() => {});
    }
  }, [isAuthenticated, user?.role, classId]);

  async function handleRegisterClick() {
    if (!isAuthenticated) {
      // Guest: send to /login and return afterward
      navigate('/login', { state: { from: location } });
      return;
    }

    if (user?.role !== 'STUDENT') {
      setRegError('Only students can register for classes.');
      return;
    }

    setRegistering(true);
    setRegError(null);
    setSuccessMsg(null);

    try {
      const res = await registerForClass(classId);
      setSuccessMsg('Your registration is pending confirmation.');
      setExistingReg({
        registrationId: res.registrationId,
        classId: res.classId,
        className: res.className,
        subjectName: cls?.subject.name || '',
        scheduleDescription: cls?.scheduleDescription || '',
        fee: cls?.fee || 0,
        status: 'PENDING',
        registeredAt: res.registeredAt,
        updatedAt: res.registeredAt,
      });
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      if (apiErr?.error === 'EMAIL_NOT_VERIFIED' || apiErr?.status === 403) {
        setShowEmailUnverifiedModal(true);
      } else if (apiErr?.error === 'ALREADY_REGISTERED' || apiErr?.status === 409) {
        setRegError(apiErr.message || 'You have already registered for this class.');
        // Refresh registrations
        getMyClassRegistrations().then((regs) => {
          const match = regs.find((r) => r.classId === classId);
          if (match) setExistingReg(match);
        }).catch(() => {});
      } else {
        setRegError(apiErr?.message || 'Failed to register for class. Please try again.');
      }
    } finally {
      setRegistering(false);
    }
  }

  async function handleResendVerification() {
    setResendingEmail(true);
    setResendStatus(null);
    try {
      const res = await resendVerification();
      setResendStatus(res.message || 'Verification email sent! Please check your inbox.');
    } catch (err: unknown) {
      const e = err as { message?: string };
      setResendStatus(e?.message || 'Failed to resend verification email.');
    } finally {
      setResendingEmail(false);
    }
  }

  return (
    <PageContainer>
      <Link
        to="/classes"
        className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground mb-6 transition"
      >
        <ArrowLeft size={14} />
        Back to class listings
      </Link>

      {/* Loading */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <Loader2 size={32} className="animate-spin text-primary mb-3" />
          <p className="text-sm">Loading class details...</p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="flex items-center gap-3 p-5 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-sm my-8">
          <AlertCircle size={20} className="shrink-0" />
          <div>
            <p className="font-semibold">{error}</p>
            <Link to="/classes" className="text-xs underline mt-1 inline-block">
              View all available classes
            </Link>
          </div>
        </div>
      )}

      {/* Class Details Card */}
      {!loading && !error && cls && (
        <div className="bg-card rounded-3xl border border-border p-8 shadow-sm">
          {/* Subject and Type */}
          <div className="flex items-center justify-between gap-4 mb-4 flex-wrap">
            <span className="text-xs font-bold uppercase tracking-wider text-primary bg-primary/10 px-3 py-1 rounded-full border border-primary/20">
              {cls.subject.name}
            </span>

            {(() => {
              const badge = getClassTypeBadge(cls.classType);
              return (
                <span
                  className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold border ${badge.style}`}
                >
                  {badge.label}
                </span>
              );
            })()}
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground mb-4 tracking-tight">
            {cls.name}
          </h1>

          {cls.description && (
            <p className="text-sm text-muted-foreground leading-relaxed mb-6 whitespace-pre-line">
              {cls.description}
            </p>
          )}

          {/* Schedule & Fee Overview */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-5 rounded-2xl bg-background border border-border mb-8">
            <div className="flex items-start gap-3">
              <div className="h-9 w-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                <Calendar size={18} />
              </div>
              <div>
                <span className="text-[11px] uppercase font-semibold text-muted-foreground tracking-wider block">
                  Class Schedule
                </span>
                <span className="text-sm font-semibold text-foreground mt-0.5 block">
                  {cls.scheduleDescription}
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="h-9 w-9 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                <GraduationCap size={18} />
              </div>
              <div>
                <span className="text-[11px] uppercase font-semibold text-muted-foreground tracking-wider block">
                  Monthly Tuition Fee
                </span>
                <span className="text-sm font-bold text-foreground mt-0.5 block">
                  {formatLKR(cls.fee)}
                </span>
                <span className="text-[10px] text-muted-foreground">
                  *Informational fee. Class fee payment is settled offline/direct with teacher.
                </span>
              </div>
            </div>
          </div>

          {/* Success feedback */}
          {successMsg && (
            <div className="flex items-start gap-3 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-300 text-sm mb-6">
              <CheckCircle2 size={18} className="shrink-0 mt-0.5 text-emerald-600" />
              <div>
                <p className="font-semibold">{successMsg}</p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Your class instructor will review and confirm your registration soon. You can view progress in{' '}
                  <Link to="/my/classes" className="underline font-semibold text-foreground">
                    My Classes
                  </Link>
                  .
                </p>
              </div>
            </div>
          )}

          {/* Registration error feedback */}
          {regError && (
            <div className="flex items-center gap-3 p-4 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-sm mb-6">
              <AlertCircle size={18} className="shrink-0" />
              <p>{regError}</p>
            </div>
          )}

          {/* Registration CTA / Status */}
          <div className="pt-6 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <span className="text-xs text-muted-foreground block">Registration Status</span>
              {existingReg ? (
                <div className="flex items-center gap-2 mt-1">
                  <span
                    id="class-registration-status-badge"
                    className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${
                      existingReg.status === 'CONFIRMED'
                        ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                        : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20'
                    }`}
                  >
                    <span className="h-1.5 w-1.5 rounded-full bg-current" />
                    Registration: {existingReg.status}
                  </span>
                  <Link
                    to="/my/classes"
                    className="text-xs font-medium text-primary hover:underline ml-1"
                  >
                    View in My Classes &rarr;
                  </Link>
                </div>
              ) : (
                <p className="text-xs font-medium text-foreground mt-1">
                  Seats available. Register now to book your spot.
                </p>
              )}
            </div>

            {!existingReg && (
              <button
                type="button"
                id="btn-register-class"
                disabled={registering}
                onClick={handleRegisterClick}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 rounded-2xl bg-primary text-primary-foreground font-semibold text-sm shadow-md hover:opacity-95 disabled:opacity-50 transition"
              >
                {registering ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    Registering...
                  </>
                ) : (
                  <>
                    <GraduationCap size={16} />
                    Register for Class
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Email Unverified Modal */}
      {showEmailUnverifiedModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-600 mb-4 mx-auto">
              <Mail size={24} />
            </div>

            <h3 className="text-lg font-bold text-center text-foreground">
              Email Verification Required
            </h3>
            <p className="text-xs text-center text-muted-foreground mt-1.5 mb-6">
              You must verify your email address before registering for classes. Please check your inbox for the verification link.
            </p>

            {resendStatus && (
              <p className="text-xs text-center p-3 rounded-xl bg-muted text-foreground mb-4">
                {resendStatus}
              </p>
            )}

            <div className="flex flex-col gap-2">
              <button
                type="button"
                disabled={resendingEmail}
                onClick={handleResendVerification}
                className="w-full py-2.5 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center justify-center gap-1.5"
              >
                {resendingEmail ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    Sending...
                  </>
                ) : (
                  <>
                    <Mail size={14} />
                    Resend Verification Email
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={() => setShowEmailUnverifiedModal(false)}
                className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}
    </PageContainer>
  );
}
