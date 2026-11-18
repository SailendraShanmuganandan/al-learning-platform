import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  getAdminPayments,
  type AdminPaymentListItem,
  type PaymentFilterParams
} from '../../../api/adminCommerceApi';
import {
  RotateCcw,
  AlertTriangle,
  ChevronRight,
  ChevronLeft,
  Loader2,
  CheckCircle2,
  Clock,
  ExternalLink,
  AlertCircle
} from 'lucide-react';

export default function AdminPaymentsPage() {
  const [searchParams] = useSearchParams();
  const [payments, setPayments] = useState<AdminPaymentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [needsReview, setNeedsReview] = useState(searchParams.get('needsReview') === 'true');
  const [orderIdInput, setOrderIdInput] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 15;

  useEffect(() => {
    loadPayments();
  }, [page, status, needsReview]);

  async function loadPayments(overrideParams?: Partial<PaymentFilterParams>) {
    setLoading(true);
    setError(null);
    try {
      let parsedOrderId: number | undefined = undefined;
      if (orderIdInput.trim()) {
        const val = parseInt(orderIdInput.trim(), 10);
        if (!isNaN(val)) parsedOrderId = val;
      }

      const params: PaymentFilterParams = {
        page,
        size: pageSize,
        status: status || undefined,
        needsReview: needsReview ? true : undefined,
        orderId: parsedOrderId,
        ...overrideParams
      };

      const res = await getAdminPayments(params);
      setPayments(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err: any) {
      setError(err.message || 'Failed to load payments.');
    } finally {
      setLoading(false);
    }
  }

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    loadPayments({ page: 0 });
  }

  function handleResetFilters() {
    setStatus('');
    setNeedsReview(false);
    setOrderIdInput('');
    setPage(0);
    loadPayments({
      page: 0,
      status: undefined,
      needsReview: undefined,
      orderId: undefined
    });
  }

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function renderStatusBadge(st: string) {
    switch (st) {
      case 'PAID':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
            <CheckCircle2 size={12} />
            Paid
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-500 border border-amber-500/20">
            <Clock size={12} />
            Pending
          </span>
        );
      case 'REFUNDED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-500/10 text-purple-500 border border-purple-500/20">
            Refunded
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle size={12} />
            Failed
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-muted text-muted-foreground">
            {st}
          </span>
        );
    }
  }

  return (
    <div className="space-y-6">
      {/* Title & Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-foreground tracking-tight">Payments & Transactions</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Review PayHere payments, resolve anomalies, and audit refund statuses.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="rounded-xl border border-border bg-card px-3.5 py-2 text-xs font-medium text-muted-foreground">
            Total Payments: <strong className="text-foreground">{totalElements}</strong>
          </span>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-3 md:grid-cols-4 gap-3">
          {/* Order ID Search */}
          <div>
            <label className="block text-[11px] font-semibold text-muted-foreground mb-1">
              Order ID
            </label>
            <input
              type="text"
              placeholder="e.g. 1042"
              value={orderIdInput}
              onChange={e => setOrderIdInput(e.target.value)}
              className="w-full py-1.5 px-3 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
            />
          </div>

          {/* Payment Status Filter */}
          <div>
            <label className="block text-[11px] font-semibold text-muted-foreground mb-1">
              Status
            </label>
            <select
              value={status}
              onChange={e => {
                setStatus(e.target.value);
                setPage(0);
              }}
              className="w-full py-1.5 px-2.5 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">All Statuses</option>
              <option value="PAID">PAID</option>
              <option value="PENDING">PENDING</option>
              <option value="REFUNDED">REFUNDED</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>

          {/* Needs Review Toggle */}
          <div className="flex items-end pb-1">
            <label className="flex items-center gap-2 cursor-pointer text-xs font-semibold text-foreground select-none">
              <input
                type="checkbox"
                checked={needsReview}
                onChange={e => {
                  setNeedsReview(e.target.checked);
                  setPage(0);
                }}
                className="h-4 w-4 rounded border-border text-primary focus:ring-primary/20"
              />
              <span className="flex items-center gap-1.5">
                <AlertTriangle size={14} className="text-amber-500" />
                <span>Needs Review Only</span>
              </span>
            </label>
          </div>

          {/* Buttons */}
          <div className="flex items-end gap-2">
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm"
            >
              Filter
            </button>
            <button
              type="button"
              onClick={handleResetFilters}
              title="Reset filters"
              className="px-3 py-2 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition flex items-center gap-1.5"
            >
              <RotateCcw size={13} />
              <span>Reset</span>
            </button>
          </div>
        </form>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-xs text-destructive flex items-center gap-3">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Payments Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-xs font-medium">Loading payments...</p>
          </div>
        ) : payments.length === 0 ? (
          <div className="p-16 text-center space-y-2">
            <p className="font-bold text-foreground text-sm">No payments found</p>
            <p className="text-xs text-muted-foreground">
              No payment records match the current filter criteria.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-border bg-muted/40 font-semibold text-muted-foreground text-[11px] uppercase tracking-wider">
                  <th className="py-3.5 px-4">Payment ID</th>
                  <th className="py-3.5 px-4">Order ID</th>
                  <th className="py-3.5 px-4">Amount</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4">Reference</th>
                  <th className="py-3.5 px-4">Needs Review</th>
                  <th className="py-3.5 px-4">Reason</th>
                  <th className="py-3.5 px-4">Created</th>
                  <th className="py-3.5 px-4 text-right">Order Link</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {payments.map(p => (
                  <tr
                    key={p.paymentId}
                    className="hover:bg-muted/30 transition group"
                  >
                    <td className="py-3.5 px-4 font-mono font-bold text-foreground">
                      #{p.paymentId}
                    </td>
                    <td className="py-3.5 px-4 font-mono">
                      <Link
                        to={`/admin/orders/${p.orderId}`}
                        className="text-primary hover:underline font-bold"
                      >
                        Order #{p.orderId}
                      </Link>
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-foreground">
                      {formatLKR(p.amount)}
                    </td>
                    <td className="py-3.5 px-4">
                      {renderStatusBadge(p.status)}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-[11px] text-muted-foreground">
                      {p.transactionReference || '—'}
                    </td>
                    <td className="py-3.5 px-4">
                      {p.needsReview ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30">
                          <AlertTriangle size={12} />
                          Flagged
                        </span>
                      ) : (
                        <span className="text-muted-foreground/60 text-[11px]">&mdash;</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-muted-foreground text-[11px]">
                      {p.reviewReason || '—'}
                    </td>
                    <td className="py-3.5 px-4 text-muted-foreground">
                      {new Date(p.createdAt).toLocaleString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        to={`/admin/orders/${p.orderId}`}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted hover:border-primary/40 transition shadow-sm"
                      >
                        <ExternalLink size={13} />
                        <span>View Order</span>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-border flex items-center justify-between gap-4">
            <span className="text-xs text-muted-foreground">
              Showing page {page + 1} of {totalPages} ({totalElements} total records)
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1"
              >
                <ChevronLeft size={14} />
                <span>Previous</span>
              </button>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1"
              >
                <span>Next</span>
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
