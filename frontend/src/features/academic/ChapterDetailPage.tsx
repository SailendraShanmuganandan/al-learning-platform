import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { getPublicChapter, type ChapterResponse } from '../../api/chapterApi';
import { getPublicSubjectById, type SubjectResponse } from '../../api/subjectApi';
import { getChapterProducts, type PublicProductSummaryResponse } from '../../api/productApi';
import { ChapterQuizCard } from '../learning-student/components/ChapterQuizCard';
import type { ApiError } from '../../api/authApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  BookMarked,
  Loader2,
  AlertCircle,
  FileText,
  Video,
  ChevronRight,
} from 'lucide-react';

export default function ChapterDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

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
    <PageContainer>
      <div className="space-y-8">
        <PageHeader
          title={chapter ? `Chapter ${chapter.chapterOrder}: ${chapter.name}` : 'Chapter Details'}
          description={chapter?.description || 'Study materials, video lessons, and interactive chapter quizzes.'}
          breadcrumb={[
            { label: 'Subjects', path: '/subjects' },
            ...(subject ? [{ label: subject.name, path: `/subjects/${subject.subjectId}` }] : []),
            { label: chapter ? `Chapter ${chapter.chapterOrder}` : 'Chapter' },
          ]}
        />

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
            <h2 className="text-lg font-bold text-foreground">Chapter Not Found</h2>
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
            <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 sm:p-8 shadow-xs">
              <div className="flex items-start gap-4">
                <div className="flex-none h-12 w-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                  {chapter.chapterOrder}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <BookMarked size={14} className="text-muted-foreground" />
                    <span className="text-xs text-muted-foreground font-medium">Chapter {chapter.chapterOrder}</span>
                  </div>
                  <h2 className="text-xl sm:text-2xl font-extrabold tracking-tight text-foreground">
                    {chapter.name}
                  </h2>
                  {chapter.description && (
                    <p className="mt-3 text-xs sm:text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                      {chapter.description}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Resources section */}
            <section
              aria-labelledby="resources-heading"
              className="space-y-4"
              data-testid="chapter-resources-section"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 id="resources-heading" className="text-base font-bold text-foreground">
                    Study Notes &amp; Video Lessons
                  </h3>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    Materials published for this chapter
                  </p>
                </div>
                {products.length > 0 && (
                  <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
                    {products.length} {products.length === 1 ? 'item' : 'items'}
                  </span>
                )}
              </div>

              {productsLoading ? (
                <div className="rounded-2xl border border-border bg-card p-6 flex items-center justify-center gap-2 text-xs text-muted-foreground">
                  <Loader2 size={16} className="animate-spin text-primary" />
                  <span>Loading resources…</span>
                </div>
              ) : products.length === 0 ? (
                <div className="rounded-2xl border border-border bg-card p-6 text-center text-xs text-muted-foreground">
                  No notes or videos have been published for this chapter yet. Check back soon.
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3" data-testid="chapter-products-list">
                  {products.map((p) => {
                    const isVideo = p.type === 'VIDEO';
                    const outOfStock = !p.inStock;
                    return (
                      <Link
                        key={p.id}
                        to={`/products/${p.slug}`}
                        data-testid={`chapter-product-${p.id}`}
                        className="group flex items-center gap-3.5 rounded-xl border border-border bg-card p-4 hover:border-primary/40 hover:shadow-xs transition"
                      >
                        <div className="flex-none h-10 w-10 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                          {isVideo ? <Video size={18} /> : <FileText size={18} />}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="font-semibold text-xs text-foreground group-hover:text-primary transition truncate">
                            {p.name}
                          </p>
                          <div className="flex items-center gap-2 text-[11px] text-muted-foreground mt-0.5">
                            <span>{isVideo ? 'Video' : 'Notes'}</span>
                            {!isVideo && p.pageCount && <span>· {p.pageCount} pages</span>}
                            {isVideo && p.durationSeconds && <span>· {Math.round(p.durationSeconds / 60)}m</span>}
                            {outOfStock && <span className="text-destructive font-medium">· Out of stock</span>}
                          </div>
                        </div>
                        <div className="text-xs font-bold text-primary">
                          {new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', minimumFractionDigits: 2 }).format(p.price)}
                        </div>
                        <ChevronRight size={14} className="text-muted-foreground group-hover:text-primary transition flex-none" />
                      </Link>
                    );
                  })}
                </div>
              )}
            </section>

            {/* Chapter Quiz & Practice Card */}
            <section aria-labelledby="quizzes-heading" className="space-y-4">
              <ChapterQuizCard chapterId={chapter.chapterId} />
            </section>
          </>
        )}
      </div>
    </PageContainer>
  );
}
