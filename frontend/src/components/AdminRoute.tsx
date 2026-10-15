import { Navigate, Outlet, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

/**
 * Route guard for admin-only pages.
 * - If still loading auth state: shows loader.
 * - If not logged in: redirects to /login (saving previous location).
 * - If logged in but role is NOT ADMIN: renders a 403 Forbidden / Not Authorized screen.
 * - If ADMIN: renders child routes via <Outlet />.
 */
export function AdminRoute() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <div
            className="h-10 w-10 rounded-full border-4 border-primary/20 border-t-primary animate-spin"
            aria-label="Loading"
          />
          <p className="text-muted-foreground text-sm">Verifying access…</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <div className="max-w-md w-full text-center bg-card border border-destructive/30 rounded-2xl p-8 shadow-sm">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10 text-destructive mb-4">
            <ShieldAlert size={32} />
          </div>
          <h1 className="text-2xl font-bold text-foreground">Access Denied</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            You do not have permission to access the administrative area. This section requires an administrator account.
          </p>
          <div className="mt-6">
            <Link
              to="/dashboard"
              className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-sm font-semibold hover:opacity-90 transition"
            >
              <ArrowLeft size={16} />
              Return to Dashboard
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
