import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getMyOrders, type Order } from '../../api/orderApi';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import {
  Package,
  ShoppingBag,
  ShoppingCart,
  ChevronRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Loader2,
  FileText,
  Video,
  Calendar,
  CreditCard
} from 'lucide-react';

export default function OrdersPage() {
  const { user } = useAuth();
  const { itemCount } = useCart();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

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
      setTotalElements(res.totalElements);
    } catch (err: any) {
      setError(err.message || 'Failed to load your orders.');
    } finally {
      setLoading(false);
    }
  }

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function getPaymentBadge(status: string) {
    switch (status) {
      case 'PAID':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
            <CheckCircle2 size={12} />
            Paid
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-500 border border-amber-500/20">
            <Clock size={12} />
            Awaiting payment
          </span>
        );
      case 'EXPIRED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-muted text-muted-foreground border border-border">
            <XCircle size={12} />
            Expired — payment window closed
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle size={12} />
            Failed
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-muted text-muted-foreground">
            {status}
          </span>
        );
    }
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
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Navbar */}
      <header className="border-b border-border bg-card px-6 py-4 flex items-center justify-between sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold">
              AL
            </div>
            <span className="font-semibold text-foreground text-sm hidden sm:inline">AL Learning Platform</span>
          </Link>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/products"
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <ShoppingBag size={13} />
            <span className="hidden sm:inline">Catalog</span>
          </Link>
          <Link
            to="/cart"
            className="relative flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <ShoppingCart size={13} />
            <span>Cart</span>
            {itemCount > 0 && (
              <span className="rounded-full bg-primary text-primary-foreground px-1.5 py-0.2 text-[10px] font-bold">
                {itemCount}
              </span>
            )}
          </Link>
          <Link
            to="/orders"
            className="flex items-center gap-1.5 rounded-lg bg-primary/10 border border-primary/20 px-3 py-1.5 text-xs font-semibold text-primary transition"
          >
            <Package size={13} />
            <span>My Orders</span>
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-8 space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">My Orders</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            View your order history, delivery progress, and payment status.
          </p>
        </div>

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
                    {getPaymentBadge(order.paymentStatus)}
                    <span className="text-muted-foreground text-xs">&bull;</span>
                    {getFulfillmentBadge(order.fulfillmentStatus)}
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
      </main>
    </div>
  );
}
