import { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod/v4';
import {
  getAdminChapters,
  createAdminChapter,
  updateAdminChapter,
  setAdminChapterStatus,
  type ChapterResponse,
} from '../../../api/chapterApi';
import type { ApiError } from '../../../api/authApi';
import {
  BookOpen,
  Plus,
  Edit2,
  Power,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  X,
  Loader2,
  ChevronLeft,
  ListOrdered,
  Hash,
} from 'lucide-react';

// ---------------------------------------------------------------------------
// Form schema
// ---------------------------------------------------------------------------

const chapterSchema = z.object({
  name: z
    .string()
    .min(1, 'Chapter name is required')
    .max(300, 'Chapter name must be at most 300 characters'),
  chapterOrder: z
    .string()
    .optional()
    .transform((v) => (v && v.trim() !== '' ? parseInt(v, 10) : undefined))
    .pipe(
      z
        .number({ error: 'Chapter order must be a positive integer' })
        .int('Chapter order must be a whole number')
        .positive('Chapter order must be a positive integer')
        .optional(),
    ),
  description: z
    .string()
    .max(10000, 'Description must be at most 10 000 characters')
    .optional()
    .or(z.literal('')),
});

type ChapterFormValues = z.infer<typeof chapterSchema>;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function AdminChaptersPage() {
  const { subjectId } = useParams<{ subjectId: string }>();
  const navigate = useNavigate();

  const [chapters, setChapters] = useState<ChapterResponse[]>([]);
  const [subjectName, setSubjectName] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  const [modalMode, setModalMode] = useState<'create' | number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [statusTogglingId, setStatusTogglingId] = useState<number | null>(null);

  const numericSubjectId = subjectId ? parseInt(subjectId, 10) : null;

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ChapterFormValues>({
    resolver: zodResolver(chapterSchema) as any,
    defaultValues: { name: '', chapterOrder: undefined, description: '' },
  });

  const fetchChapters = useCallback(async () => {
    if (!numericSubjectId) return;
    setLoading(true);
    setPageError(null);
    try {
      const data = await getAdminChapters(numericSubjectId);
      setChapters(data);
      // Derive subject name from first chapter if available
      // (AdminLayout doesn't pass it; we'll use a fallback)
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr?.status === 404) {
        setPageError('Subject not found.');
      } else {
        setPageError(apiErr?.message || 'Failed to load chapters.');
      }
    } finally {
      setLoading(false);
    }
  }, [numericSubjectId]);

  useEffect(() => {
    if (!numericSubjectId) {
      navigate('/admin/subjects', { replace: true });
      return;
    }
    fetchChapters();
  }, [numericSubjectId, fetchChapters, navigate]);

  // Keep subjectName synced if we have chapters
  useEffect(() => {
    if (chapters.length > 0 && !subjectName) {
      // We don't have the name directly; label it by id as a fallback
      setSubjectName(`Subject #${numericSubjectId}`);
    }
  }, [chapters, subjectName, numericSubjectId]);

  // ── Modal helpers ──────────────────────────────────────────────────────────

  function openCreateModal() {
    reset({ name: '', chapterOrder: undefined, description: '' });
    setFormError(null);
    setModalMode('create');
  }

  function openEditModal(chapter: ChapterResponse) {
    reset({
      name: chapter.name,
      chapterOrder: chapter.chapterOrder as unknown as undefined, // cast for string field
      description: chapter.description ?? '',
    });
    setFormError(null);
    setModalMode(chapter.chapterId);
  }

  function closeModal() {
    setModalMode(null);
    setFormError(null);
    reset();
  }

  async function onSubmit(values: ChapterFormValues) {
    if (!numericSubjectId) return;
    setSubmitting(true);
    setFormError(null);

    try {
      if (modalMode === 'create') {
        const created = await createAdminChapter({
          subjectId: numericSubjectId,
          name: values.name.trim(),
          chapterOrder: values.chapterOrder ?? undefined,
          description: values.description?.trim() || null,
        });
        setChapters((prev) =>
          [...prev, created].sort((a, b) => a.chapterOrder - b.chapterOrder),
        );
        setActionSuccess(`Chapter "${created.name}" created (order ${created.chapterOrder}).`);
      } else if (typeof modalMode === 'number') {
        const updated = await updateAdminChapter(modalMode, {
          name: values.name.trim(),
          chapterOrder: values.chapterOrder ?? undefined,
          description: values.description?.trim() || null,
        });
        setChapters((prev) =>
          prev
            .map((c) => (c.chapterId === modalMode ? updated : c))
            .sort((a, b) => a.chapterOrder - b.chapterOrder),
        );
        setActionSuccess(`Chapter "${updated.name}" updated.`);
      }
      closeModal();
      setTimeout(() => setActionSuccess(null), 5000);
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr?.status === 409) {
        setError('chapterOrder', {
          message: apiErr.message || 'That chapter order is already taken in this subject.',
        });
        setFormError(apiErr.message || 'Order conflict: that position is already taken.');
      } else {
        setFormError(apiErr?.message || 'Failed to save chapter. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(chapter: ChapterResponse) {
    const nextStatus = chapter.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setStatusTogglingId(chapter.chapterId);
    setActionSuccess(null);
    try {
      const updated = await setAdminChapterStatus(chapter.chapterId, nextStatus);
      setChapters((prev) =>
        prev.map((c) => (c.chapterId === chapter.chapterId ? updated : c)),
      );
      setActionSuccess(
        `Chapter "${chapter.name}" is now ${nextStatus === 'ACTIVE' ? 'Active' : 'Inactive'}.`,
      );
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      const apiErr = err as ApiError;
      setPageError(apiErr?.message || 'Failed to update chapter status.');
    } finally {
      setStatusTogglingId(null);
    }
  }

  const activeCount = chapters.filter((c) => c.status === 'ACTIVE').length;
  const inactiveCount = chapters.filter((c) => c.status === 'INACTIVE').length;

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          {/* Back link */}
          <Link
            to="/admin/subjects"
            className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition mb-1"
          >
            <ChevronLeft size={14} />
            <span>Back to Subjects</span>
          </Link>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Chapters</h1>
            <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
              Subject #{numericSubjectId}
            </span>
            <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
              {chapters.length} Total
            </span>
          </div>
          <p className="text-sm text-muted-foreground">
            Manage chapters, order, and visibility for this subject.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchChapters}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-xl border border-border px-3 py-2 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="Refresh chapters"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            id="admin-create-chapter-btn"
            type="button"
            onClick={openCreateModal}
            className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition"
          >
            <Plus size={15} />
            <span>Add Chapter</span>
          </button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="rounded-2xl border border-border bg-card p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-muted-foreground">Total Chapters</p>
            <p className="text-2xl font-bold text-foreground mt-1">{chapters.length}</p>
          </div>
          <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
            <ListOrdered size={20} />
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-muted-foreground">Active (Visible)</p>
            <p className="text-2xl font-bold text-green-600 mt-1">{activeCount}</p>
          </div>
          <div className="h-10 w-10 rounded-xl bg-green-500/10 text-green-600 flex items-center justify-center">
            <CheckCircle2 size={20} />
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-muted-foreground">Inactive (Hidden)</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">{inactiveCount}</p>
          </div>
          <div className="h-10 w-10 rounded-xl bg-amber-500/10 text-amber-600 flex items-center justify-center">
            <Power size={20} />
          </div>
        </div>
      </div>

      {/* Banners */}
      {actionSuccess && (
        <div
          id="admin-chapter-success-banner"
          className="rounded-xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-xs font-medium text-green-700 dark:text-green-300 flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} className="shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button type="button" onClick={() => setActionSuccess(null)} className="text-muted-foreground hover:text-foreground">
            <X size={14} />
          </button>
        </div>
      )}

      {pageError && (
        <div
          id="admin-chapter-error-banner"
          className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-xs font-medium text-destructive flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <AlertCircle size={16} className="shrink-0" />
            <span>{pageError}</span>
          </div>
          <button type="button" onClick={() => setPageError(null)} className="text-muted-foreground hover:text-foreground">
            <X size={14} />
          </button>
        </div>
      )}

      {/* Table */}
      <div className="rounded-2xl border border-border bg-card overflow-hidden shadow-sm">
        {loading && chapters.length === 0 ? (
          <div className="p-12 flex flex-col items-center justify-center gap-3">
            <Loader2 size={28} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading chapters…</p>
          </div>
        ) : chapters.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="mx-auto h-12 w-12 rounded-full bg-muted flex items-center justify-center text-muted-foreground">
              <BookOpen size={24} />
            </div>
            <h3 className="text-sm font-semibold text-foreground">No chapters yet</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              Add the first chapter for this subject.
            </p>
            <button
              type="button"
              onClick={openCreateModal}
              className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-3.5 py-1.5 text-xs font-semibold hover:opacity-90 transition"
            >
              <Plus size={14} />
              Add Chapter
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/40 text-muted-foreground font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-4 w-16">ID</th>
                  <th className="py-3 px-4 w-16">Order</th>
                  <th className="py-3 px-4">Chapter Name</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 w-28">Status</th>
                  <th className="py-3 px-4 w-40 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {chapters.map((ch) => {
                  const isActive = ch.status === 'ACTIVE';
                  const isToggling = statusTogglingId === ch.chapterId;

                  return (
                    <tr
                      key={ch.chapterId}
                      className="hover:bg-muted/20 transition-colors"
                      id={`chapter-row-${ch.chapterId}`}
                    >
                      <td className="py-3.5 px-4 font-mono text-muted-foreground">#{ch.chapterId}</td>

                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1 font-bold text-primary bg-primary/10 rounded-lg px-2 py-0.5">
                          <Hash size={11} />
                          {ch.chapterOrder}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 font-semibold text-foreground">{ch.name}</td>

                      <td className="py-3.5 px-4 text-muted-foreground max-w-xs truncate">
                        {ch.description || <span className="italic opacity-50">No description</span>}
                      </td>

                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                            isActive
                              ? 'bg-green-500/10 text-green-700 dark:text-green-300 border border-green-500/20'
                              : 'bg-muted text-muted-foreground border border-border'
                          }`}
                        >
                          <span className={`h-1.5 w-1.5 rounded-full ${isActive ? 'bg-green-600' : 'bg-muted-foreground'}`} />
                          {ch.status}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div className="inline-flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEditModal(ch)}
                            id={`edit-chapter-btn-${ch.chapterId}`}
                            className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-[11px] font-medium text-foreground hover:bg-muted transition"
                            title="Edit Chapter"
                          >
                            <Edit2 size={12} />
                            <span>Edit</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => handleToggleStatus(ch)}
                            disabled={isToggling}
                            id={`toggle-chapter-status-btn-${ch.chapterId}`}
                            className={`inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-[11px] font-medium transition disabled:opacity-50 ${
                              isActive
                                ? 'border-amber-500/30 text-amber-700 dark:text-amber-300 hover:bg-amber-500/10'
                                : 'border-green-500/30 text-green-700 dark:text-green-300 hover:bg-green-500/10'
                            }`}
                            title={isActive ? 'Deactivate chapter' : 'Reactivate chapter'}
                          >
                            {isToggling ? (
                              <Loader2 size={12} className="animate-spin" />
                            ) : (
                              <Power size={12} />
                            )}
                            <span>{isActive ? 'Deactivate' : 'Activate'}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create / Edit Modal */}
      {modalMode !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl relative space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2">
                <div className="h-8 w-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                  <ListOrdered size={16} />
                </div>
                <h3 className="font-semibold text-base text-foreground">
                  {modalMode === 'create' ? 'Add Chapter' : 'Edit Chapter'}
                </h3>
              </div>
              <button
                type="button"
                onClick={closeModal}
                disabled={submitting}
                className="rounded-lg p-1.5 text-muted-foreground hover:text-foreground hover:bg-muted transition"
              >
                <X size={16} />
              </button>
            </div>

            {/* 409 / generic form error */}
            {formError && (
              <div
                id="chapter-form-error"
                className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive flex items-start gap-2"
              >
                <AlertCircle size={15} className="shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Name */}
              <div className="space-y-1.5">
                <label htmlFor="chapter-name" className="block text-xs font-semibold text-foreground">
                  Chapter Name <span className="text-destructive">*</span>
                </label>
                <input
                  id="chapter-name"
                  type="text"
                  placeholder="e.g. Introduction to Mechanics"
                  {...register('name')}
                  disabled={submitting}
                  className={`w-full rounded-xl border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition ${
                    errors.name ? 'border-destructive focus:ring-destructive/50' : 'border-border'
                  }`}
                />
                {errors.name && (
                  <p className="text-[11px] text-destructive font-medium">{errors.name.message}</p>
                )}
              </div>

              {/* Order */}
              <div className="space-y-1.5">
                <label htmlFor="chapter-order" className="block text-xs font-semibold text-foreground">
                  Chapter Order{' '}
                  <span className="text-muted-foreground font-normal">(Optional — leave blank to auto-append)</span>
                </label>
                <input
                  id="chapter-order"
                  type="number"
                  min={1}
                  placeholder="e.g. 3"
                  {...register('chapterOrder')}
                  disabled={submitting}
                  className={`w-full rounded-xl border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition ${
                    errors.chapterOrder ? 'border-destructive focus:ring-destructive/50' : 'border-border'
                  }`}
                />
                {errors.chapterOrder && (
                  <p className="text-[11px] text-destructive font-medium">{errors.chapterOrder.message as string}</p>
                )}
                <p className="text-[11px] text-muted-foreground">
                  Order must be unique within this subject. Inactive chapters still hold their position.
                </p>
              </div>

              {/* Description */}
              <div className="space-y-1.5">
                <label htmlFor="chapter-description" className="block text-xs font-semibold text-foreground">
                  Description <span className="text-muted-foreground font-normal">(Optional)</span>
                </label>
                <textarea
                  id="chapter-description"
                  rows={4}
                  placeholder="Overview of topics covered in this chapter…"
                  {...register('description')}
                  disabled={submitting}
                  className={`w-full rounded-xl border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition resize-y ${
                    errors.description ? 'border-destructive focus:ring-destructive/50' : 'border-border'
                  }`}
                />
                {errors.description && (
                  <p className="text-[11px] text-destructive font-medium">{errors.description.message}</p>
                )}
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={submitting}
                  className="rounded-xl border border-border px-3.5 py-2 text-xs font-medium text-foreground hover:bg-muted transition"
                >
                  Cancel
                </button>

                <button
                  id="chapter-form-submit-btn"
                  type="submit"
                  disabled={submitting}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition disabled:opacity-50"
                >
                  {submitting && <Loader2 size={13} className="animate-spin" />}
                  <span>{modalMode === 'create' ? 'Add Chapter' : 'Save Changes'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
