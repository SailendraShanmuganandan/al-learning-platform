import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  getAdminQuizzes,
  updateAdminQuiz,
  updateAdminQuizStatus,
} from '../api';
import type { QuizSummaryResponse, QuizStatus } from '../types';
import { MIN_ELIGIBLE_QUESTIONS } from '../constants';
import { getErrorMessage } from '../errorHelper';
import { StatusBadge } from '../components/StatusBadge';
import { ChapterPicker } from '../components/ChapterPicker';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FeedbackAlert } from '../components/FeedbackAlert';
import {
  HelpCircle,
  Edit2,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  Layers,
  FileQuestion,
  Info,
  X,
} from 'lucide-react';

export default function AdminQuizzesPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const chapterIdParam = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : null;
  const subjectIdParam = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;
  const statusParam = (searchParams.get('status') as QuizStatus) || '';

  const [quizzes, setQuizzes] = useState<QuizSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Edit title/desc modal
  const [editingQuiz, setEditingQuiz] = useState<QuizSummaryResponse | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Status transition confirm dialog
  const [statusConfirm, setStatusConfirm] = useState<{
    quiz: QuizSummaryResponse;
    targetStatus: QuizStatus;
    title: string;
    description: string;
    warning?: string;
  } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const res = await getAdminQuizzes({
          chapterId: chapterIdParam,
          subjectId: subjectIdParam,
          status: statusParam || undefined,
          page,
          size: 20,
        });
        if (!cancelled) {
          setQuizzes(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
        }
      } catch (err) {
        if (!cancelled) setPageError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [chapterIdParam, subjectIdParam, statusParam, page, refreshKey]);

  const refreshQuizzes = () => {
    setLoading(true);
    setRefreshKey(k => k + 1);
  };

  function handleFilterStatus(st: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (st) next.set('status', st);
      else next.delete('status');
      return next;
    });
    setPage(0);
  }

  // Edit dialog
  function openEditModal(quiz: QuizSummaryResponse) {
    setEditingQuiz(quiz);
    setEditTitle(quiz.title);
    setEditDesc(quiz.description || '');
    setEditError(null);
  }

  async function handleSaveEdit(e: React.FormEvent) {
    e.preventDefault();
    if (!editingQuiz) return;
    if (!editTitle.trim()) {
      setEditError('Quiz title cannot be empty.');
      return;
    }
    setSavingEdit(true);
    setEditError(null);
    try {
      await updateAdminQuiz(editingQuiz.quizId, {
        title: editTitle.trim(),
        description: editDesc.trim() || null,
      });
      setActionSuccess('Quiz details updated successfully.');
      setEditingQuiz(null);
      refreshQuizzes();
    } catch (err) {
      setEditError(getErrorMessage(err));
    } finally {
      setSavingEdit(false);
    }
  }

  // Status transition
  function promptStatusChange(quiz: QuizSummaryResponse, target: QuizStatus) {
    let warning: string | undefined;
    let title = '';
    let description = '';

    if (target === 'PUBLISHED') {
      title = `Publish Quiz "${quiz.title}"?`;
      description = 'Students will be able to attempt this quiz once published, provided it meets the minimum question threshold.';
      if (quiz.eligibleCount < MIN_ELIGIBLE_QUESTIONS) {
        warning = `Students will see this quiz as not available until at least ${MIN_ELIGIBLE_QUESTIONS} approved questions exist in one language (currently has ${quiz.eligibleCount} eligible).`;
      }
    } else if (target === 'ARCHIVED') {
      title = `Archive Quiz "${quiz.title}"?`;
      description = 'Archiving this quiz hides it from students and prevents new quiz attempts.';
    } else if (target === 'DRAFT') {
      title = `Revert Quiz "${quiz.title}" to Draft?`;
      description = 'Moving this quiz back to Draft hides it from student assessments until republished.';
    }

    setStatusConfirm({
      quiz,
      targetStatus: target,
      title,
      description,
      warning,
    });
  }

  async function handleConfirmStatus() {
    if (!statusConfirm) return;
    setTransitioning(true);
    try {
      await updateAdminQuizStatus(statusConfirm.quiz.quizId, {
        status: statusConfirm.targetStatus,
      });
      setActionSuccess(`Quiz status changed to ${statusConfirm.targetStatus}.`);
      setStatusConfirm(null);
      refreshQuizzes();
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setTransitioning(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <HelpCircle className="text-primary" size={24} />
            <span>Chapter Quizzes</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Manage chapter quiz configurations, publish statuses, and eligibility thresholds.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={refreshQuizzes}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition disabled:opacity-50"
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {actionSuccess && (
        <FeedbackAlert
          type="success"
          message={actionSuccess}
          onClose={() => setActionSuccess(null)}
        />
      )}

      {pageError && (
        <FeedbackAlert
          type="error"
          message={pageError}
          onClose={() => setPageError(null)}
        />
      )}

      {/* Auto-create chapter quiz hint banner */}
      <div className="rounded-2xl border border-primary/20 bg-primary/5 p-4 flex items-start gap-3 text-xs text-muted-foreground leading-relaxed">
        <Info size={18} className="text-primary shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-foreground">How chapter quizzes work: </span>
          A chapter quiz is created automatically when its first question is added in the Question Bank. To configure a new quiz for an empty chapter,{' '}
          <Link to="/admin/question-bank" className="text-primary font-semibold hover:underline inline-flex items-center gap-1">
            go to Question Bank <ExternalLink size={11} />
          </Link>.
        </div>
      </div>

      {/* Filter Bar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <ChapterPicker />

        <div className="flex items-center gap-2">
          <label htmlFor="filter-quiz-status" className="text-xs font-semibold text-muted-foreground whitespace-nowrap">
            Status:
          </label>
          <select
            id="filter-quiz-status"
            value={statusParam}
            onChange={e => handleFilterStatus(e.target.value)}
            className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground shadow-xs focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
      </div>

      {/* Quizzes Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Subject</th>
                <th className="py-3 px-4">Chapter</th>
                <th className="py-3 px-4">Quiz Title</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-center">Questions</th>
                <th className="py-3 px-4 text-center">Approved</th>
                <th className="py-3 px-4 text-center">Pending</th>
                <th className="py-3 px-4 text-center">Eligible (Min 5)</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-muted-foreground">
                    <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
                    <span>Loading quizzes…</span>
                  </td>
                </tr>
              ) : quizzes.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-muted-foreground">
                    <Layers size={28} className="mx-auto mb-2 opacity-40" />
                    <p className="font-semibold text-foreground">No quizzes found</p>
                    <p className="text-xs mt-1">Try adjusting the subject/chapter filter or add questions to create one.</p>
                  </td>
                </tr>
              ) : (
                quizzes.map(quiz => {
                  const meetsThreshold = quiz.eligibleCount >= MIN_ELIGIBLE_QUESTIONS;
                  const langCounts = quiz.eligibleByLanguage || {};

                  return (
                    <tr key={quiz.quizId} className="hover:bg-muted/30 transition">
                      <td className="py-3.5 px-4 font-medium text-foreground">
                        {quiz.subjectName || '—'}
                      </td>
                      <td className="py-3.5 px-4 text-muted-foreground font-medium">
                        {quiz.chapterName}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-foreground">{quiz.title}</div>
                        {quiz.description && (
                          <div className="text-[11px] text-muted-foreground truncate max-w-xs mt-0.5">
                            {quiz.description}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={quiz.status} />
                      </td>
                      <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                        {quiz.questionCount}
                      </td>
                      <td className="py-3.5 px-4 text-center font-semibold text-emerald-600 dark:text-emerald-400">
                        {quiz.approvedCount}
                      </td>
                      <td className="py-3.5 px-4 text-center font-semibold text-amber-600 dark:text-amber-400">
                        {quiz.pendingCount}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <div
                          className={`inline-flex items-center gap-1 font-bold px-2 py-0.5 rounded-full text-[11px] ${
                            meetsThreshold
                              ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                              : 'bg-amber-500/10 text-amber-700 dark:text-amber-300'
                          }`}
                          title={`Eligible per language: ${Object.entries(langCounts).map(([lang, count]) => `${lang}: ${count}`).join(', ') || 'none'}`}
                        >
                          <span>{quiz.eligibleCount}</span>
                          {!meetsThreshold && <span className="text-[10px] opacity-75">(&lt;5)</span>}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEditModal(quiz)}
                            className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
                            title="Edit Title & Description"
                          >
                            <Edit2 size={13} />
                          </button>

                          <Link
                            to={`/admin/question-bank?chapterId=${quiz.chapterId}`}
                            className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-primary hover:bg-primary/5 transition flex items-center gap-1"
                            title="Open Question Bank"
                          >
                            <FileQuestion size={13} />
                          </Link>

                          {quiz.status !== 'PUBLISHED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusChange(quiz, 'PUBLISHED')}
                              className="px-2 py-1 rounded-lg bg-emerald-600 text-white text-[11px] font-semibold hover:bg-emerald-700 transition"
                            >
                              Publish
                            </button>
                          )}

                          {quiz.status === 'PUBLISHED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusChange(quiz, 'DRAFT')}
                              className="px-2 py-1 rounded-lg border border-border text-muted-foreground text-[11px] font-medium hover:bg-muted transition"
                            >
                              Revert to Draft
                            </button>
                          )}

                          {quiz.status !== 'ARCHIVED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusChange(quiz, 'ARCHIVED')}
                              className="px-2 py-1 rounded-lg border border-destructive/20 text-destructive text-[11px] font-medium hover:bg-destructive/10 transition"
                            >
                              Archive
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="border-t border-border px-4 py-3 flex items-center justify-between">
            <span className="text-xs text-muted-foreground">
              Showing page <strong className="text-foreground">{page + 1}</strong> of <strong className="text-foreground">{totalPages}</strong> ({totalElements} quizzes)
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
                className="p-1.5 rounded-lg border border-border text-muted-foreground hover:bg-muted transition disabled:opacity-40"
              >
                <ChevronLeft size={14} />
              </button>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                className="p-1.5 rounded-lg border border-border text-muted-foreground hover:bg-muted transition disabled:opacity-40"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Edit Title/Description Modal */}
      {editingQuiz && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <form
            onSubmit={handleSaveEdit}
            className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-150"
          >
            <div className="flex items-center justify-between pb-3 border-b border-border mb-4">
              <h3 className="text-sm font-bold text-foreground">Edit Quiz Details</h3>
              <button
                type="button"
                onClick={() => setEditingQuiz(null)}
                className="p-1 text-muted-foreground hover:text-foreground"
              >
                <X size={16} />
              </button>
            </div>

            {editError && (
              <FeedbackAlert type="error" message={editError} className="mb-4" />
            )}

            <div className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-foreground mb-1 block">
                  Title <span className="text-destructive">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={editTitle}
                  onChange={e => setEditTitle(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-foreground mb-1 block">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={editDesc}
                  onChange={e => setEditDesc(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                />
              </div>
            </div>

            <div className="mt-6 flex flex-col gap-2">
              <button
                type="submit"
                disabled={savingEdit}
                className="w-full py-2.5 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-1.5 disabled:opacity-50"
              >
                {savingEdit ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    <span>Saving…</span>
                  </>
                ) : (
                  <span>Save Changes</span>
                )}
              </button>
              <button
                type="button"
                onClick={() => setEditingQuiz(null)}
                disabled={savingEdit}
                className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Status Transition Confirm Dialog */}
      {statusConfirm && (
        <ConfirmDialog
          isOpen={true}
          title={statusConfirm.title}
          description={statusConfirm.description}
          warning={statusConfirm.warning}
          confirmLabel={`Yes, change to ${statusConfirm.targetStatus}`}
          confirmVariant={
            statusConfirm.targetStatus === 'PUBLISHED'
              ? 'emerald'
              : statusConfirm.targetStatus === 'ARCHIVED'
              ? 'destructive'
              : 'primary'
          }
          isLoading={transitioning}
          onConfirm={handleConfirmStatus}
          onCancel={() => setStatusConfirm(null)}
        />
      )}
    </div>
  );
}
