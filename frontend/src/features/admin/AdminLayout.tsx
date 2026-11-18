import { useState, useEffect, useRef } from 'react';
import { Outlet, NavLink, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getNeedsReviewCount } from '../../api/adminCommerceApi';
import { getPendingClassRegistrationsCount } from '../../api/classApi';
import { getUnreadContactMessagesCount } from '../../api/communicationApi';
import { getAdminQuestionReports } from '../learning-admin/api';
import { ADMIN_NAV_CONFIG } from '../../components/layout/adminNav';
import {
  Shield,
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
  Bell,
  ExternalLink,
  LogOut,
  ShoppingBag,
  GraduationCap,
  MessageSquare,
  Flag,
} from 'lucide-react';

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem('al_admin_sidebar_collapsed') === 'true';
    } catch {
      return false;
    }
  });

  const [mobileOpen, setMobileOpen] = useState(false);
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef<HTMLDivElement>(null);

  // Pending action counts
  const [reviewCount, setReviewCount] = useState<number>(0);
  const [pendingRegistrationsCount, setPendingRegistrationsCount] = useState<number>(0);
  const [unreadMessagesCount, setUnreadMessagesCount] = useState<number>(0);
  const [openReportsCount, setOpenReportsCount] = useState<number>(0);

  useEffect(() => {
    loadCounts();
  }, [location.pathname]);

  // Close menus on route change
  useEffect(() => {
    setMobileOpen(false);
    setBellOpen(false);
  }, [location.pathname]);

  // Click outside bell dropdown
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (bellRef.current && !bellRef.current.contains(event.target as Node)) {
        setBellOpen(false);
      }
    }
    if (bellOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [bellOpen]);

  async function loadCounts() {
    try {
      const [revCount, regCount, unreadCount, reportsRes] = await Promise.all([
        getNeedsReviewCount().catch(() => 0),
        getPendingClassRegistrationsCount().catch(() => 0),
        getUnreadContactMessagesCount().catch(() => 0),
        getAdminQuestionReports({ status: 'OPEN', size: 1 }).catch(() => null),
      ]);
      setReviewCount(revCount);
      setPendingRegistrationsCount(regCount);
      setUnreadMessagesCount(unreadCount);
      setOpenReportsCount(reportsRes?.totalElements ?? 0);
    } catch {
      // Ignore count fetch errors
    }
  }

  function toggleCollapse() {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('al_admin_sidebar_collapsed', String(next));
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

  const totalPending = reviewCount + pendingRegistrationsCount + unreadMessagesCount + openReportsCount;

  // Resolve current page title from adminNav
  let currentTitle = 'Admin Console';
  for (const group of ADMIN_NAV_CONFIG) {
    for (const item of group.items) {
      if (item.exact ? location.pathname === item.path : location.pathname.startsWith(item.path)) {
        currentTitle = item.label;
      }
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Bar */}
      <header className="sticky top-0 z-30 h-14 border-b border-border bg-card/95 backdrop-blur px-4 sm:px-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {/* Mobile hamburger */}
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            className="lg:hidden p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition"
            aria-label="Open admin navigation menu"
          >
            <Menu size={20} />
          </button>

          {/* Desktop collapse toggle */}
          <button
            type="button"
            onClick={toggleCollapse}
            className="hidden lg:flex p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition"
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
          </button>

          {/* Brand/Breadcrumb */}
          <div className="flex items-center gap-2">
            <Link to="/admin" className="flex items-center gap-2 text-foreground font-semibold text-sm">
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold text-xs shadow-sm">
                <Shield size={14} />
              </div>
              <span className="hidden sm:inline font-bold tracking-tight">AL Admin</span>
            </Link>
            <span className="text-muted-foreground/40 text-sm">/</span>
            <span className="text-sm font-medium text-foreground tracking-tight">{currentTitle}</span>
          </div>
        </div>

        {/* Right side items */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* View site button */}
          <Link
            to="/"
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="View public site"
          >
            <ExternalLink size={13} />
            <span className="hidden sm:inline">View site</span>
          </Link>

          {/* Pending actions dropdown */}
          <div className="relative" ref={bellRef}>
            <button
              type="button"
              onClick={() => setBellOpen(!bellOpen)}
              className="relative p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition"
              aria-label="Pending actions"
              title="Pending actions"
            >
              <Bell size={18} />
              {totalPending > 0 && (
                <span className="absolute top-1 right-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-destructive-foreground">
                  {totalPending > 99 ? '99+' : totalPending}
                </span>
              )}
            </button>

            {bellOpen && (
              <div className="absolute right-0 mt-2 w-80 rounded-xl border border-border bg-card p-3 shadow-lg z-50 text-xs">
                <div className="flex items-center justify-between pb-2 mb-2 border-b border-border">
                  <span className="font-semibold text-foreground">Pending Actions</span>
                  <span className="text-[11px] text-muted-foreground">{totalPending} requiring review</span>
                </div>
                <div className="space-y-1">
                  <Link
                    to="/admin/orders?status=PAYMENT_SUBMITTED"
                    onClick={() => setBellOpen(false)}
                    className="flex items-center justify-between p-2 rounded-lg hover:bg-muted transition text-foreground"
                  >
                    <div className="flex items-center gap-2">
                      <ShoppingBag size={14} className="text-amber-500" />
                      <span>Orders needing review</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      reviewCount > 0 ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400' : 'text-muted-foreground'
                    }`}>
                      {reviewCount}
                    </span>
                  </Link>

                  <Link
                    to="/admin/class-registrations?status=PENDING"
                    onClick={() => setBellOpen(false)}
                    className="flex items-center justify-between p-2 rounded-lg hover:bg-muted transition text-foreground"
                  >
                    <div className="flex items-center gap-2">
                      <GraduationCap size={14} className="text-blue-500" />
                      <span>Pending class registrations</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      pendingRegistrationsCount > 0 ? 'bg-blue-500/10 text-blue-600 dark:text-blue-400' : 'text-muted-foreground'
                    }`}>
                      {pendingRegistrationsCount}
                    </span>
                  </Link>

                  <Link
                    to="/admin/contact-messages?status=UNREAD"
                    onClick={() => setBellOpen(false)}
                    className="flex items-center justify-between p-2 rounded-lg hover:bg-muted transition text-foreground"
                  >
                    <div className="flex items-center gap-2">
                      <MessageSquare size={14} className="text-emerald-500" />
                      <span>Unread contact messages</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      unreadMessagesCount > 0 ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'
                    }`}>
                      {unreadMessagesCount}
                    </span>
                  </Link>

                  <Link
                    to="/admin/question-reports?status=OPEN"
                    onClick={() => setBellOpen(false)}
                    className="flex items-center justify-between p-2 rounded-lg hover:bg-muted transition text-foreground"
                  >
                    <div className="flex items-center gap-2">
                      <Flag size={14} className="text-red-500" />
                      <span>Question reports</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      openReportsCount > 0 ? 'bg-red-500/10 text-red-600 dark:text-red-400' : 'text-muted-foreground'
                    }`}>
                      {openReportsCount}
                    </span>
                  </Link>
                </div>
              </div>
            )}
          </div>

          <div className="h-4 w-[1px] bg-border" />

          {/* Admin name + ADMIN badge */}
          <div className="flex items-center gap-2">
            <span className="hidden md:inline font-medium text-xs text-foreground truncate max-w-[140px]">
              {user?.fullName}
            </span>
            <span className="rounded bg-primary/10 text-primary border border-primary/20 px-1.5 py-0.5 text-[10px] font-bold">
              ADMIN
            </span>
          </div>

          {/* Sign out */}
          <button
            type="button"
            onClick={handleLogout}
            title="Sign out"
            className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-muted-foreground hover:text-destructive hover:border-destructive/30 hover:bg-destructive/10 transition"
          >
            <LogOut size={13} />
            <span className="hidden sm:inline">Sign out</span>
          </button>
        </div>
      </header>

      {/* Main Container with Sidebar + Content */}
      <div className="flex-1 flex">
        {/* Desktop Left Sidebar */}
        <aside
          className={`hidden lg:flex flex-col border-r border-border bg-card shrink-0 transition-all duration-200 sticky top-14 h-[calc(100vh-3.5rem)] ${
            collapsed ? 'w-16' : 'w-60'
          }`}
        >
          {/* Sidebar Nav Items */}
          <div className="flex-1 overflow-y-auto py-4 px-2 space-y-6">
            {ADMIN_NAV_CONFIG.map((group) => (
              <div key={group.group} className="space-y-1">
                {!collapsed && (
                  <div className="px-3 text-[10px] font-semibold text-muted-foreground/70 uppercase tracking-wider">
                    {group.group}
                  </div>
                )}
                {group.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      end={item.exact}
                      title={collapsed ? item.label : undefined}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition ${
                          collapsed ? 'justify-center' : ''
                        } ${
                          isActive
                            ? 'bg-primary/10 text-primary font-semibold border-l-2 border-primary shadow-xs'
                            : 'text-muted-foreground hover:text-foreground hover:bg-muted/70'
                        }`
                      }
                    >
                      <Icon size={16} className="shrink-0" />
                      {!collapsed && (
                        <div className="flex-1 flex items-center justify-between min-w-0">
                          <span className="truncate">{item.label}</span>
                          {item.badge && (
                            <span className="ml-1.5 px-1.5 py-0.2 rounded text-[10px] font-semibold bg-primary/10 text-primary">
                              {item.badge}
                            </span>
                          )}
                        </div>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </div>

          {/* Desktop Sidebar Bottom */}
          <div className="p-3 border-t border-border">
            <button
              type="button"
              onClick={toggleCollapse}
              className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition ${
                collapsed ? 'justify-center' : ''
              }`}
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {collapsed ? (
                <ChevronRight size={16} />
              ) : (
                <>
                  <ChevronLeft size={16} />
                  <span>Collapse sidebar</span>
                </>
              )}
            </button>
          </div>
        </aside>

        {/* Mobile Slide-in Drawer */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div
              className="fixed inset-0 bg-background/80 backdrop-blur-sm"
              onClick={() => setMobileOpen(false)}
            />
            <div className="fixed inset-y-0 left-0 w-72 bg-card border-r border-border shadow-xl flex flex-col">
              {/* Drawer Header */}
              <div className="h-14 border-b border-border px-4 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold text-xs shadow-sm">
                    <Shield size={14} />
                  </div>
                  <span className="font-bold text-foreground text-sm tracking-tight">AL Admin</span>
                </div>
                <button
                  type="button"
                  onClick={() => setMobileOpen(false)}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted"
                >
                  <X size={18} />
                </button>
              </div>

              {/* Drawer Nav Items */}
              <div className="flex-1 overflow-y-auto p-3 space-y-5">
                {ADMIN_NAV_CONFIG.map((group) => (
                  <div key={group.group} className="space-y-1">
                    <div className="px-3 text-[10px] font-semibold text-muted-foreground/70 uppercase tracking-wider">
                      {group.group}
                    </div>
                    {group.items.map((item) => {
                      const Icon = item.icon;
                      return (
                        <NavLink
                          key={item.path}
                          to={item.path}
                          end={item.exact}
                          onClick={() => setMobileOpen(false)}
                          className={({ isActive }) =>
                            `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition ${
                              isActive
                                ? 'bg-primary/10 text-primary font-semibold border-l-2 border-primary'
                                : 'text-muted-foreground hover:text-foreground hover:bg-muted/70'
                            }`
                          }
                        >
                          <Icon size={16} className="shrink-0" />
                          <span className="flex-1 truncate">{item.label}</span>
                          {item.badge && (
                            <span className="px-1.5 py-0.2 rounded text-[10px] font-semibold bg-primary/10 text-primary">
                              {item.badge}
                            </span>
                          )}
                        </NavLink>
                      );
                    })}
                  </div>
                ))}
              </div>

              {/* Drawer Footer */}
              <div className="p-3 border-t border-border flex items-center justify-between">
                <div className="text-xs">
                  <div className="font-medium text-foreground truncate max-w-[160px]">{user?.fullName}</div>
                  <div className="text-[10px] text-muted-foreground">Admin Console</div>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="p-1.5 text-muted-foreground hover:text-destructive rounded-lg hover:bg-destructive/10 transition"
                  title="Sign out"
                >
                  <LogOut size={16} />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Content Outlet */}
        <main className="flex-1 min-w-0 bg-background">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
