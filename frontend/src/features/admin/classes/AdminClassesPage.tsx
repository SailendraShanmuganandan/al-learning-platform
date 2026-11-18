import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod/v4';
import {
  getAdminClasses,
  createAdminClass,
  updateAdminClass,
  setAdminClassStatus,
  type AdminClassResponse,
  type ClassStatus,
} from '../../../api/classApi';
import { getAdminSubjects, type SubjectResponse } from '../../../api/subjectApi';
import type { ApiError } from '../../../api/authApi';
import {
  GraduationCap,
  Plus,
  Edit2,
  Power,
  PowerOff,
  CheckCircle2,
  AlertCircle,
  Loader2,
  X,
  Search,
} from 'lucide-react';

function formatLKR(amount: number): string {
  return `LKR ${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

const classFormSchema = z.object({
  subjectId: z.number().min(1, 'Please select a subject'),
  name: z.string().trim().min(1, 'Class name is required'),
  description: z.string().optional().or(z.literal('')),
  classType: z.enum(['PHYSICAL', 'ONLINE', 'HYBRID']),
  scheduleDescription: z.string().trim().min(1, 'Schedule description is required'),
  fee: z
    .string()
    .trim()
    .min(1, 'Fee is required')
    .refine(
      (v) => {
        const num = parseFloat(v);
        if (isNaN(num) || num < 0) return false;
        const parts = v.split('.');
        if (parts.length > 1 && parts[1].length > 2) return false;
        return true;
      },
      { message: 'Fee must be a non-negative number with at most 2 decimal places' }
    ),
});

type ClassFormData = z.infer<typeof classFormSchema>;

export default function AdminClassesPage() {
  const [classes, setClasses] = useState<AdminClassResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingClass, setEditingClass] = useState<AdminClassResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ClassFormData>({
    resolver: zodResolver(classFormSchema),
    defaultValues: {
      subjectId: 0,
      name: '',
      description: '',
      classType: 'PHYSICAL',
      scheduleDescription: '',
      fee: '0.00',
    },
  });

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [classList, subjectList] = await Promise.all([
        getAdminClasses(),
        getAdminSubjects(),
      ]);
      setClasses(classList);
      setSubjects(subjectList);
    } catch (err: unknown) {
      const e = err as { message?: string };
      setError(e?.message || 'Failed to load classes.');
    } finally {
      setLoading(false);
    }
  }

  function openCreateModal() {
    setEditingClass(null);
    setFormError(null);
    const activeSubjects = subjects.filter((s) => s.status === 'ACTIVE');
    reset({
      subjectId: activeSubjects.length > 0 ? activeSubjects[0].subjectId : 0,
      name: '',
      description: '',
      classType: 'PHYSICAL',
      scheduleDescription: '',
      fee: '0.00',
    });
    setIsModalOpen(true);
  }

  function openEditModal(cls: AdminClassResponse) {
    setEditingClass(cls);
    setFormError(null);
    reset({
      subjectId: cls.subjectId,
      name: cls.name,
      description: cls.description || '',
      classType: cls.classType,
      scheduleDescription: cls.scheduleDescription,
      fee: cls.fee.toString(),
    });
    setIsModalOpen(true);
  }

  async function onSubmit(data: ClassFormData) {
    setSubmitting(true);
    setFormError(null);
    setActionSuccess(null);

    const feeNum = parseFloat(data.fee);

    try {
      if (editingClass) {
        const updated = await updateAdminClass(editingClass.classId, {
          name: data.name,
          description: data.description || null,
          classType: data.classType,
          scheduleDescription: data.scheduleDescription,
          fee: feeNum,
        });
        setClasses((prev) =>
          prev.map((c) => (c.classId === updated.classId ? updated : c))
        );
        setActionSuccess(`Class "${updated.name}" updated successfully.`);
      } else {
        const created = await createAdminClass({
          subjectId: data.subjectId,
          name: data.name,
          description: data.description || null,
          classType: data.classType,
          scheduleDescription: data.scheduleDescription,
          fee: feeNum,
        });
        setClasses((prev) => [created, ...prev]);
        setActionSuccess(`Class "${created.name}" created successfully.`);
      }
      setIsModalOpen(false);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setFormError(apiErr?.message || 'Failed to save class. Please check your inputs.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(cls: AdminClassResponse) {
    const nextStatus: ClassStatus = cls.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      const updated = await setAdminClassStatus(cls.classId, nextStatus);
      setClasses((prev) =>
        prev.map((c) => (c.classId === updated.classId ? updated : c))
      );
      setActionSuccess(
        `Class "${cls.name}" is now ${nextStatus.toLowerCase()}.`
      );
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setError(apiErr?.message || 'Failed to update class status.');
    }
  }

  const filtered = classes.filter((c) => {
    const q = searchQuery.toLowerCase();
    return (
      c.name.toLowerCase().includes(q) ||
      c.subjectName.toLowerCase().includes(q) ||
      c.scheduleDescription.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-foreground flex items-center gap-2.5">
            <GraduationCap className="text-primary" size={26} />
            Classes Management
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Create, update schedules and manage physical, online, and hybrid tuition classes.
          </p>
        </div>

        <button
          type="button"
          id="btn-create-class"
          onClick={openCreateModal}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:opacity-90 transition"
        >
          <Plus size={16} />
          Create New Class
        </button>
      </div>

      {/* Action Banner */}
      {actionSuccess && (
        <div className="flex items-center justify-between p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-300 text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{actionSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionSuccess(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Global Error Banner */}
      {error && (
        <div className="flex items-center justify-between p-3.5 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex items-center justify-between gap-4 bg-card p-3 rounded-2xl border border-border shadow-2xs">
        <div className="relative flex-1 max-w-sm">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            id="admin-class-search"
            type="text"
            placeholder="Search classes by name, subject, schedule..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-border bg-background text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        <div className="text-xs text-muted-foreground pr-2">
          Total Classes: <span className="font-semibold text-foreground">{classes.length}</span>
        </div>
      </div>

      {/* Classes Table */}
      <div className="bg-card rounded-2xl border border-border overflow-hidden shadow-2xs">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <Loader2 size={30} className="animate-spin text-primary mb-2" />
            <p className="text-xs">Loading classes...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16 text-muted-foreground">
            <p className="text-sm font-semibold text-foreground">No classes found</p>
            <p className="text-xs mt-1">
              {searchQuery ? 'No classes matched your search query.' : 'Click "Create New Class" to add one.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/40 text-muted-foreground font-semibold">
                  <th className="py-3 px-4">Class Name</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Fee (LKR)</th>
                  <th className="py-3 px-4">Schedule</th>
                  <th className="py-3 px-4 text-center">Registrations</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {filtered.map((cls) => {
                  const isActive = cls.status === 'ACTIVE';
                  return (
                    <tr
                      key={cls.classId}
                      id={`admin-class-row-${cls.classId}`}
                      className="hover:bg-muted/30 transition-colors"
                    >
                      <td className="py-3.5 px-4 font-semibold text-foreground">
                        <div>{cls.name}</div>
                        {cls.description && (
                          <div className="text-[11px] text-muted-foreground line-clamp-1 font-normal mt-0.5">
                            {cls.description}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 font-medium text-foreground">
                        {cls.subjectName}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold border border-border bg-muted text-foreground">
                          {cls.classType}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-bold text-foreground">
                        {formatLKR(cls.fee)}
                      </td>
                      <td className="py-3.5 px-4 text-muted-foreground max-w-[200px] truncate">
                        {cls.scheduleDescription}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <div className="inline-flex items-center gap-1.5 text-[11px]">
                          <span
                            title="Confirmed registrations"
                            className="px-2 py-0.5 rounded-md bg-emerald-500/10 text-emerald-600 font-bold"
                          >
                            {cls.confirmedRegistrationsCount} Confirmed
                          </span>
                          <span
                            title="Pending registrations"
                            className="px-2 py-0.5 rounded-md bg-amber-500/10 text-amber-600 font-bold"
                          >
                            {cls.pendingRegistrationsCount} Pending
                          </span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                            isActive
                              ? 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20'
                              : 'bg-muted text-muted-foreground border border-border'
                          }`}
                        >
                          <span className="h-1.5 w-1.5 rounded-full bg-current" />
                          {cls.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            id={`btn-edit-class-${cls.classId}`}
                            onClick={() => openEditModal(cls)}
                            title="Edit Class"
                            className="p-1.5 rounded-lg border border-border hover:bg-muted text-muted-foreground hover:text-foreground transition"
                          >
                            <Edit2 size={13} />
                          </button>
                          <button
                            type="button"
                            id={`btn-toggle-status-${cls.classId}`}
                            onClick={() => handleToggleStatus(cls)}
                            title={isActive ? 'Deactivate Class' : 'Activate Class'}
                            className={`p-1.5 rounded-lg border transition ${
                              isActive
                                ? 'border-destructive/30 text-destructive hover:bg-destructive/10'
                                : 'border-emerald-500/30 text-emerald-600 hover:bg-emerald-500/10'
                            }`}
                          >
                            {isActive ? <PowerOff size={13} /> : <Power size={13} />}
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

      {/* Create / Edit Class Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-lg w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-border mb-4">
              <h2 className="text-base font-bold text-foreground">
                {editingClass ? `Edit Class: ${editingClass.name}` : 'Create New Class'}
              </h2>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-muted-foreground hover:text-foreground p-1 rounded-lg"
              >
                <X size={16} />
              </button>
            </div>

            {formError && (
              <div className="flex items-center gap-2 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs mb-4">
                <AlertCircle size={15} />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-xs">
              {/* Subject Selection (Disabled on edit) */}
              <div>
                <label className="font-medium text-foreground block mb-1">
                  Subject *
                </label>
                {editingClass ? (
                  <input
                    type="text"
                    disabled
                    value={editingClass.subjectName}
                    className="w-full px-3 py-2 rounded-xl border border-border bg-muted/60 text-muted-foreground cursor-not-allowed"
                  />
                ) : (
                  <select
                    id="class-form-subject"
                    {...register('subjectId', { valueAsNumber: true })}
                    className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value={0}>Select active subject</option>
                    {subjects
                      .filter((s) => s.status === 'ACTIVE')
                      .map((s) => (
                        <option key={s.subjectId} value={s.subjectId}>
                          {s.name}
                        </option>
                      ))}
                  </select>
                )}
                {errors.subjectId && (
                  <p className="text-destructive text-[11px] mt-1">{errors.subjectId.message}</p>
                )}
              </div>

              {/* Class Name */}
              <div>
                <label className="font-medium text-foreground block mb-1">
                  Class Name *
                </label>
                <input
                  id="class-form-name"
                  type="text"
                  placeholder="e.g. 2026 Combined Maths Theory"
                  {...register('name')}
                  className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                {errors.name && (
                  <p className="text-destructive text-[11px] mt-1">{errors.name.message}</p>
                )}
              </div>

              {/* Description */}
              <div>
                <label className="font-medium text-foreground block mb-1">
                  Description
                </label>
                <textarea
                  id="class-form-description"
                  rows={2}
                  placeholder="Comprehensive theory lessons, revision handouts, and past paper drills..."
                  {...register('description')}
                  className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                {/* Class Type */}
                <div>
                  <label className="font-medium text-foreground block mb-1">
                    Class Type *
                  </label>
                  <select
                    id="class-form-type"
                    {...register('classType')}
                    className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value="PHYSICAL">Physical</option>
                    <option value="ONLINE">Online</option>
                    <option value="HYBRID">Hybrid</option>
                  </select>
                </div>

                {/* Fee */}
                <div>
                  <label className="font-medium text-foreground block mb-1">
                    Fee (LKR) *
                  </label>
                  <input
                    id="class-form-fee"
                    type="text"
                    placeholder="3500.00"
                    {...register('fee')}
                    className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                  {errors.fee && (
                    <p className="text-destructive text-[11px] mt-1">{errors.fee.message}</p>
                  )}
                </div>
              </div>

              {/* Schedule Description */}
              <div>
                <label className="font-medium text-foreground block mb-1">
                  Schedule Description *
                </label>
                <input
                  id="class-form-schedule"
                  type="text"
                  placeholder="e.g. Saturdays 4-6 PM, Negombo"
                  {...register('scheduleDescription')}
                  className="w-full px-3 py-2 rounded-xl border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                {errors.scheduleDescription && (
                  <p className="text-destructive text-[11px] mt-1">
                    {errors.scheduleDescription.message}
                  </p>
                )}
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-border">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-border text-foreground hover:bg-muted font-medium transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="btn-save-class"
                  disabled={submitting}
                  className="px-4 py-2 rounded-xl bg-primary text-primary-foreground font-semibold hover:opacity-90 disabled:opacity-50 transition flex items-center gap-1.5"
                >
                  {submitting && <Loader2 size={13} className="animate-spin" />}
                  {editingClass ? 'Save Changes' : 'Create Class'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
