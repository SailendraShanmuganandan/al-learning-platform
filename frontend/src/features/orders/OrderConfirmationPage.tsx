import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getOrder, createPayment, type Order } from '../../api/orderApi';
import {
  CheckCircle2,
  Clock,
  MapPin,
  ArrowRight,
  Package,
  FileText,
  Video,
  CreditCard,
  Loader2,
  AlertTriangle,
  AlertCircle
} from 'lucide-react';

export default function OrderConfirmationPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const [isExpired, setIsExpired] = useState(false);
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  useEffect(() => {
    const parsedOrderId = Number(orderId);
    if (!orderId || !Number.isSafeInteger(parsedOrderId) || parsedOrderId <= 0) {
      setError('Invalid order link.');
      setLoading(false);
      return;
    }
    loadOrder(parsedOrderId);
  }, [orderId]);

  async function loadOrder(orderId: number) {
    setLoading(true);
    try {
      const data = await getOrder(orderId);
      setOrder(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load order confirmation.');
    } finally {
      setLoading(false);
    }
  }

  async function handlePayNow() {
    if (!order || isSubmittingPayment || isExpired || order.paymentStatus !== 'PENDING') {
      return;
    }

    setIsSubmittingPayment(true);
    setPaymentError(null);

    try {
      const paymentData = await createPayment(order.orderId);

      const form = document.createElement('form');
      form.method = 'POST';
      form.action = paymentData.checkoutUrl;

      for (const [key, value] of Object.entries(paymentData.fields)) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        form.appendChild(input);
      }

      document.body.appendChild(form);
      form.submit();
    } catch (err: any) {
      setPaymentError(err.message || 'Failed to initiate payment.');
      setIsSubmittingPayment(false);
    }
  }

  // Countdown timer for order expiry
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
        setTimeRemaining(`${minutes}m ${seconds < 10 ? '0' : ''}${seconds}s remaining to complete payment`);
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
        <p className="text-sm text-muted-foreground">Loading order details...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4">
        <div className="rounded-2xl border border-border bg-card p-8 max-w-md text-center space-y-4">
          <AlertTriangle className="h-10 w-10 text-destructive mx-auto" />
          <h2 className="text-lg font-bold text-foreground">Order Not Found</h2>
          <p className="text-xs text-muted-foreground">{error || 'Could not find the requested order.'}</p>
          <Link
            to="/orders"
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition"
          >
            Go to My Orders
          </Link>
        </div>
      </div>
    );
  }

  const isFailedPayment = order.latestPaymentStatus === 'FAILED' || order.paymentStatus === 'FAILED';
  const isOrderExpired = order.paymentStatus === 'EXPIRED' || (order.paymentStatus === 'PENDING' && isExpired);
  const paymentStatusLabel = order.paymentStatus === 'PAID'
    ? 'Paid'
    : isOrderExpired
      ? 'Expired'
      : isFailedPayment
        ? 'Payment failed (retry available)'
        : 'Awaiting payment';
  const paymentStatusClass = order.paymentStatus === 'PAID'
    ? 'bg-emerald-500'
    : isOrderExpired
      ? 'bg-muted-foreground'
      : isFailedPayment
        ? 'bg-destructive'
        : 'bg-amber-500';

  return (
    <div className="min-h-screen bg-background flex flex-col py-10 px-4 sm:px-6">
      <div className="max-w-3xl w-full mx-auto space-y-8">
        {/* Celebration header */}
        <div className="text-center space-y-3">
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-500 ring-8 ring-emerald-500/5 mb-1">
            <CheckCircle2 size={36} />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
            Order #{order.orderId} Placed!
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground max-w-md mx-auto">
            Thank you for your order. Your stock and items have been reserved while we prepare your payment options.
          </p>

          {/* Status and countdown banner */}
          <div className="inline-flex flex-col sm:flex-row items-center gap-2 rounded-2xl border border-border bg-card px-5 py-3 shadow-sm mt-3">
            <div className="flex items-center gap-2">
              <span className={`flex h-2.5 w-2.5 rounded-full ${paymentStatusClass}`} />
              <span className="text-xs font-bold text-foreground uppercase tracking-wider">
                Status: {paymentStatusLabel}
              </span>
            </div>
            <span className="hidden sm:inline text-muted-foreground">&bull;</span>
            <div className="flex items-center gap-1.5 text-xs font-medium text-amber-600 dark:text-amber-400">
              <Clock size={13} />
              <span>{timeRemaining}</span>
            </div>
          </div>
        </div>

        {paymentError && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-destructive flex items-center gap-3 text-xs">
            <AlertCircle size={16} className="shrink-0" />
            <span>{paymentError}</span>
          </div>
        )}

        {/* Payment CTA Card */}
        {order.paymentStatus === 'PENDING' && !isExpired && (
          <div className="rounded-2xl border border-primary/20 bg-primary/5 p-6 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="space-y-1 text-center sm:text-left">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <CreditCard size={18} className="text-primary" />
                <h3 className="text-sm font-bold text-foreground">Pay with PayHere</h3>
              </div>
              <p className="text-xs text-muted-foreground">
                Pay securely via Credit Card, Debit Card or LankaQR.
              </p>
            </div>

            <button
              type="button"
              onClick={handlePayNow}
              disabled={isSubmittingPayment || isExpired}
              className="w-full sm:w-auto shrink-0 flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground px-6 py-3 text-xs font-bold hover:bg-primary/90 transition shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmittingPayment ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Redirecting to PayHere...</span>
                </>
              ) : (
                <>
                  <CreditCard size={14} />
                  <span>Pay Now</span>
                </>
              )}
            </button>
          </div>
        )}

        {/* Order Details card */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-6">
          <div className="flex items-center justify-between border-b border-border pb-4">
            <h2 className="text-base font-bold text-foreground">Order Summary</h2>
            <span className="text-xs text-muted-foreground">
              Placed on {new Date(order.createdAt).toLocaleDateString()} at{' '}
              {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>

          {/* Items breakdown */}
          <div className="divide-y divide-border">
            {order.items.map(item => (
              <div key={item.orderItemId} className="py-3 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0 border border-border">
                    {item.productType === 'VIDEO' ? (
                      <Video size={16} className="text-primary" />
                    ) : (
                      <FileText size={16} className="text-primary" />
                    )}
                  </div>
                  <div>
                    <div className="font-semibold text-foreground text-xs sm:text-sm">{item.productName}</div>
                    <div className="text-[11px] text-muted-foreground flex items-center gap-2 mt-0.5">
                      <span className="capitalize">{item.productType.toLowerCase().replace('_', ' ')}</span>
                      <span>&bull;</span>
                      <span>Qty: {item.quantity}</span>
                      <span>&bull;</span>
                      <span>{formatLKR(item.unitPrice)}</span>
                    </div>
                  </div>
                </div>

                <div className="text-right font-bold text-xs sm:text-sm text-foreground">
                  {formatLKR(item.subtotal)}
                </div>
              </div>
            ))}
          </div>

          {/* Shipping Address snapshot if present */}
          {order.shipRecipientName && (
            <div className="rounded-xl border border-border bg-muted/30 p-4 space-y-1 text-xs">
              <div className="flex items-center gap-1.5 font-bold text-foreground text-xs mb-1">
                <MapPin size={13} className="text-primary" />
                Shipping Snapshot
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
          )}

          {/* Financial Totals */}
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
            <div className="border-t border-border pt-2 flex justify-between text-sm font-extrabold text-foreground">
              <span>Total Amount</span>
              <span className="text-primary text-base">{formatLKR(order.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Action links */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
          <Link
            to="/orders"
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl border border-border bg-card px-5 py-2.5 text-xs font-semibold text-foreground hover:bg-muted transition"
          >
            <Package size={14} />
            View All My Orders
          </Link>
          <Link
            to="/products"
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-xs font-semibold hover:bg-primary/90 transition shadow-sm"
          >
            <span>Continue Browsing</span>
            <ArrowRight size={14} />
          </Link>
        </div>
      </div>
    </div>
  );
}
