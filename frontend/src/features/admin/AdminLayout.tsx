import { Outlet, NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  BookOpen,
  LogOut,
  ExternalLink,
  Shield,
  Layers,
  ChevronRight,
  Package,
} from 'lucide-react';

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Header */}
      <header className="border-b border-border bg-card px-6 py-3.5 flex items-center justify-between sticky top-0 z-20">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-primary-foreground font-bold text-sm shadow-sm">
            <Shield size={18} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base text-foreground tracking-tight">AL Admin</span>
              <span className="rounded-full bg-primary/10 text-primary px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider">
                Console
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/dashboard"
            className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition"
          >
            <ExternalLink size={13} />
            <span>App Dashboard</span>
          </Link>

          <div className="h-4 w-[1px] bg-border" />

          <div className="flex items-center gap-2 text-xs">
            <span className="font-medium text-foreground">{user?.fullName}</span>
            <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-semibold text-muted-foreground">
              ADMIN
            </span>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            title="Sign out"
            className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-muted-foreground hover:text-destructive hover:border-destructive/30 hover:bg-destructive/10 transition"
          >
            <LogOut size={13} />
            <span>Sign out</span>
          </button>
        </div>
      </header>

      {/* Main Body with Sidebar */}
      <div className="flex-1 flex">
        {/* Sidebar Nav */}
        <aside className="w-64 border-r border-border bg-card/50 p-4 shrink-0 flex flex-col justify-between">
          <div className="space-y-6">
            <div>
              <p className="px-3 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground/70 mb-2">
                Curriculum & Catalog
              </p>
              <nav className="space-y-1">
                <NavLink
                  to="/admin/subjects"
                  end
                  className={({ isActive }) =>
                    `flex items-center justify-between rounded-xl px-3 py-2.5 text-xs font-medium transition ${
                      isActive
                        ? 'bg-primary text-primary-foreground shadow-sm'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    }`
                  }
                >
                  <div className="flex items-center gap-2.5">
                    <BookOpen size={16} />
                    <span>Subjects</span>
                  </div>
                  <ChevronRight size={14} className="opacity-60" />
                </NavLink>

                <NavLink
                  to="/admin/products"
                  className={({ isActive }) =>
                    `flex items-center justify-between rounded-xl px-3 py-2.5 text-xs font-medium transition ${
                      isActive
                        ? 'bg-primary text-primary-foreground shadow-sm'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    }`
                  }
                >
                  <div className="flex items-center gap-2.5">
                    <Package size={16} />
                    <span>Products</span>
                  </div>
                  <ChevronRight size={14} className="opacity-60" />
                </NavLink>
              </nav>
            </div>

            {/* Upcoming slices placeholder indicator */}
            <div className="rounded-xl border border-dashed border-border/80 p-3.5 bg-muted/20">
              <div className="flex items-center gap-2 text-muted-foreground text-xs font-semibold mb-1">
                <Layers size={14} />
                <span>Next Slices</span>
              </div>
              <p className="text-[11px] text-muted-foreground leading-relaxed">
                Lessons, resources, and quiz management modules will appear here as they are integrated.
              </p>
            </div>
          </div>

          <div className="p-2 border-t border-border/60 text-[11px] text-muted-foreground text-center">
            AL Learning Platform &bull; v1.0 Admin
          </div>
        </aside>

        {/* Dynamic Route Content */}
        <main className="flex-1 p-8 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
