import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { createDiagnosticAssessment } from '../api';
import { LEARNING_ROUTES } from '../routes';
import { UnverifiedEmailPrompt } from '../components/UnverifiedEmailPrompt';
import { mapStudentError } from '../errorMapper';
import { Sparkles, Play, Loader2, Languages, ArrowLeft } from 'lucide-react';

export default function StudentOverallQuizPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [language, setLanguage] = useState<string>('EN');
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  async function handleStart() {
    setStarting(true);
    setError(null);
    try {
      const res = await createDiagnosticAssessment({
        assessmentType: 'OVERALL_QUIZ',
        scopeType: 'ALL_SUBJECTS',
        language,
      });
      navigate(LEARNING_ROUTES.ASSESSMENT_RUNNER(res.assessmentId));
    } catch (err: unknown) {
      const mapped = mapStudentError(err);
      setError(mapped.message);
      setStarting(false);
    }
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground transition"
      >
        <ArrowLeft size={14} />
        <span>Back</span>
      </button>

      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Sparkles size={24} />
          </div>
          <div>
            <span className="text-[11px] font-bold uppercase tracking-wider text-primary">
              Comprehensive Assessment
            </span>
            <h1 className="text-xl font-black text-foreground tracking-tight">
              Overall Syllabus Quiz
            </h1>
          </div>
        </div>

        <p className="text-xs text-muted-foreground leading-relaxed">
          Test your overall readiness with a randomised set of questions sampled across all topics and subjects in the A/L ICT syllabus.
          This quiz is completely repeatable and dynamically adjusts to give you a fresh sample every attempt.
        </p>

        {!isVerified && (
          <UnverifiedEmailPrompt message="You must verify your email before starting official assessments." />
        )}

        {error && (
          <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
            {error}
          </div>
        )}

        <div className="p-4 rounded-2xl border border-border bg-muted/20 space-y-2 text-xs">
          <div className="flex justify-between py-1 border-b border-border/40">
            <span className="text-muted-foreground">Scope</span>
            <span className="font-semibold text-foreground">All Subjects &amp; Chapters</span>
          </div>
          <div className="flex justify-between py-1 border-b border-border/40">
            <span className="text-muted-foreground">Format</span>
            <span className="font-semibold text-foreground">Multiple Choice Questions</span>
          </div>
          <div className="flex justify-between py-1">
            <span className="text-muted-foreground">Grading</span>
            <span className="font-semibold text-foreground">Instant breakdown &amp; recommendations</span>
          </div>
        </div>

        {/* Language Selection */}
        <div className="flex items-center gap-2">
          <label htmlFor="overall-lang-select" className="text-xs font-semibold text-muted-foreground flex items-center gap-1">
            <Languages size={13} />
            <span>Select Language:</span>
          </label>
          <select
            id="overall-lang-select"
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
          disabled={!isVerified || starting}
          onClick={handleStart}
          className="w-full px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {starting ? (
            <>
              <Loader2 size={14} className="animate-spin" />
              <span>Generating Quiz Sample…</span>
            </>
          ) : (
            <>
              <Play size={14} />
              <span>Start Overall Quiz</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}
