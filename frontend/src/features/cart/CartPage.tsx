import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import {
  ShoppingCart,
  Trash2,
  AlertTriangle,
  ArrowRight,
  BookOpen,
  ShoppingBag,
  User,
  MapPin,
  Shield,
  LogOut,
  FileText,
  Video,
  Plus,
  Minus,
  Loader2,
  PackageX,
} from 'lucide-react';

function formatLKR(amount: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
  }).format(amount);
}

export default function CartPage() {
  const { user, isAuthenticated, logout } = useAuth();
  const { cart, isLoading, updateQuantity, removeItem, clear, itemCount } = useCart();
  const navigate = useNavigate();

  const [busyItemId, setBusyItemId] = useState<number | null>(null);
  const [clearing, setClearing] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const items = cart?.items || [];
  const subtotal = cart?.subtotal || 0;

  async function handleQuantityChange(itemId: number, currentQty: number, delta: number) {
    const nextQty = currentQty + delta;
    if (nextQty < 1) return;
    setBusyItemId(itemId);
    setErrorMessage(null);
    try {
      await updateQuantity(itemId, nextQty);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to update item quantity.');
    } finally {
      setBusyItemId(null);
    }
  }

  async function handleRemove(itemId: number) {
    setBusyItemId(itemId);
    setErrorMessage(null);
    try {
      await removeItem(itemId);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to remove item.');
    } finally {
      setBusyItemId(null);
    }
  }

  async function handleClearCart() {
    setClearing(true);
    setErrorMessage(null);
    try {
      await clear();
      setShowClearConfirm(false);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to clear cart.');
    } finally {
      setClearing(false);
    }
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Navbar */}
      <header className="border-b border-border bg-card px-6 py-3.5 sticky top-0 z-20 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-3">
            <div
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold"
              aria-hidden
            >
              AL
            </div>
            <span className="font-semibold text-foreground text-sm">AL Learning Platform</span>
          </Link>
        </div>

        <nav className="flex items-center gap-2">
          <Link
            to="/subjects"
            id="nav-subjects-link"
            className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted/80 transition"
          >
            <BookOpen size={13} />
            <span>Subjects</span>
          </Link>
          <Link
            to="/products"
            id="nav-products-link"
            className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted/80 transition"
          >
            <ShoppingBag size={13} />
            <span>Notes &amp; Videos</span>
          </Link>

          {isAuthenticated ? (
            <>
              {user?.role === 'STUDENT' && (
                <Link
                  to="/cart"
                  id="nav-cart-link"
                  className="relative flex items-center gap-1.5 rounded-lg bg-primary/10 border border-primary/20 px-3 py-1.5 text-xs font-semibold text-primary transition"
                >
                  <ShoppingCart size={13} />
                  <span>Cart</span>
                  {itemCount > 0 && (
                    <span
                      id="cart-badge-count"
                      className="ml-1 rounded-full bg-primary text-primary-foreground px-1.5 py-0.2 text-[10px] font-bold"
                    >
                      {itemCount}
                    </span>
                  )}
                </Link>
              )}

              <Link
                to="/dashboard"
                id="nav-dashboard-link"
                className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                <span>Dashboard</span>
              </Link>
              <Link
                to="/profile"
                className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                <User size={13} />
                <span>Profile</span>
              </Link>
              <Link
                to="/addresses"
                className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                <MapPin size={13} />
                <span>Addresses</span>
              </Link>
              {user?.role === 'ADMIN' && (
                <Link
                  to="/admin/subjects"
                  id="nav-admin-link"
                  className="flex items-center gap-1.5 rounded-lg bg-primary/10 text-primary border border-primary/20 px-3 py-1.5 text-xs font-semibold hover:bg-primary/20 transition"
                >
                  <Shield size={13} />
                  <span>Admin</span>
                </Link>
              )}
              <button
                type="button"
                onClick={handleLogout}
                className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition"
              >
                <LogOut size={13} />
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-lg border border-border px-3.5 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="rounded-lg bg-primary text-primary-foreground px-3.5 py-1.5 text-xs font-semibold hover:opacity-90 transition"
              >
                Get Started
              </Link>
            </>
          )}
        </nav>
      </header>

      {/* Main content */}
      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-8">
        <div className="flex items-center justify-between pb-6 border-b border-border">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
              <ShoppingCart size={24} className="text-primary" />
              <span>Shopping Cart</span>
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Review your selected notes and video courses
            </p>
          </div>

          {items.length > 0 && (
            <button
              type="button"
              id="clear-cart-btn"
              onClick={() => setShowClearConfirm(true)}
              className="inline-flex items-center gap-1.5 text-xs text-destructive hover:text-destructive/80 font-medium px-3 py-1.5 rounded-lg border border-destructive/20 hover:bg-destructive/10 transition"
            >
              <Trash2 size={13} />
              <span>Clear cart</span>
            </button>
          )}
        </div>

        {/* Global error banner */}
        {errorMessage && (
          <div
            id="cart-error-banner"
            className="mt-4 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-sm flex items-center gap-2"
          >
            <AlertTriangle size={16} />
            <span>{errorMessage}</span>
          </div>
        )}

        {isLoading && items.length === 0 ? (
          <div className="py-20 flex flex-col items-center justify-center space-y-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">Loading your cart...</p>
          </div>
        ) : items.length === 0 ? (
          /* Empty state */
          <div
            id="empty-cart-state"
            className="py-20 text-center flex flex-col items-center justify-center space-y-4"
          >
            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground/40">
              <PackageX size={32} />
            </div>
            <div className="space-y-1">
              <h2 className="text-lg font-semibold text-foreground">Your cart is empty</h2>
              <p className="text-sm text-muted-foreground max-w-sm">
                Explore our catalog of physical study notes and comprehensive video lessons.
              </p>
            </div>
            <Link
              to="/products"
              id="browse-products-btn"
              className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-sm font-semibold hover:opacity-90 transition shadow-sm"
            >
              <ShoppingBag size={15} />
              <span>Browse Catalog</span>
            </Link>
          </div>
        ) : (
          /* Cart list & summary */
          <div className="mt-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Items list */}
            <div className="lg:col-span-2 space-y-4">
              {items.map((item) => {
                const isVideo = item.type === 'VIDEO';
                const isBusy = busyItemId === item.id;

                return (
                  <div
                    key={item.id}
                    id={`cart-item-${item.id}`}
                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-4 rounded-2xl border border-border bg-card gap-4 hover:border-primary/20 transition shadow-sm"
                  >
                    {/* Left: Thumbnail & Info */}
                    <div className="flex items-center gap-3.5 flex-1 min-w-0">
                      <div className="relative h-16 w-16 sm:h-20 sm:w-20 rounded-xl bg-muted overflow-hidden flex-shrink-0 border border-border">
                        {item.thumbnailUrl ? (
                          <img
                            src={item.thumbnailUrl}
                            alt={item.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-muted-foreground/30">
                            {isVideo ? <Video size={24} /> : <FileText size={24} />}
                          </div>
                        )}
                      </div>

                      <div className="space-y-1 min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span
                            className={`inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-semibold ${
                              isVideo
                                ? 'bg-purple-500/10 text-purple-600 dark:text-purple-400'
                                : 'bg-blue-500/10 text-blue-600 dark:text-blue-400'
                            }`}
                          >
                            {isVideo ? <Video size={10} /> : <FileText size={10} />}
                            {isVideo ? 'Video Course' : 'Physical Note'}
                          </span>
                        </div>

                        <Link
                          to={item.slug ? `/products/${item.slug}` : '#'}
                          className="font-medium text-sm text-foreground hover:text-primary transition line-clamp-1"
                        >
                          {item.name}
                        </Link>

                        <div className="text-xs text-muted-foreground">
                          Unit price: <span className="font-semibold text-foreground">{formatLKR(item.unitPrice)}</span>
                        </div>

                        {/* Warning badges */}
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {item.unavailable && (
                            <span
                              id={`badge-unavailable-${item.id}`}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-destructive/10 text-destructive border border-destructive/20"
                            >
                              <AlertTriangle size={10} />
                              No longer available
                            </span>
                          )}
                          {item.priceChanged && (
                            <span
                              id={`badge-price-changed-${item.id}`}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20"
                            >
                              <AlertTriangle size={10} />
                              Price changed (Current: {formatLKR(item.currentPrice)})
                            </span>
                          )}
                          {item.outOfStock && (
                            <span
                              id={`badge-out-of-stock-${item.id}`}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-destructive/10 text-destructive border border-destructive/20"
                            >
                              <AlertTriangle size={10} />
                              Out of stock
                            </span>
                          )}
                          {item.insufficientStock && !item.outOfStock && (
                            <span
                              id={`badge-insufficient-stock-${item.id}`}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20"
                            >
                              <AlertTriangle size={10} />
                              Insufficient stock
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Right: Quantity controls & Line Total */}
                    <div className="flex items-center justify-between sm:justify-end gap-5 w-full sm:w-auto pt-2 sm:pt-0 border-t sm:border-t-0 border-border">
                      {/* Quantity control */}
                      {isVideo ? (
                        <div className="text-xs font-semibold text-muted-foreground px-3 py-1.5 rounded-lg bg-muted border border-border">
                          Qty: 1
                        </div>
                      ) : (
                        <div className="flex items-center rounded-lg border border-border bg-card">
                          <button
                            type="button"
                            id={`decrease-qty-${item.id}`}
                            disabled={item.quantity <= 1 || isBusy}
                            onClick={() => handleQuantityChange(item.id, item.quantity, -1)}
                            className="p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition"
                            aria-label="Decrease quantity"
                          >
                            <Minus size={13} />
                          </button>
                          <span
                            id={`qty-display-${item.id}`}
                            className="w-8 text-center text-xs font-semibold text-foreground select-none"
                          >
                            {isBusy ? <Loader2 size={12} className="animate-spin mx-auto" /> : item.quantity}
                          </span>
                          <button
                            type="button"
                            id={`increase-qty-${item.id}`}
                            disabled={isBusy}
                            onClick={() => handleQuantityChange(item.id, item.quantity, 1)}
                            className="p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition"
                            aria-label="Increase quantity"
                          >
                            <Plus size={13} />
                          </button>
                        </div>
                      )}

                      {/* Line total */}
                      <div className="text-right min-w-[90px]">
                        <div
                          id={`line-total-${item.id}`}
                          className="text-sm font-bold text-foreground"
                        >
                          {formatLKR(item.lineTotal)}
                        </div>
                      </div>

                      {/* Remove button */}
                      <button
                        type="button"
                        id={`remove-item-${item.id}`}
                        disabled={isBusy}
                        onClick={() => handleRemove(item.id)}
                        className="p-2 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-lg transition"
                        title="Remove item"
                        aria-label="Remove item"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Order summary sidebar */}
            <div className="lg:col-span-1">
              <div className="sticky top-20 rounded-2xl border border-border bg-card p-6 space-y-6 shadow-sm">
                <h2 className="text-lg font-bold text-foreground">Order Summary</h2>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between text-muted-foreground">
                    <span>Total items</span>
                    <span id="summary-item-count" className="font-semibold text-foreground">
                      {itemCount}
                    </span>
                  </div>
                  <div className="flex justify-between text-muted-foreground">
                    <span>Subtotal</span>
                    <span id="summary-subtotal" className="font-semibold text-foreground">
                      {formatLKR(subtotal)}
                    </span>
                  </div>
                  <div className="border-t border-border pt-3 flex justify-between text-base font-bold text-foreground">
                    <span>Estimated Total</span>
                    <span id="summary-total" className="text-primary">
                      {formatLKR(subtotal)}
                    </span>
                  </div>
                </div>

                <div className="space-y-2">
                  <button
                    type="button"
                    disabled
                    id="checkout-btn"
                    className="w-full flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground py-3 text-sm font-semibold opacity-60 cursor-not-allowed transition shadow-sm"
                  >
                    <span>Proceed to Checkout</span>
                    <ArrowRight size={14} />
                  </button>
                  <p className="text-[11px] text-center text-muted-foreground">
                    Checkout &amp; payments coming soon in the next update
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Clear Cart Confirmation Modal */}
      {showClearConfirm && (
        <div
          id="clear-cart-modal"
          className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4"
        >
          <div className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-foreground">Clear shopping cart?</h3>
            <p className="text-xs text-muted-foreground">
              Are you sure you want to remove all items from your cart? This action cannot be undone.
            </p>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                id="cancel-clear-btn"
                disabled={clearing}
                onClick={() => setShowClearConfirm(false)}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg border border-border text-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
              <button
                type="button"
                id="confirm-clear-btn"
                disabled={clearing}
                onClick={handleClearCart}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-destructive text-destructive-foreground hover:opacity-90 transition flex items-center gap-1.5"
              >
                {clearing && <Loader2 size={12} className="animate-spin" />}
                <span>Clear all items</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
