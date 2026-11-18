import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { getAvailableDiagnosticChapters, createDiagnosticAssessment } from '../api';
import type { AvailableChapterDto, AssessmentScopeType } from '../types';
import { LEARNING_ROUTES } from '../routes';
import { UnverifiedEmailPrompt } from '../components/UnverifiedEmailPrompt';
import { mapStudentError } from '../errorMapper';
import {
  Compass,
  Play,
  Loader2,
  CheckSquare,
  Square,
  ArrowLeft,
  Languages,
} from 'lucide-react';

export default function StudentDiagnosticPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [availableChapters, setAvailableChapters] = useState<AvailableChapterDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Form State
  const [scopeType, setScopeType] = useState<AssessmentScopeType>('ALL_SUBJECTS');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [selectedChapterIds, setSelectedChapterIds] = useState<number[]>([]);
  const [language, setLanguage] = useState<string>('EN');

  const [starting, setStarting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const isVerified = user?.isEmailVerified ?? user?.is_email_verified ?? false;

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const res = await getAvailableDiagnosticChapters();
        if (!cancelled) {
          setAvailableChapters(res.chapters);
          // Default all eligible chapters selected
          setSelectedChapterIds(res.chapters.map(c => c.chapterId));
          if (res.chapters.length > 0) {
            setSelectedSubjectId(res.chapters[0].subjectId);
          }
        }
      } catch (err: unknown) {
        if (!cancelled) setLoadError((err as { message?: string })?.message || 'Failed to load available chapters.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  // Group chapters by subject
  const subjectGroups = availableChapters.reduce<Record<number, { subjectName: string; chapters: AvailableChapterDto[] }>>(
    (acc, chap) => {
      if (!acc[chap.subjectId]) {
        acc[chap.subjectId] = { subjectName: chap.subjectName, chapters: [] };
      }
      acc[chap.subjectId].chapters.push(chap);
      return acc;
    },
    {}
  );

  function toggleChapter(chId: number) {
    setSelectedChapterIds(prev =>
      prev.includes(chId) ? prev.filter(id => id !== chId) : [...prev, chId]
    );
  }

  function toggleSelectAllSubject(subjectChapters: AvailableChapterDto[]) {
    const ids = subjectChapters.map(c => c.chapterId);
    const allSelected = ids.every(id => selectedChapterIds.includes(id));
    if (allSelected) {
      setSelectedChapterIds(prev => prev.filter(id => !ids.includes(id)));
    } else {
      setSelectedChapterIds(prev => Array.from(new Set([...prev, ...ids])));
    }
  }

  async function handleStart() {
    if (!isVerified) return;

    if (scopeType === 'CHAPTERS' && selectedChapterIds.length === 0) {
      setSubmitError('Please select at least one chapter to include in your diagnostic assessment.');
      return;
    }

    setStarting(true);
    setSubmitError(null);

    try {
      const res = await createDiagnosticAssessment({
        assessmentType: 'INITIAL_DIAGNOSTIC',
        scopeType,
        subjectId: scopeType === 'SUBJECT' ? selectedSubjectId : null,
        chapterIds: scopeType === 'CHAPTERS' ? selectedChapterIds : [],
        language,
      });

      navigate(LEARNING_ROUTES.ASSESSMENT_RUNNER(res.assessmentId));
    } catch (err: unknown) {
      const mapped = mapStudentError(err);
      setSubmitError(mapped.message);
      setStarting(false);
    }
  }

  function handleSkip() {
    try {
      localStorage.setItem(`al_onboarding_skipped_${user?.userId || 'student'}`, 'true');
    } catch {
      // ignore
    }
    navigate(LEARNING_ROUTES.OVERVIEW);
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading assessment setup…</span>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground transition"
        >
          <ArrowLeft size={14} />
          <span>Back</span>
        </button>

        <button
          type="button"
          onClick={handleSkip}
          className="text-xs font-semibold text-muted-foreground hover:text-foreground transition underline"
        >
          Skip for now
        </button>
      </div>

      <div className="rounded-3xl border border-border bg-card p-6 md:p-8 shadow-sm space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-primary">
            <Compass size={16} />
            <span>Diagnostic Assessment</span>
          </div>
          <h1 className="text-xl md:text-2xl font-black text-foreground tracking-tight">
            Initial Knowledge Mapping
          </h1>
          <p className="text-xs text-muted-foreground leading-relaxed">
            The diagnostic maps your current knowledge to identify strong areas and calibrate tailored recommendations.
          </p>
        </div>

        {!isVerified && (
          <UnverifiedEmailPrompt message="Email verification is required before taking diagnostic assessments." />
        )}

        {submitError && (
          <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
            {submitError}
          </div>
        )}

        {loadError && (
          <div className="p-4 rounded-2xl border border-destructive/30 bg-destructive/10 text-xs text-destructive">
            {loadError}
          </div>
        )}

        {/* Scope Selector */}
        <div className="space-y-3">
          <label className="text-xs font-bold text-foreground block">
            Choose what you want to be tested on:
          </label>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <button
              type="button"
              onClick={() => setScopeType('ALL_SUBJECTS')}
              className={`p-3.5 rounded-2xl border text-left transition ${
                scopeType === 'ALL_SUBJECTS'
                  ? 'border-primary bg-primary/5 text-foreground ring-1 ring-primary/40'
                  : 'border-border bg-card text-muted-foreground hover:bg-muted/40'
              }`}
            >
              <p className="text-xs font-bold text-foreground">All Subjects</p>
              <p className="text-[11px] text-muted-foreground mt-0.5">Comprehensive whole syllabus</p>
            </button>

            <button
              type="button"
              onClick={() => setScopeType('SUBJECT')}
              className={`p-3.5 rounded-2xl border text-left transition ${
                scopeType === 'SUBJECT'
                  ? 'border-primary bg-primary/5 text-foreground ring-1 ring-primary/40'
                  : 'border-border bg-card text-muted-foreground hover:bg-muted/40'
              }`}
            >
              <p className="text-xs font-bold text-foreground">One Subject</p>
              <p className="text-[11px] text-muted-foreground mt-0.5">Focus on a specific subject</p>
            </button>

            <button
              type="button"
              onClick={() => setScopeType('CHAPTERS')}
              className={`p-3.5 rounded-2xl border text-left transition ${
                scopeType === 'CHAPTERS'
                  ? 'border-primary bg-primary/5 text-foreground ring-1 ring-primary/40'
                  : 'border-border bg-card text-muted-foreground hover:bg-muted/40'
              }`}
            >
              <p className="text-xs font-bold text-foreground">Custom Chapters</p>
              <p className="text-[11px] text-muted-foreground mt-0.5">Chapters I have covered</p>
            </button>
          </div>
        </div>

        {/* One Subject Picker if chosen */}
        {scopeType === 'SUBJECT' && (
          <div className="p-4 rounded-2xl border border-border bg-muted/20 space-y-2">
            <label htmlFor="diag-subject-select" className="text-xs font-semibold text-foreground block">
              Select Subject:
            </label>
            <select
              id="diag-subject-select"
              value={selectedSubjectId ?? ''}
              onChange={e => setSelectedSubjectId(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-card px-3 py-2 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              {Object.entries(subjectGroups).map(([subId, group]) => (
                <option key={subId} value={subId}>
                  {group.subjectName} ({group.chapters.length} available chapters)
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Chapters I have covered multi-select */}
        {scopeType === 'CHAPTERS' && (
          <div className="space-y-4 pt-2">
            <span className="text-xs font-semibold text-muted-foreground">
              Select the chapters you have studied so far:
            </span>

            {availableChapters.length === 0 ? (
              <div className="p-4 rounded-2xl border border-dashed border-border text-center text-xs text-muted-foreground">
                No chapters currently have enough eligible questions for diagnostic testing.
              </div>
            ) : (
              <div className="space-y-4 max-h-72 overflow-y-auto pr-1">
                {Object.entries(subjectGroups).map(([subId, group]) => {
                  const subjectChapters = group.chapters;
                  const allSelected = subjectChapters.every(c => selectedChapterIds.includes(c.chapterId));

                  return (
                    <div key={subId} className="rounded-2xl border border-border bg-card p-3.5 space-y-2">
                      <div className="flex items-center justify-between pb-2 border-b border-border/60">
                        <span className="text-xs font-bold text-foreground">
                          {group.subjectName}
                        </span>
                        <button
                          type="button"
                          onClick={() => toggleSelectAllSubject(subjectChapters)}
                          className="text-[11px] font-semibold text-primary hover:underline"
                        >
                          {allSelected ? 'Deselect all' : 'Select all'}
                        </button>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                        {subjectChapters.map(ch => {
                          const isChecked = selectedChapterIds.includes(ch.chapterId);
                          return (
                            <button
                              key={ch.chapterId}
                              type="button"
                              onClick={() => toggleChapter(ch.chapterId)}
                              className={`p-2.5 rounded-xl border text-left text-xs transition flex items-center justify-between ${
                                isChecked
                                  ? 'border-primary/40 bg-primary/5 text-foreground font-semibold'
                                  : 'border-border text-muted-foreground hover:bg-muted/30'
                              }`}
                            >
                              <span className="truncate pr-2">{ch.chapterName}</span>
                              {isChecked ? (
                                <CheckSquare size={14} className="text-primary shrink-0" />
                              ) : (
                                <Square size={14} className="text-muted-foreground shrink-0" />
                              )}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Language select */}
        <div className="flex items-center gap-2 pt-2">
          <label htmlFor="diag-lang-select" className="text-xs font-semibold text-muted-foreground flex items-center gap-1">
            <Languages size={13} />
            <span>Language:</span>
          </label>
          <select
            id="diag-lang-select"
            value={language}
            onChange={e => setLanguage(e.target.value)}
            className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
          >
            <option value="EN">English</option>
            <option value="SI">Sinhala</option>
            <option value="TA">Tamil</option>
          </select>
        </div>

        {/* Action buttons */}
        <div className="pt-4 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            type="button"
            onClick={handleSkip}
            className="w-full sm:w-auto px-4 py-2.5 rounded-xl border border-border text-xs font-semibold text-muted-foreground hover:bg-muted transition text-center"
          >
            Skip for now
          </button>

          <button
            type="button"
            disabled={!isVerified || starting || availableChapters.length === 0}
            onClick={handleStart}
            className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary text-primary-foreground text-xs font-bold hover:bg-primary/90 transition shadow-xs flex items-center justify-center gap-2 disabled:opacity-40"
          >
            {starting ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                <span>Generating Diagnostic…</span>
              </>
            ) : (
              <>
                <Play size={14} />
                <span>Begin Assessment</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
