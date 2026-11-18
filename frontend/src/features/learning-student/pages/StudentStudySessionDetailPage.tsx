import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  getStudySession,
  completeStudySessionItem,
  abandonStudySession,
  getQuizAttempt,
} from '../api';
import type { StudySessionResponse, QuizAttemptResponse } from '../types';
import { ResultView } from '../components/ResultView';
import { LEARNING_ROUTES } from '../routes';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import {
  Clock,
  Play,
  CheckCircle2,
  ExternalLink,
  Loader2,
  AlertCircle,
  FileText,
  Video,
  RotateCcw,
} from 'lucide-react';

export default function StudentStudySessionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const sId = Number(sessionId);
  const navigate = useNavigate();

  const [session, setSession] = useState<StudySessionResponse | null>(null);
  const [completedAttempt, setCompletedAttempt] = useState<QuizAttemptResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Countdown timer state
  const [secondsRemaining, setSecondsRemaining] = useState<number>(0);
  const [isTimeUp, setIsTimeUp] = useState(false);
  const [completingItemId, setCompletingItemId] = useState<number | null>(null);
  const [abandoning, setAbandoning] = useState(false);
  const [showAbandonConfirm, setShowAbandonConfirm] = useState(false);

  // Announce polite live message for screen readers at key moments
  const [liveAnnouncement, setLiveAnnouncement] = useState('');

  // Fetch session data
  useEffect(() => {
    if (!sId) return;
    let cancelled = false;

    async function fetchSessionData() {
      try {
        const data = await getStudySession(sId);
        if (!cancelled) {
          setSession(data);
          setSecondsRemaining(data.remainingSeconds);
          setIsTimeUp(data.remainingSeconds <= 0 && data.status === 'ACTIVE');

          if (data.status === 'COMPLETED' && data.attemptId) {
            getQuizAttempt(data.attemptId)
              .then(att => {
                if (!cancelled) setCompletedAttempt(att);
              })
              .catch(() => {});
          }
        }
      } catch (err: unknown) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchSessionData();

    // Refresh on focus / visibility change to eliminate clock drift
    function handleVisibilityChange() {
      if (document.visibilityState === 'visible') {
        fetchSessionData();
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('focus', fetchSessionData);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('focus', fetchSessionData);
    };
  }, [sId]);

  // Local ticker countdown
  useEffect(() => {
    if (session?.status !== 'ACTIVE' || secondsRemaining <= 0) return;

    const interval = setInterval(() => {
      setSecondsRemaining(prev => {
        if (prev <= 1) {
          setIsTimeUp(true);
          setLiveAnnouncement("Time is up! You can still submit within the server's grace period.");
          return 0;
        }

        // Announce at 10m, 5m, 1m intervals for screen readers
        if (prev === 600) setLiveAnnouncement('10 minutes remaining in your study session.');
        if (prev === 300) setLiveAnnouncement('5 minutes remaining in your study session.');
        if (prev === 60) setLiveAnnouncement('1 minute remaining. Prepare to submit your answers.');

        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [session?.status, secondsRemaining]);

  async function handleCompleteItem(itemId: number) {
    if (!sId) return;
    setCompletingItemId(itemId);
    try {
      await completeStudySessionItem(sId, itemId);
      setSession(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          items: prev.items.map(it => (it.itemId === itemId ? { ...it, completed: true } : it)),
        };
      });
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setCompletingItemId(null);
    }
  }

  async function handleAbandonSession() {
    if (!sId) return;
    setAbandoning(true);
    try {
      await abandonStudySession(sId);
      navigate(LEARNING_ROUTES.STUDY_SESSION_START);
    } catch (err) {
      alert(getErrorMessage(err));
      setAbandoning(false);
    }
  }

  function formatTime(sec: number) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading study session…</span>
      </div>
    );
  }

  if (error || !session) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center space-y-3">
        <AlertCircle size={28} className="text-destructive mx-auto" />
        <h2 className="text-sm font-bold text-destructive">Study Session Error</h2>
        <p className="text-xs text-muted-foreground">{error || 'Session not found.'}</p>
        <Link
          to={LEARNING_ROUTES.STUDY_SESSION_START}
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold"
        >
          Start New Study Session
        </Link>
      </div>
    );
  }

  // If completed with result, show ResultView
  if (session.status === 'COMPLETED' && completedAttempt) {
    return (
      <div className="space-y-6">
        <ResultView
          result={completedAttempt}
          title={`Study Session #${session.sessionId} Results`}
          subtitle={`${session.durationMinutes}-minute session completed`}
          footerActions={[
            {
              label: 'Start another session',
              href: LEARNING_ROUTES.STUDY_SESSION_START,
              variant: 'primary',
              icon: RotateCcw,
            },
            {
              label: 'Back to Learning Center',
              href: LEARNING_ROUTES.OVERVIEW,
              variant: 'outline',
            },
          ]}
        />
      </div>
    );
  }

  const isSessionEnded = session.status === 'ABANDONED' || (session.status === 'COMPLETED' && !completedAttempt);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Screen reader live announcement */}
      <div className="sr-only" aria-live="polite">
        {liveAnnouncement}
      </div>

      {/* Top Header & Countdown Timer */}
      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-[11px] font-bold text-primary uppercase tracking-wider">
            Study Session #{session.sessionId}
          </span>
          <h1 className="text-2xl font-black text-foreground tracking-tight">
            {session.durationMinutes}-Minute Focused Sprint
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Status:{' '}
            <span
              className={`font-semibold ${
                session.status === 'ACTIVE'
                  ? 'text-primary'
                  : session.status === 'COMPLETED'
                  ? 'text-emerald-600'
                  : 'text-muted-foreground'
              }`}
            >
              {session.status}
            </span>
          </p>
        </div>

        {session.status === 'ACTIVE' && (
          <div
            className={`flex items-center gap-3 px-5 py-3 rounded-2xl border font-bold text-base self-start md:self-auto ${
              isTimeUp || secondsRemaining <= 60
                ? 'bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400 animate-pulse'
                : 'bg-primary/10 border-primary/20 text-primary'
            }`}
          >
            <Clock size={20} />
            <span>{isTimeUp ? "Time's up" : formatTime(secondsRemaining)}</span>
          </div>
        )}
      </div>

      {/* Grace period banner if time is up */}
      {isTimeUp && session.status === 'ACTIVE' && (
        <div className="rounded-2xl border border-amber-500/40 bg-amber-500/10 p-4 text-xs text-amber-800 dark:text-amber-200 flex items-start gap-2.5">
          <AlertCircle size={18} className="text-amber-600 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold">Time's up! </span>
            You can still submit your questions within a few minutes under the server's grace period.
          </div>
        </div>
      )}

      {/* Session Ended Notice */}
      {isSessionEnded && (
        <div className="rounded-2xl border border-border bg-muted/30 p-6 text-center space-y-3">
          <h3 className="text-sm font-bold text-foreground">This session has ended</h3>
          <p className="text-xs text-muted-foreground">
            This study session was marked as {session.status.toLowerCase()}. You can start a new session anytime.
          </p>
          <Link
            to={LEARNING_ROUTES.STUDY_SESSION_START}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold"
          >
            Start New Session
          </Link>
        </div>
      )}

      {/* Study Plan Section */}
      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
        <div className="flex items-center justify-between pb-3 border-b border-border">
          <h2 className="text-sm font-bold text-foreground">Structured Study Plan</h2>
          <span className="text-xs text-muted-foreground">
            {session.items.filter(it => it.completed).length} of {session.items.length} items complete
          </span>
        </div>

        <div className="space-y-3">
          {session.items.map((it, idx) => {
            const isResource = it.itemType === 'RESOURCE_REVIEW';
            const isCompleted = it.completed;

            return (
              <div
                key={it.itemId}
                className={`p-4 rounded-2xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                  isCompleted
                    ? 'border-emerald-500/20 bg-emerald-500/5'
                    : 'border-border bg-card'
                }`}
              >
                <div className="flex items-start gap-3 min-w-0">
                  <span
                    className={`h-7 w-7 rounded-xl text-xs font-bold flex items-center justify-center shrink-0 ${
                      isCompleted ? 'bg-emerald-500 text-white' : 'bg-primary/10 text-primary'
                    }`}
                  >
                    {isCompleted ? <CheckCircle2 size={14} /> : idx + 1}
                  </span>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-foreground">
                        {isResource ? 'Resource Review' : 'Practice Item'}
                      </span>
                      {it.chapterName && (
                        <span className="text-[11px] text-muted-foreground truncate">
                          · {it.chapterName}
                        </span>
                      )}
                    </div>

                    {it.product && (
                      <div className="mt-1 flex items-center gap-2 text-xs">
                        <Link
                          to={it.product.type === 'VIDEO' && it.product.owned ? `/my-videos/${it.product.productId}` : `/products/${it.product.slug}`}
                          target="_blank"
                          rel="noreferrer"
                          className="font-semibold text-primary hover:underline inline-flex items-center gap-1"
                        >
                          {it.product.type === 'VIDEO' ? <Video size={13} /> : <FileText size={13} />}
                          <span>{it.product.name}</span>
                          <ExternalLink size={11} />
                        </Link>
                      </div>
                    )}
                  </div>
                </div>

                {/* Mark as done button for resources */}
                {session.status === 'ACTIVE' && isResource && !isCompleted && (
                  <button
                    type="button"
                    disabled={completingItemId === it.itemId}
                    onClick={() => handleCompleteItem(it.itemId)}
                    className="px-3.5 py-1.5 rounded-xl border border-border text-xs font-semibold text-foreground hover:bg-muted transition shrink-0 self-start sm:self-auto"
                  >
                    {completingItemId === it.itemId ? 'Updating…' : 'Mark as done'}
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* Start Questions Action */}
        {session.status === 'ACTIVE' && session.attemptId && (
          <div className="pt-4 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => setShowAbandonConfirm(true)}
              className="text-xs font-semibold text-rose-600 hover:underline"
            >
              Abandon session
            </button>

            <Link
              to={LEARNING_ROUTES.QUIZ_ATTEMPT(session.attemptId)}
              className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2"
            >
              <Play size={14} />
              <span>Start Question Practice</span>
            </Link>
          </div>
        )}
      </div>

      {/* Abandon Confirm Modal */}
      {showAbandonConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-sm w-full shadow-2xl space-y-4">
            <h3 className="text-sm font-bold text-foreground">Abandon Study Session?</h3>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Are you sure you want to abandon this session? Any unsaved progress will be cancelled.
            </p>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAbandonConfirm(false)}
                className="px-3 py-1.5 rounded-xl border border-border text-xs font-medium hover:bg-muted"
              >
                Keep Going
              </button>
              <button
                type="button"
                disabled={abandoning}
                onClick={handleAbandonSession}
                className="px-4 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition disabled:opacity-50"
              >
                {abandoning ? 'Abandoning…' : 'Yes, Abandon'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
