import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  getAdminClassRegistrations,
  updateAdminRegistrationStatus,
  getAdminClasses,
  type AdminRegistrationResponse,
  type AdminClassResponse,
  type RegistrationStatus,
  type PageResponse,
} from '../../../api/classApi';
import type { ApiError } from '../../../api/authApi';
import {
  Users,
  Search,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ChevronLeft,
  ChevronRight,
  Mail,
  Phone,
  RefreshCw,
} from 'lucide-react';

function formatDate(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
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

interface ActionPrompt {
  registration: AdminRegistrationResponse;
  nextStatus: RegistrationStatus;
  title: string;
  description: string;
  btnLabel: string;
  btnStyle: string;
}

export default function AdminClassRegistrationsPage() {
  const [searchParams] = useSearchParams();
  const [classes, setClasses] = useState<AdminClassResponse[]>([]);
  const [data, setData] = useState<PageResponse<AdminRegistrationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Filters
  const [classFilter, setClassFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>(searchParams.get('status') || '');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);

  // Confirmation dialog
  const [actionPrompt, setActionPrompt] = useState<ActionPrompt | null>(null);
  const [processing, setProcessing] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    getAdminClasses()
      .then(setClasses)
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadRegistrations();
  }, [classFilter, statusFilter, page]);

  async function loadRegistrations() {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdminClassRegistrations({
        page,
        size: 15,
        classId: classFilter ? parseInt(classFilter, 10) : undefined,
        status: (statusFilter as RegistrationStatus) || undefined,
        q: searchQuery.trim() || undefined,
      });
      setData(res);
    } catch (err: unknown) {
      const e = err as { message?: string };
      setError(e?.message || 'Failed to load class registrations.');
    } finally {
      setLoading(false);
    }
  }

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    loadRegistrations();
  }

  function promptAction(reg: AdminRegistrationResponse, nextStatus: RegistrationStatus) {
    setActionError(null);
    if (nextStatus === 'CONFIRMED') {
      setActionPrompt({
        registration: reg,
        nextStatus,
        title: 'Confirm Registration',
        description: `Are you sure you want to confirm registration #${reg.registrationId} for ${reg.studentName} in "${reg.className}"?`,
        btnLabel: 'Confirm Registration',
        btnStyle: 'bg-emerald-600 hover:bg-emerald-700 text-white',
      });
    } else if (nextStatus === 'REJECTED') {
      setActionPrompt({
        registration: reg,
        nextStatus,
        title: 'Reject Registration',
        description: `Are you sure you want to reject registration #${reg.registrationId} for ${reg.studentName}? This is a final action.`,
        btnLabel: 'Reject Registration',
        btnStyle: 'bg-destructive hover:bg-destructive/90 text-white',
      });
    } else if (nextStatus === 'CANCELLED') {
      setActionPrompt({
        registration: reg,
        nextStatus,
        title: 'Cancel Registration',
        description: `Are you sure you want to cancel registration #${reg.registrationId} for ${reg.studentName}? This is a final action.`,
        btnLabel: 'Cancel Registration',
        btnStyle: 'bg-destructive hover:bg-destructive/90 text-white',
      });
    }
  }

  async function executeAction() {
    if (!actionPrompt) return;
    setProcessing(true);
    setActionError(null);
    try {
      const updated = await updateAdminRegistrationStatus(
        actionPrompt.registration.registrationId,
        actionPrompt.nextStatus
      );
      setActionSuccess(
        `Registration #${updated.registrationId} status changed to ${updated.status}.`
      );
      setActionPrompt(null);
      loadRegistrations();
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setActionError(apiErr?.message || 'Failed to update registration status.');
    } finally {
      setProcessing(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-foreground flex items-center gap-2.5">
            <Users className="text-primary" size={26} />
            Class Registrations
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Review student registrations, filter by class/status, and transition statuses.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setPage(0);
            loadRegistrations();
          }}
          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted transition"
        >
          <RefreshCw size={13} />
          Refresh
        </button>
      </div>

      {/* Global Feedback */}
      {actionSuccess && (
        <div className="flex items-center justify-between p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-300 text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{actionSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionSuccess(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div className="flex items-center justify-between p-3.5 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filters Bar */}
      <div className="bg-card p-4 rounded-2xl border border-border shadow-2xs space-y-3">
        <form onSubmit={handleSearchSubmit} className="flex flex-wrap gap-3 items-center">
          {/* Search Input */}
          <div className="relative flex-1 min-w-[240px]">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              id="admin-registration-q"
              type="text"
              placeholder="Search by student name or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-border bg-background text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
          </div>

          {/* Class Filter */}
          <select
            id="admin-registration-class-filter"
            value={classFilter}
            onChange={(e) => {
              setClassFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-xl border border-border bg-background text-xs text-foreground px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer min-w-[180px]"
          >
            <option value="">All Classes</option>
            {classes.map((c) => (
              <option key={c.classId} value={c.classId}>
                {c.name}
              </option>
            ))}
          </select>

          {/* Status Filter */}
          <select
            id="admin-registration-status-filter"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-xl border border-border bg-background text-xs text-foreground px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer min-w-[140px]"
          >
            <option value="">All Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="REJECTED">REJECTED</option>
          </select>

          <button
            type="submit"
            className="px-4 py-1.5 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 transition shadow-xs"
          >
            Search
          </button>

          {(classFilter || statusFilter || searchQuery) && (
            <button
              type="button"
              onClick={() => {
                setClassFilter('');
                setStatusFilter('');
                setSearchQuery('');
                setPage(0);
              }}
              className="px-3 py-1.5 rounded-xl border border-border text-xs text-muted-foreground hover:text-destructive hover:border-destructive/30 transition"
            >
              Clear
            </button>
          )}
        </form>
      </div>

      {/* Registrations Table */}
      <div className="bg-card rounded-2xl border border-border overflow-hidden shadow-2xs">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <Loader2 size={30} className="animate-spin text-primary mb-2" />
            <p className="text-xs">Loading registrations...</p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="text-center py-16 text-muted-foreground">
            <p className="text-sm font-semibold text-foreground">No registrations found</p>
            <p className="text-xs mt-1">
              {classFilter || statusFilter || searchQuery
                ? 'Try clearing or modifying your filter criteria.'
                : 'No student registrations recorded yet.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/40 text-muted-foreground font-semibold">
                  <th className="py-3 px-4">Reg ID</th>
                  <th className="py-3 px-4">Student</th>
                  <th className="py-3 px-4">Contact</th>
                  <th className="py-3 px-4">Class</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4">Registered At</th>
                  <th className="py-3 px-4 text-right">Allowed Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {data.content.map((reg) => {
                  const badge = getStatusBadge(reg.status);
                  const isPending = reg.status === 'PENDING';
                  const isConfirmed = reg.status === 'CONFIRMED';
                  const isFinal = reg.status === 'CANCELLED' || reg.status === 'REJECTED';

                  return (
                    <tr
                      key={reg.registrationId}
                      id={`admin-reg-row-${reg.registrationId}`}
                      className="hover:bg-muted/30 transition-colors"
                    >
                      <td className="py-3.5 px-4 font-mono font-bold text-foreground">
                        #{reg.registrationId}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-foreground">
                        {reg.studentName}
                      </td>
                      <td className="py-3.5 px-4 text-muted-foreground">
                        <div className="flex items-center gap-1 font-mono text-[11px]">
                          <Mail size={12} className="shrink-0 text-primary" />
                          <span>{reg.studentEmail}</span>
                        </div>
                        {reg.studentPhone && (
                          <div className="flex items-center gap-1 text-[11px] mt-0.5">
                            <Phone size={12} className="shrink-0 text-muted-foreground" />
                            <span>{reg.studentPhone}</span>
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 font-medium text-foreground">
                        {reg.className}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${badge.style}`}
                        >
                          <span className="h-1.5 w-1.5 rounded-full bg-current" />
                          {reg.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-muted-foreground whitespace-nowrap">
                        {formatDate(reg.registeredAt)}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {isPending && (
                            <>
                              <button
                                type="button"
                                id={`btn-confirm-reg-${reg.registrationId}`}
                                onClick={() => promptAction(reg, 'CONFIRMED')}
                                className="px-2.5 py-1 rounded-lg bg-emerald-600/10 text-emerald-600 border border-emerald-600/20 hover:bg-emerald-600 hover:text-white text-[11px] font-semibold transition"
                              >
                                Confirm
                              </button>
                              <button
                                type="button"
                                id={`btn-reject-reg-${reg.registrationId}`}
                                onClick={() => promptAction(reg, 'REJECTED')}
                                className="px-2.5 py-1 rounded-lg bg-destructive/10 text-destructive border border-destructive/20 hover:bg-destructive hover:text-white text-[11px] font-semibold transition"
                              >
                                Reject
                              </button>
                              <button
                                type="button"
                                id={`btn-cancel-reg-${reg.registrationId}`}
                                onClick={() => promptAction(reg, 'CANCELLED')}
                                className="px-2 py-1 rounded-lg border border-border text-muted-foreground hover:bg-muted text-[11px] font-medium transition"
                              >
                                Cancel
                              </button>
                            </>
                          )}

                          {isConfirmed && (
                            <button
                              type="button"
                              id={`btn-cancel-reg-${reg.registrationId}`}
                              onClick={() => promptAction(reg, 'CANCELLED')}
                              className="px-2.5 py-1 rounded-lg bg-destructive/10 text-destructive border border-destructive/20 hover:bg-destructive hover:text-white text-[11px] font-semibold transition"
                            >
                              Cancel
                            </button>
                          )}

                          {isFinal && (
                            <span className="text-[11px] text-muted-foreground/60 italic">
                              Final status
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {data && data.totalPages > 1 && (
          <div className="p-4 border-t border-border flex items-center justify-between">
            <div className="text-xs text-muted-foreground">
              Showing page {data.number + 1} of {data.totalPages} ({data.totalElements} items)
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={data.first}
                onClick={() => setPage(page - 1)}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition"
              >
                <ChevronLeft size={13} />
                Previous
              </button>
              <button
                type="button"
                disabled={data.last}
                onClick={() => setPage(page + 1)}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition"
              >
                Next
                <ChevronRight size={13} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Status Transition Confirmation Modal */}
      {actionPrompt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <h3 className="text-base font-bold text-foreground mb-2">
              {actionPrompt.title}
            </h3>
            <p className="text-xs text-muted-foreground mb-4">
              {actionPrompt.description}
            </p>

            {actionError && (
              <div className="p-3 rounded-xl bg-destructive/10 text-destructive text-xs mb-4">
                {actionError}
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-border">
              <button
                type="button"
                disabled={processing}
                onClick={() => setActionPrompt(null)}
                className="px-4 py-2 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
              <button
                type="button"
                id="btn-confirm-admin-status-change"
                disabled={processing}
                onClick={executeAction}
                className={`px-4 py-2 rounded-xl text-xs font-semibold shadow-xs transition flex items-center gap-1.5 ${actionPrompt.btnStyle}`}
              >
                {processing && <Loader2 size={13} className="animate-spin" />}
                {actionPrompt.btnLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
