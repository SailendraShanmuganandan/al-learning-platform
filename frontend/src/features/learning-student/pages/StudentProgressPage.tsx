import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getSyllabusProgress } from '../api';
import type { SyllabusProgressResponse } from '../types';
import { LevelBadge } from '../components/LevelBadge';
import { TrendIcon } from '../components/TrendIcon';
import { ProgressBar } from '../components/ScoreRing';
import { LEARNING_ROUTES } from '../routes';
import {
  BarChart3,
  BookOpen,
  ChevronDown,
  ChevronRight,
  Loader2,
  Play,
  Info,
} from 'lucide-react';

export default function StudentProgressPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const subjectIdParam = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;

  const [progress, setProgress] = useState<SyllabusProgressResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSubjects, setExpandedSubjects] = useState<Record<number, boolean>>({});

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const data = await getSyllabusProgress(subjectIdParam);
        if (!cancelled) {
          setProgress(data);
          // Expand all subjects by default
          const exp: Record<number, boolean> = {};
          data.subjects.forEach(s => {
            exp[s.subjectId] = true;
          });
          setExpandedSubjects(exp);
        }
      } catch (err: unknown) {
        if (!cancelled) setError((err as { message?: string })?.message || 'Failed to load progress.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [subjectIdParam]);

  function toggleSubject(subId: number) {
    setExpandedSubjects(prev => ({
      ...prev,
      [subId]: !prev[subId],
    }));
  }

  function handleFilterSubject(subId: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (subId) next.set('subjectId', subId);
      else next.delete('subjectId');
      return next;
    });
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Computing your syllabus mastery map…</span>
      </div>
    );
  }

  if (error || !progress) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center text-xs text-destructive">
        {error || 'Failed to load syllabus progress.'}
      </div>
    );
  }

  const overall = progress.overall;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <BarChart3 className="text-primary" size={24} />
            <span>Syllabus Progress Map</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Holistic mastery breakdown across all chapters and subjects in the A/L ICT curriculum.
          </p>
        </div>

        {/* Subject Filter */}
        {progress.subjects.length > 1 && (
          <div className="flex items-center gap-2">
            <label htmlFor="progress-subject-filter" className="text-xs font-semibold text-muted-foreground">
              Subject:
            </label>
            <select
              id="progress-subject-filter"
              value={subjectIdParam ?? ''}
              onChange={e => handleFilterSubject(e.target.value)}
              className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Subjects</option>
              {progress.subjects.map(s => (
                <option key={s.subjectId} value={s.subjectId}>
                  {s.subjectName}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Overall Summary Card */}
      <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 md:p-8 shadow-sm space-y-6">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div>
            <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
              Syllabus Coverage
            </span>
            <p className="text-2xl sm:text-3xl font-black text-foreground mt-1">
              {Math.round(overall.coveragePercent)}%
            </p>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              {overall.assessedChapters} of {overall.totalChapters} chapters assessed
            </p>
          </div>

          <div>
            <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
              Average Accuracy
            </span>
            <p className="text-2xl sm:text-3xl font-black text-primary mt-1">
              {overall.averagePercent != null ? `${Math.round(overall.averagePercent)}%` : '—'}
            </p>
            <p className="text-[11px] text-muted-foreground mt-0.5">Across completed attempts</p>
          </div>

          <div>
            <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
              Strong Chapters
            </span>
            <p className="text-2xl sm:text-3xl font-black text-emerald-600 dark:text-emerald-400 mt-1">
              {overall.strongCount}
            </p>
            <p className="text-[11px] text-muted-foreground mt-0.5">&gt; 75% accuracy</p>
          </div>

          <div>
            <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
              Needs Practice
            </span>
            <p className="text-2xl sm:text-3xl font-black text-amber-600 dark:text-amber-400 mt-1">
              {overall.weakCount}
            </p>
            <p className="text-[11px] text-muted-foreground mt-0.5">&lt; 50% accuracy</p>
          </div>
        </div>

        <ProgressBar value={overall.coveragePercent} height={10} showLabel={false} />
      </div>

      {/* State Legend */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-2xs">
        <div className="flex items-center gap-1.5 text-xs font-bold text-foreground mb-3">
          <Info size={14} className="text-primary" />
          <span>Understanding Mastery States:</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 text-xs">
          <div className="flex items-start gap-2">
            <LevelBadge level="STRONG" size="sm" />
            <span className="text-muted-foreground text-[11px] leading-tight">
              ≥ 75% accuracy with consistent performance.
            </span>
          </div>
          <div className="flex items-start gap-2">
            <LevelBadge level="DEVELOPING" size="sm" />
            <span className="text-muted-foreground text-[11px] leading-tight">
              50% - 74% accuracy; progressing well.
            </span>
          </div>
          <div className="flex items-start gap-2">
            <LevelBadge level="WEAK" size="sm" />
            <span className="text-muted-foreground text-[11px] leading-tight">
              &lt; 50% accuracy; prioritised for revision.
            </span>
          </div>
          <div className="flex items-start gap-2">
            <LevelBadge level="INSUFFICIENT_DATA" size="sm" />
            <span className="text-muted-foreground text-[11px] leading-tight">
              Fewer than 10 questions answered yet.
            </span>
          </div>
        </div>
      </div>

      {/* Per-Subject Expandable Chapters */}
      <div className="space-y-4">
        {progress.subjects.map(sub => {
          const isExpanded = expandedSubjects[sub.subjectId] ?? true;

          return (
            <div key={sub.subjectId} className="rounded-3xl border border-border bg-card overflow-hidden shadow-xs">
              {/* Subject Bar Header */}
              <button
                type="button"
                onClick={() => toggleSubject(sub.subjectId)}
                className="w-full p-5 flex items-center justify-between text-left hover:bg-muted/30 transition border-b border-border/60"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary font-bold">
                    <BookOpen size={18} />
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-foreground">{sub.subjectName}</h2>
                    <p className="text-xs text-muted-foreground">
                      {sub.summary.assessedChapters} of {sub.summary.totalChapters} chapters assessed ·{' '}
                      {Math.round(sub.summary.coveragePercent)}% coverage
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <ChevronDown
                    size={18}
                    className={`text-muted-foreground transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                  />
                </div>
              </button>

              {/* Chapters List */}
              {isExpanded && (
                <div className="divide-y divide-border">
                  {sub.chapters.map(chap => {
                    const isWeak = chap.state === 'WEAK' || chap.level === 'WEAK';
                    const hasPercent = chap.percent != null;

                    return (
                      <div
                        key={chap.chapterId}
                        className="p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-muted/10 transition"
                      >
                        <div className="space-y-1.5 flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-muted-foreground">
                              Chapter {chap.chapterOrder}:
                            </span>
                            <Link
                              to={`/chapters/${chap.chapterId}`}
                              className="text-sm font-bold text-foreground hover:text-primary transition truncate"
                            >
                              {chap.chapterName}
                            </Link>
                          </div>

                          <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                            <span>{chap.answerCount} questions answered</span>
                            {chap.trend && (
                              <div className="flex items-center gap-1">
                                <span>· Trend:</span>
                                <TrendIcon trend={chap.trend} showText />
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Progress Bar & Badges */}
                        <div className="flex items-center justify-between md:justify-end gap-4 shrink-0">
                          {hasPercent && (
                            <div className="w-24 sm:w-28 text-right">
                              <span className="text-xs font-bold text-foreground">
                                {chap.percent}%
                              </span>
                              <ProgressBar value={chap.percent!} height={6} showLabel={false} />
                            </div>
                          )}

                          <LevelBadge level={chap.state || chap.level} size="md" />

                          {/* Action Button: Chapter Link or Weak Practice Shortcut */}
                          {isWeak && chap.quizAvailable && chap.quizId ? (
                            <Link
                              to={LEARNING_ROUTES.QUIZ_START(chap.quizId)}
                              className="px-3.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition shadow-xs flex items-center gap-1.5 shrink-0"
                            >
                              <Play size={12} />
                              <span>Practise</span>
                            </Link>
                          ) : (
                            <Link
                              to={`/chapters/${chap.chapterId}`}
                              className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted transition"
                              title="Go to chapter page"
                            >
                              <ChevronRight size={16} />
                            </Link>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
