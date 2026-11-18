import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getAdminLearningOverview } from '../api';
import { getAdminSubjects, type SubjectResponse } from '../../../api/subjectApi';
import type { AdminLearningOverviewResponse, AdminChapterPerformanceSummaryDto } from '../types';
import { getErrorMessage } from '../errorHelper';
import { FeedbackAlert } from '../components/FeedbackAlert';
import {
  BarChart3,
  Users,
  CheckCircle2,
  Search,
  ChevronRight,
  Loader2,
  Info,
  Layers,
} from 'lucide-react';

export default function AdminLearningOverviewPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const subjectIdParam = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;

  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [overview, setOverview] = useState<AdminLearningOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  // Student navigation search box
  const [studentInput, setStudentInput] = useState('');

  // Load subjects
  useEffect(() => {
    let cancelled = false;
    async function loadSubs() {
      try {
        const subs = await getAdminSubjects();
        if (!cancelled) setSubjects(subs);
      } catch {
        // fallback
      }
    }
    loadSubs();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await getAdminLearningOverview(subjectIdParam);
        if (!cancelled) setOverview(data);
      } catch (err) {
        if (!cancelled) setPageError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [subjectIdParam]);

  function handleSubjectChange(idStr: string) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (idStr) next.set('subjectId', idStr);
      else next.delete('subjectId');
      return next;
    });
  }

  function handleStudentSearch(e: React.FormEvent) {
    e.preventDefault();
    const cleanId = studentInput.trim().replace(/\D/g, '');
    if (cleanId) {
      navigate(`/admin/learning-analytics/students/${cleanId}`);
    }
  }

  // Chapters sorted by weak percentage descending
  const sortedChapters = overview?.chapters
    ? [...overview.chapters].sort((a, b) => b.weakPercentage - a.weakPercentage)
    : [];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <BarChart3 className="text-primary" size={24} />
            <span>Learning Analytics</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            System-wide syllabus mastery metrics, student performance distribution, and chapter difficulty breakdown.
          </p>
        </div>

        {/* Student Search Box */}
        <form onSubmit={handleStudentSearch} className="flex items-center gap-2 w-full sm:max-w-xs">
          <div className="relative flex-1">
            <Search size={14} className="absolute left-3 top-2.5 text-muted-foreground" />
            <input
              type="text"
              placeholder="Jump to student (User ID)…"
              value={studentInput}
              onChange={e => setStudentInput(e.target.value)}
              className="w-full rounded-xl border border-border bg-card pl-8 pr-3 py-1.5 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            />
          </div>
          <button
            type="submit"
            disabled={!studentInput.trim()}
            className="px-3 py-1.5 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs disabled:opacity-50 shrink-0"
          >
            Go
          </button>
        </form>
      </div>

      {pageError && (
        <FeedbackAlert type="error" message={pageError} onClose={() => setPageError(null)} />
      )}

      {/* Subject Filter Bar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <label htmlFor="overview-subject" className="text-xs font-semibold text-muted-foreground">
            Filter by Subject:
          </label>
          <select
            id="overview-subject"
            value={subjectIdParam ?? ''}
            onChange={e => handleSubjectChange(e.target.value)}
            className="rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground shadow-xs focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="">All Subjects</option>
            {subjects.map(s => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-muted-foreground">Students with Evaluated Data</span>
            <div className="text-2xl font-black text-foreground mt-1">
              {loading ? '…' : (overview?.totalStudentsWithData ?? 0)}
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">Students who completed at least 1 quiz or assessment</p>
          </div>
          <div className="h-10 w-10 rounded-2xl bg-primary/10 text-primary flex items-center justify-center">
            <Users size={20} />
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-muted-foreground">Total Graded Answers</span>
            <div className="text-2xl font-black text-foreground mt-1">
              {loading ? '…' : (overview?.totalGradedAnswers ?? 0)}
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">Across all completed attempts and diagnostic assessments</p>
          </div>
          <div className="h-10 w-10 rounded-2xl bg-emerald-500/10 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 size={20} />
          </div>
        </div>
      </div>

      {/* Note about 5 answers and insufficient data */}
      <div className="rounded-2xl border border-border/80 bg-muted/30 p-4 text-xs text-muted-foreground flex items-start gap-2.5 leading-relaxed">
        <Info size={16} className="text-primary shrink-0 mt-0.5" />
        <div>
          Performance levels (Weak &lt; 50%, Developing 50–79%, Strong &ge; 80%) require at least <strong className="text-foreground">5 graded answers</strong> per student per chapter. Students with fewer than 5 answers are classified as <span className="font-semibold text-zinc-600 dark:text-zinc-400">Not enough data</span> and are excluded from the Weak % calculation to prevent skewing.
        </div>
      </div>

      {/* Chapters Performance Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-foreground">Chapter Mastery Breakdown</h3>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Sorted by Weak % descending to highlight topics requiring curriculum reinforcement.
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-4">Subject</th>
                <th className="py-3 px-4">Chapter</th>
                <th className="py-3 px-4 text-center">Students</th>
                <th className="py-3 px-4 min-w-[200px]">Level Distribution (Weak / Dev / Strong)</th>
                <th className="py-3 px-4 text-center">Weak %</th>
                <th className="py-3 px-4 text-center">Avg Accuracy</th>
                <th className="py-3 px-4 text-center">Answers</th>
                <th className="py-3 px-4 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-muted-foreground">
                    <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
                    <span>Analyzing performance…</span>
                  </td>
                </tr>
              ) : sortedChapters.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-muted-foreground">
                    <Layers size={28} className="mx-auto mb-2 opacity-40" />
                    <p className="font-semibold text-foreground">No chapter performance data</p>
                    <p className="text-xs mt-1">Student attempts will populate chapter analytics automatically.</p>
                  </td>
                </tr>
              ) : (
                sortedChapters.map((ch: AdminChapterPerformanceSummaryDto) => {
                  const evaluatedTotal = ch.weakCount + ch.developingCount + ch.strongCount;
                  const weakPctOfBar = evaluatedTotal > 0 ? (ch.weakCount / evaluatedTotal) * 100 : 0;
                  const devPctOfBar = evaluatedTotal > 0 ? (ch.developingCount / evaluatedTotal) * 100 : 0;
                  const strongPctOfBar = evaluatedTotal > 0 ? (ch.strongCount / evaluatedTotal) * 100 : 0;

                  return (
                    <tr
                      key={ch.chapterId}
                      onClick={() => navigate(`/admin/learning-analytics/chapters/${ch.chapterId}`)}
                      className="hover:bg-muted/40 transition cursor-pointer group"
                    >
                      <td className="py-3.5 px-4 font-medium text-muted-foreground">
                        {ch.subjectName}
                      </td>
                      <td className="py-3.5 px-4 font-bold text-foreground group-hover:text-primary transition">
                        {ch.chapterName}
                      </td>
                      <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                        {ch.studentsWithData}
                      </td>

                      {/* Stacked CSS Bar */}
                      <td className="py-3.5 px-4">
                        {evaluatedTotal > 0 ? (
                          <div className="space-y-1">
                            <div className="h-3 w-full bg-muted/60 rounded-full overflow-hidden flex shadow-2xs">
                              {ch.weakCount > 0 && (
                                <div
                                  style={{ width: `${weakPctOfBar}%` }}
                                  className="bg-rose-500 h-full transition-all"
                                  title={`Weak: ${ch.weakCount} student(s)`}
                                />
                              )}
                              {ch.developingCount > 0 && (
                                <div
                                  style={{ width: `${devPctOfBar}%` }}
                                  className="bg-amber-400 h-full transition-all"
                                  title={`Developing: ${ch.developingCount} student(s)`}
                                />
                              )}
                              {ch.strongCount > 0 && (
                                <div
                                  style={{ width: `${strongPctOfBar}%` }}
                                  className="bg-emerald-500 h-full transition-all"
                                  title={`Strong: ${ch.strongCount} student(s)`}
                                />
                              )}
                            </div>
                            <div className="flex items-center justify-between text-[10px] text-muted-foreground">
                              <span className="text-rose-600 dark:text-rose-400 font-semibold">{ch.weakCount}W</span>
                              <span className="text-amber-600 dark:text-amber-400 font-semibold">{ch.developingCount}D</span>
                              <span className="text-emerald-600 dark:text-emerald-400 font-semibold">{ch.strongCount}S</span>
                            </div>
                          </div>
                        ) : (
                          <span className="text-[11px] text-muted-foreground italic">All not enough data</span>
                        )}
                      </td>

                      {/* Weak % */}
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`font-black text-xs px-2 py-0.5 rounded-full ${
                            ch.weakPercentage >= 40
                              ? 'bg-rose-500/10 text-rose-700 dark:text-rose-300 border border-rose-500/20'
                              : 'text-foreground'
                          }`}
                        >
                          {ch.weakPercentage}%
                        </span>
                      </td>

                      {/* Average Accuracy */}
                      <td className="py-3.5 px-4 text-center font-semibold text-foreground">
                        {ch.averageAccuracy !== null ? `${ch.averageAccuracy}%` : '—'}
                      </td>

                      {/* Total Graded Answers */}
                      <td className="py-3.5 px-4 text-center font-medium text-muted-foreground">
                        {ch.totalGradedAnswers}
                      </td>

                      {/* Detail Link */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="inline-flex items-center text-primary group-hover:translate-x-0.5 transition font-semibold text-xs gap-0.5">
                          <span>View Chapter</span>
                          <ChevronRight size={13} />
                        </div>
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
