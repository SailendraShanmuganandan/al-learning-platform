import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getOrder, type Order } from '../../api/orderApi';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import {
  ArrowLeft,
  Package,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  MapPin,
  FileText,
  Video,
  CreditCard,
  Loader2,
  ShieldCheck,
  ShoppingBag,
  ShoppingCart
} from 'lucide-react';

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const { itemCount } = useCart();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const [isExpired, setIsExpired] = useState(false);

  useEffect(() => {
    if (!id) return;
    loadOrder(parseInt(id, 10));
  }, [id]);

  async function loadOrder(orderId: number) {
    setLoading(true);
    setError(null);
    try {
      const data = await getOrder(orderId);
      setOrder(data);
    } catch (err: any) {
      setError(err.message || 'Order not found.');
    } finally {
      setLoading(false);
    }
  }

  // Countdown timer for pending order
  useEffect(() => {
    if (!order?.expiresAt) return;

    function updateRemaining() {
      const expiry = new Date(order!.expiresAt).getTime();
      const now = Date.now();
      const diff = expiry - now;

      if (diff <= 0 || order?.paymentStatus === 'EXPIRED') {
        setIsExpired(true);
        setTimeRemaining('Expired — payment window closed');
      } else {
        setIsExpired(false);
        const minutes = Math.floor(diff / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        setTimeRemaining(`${minutes}m ${seconds < 10 ? '0' : ''}${seconds}s remaining`);
      }
    }

    updateRemaining();
    const timer = setInterval(updateRemaining, 1000);
    return () => clearInterval(timer);
  }, [order]);

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary mb-3" />
        <p className="text-xs text-muted-foreground">Loading order details...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4">
        <div className="rounded-2xl border border-border bg-card p-8 max-w-md text-center space-y-4">
          <AlertCircle className="h-10 w-10 text-destructive mx-auto" />
          <h2 className="text-lg font-bold text-foreground">Order Not Found</h2>
          <p className="text-xs text-muted-foreground">
            {error || 'This order does not exist or does not belong to your account.'}
          </p>
          <Link
            to="/orders"
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition"
          >
            Back to My Orders
          </Link>
        </div>
      </div>
    );
  }

  const isOrderExpired = order.paymentStatus === 'EXPIRED' || isExpired;

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
            <span className="hidden sm:inline">My Orders</span>
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 py-8 space-y-6">
        <div>
          <Link
            to="/orders"
            className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground transition mb-2"
          >
            <ArrowLeft size={14} />
            Back to My Orders
          </Link>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                Order #{order.orderId}
              </h1>
              <p className="text-xs text-muted-foreground mt-0.5">
                Placed on {new Date(order.createdAt).toLocaleDateString()} at{' '}
                {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>

            {/* Status indicators */}
            <div className="flex items-center gap-2">
              {order.paymentStatus === 'PAID' && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                  <CheckCircle2 size={13} />
                  Paid
                </span>
              )}
              {order.paymentStatus === 'PENDING' && !isOrderExpired && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-500 border border-amber-500/20">
                  <Clock size={13} />
                  Awaiting payment
                </span>
              )}
              {isOrderExpired && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-muted text-muted-foreground border border-border">
                  <XCircle size={13} />
                  Expired — payment window closed
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Status explanation banner */}
        {isOrderExpired ? (
          <div className="rounded-2xl border border-border bg-muted/40 p-5 shadow-sm flex items-start gap-3">
            <XCircle className="h-5 w-5 text-muted-foreground shrink-0 mt-0.5" />
            <div className="text-xs space-y-1">
              <p className="font-bold text-foreground text-sm">Expired — payment window closed</p>
              <p className="text-muted-foreground">
                The payment reservation window for this order has elapsed and reserved stock has been released. No payment can be made for this order. You may add items back to your cart to create a new order.
              </p>
              <div className="pt-2">
                <Link
                  to="/products"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground font-semibold text-xs hover:bg-primary/90 transition shadow-sm"
                >
                  <ShoppingBag size={13} />
                  Browse Catalog
                </Link>
              </div>
            </div>
          </div>
        ) : order.paymentStatus === 'PENDING' ? (
          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-5 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="space-y-1 text-center sm:text-left">
              <div className="flex items-center justify-center sm:justify-start gap-2 text-amber-600 dark:text-amber-400 font-bold text-sm">
                <Clock size={16} />
                <span>Payment Window Open</span>
              </div>
              <p className="text-xs text-muted-foreground">
                {timeRemaining}. Your items and stock are temporarily held.
              </p>
            </div>

            <button
              type="button"
              disabled
              className="w-full sm:w-auto shrink-0 flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-xs font-bold opacity-50 cursor-not-allowed shadow-sm"
            >
              <CreditCard size={14} />
              <span>Pay Now (Coming Soon)</span>
            </button>
          </div>
        ) : null}

        {/* Order Details & Summary Card */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-6">
          <h2 className="text-base font-bold text-foreground border-b border-border pb-3">
            Purchased Items ({order.items.length})
          </h2>

          <div className="divide-y divide-border">
            {order.items.map(item => (
              <div key={item.orderItemId} className="py-4 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center shrink-0 border border-border">
                    {item.productType === 'VIDEO' ? (
                      <Video size={18} className="text-primary" />
                    ) : (
                      <FileText size={18} className="text-primary" />
                    )}
                  </div>
                  <div>
                    <div className="font-semibold text-foreground text-xs sm:text-sm">{item.productName}</div>
                    <div className="text-[11px] text-muted-foreground flex items-center gap-2 mt-0.5">
                      <span className="capitalize">{item.productType.toLowerCase().replace('_', ' ')}</span>
                      <span>&bull;</span>
                      <span>Qty: {item.quantity}</span>
                      <span>&bull;</span>
                      <span>{formatLKR(item.unitPrice)} each</span>
                    </div>
                  </div>
                </div>

                <div className="text-right font-bold text-xs sm:text-sm text-foreground">
                  {formatLKR(item.subtotal)}
                </div>
              </div>
            ))}
          </div>

          {/* Shipping Snapshot Card */}
          {order.shipRecipientName ? (
            <div className="rounded-xl border border-border bg-muted/20 p-4 space-y-1 text-xs">
              <div className="flex items-center gap-1.5 font-bold text-foreground mb-1">
                <MapPin size={14} className="text-primary" />
                Shipping Details (Historical Snapshot)
              </div>
              <div className="font-semibold text-foreground">{order.shipRecipientName}</div>
              <div className="text-muted-foreground">{order.shipPhone}</div>
              <div className="text-muted-foreground">
                {order.shipAddressLine1}
                {order.shipAddressLine2 ? `, ${order.shipAddressLine2}` : ''}
              </div>
              <div className="text-muted-foreground">
                {order.shipCity}, {order.shipDistrict} {order.shipPostalCode}
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-border bg-muted/20 p-4 flex items-center gap-2 text-xs text-muted-foreground">
              <ShieldCheck size={16} className="text-emerald-500 shrink-0" />
              <span>Digital Order — no shipping address required.</span>
            </div>
          )}

          {/* Financial Breakdown */}
          <div className="border-t border-border pt-4 space-y-2 text-xs">
            <div className="flex justify-between text-muted-foreground">
              <span>Subtotal</span>
              <span className="font-semibold text-foreground">{formatLKR(order.subtotal)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Delivery Fee</span>
              <span className="font-semibold text-foreground">
                {order.deliveryFee > 0 ? formatLKR(order.deliveryFee) : 'Free'}
              </span>
            </div>
            <div className="border-t border-border pt-2 flex justify-between text-base font-extrabold text-foreground">
              <span>Total Amount</span>
              <span className="text-primary">{formatLKR(order.totalAmount)}</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
