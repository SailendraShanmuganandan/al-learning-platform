import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import {
  getDiagnosticAssessment,
  submitDiagnosticAssessment,
  getNextRecommendation,
} from '../api';
import type { DiagnosticAssessmentResponse, RecommendationResponse } from '../types';
import { QuestionRunner } from '../components/QuestionRunner';
import { ResultView } from '../components/ResultView';
import { RecommendationCard } from '../components/RecommendationCard';
import { LEARNING_ROUTES } from '../routes';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import { Loader2, AlertCircle, Compass, History, Sparkles } from 'lucide-react';

export default function StudentAssessmentPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const id = Number(assessmentId);
  const navigate = useNavigate();
  const { user } = useAuth();

  const [assessment, setAssessment] = useState<DiagnosticAssessmentResponse | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    async function load() {
      try {
        const data = await getDiagnosticAssessment(id);
        if (!cancelled) {
          setAssessment(data);
          if (data.status === 'COMPLETED' && data.assessmentType === 'INITIAL_DIAGNOSTIC') {
            getNextRecommendation()
              .then(rec => {
                if (!cancelled) setRecommendation(rec);
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

    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleSubmit(answers: { questionId: number; selectedOptionId: number }[]) {
    if (!id) return;
    const res = await submitDiagnosticAssessment(id, { answers });
    setAssessment(res);

    if (res.assessmentType === 'INITIAL_DIAGNOSTIC') {
      try {
        const rec = await getNextRecommendation();
        setRecommendation(rec);
      } catch {
        // fallback
      }
    }
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading assessment…</span>
      </div>
    );
  }

  if (error || !assessment) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center space-y-3">
        <AlertCircle size={28} className="text-destructive mx-auto" />
        <h2 className="text-sm font-bold text-destructive">Assessment Error</h2>
        <p className="text-xs text-muted-foreground">{error || 'Assessment not found.'}</p>
        <button
          type="button"
          onClick={() => navigate(LEARNING_ROUTES.ASSESSMENTS_HISTORY)}
          className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold"
        >
          View Assessment History
        </button>
      </div>
    );
  }

  const storageKey = `al_assessment_draft_${user?.userId || 'guest'}_${assessment.assessmentType}_${assessment.assessmentId}`;

  // If IN_PROGRESS, render QuestionRunner
  if (assessment.status === 'IN_PROGRESS') {
    const title =
      assessment.assessmentType === 'INITIAL_DIAGNOSTIC'
        ? 'Diagnostic Assessment'
        : 'Overall Syllabus Quiz';

    return (
      <QuestionRunner
        questions={assessment.questions}
        onSubmit={handleSubmit}
        storageKey={storageKey}
        title={title}
        subtitle={`Assessment #${assessment.assessmentId} · ${assessment.questions.length} questions`}
      />
    );
  }

  // If COMPLETED, render ResultView with chapter breakdown
  const footerActions = [
    {
      label: 'Learning Center Overview',
      href: LEARNING_ROUTES.OVERVIEW,
      variant: 'primary' as const,
      icon: Compass,
    },
    {
      label: 'View Syllabus Progress',
      href: LEARNING_ROUTES.PROGRESS,
      variant: 'secondary' as const,
      icon: Sparkles,
    },
    {
      label: 'Assessment History',
      href: LEARNING_ROUTES.ASSESSMENTS_HISTORY,
      variant: 'outline' as const,
      icon: History,
    },
  ];

  const title =
    assessment.assessmentType === 'INITIAL_DIAGNOSTIC'
      ? 'Diagnostic Assessment Results'
      : 'Overall Quiz Results';

  return (
    <div className="space-y-6">
      <ResultView
        result={assessment}
        title={title}
        subtitle={`Assessment #${assessment.assessmentId} · Completed`}
        footerActions={footerActions}
      />

      {/* Starting recommendation shown after initial diagnostic */}
      {recommendation && (
        <div className="max-w-4xl mx-auto pt-2">
          <RecommendationCard recommendation={recommendation} />
        </div>
      )}
    </div>
  );
}

