import { useState, useEffect, useRef } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import { getOrder, createPayment, type Order } from '../../api/orderApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
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
  AlertTriangle,
  PlayCircle,
  Truck,
  ShoppingBag
} from 'lucide-react';

export default function OrderDetailPage() {
  const { orderId, id } = useParams<{ orderId?: string; id?: string }>();
  const effectiveId = orderId || id;
  const [searchParams] = useSearchParams();
  const paymentQuery = searchParams.get('payment'); // 'return' | 'cancelled' | null

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const [isExpired, setIsExpired] = useState(false);

  // Payment state
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  // Return polling state
  const [isPolling, setIsPolling] = useState(false);
  const [pollResult, setPollResult] = useState<'pending' | 'confirmed' | 'unconfirmed' | null>(null);
  const pollingAttempts = useRef(0);

  useEffect(() => {
    if (!effectiveId) {
      setError('Order ID is missing.');
      setLoading(false);
      return;
    }
    const parsed = parseInt(effectiveId, 10);
    if (isNaN(parsed)) {
      setError('Invalid order ID.');
      setLoading(false);
      return;
    }
    loadOrder(parsed);
  }, [effectiveId]);

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

  // Handle return flow polling (?payment=return)
  useEffect(() => {
    if (paymentQuery !== 'return' || !order) return;

    if (order.paymentStatus === 'PAID') {
      setPollResult('confirmed');
      setIsPolling(false);
      return;
    }

    setIsPolling(true);
    setPollResult('pending');
    pollingAttempts.current = 0;

    const pollInterval = setInterval(async () => {
      pollingAttempts.current += 1;
      try {
        const fresh = await getOrder(order.orderId);
        setOrder(fresh);

        if (fresh.paymentStatus === 'PAID') {
          setPollResult('confirmed');
          setIsPolling(false);
          clearInterval(pollInterval);
          return;
        }

        // 15 attempts * 2s = 30 seconds
        if (pollingAttempts.current >= 15) {
          setPollResult('unconfirmed');
          setIsPolling(false);
          clearInterval(pollInterval);
        }
      } catch (err) {
        if (pollingAttempts.current >= 15) {
          setPollResult('unconfirmed');
          setIsPolling(false);
          clearInterval(pollInterval);
        }
      }
    }, 2000);

    return () => clearInterval(pollInterval);
  }, [paymentQuery, order?.orderId]);

  // Countdown timer for pending order
  useEffect(() => {
    if (!order?.expiresAt) return;

    if (order.paymentStatus !== 'PENDING') {
      setIsExpired(order.paymentStatus === 'EXPIRED');
      setTimeRemaining(order.paymentStatus === 'EXPIRED' ? 'Expired — payment window closed' : '');
      return;
    }

    function updateRemaining() {
      const expiry = new Date(order!.expiresAt).getTime();
      const now = Date.now();
      const diff = expiry - now;

      if (diff <= 0) {
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

  async function handlePayNow() {
    if (!order || isSubmittingPayment || isOrderExpired || order.paymentStatus !== 'PENDING') {
      return;
    }

    setIsSubmittingPayment(true);
    setPaymentError(null);

    try {
      const paymentData = await createPayment(order.orderId);

      // Build hidden HTML form and submit POST to checkoutUrl
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
      setPaymentError(err.message || 'Failed to initiate payment. Please try again.');
      setIsSubmittingPayment(false);
    }
  }

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

  const isOrderExpired = order.paymentStatus === 'EXPIRED' || (order.paymentStatus === 'PENDING' && isExpired);
  const hasVideoItems = order.items.some(item => item.productType === 'VIDEO');
  const isFailedPayment = order.latestPaymentStatus === 'FAILED' || order.paymentStatus === 'FAILED';

  return (
    <PageContainer>
      <div className="space-y-6">
        <PageHeader
          title={`Order #${order.orderId}`}
          description="Review order summary, items, payment and delivery status."
          breadcrumb={[
            { label: 'My Orders', path: '/orders' },
            { label: `Order #${order.orderId}` },
          ]}
        />
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
            <div className="flex items-center gap-2 flex-wrap">
              {order.paymentStatus === 'PAID' && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                  <CheckCircle2 size={13} />
                  Paid
                </span>
              )}
              {order.paymentStatus === 'PENDING' && !isOrderExpired && isFailedPayment && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-destructive/10 text-destructive border border-destructive/20">
                  <AlertCircle size={13} />
                  Payment failed (retry available)
                </span>
              )}
              {order.paymentStatus === 'PENDING' && !isOrderExpired && !isFailedPayment && (
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

        {/* Payment Error Alert */}
        {paymentError && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-destructive flex items-center gap-3 text-xs">
            <AlertCircle size={16} className="shrink-0" />
            <span>{paymentError}</span>
          </div>
        )}

        {/* Return flow banners (?payment=return) */}
        {paymentQuery === 'return' && (
          <div>
            {isPolling && (
              <div className="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-sm flex items-center gap-3">
                <Loader2 className="h-5 w-5 animate-spin text-primary shrink-0" />
                <div className="text-xs">
                  <p className="font-bold text-foreground text-sm">Verifying payment with PayHere...</p>
                  <p className="text-muted-foreground mt-0.5">
                    Please wait a moment while we confirm receipt of your payment.
                  </p>
                </div>
              </div>
            )}
            {!isPolling && pollResult === 'confirmed' && (
              <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-5 shadow-sm flex items-center gap-3">
                <CheckCircle2 className="h-6 w-6 text-emerald-500 shrink-0" />
                <div className="text-xs">
                  <p className="font-bold text-emerald-600 dark:text-emerald-400 text-sm">Payment confirmed</p>
                  <p className="text-muted-foreground mt-0.5">
                    Your payment was successfully received and verified!
                  </p>
                </div>
              </div>
            )}
            {!isPolling && pollResult === 'unconfirmed' && (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-5 shadow-sm flex items-center gap-3">
                <AlertTriangle className="h-6 w-6 text-amber-500 shrink-0" />
                <div className="text-xs">
                  <p className="font-bold text-amber-600 dark:text-amber-400 text-sm">Payment status pending</p>
                  <p className="text-muted-foreground mt-0.5">
                    We haven't received confirmation yet. Please check My Orders in a minute and do NOT pay again.
                  </p>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Cancelled flow banner (?payment=cancelled) */}
        {paymentQuery === 'cancelled' && (
          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-5 shadow-sm flex items-center gap-3">
            <AlertCircle className="h-5 w-5 text-amber-500 shrink-0" />
            <div className="text-xs">
              <p className="font-bold text-foreground text-sm">Payment cancelled</p>
              <p className="text-muted-foreground mt-0.5">
                The payment process was cancelled on PayHere. You can try again below while your order reservation is active.
              </p>
            </div>
          </div>
        )}

        {/* Status explanation / Payment CTA banner */}
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
              onClick={handlePayNow}
              disabled={isSubmittingPayment || isOrderExpired}
              className="w-full sm:w-auto shrink-0 flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground px-6 py-3 text-xs font-bold hover:bg-primary/90 transition shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmittingPayment ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Redirecting to PayHere...</span>
                </>
              ) : (
                <>
                  <CreditCard size={15} />
                  <span>Pay Now</span>
                </>
              )}
            </button>
          </div>
        ) : null}

        {/* Video Course Access Banner for PAID Orders */}
        {order.paymentStatus === 'PAID' && hasVideoItems && (
          <div className="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="space-y-1 text-center sm:text-left">
              <div className="flex items-center justify-center sm:justify-start gap-2 text-primary font-bold text-sm">
                <PlayCircle size={18} />
                <span>Video Course Access Unlocked</span>
              </div>
              <p className="text-xs text-muted-foreground">
                You now own access to the video lessons in this order.
              </p>
            </div>

            <Link
              to="/my-videos"
              id="order-detail-my-videos-btn"
              className="w-full sm:w-auto shrink-0 flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 px-5 py-2.5 text-xs font-bold transition shadow-sm"
            >
              <Video size={14} />
              <span>Go to My Videos</span>
            </Link>
          </div>
        )}

        {/* Physical Item Fulfillment Progress (Processing -> Shipped -> Delivered) */}
        {order.fulfillmentStatus !== 'NOT_REQUIRED' && (
          <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Truck className="h-5 w-5 text-primary" />
                <h2 className="text-base font-bold text-foreground">Delivery Progress</h2>
              </div>
              <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary border border-primary/20 capitalize">
                {order.fulfillmentStatus.toLowerCase().replace('_', ' ')}
              </span>
            </div>

            {/* Step Progress Bar */}
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              {/* Step 1: Processing */}
              <div className="flex flex-col items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mb-2 transition ${
                    order.fulfillmentStatus === 'DELIVERED' || order.fulfillmentStatus === 'SHIPPED'
                      ? 'bg-emerald-500 text-white shadow-sm'
                      : order.fulfillmentStatus === 'PROCESSING' || order.fulfillmentStatus === 'PENDING'
                      ? 'bg-primary text-primary-foreground shadow-sm ring-4 ring-primary/20'
                      : 'bg-muted text-muted-foreground'
                  }`}
                >
                  {order.fulfillmentStatus === 'DELIVERED' || order.fulfillmentStatus === 'SHIPPED' ? (
                    <CheckCircle2 size={16} />
                  ) : (
                    '1'
                  )}
                </div>
                <span className="font-semibold text-foreground">Processing</span>
                <span className="text-[10px] text-muted-foreground mt-0.5">
                  {order.fulfillmentStatus === 'PENDING' ? 'Queued' : 'In Preparation'}
                </span>
              </div>

              {/* Step 2: Shipped */}
              <div className="flex flex-col items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mb-2 transition ${
                    order.fulfillmentStatus === 'DELIVERED'
                      ? 'bg-emerald-500 text-white shadow-sm'
                      : order.fulfillmentStatus === 'SHIPPED'
                      ? 'bg-primary text-primary-foreground shadow-sm ring-4 ring-primary/20'
                      : 'bg-muted text-muted-foreground'
                  }`}
                >
                  {order.fulfillmentStatus === 'DELIVERED' ? <CheckCircle2 size={16} /> : '2'}
                </div>
                <span className="font-semibold text-foreground">Shipped</span>
                <span className="text-[10px] text-muted-foreground mt-0.5">
                  {order.fulfillmentStatus === 'SHIPPED' ? 'In Transit' : order.fulfillmentStatus === 'DELIVERED' ? 'Dispatched' : 'Awaiting Dispatch'}
                </span>
              </div>

              {/* Step 3: Delivered */}
              <div className="flex flex-col items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mb-2 transition ${
                    order.fulfillmentStatus === 'DELIVERED'
                      ? 'bg-emerald-500 text-white shadow-sm ring-4 ring-emerald-500/20'
                      : 'bg-muted text-muted-foreground'
                  }`}
                >
                  {order.fulfillmentStatus === 'DELIVERED' ? <CheckCircle2 size={16} /> : '3'}
                </div>
                <span className="font-semibold text-foreground">Delivered</span>
                <span className="text-[10px] text-muted-foreground mt-0.5">
                  {order.fulfillmentStatus === 'DELIVERED' ? 'Completed' : 'Pending Delivery'}
                </span>
              </div>
            </div>

            {/* Tracking Note */}
            {order.trackingNote && (
              <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 text-xs space-y-1 mt-3">
                <div className="font-semibold text-primary flex items-center gap-1.5">
                  <Package size={14} />
                  <span>Courier Tracking Reference</span>
                </div>
                <p className="text-foreground font-mono text-sm font-semibold">{order.trackingNote}</p>
              </div>
            )}
          </div>
        )}

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
      </div>
    </PageContainer>
  );
}
