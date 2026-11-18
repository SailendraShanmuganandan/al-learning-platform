import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getChapterQuizAvailability, startQuizAttempt } from '../api';
import type { QuizAvailabilityResponse } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import {
  HelpCircle,
  Play,
  Loader2,
  AlertCircle,
  History,
  Languages,
} from 'lucide-react';

interface ChapterQuizCardProps {
  chapterId: number;
  chapterName?: string;
}

export function ChapterQuizCard({ chapterId }: ChapterQuizCardProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();

  const [availability, setAvailability] = useState<QuizAvailabilityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedLanguage, setSelectedLanguage] = useState<string>('EN');
  const [starting, setStarting] = useState(false);
  const [startNotice, setStartNotice] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function checkAvailability() {
      try {
        const res = await getChapterQuizAvailability(chapterId);
        if (!cancelled) {
          setAvailability(res);
          if (res.defaultLanguage) {
            setSelectedLanguage(res.defaultLanguage);
          }
        }
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    checkAvailability();
    return () => {
      cancelled = true;
    };
  }, [chapterId]);

  async function handleStartQuiz() {
    if (!isAuthenticated) {
      // Send guest to login and return back to this chapter page afterwards
      navigate(`/login?returnUrl=${encodeURIComponent(location.pathname)}`);
      return;
    }

    if (!availability?.quizId) return;

    setStarting(true);
    setStartNotice(null);

    try {
      const attempt = await startQuizAttempt(availability.quizId, selectedLanguage);
      if (attempt.resumed) {
        setStartNotice('Resumed in-progress quiz attempt. Your original sampled questions have been preserved.');
        setTimeout(() => {
          navigate(LEARNING_ROUTES.QUIZ_ATTEMPT(attempt.attemptId));
        }, 1000);
      } else {
        navigate(LEARNING_ROUTES.QUIZ_ATTEMPT(attempt.attemptId));
      }
    } catch (err: unknown) {
      alert(getErrorMessage(err));
      setStarting(false);
    }
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-border bg-card p-6 flex items-center justify-center gap-2 text-muted-foreground text-xs">
        <Loader2 size={16} className="animate-spin text-primary" />
        <span>Checking chapter quiz availability…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-xs text-destructive">
        {error}
      </div>
    );
  }

  // State 1: No quiz exists
  if (!availability?.exists || !availability.quizId) {
    return (
      <div className="rounded-2xl border border-dashed border-border bg-muted/20 p-6 space-y-2">
        <div className="flex items-center gap-2">
          <HelpCircle size={16} className="text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">Chapter Quiz</h3>
        </div>
        <p className="text-xs text-muted-foreground">
          No quiz for this chapter yet. Questions will be added as the syllabus expands.
        </p>
      </div>
    );
  }

  // State 2: Exists but unavailable
  if (!availability.available) {
    return (
      <div className="rounded-2xl border border-border bg-card p-6 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <HelpCircle size={16} className="text-primary" />
            <h3 className="text-sm font-bold text-foreground">
              {availability.title || 'Chapter Quiz'}
            </h3>
          </div>
          <span className="text-[11px] font-semibold text-amber-600 dark:text-amber-400 bg-amber-500/10 px-2.5 py-0.5 rounded-full border border-amber-500/20">
            Not available yet
          </span>
        </div>
        <p className="text-xs text-muted-foreground leading-relaxed">
          {availability.unavailableReason === 'NOT_ENOUGH_QUESTIONS'
            ? `This quiz requires at least ${availability.minEligibleQuestions || 5} approved questions before it can be attempted.`
            : 'This chapter quiz is currently undergoing maintenance and will be available soon.'}
        </p>
      </div>
    );
  }

  // State 3: Available
  const languages = availability.languages || [];
  const showLanguageSelect = languages.length > 1;

  return (
    <div className="rounded-2xl border border-border bg-card p-6 space-y-4 shadow-xs">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <HelpCircle size={18} />
          </div>
          <div>
            <h3 className="text-sm font-bold text-foreground">
              {availability.title || 'Chapter Quiz'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {availability.questionsPerAttempt || 10} questions per attempt · Multiple Choice
            </p>
          </div>
        </div>

        {isAuthenticated && availability.quizId && (
          <Link
            to={LEARNING_ROUTES.MY_ATTEMPTS(availability.quizId)}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline self-start sm:self-auto"
          >
            <History size={13} />
            <span>My attempts</span>
          </Link>
        )}
      </div>

      {availability.description && (
        <p className="text-xs text-muted-foreground leading-relaxed">
          {availability.description}
        </p>
      )}

      {startNotice && (
        <div className="p-3 rounded-xl border border-primary/20 bg-primary/5 text-xs text-primary flex items-center gap-2">
          <AlertCircle size={14} className="shrink-0" />
          <span>{startNotice}</span>
        </div>
      )}

      {/* Language select if multiple */}
      {showLanguageSelect && (
        <div className="flex items-center gap-2 pt-1">
          <label htmlFor="quiz-language-select" className="text-xs font-semibold text-muted-foreground flex items-center gap-1">
            <Languages size={13} />
            <span>Language:</span>
          </label>
          <select
            id="quiz-language-select"
            value={selectedLanguage}
            onChange={e => setSelectedLanguage(e.target.value)}
            className="rounded-xl border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            {languages.map(l => (
              <option key={l.language} value={l.language}>
                {l.language === 'EN' ? 'English' : l.language === 'SI' ? 'Sinhala' : 'Tamil'} ({l.eligibleCount} questions)
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="pt-2 flex items-center justify-between gap-4">
        <button
          type="button"
          disabled={starting}
          onClick={handleStartQuiz}
          className="px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-2 disabled:opacity-50"
        >
          {starting ? (
            <>
              <Loader2 size={14} className="animate-spin" />
              <span>Starting Quiz…</span>
            </>
          ) : (
            <>
              <Play size={14} />
              <span>Start Quiz</span>
            </>
          )}
        </button>

        {!isAuthenticated && (
          <span className="text-[11px] text-muted-foreground">
            Sign in to record your scores and track syllabus mastery
          </span>
        )}
      </div>
    </div>
  );
}
