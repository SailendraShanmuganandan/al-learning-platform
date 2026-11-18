import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getMyAssessments } from '../api';
import type { DiagnosticAssessmentResponse } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { EmptyState } from '../components/EmptyState';
import { UnverifiedEmailPrompt } from '../components/UnverifiedEmailPrompt';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import {
  Compass,
  History,
  Calendar,
  ChevronLeft,
  ChevronRight,
  ArrowRight,
  Loader2,
  Sparkles,
} from 'lucide-react';

export default function StudentAssessmentsHistoryPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const typeParam = searchParams.get('type') || '';
  const statusParam = searchParams.get('status') || '';

  const [assessments, setAssessments] = useState<DiagnosticAssessmentResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const res = await getMyAssessments({
          type: typeParam || undefined,
          status: statusParam || undefined,
          page,
          size: 10,
        });

        if (!cancelled) {
          setAssessments(res.content);
          setTotalPages(res.totalPages);
        }
      } catch (err: unknown) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [typeParam, statusParam, page]);

  function handleFilterType(t: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (t) next.set('type', t);
      else next.delete('type');
      return next;
    });
    setPage(0);
  }

  function handleFilterStatus(s: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (s) next.set('status', s);
      else next.delete('status');
      return next;
    });
    setPage(0);
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading assessments history…</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <History className="text-primary" size={24} />
            <span>Assessment History</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Past diagnostic assessments and overall syllabus quiz attempts.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to={LEARNING_ROUTES.OVERALL_QUIZ_START}
            className="px-3.5 py-1.5 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5"
          >
            <Sparkles size={13} />
            <span>Take Overall Quiz</span>
          </Link>
        </div>
      </div>

      {!isVerified && (
        <UnverifiedEmailPrompt message="Please verify your email address to unlock official assessments." />
      )}

      {error && (
        <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
          {error}
        </div>
      )}

      {/* Filter Bar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-2xs flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <label htmlFor="history-type-filter" className="text-xs font-semibold text-muted-foreground">
            Type:
          </label>
          <select
            id="history-type-filter"
            value={typeParam}
            onChange={e => handleFilterType(e.target.value)}
            className="rounded-xl border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="">All Types</option>
            <option value="INITIAL_DIAGNOSTIC">Diagnostic Assessment</option>
            <option value="OVERALL_QUIZ">Overall Syllabus Quiz</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="history-status-filter" className="text-xs font-semibold text-muted-foreground">
            Status:
          </label>
          <select
            id="history-status-filter"
            value={statusParam}
            onChange={e => handleFilterStatus(e.target.value)}
            className="rounded-xl border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="">All Statuses</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
      </div>

      {/* List */}
      {assessments.length === 0 ? (
        <EmptyState
          icon={Compass}
          title="No assessments recorded yet"
          description="Take an initial diagnostic or an overall syllabus quiz to map your mastery."
          actionText="Take Diagnostic"
          actionHref={LEARNING_ROUTES.DIAGNOSTIC_START}
        />
      ) : (
        <div className="rounded-3xl border border-border bg-card divide-y divide-border overflow-hidden shadow-xs">
          {assessments.map(a => {
            const isCompleted = a.status === 'COMPLETED';
            const pct = Math.round(a.percentage ?? 0);
            const isDiagnostic = a.assessmentType === 'INITIAL_DIAGNOSTIC';

            return (
              <div
                key={a.assessmentId}
                className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-muted/30 transition"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-foreground">
                      {isDiagnostic ? 'Diagnostic Assessment' : 'Overall Syllabus Quiz'} #{a.assessmentId}
                    </span>
                    <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                      · {a.language}
                    </span>
                    {!isCompleted && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20">
                        In Progress
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Calendar size={12} />
                    <span>{new Date(a.startedAt).toLocaleDateString()}</span>
                    <span>·</span>
                    <span>Scope: {a.scopeType.replace('_', ' ')}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-4">
                  {isCompleted ? (
                    <div className="text-right">
                      <span
                        className={`text-lg font-black ${
                          pct >= 75
                            ? 'text-emerald-600 dark:text-emerald-400'
                            : pct >= 50
                            ? 'text-sky-600 dark:text-sky-400'
                            : 'text-amber-600 dark:text-amber-400'
                        }`}
                      >
                        {pct}%
                      </span>
                      <p className="text-[11px] text-muted-foreground">
                        {a.score} / {a.totalMarks} marks
                      </p>
                    </div>
                  ) : (
                    <span className="text-xs text-amber-600 font-semibold">
                      Unfinished
                    </span>
                  )}

                  <Link
                    to={LEARNING_ROUTES.ASSESSMENT_RUNNER(a.assessmentId)}
                    className="px-3.5 py-1.5 rounded-xl border border-border text-xs font-semibold text-foreground hover:bg-muted transition inline-flex items-center gap-1.5"
                  >
                    <span>{isCompleted ? 'View Results' : 'Resume'}</span>
                    <ArrowRight size={13} />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted-foreground pt-2">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage(p => Math.max(0, p - 1))}
              className="p-1.5 rounded-xl border border-border disabled:opacity-40"
            >
              <ChevronLeft size={14} />
            </button>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}
              className="p-1.5 rounded-xl border border-border disabled:opacity-40"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
