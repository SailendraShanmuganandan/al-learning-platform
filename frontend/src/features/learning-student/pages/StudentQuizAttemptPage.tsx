import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getQuizAttempt, submitQuizAttempt, startQuizAttempt } from '../api';
import type { QuizAttemptResponse } from '../types';
import { QuestionRunner } from '../components/QuestionRunner';
import { ResultView } from '../components/ResultView';
import { LEARNING_ROUTES } from '../routes';
import { getErrorMessage } from '../../learning-admin/errorHelper';
import { Loader2, AlertCircle, RotateCcw, BookOpen, Award, Compass } from 'lucide-react';

export default function StudentQuizAttemptPage() {
  const { attemptId } = useParams<{ attemptId: string }>();
  const aId = Number(attemptId);
  const navigate = useNavigate();
  const { user } = useAuth();

  const [attempt, setAttempt] = useState<QuizAttemptResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!aId) return;
    let cancelled = false;

    async function load() {
      try {
        const data = await getQuizAttempt(aId);
        if (!cancelled) setAttempt(data);
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
  }, [aId]);

  async function handleSubmit(answers: { questionId: number; selectedOptionId: number }[]) {
    if (!aId) return;
    const res = await submitQuizAttempt(aId, { answers });
    setAttempt(res);
  }

  async function handleTryAnotherSet() {
    if (!attempt?.quizId) return;
    try {
      const next = await startQuizAttempt(attempt.quizId, attempt.language);
      navigate(LEARNING_ROUTES.QUIZ_ATTEMPT(next.attemptId));
    } catch (err) {
      alert(getErrorMessage(err));
    }
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading attempt details…</span>
      </div>
    );
  }

  if (error || !attempt) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 rounded-3xl border border-destructive/30 bg-destructive/10 text-center space-y-3">
        <AlertCircle size={28} className="text-destructive mx-auto" />
        <h2 className="text-sm font-bold text-destructive">Attempt Error</h2>
        <p className="text-xs text-muted-foreground">{error || 'Attempt not found.'}</p>
        <button
          type="button"
          onClick={() => navigate(LEARNING_ROUTES.OVERVIEW)}
          className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold"
        >
          Go to Learning Center
        </button>
      </div>
    );
  }

  const storageKey = `al_attempt_draft_${user?.userId || 'guest'}_${attempt.attemptType}_${attempt.attemptId}`;

  // If still IN_PROGRESS, render QuestionRunner
  if (attempt.status === 'IN_PROGRESS') {
    let title = 'Chapter Quiz';
    if (attempt.attemptType === 'MISTAKE_PRACTICE') title = 'Mistake Practice Session';
    if (attempt.attemptType === 'STUDY_SESSION') title = 'Focused Study Questions';

    return (
      <QuestionRunner
        questions={attempt.questions}
        onSubmit={handleSubmit}
        storageKey={storageKey}
        title={title}
        subtitle={`Attempt #${attempt.attemptId} · ${attempt.questions.length} questions`}
      />
    );
  }

  // If COMPLETED, render ResultView with contextual footer actions
  const footerActions = [];

  if (attempt.quizId) {
    footerActions.push({
      label: 'Try another sample set',
      onClick: handleTryAnotherSet,
      variant: 'primary' as const,
      icon: RotateCcw,
    });
  }

  footerActions.push({
    label: 'Practise my mistakes',
    href: LEARNING_ROUTES.MISTAKES,
    variant: 'secondary' as const,
    icon: Award,
  });

  if (attempt.chapterId) {
    footerActions.push({
      label: 'Back to chapter',
      href: `/chapters/${attempt.chapterId}`,
      variant: 'outline' as const,
      icon: BookOpen,
    });
  } else {
    footerActions.push({
      label: 'Learning Center',
      href: LEARNING_ROUTES.OVERVIEW,
      variant: 'outline' as const,
      icon: Compass,
    });
  }

  return (
    <ResultView
      result={attempt}
      title={attempt.attemptType === 'MISTAKE_PRACTICE' ? 'Mistake Practice Results' : 'Quiz Results'}
      subtitle={`Attempt #${attempt.attemptId} · Completed`}
      footerActions={footerActions}
    />
  );
}

