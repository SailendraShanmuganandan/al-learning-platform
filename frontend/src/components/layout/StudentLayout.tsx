import { useState, useEffect } from 'react';
import { NavLink, Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import { resendVerification } from '../../api/authApi';
import { STUDENT_NAV_CONFIG } from './studentNav';
import {
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
  ShoppingCart,
  User,
  LogOut,
  AlertCircle,
  Mail,
  Loader2,
  MapPin,
  Settings,
  ChevronDown,
} from 'lucide-react';

export default function StudentLayout() {
  const { user, logout } = useAuth();
  const { itemCount } = useCart();
  const location = useLocation();
  const navigate = useNavigate();

  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem('al_student_sidebar_collapsed') === 'true';
    } catch {
      return false;
    }
  });

  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  // Email verification state
  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [blockedPrompt, setBlockedPrompt] = useState<string | null>(null);

  // Listen for global email verification block
  useEffect(() => {
    function handleEmailNotVerified(event: Event) {
      const customEvent = event as CustomEvent<{ message?: string }>;
      setBlockedPrompt(
        customEvent.detail?.message ||
          'Your action was blocked because your email address has not been verified.'
      );
    }

    window.addEventListener('al:email-not-verified', handleEmailNotVerified);
    return () => {
      window.removeEventListener('al:email-not-verified', handleEmailNotVerified);
    };
  }, []);

  // Close mobile drawer and menus on navigation
  useEffect(() => {
    setMobileOpen(false);
    setUserMenuOpen(false);
  }, [location.pathname]);

  function toggleCollapse() {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('al_student_sidebar_collapsed', String(next));
      } catch {
        // ignore
      }
      return next;
    });
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  async function handleResend() {
    setResending(true);
    setResendStatus(null);
    try {
      const res = await resendVerification();
      setResendStatus({
        type: 'success',
        message: res.message || 'Verification link sent to your email!',
      });
    } catch {
      setResendStatus({
        type: 'error',
        message: 'Failed to send verification email. Please try again.',
      });
    } finally {
      setResending(false);
    }
  }

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  // Determine page title based on current path
  const currentItem = STUDENT_NAV_CONFIG.flatMap((g) => g.items).find((item) =>
    item.exact ? location.pathname === item.path : location.pathname.startsWith(item.path)
  );
  const pageTitle = currentItem?.label || 'Student Portal';

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Slim Top Bar */}
      <header className="sticky top-0 z-30 border-b border-border bg-card/95 backdrop-blur-md px-4 sm:px-6 py-2.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {/* Mobile menu trigger */}
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            className="md:hidden p-1.5 rounded-xl border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
            aria-label="Open navigation menu"
          >
            <Menu size={18} />
          </button>

          {/* Desktop collapse toggle */}
          <button
            type="button"
            onClick={toggleCollapse}
            className="hidden md:flex p-1.5 rounded-xl border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
          </button>

          {/* Logo brand */}
          <Link to="/" className="flex items-center gap-2.5 mr-2">
            <div
              className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold shadow-xs"
              aria-hidden
            >
              AL
            </div>
            <span className="font-bold text-foreground text-sm tracking-tight hidden sm:inline">
              AL Learning
            </span>
          </Link>

          <span className="hidden sm:inline text-muted-foreground/40">|</span>
          <span className="text-xs font-semibold text-foreground truncate max-w-[200px] sm:max-w-xs">
            {pageTitle}
          </span>
        </div>

        {/* Right side items */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Verify Email Chip if unverified */}
          {!isVerified && (
            <button
              type="button"
              onClick={handleResend}
              disabled={resending}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20 hover:bg-amber-500/20 transition disabled:opacity-50"
              title="Click to resend verification email"
            >
              {resending ? <Loader2 size={12} className="animate-spin" /> : <AlertCircle size={12} />}
              <span className="hidden sm:inline">Verify your email</span>
              <span className="sm:hidden">Verify</span>
            </button>
          )}

          {/* Cart Icon with count */}
          <Link
            to="/cart"
            id="nav-cart-icon"
            className="relative p-2 rounded-xl border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
            aria-label="Shopping Cart"
          >
            <ShoppingCart size={16} />
            {itemCount > 0 && (
              <span className="absolute -top-1 -right-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-primary-foreground shadow-xs">
                {itemCount}
              </span>
            )}
          </Link>

          {/* User Menu Dropdown */}
          <div className="relative">
            <button
              type="button"
              onClick={() => setUserMenuOpen((prev) => !prev)}
              className="flex items-center gap-2 p-1 sm:px-2.5 sm:py-1 rounded-xl border border-border hover:bg-muted transition text-xs font-medium text-foreground"
              aria-expanded={userMenuOpen}
              aria-haspopup="true"
            >
              <div className="h-6 w-6 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-[11px]">
                {user?.fullName?.charAt(0).toUpperCase() || 'U'}
              </div>
              <span className="hidden md:inline font-semibold">{user?.fullName?.split(' ')[0]}</span>
              <ChevronDown size={13} className="text-muted-foreground" />
            </button>

            {userMenuOpen && (
              <div
                className="absolute right-0 mt-2 w-52 rounded-2xl border border-border bg-card p-2 shadow-xl z-50 animate-in fade-in zoom-in-95 duration-100"
                role="menu"
              >
                <div className="px-3 py-2 border-b border-border/60 mb-1">
                  <p className="text-xs font-bold text-foreground truncate">{user?.fullName}</p>
                  <p className="text-[11px] text-muted-foreground truncate">{user?.email}</p>
                </div>

                <Link
                  to="/profile"
                  className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-foreground hover:bg-muted transition"
                  role="menuitem"
                >
                  <User size={14} className="text-muted-foreground" />
                  <span>Profile</span>
                </Link>

                <Link
                  to="/addresses"
                  className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-foreground hover:bg-muted transition"
                  role="menuitem"
                >
                  <MapPin size={14} className="text-muted-foreground" />
                  <span>My Addresses</span>
                </Link>

                <Link
                  to="/account-settings"
                  className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-foreground hover:bg-muted transition"
                  role="menuitem"
                >
                  <Settings size={14} className="text-muted-foreground" />
                  <span>Account Settings</span>
                </Link>

                <div className="my-1 border-t border-border/60" />

                <button
                  type="button"
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-destructive hover:bg-destructive/10 transition text-left"
                  role="menuitem"
                >
                  <LogOut size={14} />
                  <span>Sign out</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Email Verification Banner */}
      {!isVerified && (
        <div className="border-b border-amber-500/30 bg-amber-500/10 px-4 sm:px-6 py-2.5 text-xs text-amber-800 dark:text-amber-200">
          <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <AlertCircle size={15} className="text-amber-600 shrink-0" />
              <span>
                <strong>Your email is not verified.</strong> Please verify to unlock official assessments and order notifications.
              </span>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                disabled={resending}
                onClick={handleResend}
                className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white px-2.5 py-1 text-[11px] font-semibold transition disabled:opacity-50 shadow-2xs"
              >
                {resending ? <Loader2 size={12} className="animate-spin" /> : <Mail size={12} />}
                <span>Resend verification link</span>
              </button>
              {resendStatus && (
                <span className={`text-[11px] font-medium ${resendStatus.type === 'success' ? 'text-emerald-700 dark:text-emerald-300' : 'text-destructive'}`}>
                  {resendStatus.message}
                </span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Blocked Action Alert */}
      {blockedPrompt && (
        <div className="max-w-4xl mx-auto mt-4 px-4 w-full">
          <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-destructive text-xs flex items-start gap-3">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-bold">Email Verification Required</h4>
              <p className="mt-0.5 opacity-90">{blockedPrompt}</p>
              <div className="mt-2.5 flex items-center gap-3">
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={resending}
                  className="px-3 py-1 rounded-lg bg-destructive text-destructive-foreground font-semibold hover:opacity-90"
                >
                  Resend verification email
                </button>
                <button
                  type="button"
                  onClick={() => setBlockedPrompt(null)}
                  className="underline hover:no-underline font-medium"
                >
                  Dismiss
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Body Area with Sidebar and Outlet */}
      <div className="flex-1 flex overflow-hidden">
        {/* Desktop Sidebar */}
        <aside
          className={`hidden md:flex flex-col border-r border-border bg-card/60 transition-all duration-200 shrink-0 select-none ${
            collapsed ? 'w-16' : 'w-64'
          }`}
        >
          <div className="flex-1 overflow-y-auto py-4 px-3 space-y-6">
            {STUDENT_NAV_CONFIG.map((group) => (
              <div key={group.group} className="space-y-1">
                {!collapsed && (
                  <p className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-muted-foreground/60 mb-1.5">
                    {group.group}
                  </p>
                )}
                {group.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      end={item.exact}
                      className={({ isActive }) =>
                        `flex items-center gap-3 rounded-xl px-3 py-2 text-xs font-medium transition ${
                          collapsed ? 'justify-center px-2' : ''
                        } ${
                          isActive
                            ? 'bg-primary text-primary-foreground font-bold shadow-xs'
                            : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                        }`
                      }
                      title={collapsed ? item.label : undefined}
                    >
                      <Icon size={16} className="shrink-0" />
                      {!collapsed && <span className="truncate">{item.label}</span>}
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </div>

          <div className="p-3 border-t border-border/60 text-[11px] text-muted-foreground text-center">
            {!collapsed ? <span>A/L ICT Platform</span> : <span>AL</span>}
          </div>
        </aside>

        {/* Mobile Slide-in Drawer */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 md:hidden flex">
            {/* Backdrop */}
            <div
              className="fixed inset-0 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200"
              onClick={() => setMobileOpen(false)}
            />

            {/* Drawer Content */}
            <div className="relative w-72 max-w-[80vw] bg-card border-r border-border h-full flex flex-col z-10 p-4 animate-in slide-in-from-left duration-200">
              <div className="flex items-center justify-between pb-3 border-b border-border">
                <div className="flex items-center gap-2">
                  <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold">
                    AL
                  </div>
                  <span className="font-bold text-foreground text-sm">Navigation</span>
                </div>
                <button
                  type="button"
                  onClick={() => setMobileOpen(false)}
                  className="p-1 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted"
                  aria-label="Close menu"
                >
                  <X size={18} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto py-4 space-y-6">
                {STUDENT_NAV_CONFIG.map((group) => (
                  <div key={group.group} className="space-y-1">
                    <p className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-muted-foreground/60 mb-1.5">
                      {group.group}
                    </p>
                    {group.items.map((item) => {
                      const Icon = item.icon;
                      return (
                        <NavLink
                          key={item.path}
                          to={item.path}
                          end={item.exact}
                          onClick={() => setMobileOpen(false)}
                          className={({ isActive }) =>
                            `flex items-center gap-3 rounded-xl px-3 py-2 text-xs font-medium transition ${
                              isActive
                                ? 'bg-primary text-primary-foreground font-bold shadow-xs'
                                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                            }`
                          }
                        >
                          <Icon size={16} />
                          <span>{item.label}</span>
                        </NavLink>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
