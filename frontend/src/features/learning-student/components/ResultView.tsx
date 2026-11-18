import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { QuizAttemptResponse, DiagnosticAssessmentResponse, QuizAttemptQuestionDto } from '../types';
import { ScoreRing } from './ScoreRing';
import { QuestionReportDialog } from './QuestionReportDialog';
import {
  CheckCircle2,
  XCircle,
  Flag,
  ArrowRight,
  Filter,
  Layers,
  Sparkles,
} from 'lucide-react';

/**
 * Extension slot for future AI-powered "Explain my mistake" feature.
 * Renders null for now as per phase specification.
 */
export function ExplainMyMistakeSlot({ questionId }: { questionId: number }) {
  void questionId;
  return null;
}

interface ResultFooterAction {
  label: string;
  href?: string;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'outline';
  icon?: React.ElementType;
}

interface ResultViewProps {
  result: QuizAttemptResponse | DiagnosticAssessmentResponse;
  title?: string;
  subtitle?: string;
  footerActions?: ResultFooterAction[];
}

export function ResultView({
  result,
  title = 'Assessment Results',
  subtitle,
  footerActions = [],
}: ResultViewProps) {
  const [filterIncorrectOnly, setFilterIncorrectOnly] = useState(false);
  const [reportingQuestionId, setReportingQuestionId] = useState<number | null>(null);
  const [reportedIds, setReportedIds] = useState<Set<number>>(() => {
    const s = new Set<number>();
    result.questions.forEach(q => {
      if (q.reportedByMe) s.add(q.questionId);
    });
    return s;
  });

  const percentage = result.percentage ?? 0;
  const score = result.score ?? 0;
  const total = result.totalMarks ?? result.questions.length;

  const correctCount = result.questions.filter(q => q.isCorrect).length;
  const incorrectCount = result.questions.length - correctCount;

  const displayedQuestions = filterIncorrectOnly
    ? result.questions.filter(q => !q.isCorrect)
    : result.questions;

  function handleReportSuccess(qId: number) {
    setReportedIds(prev => new Set(prev).add(qId));
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Score Summary Card */}
      <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 md:p-8 shadow-sm">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 text-center md:text-left">
          <div className="space-y-2">
            <span className="text-xs font-black uppercase tracking-widest text-primary">
              {result.status === 'COMPLETED' ? 'Completed' : 'Attempt Review'}
            </span>
            <h1 className="text-2xl md:text-3xl font-black text-foreground tracking-tight">
              {title}
            </h1>
            {subtitle && (
              <p className="text-xs text-muted-foreground max-w-md">{subtitle}</p>
            )}

            <div className="pt-2 flex flex-wrap items-center justify-center md:justify-start gap-4 text-xs font-semibold">
              <span className="inline-flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400">
                <CheckCircle2 size={16} />
                <span>{correctCount} Correct</span>
              </span>
              <span className="inline-flex items-center gap-1.5 text-rose-600 dark:text-rose-400">
                <XCircle size={16} />
                <span>{incorrectCount} Incorrect</span>
              </span>
              <span className="text-muted-foreground">
                Score: {score} / {total} marks
              </span>
            </div>
          </div>

          <div className="flex flex-col items-center justify-center shrink-0">
            <ScoreRing percentage={percentage} size={96} strokeWidth={8} />
          </div>
        </div>
      </div>

      {/* Chapter Breakdown if available */}
      {result.chapterBreakdown && result.chapterBreakdown.length > 1 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <Layers size={18} className="text-primary" />
            <h2 className="text-sm font-bold text-foreground">Performance by Chapter</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {result.chapterBreakdown.map(ch => {
              const chPct = Math.round((ch.accuracy ?? (ch.correctCount / (ch.totalQuestions || 1))) * 100);
              return (
                <div
                  key={ch.chapterId}
                  className="p-3.5 rounded-2xl border border-border bg-muted/20 flex flex-col justify-between"
                >
                  <p className="text-xs font-bold text-foreground truncate">{ch.chapterName}</p>
                  <div className="mt-2 flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">
                      {ch.correctCount} / {ch.totalQuestions} correct
                    </span>
                    <span
                      className={`font-extrabold ${
                        chPct >= 75
                          ? 'text-emerald-600 dark:text-emerald-400'
                          : chPct >= 50
                          ? 'text-sky-600 dark:text-sky-400'
                          : 'text-amber-600 dark:text-amber-400'
                      }`}
                    >
                      {chPct}%
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Per-question Review Header & Filter */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-2xs flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs font-bold text-foreground flex items-center gap-2">
          <span>Question Review ({result.questions.length})</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setFilterIncorrectOnly(false)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
              !filterIncorrectOnly
                ? 'bg-primary text-primary-foreground shadow-2xs'
                : 'border border-border text-muted-foreground hover:bg-muted'
            }`}
          >
            All ({result.questions.length})
          </button>
          <button
            type="button"
            onClick={() => setFilterIncorrectOnly(true)}
            disabled={incorrectCount === 0}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 ${
              filterIncorrectOnly
                ? 'bg-primary text-primary-foreground shadow-2xs'
                : 'border border-border text-muted-foreground hover:bg-muted disabled:opacity-40'
            }`}
          >
            <Filter size={12} />
            <span>Incorrect Only ({incorrectCount})</span>
          </button>
        </div>
      </div>

      {/* Questions list */}
      <div className="space-y-4">
        {displayedQuestions.map((q: QuizAttemptQuestionDto, idx: number) => {
          const isCorrect = Boolean(q.isCorrect);
          const isReported = reportedIds.has(q.questionId);

          return (
            <div
              key={q.questionId}
              className={`rounded-3xl border p-6 shadow-xs transition space-y-4 ${
                isCorrect
                  ? 'border-emerald-500/20 bg-card'
                  : 'border-rose-500/30 bg-card'
              }`}
            >
              {/* Question Header */}
              <div className="flex flex-wrap items-start justify-between gap-3 pb-3 border-b border-border">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-muted-foreground">
                    #{idx + 1}
                  </span>
                  {isCorrect ? (
                    <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-full border border-emerald-500/20">
                      <CheckCircle2 size={13} />
                      <span>Correct</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-xs font-bold text-rose-600 dark:text-rose-400 bg-rose-500/10 px-2.5 py-0.5 rounded-full border border-rose-500/20">
                      <XCircle size={13} />
                      <span>Incorrect</span>
                    </span>
                  )}
                  {q.chapterName && (
                    <span className="text-[11px] text-muted-foreground hidden sm:inline">
                      · {q.chapterName}
                    </span>
                  )}
                </div>

                <button
                  type="button"
                  disabled={isReported}
                  onClick={() => setReportingQuestionId(q.questionId)}
                  className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 rounded-xl transition ${
                    isReported
                      ? 'text-muted-foreground bg-muted cursor-not-allowed opacity-60'
                      : 'text-muted-foreground hover:text-foreground border border-border hover:bg-muted'
                  }`}
                  title={isReported ? 'You have reported this question' : 'Report an issue with this question'}
                >
                  <Flag size={12} className={isReported ? 'text-primary' : ''} />
                  <span>{isReported ? 'Reported' : 'Report question'}</span>
                </button>
              </div>

              {/* Question Text */}
              <p className="text-sm font-semibold text-foreground leading-relaxed">
                {q.text}
              </p>

              {/* Options Breakdown */}
              <div className="space-y-2">
                {q.options.map((opt, optIndex) => {
                  const letter = String.fromCharCode(65 + optIndex);
                  const isStudentPick = opt.optionId === q.selectedOptionId;
                  const isKey = opt.optionId === q.correctOptionId;

                  let optClass = 'border-border bg-muted/10 text-muted-foreground';
                  if (isKey) {
                    optClass = 'border-emerald-500/40 bg-emerald-500/10 text-emerald-900 dark:text-emerald-200 font-semibold ring-1 ring-emerald-500/30';
                  } else if (isStudentPick && !isCorrect) {
                    optClass = 'border-rose-500/40 bg-rose-500/10 text-rose-900 dark:text-rose-200 font-medium';
                  }

                  return (
                    <div
                      key={opt.optionId}
                      className={`flex items-start gap-3 p-3 rounded-2xl border text-xs transition ${optClass}`}
                    >
                      <span className="font-bold shrink-0">{letter}.</span>
                      <span className="flex-1">{opt.text}</span>

                      {isStudentPick && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-card border border-border shrink-0">
                          Your choice
                        </span>
                      )}

                      {isKey && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500 text-white shrink-0">
                          Correct answer
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Explanation if present */}
              {q.explanation && (
                <div className="rounded-2xl border border-primary/20 bg-primary/5 p-4 text-xs space-y-1">
                  <div className="flex items-center gap-1 font-bold text-primary">
                    <Sparkles size={13} />
                    <span>Explanation</span>
                  </div>
                  <p className="text-muted-foreground leading-relaxed whitespace-pre-line">
                    {q.explanation}
                  </p>
                </div>
              )}

              {/* Extension slot for Explain My Mistake (future phase) */}
              <ExplainMyMistakeSlot questionId={q.questionId} />
            </div>
          );
        })}
      </div>

      {/* Footer Navigation Actions */}
      {footerActions.length > 0 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs flex flex-wrap items-center justify-center gap-3">
          {footerActions.map((act, i) => {
            const Icon = act.icon || ArrowRight;
            const btnClass =
              act.variant === 'primary'
                ? 'bg-primary text-primary-foreground font-bold hover:bg-primary/90 shadow-xs'
                : act.variant === 'outline'
                ? 'border border-border text-foreground hover:bg-muted font-semibold'
                : 'bg-muted text-foreground font-semibold hover:bg-muted/80';

            if (act.href) {
              return (
                <Link
                  key={i}
                  to={act.href}
                  className={`px-5 py-2.5 rounded-xl text-xs transition inline-flex items-center gap-1.5 ${btnClass}`}
                >
                  <Icon size={14} />
                  <span>{act.label}</span>
                </Link>
              );
            }

            return (
              <button
                key={i}
                type="button"
                onClick={act.onClick}
                className={`px-5 py-2.5 rounded-xl text-xs transition inline-flex items-center gap-1.5 ${btnClass}`}
              >
                <Icon size={14} />
                <span>{act.label}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Report Modal */}
      {reportingQuestionId && (
        <QuestionReportDialog
          questionId={reportingQuestionId}
          isOpen={Boolean(reportingQuestionId)}
          onClose={() => setReportingQuestionId(null)}
          onReportSuccess={handleReportSuccess}
        />
      )}
    </div>
  );
}
