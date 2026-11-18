import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  getAdminAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  updateAnnouncementStatus,
  type AdminAnnouncementResponse,
  type AnnouncementStatus,
} from '../../../api/communicationApi';
import type { PageResponse } from '../../../api/classApi';
import {
  Bell,
  Plus,
  Edit2,
  Send,
  Archive,
  RotateCcw,
  Loader2,
  Calendar,
  User,
  X,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Filter,
} from 'lucide-react';

const announcementFormSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, 'Title is required')
    .max(150, 'Title must not exceed 150 characters'),
  content: z
    .string()
    .trim()
    .min(1, 'Content is required')
    .max(5000, 'Content must not exceed 5000 characters'),
});

type AnnouncementFormData = z.infer<typeof announcementFormSchema>;

export default function AdminAnnouncementsPage() {
  const [pageData, setPageData] = useState<PageResponse<AdminAnnouncementResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<AnnouncementStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal states: Create / Edit
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<AdminAnnouncementResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Confirmation dialog for status transition
  const [confirmDialog, setConfirmDialog] = useState<{
    announcement: AdminAnnouncementResponse;
    targetStatus: AnnouncementStatus;
    title: string;
    description: string;
  } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AnnouncementFormData>({
    resolver: zodResolver(announcementFormSchema),
    defaultValues: {
      title: '',
      content: '',
    },
  });

  const titleVal = watch('title') || '';
  const contentVal = watch('content') || '';

  useEffect(() => {
    loadAnnouncements(currentPage, statusFilter || undefined);
  }, [currentPage, statusFilter]);

  async function loadAnnouncements(page: number, status?: AnnouncementStatus) {
    setLoading(true);
    setError(null);
    try {
      const data = await getAdminAnnouncements(status, page, 15);
      setPageData(data);
    } catch {
      setError('Failed to load announcements');
    } finally {
      setLoading(false);
    }
  }

  function handleOpenCreate() {
    setEditingItem(null);
    setFormError(null);
    reset({ title: '', content: '' });
    setModalOpen(true);
  }

  function handleOpenEdit(item: AdminAnnouncementResponse) {
    setEditingItem(item);
    setFormError(null);
    reset({ title: item.title, content: item.content });
    setModalOpen(true);
  }

  async function onSubmitForm(data: AnnouncementFormData) {
    setSubmitting(true);
    setFormError(null);
    try {
      if (editingItem) {
        await updateAnnouncement(editingItem.id, {
          title: data.title,
          content: data.content,
        });
      } else {
        await createAnnouncement({
          title: data.title,
          content: data.content,
        });
      }
      setModalOpen(false);
      loadAnnouncements(currentPage, statusFilter || undefined);
    } catch (err: unknown) {
      const anyErr = err as { message?: string };
      setFormError(anyErr?.message || 'Failed to save announcement');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirmStatusTransition() {
    if (!confirmDialog) return;
    setTransitioning(true);
    try {
      await updateAnnouncementStatus(confirmDialog.announcement.id, confirmDialog.targetStatus);
      setConfirmDialog(null);
      loadAnnouncements(currentPage, statusFilter || undefined);
    } catch (err: unknown) {
      const anyErr = err as { message?: string };
      alert(anyErr?.message || 'Failed to update announcement status');
    } finally {
      setTransitioning(false);
    }
  }

  function promptPublish(item: AdminAnnouncementResponse) {
    setConfirmDialog({
      announcement: item,
      targetStatus: 'PUBLISHED',
      title: 'Publish Announcement?',
      description: `This will make "${item.title}" visible publicly to all students and guests immediately. Published timestamp will be updated to now.`,
    });
  }

  function promptArchive(item: AdminAnnouncementResponse) {
    setConfirmDialog({
      announcement: item,
      targetStatus: 'ARCHIVED',
      title: 'Archive Announcement?',
      description: `This will remove "${item.title}" from the public announcement feed. You can restore it to draft later if needed.`,
    });
  }

  function promptRestoreToDraft(item: AdminAnnouncementResponse) {
    setConfirmDialog({
      announcement: item,
      targetStatus: 'DRAFT',
      title: 'Move to Draft?',
      description: `This will move "${item.title}" back to DRAFT mode so you can revise it before publishing again.`,
    });
  }

  function getStatusBadge(status: AnnouncementStatus) {
    switch (status) {
      case 'PUBLISHED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border border-emerald-500/20">
            PUBLISHED
          </span>
        );
      case 'DRAFT':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/20">
            DRAFT
          </span>
        );
      case 'ARCHIVED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-muted text-muted-foreground border border-border">
            ARCHIVED
          </span>
        );
    }
  }

  function formatDate(iso: string | null) {
    if (!iso) return '—';
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return iso;
    }
  }

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <Bell size={20} className="text-primary" />
            <span>Announcements Management</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Create, edit, and publish platform announcements and notices for students.
          </p>
        </div>

        <button
          type="button"
          id="btn-create-announcement"
          onClick={handleOpenCreate}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:opacity-90 transition"
        >
          <Plus size={14} />
          <span>New Announcement</span>
        </button>
      </div>

      {/* Filter row */}
      <div className="flex items-center gap-2">
        <Filter size={14} className="text-muted-foreground" />
        <span className="text-xs text-muted-foreground font-medium">Filter by Status:</span>
        <div className="flex items-center gap-1.5">
          {(['', 'DRAFT', 'PUBLISHED', 'ARCHIVED'] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => {
                setStatusFilter(st);
                setCurrentPage(0);
              }}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium transition ${
                statusFilter === st
                  ? 'bg-primary text-primary-foreground font-semibold shadow-xs'
                  : 'bg-card border border-border text-muted-foreground hover:bg-muted hover:text-foreground'
              }`}
            >
              {st === '' ? 'All' : st}
            </button>
          ))}
        </div>
      </div>

      {/* Table view */}
      <div className="bg-card rounded-2xl border border-border overflow-hidden shadow-xs">
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-2 text-muted-foreground">
            <Loader2 size={24} className="animate-spin text-primary" />
            <span className="text-xs">Loading announcements…</span>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-xs text-destructive">{error}</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="py-16 text-center text-xs text-muted-foreground space-y-2">
            <Bell size={24} className="mx-auto opacity-40" />
            <p>No announcements found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-muted/50 border-b border-border text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Title</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Creator</th>
                  <th className="py-3 px-4">Published Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {pageData.content.map((item) => (
                  <tr key={item.id} id={`admin-announcement-row-${item.id}`} className="hover:bg-muted/30 transition">
                    <td className="py-3.5 px-4 font-semibold text-foreground max-w-xs truncate">
                      {item.title}
                    </td>
                    <td className="py-3.5 px-4">{getStatusBadge(item.status)}</td>
                    <td className="py-3.5 px-4 text-muted-foreground">
                      <div className="flex items-center gap-1.5">
                        <User size={12} className="opacity-60" />
                        <span>{item.createdByName}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-muted-foreground">
                      <div className="flex items-center gap-1.5">
                        <Calendar size={12} className="opacity-60" />
                        <span>{formatDate(item.publishedAt)}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Edit allowed in any status */}
                        <button
                          type="button"
                          id={`btn-edit-announcement-${item.id}`}
                          onClick={() => handleOpenEdit(item)}
                          className="px-2.5 py-1 rounded-lg border border-border hover:bg-muted text-foreground transition text-xs font-medium inline-flex items-center gap-1"
                          title="Edit title & content"
                        >
                          <Edit2 size={12} />
                          <span>Edit</span>
                        </button>

                        {/* Transitions based on current status */}
                        {item.status === 'DRAFT' && (
                          <>
                            <button
                              type="button"
                              id={`btn-publish-announcement-${item.id}`}
                              onClick={() => promptPublish(item)}
                              className="px-2.5 py-1 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 transition text-xs font-semibold inline-flex items-center gap-1 shadow-xs"
                            >
                              <Send size={12} />
                              <span>Publish</span>
                            </button>
                            <button
                              type="button"
                              id={`btn-archive-announcement-${item.id}`}
                              onClick={() => promptArchive(item)}
                              className="px-2 py-1 rounded-lg border border-border hover:bg-muted text-muted-foreground transition text-xs"
                              title="Archive"
                            >
                              <Archive size={12} />
                            </button>
                          </>
                        )}

                        {item.status === 'PUBLISHED' && (
                          <button
                            type="button"
                            id={`btn-archive-announcement-${item.id}`}
                            onClick={() => promptArchive(item)}
                            className="px-2.5 py-1 rounded-lg border border-amber-500/30 text-amber-700 dark:text-amber-300 hover:bg-amber-500/10 transition text-xs font-semibold inline-flex items-center gap-1"
                          >
                            <Archive size={12} />
                            <span>Archive</span>
                          </button>
                        )}

                        {item.status === 'ARCHIVED' && (
                          <button
                            type="button"
                            id={`btn-restore-announcement-${item.id}`}
                            onClick={() => promptRestoreToDraft(item)}
                            className="px-2.5 py-1 rounded-lg border border-border text-foreground hover:bg-muted transition text-xs font-medium inline-flex items-center gap-1"
                          >
                            <RotateCcw size={12} />
                            <span>To Draft</span>
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

        {/* Pagination */}
        {pageData && pageData.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-card/60">
            <span className="text-[11px] text-muted-foreground">
              Showing page {pageData.number + 1} of {pageData.totalPages} ({pageData.totalElements} total)
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={pageData.first}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="p-1 rounded-lg border border-border disabled:opacity-40 text-foreground hover:bg-muted transition"
              >
                <ChevronLeft size={14} />
              </button>
              <button
                type="button"
                disabled={pageData.last}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="p-1 rounded-lg border border-border disabled:opacity-40 text-foreground hover:bg-muted transition"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-lg w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-border">
              <h3 className="text-base font-bold text-foreground">
                {editingItem ? 'Edit Announcement' : 'Create New Announcement'}
              </h3>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="p-1 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit(onSubmitForm)} className="space-y-4 pt-4">
              {formError && (
                <div className="p-3 rounded-xl bg-destructive/10 text-destructive text-xs">
                  {formError}
                </div>
              )}

              {/* Title input with character counter */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label htmlFor="announcement-title-input" className="text-xs font-semibold text-foreground">
                    Title <span className="text-destructive">*</span>
                  </label>
                  <span className={`text-[10px] ${titleVal.length > 150 ? 'text-destructive font-bold' : 'text-muted-foreground'}`}>
                    {titleVal.length} / 150
                  </span>
                </div>
                <input
                  id="announcement-title-input"
                  type="text"
                  placeholder="e.g. 2026 Examination Revision Timetable Released"
                  {...register('title')}
                  className={`w-full rounded-xl border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 ${
                    errors.title ? 'border-destructive' : 'border-border'
                  }`}
                />
                {errors.title && (
                  <p className="mt-1 text-[11px] text-destructive">{errors.title.message}</p>
                )}
              </div>

              {/* Content textarea with character counter */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label htmlFor="announcement-content-input" className="text-xs font-semibold text-foreground">
                    Content (Plain Text) <span className="text-destructive">*</span>
                  </label>
                  <span className={`text-[10px] ${contentVal.length > 5000 ? 'text-destructive font-bold' : 'text-muted-foreground'}`}>
                    {contentVal.length} / 5000
                  </span>
                </div>
                <textarea
                  id="announcement-content-input"
                  rows={6}
                  placeholder="Write announcement details. Line breaks will be preserved when displayed."
                  {...register('content')}
                  className={`w-full rounded-xl border bg-background p-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 font-sans whitespace-pre-wrap ${
                    errors.content ? 'border-destructive' : 'border-border'
                  }`}
                />
                {errors.content && (
                  <p className="mt-1 text-[11px] text-destructive">{errors.content.message}</p>
                )}
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-3 py-2 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="btn-save-announcement"
                  disabled={submitting}
                  className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center gap-1.5 shadow-xs"
                >
                  {submitting ? (
                    <>
                      <Loader2 size={13} className="animate-spin" />
                      <span>Saving…</span>
                    </>
                  ) : (
                    <span>{editingItem ? 'Save Changes' : 'Create as Draft'}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Dialog for Status Transitions */}
      {confirmDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-600 mb-4 mx-auto">
              <AlertTriangle size={24} />
            </div>

            <h3 className="text-base font-bold text-center text-foreground">
              {confirmDialog.title}
            </h3>

            <p className="text-xs text-center text-muted-foreground mt-2 mb-6 leading-relaxed">
              {confirmDialog.description}
            </p>

            <div className="flex flex-col gap-2">
              <button
                type="button"
                id="btn-confirm-announcement-status"
                disabled={transitioning}
                onClick={handleConfirmStatusTransition}
                className={`w-full py-2.5 px-4 rounded-xl text-white text-xs font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center justify-center gap-1.5 ${
                  confirmDialog.targetStatus === 'PUBLISHED'
                    ? 'bg-emerald-600'
                    : confirmDialog.targetStatus === 'ARCHIVED'
                    ? 'bg-amber-600'
                    : 'bg-primary'
                }`}
              >
                {transitioning ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    <span>Updating status…</span>
                  </>
                ) : (
                  <span>Confirm Status Update</span>
                )}
              </button>
              <button
                type="button"
                disabled={transitioning}
                onClick={() => setConfirmDialog(null)}
                className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
