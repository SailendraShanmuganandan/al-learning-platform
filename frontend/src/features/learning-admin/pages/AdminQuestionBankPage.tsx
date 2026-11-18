import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  getAllAdminQuestions,
  getChapterAdminQuestions,
  updateAdminQuestionStatus,
  bulkUpdateQuestionStatus,
  getAdminTopics,
} from '../api';
import type {
  QuestionSummaryResponse,
  QuestionStatus,
  QuestionDifficulty,
  QuestionSource,
  TopicResponse,
  BulkStatusResponse,
} from '../types';
import { getErrorMessage } from '../errorHelper';
import { StatusBadge } from '../components/StatusBadge';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { SourceBadge } from '../components/SourceBadge';
import { ChapterPicker } from '../components/ChapterPicker';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FeedbackAlert } from '../components/FeedbackAlert';
import { BulkResultDialog } from '../components/BulkResultDialog';
import { TopicsManagerModal } from '../components/TopicsManagerModal';
import {
  FileQuestion,
  Plus,
  FolderTree,
  Search,
  RotateCcw,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  Edit2,
  CheckCircle2,
  Archive,
  Loader2,
  Layers,
  Flag,
} from 'lucide-react';

export default function AdminQuestionBankPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL query params
  const scopeParam = searchParams.get('scope') || (searchParams.get('chapterId') ? 'chapter' : 'all');
  const chapterIdParam = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : null;
  const subjectIdParam = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : null;
  const statusParam = (searchParams.get('status') as QuestionStatus) || '';
  const topicIdParam = searchParams.get('topicId') ? Number(searchParams.get('topicId')) : null;
  const difficultyParam = (searchParams.get('difficulty') as QuestionDifficulty) || '';
  const languageParam = searchParams.get('language') || '';
  const sourceParam = (searchParams.get('source') as QuestionSource) || '';
  const searchParam = searchParams.get('search') || '';
  const reportedParam = searchParams.get('reported');
  const lowRateParam = searchParams.get('lowCorrectRate');
  const docRemovedParam = searchParams.get('sourceDocumentRemoved');
  const sourceDocIdParam = searchParams.get('sourceDocumentId') ? Number(searchParams.get('sourceDocumentId')) : null;
  const genJobIdParam = searchParams.get('generationJobId') ? Number(searchParams.get('generationJobId')) : null;

  // Local state
  const [questions, setQuestions] = useState<QuestionSummaryResponse[]>([]);
  const [topics, setTopics] = useState<TopicResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Search input local buffer
  const [searchInput, setSearchInput] = useState(searchParam);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Row selection for bulk
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [bulkRunning, setBulkRunning] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkStatusResponse | null>(null);

  // Topics modal
  const [topicsModalOpen, setTopicsModalOpen] = useState(false);

  // Prompt chapter select modal if clicked Add Question without chapter
  const [needsChapterModal, setNeedsChapterModal] = useState(false);
  const [pendingQuestionChapterId, setPendingQuestionChapterId] = useState<number | null>(null);

  // Single question status transition confirmation
  const [transitionConfirm, setTransitionConfirm] = useState<{
    question: QuestionSummaryResponse;
    targetStatus: QuestionStatus;
    title: string;
    description: string;
  } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  // Load chapter topics when chapter is chosen
  useEffect(() => {
    let cancelled = false;
    async function loadTopics() {
      if (!chapterIdParam) {
        setTopics([]);
        return;
      }
      try {
        const data = await getAdminTopics(chapterIdParam);
        if (!cancelled) setTopics(data);
      } catch {
        if (!cancelled) setTopics([]);
      }
    }
    loadTopics();
    return () => { cancelled = true; };
  }, [chapterIdParam]);

  const [refreshKey, setRefreshKey] = useState(0);

  // Fetch questions
  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const commonFilters = {
          quizId: undefined,
          topicId: topicIdParam || undefined,
          status: statusParam || undefined,
          difficulty: difficultyParam || undefined,
          language: languageParam || undefined,
          source: sourceParam || undefined,
          search: searchParam || undefined,
          reported: reportedParam === 'true' ? true : reportedParam === 'false' ? false : undefined,
          lowCorrectRate: lowRateParam === 'true' ? true : lowRateParam === 'false' ? false : undefined,
          sourceDocumentRemoved: docRemovedParam === 'true' ? true : docRemovedParam === 'false' ? false : undefined,
          sourceDocumentId: sourceDocIdParam || undefined,
          generationJobId: genJobIdParam || undefined,
          page,
          size: 20,
        };

        let res;
        if (scopeParam === 'chapter' && chapterIdParam) {
          res = await getChapterAdminQuestions(chapterIdParam, commonFilters);
        } else {
          res = await getAllAdminQuestions({
            ...commonFilters,
            chapterId: chapterIdParam || undefined,
            subjectId: subjectIdParam || undefined,
          });
        }

        if (!cancelled) {
          setQuestions(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
          setSelectedIds([]);
        }
      } catch (err) {
        if (!cancelled) setPageError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [
    scopeParam,
    chapterIdParam,
    subjectIdParam,
    statusParam,
    topicIdParam,
    difficultyParam,
    languageParam,
    sourceParam,
    searchParam,
    reportedParam,
    lowRateParam,
    docRemovedParam,
    sourceDocIdParam,
    genJobIdParam,
    page,
    refreshKey,
  ]);

  const refreshQuestions = () => {
    setLoading(true);
    setRefreshKey(k => k + 1);
  };

  // Update query params helper
  function updateParam(key: string, val: string | null) {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (val !== null && val !== '') next.set(key, val);
      else next.delete(key);
      return next;
    });
    setPage(0);
  }

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    updateParam('search', searchInput.trim() || null);
  }

  function handleResetFilters() {
    setSearchInput('');
    setSearchParams({});
    setPage(0);
  }

  function handleScopeChange(newScope: 'all' | 'chapter') {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.set('scope', newScope);
      if (newScope === 'all') {
        next.delete('chapterId');
        next.delete('topicId');
      }
      return next;
    });
    setPage(0);
  }

  // Row selection
  const allPageIds = questions.map(q => q.questionId);
  const isAllSelected = allPageIds.length > 0 && allPageIds.every(id => selectedIds.includes(id));

  function toggleSelectAll() {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(allPageIds);
    }
  }

  function toggleSelectRow(id: number) {
    setSelectedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  }

  // Bulk actions
  async function executeBulk(targetStatus: QuestionStatus) {
    if (selectedIds.length === 0) return;
    setBulkRunning(true);
    setPageError(null);
    try {
      const res = await bulkUpdateQuestionStatus({
        questionIds: selectedIds,
        targetStatus,
      });
      setBulkResult(res);
      refreshQuestions();
    } catch (err) {
      setPageError(getErrorMessage(err));
    } finally {
      setBulkRunning(false);
    }
  }

  // Single status transition prompt
  function promptStatusTransition(q: QuestionSummaryResponse, target: QuestionStatus) {
    let title = '';
    let description = '';

    if (target === 'APPROVED') {
      title = `Approve Question #${q.questionId}?`;
      description = 'Approving this question makes it eligible for chapter quizzes and student study sessions.';
    } else if (target === 'PENDING') {
      title = `Return Question #${q.questionId} to Review?`;
      description = 'Question will be placed back in Pending status for review or editing.';
    } else if (target === 'ARCHIVED') {
      title = `Archive Question #${q.questionId}?`;
      description = 'Archived questions will no longer be used in new quiz attempts.';
    }

    setTransitionConfirm({
      question: q,
      targetStatus: target,
      title,
      description,
    });
  }

  async function handleConfirmTransition() {
    if (!transitionConfirm) return;
    setTransitioning(true);
    try {
      await updateAdminQuestionStatus(transitionConfirm.question.questionId, {
        status: transitionConfirm.targetStatus,
      });
      setActionSuccess(`Question #${transitionConfirm.question.questionId} status updated to ${transitionConfirm.targetStatus}.`);
      setTransitionConfirm(null);
      refreshQuestions();
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setTransitioning(false);
    }
  }

  function handleAddQuestionClick() {
    if (chapterIdParam) {
      navigate(`/admin/question-bank/new?chapterId=${chapterIdParam}`);
    } else {
      setPendingQuestionChapterId(null);
      setNeedsChapterModal(true);
    }
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
            <FileQuestion className="text-primary" size={24} />
            <span>Question Bank</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Author, review, approve, and manage question bank items across all chapters.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {chapterIdParam && (
            <button
              type="button"
              onClick={() => setTopicsModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-border text-xs font-semibold text-foreground hover:bg-muted transition shadow-xs"
            >
              <FolderTree size={14} className="text-primary" />
              <span>Manage Topics</span>
            </button>
          )}

          <button
            type="button"
            onClick={handleAddQuestionClick}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs"
          >
            <Plus size={14} />
            <span>Add Question</span>
          </button>
        </div>
      </div>

      {actionSuccess && (
        <FeedbackAlert
          type="success"
          message={actionSuccess}
          onClose={() => setActionSuccess(null)}
        />
      )}

      {pageError && (
        <FeedbackAlert
          type="error"
          message={pageError}
          onClose={() => setPageError(null)}
        />
      )}

      {/* Scope Selector Card */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-muted-foreground">Scope:</span>
          <div className="inline-flex rounded-xl border border-border p-0.5 bg-muted/40">
            <button
              type="button"
              onClick={() => handleScopeChange('all')}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition ${
                scopeParam !== 'chapter'
                  ? 'bg-card text-foreground shadow-xs'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              Everything
            </button>
            <button
              type="button"
              onClick={() => handleScopeChange('chapter')}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition ${
                scopeParam === 'chapter'
                  ? 'bg-card text-foreground shadow-xs'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              Chosen Chapter
            </button>
          </div>
        </div>

        {/* Chapter Picker for Scope */}
        <ChapterPicker
          allowAllSubjects={true}
          allowAllChapters={scopeParam !== 'chapter'}
          onSelectionChange={(chId, subId) => {
            if (chId) {
              setSearchParams(prev => {
                const next = new URLSearchParams(prev);
                next.set('chapterId', String(chId));
                if (subId) next.set('subjectId', String(subId));
                return next;
              });
            }
          }}
        />
      </div>

      {/* Advanced Filter Bar */}
      <div className="rounded-2xl border border-border bg-card p-4 shadow-sm space-y-3">
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
          {/* Status filter */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Status</label>
            <select
              value={statusParam}
              onChange={e => updateParam('status', e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending Review</option>
              <option value="APPROVED">Approved</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>

          {/* Topic filter (only active when chapter chosen) */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Topic</label>
            <select
              disabled={!chapterIdParam || topics.length === 0}
              value={topicIdParam || ''}
              onChange={e => updateParam('topicId', e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden disabled:opacity-50"
            >
              <option value="">{chapterIdParam ? 'All Topics' : 'Select chapter first'}</option>
              {topics.map(t => (
                <option key={t.topicId} value={t.topicId}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>

          {/* Difficulty */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Difficulty</label>
            <select
              value={difficultyParam}
              onChange={e => updateParam('difficulty', e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Difficulties</option>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </div>

          {/* Language */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Language</label>
            <select
              value={languageParam}
              onChange={e => updateParam('language', e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Languages</option>
              <option value="EN">English</option>
              <option value="SI">Sinhala</option>
              <option value="TA">Tamil</option>
            </select>
          </div>

          {/* Source */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Source</label>
            <select
              value={sourceParam}
              onChange={e => updateParam('source', e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">All Sources</option>
              <option value="MANUAL">Manual</option>
              <option value="AI">AI Generated</option>
            </select>
          </div>

          {/* Quick flags: Reported / Low Rate / Doc Removed */}
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground mb-1 block">Special Flags</label>
            <select
              value={
                reportedParam === 'true'
                  ? 'reported'
                  : lowRateParam === 'true'
                  ? 'lowRate'
                  : docRemovedParam === 'true'
                  ? 'docRemoved'
                  : ''
              }
              onChange={e => {
                const val = e.target.value;
                setSearchParams(prev => {
                  const next = new URLSearchParams(prev);
                  next.delete('reported');
                  next.delete('lowCorrectRate');
                  next.delete('sourceDocumentRemoved');
                  if (val === 'reported') next.set('reported', 'true');
                  if (val === 'lowRate') next.set('lowCorrectRate', 'true');
                  if (val === 'docRemoved') next.set('sourceDocumentRemoved', 'true');
                  return next;
                });
                setPage(0);
              }}
              className="w-full rounded-xl border border-border bg-background px-2.5 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
            >
              <option value="">No Special Flag</option>
              <option value="reported">Has Open Reports</option>
              <option value="lowRate">Low Correct Rate (&lt;20%)</option>
              <option value="docRemoved">Source Document Removed</option>
            </select>
          </div>
        </div>

        {/* Search bar & Reset */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2 border-t border-border/60">
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 w-full sm:max-w-md">
            <div className="relative flex-1">
              <Search size={14} className="absolute left-3 top-2.5 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search question text or explanation…"
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                className="w-full rounded-xl border border-border bg-background pl-8 pr-3 py-1.5 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              />
            </div>
            <button
              type="submit"
              className="px-3 py-1.5 rounded-xl bg-muted text-foreground text-xs font-semibold hover:bg-muted/80 transition shrink-0"
            >
              Search
            </button>
          </form>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              type="button"
              onClick={handleResetFilters}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition px-2 py-1"
            >
              <RotateCcw size={12} />
              <span>Reset Filters</span>
            </button>
          </div>
        </div>
      </div>

      {/* Bulk Action Bar */}
      {selectedIds.length > 0 && (
        <div className="rounded-2xl border border-primary/30 bg-primary/10 p-3.5 flex flex-wrap items-center justify-between gap-3 animate-in fade-in duration-150">
          <div className="flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-primary text-primary-foreground font-black text-xs">
              {selectedIds.length}
            </span>
            <span className="text-xs font-semibold text-foreground">
              question{selectedIds.length === 1 ? '' : 's'} selected
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={bulkRunning}
              onClick={() => executeBulk('APPROVED')}
              className="px-3 py-1.5 rounded-xl bg-emerald-600 text-white text-xs font-semibold hover:bg-emerald-700 transition disabled:opacity-50 flex items-center gap-1 shadow-xs"
            >
              {bulkRunning ? <Loader2 size={13} className="animate-spin" /> : <CheckCircle2 size={13} />}
              <span>Approve Selected</span>
            </button>

            <button
              type="button"
              disabled={bulkRunning}
              onClick={() => executeBulk('ARCHIVED')}
              className="px-3 py-1.5 rounded-xl border border-destructive/30 bg-destructive/10 text-destructive text-xs font-semibold hover:bg-destructive/20 transition disabled:opacity-50 flex items-center gap-1"
            >
              {bulkRunning ? <Loader2 size={13} className="animate-spin" /> : <Archive size={13} />}
              <span>Archive Selected</span>
            </button>

            <button
              type="button"
              onClick={() => setSelectedIds([])}
              className="px-2.5 py-1.5 text-xs text-muted-foreground hover:text-foreground"
            >
              Deselect All
            </button>
          </div>
        </div>
      )}

      {/* Questions Table */}
      <div className="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="py-3 px-3 w-8">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    onChange={toggleSelectAll}
                    className="rounded border-border text-primary focus:ring-primary/40 cursor-pointer"
                  />
                </th>
                <th className="py-3 px-4 min-w-[260px]">Question</th>
                {scopeParam !== 'chapter' && <th className="py-3 px-4">Chapter</th>}
                <th className="py-3 px-4">Topic</th>
                <th className="py-3 px-4">Difficulty</th>
                <th className="py-3 px-4">Lang</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Source</th>
                <th className="py-3 px-4 text-center">Answers</th>
                <th className="py-3 px-4 text-center">Correct Rate</th>
                <th className="py-3 px-4 text-center">Reports</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={12} className="py-12 text-center text-muted-foreground">
                    <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
                    <span>Loading questions…</span>
                  </td>
                </tr>
              ) : questions.length === 0 ? (
                <tr>
                  <td colSpan={12} className="py-12 text-center text-muted-foreground">
                    <Layers size={28} className="mx-auto mb-2 opacity-40" />
                    <p className="font-semibold text-foreground">No questions found</p>
                    <p className="text-xs mt-1">Try adjusting the filter criteria or add a new question.</p>
                  </td>
                </tr>
              ) : (
                questions.map(q => {
                  const isLowRate = q.answerCount >= 20 && q.correctRate !== null && q.correctRate < 20;

                  return (
                    <tr
                      key={q.questionId}
                      className={`hover:bg-muted/30 transition ${
                        selectedIds.includes(q.questionId) ? 'bg-primary/5' : ''
                      }`}
                    >
                      <td className="py-3 px-3">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(q.questionId)}
                          onChange={() => toggleSelectRow(q.questionId)}
                          className="rounded border-border text-primary focus:ring-primary/40 cursor-pointer"
                        />
                      </td>

                      {/* Question Text */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-start gap-1.5">
                          {q.sourceDocumentRemoved && (
                            <span
                              title="The source document used to generate this question has been deleted."
                              className="text-amber-500 shrink-0 mt-0.5"
                            >
                              <AlertTriangle size={14} />
                            </span>
                          )}
                          <div>
                            <Link
                              to={`/admin/question-bank/${q.questionId}`}
                              className="font-medium text-foreground hover:text-primary transition line-clamp-2 leading-relaxed"
                              title={q.questionText}
                            >
                              {q.questionText}
                            </Link>
                            <span className="text-[10px] text-muted-foreground font-mono">
                              #{q.questionId} &bull; {q.optionsCount} options
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Chapter (when scope is everything) */}
                      {scopeParam !== 'chapter' && (
                        <td className="py-3.5 px-4 text-muted-foreground">
                          <div className="font-medium text-foreground truncate max-w-[140px]" title={q.chapterName || ''}>
                            {q.chapterName || '—'}
                          </div>
                          {q.subjectName && (
                            <div className="text-[10px] text-muted-foreground truncate max-w-[140px]">
                              {q.subjectName}
                            </div>
                          )}
                        </td>
                      )}

                      {/* Topic */}
                      <td className="py-3.5 px-4 text-muted-foreground font-medium">
                        {q.topicName || <span className="opacity-50">None</span>}
                      </td>

                      {/* Difficulty */}
                      <td className="py-3.5 px-4">
                        <DifficultyBadge difficulty={q.difficulty} />
                      </td>

                      {/* Language */}
                      <td className="py-3.5 px-4 font-mono text-[11px] font-semibold text-muted-foreground">
                        {q.language}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        <StatusBadge status={q.status} />
                      </td>

                      {/* Source */}
                      <td className="py-3.5 px-4">
                        <SourceBadge source={q.source} />
                      </td>

                      {/* Answers count */}
                      <td className="py-3.5 px-4 text-center font-medium text-foreground">
                        {q.answerCount}
                      </td>

                      {/* Correct rate */}
                      <td className="py-3.5 px-4 text-center">
                        {q.correctRate !== null ? (
                          <span
                            className={`font-semibold text-xs px-2 py-0.5 rounded-full ${
                              isLowRate
                                ? 'bg-rose-500/10 text-rose-700 dark:text-rose-300 font-black border border-rose-500/20'
                                : 'text-foreground'
                            }`}
                            title={isLowRate ? 'Low correct rate (< 20% over 20+ answers)' : ''}
                          >
                            {q.correctRate}%
                          </span>
                        ) : (
                          <span className="text-muted-foreground">—</span>
                        )}
                      </td>

                      {/* Open reports count */}
                      <td className="py-3.5 px-4 text-center">
                        {q.openReportCount > 0 ? (
                          <Link
                            to={`/admin/question-reports?questionId=${q.questionId}`}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-700 dark:text-rose-300 font-bold border border-rose-500/20 hover:bg-rose-500/20 transition text-[11px]"
                            title="View open reports"
                          >
                            <Flag size={11} />
                            <span>{q.openReportCount}</span>
                          </Link>
                        ) : (
                          <span className="text-muted-foreground text-xs">0</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Link
                            to={`/admin/question-bank/${q.questionId}`}
                            className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
                            title="View / Edit"
                          >
                            <Edit2 size={13} />
                          </Link>

                          {q.status === 'PENDING' && (
                            <button
                              type="button"
                              onClick={() => promptStatusTransition(q, 'APPROVED')}
                              className="px-2 py-1 rounded-lg bg-emerald-600 text-white text-[11px] font-semibold hover:bg-emerald-700 transition"
                            >
                              Approve
                            </button>
                          )}

                          {q.status === 'APPROVED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusTransition(q, 'PENDING')}
                              className="px-2 py-1 rounded-lg border border-amber-500/30 text-amber-600 hover:bg-amber-500/10 text-[11px] font-medium transition"
                              title="Return to review"
                            >
                              Review
                            </button>
                          )}

                          {q.status !== 'ARCHIVED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusTransition(q, 'ARCHIVED')}
                              className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition"
                              title="Archive Question"
                            >
                              <Archive size={13} />
                            </button>
                          )}

                          {q.status === 'ARCHIVED' && (
                            <button
                              type="button"
                              onClick={() => promptStatusTransition(q, 'PENDING')}
                              className="px-2 py-1 rounded-lg border border-border text-muted-foreground text-[11px] font-medium hover:bg-muted transition"
                            >
                              Restore
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="border-t border-border px-4 py-3 flex items-center justify-between">
            <span className="text-xs text-muted-foreground">
              Showing page <strong className="text-foreground">{page + 1}</strong> of <strong className="text-foreground">{totalPages}</strong> ({totalElements} questions)
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
                className="p-1.5 rounded-lg border border-border text-muted-foreground hover:bg-muted transition disabled:opacity-40"
              >
                <ChevronLeft size={14} />
              </button>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                className="p-1.5 rounded-lg border border-border text-muted-foreground hover:bg-muted transition disabled:opacity-40"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Single Status Confirm Dialog */}
      {transitionConfirm && (
        <ConfirmDialog
          isOpen={true}
          title={transitionConfirm.title}
          description={transitionConfirm.description}
          confirmLabel={`Yes, change to ${transitionConfirm.targetStatus}`}
          confirmVariant={
            transitionConfirm.targetStatus === 'APPROVED'
              ? 'emerald'
              : transitionConfirm.targetStatus === 'ARCHIVED'
              ? 'destructive'
              : 'amber'
          }
          isLoading={transitioning}
          onConfirm={handleConfirmTransition}
          onCancel={() => setTransitionConfirm(null)}
        />
      )}

      {/* Bulk Result Dialog */}
      <BulkResultDialog
        isOpen={bulkResult !== null}
        result={bulkResult}
        onClose={() => setBulkResult(null)}
      />

      {/* Topics Manager Modal */}
      {chapterIdParam && (
        <TopicsManagerModal
          isOpen={topicsModalOpen}
          chapterId={chapterIdParam}
          chapterName={questions[0]?.chapterName || undefined}
          onClose={() => setTopicsModalOpen(false)}
          onTopicsUpdated={refreshQuestions}
        />
      )}

      {/* Need Chapter Prompt Modal */}
      {needsChapterModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-150">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-4 mx-auto">
              <FileQuestion size={24} />
            </div>
            <h3 className="text-base font-bold text-center text-foreground">
              Select a Chapter
            </h3>
            <p className="text-xs text-center text-muted-foreground mt-2 mb-6 leading-relaxed">
              Every question in the question bank belongs to a specific chapter. Please select a chapter to author questions for:
            </p>
            <div className="mb-6 flex justify-center">
              <ChapterPicker
                allowAllChapters={false}
                allowAllSubjects={false}
                syncWithUrl={false}
                onSelectionChange={(chId) => {
                  if (chId) {
                    setPendingQuestionChapterId(chId);
                  } else {
                    setPendingQuestionChapterId(null);
                  }
                }}
              />
            </div>
            <button
              type="button"
              disabled={!pendingQuestionChapterId}
              onClick={() => {
                if (!pendingQuestionChapterId) return;
                setNeedsChapterModal(false);
                navigate(`/admin/question-bank/new?chapterId=${pendingQuestionChapterId}`);
              }}
              className="w-full py-2 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-medium hover:bg-primary/90 transition disabled:cursor-not-allowed disabled:opacity-50 mb-2"
            >
              Continue to New Question
            </button>
            <button
              type="button"
              onClick={() => setNeedsChapterModal(false)}
              className="w-full py-2 px-4 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
