import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import { getCart, type CartResponse } from '../../api/cartApi';
import { listAddresses, type AddressResponse } from '../../api/userApi';
import { createOrder } from '../../api/orderApi';
import { resendVerification } from '../../api/authApi';
import {
  ShoppingBag,
  ArrowLeft,
  MapPin,
  Check,
  AlertCircle,
  Loader2,
  FileText,
  Video,
  ShieldCheck,
  PlusCircle,
  CreditCard,
  Package,
  BookOpen,
  User,
  ShoppingCart,
  ExternalLink
} from 'lucide-react';

export default function CheckoutPage() {
  const { user, logout } = useAuth();
  const { fetchCart, itemCount } = useCart();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);

  // Error handling state
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<any>(null);

  // Email verification state
  const [resendingEmail, setResendingEmail] = useState(false);
  const [resendStatus, setResendStatus] = useState<string | null>(null);

  useEffect(() => {
    loadCheckoutData();
  }, []);

  async function loadCheckoutData() {
    setLoading(true);
    setErrorMessage(null);
    try {
      const [cartData, addressList] = await Promise.all([
        getCart(),
        listAddresses().catch(() => [])
      ]);

      setCart(cartData);
      setAddresses(addressList);

      // Pre-select default address, or the first one available
      if (addressList.length > 0) {
        const defaultAddr = addressList.find(a => a.isDefault) || addressList[0];
        setSelectedAddressId(defaultAddr.addressId);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to load checkout details.');
    } finally {
      setLoading(false);
    }
  }

  const hasPhysicalItems = Boolean(cart?.items.some(item => item.type === 'PHYSICAL_NOTE'));
  // Assume delivery flat fee of 150 LKR for physical items, or 0 for video-only
  const deliveryFee = hasPhysicalItems ? 150 : 0;
  const subtotal = cart?.subtotal ?? 0;
  const totalAmount = subtotal + deliveryFee;

  function formatLKR(val: number) {
    return `Rs. ${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  async function handlePlaceOrder() {
    if (submitting) return;

    if (hasPhysicalItems && !selectedAddressId) {
      setErrorCode('ADDRESS_REQUIRED');
      setErrorMessage('Please select a delivery address before placing your order.');
      return;
    }

    setSubmitting(true);
    setErrorMessage(null);
    setErrorCode(null);
    setErrorDetails(null);

    try {
      const payload = {
        addressId: hasPhysicalItems ? selectedAddressId : null,
      };

      const createdOrder = await createOrder(payload);

      // Refresh cart in context
      await fetchCart();

      // Navigate to order confirmation page
      navigate(`/orders/${createdOrder.orderId}/confirmation`);
    } catch (err: any) {
      const code = err.data?.code || err.code;
      setErrorCode(code);
      setErrorMessage(err.message || 'Failed to place order.');
      setErrorDetails(err.data || {});
    } finally {
      setSubmitting(false);
    }
  }

  async function handleResendVerification() {
    setResendingEmail(true);
    setResendStatus(null);
    try {
      const res = await resendVerification();
      setResendStatus(res.message || 'Verification link sent to your email.');
    } catch {
      setResendStatus('Failed to send verification email. Please try again.');
    } finally {
      setResendingEmail(false);
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary mb-3" />
        <p className="text-sm text-muted-foreground">Preparing your checkout...</p>
      </div>
    );
  }

  const isCartEmpty = !cart || cart.items.length === 0;

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
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
          >
            <Package size={13} />
            <span className="hidden sm:inline">My Orders</span>
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-8">
        <div className="mb-6">
          <Link
            to="/cart"
            id="back-to-cart-link"
            className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground transition mb-2"
          >
            <ArrowLeft size={14} />
            Back to Cart
          </Link>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Checkout</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Review your order details and delivery address.
          </p>
        </div>

        {/* Global Error Banner */}
        {errorCode && (
          <div className="mb-6 rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-destructive space-y-2 animate-in fade-in">
            <div className="flex items-start gap-3">
              <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
              <div className="flex-1 text-xs">
                <p className="font-semibold text-sm mb-1">
                  {errorCode === 'PRICE_CHANGED' && 'Price Updated in Cart'}
                  {errorCode === 'INSUFFICIENT_STOCK' && 'Stock Unavailable'}
                  {errorCode === 'PRODUCT_UNAVAILABLE' && 'Product Unavailable'}
                  {errorCode === 'VIDEO_ALREADY_OWNED' && 'Video Already Purchased'}
                  {errorCode === 'VIDEO_PENDING_ORDER' && 'Video Pending in Existing Order'}
                  {errorCode === 'ADDRESS_REQUIRED' && 'Shipping Address Required'}
                  {errorCode === 'EMPTY_CART' && 'Empty Cart'}
                  {errorCode === 'EMAIL_NOT_VERIFIED' && 'Email Verification Required'}
                  {!['PRICE_CHANGED', 'INSUFFICIENT_STOCK', 'PRODUCT_UNAVAILABLE', 'VIDEO_ALREADY_OWNED', 'VIDEO_PENDING_ORDER', 'ADDRESS_REQUIRED', 'EMPTY_CART', 'EMAIL_NOT_VERIFIED'].includes(errorCode) && 'Unable to Place Order'}
                </p>
                <p className="text-destructive/90">{errorMessage}</p>

                {/* Contextual actions per error code */}
                {errorCode === 'PRICE_CHANGED' && (
                  <div className="mt-3 flex items-center gap-3">
                    <Link
                      to="/cart"
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-destructive text-destructive-foreground font-semibold text-xs hover:opacity-90 transition"
                    >
                      <ShoppingCart size={13} />
                      Return to Cart to Review
                    </Link>
                  </div>
                )}

                {errorCode === 'VIDEO_PENDING_ORDER' && errorDetails?.pendingOrderId && (
                  <div className="mt-3">
                    <Link
                      to={`/orders/${errorDetails.pendingOrderId}`}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-destructive text-destructive-foreground font-semibold text-xs hover:opacity-90 transition"
                    >
                      <ExternalLink size={13} />
                      View Pending Order #{errorDetails.pendingOrderId}
                    </Link>
                  </div>
                )}

                {errorCode === 'EMAIL_NOT_VERIFIED' && (
                  <div className="mt-3 flex items-center gap-3">
                    <button
                      type="button"
                      disabled={resendingEmail}
                      onClick={handleResendVerification}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-destructive text-destructive-foreground font-semibold text-xs hover:opacity-90 transition disabled:opacity-50"
                    >
                      {resendingEmail && <Loader2 size={12} className="animate-spin" />}
                      Resend Verification Email
                    </button>
                    {resendStatus && <span className="text-xs font-medium text-foreground">{resendStatus}</span>}
                  </div>
                )}

                {(errorCode === 'INSUFFICIENT_STOCK' || errorCode === 'PRODUCT_UNAVAILABLE') && (
                  <div className="mt-3">
                    <Link
                      to="/cart"
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-destructive text-destructive-foreground font-semibold text-xs hover:opacity-90 transition"
                    >
                      <ShoppingCart size={13} />
                      Update Your Cart
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {isCartEmpty ? (
          <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center max-w-lg mx-auto space-y-4">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <ShoppingBag size={24} />
            </div>
            <h2 className="text-lg font-bold text-foreground">Your cart is empty</h2>
            <p className="text-xs text-muted-foreground">
              Add revision notes or video lessons from our catalog before proceeding to checkout.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-xs font-semibold hover:bg-primary/90 transition shadow-sm"
            >
              Browse Catalog
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left 2 cols: Delivery Address & Cart Items */}
            <div className="lg:col-span-2 space-y-6">
              {/* Shipping Address Section (Conditional on physical items) */}
              {hasPhysicalItems ? (
                <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-4">
                  <div className="flex items-center justify-between border-b border-border pb-3">
                    <div className="flex items-center gap-2">
                      <MapPin size={18} className="text-primary" />
                      <h2 className="text-base font-bold text-foreground">Shipping Address</h2>
                    </div>
                    <Link
                      to="/addresses"
                      className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                    >
                      <PlusCircle size={13} />
                      Manage Addresses
                    </Link>
                  </div>

                  {addresses.length === 0 ? (
                    <div className="rounded-xl border border-dashed border-border p-6 text-center space-y-3">
                      <p className="text-xs text-muted-foreground">
                        You have not saved any delivery addresses yet. A shipping address is required for printed notes.
                      </p>
                      <Link
                        to="/addresses"
                        id="add-address-shortcut"
                        className="inline-flex items-center gap-2 rounded-lg bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:bg-primary/90 transition"
                      >
                        <PlusCircle size={14} />
                        Add a New Address
                      </Link>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3" role="radiogroup">
                      {addresses.map(addr => {
                        const isSelected = selectedAddressId === addr.addressId;
                        return (
                          <div
                            key={addr.addressId}
                            onClick={() => setSelectedAddressId(addr.addressId)}
                            className={`cursor-pointer rounded-xl border p-4 transition text-xs space-y-1 relative ${
                              isSelected
                                ? 'border-primary bg-primary/5 ring-1 ring-primary'
                                : 'border-border bg-card hover:border-primary/40'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="font-bold text-foreground text-sm">{addr.recipientName}</span>
                              {isSelected ? (
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-primary-foreground">
                                  <Check size={12} />
                                </span>
                              ) : (
                                <span className="h-5 w-5 rounded-full border border-muted-foreground/30" />
                              )}
                            </div>
                            <div className="text-muted-foreground text-[11px]">{addr.phone}</div>
                            <div className="text-muted-foreground mt-2 line-clamp-2">
                              {addr.addressLine1}
                              {addr.addressLine2 ? `, ${addr.addressLine2}` : ''}
                              <br />
                              {addr.city}, {addr.district} {addr.postalCode}
                            </div>
                            {addr.isDefault && (
                              <span className="inline-block mt-2 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-muted text-muted-foreground">
                                Default
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center gap-3 text-xs text-muted-foreground">
                  <ShieldCheck size={20} className="text-emerald-500 shrink-0" />
                  <div>
                    <span className="font-bold text-foreground block">Digital-Only Order</span>
                    All items in your cart are online videos. No physical shipping address or delivery fee is required.
                  </div>
                </div>
              )}

              {/* Order Items review */}
              <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-4">
                <h2 className="text-base font-bold text-foreground border-b border-border pb-3">
                  Items in Order ({cart.items.length})
                </h2>

                <div className="divide-y divide-border">
                  {cart.items.map(item => (
                    <div key={item.id} className="py-3 flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div className="h-12 w-12 rounded-lg bg-muted flex items-center justify-center shrink-0 border border-border overflow-hidden">
                          {item.thumbnailUrl ? (
                            <img src={item.thumbnailUrl} alt={item.name} className="h-full w-full object-cover" />
                          ) : item.type === 'VIDEO' ? (
                            <Video size={18} className="text-primary" />
                          ) : (
                            <FileText size={18} className="text-primary" />
                          )}
                        </div>
                        <div>
                          <div className="font-semibold text-foreground text-xs sm:text-sm">{item.name}</div>
                          <div className="text-[11px] text-muted-foreground flex items-center gap-2 mt-0.5">
                            <span className="capitalize">{item.type.toLowerCase().replace('_', ' ')}</span>
                            <span>&bull;</span>
                            <span>Qty: {item.quantity}</span>
                            <span>&bull;</span>
                            <span>{formatLKR(item.unitPrice)} each</span>
                          </div>
                        </div>
                      </div>

                      <div className="text-right font-bold text-xs sm:text-sm text-foreground">
                        {formatLKR(item.lineTotal)}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Right col: Order summary & Place Order CTA */}
            <div className="lg:col-span-1">
              <div className="sticky top-20 rounded-2xl border border-border bg-card p-6 shadow-sm space-y-6">
                <h2 className="text-base font-bold text-foreground border-b border-border pb-3">
                  Order Summary
                </h2>

                <div className="space-y-3 text-xs">
                  <div className="flex justify-between text-muted-foreground">
                    <span>Items Subtotal</span>
                    <span className="font-semibold text-foreground">{formatLKR(subtotal)}</span>
                  </div>

                  <div className="flex justify-between text-muted-foreground">
                    <span>Delivery Fee</span>
                    <span className="font-semibold text-foreground">
                      {deliveryFee > 0 ? formatLKR(deliveryFee) : 'Free'}
                    </span>
                  </div>

                  <div className="border-t border-border pt-3 flex justify-between text-sm font-bold text-foreground">
                    <span>Total Amount</span>
                    <span id="checkout-total-amount" className="text-primary text-base">
                      {formatLKR(totalAmount)}
                    </span>
                  </div>
                </div>

                <div className="space-y-3 pt-2">
                  <button
                    type="button"
                    id="place-order-btn"
                    disabled={submitting || (hasPhysicalItems && !selectedAddressId)}
                    onClick={handlePlaceOrder}
                    className="w-full flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground py-3 text-sm font-semibold hover:bg-primary/90 transition shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submitting ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        <span>Placing Order...</span>
                      </>
                    ) : (
                      <>
                        <CreditCard size={16} />
                        <span>Place Order</span>
                      </>
                    )}
                  </button>

                  <p className="text-[11px] text-center text-muted-foreground">
                    Payment will be completed in the next step. Your order will be reserved for 30 minutes.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
