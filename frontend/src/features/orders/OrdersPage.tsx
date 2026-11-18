import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getMyOrders, type Order } from '../../api/orderApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  Package,
  ChevronRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Loader2,
  FileText,
  Video,
  Calendar,
  ShoppingBag,
} from 'lucide-react';

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadOrders(page);
  }, [page]);

  async function loadOrders(pageIndex: number) {
    setLoading(true);
    setError(null);
    try {
      const res = await getMyOrders(pageIndex, 10);
      setOrders(res.content);
      setTotalPages(res.totalPages);
    } catch (err: any) {
      setError(err.message || 'Failed to load your orders.');
    } finally {
      setLoading(false);
    }
  }

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function getPaymentBadge(status: string, latestPaymentStatus?: string | null) {
    if (status === 'PAID') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
          <CheckCircle2 size={12} />
          Paid
        </span>
      );
    }
    if (status === 'EXPIRED') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-muted text-muted-foreground border border-border">
          <XCircle size={12} />
          Expired
        </span>
      );
    }
    if (status === 'FAILED' || latestPaymentStatus === 'FAILED') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-destructive/10 text-destructive border border-destructive/20">
          <AlertCircle size={12} />
          Payment failed (retry available)
        </span>
      );
    }
    if (status === 'PENDING') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-500 border border-amber-500/20">
          <Clock size={12} />
          Awaiting payment
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-muted text-muted-foreground">
        {status}
      </span>
    );
  }

  function getFulfillmentBadge(status: string) {
    switch (status) {
      case 'DELIVERED':
        return (
          <span className="text-[11px] font-semibold text-emerald-500">Delivered</span>
        );
      case 'SHIPPED':
        return (
          <span className="text-[11px] font-semibold text-indigo-500">Shipped</span>
        );
      case 'PROCESSING':
        return (
          <span className="text-[11px] font-semibold text-blue-500">Processing</span>
        );
      case 'PENDING':
        return (
          <span className="text-[11px] font-semibold text-amber-500">Shipping Pending</span>
        );
      case 'NOT_REQUIRED':
      default:
        return (
          <span className="text-[11px] font-semibold text-muted-foreground">Digital (No Shipping)</span>
        );
    }
  }

  return (
    <PageContainer>
      <div className="space-y-6">
        <PageHeader
          title="My Orders"
          description="View your order history, delivery progress, and payment status."
        />

        {error && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-destructive flex items-center gap-3 text-xs">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-primary mb-3" />
            <p className="text-xs text-muted-foreground">Loading your orders...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center max-w-md mx-auto space-y-4">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <Package size={24} />
            </div>
            <h2 className="text-base font-bold text-foreground">No orders yet</h2>
            <p className="text-xs text-muted-foreground">
              When you purchase notes or video lessons, your order details will appear here.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:bg-primary/90 transition shadow-sm"
            >
              <ShoppingBag size={14} />
              Explore Catalog
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map(order => (
              <div
                key={order.orderId}
                className="rounded-2xl border border-border bg-card p-5 sm:p-6 shadow-sm hover:border-primary/30 transition space-y-4"
              >
                {/* Header line: Order #, date, badges */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border pb-3">
                  <div className="flex items-center gap-3">
                    <span className="font-extrabold text-foreground text-sm sm:text-base">
                      Order #{order.orderId}
                    </span>
                    <span className="text-muted-foreground text-xs">&bull;</span>
                    <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                      <Calendar size={13} />
                      <span>{new Date(order.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 flex-wrap">
                    {getPaymentBadge(order.paymentStatus, order.latestPaymentStatus)}
                    <span className="text-muted-foreground text-xs">&bull;</span>
                    {getFulfillmentBadge(order.fulfillmentStatus)}
                    {order.trackingNote && (
                      <>
                        <span className="text-muted-foreground text-xs">&bull;</span>
                        <span className="text-[11px] font-mono text-muted-foreground bg-muted px-2 py-0.5 rounded border border-border">
                          Track: {order.trackingNote}
                        </span>
                      </>
                    )}
                  </div>
                </div>

                {/* Items summary */}
                <div className="space-y-2">
                  {order.items.map(item => (
                    <div key={item.orderItemId} className="flex items-center justify-between text-xs py-1">
                      <div className="flex items-center gap-2">
                        {item.productType === 'VIDEO' ? (
                          <Video size={14} className="text-primary" />
                        ) : (
                          <FileText size={14} className="text-primary" />
                        )}
                        <span className="font-medium text-foreground">{item.productName}</span>
                        <span className="text-muted-foreground">x {item.quantity}</span>
                      </div>
                      <span className="font-semibold text-foreground">{formatLKR(item.subtotal)}</span>
                    </div>
                  ))}
                </div>

                {/* Footer line: Total & Details link */}
                <div className="flex items-center justify-between border-t border-border pt-3">
                  <div className="text-xs">
                    <span className="text-muted-foreground">Total: </span>
                    <span className="font-bold text-foreground text-sm">{formatLKR(order.totalAmount)}</span>
                  </div>

                  <Link
                    to={`/orders/${order.orderId}`}
                    className="inline-flex items-center gap-1 text-xs font-bold text-primary hover:text-primary/80 transition"
                  >
                    <span>View Details</span>
                    <ChevronRight size={14} />
                  </Link>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 pt-4">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted disabled:opacity-30 disabled:cursor-not-allowed transition"
                >
                  Previous
                </button>
                <span className="text-xs text-muted-foreground px-2">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  type="button"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                  className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-foreground hover:bg-muted disabled:opacity-30 disabled:cursor-not-allowed transition"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
