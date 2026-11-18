import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getStudentLearningOverview, getAssessmentOnboardingStatus } from '../api';
import type { StudentLearningOverviewResponse } from '../types';
import { RecommendationCard } from '../components/RecommendationCard';
import { OnboardingOfferModal } from '../components/OnboardingOfferModal';
import { ProgressBar } from '../components/ScoreRing';
import { LEARNING_ROUTES } from '../routes';
import {
  Sparkles,
  BarChart3,
  Award,
  Clock,
  Play,
  ArrowRight,
  Loader2,
  AlertTriangle,
  ChevronRight,
} from 'lucide-react';

export default function StudentLearningOverviewPage() {
  const { user } = useAuth();

  const [data, setData] = useState<StudentLearningOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Onboarding offer state
  const [showOnboardingModal, setShowOnboardingModal] = useState(false);
  const skipStorageKey = `al_onboarding_skipped_${user?.userId || 'student'}`;

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [overview, onboarding] = await Promise.all([
          getStudentLearningOverview(),
          getAssessmentOnboardingStatus().catch(() => null),
        ]);

        if (!cancelled) {
          setData(overview);

          const isSkipped = localStorage.getItem(skipStorageKey) === 'true';
          if ((overview.offerInitialDiagnostic || onboarding?.offerInitialDiagnostic) && !isSkipped) {
            setShowOnboardingModal(true);
          }
        }
      } catch (err: unknown) {
        if (!cancelled) setError((err as { message?: string })?.message || 'Failed to load learning overview.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [skipStorageKey]);

  function handleSkipOnboarding() {
    try {
      localStorage.setItem(skipStorageKey, 'true');
    } catch {
      // ignore
    }
    setShowOnboardingModal(false);
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading your personalised learning dashboard…</span>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center text-xs text-destructive">
        {error || 'Failed to load learning overview.'}
      </div>
    );
  }


  return (
    <div className="space-y-6">
      {/* Onboarding Dialog if triggered */}
      <OnboardingOfferModal
        isOpen={showOnboardingModal}
        onClose={() => setShowOnboardingModal(false)}
        onSkip={handleSkipOnboarding}
      />

      {/* Top Banner / Greeting */}
      <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 md:p-8 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-1">
          <span className="text-[11px] font-bold uppercase tracking-widest text-primary">
            Student Learning Center
          </span>
          <h1 className="text-2xl md:text-3xl font-black text-foreground tracking-tight">
            Keep up the momentum, {user?.fullName?.split(' ')[0]}!
          </h1>
          <p className="text-xs text-muted-foreground max-w-xl leading-relaxed">
            Personalised quizzes, targeted mistake drills, and diagnostic assessments built specifically for A/L ICT excellence.
          </p>
        </div>

        {/* Quick Action Buttons */}
        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <Link
            to={LEARNING_ROUTES.OVERALL_QUIZ_START}
            className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5"
          >
            <Sparkles size={14} />
            <span>Overall Quiz</span>
          </Link>
          <Link
            to={LEARNING_ROUTES.STUDY_SESSION_START}
            className="px-4 py-2 rounded-xl border border-border bg-card text-foreground text-xs font-semibold hover:bg-muted transition flex items-center gap-1.5"
          >
            <Clock size={14} />
            <span>Study Session</span>
          </Link>
        </div>
      </div>

      {/* Active Study Session Banner if present */}
      {data.activeStudySession && (
        <div className="rounded-3xl border border-primary/30 bg-primary/10 p-5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-primary-foreground font-bold">
              <Clock size={20} />
            </div>
            <div>
              <span className="text-[10px] font-extrabold uppercase tracking-wide text-primary">
                Active Study Session
              </span>
              <h3 className="text-sm font-bold text-foreground">
                You have a focused study session in progress
              </h3>
              <p className="text-xs text-muted-foreground">
                {Math.max(0, Math.round(data.activeStudySession.remainingSeconds / 60))} minutes remaining ·{' '}
                {data.activeStudySession.questionCount} questions
              </p>
            </div>
          </div>

          <Link
            to={LEARNING_ROUTES.STUDY_SESSION_DETAIL(data.activeStudySession.sessionId)}
            className="px-5 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 self-start sm:self-auto shrink-0"
          >
            <span>Continue Study Session</span>
            <ArrowRight size={14} />
          </Link>
        </div>
      )}

      {/* Primary Recommendation Card */}
      {data.recommendation && (
        <RecommendationCard recommendation={data.recommendation} />
      )}

      {/* Two Column Grid: Progress Snapshot & Mistakes Notebook */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Progress Snapshot */}
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <BarChart3 size={18} className="text-primary" />
                <h3 className="text-sm font-bold text-foreground">Syllabus Progress</h3>
              </div>
              <Link
                to={LEARNING_ROUTES.PROGRESS}
                className="text-xs font-semibold text-primary hover:underline flex items-center gap-1"
              >
                <span>Full map</span>
                <ChevronRight size={13} />
              </Link>
            </div>

            <p className="text-xs text-muted-foreground leading-relaxed">
              Track how much of the syllabus you have covered and tested through chapter assessments.
            </p>
          </div>

          <div className="space-y-3 pt-2">
            <div className="flex justify-between text-xs font-semibold text-foreground">
              <span>Overall Syllabus Coverage</span>
              <span>{Math.round(data.progress?.coveragePercent ?? 0)}%</span>
            </div>
            <ProgressBar value={data.progress?.coveragePercent ?? 0} height={10} showLabel={false} />

            <div className="grid grid-cols-3 gap-2 pt-2 text-center">
              <div className="p-2.5 rounded-2xl bg-muted/40 border border-border/40">
                <span className="text-[10px] text-muted-foreground font-semibold">Strong</span>
                <p className="text-base font-black text-emerald-600 dark:text-emerald-400 mt-0.5">
                  {data.progress?.strongCount ?? 0}
                </p>
              </div>
              <div className="p-2.5 rounded-2xl bg-muted/40 border border-border/40">
                <span className="text-[10px] text-muted-foreground font-semibold">Developing</span>
                <p className="text-base font-black text-sky-600 dark:text-sky-400 mt-0.5">
                  {data.progress?.developingCount ?? 0}
                </p>
              </div>
              <div className="p-2.5 rounded-2xl bg-muted/40 border border-border/40">
                <span className="text-[10px] text-muted-foreground font-semibold">Needs Work</span>
                <p className="text-base font-black text-amber-600 dark:text-amber-400 mt-0.5">
                  {data.progress?.weakCount ?? 0}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Mistake Notebook Snapshot */}
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Award size={18} className="text-primary" />
                <h3 className="text-sm font-bold text-foreground">Mistakes Notebook</h3>
              </div>
              <Link
                to={LEARNING_ROUTES.MISTAKES}
                className="text-xs font-semibold text-primary hover:underline flex items-center gap-1"
              >
                <span>View all</span>
                <ChevronRight size={13} />
              </Link>
            </div>

            <p className="text-xs text-muted-foreground leading-relaxed">
              Every incorrect answer enters your notebook. Master them with 3 consecutive correct answers.
            </p>
          </div>

          <div className="flex items-center justify-between p-4 rounded-2xl border border-border bg-muted/20">
            <div>
              <span className="text-2xl font-black text-foreground">
                {data.mistakes.practiceableCount}
              </span>
              <p className="text-xs text-muted-foreground">Ready for practice</p>
            </div>

            <Link
              to={LEARNING_ROUTES.MISTAKES}
              className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5"
            >
              <Play size={13} />
              <span>Practise Now</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Weak Areas (WEAK only) */}
      {data.weakAreas && data.weakAreas.length > 0 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <AlertTriangle size={18} className="text-amber-500" />
            <h3 className="text-sm font-bold text-foreground">
              Priority Focus Areas ({data.weakAreas.length})
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {data.weakAreas.map(w => (
              <div
                key={w.chapterId}
                className="p-4 rounded-2xl border border-amber-500/20 bg-amber-500/5 flex flex-col justify-between space-y-3"
              >
                <div>
                  <span className="text-[10px] font-bold text-amber-700 dark:text-amber-400 uppercase">
                    {w.subjectName}
                  </span>
                  <h4 className="text-xs font-bold text-foreground truncate mt-0.5">
                    {w.chapterName}
                  </h4>
                  {w.weakTopics && w.weakTopics.length > 0 && (
                    <div className="mt-1 flex flex-wrap gap-1">
                      {w.weakTopics.map((t, idx) => (
                        <span key={idx} className="text-[10px] text-muted-foreground bg-card px-2 py-0.5 rounded-md border border-border">
                          {t}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-amber-500/10">
                  <span className="text-xs font-black text-amber-600 dark:text-amber-400">
                    {Math.round(w.accuracy * 100)}% accuracy
                  </span>

                  {w.quizId && (
                    <Link
                      to={LEARNING_ROUTES.QUIZ_START(w.quizId)}
                      className="px-3 py-1 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-[11px] font-semibold transition shadow-2xs"
                    >
                      Practise
                    </Link>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Activity */}
      {data.recentActivity && data.recentActivity.length > 0 && (
        <div className="rounded-3xl border border-border bg-card p-6 shadow-xs space-y-3">
          <h3 className="text-sm font-bold text-foreground">Recent Attempts</h3>
          <div className="divide-y divide-border">
            {data.recentActivity.map(act => (
              <div
                key={act.attemptId}
                className="py-3 flex items-center justify-between gap-3 text-xs hover:bg-muted/10 transition"
              >
                <div>
                  <p className="font-semibold text-foreground">{act.title}</p>
                  <p className="text-[11px] text-muted-foreground">
                    {new Date(act.completedAt).toLocaleDateString()} · Score: {act.score} / {act.totalMarks}
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <span className="font-extrabold text-primary">
                    {Math.round(act.percentage)}%
                  </span>
                  <Link
                    to={LEARNING_ROUTES.QUIZ_ATTEMPT(act.attemptId)}
                    className="p-1.5 rounded-lg text-muted-foreground hover:bg-muted transition"
                    title="View attempt results"
                  >
                    <ChevronRight size={15} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Advice slot (renders nothing when advice is null as specified) */}
      {data.advice && (
        <div className="rounded-2xl border border-primary/20 bg-primary/5 p-4 text-xs text-muted-foreground">
          {data.advice}
        </div>
      )}
    </div>
  );
}
