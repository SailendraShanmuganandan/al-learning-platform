import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPublicSubjectById, type SubjectResponse } from '../../api/subjectApi';
import { getPublicChapters, type ChapterResponse } from '../../api/chapterApi';
import type { ApiError } from '../../api/authApi';
import {
  BookOpen,
  ArrowRight,
  ChevronRight,
  Shield,
  User,
  LogOut,
  MapPin,
  Layers,
  ListOrdered,
  Loader2,
  AlertCircle,
  ShoppingBag,
} from 'lucide-react';

export default function SubjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const [subject, setSubject] = useState<SubjectResponse | null>(null);
  const [chapters, setChapters] = useState<ChapterResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!id) { navigate('/subjects', { replace: true }); return; }
    const subjectId = parseInt(id, 10);

    async function load() {
      setLoading(true);
      setError(null);
      setNotFound(false);
      try {
        const [sub, chaps] = await Promise.all([
          getPublicSubjectById(subjectId),
          getPublicChapters(subjectId),
        ]);
        setSubject(sub);
        setChapters(chaps);
      } catch (err) {
        const apiErr = err as ApiError;
        if (apiErr?.status === 404) {
          setNotFound(true);
        } else {
          setError(apiErr?.message || 'Failed to load subject details.');
        }
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, navigate]);

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

      <main className="flex-1 max-w-4xl mx-auto w-full px-6 py-8 space-y-8">
        {/* Breadcrumbs */}
        <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Link to="/subjects" className="hover:text-foreground transition">Subjects</Link>
          <ChevronRight size={13} className="opacity-50" />
          <span className="text-foreground font-medium truncate max-w-[200px]">
            {loading ? '…' : (subject?.name ?? 'Subject')}
          </span>
        </nav>

        {/* Loading state */}
        {loading && (
          <div className="py-24 flex flex-col items-center justify-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading subject…</p>
          </div>
        )}

        {/* Not found */}
        {!loading && notFound && (
          <div className="rounded-3xl border border-border bg-card p-12 text-center space-y-4">
            <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <AlertCircle size={28} />
            </div>
            <h1 className="text-lg font-bold text-foreground">Subject Not Found</h1>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              This subject doesn't exist or is no longer available.
            </p>
            <Link
              to="/subjects"
              className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 transition"
            >
              Back to Subjects
            </Link>
          </div>
        )}

        {/* Error state */}
        {!loading && error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 text-center text-destructive text-sm">
            {error}
          </div>
        )}

        {/* Subject detail */}
        {!loading && subject && (
          <>
            {/* Subject hero */}
            <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-8 shadow-xs space-y-3">
              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <Layers size={22} />
                </div>
                <div className="flex-1 min-w-0">
                  <h1 className="text-2xl font-extrabold tracking-tight text-foreground">
                    {subject.name}
                  </h1>
                  {subject.description && (
                    <p className="mt-2 text-sm text-muted-foreground leading-relaxed">
                      {subject.description}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Chapters section */}
            <section aria-labelledby="chapters-heading">
              <div className="flex items-center gap-2 mb-4">
                <ListOrdered size={18} className="text-primary" />
                <h2
                  id="chapters-heading"
                  className="text-base font-bold text-foreground"
                >
                  Chapters
                </h2>
                <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
                  {chapters.length}
                </span>
              </div>

              {chapters.length === 0 ? (
                <div className="rounded-3xl border border-border bg-card p-10 text-center space-y-3">
                  <div className="mx-auto h-12 w-12 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
                    <BookOpen size={24} />
                  </div>
                  <h3 className="text-sm font-semibold text-foreground">No chapters yet</h3>
                  <p className="text-xs text-muted-foreground max-w-xs mx-auto">
                    Chapters for this subject haven't been published yet. Please check back later.
                  </p>
                </div>
              ) : (
                <ol className="space-y-3">
                  {chapters.map((chapter) => (
                    <li key={chapter.chapterId}>
                      <Link
                        to={`/chapters/${chapter.chapterId}`}
                        id={`chapter-card-${chapter.chapterId}`}
                        className="group flex items-center gap-4 rounded-2xl border border-border bg-card px-5 py-4 shadow-xs hover:shadow-md hover:border-primary/40 transition-all"
                      >
                        {/* Order badge */}
                        <div className="flex-none h-9 w-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center text-sm font-bold group-hover:bg-primary group-hover:text-primary-foreground transition-colors">
                          {chapter.chapterOrder}
                        </div>

                        <div className="flex-1 min-w-0">
                          <p className="font-semibold text-sm text-foreground group-hover:text-primary transition-colors truncate">
                            {chapter.name}
                          </p>
                          {chapter.description && (
                            <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">
                              {chapter.description}
                            </p>
                          )}
                        </div>

                        <ArrowRight
                          size={16}
                          className="flex-none text-muted-foreground group-hover:text-primary group-hover:translate-x-0.5 transition-all"
                        />
                      </Link>
                    </li>
                  ))}
                </ol>
              )}
            </section>
          </>
        )}
      </main>

      <footer className="border-t border-border bg-card/60 px-6 py-6 text-center text-xs text-muted-foreground">
        <p>&copy; {new Date().getFullYear()} AL Learning Platform. Built for Advanced Level students.</p>
      </footer>
    </div>
  );
}
