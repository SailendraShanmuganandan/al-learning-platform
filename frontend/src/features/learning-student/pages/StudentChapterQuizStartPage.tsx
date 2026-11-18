import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getQuizAvailability, startQuizAttempt } from '../api';
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
  ArrowLeft,
} from 'lucide-react';

export default function StudentChapterQuizStartPage() {
  const { quizId } = useParams<{ quizId: string }>();
  const qId = Number(quizId);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [availability, setAvailability] = useState<QuizAvailabilityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedLanguage, setSelectedLanguage] = useState<string>('EN');
  const [starting, setStarting] = useState(false);
  const [resumedNotice, setResumedNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!qId) return;
    let cancelled = false;

    async function load() {
      try {
        const data = await getQuizAvailability(qId);
        if (!cancelled) {
          setAvailability(data);
          if (data.defaultLanguage) setSelectedLanguage(data.defaultLanguage);
        }
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
  }, [qId]);

  async function handleStart() {
    if (!isAuthenticated) {
      navigate(`/login?returnUrl=/quizzes/${qId}`);
      return;
    }
    setStarting(true);
    setResumedNotice(null);

    try {
      const attempt = await startQuizAttempt(qId, selectedLanguage);
      if (attempt.resumed) {
        setResumedNotice('Resumed existing in-progress attempt. Your original sampled questions are kept.');
        setTimeout(() => {
          navigate(LEARNING_ROUTES.QUIZ_ATTEMPT(attempt.attemptId));
        }, 1200);
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
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading quiz details…</span>
      </div>
    );
  }

  if (error || !availability) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center space-y-3">
        <AlertCircle size={28} className="text-destructive mx-auto" />
        <h2 className="text-sm font-bold text-destructive">Unable to Load Quiz</h2>
        <p className="text-xs text-muted-foreground">{error || 'Quiz not found.'}</p>
        <Link
          to="/subjects"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold"
        >
          Browse Subjects
        </Link>
      </div>
    );
  }

  const languages = availability.languages || [];
  const showLang = languages.length > 1;

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <Link
        to={availability.chapterId ? `/chapters/${availability.chapterId}` : '/subjects'}
        className="inline-flex items-center gap-1 text-xs font-semibold text-muted-foreground hover:text-foreground transition"
      >
        <ArrowLeft size={14} />
        <span>Back to Chapter</span>
      </Link>

      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <HelpCircle size={24} />
          </div>
          <div>
            <span className="text-[11px] font-bold text-primary uppercase tracking-wide">
              {availability.chapterName || 'Chapter Quiz'}
            </span>
            <h1 className="text-xl font-black text-foreground tracking-tight">
              {availability.title || 'Chapter Assessment'}
            </h1>
          </div>
        </div>

        {availability.description && (
          <p className="text-xs text-muted-foreground leading-relaxed whitespace-pre-line">
            {availability.description}
          </p>
        )}

        {/* Availability status */}
        {!availability.available ? (
          <div className="p-4 rounded-2xl border border-amber-500/30 bg-amber-500/10 text-xs text-amber-800 dark:text-amber-200">
            <h3 className="font-bold">Not Available Yet</h3>
            <p className="mt-1">
              {availability.unavailableReason === 'NOT_ENOUGH_QUESTIONS'
                ? `This quiz needs at least ${availability.minEligibleQuestions || 5} approved questions before it can be attempted.`
                : 'This quiz is currently unavailable.'}
            </p>
          </div>
        ) : (
          <div className="p-4 rounded-2xl border border-border bg-muted/20 space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-border/40">
              <span className="text-muted-foreground">Format</span>
              <span className="font-semibold text-foreground">Multiple Choice Questions (MCQ)</span>
            </div>
            <div className="flex justify-between py-1 border-b border-border/40">
              <span className="text-muted-foreground">Questions per attempt</span>
              <span className="font-semibold text-foreground">{availability.questionsPerAttempt || 10}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted-foreground">Scoring</span>
              <span className="font-semibold text-foreground">Instant grading &amp; mistake analysis</span>
            </div>
          </div>
        )}

        {showLang && availability.available && (
          <div className="flex items-center gap-2">
            <label htmlFor="start-lang-select" className="text-xs font-semibold text-muted-foreground flex items-center gap-1">
              <Languages size={13} />
              <span>Language:</span>
            </label>
            <select
              id="start-lang-select"
              value={selectedLanguage}
              onChange={e => setSelectedLanguage(e.target.value)}
              className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              {languages.map(l => (
                <option key={l.language} value={l.language}>
                  {l.language === 'EN' ? 'English' : l.language === 'SI' ? 'Sinhala' : 'Tamil'}
                </option>
              ))}
            </select>
          </div>
        )}

        {resumedNotice && (
          <div className="p-3.5 rounded-2xl border border-primary/20 bg-primary/5 text-xs text-primary flex items-center gap-2">
            <AlertCircle size={15} className="shrink-0" />
            <span>{resumedNotice}</span>
          </div>
        )}

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            type="button"
            disabled={!availability.available || starting}
            onClick={handleStart}
            className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2 disabled:opacity-40"
          >
            {starting ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                <span>Preparing Attempt…</span>
              </>
            ) : (
              <>
                <Play size={14} />
                <span>Start Quiz</span>
              </>
            )}
          </button>

          {isAuthenticated && (
            <Link
              to={LEARNING_ROUTES.MY_ATTEMPTS(qId)}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground transition"
            >
              <History size={13} />
              <span>View my attempt history</span>
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
