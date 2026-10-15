import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod/v4';
import {
  getAdminProducts,
  getAdminProductById,
  createAdminProduct,
  updateAdminProduct,
  setAdminProductStatus,
  type ProductSummaryResponse,
  type ProductType,
  type ProductStatus,
  type ProductFilterParams,
} from '../../../api/productApi';
import { getAdminSubjects, type SubjectResponse } from '../../../api/subjectApi';
import { getAllAdminChapters, type ChapterResponse } from '../../../api/chapterApi';
import type { ApiError } from '../../../api/authApi';
import {
  Package,
  Plus,
  Edit2,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  X,
  Loader2,
  FileText,
  Video,
  Search,
  ChevronLeft,
  ChevronRight,
  Layers,
  Archive,
  Send,
  RotateCcw,
  Clock,
  BookOpen,
} from 'lucide-react';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatLKR(amount: number): string {
  return `LKR ${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatDuration(seconds: number): string {
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  if (hrs > 0) {
    return `${hrs}h ${mins}m`;
  }
  return `${mins}m ${secs > 0 ? `${secs}s` : ''}`.trim();
}

// ---------------------------------------------------------------------------
// Form Schema
// ---------------------------------------------------------------------------

const productFormSchema = z.object({
  productType: z.enum(['PHYSICAL_NOTE', 'VIDEO']),
  name: z
    .string()
    .min(1, 'Product name is required')
    .max(300, 'Product name must be at most 300 characters'),
  description: z.string().optional().or(z.literal('')),
  price: z
    .string()
    .min(1, 'Price is required')
    .refine((v) => !isNaN(parseFloat(v)) && parseFloat(v) >= 0, {
      message: 'Price must be a valid non-negative number',
    }),
  thumbnailUrl: z.string().optional().or(z.literal('')),
  // Note-specific fields
  pageCount: z
    .string()
    .optional()
    .transform((v) => (v && v.trim() !== '' ? parseInt(v, 10) : undefined)),
  stockQuantity: z
    .string()
    .optional()
    .transform((v) => (v && v.trim() !== '' ? parseInt(v, 10) : undefined)),
  // Video-specific fields
  videoUrl: z.string().optional().or(z.literal('')),
  durationSeconds: z
    .string()
    .optional()
    .transform((v) => (v && v.trim() !== '' ? parseInt(v, 10) : undefined)),
  videoDescription: z.string().optional().or(z.literal('')),
});

type ProductFormValues = z.infer<typeof productFormSchema>;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function AdminProductsPage() {
  const [products, setProducts] = useState<ProductSummaryResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  // Filters
  const [filters, setFilters] = useState<ProductFilterParams>({
    type: '',
    status: '',
    search: '',
    page: 0,
    size: 10,
  });
  const [searchInput, setSearchInput] = useState('');

  // Curriculum data for chapter multi-select
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [chapters, setChapters] = useState<ChapterResponse[]>([]);
  const [loadingCurriculum, setLoadingCurriculum] = useState(false);

  // Modal state
  const [modalMode, setModalMode] = useState<'create' | number | null>(null);
  const [selectedType, setSelectedType] = useState<ProductType>('PHYSICAL_NOTE');
  const [selectedChapterIds, setSelectedChapterIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Status transition confirmation modal
  const [statusModal, setStatusModal] = useState<{
    product: ProductSummaryResponse;
    targetStatus: ProductStatus;
  } | null>(null);
  const [statusSubmitting, setStatusSubmitting] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema) as any,
    defaultValues: {
      productType: 'PHYSICAL_NOTE',
      name: '',
      description: '',
      price: '',
      thumbnailUrl: '',
      pageCount: undefined,
      stockQuantity: undefined,
      videoUrl: '',
      durationSeconds: undefined,
      videoDescription: '',
    },
  });

  const watchedThumbUrl = watch('thumbnailUrl');
  const watchedDuration = watch('durationSeconds');

  // ── Fetching Data ──────────────────────────────────────────────────────────

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setPageError(null);
    try {
      const pageData = await getAdminProducts(filters);
      setProducts(pageData.content);
      setTotalPages(pageData.totalPages);
      setTotalElements(pageData.totalElements);
    } catch (err) {
      const apiErr = err as ApiError;
      setPageError(apiErr?.message || 'Failed to load products.');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const fetchCurriculum = useCallback(async () => {
    setLoadingCurriculum(true);
    try {
      const [subs, chaps] = await Promise.all([
        getAdminSubjects(),
        getAllAdminChapters().catch(() => []),
      ]);
      setSubjects(subs);
      setChapters(chaps);
    } catch (err) {
      console.error('Failed to load curriculum data', err);
    } finally {
      setLoadingCurriculum(false);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    fetchCurriculum();
  }, [fetchCurriculum]);

  // Handle Search submit
  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFilters((prev) => ({ ...prev, search: searchInput, page: 0 }));
  }

  // ── Create & Edit Modal Operations ─────────────────────────────────────────

  function openCreateModal() {
    setSelectedType('PHYSICAL_NOTE');
    setSelectedChapterIds([]);
    reset({
      productType: 'PHYSICAL_NOTE',
      name: '',
      description: '',
      price: '',
      thumbnailUrl: '',
      pageCount: undefined,
      stockQuantity: undefined,
      videoUrl: '',
      durationSeconds: undefined,
      videoDescription: '',
    });
    setFormError(null);
    setModalMode('create');
  }

  async function openEditModal(summary: ProductSummaryResponse) {
    setFormError(null);
    setModalMode(summary.productId);
    setSelectedType(summary.productType);
    setSubmitting(true);

    try {
      const full = await getAdminProductById(summary.productId);
      setSelectedType(full.productType);
      setSelectedChapterIds(full.chapterIds || []);

      reset({
        productType: full.productType,
        name: full.name,
        description: full.description || '',
        price: full.price.toString(),
        thumbnailUrl: full.thumbnailUrl || '',
        pageCount: full.pageCount ?? undefined,
        stockQuantity: full.stockQuantity ?? undefined,
        videoUrl: full.videoUrl || '',
        durationSeconds: full.durationSeconds ?? undefined,
        videoDescription: full.videoDescription || '',
      });
    } catch (err) {
      const apiErr = err as ApiError;
      setFormError(apiErr?.message || 'Failed to load product details.');
    } finally {
      setSubmitting(false);
    }
  }

  function closeModal() {
    if (submitting) return;
    setModalMode(null);
    setFormError(null);
  }

  function toggleChapterSelection(chapterId: number) {
    setSelectedChapterIds((prev) =>
      prev.includes(chapterId)
        ? prev.filter((id) => id !== chapterId)
        : [...prev, chapterId]
    );
  }

  async function onSubmit(values: ProductFormValues) {
    setSubmitting(true);
    setFormError(null);

    try {
      const priceNum = parseFloat(values.price);

      if (modalMode === 'create') {
        // Validation checks for create
        if (selectedType === 'PHYSICAL_NOTE') {
          if (!values.pageCount || values.pageCount <= 0) {
            setFormError('Page count is required and must be greater than 0.');
            setSubmitting(false);
            return;
          }
        } else {
          if (!values.videoUrl || values.videoUrl.trim() === '') {
            setFormError('Video URL is required for video courses.');
            setSubmitting(false);
            return;
          }
          if (!values.durationSeconds || values.durationSeconds <= 0) {
            setFormError('Video duration is required and must be greater than 0.');
            setSubmitting(false);
            return;
          }
        }

        await createAdminProduct({
          name: values.name.trim(),
          description: values.description?.trim() || null,
          productType: selectedType,
          price: priceNum,
          thumbnailUrl: values.thumbnailUrl?.trim() || null,
          chapterIds: selectedChapterIds,
          pageCount: selectedType === 'PHYSICAL_NOTE' ? values.pageCount : undefined,
          stockQuantity: selectedType === 'PHYSICAL_NOTE' ? values.stockQuantity ?? 0 : undefined,
          videoUrl: selectedType === 'VIDEO' ? values.videoUrl?.trim() : undefined,
          durationSeconds: selectedType === 'VIDEO' ? values.durationSeconds : undefined,
          videoDescription: selectedType === 'VIDEO' ? values.videoDescription?.trim() : undefined,
        });

        setActionSuccess(`Product "${values.name}" created successfully as DRAFT.`);
      } else if (typeof modalMode === 'number') {
        // Edit mode
        await updateAdminProduct(modalMode, {
          name: values.name.trim(),
          description: values.description?.trim() || null,
          price: priceNum,
          thumbnailUrl: values.thumbnailUrl?.trim() || null,
          chapterIds: selectedChapterIds,
          pageCount: selectedType === 'PHYSICAL_NOTE' ? values.pageCount : undefined,
          stockQuantity: selectedType === 'PHYSICAL_NOTE' ? values.stockQuantity : undefined,
          videoUrl: selectedType === 'VIDEO' ? values.videoUrl?.trim() : undefined,
          durationSeconds: selectedType === 'VIDEO' ? values.durationSeconds : undefined,
          videoDescription: selectedType === 'VIDEO' ? values.videoDescription?.trim() : undefined,
        });

        setActionSuccess(`Product "${values.name}" updated successfully.`);
      }

      setModalMode(null);
      fetchProducts();
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      const apiErr = err as ApiError;
      setFormError(apiErr?.message || 'Failed to save product. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  // ── Status Transition Modal ────────────────────────────────────────────────

  function openStatusModal(product: ProductSummaryResponse, targetStatus: ProductStatus) {
    setStatusError(null);
    setStatusModal({ product, targetStatus });
  }

  async function handleConfirmStatusChange() {
    if (!statusModal) return;
    setStatusSubmitting(true);
    setStatusError(null);

    try {
      await setAdminProductStatus(statusModal.product.productId, statusModal.targetStatus);
      setActionSuccess(
        `Product "${statusModal.product.name}" is now ${statusModal.targetStatus}.`
      );
      setStatusModal(null);
      fetchProducts();
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      const apiErr = err as ApiError;
      // Show backend's detailed missing requirements or conflict message
      setStatusError(
        apiErr?.message || 'Status transition failed. Please resolve errors and try again.'
      );
    } finally {
      setStatusSubmitting(false);
    }
  }

  // Group chapters by subject for display
  const chaptersBySubject: Record<string, ChapterResponse[]> = {};
  subjects.forEach((s) => {
    chaptersBySubject[s.name] = chapters.filter((c) => c.subjectId === s.subjectId);
  });

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Products</h1>
            <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
              {totalElements} Total
            </span>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Manage physical note books and video learning packages in the catalog.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchProducts}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-xl border border-border px-3 py-2 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="Refresh products"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            id="admin-create-product-btn"
            type="button"
            onClick={openCreateModal}
            className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition"
          >
            <Plus size={15} />
            <span>New Product</span>
          </button>
        </div>
      </div>

      {/* Global Success / Error Banners */}
      {actionSuccess && (
        <div
          id="admin-product-success-banner"
          className="rounded-xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-xs font-medium text-green-700 dark:text-green-300 flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} className="shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button
            onClick={() => setActionSuccess(null)}
            className="text-green-700/60 hover:text-green-700 dark:text-green-300/60 dark:hover:text-green-300"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {pageError && (
        <div className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-xs font-medium text-destructive flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} className="shrink-0" />
            <span>{pageError}</span>
          </div>
          <button onClick={() => setPageError(null)} className="opacity-70 hover:opacity-100">
            <X size={14} />
          </button>
        </div>
      )}

      {/* Filters Toolbar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
          <Search size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by product name..."
            className="w-full rounded-xl border border-border bg-background pl-9 pr-20 py-2 text-xs placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          />
          <button
            type="submit"
            className="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-lg bg-muted px-2.5 py-1 text-[11px] font-medium text-foreground hover:bg-muted/80 transition"
          >
            Search
          </button>
        </form>

        {/* Dropdown Filters */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Type Filter */}
          <select
            value={filters.type || ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                type: (e.target.value as ProductType) || '',
                page: 0,
              }))
            }
            className="rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
          >
            <option value="">All Types</option>
            <option value="PHYSICAL_NOTE">Physical Notes</option>
            <option value="VIDEO">Video Courses</option>
          </select>

          {/* Status Filter */}
          <select
            value={filters.status || ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                status: (e.target.value as ProductStatus) || '',
                page: 0,
              }))
            }
            className="rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
      </div>

      {/* Products Table */}
      <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <Loader2 size={32} className="animate-spin text-primary mb-3" />
            <p className="text-xs">Loading products...</p>
          </div>
        ) : products.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center px-4">
            <div className="h-14 w-14 rounded-2xl bg-muted/50 flex items-center justify-center text-muted-foreground mb-4">
              <Package size={28} />
            </div>
            <h3 className="text-sm font-semibold text-foreground">No products found</h3>
            <p className="text-xs text-muted-foreground max-w-sm mt-1">
              {filters.search || filters.type || filters.status
                ? 'Try adjusting your search filters to find what you are looking for.'
                : 'Get started by creating your first physical note book or video masterclass.'}
            </p>
            <button
              onClick={openCreateModal}
              className="mt-4 inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition"
            >
              <Plus size={14} />
              <span>New Product</span>
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-border bg-muted/30 text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                  <th className="py-3.5 px-4">Product</th>
                  <th className="py-3.5 px-4">Type</th>
                  <th className="py-3.5 px-4">Price</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4">Details / Stock</th>
                  <th className="py-3.5 px-4">Chapters</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-xs">
                {products.map((p) => (
                  <tr
                    key={p.productId}
                    className="hover:bg-muted/30 transition group"
                  >
                    {/* Product Name & Thumbnail */}
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-3">
                        {p.thumbnailUrl ? (
                          <img
                            src={p.thumbnailUrl}
                            alt=""
                            className="h-10 w-10 rounded-lg object-cover border border-border shrink-0 bg-muted"
                            onError={(e) => {
                              (e.target as HTMLElement).style.display = 'none';
                            }}
                          />
                        ) : (
                          <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center shrink-0 text-muted-foreground">
                            {p.productType === 'PHYSICAL_NOTE' ? (
                              <FileText size={18} />
                            ) : (
                              <Video size={18} />
                            )}
                          </div>
                        )}
                        <div>
                          <p className="font-semibold text-foreground group-hover:text-primary transition line-clamp-1">
                            {p.name}
                          </p>
                          <p className="text-[11px] font-mono text-muted-foreground/70">
                            /{p.slug}
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* Product Type */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {p.productType === 'PHYSICAL_NOTE' ? (
                        <span className="inline-flex items-center gap-1 rounded-md bg-blue-500/10 px-2 py-0.5 text-[11px] font-semibold text-blue-700 dark:text-blue-300">
                          <FileText size={12} />
                          <span>Note</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-md bg-purple-500/10 px-2 py-0.5 text-[11px] font-semibold text-purple-700 dark:text-purple-300">
                          <Video size={12} />
                          <span>Video</span>
                        </span>
                      )}
                    </td>

                    {/* Price in LKR */}
                    <td className="py-3.5 px-4 font-semibold text-foreground whitespace-nowrap">
                      {formatLKR(p.price)}
                    </td>

                    {/* Status Badge */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {p.status === 'PUBLISHED' && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2.5 py-0.5 text-[11px] font-semibold text-green-700 dark:text-green-300">
                          <span className="h-1.5 w-1.5 rounded-full bg-green-600 dark:bg-green-400" />
                          <span>Published</span>
                        </span>
                      )}
                      {p.status === 'DRAFT' && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-[11px] font-semibold text-amber-700 dark:text-amber-300">
                          <span className="h-1.5 w-1.5 rounded-full bg-amber-600 dark:bg-amber-400" />
                          <span>Draft</span>
                        </span>
                      )}
                      {p.status === 'ARCHIVED' && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-semibold text-muted-foreground">
                          <Archive size={11} />
                          <span>Archived</span>
                        </span>
                      )}
                    </td>

                    {/* Details / Stock */}
                    <td className="py-3.5 px-4 text-muted-foreground whitespace-nowrap">
                      {p.productType === 'PHYSICAL_NOTE' ? (
                        <div className="space-y-0.5">
                          <p className="text-[11px]">
                            Pages: <span className="font-semibold text-foreground">{p.pageCount ?? '-'}</span>
                          </p>
                          <p className="text-[11px]">
                            Stock:{' '}
                            <span
                              className={`font-semibold ${
                                (p.stockQuantity ?? 0) <= 0
                                  ? 'text-destructive'
                                  : 'text-foreground'
                              }`}
                            >
                              {p.stockQuantity ?? 0}
                            </span>
                          </p>
                        </div>
                      ) : (
                        <div className="space-y-0.5">
                          <p className="text-[11px] flex items-center gap-1">
                            <Clock size={11} />
                            <span>{p.durationSeconds ? formatDuration(p.durationSeconds) : '-'}</span>
                          </p>
                        </div>
                      )}
                    </td>

                    {/* Chapters linked */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="inline-flex items-center gap-1 text-[11px] text-muted-foreground bg-muted/50 px-2 py-0.5 rounded-md">
                        <Layers size={12} />
                        <span>{p.chapterCount} {p.chapterCount === 1 ? 'chapter' : 'chapters'}</span>
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center gap-1.5 justify-end">
                        {/* Edit Button */}
                        <button
                          type="button"
                          onClick={() => openEditModal(p)}
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-[11px] font-medium text-foreground hover:bg-muted transition"
                          title="Edit product"
                        >
                          <Edit2 size={12} />
                          <span>Edit</span>
                        </button>

                        {/* Status Transition Action Buttons */}
                        {p.status === 'DRAFT' && (
                          <>
                            <button
                              type="button"
                              onClick={() => openStatusModal(p, 'PUBLISHED')}
                              className="inline-flex items-center gap-1 rounded-lg bg-green-600/10 text-green-700 dark:text-green-300 hover:bg-green-600/20 px-2.5 py-1 text-[11px] font-semibold transition"
                              title="Publish product"
                            >
                              <Send size={12} />
                              <span>Publish</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => openStatusModal(p, 'ARCHIVED')}
                              className="inline-flex items-center gap-1 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted px-2 py-1 text-[11px] font-medium transition"
                              title="Archive product"
                            >
                              <Archive size={12} />
                            </button>
                          </>
                        )}

                        {p.status === 'PUBLISHED' && (
                          <button
                            type="button"
                            onClick={() => openStatusModal(p, 'ARCHIVED')}
                            className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-[11px] font-medium text-muted-foreground hover:text-destructive hover:border-destructive/30 transition"
                            title="Archive product"
                          >
                            <Archive size={12} />
                            <span>Archive</span>
                          </button>
                        )}

                        {p.status === 'ARCHIVED' && (
                          <button
                            type="button"
                            onClick={() => openStatusModal(p, 'DRAFT')}
                            className="inline-flex items-center gap-1 rounded-lg bg-blue-500/10 text-blue-700 dark:text-blue-300 hover:bg-blue-500/20 px-2.5 py-1 text-[11px] font-semibold transition"
                            title="Restore to Draft"
                          >
                            <RotateCcw size={12} />
                            <span>Restore</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-3 bg-muted/20 text-xs text-muted-foreground">
            <p>
              Page <span className="font-semibold text-foreground">{(filters.page ?? 0) + 1}</span> of{' '}
              <span className="font-semibold text-foreground">{totalPages}</span>
            </p>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={(filters.page ?? 0) <= 0}
                onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page ?? 0) - 1 }))}
                className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs disabled:opacity-40 disabled:cursor-not-allowed hover:bg-muted transition"
              >
                <ChevronLeft size={14} />
                <span>Previous</span>
              </button>
              <button
                type="button"
                disabled={(filters.page ?? 0) + 1 >= totalPages}
                onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page ?? 0) + 1 }))}
                className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs disabled:opacity-40 disabled:cursor-not-allowed hover:bg-muted transition"
              >
                <span>Next</span>
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ── Create / Edit Modal ────────────────────────────────────────────── */}
      {modalMode !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-xs">
          <div className="relative w-full max-w-2xl max-h-[90vh] flex flex-col rounded-2xl border border-border bg-card shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-border px-6 py-4">
              <div>
                <h2 className="text-base font-semibold text-foreground">
                  {modalMode === 'create' ? 'Create New Product' : 'Edit Product'}
                </h2>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {modalMode === 'create'
                    ? 'Products start in DRAFT status. Select type, set details, and link chapters.'
                    : 'Product type cannot be modified. Edit details, subtype fields, or linked chapters.'}
                </p>
              </div>
              <button
                type="button"
                onClick={closeModal}
                disabled={submitting}
                className="rounded-lg p-1 text-muted-foreground hover:text-foreground hover:bg-muted transition"
              >
                <X size={18} />
              </button>
            </div>

            {/* Modal Scrollable Body */}
            <form onSubmit={handleSubmit(onSubmit)} className="overflow-y-auto p-6 space-y-5">
              {/* Error banner */}
              {formError && (
                <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs font-medium text-destructive flex items-start gap-2">
                  <AlertCircle size={16} className="shrink-0 mt-0.5" />
                  <span>{formError}</span>
                </div>
              )}

              {/* Type selector (Locked on Edit) */}
              <div>
                <label className="block text-xs font-semibold text-foreground mb-1.5">
                  Product Type <span className="text-destructive">*</span>
                </label>
                {modalMode === 'create' ? (
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedType('PHYSICAL_NOTE');
                        setValue('productType', 'PHYSICAL_NOTE');
                      }}
                      className={`flex items-center gap-3 rounded-xl border p-3 text-left transition ${
                        selectedType === 'PHYSICAL_NOTE'
                          ? 'border-primary bg-primary/5 ring-1 ring-primary'
                          : 'border-border bg-card hover:bg-muted/50'
                      }`}
                    >
                      <div className="h-9 w-9 rounded-lg bg-blue-500/10 text-blue-600 flex items-center justify-center shrink-0">
                        <FileText size={18} />
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-foreground">Physical Note</p>
                        <p className="text-[11px] text-muted-foreground">
                          Printed book or syllabus summary notes
                        </p>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setSelectedType('VIDEO');
                        setValue('productType', 'VIDEO');
                      }}
                      className={`flex items-center gap-3 rounded-xl border p-3 text-left transition ${
                        selectedType === 'VIDEO'
                          ? 'border-primary bg-primary/5 ring-1 ring-primary'
                          : 'border-border bg-card hover:bg-muted/50'
                      }`}
                    >
                      <div className="h-9 w-9 rounded-lg bg-purple-500/10 text-purple-600 flex items-center justify-center shrink-0">
                        <Video size={18} />
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-foreground">Video Course</p>
                        <p className="text-[11px] text-muted-foreground">
                          On-demand recorded tutorial lectures
                        </p>
                      </div>
                    </button>
                  </div>
                ) : (
                  <div className="rounded-xl border border-border bg-muted/40 p-3 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      {selectedType === 'PHYSICAL_NOTE' ? (
                        <FileText size={16} className="text-blue-600" />
                      ) : (
                        <Video size={16} className="text-purple-600" />
                      )}
                      <span className="text-xs font-semibold text-foreground">
                        {selectedType === 'PHYSICAL_NOTE' ? 'Physical Note' : 'Video Course'}
                      </span>
                    </div>
                    <span className="text-[11px] font-medium text-muted-foreground">
                      Locked (cannot be changed)
                    </span>
                  </div>
                )}
              </div>

              {/* Name & Price */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="sm:col-span-2 space-y-1">
                  <label className="block text-xs font-semibold text-foreground">
                    Product Name <span className="text-destructive">*</span>
                  </label>
                  <input
                    type="text"
                    {...register('name')}
                    placeholder="e.g. Combined Mathematics Pure Calculus 2026"
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                  {errors.name && (
                    <p className="text-[11px] text-destructive">{errors.name.message}</p>
                  )}
                </div>

                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-foreground">
                    Price (LKR) <span className="text-destructive">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    {...register('price')}
                    placeholder="1500.00"
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                  {errors.price && (
                    <p className="text-[11px] text-destructive">{errors.price.message}</p>
                  )}
                </div>
              </div>

              {/* Description */}
              <div className="space-y-1">
                <label className="block text-xs font-semibold text-foreground">
                  Description
                </label>
                <textarea
                  rows={2}
                  {...register('description')}
                  placeholder="Overview, syllabus topics covered, target grade..."
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-y"
                />
              </div>

              {/* Thumbnail URL with live preview */}
              <div className="space-y-2">
                <label className="block text-xs font-semibold text-foreground">
                  Thumbnail Image URL
                </label>
                <div className="flex items-center gap-3">
                  <input
                    type="url"
                    {...register('thumbnailUrl')}
                    placeholder="https://example.com/images/cover.jpg"
                    className="flex-1 rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                  {watchedThumbUrl && (
                    <div className="h-10 w-10 rounded-lg border border-border overflow-hidden shrink-0 bg-muted">
                      <img
                        src={watchedThumbUrl}
                        alt="Preview"
                        className="h-full w-full object-cover"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                    </div>
                  )}
                </div>
              </div>

              {/* ── Subtype-specific fields ── */}
              {selectedType === 'PHYSICAL_NOTE' ? (
                <div className="rounded-xl border border-blue-500/20 bg-blue-500/5 p-4 space-y-3">
                  <p className="text-xs font-semibold text-blue-900 dark:text-blue-300 flex items-center gap-1.5">
                    <FileText size={14} />
                    <span>Physical Note Specifications</span>
                  </p>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <label className="block text-xs font-medium text-foreground">
                        Page Count <span className="text-destructive">*</span>
                      </label>
                      <input
                        type="number"
                        min="1"
                        {...register('pageCount')}
                        placeholder="120"
                        className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                      />
                      {errors.pageCount && (
                        <p className="text-[11px] text-destructive">
                          {errors.pageCount.message}
                        </p>
                      )}
                    </div>

                    <div className="space-y-1">
                      <label className="block text-xs font-medium text-foreground">
                        Stock Quantity (In Stock)
                      </label>
                      <input
                        type="number"
                        min="0"
                        {...register('stockQuantity')}
                        placeholder="0"
                        className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                      />
                      {errors.stockQuantity && (
                        <p className="text-[11px] text-destructive">
                          {errors.stockQuantity.message}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="rounded-xl border border-purple-500/20 bg-purple-500/5 p-4 space-y-3">
                  <p className="text-xs font-semibold text-purple-900 dark:text-purple-300 flex items-center gap-1.5">
                    <Video size={14} />
                    <span>Video Course Specifications</span>
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1 sm:col-span-2">
                      <label className="block text-xs font-medium text-foreground">
                        Video URL <span className="text-destructive">*</span>
                      </label>
                      <input
                        type="text"
                        {...register('videoUrl')}
                        placeholder="https://player.vimeo.com/video/..."
                        className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                      />
                      <p className="text-[10px] text-muted-foreground">
                        URL string only; no video media files are stored on this server.
                      </p>
                    </div>

                    <div className="space-y-1">
                      <label className="block text-xs font-medium text-foreground">
                        Duration in Seconds <span className="text-destructive">*</span>
                      </label>
                      <input
                        type="number"
                        min="1"
                        {...register('durationSeconds')}
                        placeholder="3600 (1 hour)"
                        className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                      />
                      {watchedDuration && watchedDuration > 0 && (
                        <p className="text-[11px] text-purple-600 dark:text-purple-400 font-medium">
                          Formatted: {formatDuration(watchedDuration)}
                        </p>
                      )}
                    </div>

                    <div className="space-y-1">
                      <label className="block text-xs font-medium text-foreground">
                        Video Description / Timestamps
                      </label>
                      <input
                        type="text"
                        {...register('videoDescription')}
                        placeholder="Resolution, lecture notes, or timestamps"
                        className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 transition"
                      />
                    </div>
                  </div>
                </div>
              )}

              {/* ── Linked Chapters Multi-Select (Grouped by Subject) ── */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-semibold text-foreground">
                    Linked Chapters{' '}
                    <span className="text-muted-foreground font-normal">
                      ({selectedChapterIds.length} selected)
                    </span>
                  </label>
                  {selectedChapterIds.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setSelectedChapterIds([])}
                      className="text-[11px] text-destructive hover:underline"
                    >
                      Clear Selection
                    </button>
                  )}
                </div>

                {loadingCurriculum ? (
                  <p className="text-xs text-muted-foreground py-2">Loading curriculum chapters...</p>
                ) : Object.keys(chaptersBySubject).length === 0 ? (
                  <p className="text-xs text-muted-foreground py-2">
                    No curriculum subjects or chapters found. Create subjects and chapters first.
                  </p>
                ) : (
                  <div className="rounded-xl border border-border bg-muted/20 p-3 max-h-56 overflow-y-auto space-y-3">
                    {Object.entries(chaptersBySubject).map(([subjectName, subjectChapters]) => (
                      <div key={subjectName} className="space-y-1.5">
                        <div className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-muted-foreground border-b border-border/50 pb-1">
                          <BookOpen size={12} />
                          <span>{subjectName}</span>
                          <span className="text-[10px] font-normal lowercase">
                            ({subjectChapters.length})
                          </span>
                        </div>
                        {subjectChapters.length === 0 ? (
                          <p className="text-[11px] text-muted-foreground/60 italic pl-3">
                            No chapters created under this subject yet.
                          </p>
                        ) : (
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 pl-2">
                            {subjectChapters.map((chap) => {
                              const isChecked = selectedChapterIds.includes(chap.chapterId);
                              return (
                                <label
                                  key={chap.chapterId}
                                  className={`flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-xs cursor-pointer select-none transition ${
                                    isChecked
                                      ? 'bg-primary/10 text-primary font-medium'
                                      : 'hover:bg-muted text-foreground'
                                  }`}
                                >
                                  <input
                                    type="checkbox"
                                    checked={isChecked}
                                    onChange={() => toggleChapterSelection(chap.chapterId)}
                                    className="rounded border-border text-primary focus:ring-primary/20 h-3.5 w-3.5"
                                  />
                                  <span className="line-clamp-1">
                                    <span className="opacity-60 mr-1 font-mono">
                                      #{chap.chapterOrder}
                                    </span>
                                    {chap.name}
                                  </span>
                                </label>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Modal Actions */}
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={submitting}
                  className="rounded-xl border border-border px-4 py-2 text-xs font-medium text-muted-foreground hover:bg-muted transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-5 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition disabled:opacity-50"
                >
                  {submitting && <Loader2 size={13} className="animate-spin" />}
                  <span>{modalMode === 'create' ? 'Create Product' : 'Save Changes'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Status Transition Confirmation Modal ───────────────────────────── */}
      {statusModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-xs">
          <div className="relative w-full max-w-md rounded-2xl border border-border bg-card shadow-xl p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3">
              <div
                className={`h-10 w-10 rounded-xl flex items-center justify-center ${
                  statusModal.targetStatus === 'PUBLISHED'
                    ? 'bg-green-500/10 text-green-600'
                    : statusModal.targetStatus === 'ARCHIVED'
                    ? 'bg-muted text-muted-foreground'
                    : 'bg-blue-500/10 text-blue-600'
                }`}
              >
                {statusModal.targetStatus === 'PUBLISHED' && <Send size={20} />}
                {statusModal.targetStatus === 'ARCHIVED' && <Archive size={20} />}
                {statusModal.targetStatus === 'DRAFT' && <RotateCcw size={20} />}
              </div>
              <div>
                <h3 className="text-sm font-semibold text-foreground">
                  Confirm Status Change
                </h3>
                <p className="text-xs text-muted-foreground">
                  Moving product to{' '}
                  <span className="font-semibold text-foreground uppercase">
                    {statusModal.targetStatus}
                  </span>
                </p>
              </div>
            </div>

            {/* Error banner (e.g. backend missing publishing requirements) */}
            {statusError && (
              <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs font-medium text-destructive space-y-1">
                <div className="flex items-center gap-1.5 font-semibold">
                  <AlertCircle size={15} />
                  <span>Cannot Publish Product</span>
                </div>
                <p className="text-[11px] leading-relaxed">{statusError}</p>
              </div>
            )}

            <p className="text-xs text-muted-foreground leading-relaxed">
              Are you sure you want to change the status of{' '}
              <span className="font-semibold text-foreground">"{statusModal.product.name}"</span>{' '}
              from{' '}
              <span className="font-semibold uppercase">{statusModal.product.status}</span> to{' '}
              <span className="font-semibold uppercase">{statusModal.targetStatus}</span>?
              {statusModal.targetStatus === 'PUBLISHED' &&
                ' This will make the product visible to learners once public catalog routes are active.'}
              {statusModal.targetStatus === 'ARCHIVED' &&
                ' Archiving will remove it from learner views without permanently deleting it.'}
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setStatusModal(null)}
                disabled={statusSubmitting}
                className="rounded-xl border border-border px-3.5 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmStatusChange}
                disabled={statusSubmitting}
                className={`inline-flex items-center gap-1.5 rounded-xl px-4 py-1.5 text-xs font-semibold shadow-sm transition disabled:opacity-50 ${
                  statusModal.targetStatus === 'PUBLISHED'
                    ? 'bg-green-600 text-white hover:bg-green-700'
                    : statusModal.targetStatus === 'ARCHIVED'
                    ? 'bg-destructive text-destructive-foreground hover:opacity-90'
                    : 'bg-primary text-primary-foreground hover:opacity-90'
                }`}
              >
                {statusSubmitting && <Loader2 size={13} className="animate-spin" />}
                <span>
                  Confirm{' '}
                  {statusModal.targetStatus === 'PUBLISHED'
                    ? 'Publish'
                    : statusModal.targetStatus === 'ARCHIVED'
                    ? 'Archive'
                    : 'Restore'}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
