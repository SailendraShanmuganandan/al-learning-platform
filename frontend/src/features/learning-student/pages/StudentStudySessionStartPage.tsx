import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { createStudySession, getStudySessionsList } from '../api';
import { getPublicSubjects, type SubjectResponse } from '../../../api/subjectApi';
import type { StudySessionResponse } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { mapStudentError } from '../errorMapper';
import {
  Clock,
  Play,
  Loader2,
  History,
  ArrowRight,
} from 'lucide-react';

const DURATION_OPTIONS = [
  { minutes: 10, label: '10 Minutes', approxQuestions: '5 questions', desc: 'Quick warmup or focused review' },
  { minutes: 20, label: '20 Minutes', approxQuestions: '10 questions', desc: 'Standard balanced study sprint' },
  { minutes: 30, label: '30 Minutes', approxQuestions: '15 questions', desc: 'Deep dive into complex topics' },
  { minutes: 60, label: '1 Hour', approxQuestions: '30 questions', desc: 'Full exam simulation session' },
];

export default function StudentStudySessionStartPage() {
  const navigate = useNavigate();

  const [durationMinutes, setDurationMinutes] = useState<number>(20);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [language, setLanguage] = useState<string>('EN');

  const [activeSession, setActiveSession] = useState<StudySessionResponse | null>(null);
  const [historyList, setHistoryList] = useState<StudySessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [subs, sessions] = await Promise.all([
          getPublicSubjects().catch(() => []),
          getStudySessionsList({ page: 0, size: 5 }).catch(() => ({ content: [] })),
        ]);

        if (!cancelled) {
          setSubjects(subs);
          setHistoryList(sessions.content);

          const active = sessions.content.find(s => s.status === 'ACTIVE');
          if (active) setActiveSession(active);
        }
      } catch {
        // silent
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleStart() {
    setStarting(true);
    setError(null);

    try {
      const res = await createStudySession({
        durationMinutes,
        subjectId: selectedSubjectId,
        language,
      });

      navigate(LEARNING_ROUTES.STUDY_SESSION_DETAIL(res.sessionId));
    } catch (err: unknown) {
      const mapped = mapStudentError(err);
      setError(mapped.message);
      setStarting(false);
    }
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading study session options…</span>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Active Session Reminder Banner */}
      {activeSession && (
        <div className="rounded-3xl border border-primary/30 bg-primary/10 p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Clock size={22} className="text-primary" />
            <div>
              <h3 className="text-sm font-bold text-foreground">
                You have an active session in progress
              </h3>
              <p className="text-xs text-muted-foreground">
                {Math.max(0, Math.round(activeSession.remainingSeconds / 60))} minutes remaining
              </p>
            </div>
          </div>

          <Link
            to={LEARNING_ROUTES.STUDY_SESSION_DETAIL(activeSession.sessionId)}
            className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 self-start sm:self-auto"
          >
            <span>Continue Session</span>
            <ArrowRight size={13} />
          </Link>
        </div>
      )}

      {/* Start Config Card */}
      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-primary">
            <Clock size={16} />
            <span>Focused Practice</span>
          </div>
          <h1 className="text-xl md:text-2xl font-black text-foreground tracking-tight">
            Start a Study Session
          </h1>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Timed study sprints pair essential study material revision with instant question practice.
          </p>
        </div>

        {error && (
          <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
            {error}
          </div>
        )}

        {/* Duration Cards */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-foreground block">
            Select Duration:
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {DURATION_OPTIONS.map(opt => {
              const isSelected = durationMinutes === opt.minutes;
              return (
                <button
                  key={opt.minutes}
                  type="button"
                  onClick={() => setDurationMinutes(opt.minutes)}
                  className={`p-4 rounded-2xl border text-left transition ${
                    isSelected
                      ? 'border-primary bg-primary/5 text-foreground ring-1 ring-primary/40'
                      : 'border-border bg-card hover:bg-muted/40 text-muted-foreground'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-foreground">{opt.label}</span>
                    <span className="text-[11px] font-semibold text-primary">{opt.approxQuestions}</span>
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">{opt.desc}</p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Scope selector */}
        {subjects.length > 0 && (
          <div className="space-y-2">
            <label htmlFor="study-subject" className="text-xs font-bold text-foreground block">
              Focus Subject (Optional):
            </label>
            <select
              id="study-subject"
              value={selectedSubjectId ?? ''}
              onChange={e => setSelectedSubjectId(e.target.value ? Number(e.target.value) : null)}
              className="w-full rounded-xl border border-border bg-card px-3 py-2 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Subjects (Balanced Mix)</option>
              {subjects.map(s => (
                <option key={s.subjectId} value={s.subjectId}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Language Selection */}
        <div className="flex items-center gap-2">
          <label htmlFor="study-lang-select" className="text-xs font-semibold text-muted-foreground">
            Language:
          </label>
          <select
            id="study-lang-select"
            value={language}
            onChange={e => setLanguage(e.target.value)}
            className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="EN">English</option>
            <option value="SI">Sinhala</option>
            <option value="TA">Tamil</option>
          </select>
        </div>

        <button
          type="button"
          disabled={starting}
          onClick={handleStart}
          className="w-full px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2 disabled:opacity-40"
        >
          {starting ? (
            <>
              <Loader2 size={14} className="animate-spin" />
              <span>Building Study Plan…</span>
            </>
          ) : (
            <>
              <Play size={14} />
              <span>Start {durationMinutes}-Minute Session</span>
            </>
          )}
        </button>
      </div>

      {/* Recent Sessions History */}
      {historyList.length > 0 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-3">
          <div className="flex items-center gap-2">
            <History size={16} className="text-primary" />
            <h3 className="text-sm font-bold text-foreground">Recent Study Sessions</h3>
          </div>

          <div className="divide-y divide-border">
            {historyList.map(s => (
              <div
                key={s.sessionId}
                className="py-3 flex items-center justify-between text-xs hover:bg-muted/10 transition"
              >
                <div>
                  <p className="font-semibold text-foreground">
                    {s.durationMinutes}m Session #{s.sessionId}
                  </p>
                  <p className="text-[11px] text-muted-foreground">
                    {new Date(s.startedAt).toLocaleDateString()} · {s.status}
                  </p>
                </div>

                <Link
                  to={LEARNING_ROUTES.STUDY_SESSION_DETAIL(s.sessionId)}
                  className="px-3 py-1 rounded-lg border border-border text-xs font-semibold hover:bg-muted transition"
                >
                  View
                </Link>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
