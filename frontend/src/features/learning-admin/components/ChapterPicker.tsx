import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getAdminSubjects, type SubjectResponse } from '../../../api/subjectApi';
import { getAdminChapters, type ChapterResponse } from '../../../api/chapterApi';

interface ChapterPickerProps {
  selectedChapterId?: number | null;
  selectedSubjectId?: number | null;
  onSelectionChange?: (chapterId: number | null, subjectId: number | null) => void;
  syncWithUrl?: boolean;
  allowAllChapters?: boolean;
  allowAllSubjects?: boolean;
  className?: string;
  disabled?: boolean;
}

export function ChapterPicker({
  selectedChapterId,
  selectedSubjectId,
  onSelectionChange,
  syncWithUrl = true,
  allowAllChapters = true,
  allowAllSubjects = true,
  className = '',
  disabled = false,
}: ChapterPickerProps) {
  const [searchParams, setSearchParams] = useSearchParams();

  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [chapters, setChapters] = useState<ChapterResponse[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(false);
  const [loadingChapters, setLoadingChapters] = useState(false);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);
  const [chaptersError, setChaptersError] = useState<string | null>(null);

  // Initialize from searchParams
  const paramSubjectId = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;
  const paramChapterId = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : null;

  const [localSubjectId, setLocalSubjectId] = useState<number | null>(paramSubjectId);
  const [localChapterId, setLocalChapterId] = useState<number | null>(paramChapterId);

  const currentSubjectId = selectedSubjectId !== undefined ? selectedSubjectId : localSubjectId;
  const currentChapterId = selectedChapterId !== undefined ? selectedChapterId : localChapterId;

  // Load subjects on mount
  useEffect(() => {
    let cancelled = false;
    async function loadSubjects() {
      setLoadingSubjects(true);
      setSubjectsError(null);
      try {
        const subs = await getAdminSubjects();
        if (!cancelled) setSubjects(subs);
      } catch (error) {
        if (!cancelled) {
          setSubjects([]);
          setSubjectsError(error instanceof Error ? error.message : 'Could not load subjects');
        }
      } finally {
        if (!cancelled) setLoadingSubjects(false);
      }
    }
    loadSubjects();
    return () => { cancelled = true; };
  }, []);

  // Load chapters when currentSubjectId changes
  useEffect(() => {
    let cancelled = false;
    async function loadChapters() {
      if (!currentSubjectId) {
        setChapters([]);
        setChaptersError(null);
        return;
      }
      setLoadingChapters(true);
      setChaptersError(null);
      try {
        const chs = await getAdminChapters(currentSubjectId);
        if (!cancelled) {
          setChapters(chs);
        }
      } catch (error) {
        if (!cancelled) {
          setChapters([]);
          setChaptersError(error instanceof Error ? error.message : 'Could not load chapters');
        }
      } finally {
        if (!cancelled) setLoadingChapters(false);
      }
    }
    loadChapters();
    return () => { cancelled = true; };
  }, [currentSubjectId]);

  function handleSubjectChange(subIdStr: string) {
    const nextSubId = subIdStr ? Number(subIdStr) : null;
    setLocalSubjectId(nextSubId);
    setLocalChapterId(null);

    if (syncWithUrl) {
      setSearchParams(prev => {
        const next = new URLSearchParams(prev);
        if (nextSubId) next.set('subjectId', String(nextSubId));
        else next.delete('subjectId');
        next.delete('chapterId');
        return next;
      });
    }

    onSelectionChange?.(null, nextSubId);
  }

  function handleChapterChange(chIdStr: string) {
    const nextChId = chIdStr ? Number(chIdStr) : null;
    setLocalChapterId(nextChId);

    if (syncWithUrl) {
      setSearchParams(prev => {
        const next = new URLSearchParams(prev);
        if (nextChId) next.set('chapterId', String(nextChId));
        else next.delete('chapterId');
        return next;
      });
    }

    onSelectionChange?.(nextChId, currentSubjectId);
  }

  return (
    <div className={`flex flex-wrap items-center gap-3 ${className}`}>
      {/* Subject select */}
      <div className="flex items-center gap-2">
        <label htmlFor="chapter-picker-subject" className="text-xs font-semibold text-muted-foreground whitespace-nowrap">
          Subject:
        </label>
        <select
          id="chapter-picker-subject"
          disabled={disabled || loadingSubjects}
          value={currentSubjectId ?? ''}
          onChange={e => handleSubjectChange(e.target.value)}
          className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground shadow-xs focus:outline-hidden focus:ring-2 focus:ring-primary/40 disabled:opacity-50"
        >
          {allowAllSubjects && <option value="">All Subjects</option>}
          {!allowAllSubjects && subjects.length === 0 && !loadingSubjects && <option value="">No subjects available</option>}
          {subjects.map(s => (
            <option key={s.subjectId} value={s.subjectId}>
              {s.name} {s.status === 'INACTIVE' ? '(Inactive)' : ''}
            </option>
          ))}
        </select>
      </div>

      {/* Chapter select */}
      <div className="flex items-center gap-2">
        <label htmlFor="chapter-picker-chapter" className="text-xs font-semibold text-muted-foreground whitespace-nowrap">
          Chapter:
        </label>
        <select
          id="chapter-picker-chapter"
          disabled={disabled || !currentSubjectId || loadingChapters}
          value={currentChapterId ?? ''}
          onChange={e => handleChapterChange(e.target.value)}
          className="rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground shadow-xs focus:outline-hidden focus:ring-2 focus:ring-primary/40 disabled:opacity-50 min-w-[180px]"
        >
          {allowAllChapters && <option value="">{currentSubjectId ? 'All Chapters' : 'Select subject first'}</option>}
          {!allowAllChapters && !currentSubjectId && <option value="">Select subject first</option>}
          {!allowAllChapters && currentSubjectId && chapters.length === 0 && !loadingChapters && <option value="">No chapters available</option>}
          {chapters.map(c => (
            <option key={c.chapterId} value={c.chapterId}>
              {c.name} {c.status === 'INACTIVE' ? '(Inactive)' : ''}
            </option>
          ))}
        </select>
      </div>
      {(subjectsError || chaptersError) && (
        <span className="w-full text-xs text-destructive">
          {subjectsError || chaptersError}
        </span>
      )}
    </div>
  );
}
