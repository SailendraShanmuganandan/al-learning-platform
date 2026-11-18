import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAdminStudentLearning } from '../api';
import type { AdminStudentLearningDetailResponse, PerformanceTrend } from '../types';
import { getErrorMessage } from '../errorHelper';
import { LevelBadge } from '../components/LevelBadge';
import { FeedbackAlert } from '../components/FeedbackAlert';
import {
  ArrowLeft,
  User,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Minus,
  Loader2,
} from 'lucide-react';

function TrendIndicator({ trend }: { trend: PerformanceTrend | null }) {
  if (!trend) return null;

  if (trend === 'IMPROVING') {
    return (
      <span
        title="Performance is improving"
        className="inline-flex items-center gap-1 font-semibold text-emerald-600 dark:text-emerald-400 text-xs"
      >
        <TrendingUp size={14} />
        <span>Improving</span>
      </span>
    );
  }

  if (trend === 'DECLINING') {
    return (
      <span
        title="Performance is declining"
        className="inline-flex items-center gap-1 font-semibold text-rose-600 dark:text-rose-400 text-xs"
      >
        <TrendingDown size={14} />
        <span>Declining</span>
      </span>
    );
  }

  return (
    <span
      title="Performance is steady"
      className="inline-flex items-center gap-1 font-medium text-blue-600 dark:text-blue-400 text-xs"
    >
      <Minus size={14} />
      <span>Steady</span>
    </span>
  );
}

export default function AdminStudentLearningPage() {
  const { userId } = useParams<{ userId: string }>();
  const uId = Number(userId);
  const navigate = useNavigate();

  const [student, setStudent] = useState<AdminStudentLearningDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!uId) return;
    let cancelled = false;
    async function load() {
      try {
        const data = await getAdminStudentLearning(uId);
        if (!cancelled) setStudent(data);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [uId]);

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading student learning records…</span>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
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
            <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
              <User className="text-primary" size={22} />
              <span>{student?.fullName || `Student #${uId}`}</span>
            </h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              {student?.email} &bull; User ID: #{uId} &bull;{' '}
              {student?.isEmailVerified ? (
                <span className="text-emerald-600 dark:text-emerald-400 font-semibold">Verified Student</span>
              ) : (
                <span className="text-amber-600 dark:text-amber-400 font-semibold">Unverified Email</span>
              )}
            </p>
          </div>
        </div>
      </div>

      {error && <FeedbackAlert type="error" message={error} onClose={() => setError(null)} />}

      {/* Summary Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground">Total Answered Questions</span>
          <div className="text-2xl font-black text-foreground mt-1">
            {student?.totalAnswerCount ?? 0}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Across all assessment types</p>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground">Active Mistakes in Bank</span>
          <div className="text-2xl font-black text-amber-600 dark:text-amber-400 mt-1">
            {student?.activeMistakeCount ?? 0}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">
            {student?.resolvedMistakeCount ?? 0} mastered &amp; resolved
          </p>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground">Identified Weak Areas</span>
          <div className="text-2xl font-black text-rose-600 dark:text-rose-400 mt-1">
            {student?.weakAreas?.length ?? 0}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Chapters / topics &lt; 50% accuracy</p>
        </div>
      </div>

      {/* Chapter Performance Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-border">
          <h3 className="text-sm font-bold text-foreground">Syllabus Chapter Mastery</h3>
          <p className="text-[11px] text-muted-foreground">Performance levels and accuracy across syllabus chapters.</p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Subject</th>
                <th className="py-3 px-4">Chapter</th>
                <th className="py-3 px-4">Level</th>
                <th className="py-3 px-4">Trend</th>
                <th className="py-3 px-4 text-center">Answers</th>
                <th className="py-3 px-4 text-center">Accuracy</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {!student?.chapterPerformances || student.chapterPerformances.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-muted-foreground">
                    No chapter attempts recorded for this student yet.
                  </td>
                </tr>
              ) : (
                student.chapterPerformances.map(cp => (
                  <tr key={cp.chapterId} className="hover:bg-muted/30 transition">
                    <td className="py-3.5 px-4 text-muted-foreground font-medium">
                      {cp.subjectName}
                    </td>
                    <td className="py-3.5 px-4 font-bold text-foreground">
                      {cp.chapterName}
                    </td>
                    <td className="py-3.5 px-4">
                      <LevelBadge level={cp.level} />
                    </td>
                    <td className="py-3.5 px-4">
                      <TrendIndicator trend={cp.trend} />
                    </td>
                    <td className="py-3.5 px-4 text-center font-medium text-foreground">
                      {cp.answerCount}
                    </td>
                    <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                      {cp.accuracy !== null ? `${cp.accuracy}%` : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Weak Areas Breakdown */}
      {student?.weakAreas && student.weakAreas.length > 0 && (
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-5 shadow-sm space-y-3">
          <div className="flex items-center gap-2">
            <AlertTriangle size={18} className="text-rose-600 dark:text-rose-400" />
            <h3 className="text-sm font-bold text-foreground">Specific Weak Focus Areas</h3>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {student.weakAreas.map((item, idx) => (
              <div
                key={idx}
                className="rounded-xl border border-border bg-card p-3.5 text-xs space-y-1 shadow-2xs"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-foreground">{item.name}</span>
                  <span className="px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-700 dark:text-rose-300 font-bold text-[11px]">
                    {item.accuracy}%
                  </span>
                </div>
                <div className="text-[11px] text-muted-foreground">
                  {item.chapterName} &bull; {item.answerCount} answers
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Activities */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-border">
          <h3 className="text-sm font-bold text-foreground">Recent Learning Activities</h3>
          <p className="text-[11px] text-muted-foreground">Latest quiz and assessment submissions.</p>
        </div>

        <div className="divide-y divide-border">
          {!student?.recentActivities || student.recentActivities.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-xs">
              No recent activity submissions.
            </div>
          ) : (
            student.recentActivities.map((act, idx) => (
              <div key={idx} className="p-4 flex items-center justify-between gap-4 text-xs">
                <div className="space-y-0.5">
                  <div className="font-bold text-foreground">{act.title}</div>
                  <div className="text-[11px] text-muted-foreground">
                    {act.chapterName} &bull;{' '}
                    {new Date(act.completedAt).toLocaleString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                </div>

                <div className="text-right">
                  {act.accuracy !== null ? (
                    <span className="font-bold text-foreground text-sm">
                      {act.accuracy}%
                    </span>
                  ) : act.score !== null ? (
                    <span className="font-bold text-foreground text-sm">
                      {act.score} / {act.totalMarks}
                    </span>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
