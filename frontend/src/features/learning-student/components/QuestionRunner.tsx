import { useState, useEffect, useRef } from 'react';
import type { QuizAttemptQuestionDto } from '../types';
import { DifficultyBadge } from './DifficultyBadge';
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Send,
  Loader2,
  Clock,
  AlertCircle,
  FolderTree,
} from 'lucide-react';

interface QuestionRunnerProps {
  questions: QuizAttemptQuestionDto[];
  onSubmit: (answers: { questionId: number; selectedOptionId: number }[]) => Promise<void>;
  storageKey: string;
  title?: string;
  subtitle?: string;
  timerSeconds?: number;
  isTimeUp?: boolean;
}

export function QuestionRunner({
  questions,
  onSubmit,
  storageKey,
  title,
  subtitle,
  timerSeconds,
  isTimeUp = false,
}: QuestionRunnerProps) {
  // Map of questionId -> selectedOptionId
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, number>>(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (typeof parsed === 'object' && parsed !== null) {
          return parsed;
        }
      }
    } catch {
      // ignore
    }
    return {};
  });

  const [currentIndex, setCurrentIndex] = useState(0);
  const [isReviewMode, setIsReviewMode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Sync draft to localStorage on changes
  useEffect(() => {
    try {
      if (Object.keys(selectedAnswers).length > 0) {
        localStorage.setItem(storageKey, JSON.stringify(selectedAnswers));
      }
    } catch {
      // ignore
    }
  }, [selectedAnswers, storageKey]);

  // Warn before unload if progress has been made
  const hasAnswers = Object.keys(selectedAnswers).length > 0;
  const isComplete = questions.length > 0 && questions.every(q => selectedAnswers[q.questionId] !== undefined);

  useEffect(() => {
    function handleBeforeUnload(e: BeforeUnloadEvent) {
      if (hasAnswers && !submitting) {
        e.preventDefault();
        e.returnValue = '';
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [hasAnswers, submitting]);

  const currentQ = questions[currentIndex];
  const answeredCount = questions.filter(q => selectedAnswers[q.questionId] !== undefined).length;
  const totalCount = questions.length;

  const questionRef = useRef<HTMLDivElement>(null);

  // Focus management when question changes
  useEffect(() => {
    questionRef.current?.focus();
  }, [currentIndex, isReviewMode]);

  function handleSelectOption(optionId: number) {
    if (submitting || !currentQ) return;
    setSelectedAnswers(prev => ({
      ...prev,
      [currentQ.questionId]: optionId,
    }));
  }

  function handleNext() {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex(prev => prev + 1);
    } else {
      setIsReviewMode(true);
    }
  }

  function handlePrev() {
    if (isReviewMode) {
      setIsReviewMode(false);
    } else if (currentIndex > 0) {
      setCurrentIndex(prev => prev - 1);
    }
  }

  function handleJump(index: number) {
    setIsReviewMode(false);
    setCurrentIndex(index);
  }

  async function handleFinalSubmit() {
    if (!isComplete || submitting) return;
    setSubmitting(true);
    setSubmitError(null);

    const answersPayload = questions.map(q => ({
      questionId: q.questionId,
      selectedOptionId: selectedAnswers[q.questionId],
    }));

    try {
      await onSubmit(answersPayload);
      try {
        localStorage.removeItem(storageKey);
      } catch {
        // ignore
      }
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string; unansweredQuestionIds?: number[]; status?: number };
      if (e?.code === 'UNANSWERED_QUESTIONS' && e.unansweredQuestionIds && e.unansweredQuestionIds.length > 0) {
        const firstMissing = e.unansweredQuestionIds[0];
        const idx = questions.findIndex(q => q.questionId === firstMissing);
        if (idx !== -1) {
          setIsReviewMode(false);
          setCurrentIndex(idx);
          setSubmitError('Please answer this required question before submitting.');
        } else {
          setSubmitError('Some questions were left unanswered.');
        }
      } else if (e?.status === 409) {
        // already submitted
        try {
          localStorage.removeItem(storageKey);
        } catch {
          // ignore
        }
        setSubmitError(e.message || 'Attempt already submitted. Loading results…');
      } else {
        setSubmitError(e?.message || 'Failed to submit quiz. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  function formatTime(sec: number) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header bar */}
      <div className="rounded-3xl border border-border bg-card p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          {title && (
            <h1 className="text-xl font-black text-foreground tracking-tight">
              {title}
            </h1>
          )}
          {subtitle && (
            <p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>
          )}
          <div className="mt-2 flex items-center gap-2 text-xs font-semibold text-primary">
            <span>
              {answeredCount} of {totalCount} answered
            </span>
            <span className="text-muted-foreground">·</span>
            <span className="text-muted-foreground">
              {Math.round((answeredCount / (totalCount || 1)) * 100)}% complete
            </span>
          </div>
        </div>

        {/* Timer if present */}
        {timerSeconds !== undefined && (
          <div
            className={`flex items-center gap-2 px-4 py-2 rounded-2xl border font-bold text-sm ${
              isTimeUp || timerSeconds <= 60
                ? 'bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400 animate-pulse'
                : 'bg-primary/5 border-primary/20 text-foreground'
            }`}
          >
            <Clock size={16} />
            <span>{isTimeUp ? "Time's up" : formatTime(timerSeconds)}</span>
          </div>
        )}
      </div>

      {/* Question Navigator Dots / Grid */}
      <nav aria-label="Question Navigator" className="rounded-2xl border border-border bg-card p-3 shadow-2xs">
        <div className="flex flex-wrap items-center gap-1.5 justify-center sm:justify-start">
          {questions.map((q, idx) => {
            const isAnswered = selectedAnswers[q.questionId] !== undefined;
            const isCurrent = !isReviewMode && currentIndex === idx;

            let buttonClass = 'border-border text-muted-foreground bg-muted/30 hover:bg-muted';
            if (isAnswered) {
              buttonClass = 'bg-primary/10 border-primary/30 text-primary font-bold';
            }
            if (isCurrent) {
              buttonClass = 'ring-2 ring-primary ring-offset-2 ring-offset-background bg-primary text-primary-foreground font-bold';
            }

            return (
              <button
                key={q.questionId}
                type="button"
                onClick={() => handleJump(idx)}
                className={`h-8 w-8 rounded-xl border text-xs font-semibold transition flex items-center justify-center ${buttonClass}`}
                aria-label={`Go to Question ${idx + 1}${isAnswered ? ' (Answered)' : ' (Unanswered)'}`}
              >
                {idx + 1}
              </button>
            );
          })}

          <button
            type="button"
            onClick={() => setIsReviewMode(true)}
            className={`ml-auto px-3 py-1.5 rounded-xl border text-xs font-semibold transition ${
              isReviewMode
                ? 'bg-primary text-primary-foreground border-primary'
                : 'border-border text-muted-foreground hover:bg-muted'
            }`}
          >
            Review Summary
          </button>
        </div>
      </nav>

      {submitError && (
        <div
          role="alert"
          className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-xs text-destructive flex items-center gap-2"
        >
          <AlertCircle size={16} className="shrink-0" />
          <span>{submitError}</span>
        </div>
      )}

      {/* Main Runner Area */}
      {!isReviewMode && currentQ ? (
        <div
          ref={questionRef}
          tabIndex={-1}
          className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6 focus:outline-hidden"
        >
          {/* Question Metadata */}
          <div className="flex flex-wrap items-center justify-between gap-2 pb-4 border-b border-border">
            <span className="text-xs font-black tracking-wider uppercase text-muted-foreground">
              Question {currentIndex + 1} of {totalCount}
            </span>
            <div className="flex items-center gap-2">
              {currentQ.topicName && (
                <span className="inline-flex items-center gap-1 text-[11px] font-medium text-muted-foreground bg-muted px-2.5 py-1 rounded-full">
                  <FolderTree size={11} />
                  <span>{currentQ.topicName}</span>
                </span>
              )}
              <DifficultyBadge difficulty={currentQ.difficulty} size="sm" />
            </div>
          </div>

          {/* Accessible Fieldset for Options */}
          <fieldset className="space-y-4">
            <legend className="text-base sm:text-lg font-bold text-foreground leading-relaxed">
              {currentQ.text}
            </legend>

            <div className="space-y-2.5 pt-2" role="radiogroup" aria-label={`Options for Question ${currentIndex + 1}`}>
              {currentQ.options.map((opt, optIndex) => {
                const isSelected = selectedAnswers[currentQ.questionId] === opt.optionId;
                const letter = String.fromCharCode(65 + optIndex);

                return (
                  <label
                    key={opt.optionId}
                    htmlFor={`opt-${currentQ.questionId}-${opt.optionId}`}
                    className={`flex items-start gap-3.5 p-4 rounded-2xl border cursor-pointer transition select-none ${
                      isSelected
                        ? 'border-primary bg-primary/5 text-foreground ring-1 ring-primary/40'
                        : 'border-border bg-card hover:bg-muted/40 text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    <input
                      type="radio"
                      id={`opt-${currentQ.questionId}-${opt.optionId}`}
                      name={`question-${currentQ.questionId}`}
                      value={opt.optionId}
                      checked={isSelected}
                      onChange={() => handleSelectOption(opt.optionId)}
                      className="sr-only"
                    />

                    <span
                      className={`h-6 w-6 rounded-lg text-xs font-bold flex items-center justify-center shrink-0 transition ${
                        isSelected
                          ? 'bg-primary text-primary-foreground shadow-xs'
                          : 'border border-border bg-muted/50 text-muted-foreground'
                      }`}
                      aria-hidden
                    >
                      {letter}
                    </span>

                    <span className="text-sm font-medium leading-relaxed flex-1 pt-0.5">
                      {opt.text}
                    </span>

                    {isSelected && (
                      <CheckCircle2 size={18} className="text-primary shrink-0 mt-0.5" />
                    )}
                  </label>
                );
              })}
            </div>
          </fieldset>

          {/* Bottom Navigation Controls */}
          <div className="pt-6 border-t border-border flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={handlePrev}
              disabled={currentIndex === 0}
              className="px-4 py-2 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:text-foreground hover:bg-muted transition disabled:opacity-30 disabled:pointer-events-none flex items-center gap-1.5"
            >
              <ChevronLeft size={14} />
              <span>Previous</span>
            </button>

            {currentIndex === questions.length - 1 ? (
              <button
                type="button"
                onClick={() => setIsReviewMode(true)}
                className="px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5"
              >
                <span>Review &amp; Submit</span>
                <ChevronRight size={14} />
              </button>
            ) : (
              <button
                type="button"
                onClick={handleNext}
                className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5"
              >
                <span>Next</span>
                <ChevronRight size={14} />
              </button>
            )}
          </div>
        </div>
      ) : (
        /* Review Mode View */
        <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-border">
            <div>
              <h2 className="text-lg font-bold text-foreground">Review Your Answers</h2>
              <p className="text-xs text-muted-foreground">
                Double-check your choices before submitting. You cannot change answers after submission.
              </p>
            </div>
            <div className="text-xs font-bold text-foreground bg-muted px-3 py-1.5 rounded-xl self-start sm:self-auto">
              {answeredCount} / {totalCount} Answered
            </div>
          </div>

          <div className="space-y-3 max-h-[50vh] overflow-y-auto pr-1">
            {questions.map((q, idx) => {
              const ansId = selectedAnswers[q.questionId];
              const isAnswered = ansId !== undefined;
              const chosenOpt = q.options.find(o => o.optionId === ansId);

              return (
                <div
                  key={q.questionId}
                  className={`p-3.5 rounded-2xl border transition flex items-center justify-between gap-3 ${
                    isAnswered
                      ? 'border-border bg-card'
                      : 'border-amber-500/30 bg-amber-500/5'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <span
                      className={`h-7 w-7 rounded-xl text-xs font-bold flex items-center justify-center shrink-0 ${
                        isAnswered
                          ? 'bg-primary/10 text-primary'
                          : 'bg-amber-500/10 text-amber-600'
                      }`}
                    >
                      {idx + 1}
                    </span>
                    <div className="min-w-0">
                      <p className="text-xs font-medium text-foreground truncate">
                        {q.text}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5 truncate">
                        {isAnswered ? `Answered: ${chosenOpt?.text || 'Option selected'}` : 'Not answered yet'}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleJump(idx)}
                    className="px-3 py-1 rounded-lg border border-border text-xs font-semibold text-primary hover:bg-primary/5 transition shrink-0"
                  >
                    {isAnswered ? 'Change' : 'Answer'}
                  </button>
                </div>
              );
            })}
          </div>

          <div className="pt-4 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => setIsReviewMode(false)}
              className="px-4 py-2 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:bg-muted transition"
            >
              Back to Questions
            </button>

            <div className="flex items-center gap-3 w-full sm:w-auto">
              {!isComplete && (
                <span className="text-xs text-amber-600 dark:text-amber-400 font-medium">
                  Please answer all {totalCount} questions before submitting.
                </span>
              )}

              <button
                type="button"
                disabled={!isComplete || submitting}
                onClick={handleFinalSubmit}
                className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-sm disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {submitting ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    <span>Submitting &amp; Grading…</span>
                  </>
                ) : (
                  <>
                    <Send size={14} />
                    <span>Submit &amp; View Results</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

