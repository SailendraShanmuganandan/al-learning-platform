import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  getAdminOrders,
  type AdminOrderListItem,
  type OrderFilterParams
} from '../../../api/adminCommerceApi';
import {
  Search,
  RotateCcw,
  AlertTriangle,
  ChevronRight,
  ChevronLeft,
  Loader2,
  CheckCircle2,
  Clock,
  XCircle,
  Truck,
  Eye,
  AlertCircle
} from 'lucide-react';

export default function AdminOrdersPage() {
  const [searchParams] = useSearchParams();
  const [orders, setOrders] = useState<AdminOrderListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [q, setQ] = useState('');
  const [paymentStatus, setPaymentStatus] = useState(searchParams.get('paymentStatus') || '');
  const [fulfillmentStatus, setFulfillmentStatus] = useState(searchParams.get('fulfillmentStatus') || '');
  const [needsReview, setNeedsReview] = useState(searchParams.get('needsReview') === 'true');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 15;

  useEffect(() => {
    loadOrders();
  }, [page, paymentStatus, fulfillmentStatus, needsReview]);

  async function loadOrders(overrideParams?: Partial<OrderFilterParams>) {
    setLoading(true);
    setError(null);
    try {
      const params: OrderFilterParams = {
        page,
        size: pageSize,
        paymentStatus: paymentStatus || undefined,
        fulfillmentStatus: fulfillmentStatus || undefined,
        needsReview: needsReview ? true : undefined,
        q: q.trim() || undefined,
        from: fromDate ? `${fromDate}T00:00:00Z` : undefined,
        to: toDate ? `${toDate}T23:59:59Z` : undefined,
        ...overrideParams
      };

      const res = await getAdminOrders(params);
      setOrders(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err: any) {
      setError(err.message || 'Failed to load orders.');
    } finally {
      setLoading(false);
    }
  }

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    loadOrders({ page: 0 });
  }

  function handleResetFilters() {
    setQ('');
    setPaymentStatus('');
    setFulfillmentStatus('');
    setNeedsReview(false);
    setFromDate('');
    setToDate('');
    setPage(0);
    loadOrders({
      page: 0,
      q: undefined,
      paymentStatus: undefined,
      fulfillmentStatus: undefined,
      needsReview: undefined,
      from: undefined,
      to: undefined
    });
  }

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function renderPaymentBadge(status: string) {
    switch (status) {
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
      case 'EXPIRED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-muted text-muted-foreground border border-border">
            <XCircle size={12} />
            Expired
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
            {status}
          </span>
        );
    }
  }

  function renderFulfillmentBadge(status: string) {
    switch (status) {
      case 'DELIVERED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-500">
            Delivered
          </span>
        );
      case 'SHIPPED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-500">
            <Truck size={12} />
            Shipped
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-500/10 text-sky-500">
            Processing
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-500">
            Pending
          </span>
        );
      case 'NOT_REQUIRED':
      default:
        return (
          <span className="text-xs text-muted-foreground">
            Not Required
          </span>
        );
    }
  }

  return (
    <div className="space-y-6">
      {/* Title & Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-foreground tracking-tight">Order Management</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Monitor, fulfill and inspect customer orders and historical shipments.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="rounded-xl border border-border bg-card px-3.5 py-2 text-xs font-medium text-muted-foreground">
            Total Orders: <strong className="text-foreground">{totalElements}</strong>
          </span>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search by Order ID or Customer Email..."
              value={q}
              onChange={e => setQ(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-xs rounded-xl border border-border bg-background focus:outline-none focus:ring-2 focus:ring-primary/20 text-foreground placeholder:text-muted-foreground"
            />
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm"
            >
              Search
            </button>
            <button
              type="button"
              onClick={handleResetFilters}
              title="Reset all filters"
              className="px-3 py-2 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition flex items-center gap-1.5"
            >
              <RotateCcw size={13} />
              <span>Reset</span>
            </button>
          </div>
        </form>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 pt-2 border-t border-border/60">
          {/* Payment Status Filter */}
          <div>
            <label className="block text-[11px] font-semibold text-muted-foreground mb-1">
              Payment Status
            </label>
            <select
              value={paymentStatus}
              onChange={e => {
                setPaymentStatus(e.target.value);
                setPage(0);
              }}
              className="w-full py-1.5 px-2.5 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">All Payment Statuses</option>
              <option value="PAID">PAID</option>
              <option value="PENDING">PENDING</option>
              <option value="REFUNDED">REFUNDED</option>
              <option value="EXPIRED">EXPIRED</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>

          {/* Fulfillment Status Filter */}
          <div>
            <label className="block text-[11px] font-semibold text-muted-foreground mb-1">
              Fulfillment Status
            </label>
            <select
              value={fulfillmentStatus}
              onChange={e => {
                setFulfillmentStatus(e.target.value);
                setPage(0);
              }}
              className="w-full py-1.5 px-2.5 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">All Fulfillment Statuses</option>
              <option value="PENDING">PENDING</option>
              <option value="PROCESSING">PROCESSING</option>
              <option value="SHIPPED">SHIPPED</option>
              <option value="DELIVERED">DELIVERED</option>
              <option value="NOT_REQUIRED">NOT_REQUIRED</option>
            </select>
          </div>

          {/* Date Range: From / To */}
          <div>
            <label className="block text-[11px] font-semibold text-muted-foreground mb-1">
              Created Date Range
            </label>
            <div className="flex items-center gap-1.5">
              <input
                type="date"
                value={fromDate}
                onChange={e => setFromDate(e.target.value)}
                className="w-full py-1.5 px-2 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
              <span className="text-muted-foreground text-xs">&ndash;</span>
              <input
                type="date"
                value={toDate}
                onChange={e => setToDate(e.target.value)}
                className="w-full py-1.5 px-2 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
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
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-xs text-destructive flex items-center gap-3">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Orders Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-xs font-medium">Loading orders...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="p-16 text-center space-y-2">
            <p className="font-bold text-foreground text-sm">No orders found</p>
            <p className="text-xs text-muted-foreground">
              No orders matched your selected filters or search query.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-border bg-muted/40 font-semibold text-muted-foreground text-[11px] uppercase tracking-wider">
                  <th className="py-3.5 px-4">Order ID</th>
                  <th className="py-3.5 px-4">Customer</th>
                  <th className="py-3.5 px-4">Total</th>
                  <th className="py-3.5 px-4">Payment</th>
                  <th className="py-3.5 px-4">Fulfillment</th>
                  <th className="py-3.5 px-4">Date</th>
                  <th className="py-3.5 px-4">Review</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {orders.map(order => (
                  <tr
                    key={order.orderId}
                    className="hover:bg-muted/30 transition group"
                  >
                    <td className="py-3.5 px-4 font-mono font-bold text-foreground">
                      <Link
                        to={`/admin/orders/${order.orderId}`}
                        className="text-primary hover:underline"
                      >
                        #{order.orderId}
                      </Link>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-foreground">{order.customerName}</div>
                      <div className="text-[11px] text-muted-foreground">{order.customerEmail}</div>
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-foreground">
                      {formatLKR(order.totalAmount)}
                    </td>
                    <td className="py-3.5 px-4">
                      {renderPaymentBadge(order.paymentStatus)}
                    </td>
                    <td className="py-3.5 px-4">
                      {renderFulfillmentBadge(order.fulfillmentStatus)}
                    </td>
                    <td className="py-3.5 px-4 text-muted-foreground">
                      {new Date(order.createdAt).toLocaleString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                    <td className="py-3.5 px-4">
                      {order.needsReview ? (
                        <span
                          title="Contains payment requiring administrative review"
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30"
                        >
                          <AlertTriangle size={12} />
                          Needs Review
                        </span>
                      ) : (
                        <span className="text-muted-foreground/60 text-[11px]">&mdash;</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        to={`/admin/orders/${order.orderId}`}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted hover:border-primary/40 transition shadow-sm"
                      >
                        <Eye size={13} />
                        <span>View</span>
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
              Showing page {page + 1} of {totalPages} ({totalElements} total items)
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
