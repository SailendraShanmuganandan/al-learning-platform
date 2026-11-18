import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  getMyClassRegistrations,
  cancelMyClassRegistration,
  type StudentRegistrationResponse,
  type RegistrationStatus,
} from '../../api/classApi';
import type { ApiError } from '../../api/authApi';
import {
  GraduationCap,
  Calendar,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Ban,
  ArrowRight,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';

function formatLKR(fee: number): string {
  return `LKR ${Number(fee).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatDate(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

function getStatusBadge(status: RegistrationStatus) {
  switch (status) {
    case 'PENDING':
      return {
        label: 'Pending',
        style: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
      };
    case 'CONFIRMED':
      return {
        label: 'Confirmed',
        style: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
      };
    case 'CANCELLED':
      return {
        label: 'Cancelled',
        style: 'bg-muted text-muted-foreground border-border',
      };
    case 'REJECTED':
      return {
        label: 'Rejected',
        style: 'bg-destructive/10 text-destructive border-destructive/20',
      };
    default:
      return {
        label: status,
        style: 'bg-muted text-muted-foreground border-border',
      };
  }
}

export default function MyClassesPage() {
  const [registrations, setRegistrations] = useState<StudentRegistrationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Cancellation modal state
  const [targetToCancel, setTargetToCancel] = useState<StudentRegistrationResponse | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancelSuccess, setCancelSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadRegistrations();
  }, []);

  async function loadRegistrations() {
    setLoading(true);
    setError(null);
    try {
      const data = await getMyClassRegistrations();
      setRegistrations(data);
    } catch (err: unknown) {
      const e = err as { message?: string };
      setError(e?.message || 'Failed to load your class registrations.');
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirmCancel() {
    if (!targetToCancel) return;

    setCancelling(true);
    setCancelError(null);
    try {
      const updated = await cancelMyClassRegistration(targetToCancel.registrationId);
      setCancelSuccess(`Successfully cancelled registration for ${targetToCancel.className}.`);
      setRegistrations((prev) =>
        prev.map((r) => (r.registrationId === updated.registrationId ? updated : r))
      );
      setTargetToCancel(null);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      if (apiErr?.error === 'CANCEL_NOT_ALLOWED') {
        setCancelError(apiErr.message || 'Confirmed registrations can only be cancelled by the teacher. Please contact us.');
      } else {
        setCancelError(apiErr?.message || 'Failed to cancel registration.');
      }
    } finally {
      setCancelling(false);
    }
  }

  return (
    <PageContainer>
      <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
        <PageHeader title="My Registered Classes" />
        <Link
          to="/classes"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:opacity-90 transition"
        >
          <GraduationCap size={14} />
          Browse New Classes
        </Link>
      </div>

      {/* Global Feedback */}
      {cancelSuccess && (
        <div className="flex items-center justify-between p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-300 text-xs mb-6">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{cancelSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setCancelSuccess(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
          <Loader2 size={32} className="animate-spin text-primary mb-3" />
          <p className="text-sm">Loading your registrations...</p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="flex items-center gap-3 p-4 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-sm my-6">
          <AlertCircle size={20} className="shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && registrations.length === 0 && (
        <div className="text-center py-20 border border-dashed border-border rounded-3xl bg-card/40 my-8">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground mb-4">
            <GraduationCap size={28} />
          </div>
          <h3 className="text-lg font-bold text-foreground">No class registrations yet</h3>
          <p className="text-sm text-muted-foreground mt-1 max-w-sm mx-auto">
            You haven't registered for any classes yet. Browse our active classes to find schedules that match your study goals.
          </p>
          <Link
            to="/classes"
            className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:opacity-90 transition"
          >
            Explore Classes
          </Link>
        </div>
      )}

      {/* Registrations List */}
      {!loading && !error && registrations.length > 0 && (
        <div className="space-y-4">
          {registrations.map((reg) => {
            const badge = getStatusBadge(reg.status);
            const isPending = reg.status === 'PENDING';
            const isConfirmed = reg.status === 'CONFIRMED';

            return (
              <div
                key={reg.registrationId}
                id={`my-class-reg-${reg.registrationId}`}
                className="bg-card rounded-2xl border border-border p-6 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 hover:border-primary/30 transition"
              >
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <span className="text-xs font-bold uppercase tracking-wider text-primary">
                      {reg.subjectName}
                    </span>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${badge.style}`}
                    >
                      {badge.label}
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      Registered on {formatDate(reg.registeredAt)}
                    </span>
                  </div>

                  <h3 className="text-base font-bold text-foreground">
                    {reg.className}
                  </h3>

                  <div className="flex items-center gap-5 text-xs text-muted-foreground flex-wrap">
                    <div className="flex items-center gap-1.5 font-medium">
                      <Calendar size={13} className="text-primary" />
                      <span>{reg.scheduleDescription}</span>
                    </div>
                    <div className="font-semibold text-foreground">
                      Fee: {formatLKR(reg.fee)}
                    </div>
                  </div>
                </div>

                {/* Action / Guidance */}
                <div className="flex flex-col sm:items-end gap-2 shrink-0 w-full sm:w-auto pt-4 sm:pt-0 border-t sm:border-t-0 border-border">
                  {isPending && (
                    <button
                      type="button"
                      id={`btn-cancel-reg-${reg.registrationId}`}
                      onClick={() => {
                        setCancelError(null);
                        setTargetToCancel(reg);
                      }}
                      className="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl border border-destructive/30 text-destructive text-xs font-semibold hover:bg-destructive/10 transition"
                    >
                      <Ban size={13} />
                      Cancel registration
                    </button>
                  )}

                  {isConfirmed && (
                    <div className="text-xs text-muted-foreground bg-muted/60 px-3 py-1.5 rounded-xl border border-border/80">
                      To cancel, please contact us
                    </div>
                  )}

                  <Link
                    to={`/classes/${reg.classId}`}
                    className="text-xs font-medium text-primary hover:underline inline-flex items-center gap-1"
                  >
                    View class
                    <ArrowRight size={12} />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {targetToCancel && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-destructive/10 text-destructive mb-4 mx-auto">
              <Ban size={24} />
            </div>

            <h3 className="text-lg font-bold text-center text-foreground">
              Cancel Registration?
            </h3>
            <p className="text-xs text-center text-muted-foreground mt-1.5 mb-5">
              Are you sure you want to cancel your registration for{' '}
              <span className="font-semibold text-foreground">{targetToCancel.className}</span>?
              You can register again at any time if seats remain available.
            </p>

            {cancelError && (
              <div className="p-3 rounded-xl bg-destructive/10 text-destructive text-xs mb-4">
                {cancelError}
              </div>
            )}

            <div className="flex flex-col gap-2">
              <button
                type="button"
                id="btn-confirm-cancel-dialog"
                disabled={cancelling}
                onClick={handleConfirmCancel}
                className="w-full py-2.5 px-4 rounded-xl bg-destructive text-white text-xs font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center justify-center gap-1.5"
              >
                {cancelling ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    Cancelling...
                  </>
                ) : (
                  'Yes, Cancel Registration'
                )}
              </button>
              <button
                type="button"
                disabled={cancelling}
                onClick={() => setTargetToCancel(null)}
                className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Keep Registration
              </button>
            </div>
          </div>
        </div>
      )}
    </PageContainer>
  );
}
