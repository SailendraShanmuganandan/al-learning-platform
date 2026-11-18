import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  Menu,
  X,
  BookOpen,
  ShoppingBag,
  GraduationCap,
  Megaphone,
  Mail,
  Info,
  Shield,
  LayoutDashboard,
  Home,
} from 'lucide-react';

export default function PublicLayout() {
  const { user, isAuthenticated } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navLinks = [
    { label: 'Home', path: '/', icon: Home },
    { label: 'Subjects', path: '/subjects', icon: BookOpen },
    { label: 'Notes & Videos', path: '/products', icon: ShoppingBag },
    { label: 'Classes', path: '/classes', icon: GraduationCap },
    { label: 'Announcements', path: '/announcements', icon: Megaphone },
    { label: 'About', path: '/about', icon: Info },
    { label: 'Contact', path: '/contact', icon: Mail },
  ];

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Sticky Top Navbar */}
      <header className="sticky top-0 z-40 border-b border-border bg-card/95 backdrop-blur-md px-6 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2.5">
            <div
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold shadow-xs"
              aria-hidden
            >
              AL
            </div>
            <span className="font-bold text-foreground text-sm tracking-tight">
              AL Learning Platform
            </span>
          </Link>

          {/* Desktop Nav Links */}
          <nav className="hidden lg:flex items-center gap-1">
            {navLinks.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  `px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                    isActive
                      ? 'bg-muted text-foreground font-semibold'
                      : 'text-muted-foreground hover:text-foreground hover:bg-muted/60'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Right side auth buttons / admin link */}
        <div className="hidden sm:flex items-center gap-3">
          {isAuthenticated ? (
            user?.role === 'ADMIN' ? (
              <Link
                to="/admin"
                className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-1.5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90 transition"
              >
                <Shield size={14} />
                <span>Admin Console</span>
              </Link>
            ) : (
              <Link
                to="/dashboard"
                className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-1.5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90 transition"
              >
                <LayoutDashboard size={14} />
                <span>Dashboard</span>
              </Link>
            )
          ) : (
            <>
              <Link
                to="/login"
                id="public-nav-login"
                className="rounded-xl border border-border px-3.5 py-1.5 text-xs font-semibold text-foreground hover:bg-muted transition"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                id="public-nav-register"
                className="rounded-xl bg-primary px-3.5 py-1.5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90 transition"
              >
                Get Started
              </Link>
            </>
          )}
        </div>

        {/* Mobile menu trigger */}
        <button
          type="button"
          onClick={() => setMobileMenuOpen((prev) => !prev)}
          className="lg:hidden p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted transition"
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </header>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-b border-border bg-card p-4 space-y-2 animate-in slide-in-from-top-2 duration-150">
          <nav className="flex flex-col space-y-1">
            {navLinks.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  end={item.path === '/'}
                  onClick={() => setMobileMenuOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium transition ${
                      isActive
                        ? 'bg-primary text-primary-foreground font-semibold'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    }`
                  }
                >
                  <Icon size={15} />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>

          <div className="pt-3 border-t border-border flex flex-col gap-2">
            {isAuthenticated ? (
              user?.role === 'ADMIN' ? (
                <Link
                  to="/admin"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow-xs"
                >
                  Admin Console
                </Link>
              ) : (
                <Link
                  to="/dashboard"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow-xs"
                >
                  Go to Dashboard
                </Link>
              )
            ) : (
              <div className="grid grid-cols-2 gap-2">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-center rounded-xl border border-border px-3 py-2 text-xs font-semibold text-foreground hover:bg-muted"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-center rounded-xl bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground shadow-xs"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Main Outlet */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Public Footer */}
      <footer className="border-t border-border bg-card/60 px-6 py-10 mt-auto">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6 text-xs text-muted-foreground text-center md:text-left">
          <div className="space-y-1">
            <div className="flex items-center justify-center md:justify-start gap-2 text-foreground font-bold text-sm">
              <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary text-primary-foreground text-[10px] font-bold">
                AL
              </div>
              <span>AL Learning Platform</span>
            </div>
            <p className="max-w-sm">
              Comprehensive G.C.E. Advanced Level ICT curriculum, video lessons, past papers, and practice assessments.
            </p>
          </div>

          <div className="flex flex-wrap justify-center gap-6 font-medium">
            <Link to="/subjects" className="hover:text-foreground transition">Subjects</Link>
            <Link to="/products" className="hover:text-foreground transition">Notes &amp; Videos</Link>
            <Link to="/classes" className="hover:text-foreground transition">Classes</Link>
            <Link to="/announcements" className="hover:text-foreground transition">Announcements</Link>
            <Link to="/about" className="hover:text-foreground transition">About</Link>
            <Link to="/contact" className="hover:text-foreground transition">Contact</Link>
          </div>

          <div>
            <p>&copy; {new Date().getFullYear()} AL Learning Platform. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
