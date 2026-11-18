import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  getPublicProducts,
  type PublicProductSummaryResponse,
  type PublicPageResponse,
  type ProductType,
} from '../../api/productApi';
import { getPublicSubjects, type SubjectResponse } from '../../api/subjectApi';
import {
  Video,
  Search,
  ChevronLeft,
  ChevronRight,
  Loader2,
  AlertCircle,
  FileText,
  Package,
} from 'lucide-react';

// ─── helpers ────────────────────────────────────────────────────────────────

function formatLKR(price: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
  }).format(price);
}

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

// ─── Product card ────────────────────────────────────────────────────────────

function ProductCard({ product }: { product: PublicProductSummaryResponse }) {
  const isNote = product.type === 'PHYSICAL_NOTE';
  const isVideo = product.type === 'VIDEO';
  const outOfStock = isNote && product.inStock === false;

  return (
    <Link
      to={`/products/${product.slug}`}
      id={`product-card-${product.id}`}
      className="group relative flex flex-col rounded-2xl border border-border bg-card overflow-hidden hover:shadow-lg hover:border-primary/30 transition-all duration-200"
    >
      {/* Thumbnail */}
      <div className="relative aspect-video bg-muted overflow-hidden">
        {product.thumbnailUrl ? (
          <img
            src={product.thumbnailUrl}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            {isVideo ? (
              <Video size={40} className="text-muted-foreground/30" />
            ) : (
              <FileText size={40} className="text-muted-foreground/30" />
            )}
          </div>
        )}

        {/* Type badge */}
        <div className={`absolute top-2 left-2 flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wide shadow-sm ${
          isVideo
            ? 'bg-primary text-primary-foreground'
            : 'bg-emerald-500 text-white'
        }`}>
          {isVideo ? <Video size={10} /> : <FileText size={10} />}
          {isVideo ? 'Video' : 'Notes'}
        </div>

        {/* Out of stock badge */}
        {outOfStock && (
          <div className="absolute top-2 right-2 bg-destructive text-destructive-foreground text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm">
            Out of stock
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex flex-col flex-1 p-4 gap-2">
        <h3 className="font-semibold text-sm text-foreground line-clamp-2 group-hover:text-primary transition-colors">
          {product.name}
        </h3>

        <div className="flex items-center gap-2 text-[11px] text-muted-foreground mt-auto pt-2">
          {isNote && product.pageCount && (
            <span>{product.pageCount} pages</span>
          )}
          {isVideo && product.durationSeconds && (
            <span>{formatDuration(product.durationSeconds)}</span>
          )}
          {isVideo && product.hasAttachmentPdf && (
            <span className="inline-flex items-center gap-1 text-primary font-medium">
              <FileText size={10} /> PDF Notes
            </span>
          )}
          {product.chapterCount > 0 && (
            <span className="ml-auto">{product.chapterCount} chapter{product.chapterCount !== 1 ? 's' : ''}</span>
          )}
        </div>

        <div className="flex items-center justify-between mt-1">
          <span className="text-base font-extrabold text-primary">
            {formatLKR(product.price)}
          </span>
          {outOfStock ? (
            <span className="text-[10px] font-medium text-destructive">Unavailable</span>
          ) : (
            <span className="text-[10px] font-medium text-emerald-500 dark:text-emerald-400">Available</span>
          )}
        </div>
      </div>
    </Link>
  );
}

// ─── Main page ───────────────────────────────────────────────────────────────

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Filter state — sourced from URL
  const typeParam = (searchParams.get('type') as ProductType | null) ?? '';
  const subjectIdParam = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;
  const qParam = searchParams.get('q') ?? '';
  const sortParam = (searchParams.get('sort') ?? 'newest') as 'newest' | 'price_asc' | 'price_desc';
  const pageParam = Number(searchParams.get('page') ?? '0');

  const [data, setData] = useState<PublicPageResponse<PublicProductSummaryResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);

  // Local search input (debounced into URL)
  const [searchInput, setSearchInput] = useState(qParam);

  // Fetch subjects for the subject filter dropdown
  useEffect(() => {
    getPublicSubjects().then(setSubjects).catch(() => {});
  }, []);

  // Fetch products whenever URL params change
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getPublicProducts({
      type: typeParam || undefined,
      subjectId: subjectIdParam ?? undefined,
      q: qParam || undefined,
      sort: sortParam,
      page: pageParam,
      size: 20,
    })
      .then(d => { if (!cancelled) setData(d); })
      .catch(err => { if (!cancelled) setError(err?.message ?? 'Failed to load products.'); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [typeParam, subjectIdParam, qParam, sortParam, pageParam]);

  // Debounce search input → URL
  useEffect(() => {
    const timer = setTimeout(() => {
      updateParams({ q: searchInput || null, page: null });
    }, 400);
    return () => clearTimeout(timer);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  function updateParams(updates: Record<string, string | number | null>) {
    const next = new URLSearchParams(searchParams);
    for (const [k, v] of Object.entries(updates)) {
      if (v === null || v === '' || v === 0) {
        next.delete(k);
      } else {
        next.set(k, String(v));
      }
    }
    // Reset page when filters change (unless we're explicitly setting page)
    if (!('page' in updates)) next.delete('page');
    setSearchParams(next, { replace: true });
  }

  const totalPages = data?.totalPages ?? 0;
  const currentPage = pageParam;

  return (
    <PageContainer>
      <div className="space-y-8">
        <PageHeader
          title="Notes & Videos"
          description="Browse study notes and video courses for A/L subjects"
        />

        {/* Filters bar */}
        <div className="flex flex-wrap gap-3 mb-6 items-center">
          {/* Search */}
          <div className="relative flex-1 min-w-[220px]">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              id="product-search"
              type="search"
              placeholder="Search products…"
              value={searchInput}
              onChange={e => setSearchInput(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl border border-border bg-card text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
          </div>

          {/* Type filter */}
          <select
            id="product-type-filter"
            value={typeParam}
            onChange={e => updateParams({ type: e.target.value || null })}
            className="rounded-xl border border-border bg-card text-sm text-foreground px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer"
          >
            <option value="">All types</option>
            <option value="PHYSICAL_NOTE">Notes</option>
            <option value="VIDEO">Videos</option>
          </select>

          {/* Subject filter */}
          <select
            id="product-subject-filter"
            value={subjectIdParam ?? ''}
            onChange={e => updateParams({ subjectId: e.target.value || null })}
            className="rounded-xl border border-border bg-card text-sm text-foreground px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer"
          >
            <option value="">All subjects</option>
            {subjects.map(s => (
              <option key={s.subjectId} value={s.subjectId}>{s.name}</option>
            ))}
          </select>

          {/* Sort */}
          <select
            id="product-sort"
            value={sortParam}
            onChange={e => updateParams({ sort: e.target.value, page: null })}
            className="rounded-xl border border-border bg-card text-sm text-foreground px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer"
          >
            <option value="newest">Newest first</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
          </select>

          {/* Active filters indicator */}
          {(typeParam || subjectIdParam || qParam) && (
            <button
              type="button"
              onClick={() => {
                setSearchInput('');
                setSearchParams({});
              }}
              className="text-xs font-medium text-muted-foreground hover:text-destructive transition px-2 py-1 rounded-lg border border-border hover:border-destructive/30"
            >
              Clear filters
            </button>
          )}
        </div>

        {/* Results count */}
        {!loading && data && (
          <p className="text-xs text-muted-foreground mb-4">
            {data.totalElements} product{data.totalElements !== 1 ? 's' : ''} found
          </p>
        )}

        {/* Loading */}
        {loading && (
          <div className="py-24 flex flex-col items-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading products…</p>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 flex items-center gap-3 text-destructive text-sm">
            <AlertCircle size={18} />
            {error}
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && data && data.content.length === 0 && (
          <div className="py-24 flex flex-col items-center gap-4">
            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <Package size={32} />
            </div>
            <div className="text-center">
              <h2 className="text-sm font-semibold text-foreground">No products found</h2>
              <p className="text-xs text-muted-foreground mt-1">
                {(typeParam || subjectIdParam || qParam)
                  ? 'Try adjusting your filters or search query.'
                  : 'No products have been published yet. Check back soon!'}
              </p>
            </div>
            {(typeParam || subjectIdParam || qParam) && (
              <button
                type="button"
                onClick={() => { setSearchInput(''); setSearchParams({}); }}
                className="text-xs font-medium text-primary hover:underline"
              >
                Clear all filters
              </button>
            )}
          </div>
        )}

        {/* Product grid */}
        {!loading && !error && data && data.content.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            {data.content.map(product => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-10">
            <button
              type="button"
              disabled={currentPage === 0}
              onClick={() => updateParams({ page: currentPage - 1 })}
              className="flex items-center gap-1 rounded-xl border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <ChevronLeft size={14} />
              Previous
            </button>
            <span className="text-xs text-muted-foreground">
              Page {currentPage + 1} of {totalPages}
            </span>
            <button
              type="button"
              disabled={currentPage >= totalPages - 1}
              onClick={() => updateParams({ page: currentPage + 1 })}
              className="flex items-center gap-1 rounded-xl border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next
              <ChevronRight size={14} />
            </button>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
