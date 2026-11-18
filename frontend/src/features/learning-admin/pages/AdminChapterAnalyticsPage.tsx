import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getAdminChapterAnalytics } from '../api';
import type { AdminChapterAnalyticsDetailResponse } from '../types';
import { getErrorMessage } from '../errorHelper';
import { StatusBadge } from '../components/StatusBadge';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { FeedbackAlert } from '../components/FeedbackAlert';
import {
  ArrowLeft,
  FolderTree,
  AlertTriangle,
  ExternalLink,
  Loader2,
  FileQuestion,
} from 'lucide-react';

export default function AdminChapterAnalyticsPage() {
  const { chapterId } = useParams<{ chapterId: string }>();
  const chId = Number(chapterId);
  const navigate = useNavigate();

  const [detail, setDetail] = useState<AdminChapterAnalyticsDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!chId) return;
    let cancelled = false;
    async function load() {
      try {
        const data = await getAdminChapterAnalytics(chId);
        if (!cancelled) setDetail(data);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [chId]);

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading chapter analytics…</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate('/admin/learning-analytics')}
            className="p-2 rounded-xl border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="Back to Overview"
          >
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className="text-xl font-black text-foreground tracking-tight">
              {detail ? `${detail.chapterName} Analytics` : 'Chapter Analytics'}
            </h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              Subject: <strong className="text-foreground">{detail?.subjectName || '—'}</strong> &bull; Chapter ID #{chId}
            </p>
          </div>
        </div>

        <Link
          to={`/admin/question-bank?chapterId=${chId}`}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-border text-xs font-semibold text-primary hover:bg-primary/5 transition shadow-xs"
        >
          <FileQuestion size={14} />
          <span>View Question Bank</span>
        </Link>
      </div>

      {error && <FeedbackAlert type="error" message={error} onClose={() => setError(null)} />}

      {/* Topics Breakdown Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center gap-2">
          <FolderTree size={16} className="text-primary" />
          <div>
            <h3 className="text-sm font-bold text-foreground">Topic Performance</h3>
            <p className="text-[11px] text-muted-foreground">Performance distribution across chapter topics.</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Topic Name</th>
                <th className="py-3 px-4 text-center">Students With Data</th>
                <th className="py-3 px-4 text-center">Weak Students (&lt; 50%)</th>
                <th className="py-3 px-4 text-center">Average Accuracy</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {!detail?.topics || detail.topics.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-muted-foreground">
                    No active topics defined for this chapter yet.
                  </td>
                </tr>
              ) : (
                detail.topics.map(t => (
                  <tr key={t.topicId} className="hover:bg-muted/30 transition">
                    <td className="py-3.5 px-4 font-bold text-foreground">
                      {t.topicName}
                    </td>
                    <td className="py-3.5 px-4 text-center font-medium text-foreground">
                      {t.studentsWithData}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <span className={`font-semibold ${t.weakCount > 0 ? 'text-rose-600 dark:text-rose-400' : 'text-muted-foreground'}`}>
                        {t.weakCount}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                      {t.averageAccuracy !== null ? `${t.averageAccuracy}%` : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Hardest Questions Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-rose-600 dark:text-rose-400" />
            <div>
              <h3 className="text-sm font-bold text-foreground">Hardest Questions in Chapter</h3>
              <p className="text-[11px] text-muted-foreground">Questions with lowest correct rate (minimum 3 student attempts).</p>
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Question</th>
                <th className="py-3 px-4">Topic</th>
                <th className="py-3 px-4">Difficulty</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-center">Student Answers</th>
                <th className="py-3 px-4 text-center">Correct Rate</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {!detail?.hardestQuestions || detail.hardestQuestions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-muted-foreground">
                    No questions have accumulated enough student attempts yet.
                  </td>
                </tr>
              ) : (
                detail.hardestQuestions.map(q => {
                  const isCriticallyLow = q.correctRate < 20;

                  return (
                    <tr key={q.questionId} className="hover:bg-muted/30 transition">
                      <td className="py-3.5 px-4 max-w-sm">
                        <Link
                          to={`/admin/question-bank/${q.questionId}`}
                          className="font-medium text-foreground hover:text-primary transition line-clamp-2 leading-relaxed"
                          title={q.questionText}
                        >
                          {q.questionText}
                        </Link>
                        <span className="text-[10px] text-muted-foreground font-mono">
                          #{q.questionId}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-muted-foreground font-medium">
                        {q.topicName || <span className="opacity-40">None</span>}
                      </td>

                      <td className="py-3.5 px-4">
                        <DifficultyBadge difficulty={q.difficulty} />
                      </td>

                      <td className="py-3.5 px-4">
                        <StatusBadge status={q.status} />
                      </td>

                      <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                        {q.answerCount}
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`font-black text-xs px-2.5 py-0.5 rounded-full ${
                            isCriticallyLow
                              ? 'bg-rose-500/10 text-rose-700 dark:text-rose-300 border border-rose-500/20'
                              : 'text-foreground'
                          }`}
                        >
                          {q.correctRate}%
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <Link
                          to={`/admin/question-bank/${q.questionId}`}
                          className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition inline-flex items-center gap-1"
                          title="Edit Question"
                        >
                          <span>Edit</span>
                          <ExternalLink size={12} />
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
