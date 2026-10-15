import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPublicSubjects, type SubjectResponse } from '../../api/subjectApi';
import type { ApiError } from '../../api/authApi';
import {
  BookOpen,
  Search,
  ArrowRight,
  Shield,
  User,
  LogOut,
  MapPin,
  Layers,
  Sparkles,
  Loader2,
  ShoppingBag,
} from 'lucide-react';

export default function SubjectsPage() {
  const { user, isAuthenticated, logout } = useAuth();
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getPublicSubjects();
        setSubjects(data);
      } catch (err) {
        const apiErr = err as ApiError;
        setError(apiErr?.message || 'Failed to load subjects.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const filtered = subjects.filter((s) => {
    const q = searchQuery.toLowerCase();
    return s.name.toLowerCase().includes(q) || (s.description && s.description.toLowerCase().includes(q));
  });

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Navbar */}
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

        <nav className="flex items-center gap-3">
          <Link
            to="/subjects"
            id="nav-public-subjects-link"
            className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-1.5 text-xs font-semibold text-foreground transition"
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
                onClick={logout}
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

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl mx-auto w-full px-6 py-10 space-y-8">
        {/* Hero Section */}
        <div className="relative rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-8 sm:p-10 overflow-hidden shadow-xs">
          <div className="max-w-2xl space-y-3 relative z-10">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <Sparkles size={13} />
              <span>G.C.E. Advanced Level Curriculum</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground">
              Explore Academic Subjects
            </h1>
            <p className="text-muted-foreground text-sm sm:text-base leading-relaxed">
              Discover comprehensive subject streams, interactive past papers, and expert learning materials tailored for your A/L examination success.
            </p>
          </div>

          <div className="mt-6 max-w-md relative z-10">
            <div className="relative">
              <Search
                size={16}
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"
              />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search subjects by name or keyword…"
                className="w-full rounded-2xl border border-border bg-background/80 backdrop-blur-xs pl-10 pr-4 py-2.5 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 shadow-xs"
              />
            </div>
          </div>
        </div>

        {/* Subjects Grid / Status */}
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading active subjects…</p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 text-center text-destructive text-sm">
            {error}
          </div>
        ) : filtered.length === 0 ? (
          <div className="rounded-3xl border border-border bg-card p-12 text-center space-y-3">
            <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <BookOpen size={28} />
            </div>
            <h3 className="text-base font-semibold text-foreground">No subjects found</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              {searchQuery
                ? `No subjects match "${searchQuery}". Try searching for something else.`
                : 'No active curriculum subjects are available at this time. Please check back later.'}
            </p>
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="text-xs font-semibold text-primary hover:underline"
              >
                Clear Search
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filtered.map((subject) => (
              <Link
                key={subject.subjectId}
                to={`/subjects/${subject.subjectId}`}
                id={`public-subject-card-${subject.subjectId}`}
                className="group relative rounded-2xl border border-border bg-card p-6 shadow-xs hover:shadow-md hover:border-primary/40 transition-all flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-sm">
                      <Layers size={18} />
                    </div>
                    <span className="rounded-full bg-green-500/10 text-green-700 dark:text-green-300 border border-green-500/20 px-2 py-0.5 text-[10px] font-semibold">
                      Curriculum
                    </span>
                  </div>

                  <div>
                    <h2 className="text-lg font-bold text-foreground group-hover:text-primary transition-colors">
                      {subject.name}
                    </h2>
                    <p className="mt-2 text-xs text-muted-foreground line-clamp-3 leading-relaxed">
                      {subject.description || 'Comprehensive study modules, video lessons, and revision quizzes are prepared for this subject.'}
                    </p>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
                  <span className="text-[11px]">A/L Level</span>
                  <div className="inline-flex items-center gap-1 font-semibold text-primary group-hover:translate-x-0.5 transition-transform">
                    <span>View Chapters</span>
                    <ArrowRight size={13} />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-border bg-card/60 px-6 py-6 text-center text-xs text-muted-foreground">
        <p>&copy; {new Date().getFullYear()} AL Learning Platform. Built for Advanced Level students.</p>
      </footer>
    </div>
  );
}
