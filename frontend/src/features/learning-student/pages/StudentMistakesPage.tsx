import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getMistakesSummary, getMistakes, startMistakePractice } from '../api';
import type { MistakeSummaryResponse, MistakeItemResponse } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { EmptyState } from '../components/EmptyState';
import { mapStudentError } from '../errorMapper';
import {
  Award,
  Flame,
  CheckCircle2,
  Play,
  Loader2,
  FolderTree,
  ChevronLeft,
  ChevronRight,
  Info,
} from 'lucide-react';

export default function StudentMistakesPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const statusFilter = searchParams.get('status') || 'ACTIVE';
  const chapterIdFilter = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : undefined;

  const [summary, setSummary] = useState<MistakeSummaryResponse | null>(null);
  const [mistakes, setMistakes] = useState<MistakeItemResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [practicing, setPracticing] = useState(false);
  const [practiceError, setPracticeError] = useState<string | null>(null);

  // Load summary and list
  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      try {
        const [sum, list] = await Promise.all([
          getMistakesSummary(),
          getMistakes({
            status: statusFilter === 'ALL' ? undefined : statusFilter,
            chapterId: chapterIdFilter,
            page,
            size: 15,
          }),
        ]);

        if (!cancelled) {
          setSummary(sum);
          setMistakes(list.content);
          setTotalPages(list.totalPages);
          setTotalElements(list.totalElements);
        }
      } catch {
        // fallback
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadData();
    return () => {
      cancelled = true;
    };
  }, [statusFilter, chapterIdFilter, page]);

  async function handleStartPractice(scope?: { chapterId?: number; subjectId?: number }) {
    setPracticing(true);
    setPracticeError(null);
    try {
      const attempt = await startMistakePractice({
        chapterId: scope?.chapterId || chapterIdFilter || null,
        subjectId: scope?.subjectId || null,
        count: 10,
      });
      navigate(LEARNING_ROUTES.QUIZ_ATTEMPT(attempt.attemptId));
    } catch (err: unknown) {
      const mapped = mapStudentError(err);
      setPracticeError(mapped.message);
    } finally {
      setPracticing(false);
    }
  }

  function handleFilterStatus(st: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.set('status', st);
      return next;
    });
    setPage(0);
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading your mistake notebook…</span>
      </div>
    );
  }

  const activeCount = summary?.activeCount ?? 0;
  const practiceableCount = summary?.practiceableCount ?? 0;
  const masteredCount = summary?.masteredCount ?? 0;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <Award className="text-primary" size={24} />
            <span>Mistake Practice Notebook</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Turn your quiz errors into lasting knowledge through targeted spaced repetition.
          </p>
        </div>

        {/* Global Practice Button */}
        <button
          type="button"
          disabled={practicing || practiceableCount === 0}
          onClick={() => handleStartPractice()}
          className="px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed self-start sm:self-auto"
          title={
            practiceableCount === 0
              ? 'No practiceable mistakes available right now'
              : 'Start spaced-repetition practice across all mistakes'
          }
        >
          {practicing ? (
            <>
              <Loader2 size={14} className="animate-spin" />
              <span>Starting Session…</span>
            </>
          ) : (
            <>
              <Play size={14} />
              <span>Practise My Mistakes ({practiceableCount})</span>
            </>
          )}
        </button>
      </div>

      {practiceError && (
        <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
          {practiceError}
        </div>
      )}

      {/* Mastery Explanation Banner */}
      <div className="rounded-2xl border border-primary/20 bg-primary/5 p-4 flex items-start gap-3 text-xs text-muted-foreground leading-relaxed">
        <Info size={16} className="text-primary shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-foreground">How mastery works: </span>
          When you answer a question incorrectly during any quiz or assessment, it enters your mistake notebook.
          To achieve <strong>Mastered</strong> status, answer it correctly <strong>3 times in a row</strong> during mistake practice sessions.
        </div>
      </div>

      {/* Summary Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="p-5 rounded-3xl border border-border bg-card shadow-2xs">
          <div className="flex items-center justify-between text-xs font-semibold text-muted-foreground">
            <span>Active Mistakes</span>
            <Flame size={16} className="text-amber-500" />
          </div>
          <p className="text-3xl font-black text-foreground mt-2">{activeCount}</p>
          <p className="text-[11px] text-muted-foreground mt-1">
            Questions currently needing repetition
          </p>
        </div>

        <div className="p-5 rounded-3xl border border-border bg-card shadow-2xs">
          <div className="flex items-center justify-between text-xs font-semibold text-muted-foreground">
            <span>Ready to Practise</span>
            <Play size={16} className="text-primary" />
          </div>
          <p className="text-3xl font-black text-primary mt-2">{practiceableCount}</p>
          <p className="text-[11px] text-muted-foreground mt-1">
            Eligible for a practice session today
          </p>
        </div>

        <div className="p-5 rounded-3xl border border-border bg-card shadow-2xs">
          <div className="flex items-center justify-between text-xs font-semibold text-muted-foreground">
            <span>Mastered</span>
            <CheckCircle2 size={16} className="text-emerald-500" />
          </div>
          <p className="text-3xl font-black text-emerald-600 dark:text-emerald-400 mt-2">{masteredCount}</p>
          <p className="text-[11px] text-muted-foreground mt-1">
            Answered correctly 3 times in a row
          </p>
        </div>
      </div>

      {/* Per-Chapter Mistake Distribution Table */}
      {summary?.byChapter && summary.byChapter.length > 0 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-3">
          <h3 className="text-sm font-bold text-foreground">Mistakes by Chapter</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-border text-muted-foreground">
                  <th className="py-2.5 font-semibold">Chapter</th>
                  <th className="py-2.5 font-semibold">Active</th>
                  <th className="py-2.5 font-semibold">Practiceable</th>
                  <th className="py-2.5 font-semibold">Mastered</th>
                  <th className="py-2.5 text-right font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {summary.byChapter.map(ch => (
                  <tr key={ch.chapterId} className="hover:bg-muted/30 transition">
                    <td className="py-3 font-semibold text-foreground">
                      {ch.chapterName}
                      <span className="text-[10px] text-muted-foreground ml-1.5 font-normal">
                        ({ch.subjectName})
                      </span>
                    </td>
                    <td className="py-3 text-amber-600 dark:text-amber-400 font-bold">{ch.activeCount}</td>
                    <td className="py-3 text-primary font-bold">{ch.practiceableCount}</td>
                    <td className="py-3 text-emerald-600 dark:text-emerald-400 font-bold">{ch.masteredCount}</td>
                    <td className="py-3 text-right">
                      <button
                        type="button"
                        disabled={practicing || ch.practiceableCount === 0}
                        onClick={() => handleStartPractice({ chapterId: ch.chapterId })}
                        className="px-3 py-1.5 rounded-xl border border-border text-xs font-semibold text-foreground hover:bg-muted transition disabled:opacity-30 disabled:pointer-events-none"
                      >
                        Practise
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
        <div className="inline-flex rounded-xl border border-border p-0.5 bg-muted/40">
          <button
            type="button"
            onClick={() => handleFilterStatus('ACTIVE')}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
              statusFilter === 'ACTIVE'
                ? 'bg-card text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Active ({activeCount})
          </button>
          <button
            type="button"
            onClick={() => handleFilterStatus('MASTERED')}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
              statusFilter === 'MASTERED'
                ? 'bg-card text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Mastered ({masteredCount})
          </button>
          <button
            type="button"
            onClick={() => handleFilterStatus('ALL')}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
              statusFilter === 'ALL'
                ? 'bg-card text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            All Questions ({activeCount + masteredCount})
          </button>
        </div>

        <span className="text-xs text-muted-foreground">
          Showing {mistakes.length} of {totalElements} mistakes
        </span>
      </div>

      {/* Mistakes List */}
      {mistakes.length === 0 ? (
        <EmptyState
          icon={Award}
          title={statusFilter === 'MASTERED' ? 'No mastered mistakes yet' : 'No active mistakes'}
          description={
            statusFilter === 'MASTERED'
              ? 'Complete mistake practice sessions to master questions and move them here.'
              : 'Great job! You have no active errors recorded in this scope.'
          }
          actionText="Explore Syllabus"
          actionHref="/subjects"
        />
      ) : (
        <div className="rounded-3xl border border-border bg-card divide-y divide-border overflow-hidden shadow-xs">
          {mistakes.map(m => {
            const streak = m.consecutiveCorrect ?? 0;
            const isMastered = m.status === 'MASTERED' || streak >= 3;

            return (
              <div key={m.mistakeId} className="p-5 space-y-3 hover:bg-muted/20 transition">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-bold text-muted-foreground bg-muted px-2.5 py-0.5 rounded-full">
                      {m.chapterName}
                    </span>
                    {m.topicName && (
                      <span className="text-[11px] text-muted-foreground flex items-center gap-1">
                        <FolderTree size={11} />
                        <span>{m.topicName}</span>
                      </span>
                    )}
                  </div>

                  {/* 3-step streak indicator */}
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground font-medium">
                      Streak: {streak} of 3 in a row
                    </span>
                    <div className="flex items-center gap-1" aria-label={`Streak ${streak} of 3`}>
                      {[1, 2, 3].map(step => (
                        <div
                          key={step}
                          className={`h-2.5 w-2.5 rounded-full transition ${
                            step <= streak
                              ? isMastered
                                ? 'bg-emerald-500'
                                : 'bg-primary'
                              : 'bg-muted border border-border'
                          }`}
                        />
                      ))}
                    </div>
                  </div>
                </div>

                <p className="text-sm font-semibold text-foreground leading-relaxed">
                  {m.questionText}
                </p>

                <div className="flex items-center justify-between text-xs pt-1">
                  <span className="text-muted-foreground">
                    {m.lastPracticedAt
                      ? `Last practiced: ${new Date(m.lastPracticedAt).toLocaleDateString()}`
                      : 'Not yet practiced'}
                  </span>

                  {isMastered ? (
                    <span className="inline-flex items-center gap-1 font-bold text-emerald-600 dark:text-emerald-400">
                      <CheckCircle2 size={13} />
                      <span>Mastered</span>
                    </span>
                  ) : (
                    <span className="text-amber-600 dark:text-amber-400 font-semibold">
                      Needs {3 - streak} more correct
                    </span>
                  )}
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
