import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  getAdminQuestionReports,
  updateAdminQuestionReportStatus,
} from '../api';
import type {
  QuestionReportAdminResponse,
  QuestionReportStatus,
} from '../types';
import { getErrorMessage } from '../errorHelper';
import { StatusBadge } from '../components/StatusBadge';
import { ChapterPicker } from '../components/ChapterPicker';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FeedbackAlert } from '../components/FeedbackAlert';
import {
  Flag,
  AlertTriangle,
  RefreshCw,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Layers,
  Info,
} from 'lucide-react';

export default function AdminQuestionReportsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const statusParam = (searchParams.get('status') as QuestionReportStatus) || 'OPEN';
  const chapterIdParam = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : null;
  const questionIdParam = searchParams.get('questionId') ? Number(searchParams.get('questionId')) : null;

  const [reports, setReports] = useState<QuestionReportAdminResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Confirm dialog for status resolution
  const [confirmAction, setConfirmAction] = useState<{
    report: QuestionReportAdminResponse;
    targetStatus: QuestionReportStatus;
    title: string;
    description: string;
  } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const res = await getAdminQuestionReports({
          status: statusParam || undefined,
          chapterId: chapterIdParam || undefined,
          questionId: questionIdParam || undefined,
          page,
          size: 20,
        });
        if (!cancelled) {
          setReports(res.content);
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
  }, [statusParam, chapterIdParam, questionIdParam, page, refreshKey]);

  const refreshReports = () => {
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

  function handleClearQuestionFilter() {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.delete('questionId');
      return next;
    });
    setPage(0);
  }

  function promptResolve(report: QuestionReportAdminResponse, target: QuestionReportStatus) {
    const isResolve = target === 'RESOLVED';
    setConfirmAction({
      report,
      targetStatus: target,
      title: isResolve ? `Resolve Report #${report.reportId}?` : `Dismiss Report #${report.reportId}?`,
      description: isResolve
        ? 'Marking as resolved indicates the issue in the question has been addressed or corrected.'
        : 'Dismissing the report indicates the question was verified to be correct as is.',
    });
  }

  async function handleConfirmReportStatus() {
    if (!confirmAction) return;
    setTransitioning(true);
    try {
      await updateAdminQuestionReportStatus(confirmAction.report.reportId, {
        status: confirmAction.targetStatus,
      });
      setActionSuccess(`Report #${confirmAction.report.reportId} marked as ${confirmAction.targetStatus}.`);
      setConfirmAction(null);
      refreshReports();
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setTransitioning(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <Flag className="text-primary" size={24} />
            <span>Question Reports</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Review and resolve student-submitted problem reports regarding question accuracy or clarity.
          </p>
        </div>

        <button
          type="button"
          onClick={refreshReports}
          disabled={loading}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition disabled:opacity-50 self-start sm:self-auto"
        >
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </button>
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

      {/* Info Banner on Threshold Rule */}
      <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4 flex items-start gap-3 text-xs text-muted-foreground leading-relaxed">
        <Info size={18} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-foreground">Automatic Review Threshold: </span>
          3 open reports from distinct students automatically return an approved question back to <strong className="text-foreground">Pending</strong> for administrative review. Re-approving a question automatically marks all its open reports as <strong className="text-foreground">Resolved</strong>.
        </div>
      </div>

      {/* Filter Bar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          <ChapterPicker />

          <div className="flex items-center gap-2">
            <label htmlFor="filter-report-status" className="text-xs font-semibold text-muted-foreground whitespace-nowrap">
              Status:
            </label>
            <select
              id="filter-report-status"
              value={statusParam}
              onChange={e => handleFilterStatus(e.target.value)}
              className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground shadow-xs focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open (Default)</option>
              <option value="RESOLVED">Resolved</option>
              <option value="DISMISSED">Dismissed</option>
            </select>
          </div>
        </div>

        {/* Specific Question Filter Indicator */}
        {questionIdParam && (
          <div className="flex items-center gap-2 bg-primary/10 border border-primary/20 text-primary px-3 py-1 rounded-full text-xs font-semibold">
            <span>Filtered for Question #{questionIdParam}</span>
            <button
              type="button"
              onClick={handleClearQuestionFilter}
              className="text-primary hover:text-foreground font-black"
              title="Clear question filter"
            >
              &times;
            </button>
          </div>
        )}
      </div>

      {/* Reports Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Question</th>
                <th className="py-3 px-4">Question Status</th>
                <th className="py-3 px-4">Chapter</th>
                <th className="py-3 px-4">Reason</th>
                <th className="py-3 px-4">Comment</th>
                <th className="py-3 px-4">Reporter</th>
                <th className="py-3 px-4">Date</th>
                <th className="py-3 px-4">Report Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-muted-foreground">
                    <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
                    <span>Loading reports…</span>
                  </td>
                </tr>
              ) : reports.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-muted-foreground">
                    <Layers size={28} className="mx-auto mb-2 opacity-40" />
                    <p className="font-semibold text-foreground">No reports found</p>
                    <p className="text-xs mt-1">There are no reports matching the selected filters.</p>
                  </td>
                </tr>
              ) : (
                reports.map(rep => {
                  const isAutoReturned = rep.questionStatus === 'PENDING';

                  return (
                    <tr key={rep.reportId} className="hover:bg-muted/30 transition">
                      {/* Question Text & Link */}
                      <td className="py-3.5 px-4 max-w-xs">
                        <Link
                          to={`/admin/question-bank/${rep.questionId}`}
                          className="font-medium text-foreground hover:text-primary transition line-clamp-2 leading-relaxed"
                          title={rep.questionText}
                        >
                          {rep.questionText}
                        </Link>
                        <span className="text-[10px] text-muted-foreground font-mono">
                          Q#{rep.questionId}
                        </span>
                      </td>

                      {/* Question Status (Pending with reports = auto returned) */}
                      <td className="py-3.5 px-4">
                        <div className="space-y-1">
                          <StatusBadge status={rep.questionStatus} />
                          {isAutoReturned && (
                            <div className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold flex items-center gap-1">
                              <AlertTriangle size={10} />
                              <span>Needs Review</span>
                            </div>
                          )}
                        </div>
                      </td>

                      {/* Chapter */}
                      <td className="py-3.5 px-4 text-muted-foreground font-medium truncate max-w-[140px]">
                        {rep.chapterName}
                      </td>

                      {/* Reason */}
                      <td className="py-3.5 px-4 font-semibold text-foreground whitespace-nowrap">
                        <span className="px-2 py-0.5 rounded-full bg-muted border border-border text-[11px]">
                          {rep.reason}
                        </span>
                      </td>

                      {/* Student Comment */}
                      <td className="py-3.5 px-4 max-w-xs text-muted-foreground leading-relaxed">
                        {rep.comment ? (
                          <span title={rep.comment} className="line-clamp-2 italic">
                            "{rep.comment}"
                          </span>
                        ) : (
                          <span className="opacity-40">No comment</span>
                        )}
                      </td>

                      {/* Reporter */}
                      <td className="py-3.5 px-4 text-foreground font-medium whitespace-nowrap">
                        {rep.reporterName}
                      </td>

                      {/* Date */}
                      <td className="py-3.5 px-4 text-muted-foreground whitespace-nowrap text-[11px]">
                        {new Date(rep.createdAt).toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                        })}
                      </td>

                      {/* Report Status */}
                      <td className="py-3.5 px-4">
                        <StatusBadge status={rep.status} />
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Link
                            to={`/admin/question-bank/${rep.questionId}`}
                            className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-primary hover:bg-primary/5 transition"
                            title="Open Question"
                          >
                            <ExternalLink size={13} />
                          </Link>

                          {rep.status === 'OPEN' && (
                            <>
                              <button
                                type="button"
                                onClick={() => promptResolve(rep, 'RESOLVED')}
                                className="px-2 py-1 rounded-lg bg-emerald-600 text-white text-[11px] font-semibold hover:bg-emerald-700 transition"
                                title="Resolve Report"
                              >
                                Resolve
                              </button>
                              <button
                                type="button"
                                onClick={() => promptResolve(rep, 'DISMISSED')}
                                className="px-2 py-1 rounded-lg border border-border text-muted-foreground text-[11px] font-medium hover:bg-muted transition"
                                title="Dismiss Report"
                              >
                                Dismiss
                              </button>
                            </>
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
              Showing page <strong className="text-foreground">{page + 1}</strong> of <strong className="text-foreground">{totalPages}</strong> ({totalElements} reports)
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

      {/* Confirmation Dialog */}
      {confirmAction && (
        <ConfirmDialog
          isOpen={true}
          title={confirmAction.title}
          description={confirmAction.description}
          confirmLabel={`Yes, mark as ${confirmAction.targetStatus}`}
          confirmVariant={confirmAction.targetStatus === 'RESOLVED' ? 'emerald' : 'primary'}
          isLoading={transitioning}
          onConfirm={handleConfirmReportStatus}
          onCancel={() => setConfirmAction(null)}
        />
      )}
    </div>
  );
}
