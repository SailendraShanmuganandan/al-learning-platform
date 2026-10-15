import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod/v4';
import {
  getAdminSubjects,
  createAdminSubject,
  updateAdminSubject,
  setAdminSubjectStatus,
  type SubjectResponse,
} from '../../../api/subjectApi';
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
  Calendar,
  Layers,
} from 'lucide-react';

const subjectSchema = z.object({
  name: z
    .string()
    .min(1, 'Subject name is required')
    .max(200, 'Subject name must be at most 200 characters'),
  description: z
    .string()
    .max(5000, 'Description must be at most 5000 characters')
    .optional()
    .or(z.literal('')),
});

type SubjectFormValues = z.infer<typeof subjectSchema>;

export default function AdminSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  // Form modal state (null = closed; 'create' = new subject; number = editing subjectId)
  const [modalMode, setModalMode] = useState<'create' | number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Status toggle state tracker: subjectId -> boolean
  const [statusTogglingId, setStatusTogglingId] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<SubjectFormValues>({
    resolver: zodResolver(subjectSchema),
    defaultValues: { name: '', description: '' },
  });

  const fetchSubjects = useCallback(async () => {
    setLoading(true);
    setPageError(null);
    try {
      const data = await getAdminSubjects();
      setSubjects(data);
    } catch (err) {
      const apiErr = err as ApiError;
      setPageError(apiErr?.message || 'Failed to load subjects.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSubjects();
  }, [fetchSubjects]);

  function openCreateModal() {
    reset({ name: '', description: '' });
    setFormError(null);
    setModalMode('create');
  }

  function openEditModal(subject: SubjectResponse) {
    reset({
      name: subject.name,
      description: subject.description ?? '',
    });
    setFormError(null);
    setModalMode(subject.subjectId);
  }

  function closeModal() {
    setModalMode(null);
    setFormError(null);
    reset();
  }

  async function onSubmit(values: SubjectFormValues) {
    setSubmitting(true);
    setFormError(null);

    const payload = {
      name: values.name.trim(),
      description: values.description?.trim() ? values.description.trim() : null,
    };

    try {
      if (modalMode === 'create') {
        const created = await createAdminSubject(payload);
        setSubjects((prev) => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)));
        setActionSuccess(`Subject "${created.name}" created successfully.`);
      } else if (typeof modalMode === 'number') {
        const updated = await updateAdminSubject(modalMode, payload);
        setSubjects((prev) =>
          prev
            .map((s) => (s.subjectId === modalMode ? updated : s))
            .sort((a, b) => a.name.localeCompare(b.name))
        );
        setActionSuccess(`Subject "${updated.name}" updated successfully.`);
      }
      closeModal();
      setTimeout(() => setActionSuccess(null), 5000);
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr?.status === 409) {
        setError('name', { message: apiErr.message || 'A subject with that name already exists' });
        setFormError(apiErr.message || 'A subject with that name already exists.');
      } else {
        setFormError(apiErr?.message || 'Failed to save subject. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(subject: SubjectResponse) {
    const nextStatus = subject.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setStatusTogglingId(subject.subjectId);
    setActionSuccess(null);
    try {
      const updated = await setAdminSubjectStatus(subject.subjectId, nextStatus);
      setSubjects((prev) =>
        prev.map((s) => (s.subjectId === subject.subjectId ? updated : s))
      );
      setActionSuccess(
        `Subject "${subject.name}" is now ${nextStatus === 'ACTIVE' ? 'Active' : 'Inactive'}.`
      );
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      const apiErr = err as ApiError;
      setPageError(apiErr?.message || 'Failed to update subject status.');
    } finally {
      setStatusTogglingId(null);
    }
  }

  const activeCount = subjects.filter((s) => s.status === 'ACTIVE').length;
  const inactiveCount = subjects.filter((s) => s.status === 'INACTIVE').length;

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Subjects</h1>
            <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
              {subjects.length} Total
            </span>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Manage academic subjects, streams, and curriculum categories.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchSubjects}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-xl border border-border px-3 py-2 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="Refresh subjects"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            id="admin-create-subject-btn"
            type="button"
            onClick={openCreateModal}
            className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition"
          >
            <Plus size={15} />
            <span>Create Subject</span>
          </button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="rounded-2xl border border-border bg-card p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-muted-foreground">Total Subjects</p>
            <p className="text-2xl font-bold text-foreground mt-1">{subjects.length}</p>
          </div>
          <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
            <BookOpen size={20} />
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

      {/* Global Success / Error Banners */}
      {actionSuccess && (
        <div
          id="admin-subject-success-banner"
          className="rounded-xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-xs font-medium text-green-700 dark:text-green-300 flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} className="shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionSuccess(null)}
            className="text-muted-foreground hover:text-foreground"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {pageError && (
        <div
          id="admin-subject-error-banner"
          className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-xs font-medium text-destructive flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <AlertCircle size={16} className="shrink-0" />
            <span>{pageError}</span>
          </div>
          <button
            type="button"
            onClick={() => setPageError(null)}
            className="text-muted-foreground hover:text-foreground"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {/* Table Container */}
      <div className="rounded-2xl border border-border bg-card overflow-hidden shadow-sm">
        {loading && subjects.length === 0 ? (
          <div className="p-12 flex flex-col items-center justify-center gap-3">
            <Loader2 size={28} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading subjects…</p>
          </div>
        ) : subjects.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="mx-auto h-12 w-12 rounded-full bg-muted flex items-center justify-center text-muted-foreground">
              <BookOpen size={24} />
            </div>
            <h3 className="text-sm font-semibold text-foreground">No subjects found</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              Get started by creating your first academic subject for students.
            </p>
            <button
              type="button"
              onClick={openCreateModal}
              className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-3.5 py-1.5 text-xs font-semibold hover:opacity-90 transition"
            >
              <Plus size={14} />
              Create Subject
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/40 text-muted-foreground font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-4 w-16">ID</th>
                  <th className="py-3 px-4">Subject Name</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 w-28">Status</th>
                  <th className="py-3 px-4 w-36">Created</th>
                  <th className="py-3 px-4 w-52 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {subjects.map((sub) => {
                  const isActive = sub.status === 'ACTIVE';
                  const isToggling = statusTogglingId === sub.subjectId;

                  return (
                    <tr
                      key={sub.subjectId}
                      className="hover:bg-muted/20 transition-colors"
                      id={`subject-row-${sub.subjectId}`}
                    >
                      <td className="py-3.5 px-4 font-mono text-muted-foreground">
                        #{sub.subjectId}
                      </td>

                      <td className="py-3.5 px-4 font-medium text-foreground">
                        <span className="font-semibold">{sub.name}</span>
                      </td>

                      <td className="py-3.5 px-4 text-muted-foreground max-w-xs truncate">
                        {sub.description || <span className="italic opacity-50">No description</span>}
                      </td>

                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                            isActive
                              ? 'bg-green-500/10 text-green-700 dark:text-green-300 border border-green-500/20'
                              : 'bg-muted text-muted-foreground border border-border'
                          }`}
                        >
                          <span
                            className={`h-1.5 w-1.5 rounded-full ${
                              isActive ? 'bg-green-600' : 'bg-muted-foreground'
                            }`}
                          />
                          {sub.status}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-muted-foreground text-[11px]">
                        <span className="inline-flex items-center gap-1">
                          <Calendar size={12} className="opacity-70" />
                          {new Date(sub.createdAt).toLocaleDateString()}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div className="inline-flex items-center justify-end gap-1.5">
                          <Link
                            to={`/admin/subjects/${sub.subjectId}/chapters`}
                            id={`manage-chapters-btn-${sub.subjectId}`}
                            className="inline-flex items-center gap-1 rounded-lg border border-primary/30 bg-primary/5 px-2.5 py-1 text-[11px] font-medium text-primary hover:bg-primary/10 transition"
                            title="Manage Chapters"
                          >
                            <Layers size={12} />
                            <span>Chapters</span>
                          </Link>

                          <button
                            type="button"
                            onClick={() => openEditModal(sub)}
                            id={`edit-subject-btn-${sub.subjectId}`}
                            className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-[11px] font-medium text-foreground hover:bg-muted transition"
                            title="Edit Subject"
                          >
                            <Edit2 size={12} />
                            <span>Edit</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => handleToggleStatus(sub)}
                            disabled={isToggling}
                            id={`toggle-status-btn-${sub.subjectId}`}
                            className={`inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-[11px] font-medium transition disabled:opacity-50 ${
                              isActive
                                ? 'border-amber-500/30 text-amber-700 dark:text-amber-300 hover:bg-amber-500/10'
                                : 'border-green-500/30 text-green-700 dark:text-green-300 hover:bg-green-500/10'
                            }`}
                            title={isActive ? 'Deactivate subject' : 'Reactivate subject'}
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

      {/* Create / Edit Modal Dialog */}
      {modalMode !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl relative space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2">
                <div className="h-8 w-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold text-sm">
                  <BookOpen size={16} />
                </div>
                <h3 className="font-semibold text-base text-foreground">
                  {modalMode === 'create' ? 'Create New Subject' : 'Edit Subject'}
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

            {/* Error in modal */}
            {formError && (
              <div
                id="subject-form-error"
                className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive flex items-start gap-2"
              >
                <AlertCircle size={15} className="shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="space-y-1.5">
                <label
                  htmlFor="subject-name"
                  className="block text-xs font-semibold text-foreground"
                >
                  Subject Name <span className="text-destructive">*</span>
                </label>
                <input
                  id="subject-name"
                  type="text"
                  placeholder="e.g. Combined Mathematics, Physics, Accounting"
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

              <div className="space-y-1.5">
                <label
                  htmlFor="subject-description"
                  className="block text-xs font-semibold text-foreground"
                >
                  Description <span className="text-muted-foreground font-normal">(Optional)</span>
                </label>
                <textarea
                  id="subject-description"
                  rows={4}
                  placeholder="Provide an overview of this subject, topics covered, or stream details..."
                  {...register('description')}
                  disabled={submitting}
                  className={`w-full rounded-xl border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition resize-y ${
                    errors.description ? 'border-destructive focus:ring-destructive/50' : 'border-border'
                  }`}
                />
                {errors.description && (
                  <p className="text-[11px] text-destructive font-medium">
                    {errors.description.message}
                  </p>
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
                  id="subject-form-submit-btn"
                  type="submit"
                  disabled={submitting}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 shadow-sm transition disabled:opacity-50"
                >
                  {submitting && <Loader2 size={13} className="animate-spin" />}
                  <span>{modalMode === 'create' ? 'Create Subject' : 'Save Changes'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
