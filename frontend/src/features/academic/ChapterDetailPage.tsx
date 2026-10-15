import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPublicChapter, type ChapterResponse } from '../../api/chapterApi';
import { getPublicSubjectById, type SubjectResponse } from '../../api/subjectApi';
import { getChapterProducts, type PublicProductSummaryResponse } from '../../api/productApi';
import type { ApiError } from '../../api/authApi';
import {
  BookOpen,
  ChevronRight,
  Shield,
  User,
  LogOut,
  MapPin,
  BookMarked,
  Loader2,
  AlertCircle,
  ListOrdered,
  ShoppingBag,
  FileText,
  Video,
} from 'lucide-react';

export default function ChapterDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const [chapter, setChapter] = useState<ChapterResponse | null>(null);
  const [subject, setSubject] = useState<SubjectResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [products, setProducts] = useState<PublicProductSummaryResponse[]>([]);
  const [productsLoading, setProductsLoading] = useState(false);

  useEffect(() => {
    if (!id) { navigate('/subjects', { replace: true }); return; }
    const chapterId = parseInt(id, 10);

    async function load() {
      setLoading(true);
      setError(null);
      setNotFound(false);
      try {
        const chap = await getPublicChapter(chapterId);
        setChapter(chap);
        // Load the parent subject for breadcrumb
        const sub = await getPublicSubjectById(chap.subjectId);
        setSubject(sub);
        // Load chapter products (non-blocking)
        setProductsLoading(true);
        getChapterProducts(chapterId)
          .then(setProducts)
          .catch(() => setProducts([]))
          .finally(() => setProductsLoading(false));
      } catch (err) {
        const apiErr = err as ApiError;
        if (apiErr?.status === 404) {
          setNotFound(true);
        } else {
          setError(apiErr?.message || 'Failed to load chapter details.');
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
        <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-xs text-muted-foreground flex-wrap">
          <Link to="/subjects" className="hover:text-foreground transition">Subjects</Link>
          <ChevronRight size={13} className="opacity-50" />
          {subject ? (
            <Link
              to={`/subjects/${subject.subjectId}`}
              className="hover:text-foreground transition truncate max-w-[140px]"
            >
              {subject.name}
            </Link>
          ) : (
            <span className="opacity-50">Subject</span>
          )}
          <ChevronRight size={13} className="opacity-50" />
          <span className="text-foreground font-medium truncate max-w-[180px]">
            {loading ? '…' : (chapter?.name ?? 'Chapter')}
          </span>
        </nav>

        {/* Loading state */}
        {loading && (
          <div className="py-24 flex flex-col items-center justify-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading chapter…</p>
          </div>
        )}

        {/* Not found */}
        {!loading && notFound && (
          <div className="rounded-3xl border border-border bg-card p-12 text-center space-y-4">
            <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <AlertCircle size={28} />
            </div>
            <h1 className="text-lg font-bold text-foreground">Chapter Not Found</h1>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              This chapter doesn't exist or is no longer available.
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

        {/* Chapter detail */}
        {!loading && chapter && (
          <>
            {/* Chapter hero */}
            <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-8 shadow-xs">
              <div className="flex items-start gap-4">
                <div className="flex-none h-12 w-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                  {chapter.chapterOrder}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <BookMarked size={14} className="text-muted-foreground" />
                    <span className="text-xs text-muted-foreground font-medium">Chapter {chapter.chapterOrder}</span>
                  </div>
                  <h1 className="text-2xl font-extrabold tracking-tight text-foreground">
                    {chapter.name}
                  </h1>
                  {chapter.description && (
                    <p className="mt-3 text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                      {chapter.description}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* ── Resources section ── */}
            <section
              aria-labelledby="resources-heading"
              className="rounded-2xl border border-border bg-card p-6 space-y-4"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ListOrdered size={16} className="text-muted-foreground" />
                  <h2 id="resources-heading" className="text-sm font-semibold text-foreground">
                    Study Materials
                  </h2>
                </div>
                {products.length > 0 && (
                  <Link
                    to={`/products?chapterId=${id}`}
                    className="text-xs font-medium text-primary hover:underline"
                  >
                    View all
                  </Link>
                )}
              </div>

              {productsLoading && (
                <div className="flex items-center gap-2 py-4 text-muted-foreground">
                  <Loader2 size={14} className="animate-spin" />
                  <span className="text-xs">Loading materials…</span>
                </div>
              )}

              {!productsLoading && products.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  No study materials have been published for this chapter yet.
                </p>
              )}

              {!productsLoading && products.length > 0 && (
                <div className="space-y-2">
                  {products.map(p => {
                    const isVideo = p.type === 'VIDEO';
                    const outOfStock = !isVideo && p.inStock === false;
                    return (
                      <Link
                        key={p.id}
                        to={`/products/${p.slug}`}
                        id={`chapter-product-${p.id}`}
                        className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 px-4 py-3 hover:border-primary/30 hover:bg-primary/5 transition group"
                      >
                        <div className={`flex-none h-9 w-9 rounded-xl flex items-center justify-center ${isVideo ? 'bg-primary/10 text-primary' : 'bg-emerald-500/10 text-emerald-600'}`}>
                          {isVideo ? <Video size={16} /> : <FileText size={16} />}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-foreground group-hover:text-primary transition truncate">{p.name}</p>
                          <div className="flex items-center gap-2 text-[11px] text-muted-foreground mt-0.5">
                            <span>{isVideo ? 'Video' : 'Notes'}</span>
                            {!isVideo && p.pageCount && <span>· {p.pageCount} pages</span>}
                            {isVideo && p.durationSeconds && <span>· {Math.round(p.durationSeconds / 60)}m</span>}
                            {outOfStock && <span className="text-destructive font-medium">· Out of stock</span>}
                          </div>
                        </div>
                        <div className="text-sm font-bold text-primary">
                          {new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', minimumFractionDigits: 2 }).format(p.price)}
                        </div>
                        <ChevronRight size={14} className="text-muted-foreground group-hover:text-primary transition flex-none" />
                      </Link>
                    );
                  })}
                </div>
              )}
            </section>

            {/* ── Quizzes section ── (placeholder for future slice) */}
            <section
              aria-labelledby="quizzes-heading"
              className="rounded-2xl border border-dashed border-border bg-muted/20 p-6"
            >
              <div className="flex items-center gap-2 mb-2">
                <BookOpen size={16} className="text-muted-foreground" />
                <h2 id="quizzes-heading" className="text-sm font-semibold text-foreground">
                  Quizzes &amp; Practice
                </h2>
              </div>
              <p className="text-xs text-muted-foreground">
                {/* TODO (next slice): Display MCQ quizzes and past paper questions for this chapter */}
                Practice questions and quizzes for this chapter will appear here once they are added.
              </p>
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
