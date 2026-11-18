import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  getAdminOrderDetail,
  updateFulfillmentStatus,
  resolvePaymentReview,
  refundPayment,
  type AdminOrderDetail,
  type AdminPaymentDetail
} from '../../../api/adminCommerceApi';
import {
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Truck,
  MapPin,
  FileText,
  Video,
  CreditCard,
  Loader2,
  ShieldCheck,
  AlertTriangle,
  RotateCcw,
  Check,
  Info
} from 'lucide-react';

export default function AdminOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<AdminOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fulfillment transition form state
  const [trackingNoteInput, setTrackingNoteInput] = useState('');
  const [isUpdatingFulfillment, setIsUpdatingFulfillment] = useState(false);
  const [fulfillmentError, setFulfillmentError] = useState<string | null>(null);
  const [showFulfillmentConfirm, setShowFulfillmentConfirm] = useState(false);

  // Payment review modal / action state
  const [activePaymentModal, setActivePaymentModal] = useState<{
    payment: AdminPaymentDetail;
    action: 'resolve' | 'refund';
  } | null>(null);
  const [reviewNoteInput, setReviewNoteInput] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [reviewActionError, setReviewActionError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadOrderDetail(parseInt(id, 10));
    }
  }, [id]);

  async function loadOrderDetail(orderId: number) {
    setLoading(true);
    setError(null);
    try {
      const data = await getAdminOrderDetail(orderId);
      setOrder(data);
      if (data.trackingNote) {
        setTrackingNoteInput(data.trackingNote);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load order detail.');
    } finally {
      setLoading(false);
    }
  }

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  // Next fulfillment target calculation
  function getNextFulfillmentTarget(): string | null {
    if (!order) return null;
    if (order.paymentStatus !== 'PAID') return null;
    if (order.fulfillmentStatus === 'NOT_REQUIRED') return null;
    if (order.fulfillmentStatus === 'PENDING') return 'PROCESSING';
    if (order.fulfillmentStatus === 'PROCESSING') return 'SHIPPED';
    if (order.fulfillmentStatus === 'SHIPPED') return 'DELIVERED';
    return null;
  }

  const nextStatus = getNextFulfillmentTarget();

  async function handleFulfillmentSubmit() {
    if (!order || !nextStatus) return;
    setIsUpdatingFulfillment(true);
    setFulfillmentError(null);
    try {
      const updated = await updateFulfillmentStatus(
        order.orderId,
        nextStatus,
        nextStatus === 'SHIPPED' ? trackingNoteInput : undefined
      );
      setOrder(updated);
      setShowFulfillmentConfirm(false);
    } catch (err: any) {
      setFulfillmentError(err.message || 'Failed to transition fulfillment status.');
    } finally {
      setIsUpdatingFulfillment(false);
    }
  }

  async function handlePaymentReviewAction() {
    if (!activePaymentModal || !reviewNoteInput.trim()) {
      setReviewActionError('An explanation note is required.');
      return;
    }
    setIsSubmittingReview(true);
    setReviewActionError(null);
    try {
      if (activePaymentModal.action === 'resolve') {
        await resolvePaymentReview(activePaymentModal.payment.paymentId, reviewNoteInput.trim());
      } else {
        await refundPayment(activePaymentModal.payment.paymentId, reviewNoteInput.trim());
      }
      // Reload order details to refresh payments and statuses
      if (order) {
        await loadOrderDetail(order.orderId);
      }
      setActivePaymentModal(null);
      setReviewNoteInput('');
    } catch (err: any) {
      setReviewActionError(err.message || `Failed to ${activePaymentModal.action} payment.`);
    } finally {
      setIsSubmittingReview(false);
    }
  }

  function getPlainReviewReason(reason: string | null) {
    if (!reason) return 'Administrative attention required.';
    switch (reason) {
      case 'LATE_PAYMENT_NOT_FULFILLABLE':
        return 'Payment arrived after the order expired and stock could not be restored. Refund in PayHere, then mark as refunded, or fulfil manually and mark as resolved.';
      case 'AMOUNT_MISMATCH':
        return 'Payment amount received does not match the order total amount. Verify the transaction in the PayHere dashboard.';
      case 'CHARGEBACK':
        return 'A chargeback or payment reversal was reported by the payment provider. Review dispute before taking action.';
      default:
        return reason;
    }
  }

  if (loading) {
    return (
      <div className="p-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-xs font-medium">Loading order details...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="space-y-4">
        <Link
          to="/admin/orders"
          className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition"
        >
          <ArrowLeft size={14} />
          <span>Back to Orders</span>
        </Link>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-6 text-destructive text-xs space-y-2">
          <p className="font-bold text-sm">Error Loading Order</p>
          <p>{error || 'Order was not found.'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Top Breadcrumb & Header */}
      <div className="space-y-2">
        <Link
          to="/admin/orders"
          className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition"
        >
          <ArrowLeft size={14} />
          <span>Back to Orders List</span>
        </Link>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-black text-foreground tracking-tight">
              Order #{order.orderId}
            </h1>
            <span className="text-xs text-muted-foreground font-mono">
              Created {new Date(order.createdAt).toLocaleString()}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-bold border capitalize bg-primary/10 text-primary border-primary/20">
              Payment: {order.paymentStatus}
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-bold border capitalize bg-muted text-muted-foreground border-border">
              Fulfillment: {order.fulfillmentStatus.replace('_', ' ')}
            </span>
          </div>
        </div>
      </div>

      {/* Fulfillment Action Bar */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <Truck size={18} className="text-primary" />
            <span className="font-bold text-foreground text-sm">Fulfillment Workflow</span>
          </div>
          <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-muted text-muted-foreground">
            Current: {order.fulfillmentStatus}
          </span>
        </div>

        {order.fulfillmentStatus === 'NOT_REQUIRED' ? (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <ShieldCheck size={16} className="text-emerald-500 shrink-0" />
            <span>This order contains only digital items. Physical fulfillment is not required.</span>
          </div>
        ) : order.paymentStatus !== 'PAID' ? (
          <div className="flex items-center gap-2 text-xs text-amber-500">
            <AlertTriangle size={16} className="shrink-0" />
            <span>Fulfillment transitions are locked until the order is PAID. Current status: {order.paymentStatus}.</span>
          </div>
        ) : order.fulfillmentStatus === 'DELIVERED' ? (
          <div className="flex items-center gap-2 text-xs text-emerald-500">
            <CheckCircle2 size={16} className="shrink-0" />
            <span>Order has been successfully DELIVERED. Fulfillment completed.</span>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold text-foreground">
                  Next Step: <strong className="text-primary">{nextStatus}</strong>
                </p>
                <p className="text-[11px] text-muted-foreground">
                  Fulfillment must proceed one step at a time: PENDING &rarr; PROCESSING &rarr; SHIPPED &rarr; DELIVERED.
                </p>
              </div>

              {!showFulfillmentConfirm && (
                <button
                  type="button"
                  onClick={() => setShowFulfillmentConfirm(true)}
                  className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm"
                >
                  Mark as {nextStatus}
                </button>
              )}
            </div>

            {/* Confirmation Box */}
            {showFulfillmentConfirm && (
              <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 space-y-3">
                <p className="text-xs font-semibold text-foreground">
                  Confirm transition to <span className="text-primary underline">{nextStatus}</span>?
                </p>

                {/* Tracking note field if moving to SHIPPED */}
                {nextStatus === 'SHIPPED' && (
                  <div className="space-y-1">
                    <label className="block text-[11px] font-semibold text-foreground">
                      Courier / Tracking Note (Optional, max 500 characters)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. PromptX Courier #TRK-984210"
                      value={trackingNoteInput}
                      onChange={e => setTrackingNoteInput(e.target.value)}
                      maxLength={500}
                      className="w-full px-3 py-1.5 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
                    />
                    <p className="text-[10px] text-muted-foreground">
                      This note will be displayed to the student on their order page.
                    </p>
                  </div>
                )}

                {fulfillmentError && (
                  <div className="p-3 rounded-xl bg-destructive/10 text-destructive text-xs border border-destructive/20 flex items-center gap-2">
                    <AlertCircle size={14} className="shrink-0" />
                    <span>{fulfillmentError}</span>
                  </div>
                )}

                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="button"
                    disabled={isUpdatingFulfillment}
                    onClick={handleFulfillmentSubmit}
                    className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm disabled:opacity-50 flex items-center gap-1.5"
                  >
                    {isUpdatingFulfillment ? (
                      <Loader2 size={13} className="animate-spin" />
                    ) : (
                      <Check size={13} />
                    )}
                    <span>Confirm & Mark as {nextStatus}</span>
                  </button>
                  <button
                    type="button"
                    disabled={isUpdatingFulfillment}
                    onClick={() => {
                      setShowFulfillmentConfirm(false);
                      setFulfillmentError(null);
                    }}
                    className="px-3 py-2 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:bg-muted hover:text-foreground transition"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Existing Tracking Note display if present */}
        {order.trackingNote && (
          <div className="rounded-xl border border-border bg-muted/20 p-3 text-xs space-y-0.5">
            <span className="font-semibold text-muted-foreground text-[10px] uppercase">
              Current Tracking Information
            </span>
            <p className="text-foreground font-mono font-bold">{order.trackingNote}</p>
          </div>
        )}
      </div>

      {/* Grid: Customer Details + Shipping Snapshot */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Customer Information */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-3">
          <h2 className="text-sm font-bold text-foreground border-b border-border pb-2">
            Customer Information
          </h2>
          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Name:</span>
              <span className="font-semibold text-foreground">{order.customerName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Email:</span>
              <span className="font-semibold text-foreground">{order.customerEmail}</span>
            </div>
            {order.customerPhone && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Phone:</span>
                <span className="font-semibold text-foreground">{order.customerPhone}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-muted-foreground">Order Expires At:</span>
              <span className="font-semibold text-foreground">
                {new Date(order.expiresAt).toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* Shipping Snapshot */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-3">
          <div className="flex items-center gap-1.5 font-bold text-foreground text-sm border-b border-border pb-2">
            <MapPin size={15} className="text-primary" />
            <span>Shipping Snapshot</span>
          </div>
          {order.shipRecipientName ? (
            <div className="space-y-1 text-xs text-muted-foreground">
              <div className="font-bold text-foreground">{order.shipRecipientName}</div>
              <div>{order.shipPhone}</div>
              <div>
                {order.shipAddressLine1}
                {order.shipAddressLine2 ? `, ${order.shipAddressLine2}` : ''}
              </div>
              <div>
                {order.shipCity}, {order.shipDistrict} {order.shipPostalCode}
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs text-muted-foreground pt-2">
              <ShieldCheck size={16} className="text-emerald-500" />
              <span>No physical shipping address (Digital course order).</span>
            </div>
          )}
        </div>
      </div>

      {/* Items Table */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <h2 className="text-sm font-bold text-foreground border-b border-border pb-2">
          Order Items ({order.items.length})
        </h2>
        <div className="divide-y divide-border text-xs">
          {order.items.map(item => (
            <div key={item.orderItemId} className="py-3 flex items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-lg bg-muted flex items-center justify-center border border-border shrink-0">
                  {item.productType === 'VIDEO' ? (
                    <Video size={16} className="text-primary" />
                  ) : (
                    <FileText size={16} className="text-primary" />
                  )}
                </div>
                <div>
                  <div className="font-bold text-foreground">{item.productName}</div>
                  <div className="text-[11px] text-muted-foreground capitalize">
                    {item.productType.toLowerCase().replace('_', ' ')} &bull; Qty: {item.quantity} &bull; {formatLKR(item.unitPrice)} each
                  </div>
                </div>
              </div>
              <div className="text-right font-bold text-foreground">
                {formatLKR(item.subtotal)}
              </div>
            </div>
          ))}
        </div>

        {/* Totals Summary */}
        <div className="border-t border-border pt-3 space-y-1.5 text-xs max-w-xs ml-auto">
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
          <div className="border-t border-border pt-1.5 flex justify-between text-sm font-black text-foreground">
            <span>Total</span>
            <span className="text-primary">{formatLKR(order.totalAmount)}</span>
          </div>
        </div>
      </div>

      {/* Payments Section */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <CreditCard size={18} className="text-primary" />
            <h2 className="text-sm font-bold text-foreground">
              Payments & Review Audit ({order.payments.length})
            </h2>
          </div>
        </div>

        {order.payments.length === 0 ? (
          <p className="text-xs text-muted-foreground py-4 text-center">
            No payments recorded yet for this order.
          </p>
        ) : (
          <div className="space-y-4">
            {order.payments.map(p => (
              <div
                key={p.paymentId}
                className={`rounded-xl border p-4 text-xs space-y-3 transition ${
                  p.needsReview
                    ? 'border-amber-500/40 bg-amber-500/5'
                    : 'border-border bg-muted/10'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-border/60 pb-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-mono font-bold text-foreground">
                      Payment #{p.paymentId}
                    </span>
                    <span className="text-muted-foreground">&bull;</span>
                    <span className="font-bold text-foreground">{formatLKR(p.amount)}</span>
                    <span className="text-muted-foreground">&bull;</span>
                    <span className="capitalize text-muted-foreground">{p.paymentMethod}</span>
                    <span className="text-muted-foreground">&bull;</span>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        p.paymentStatus === 'PAID'
                          ? 'bg-emerald-500/10 text-emerald-500'
                          : p.paymentStatus === 'REFUNDED'
                          ? 'bg-purple-500/10 text-purple-500'
                          : p.paymentStatus === 'FAILED'
                          ? 'bg-destructive/10 text-destructive'
                          : 'bg-amber-500/10 text-amber-500'
                      }`}
                    >
                      {p.paymentStatus}
                    </span>
                  </div>

                  <span className="text-[11px] text-muted-foreground font-mono">
                    {new Date(p.createdAt).toLocaleString()}
                  </span>
                </div>

                {/* Transaction Ref */}
                {p.transactionReference && (
                  <div className="text-[11px] text-muted-foreground font-mono">
                    Ref: <span className="text-foreground">{p.transactionReference}</span>
                    {p.paidAt && (
                      <span className="ml-2">
                        (Paid: {new Date(p.paidAt).toLocaleTimeString()})
                      </span>
                    )}
                  </div>
                )}

                {/* Flagged Review Alert */}
                {p.needsReview && (
                  <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-3.5 space-y-2">
                    <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-bold text-xs">
                      <AlertTriangle size={15} />
                      <span>Action Required: Flagged for Administrative Review</span>
                    </div>
                    <div className="text-[11px] text-muted-foreground space-y-1">
                      <p className="font-mono text-foreground font-bold">
                        Reason: {p.reviewReason || 'MANUAL_REVIEW'}
                      </p>
                      <p className="leading-relaxed">
                        {getPlainReviewReason(p.reviewReason)}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 pt-1 flex-wrap">
                      <button
                        type="button"
                        onClick={() => {
                          setActivePaymentModal({ payment: p, action: 'resolve' });
                          setReviewNoteInput('');
                          setReviewActionError(null);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm"
                      >
                        Mark Resolved
                      </button>
                      {p.paymentStatus === 'PAID' && (
                        <button
                          type="button"
                          onClick={() => {
                            setActivePaymentModal({ payment: p, action: 'refund' });
                            setReviewNoteInput('');
                            setReviewActionError(null);
                          }}
                          className="px-3 py-1.5 rounded-lg bg-destructive text-destructive-foreground text-xs font-bold hover:bg-destructive/90 transition shadow-sm"
                        >
                          Mark as Refunded
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {/* Review Audit History */}
                {p.reviewNote && (
                  <div className="rounded-lg border border-border bg-card p-3 text-[11px] space-y-1">
                    <div className="font-semibold text-foreground flex items-center gap-1.5">
                      <Info size={13} className="text-primary" />
                      <span>Review / Audit Note</span>
                      {p.reviewedAt && (
                        <span className="text-muted-foreground font-normal">
                          &bull; {new Date(p.reviewedAt).toLocaleString()}
                        </span>
                      )}
                    </div>
                    <p className="text-muted-foreground font-mono">{p.reviewNote}</p>
                    {p.refundedAt && (
                      <p className="text-purple-600 dark:text-purple-400 font-semibold mt-1">
                        Refund recorded at: {new Date(p.refundedAt).toLocaleString()}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Payment Action Modal (Resolve or Refund) */}
      {activePaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-bold text-foreground text-sm flex items-center gap-2">
                {activePaymentModal.action === 'resolve' ? (
                  <>
                    <CheckCircle2 size={16} className="text-primary" />
                    <span>Resolve Payment Review (Payment #{activePaymentModal.payment.paymentId})</span>
                  </>
                ) : (
                  <>
                    <RotateCcw size={16} className="text-destructive" />
                    <span>Record Payment Refund (Payment #{activePaymentModal.payment.paymentId})</span>
                  </>
                )}
              </h3>
              <button
                type="button"
                onClick={() => setActivePaymentModal(null)}
                className="text-muted-foreground hover:text-foreground text-xs"
              >
                &times;
              </button>
            </div>

            {/* Clear warning on refund action */}
            {activePaymentModal.action === 'refund' && (
              <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-600 dark:text-amber-400 space-y-1">
                <p className="font-bold flex items-center gap-1.5">
                  <AlertTriangle size={14} className="shrink-0" />
                  <span>Important Provider Warning</span>
                </p>
                <p className="text-[11px] leading-relaxed">
                  Do this only after issuing the refund in the PayHere dashboard. This operation records the refund in our system and revokes any student digital access.
                </p>
              </div>
            )}

            <div className="space-y-1.5 text-xs">
              <label className="block font-semibold text-foreground">
                Administrative Note (Required)
              </label>
              <textarea
                rows={3}
                placeholder={
                  activePaymentModal.action === 'resolve'
                    ? 'Explain how this flagged issue was resolved (e.g. verified with bank, manually packed)'
                    : 'Enter PayHere refund transaction details or notes...'
                }
                value={reviewNoteInput}
                onChange={e => setReviewNoteInput(e.target.value)}
                className="w-full p-2.5 text-xs rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>

            {reviewActionError && (
              <div className="p-3 rounded-xl bg-destructive/10 text-destructive text-xs border border-destructive/20 flex items-center gap-2">
                <AlertCircle size={14} className="shrink-0" />
                <span>{reviewActionError}</span>
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
              <button
                type="button"
                disabled={isSubmittingReview}
                onClick={() => setActivePaymentModal(null)}
                className="px-3 py-2 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:bg-muted hover:text-foreground transition"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isSubmittingReview || !reviewNoteInput.trim()}
                onClick={handlePaymentReviewAction}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition shadow-sm disabled:opacity-50 flex items-center gap-1.5 ${
                  activePaymentModal.action === 'refund'
                    ? 'bg-destructive text-destructive-foreground hover:bg-destructive/90'
                    : 'bg-primary text-primary-foreground hover:bg-primary/90'
                }`}
              >
                {isSubmittingReview && <Loader2 size={13} className="animate-spin" />}
                <span>
                  {activePaymentModal.action === 'resolve' ? 'Confirm Resolve' : 'Confirm Refund'}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
