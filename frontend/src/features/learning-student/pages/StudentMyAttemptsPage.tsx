import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getMyQuizAttempts } from '../api';
import type { MyAttemptsSummaryResponse } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { EmptyState } from '../components/EmptyState';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import {
  History,
  Trophy,
  ArrowRight,
  Loader2,
  Calendar,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
} from 'lucide-react';

export default function StudentMyAttemptsPage() {
  const { quizId } = useParams<{ quizId: string }>();
  const qId = Number(quizId);

  const [data, setData] = useState<MyAttemptsSummaryResponse | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!qId) return;
    let cancelled = false;

    async function load() {
      try {
        const res = await getMyQuizAttempts(qId, page, 10);
        if (!cancelled) setData(res);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [qId, page]);

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading your attempts history…</span>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center text-xs text-destructive">
        {error || 'Failed to load attempt history.'}
      </div>
    );
  }

  const attemptsList = data.attempts?.content || [];
  const totalPages = data.attempts?.totalPages || 0;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <History className="text-primary" size={24} />
            <span>My Quiz Attempts</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Track your performance history and score improvements over time.
          </p>
        </div>

        <Link
          to={LEARNING_ROUTES.QUIZ_START(qId)}
          className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 self-start sm:self-auto"
        >
          <RotateCcw size={13} />
          <span>Take Quiz Again</span>
        </Link>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-4 rounded-3xl border border-border bg-card shadow-2xs">
          <span className="text-[11px] font-semibold text-muted-foreground">Total Attempts</span>
          <p className="text-2xl font-black text-foreground mt-1">{data.attemptCount}</p>
        </div>

        <div className="p-4 rounded-3xl border border-border bg-card shadow-2xs">
          <span className="text-[11px] font-semibold text-muted-foreground">Best Score</span>
          <p className="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-1">
            {data.bestPercentage != null ? `${Math.round(data.bestPercentage)}%` : '—'}
          </p>
        </div>

        <div className="p-4 rounded-3xl border border-border bg-card shadow-2xs">
          <span className="text-[11px] font-semibold text-muted-foreground">Latest Score</span>
          <p className="text-2xl font-black text-primary mt-1">
            {data.latestPercentage != null ? `${Math.round(data.latestPercentage)}%` : '—'}
          </p>
        </div>

        <div className="p-4 rounded-3xl border border-border bg-card shadow-2xs">
          <span className="text-[11px] font-semibold text-muted-foreground">Average Score</span>
          <p className="text-2xl font-black text-foreground mt-1">
            {data.averagePercentage != null ? `${Math.round(data.averagePercentage)}%` : '—'}
          </p>
        </div>
      </div>

      {/* History List */}
      {attemptsList.length === 0 ? (
        <EmptyState
          icon={Trophy}
          title="No attempts recorded yet"
          description="Take this chapter quiz to test your understanding and unlock your performance metrics."
          actionText="Start Quiz"
          actionHref={LEARNING_ROUTES.QUIZ_START(qId)}
        />
      ) : (
        <div className="rounded-3xl border border-border bg-card overflow-hidden shadow-xs divide-y divide-border">
          {attemptsList.map(a => {
            const isCompleted = a.status === 'COMPLETED';
            const pct = Math.round(a.percentage ?? 0);

            return (
              <div
                key={a.attemptId}
                className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-muted/30 transition"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-foreground">
                      Attempt #{a.attemptId}
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
                    <span>{a.questions?.length || 10} questions</span>
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
                    to={LEARNING_ROUTES.QUIZ_ATTEMPT(a.attemptId)}
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
