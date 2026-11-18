import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams, useLocation, Link } from 'react-router-dom';
import { useForm, useFieldArray, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  getAdminQuestionDetail,
  createAdminQuestion,
  updateAdminQuestion,
  updateAdminQuestionStatus,
  getAdminTopics,
  createAdminTopic,
} from '../api';
import type {
  QuestionDetailResponse,
  TopicResponse,
} from '../types';
import { getAllAdminChapters, type ChapterResponse } from '../../../api/chapterApi';
import { ChapterPicker } from '../components/ChapterPicker';
import { getErrorMessage, applyFormFieldErrors } from '../errorHelper';
import { StatusBadge } from '../components/StatusBadge';
import { SourceBadge } from '../components/SourceBadge';
import { FeedbackAlert } from '../components/FeedbackAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  ArrowLeft,
  Plus,
  Trash2,
  CheckCircle2,
  Loader2,
  Lock,
  Save,
  Archive,
  Flag,
  BookOpen,
} from 'lucide-react';

// Zod Schema
const questionFormSchema = z.object({
  questionText: z.string().min(1, 'Question text is required'),
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
  language: z.enum(['EN', 'SI', 'TA']),
  explanation: z.string().optional(),
  topicId: z.number().nullable().optional(),
  options: z
    .array(
      z.object({
        optionId: z.number().nullable().optional(),
        optionText: z.string().min(1, 'Option text is required'),
        isCorrect: z.boolean(),
      })
    )
    .min(2, 'At least 2 options are required')
    .max(6, 'At most 6 options are allowed')
    .refine(
      opts => opts.filter(o => o.isCorrect).length === 1,
      { message: 'Exactly one option must be marked as correct' }
    )
    .refine(
      opts => {
        const texts = opts.map(o => o.optionText.trim().toLowerCase()).filter(Boolean);
        const unique = new Set(texts);
        return unique.size === texts.length;
      },
      { message: 'Options must have distinct text (no duplicate options)' }
    ),
});

type QuestionFormValues = z.infer<typeof questionFormSchema>;

export default function AdminQuestionFormPage() {
  const { id } = useParams<{ id?: string }>();
  const isEditMode = Boolean(id);
  const questionId = id ? Number(id) : null;

  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const chapterIdQuery = searchParams.get('chapterId') ? Number(searchParams.get('chapterId')) : null;

  // Local state
  const [chapterId, setChapterId] = useState<number | null>(chapterIdQuery);
  const [allChapters, setAllChapters] = useState<ChapterResponse[]>([]);
  const [showChangeChapter, setShowChangeChapter] = useState(false);
  const [sessionCreatedCount, setSessionCreatedCount] = useState(0);
  const [unsavedDoneConfirm, setUnsavedDoneConfirm] = useState(false);
  const [existingQuestion, setExistingQuestion] = useState<QuestionDetailResponse | null>(null);
  const [topics, setTopics] = useState<TopicResponse[]>([]);
  const [loadingInitial, setLoadingInitial] = useState(isEditMode);
  const [pageError, setPageError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const questionTextRef = useRef<HTMLTextAreaElement | null>(null);

  // Quick "+ New Topic" state
  const [newTopicName, setNewTopicName] = useState('');
  const [showNewTopicInput, setShowNewTopicInput] = useState(false);
  const [creatingTopic, setCreatingTopic] = useState(false);

  // Archive & Replace state
  const [archiveAndReplaceConfirm, setArchiveAndReplaceConfirm] = useState(false);
  const [archiving, setArchiving] = useState(false);

  // Form Setup
  const {
    register,
    control,
    handleSubmit,
    setValue,
    reset,
    setError,
    formState: { errors, isDirty },
  } = useForm<QuestionFormValues>({
    resolver: zodResolver(questionFormSchema),
    defaultValues: {
      questionText: '',
      difficulty: 'MEDIUM',
      language: 'EN',
      explanation: '',
      topicId: null,
      options: [
        { optionText: '', isCorrect: true },
        { optionText: '', isCorrect: false },
        { optionText: '', isCorrect: false },
        { optionText: '', isCorrect: false },
      ],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'options',
  });

  const watchedQuestionText = useWatch({ control, name: 'questionText' });
  const watchedOptions = useWatch({ control, name: 'options' }) || [];
  const watchedTopicId = useWatch({ control, name: 'topicId' });

  // Load all chapters to display chapter name
  useEffect(() => {
    let cancelled = false;
    getAllAdminChapters()
      .then(data => {
        if (!cancelled) setAllChapters(data);
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const currentChapter = allChapters.find(c => c.chapterId === chapterId);

  // Prefill from clone or archive replacement state
  useEffect(() => {
    if (!isEditMode && location.state && (location.state as { prefill?: Partial<QuestionFormValues> }).prefill) {
      const p = (location.state as { prefill: Partial<QuestionFormValues> }).prefill;
      reset({
        questionText: p.questionText || '',
        difficulty: p.difficulty || 'MEDIUM',
        language: p.language || 'EN',
        explanation: p.explanation || '',
        topicId: p.topicId ?? null,
        options: p.options && p.options.length > 0 ? p.options : [
          { optionText: '', isCorrect: true },
          { optionText: '', isCorrect: false },
          { optionText: '', isCorrect: false },
          { optionText: '', isCorrect: false },
        ],
      });
    }
  }, [isEditMode, location.state, reset]);

  // Load existing question in edit mode
  useEffect(() => {
    if (!isEditMode || !questionId) return;
    let cancelled = false;

    async function loadQuestion() {
      try {
        const data = await getAdminQuestionDetail(questionId!);
        if (cancelled) return;
        setExistingQuestion(data);
        reset({
          questionText: data.questionText,
          difficulty: data.difficulty,
          language: (data.language as 'EN' | 'SI' | 'TA') || 'EN',
          explanation: data.explanation || '',
          topicId: data.topicId,
          options: data.options.map(o => ({
            optionId: o.optionId,
            optionText: o.text,
            isCorrect: o.isCorrect,
          })),
        });
      } catch (err) {
        if (!cancelled) setPageError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoadingInitial(false);
      }
    }

    loadQuestion();
    return () => { cancelled = true; };
  }, [isEditMode, questionId, reset]);

  // Load topics for the chapter
  useEffect(() => {
    if (!chapterId) return;
    let cancelled = false;
    async function loadTopics() {
      try {
        const data = await getAdminTopics(chapterId!);
        if (!cancelled) setTopics(data);
      } catch {
        if (!cancelled) setTopics([]);
      }
    }
    loadTopics();
    return () => { cancelled = true; };
  }, [chapterId]);

  // If edit mode and prefill from clone query
  useEffect(() => {
    const cloneText = searchParams.get('cloneText');
    if (cloneText) {
      setValue('questionText', cloneText);
    }
  }, [searchParams, setValue]);

  // Handle setting correct option
  function setCorrectOption(index: number) {
    if (isAnsweredLocked) return;
    watchedOptions.forEach((_, idx) => {
      setValue(`options.${idx}.isCorrect`, idx === index, { shouldDirty: true });
    });
  }

  // Quick create topic inline
  async function handleQuickCreateTopic() {
    if (!chapterId || !newTopicName.trim()) return;
    setCreatingTopic(true);
    try {
      const created = await createAdminTopic(chapterId, { name: newTopicName.trim() });
      const updatedTopics = await getAdminTopics(chapterId);
      setTopics(updatedTopics);
      setValue('topicId', created.topicId, { shouldDirty: true });
      setNewTopicName('');
      setShowNewTopicInput(false);
      setActionSuccess(`Topic "${created.name}" created and selected.`);
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setCreatingTopic(false);
    }
  }

  // Submit Handler
  async function onSaveSubmit(data: QuestionFormValues, saveMode: 'PENDING' | 'APPROVED') {
    if (saveMode === 'APPROVED' && !data.topicId) {
      setError('topicId', {
        type: 'manual',
        message: 'A topic is required when saving or approving a question.',
      });
      return;
    }

    setSaving(true);
    setPageError(null);
    setActionSuccess(null);

    try {
      if (isEditMode && questionId) {
        // PUT edit question
        await updateAdminQuestion(questionId, {
          topicId: data.topicId,
          questionText: data.questionText,
          difficulty: data.difficulty,
          language: data.language,
          explanation: data.explanation || null,
          options: data.options.map((o, idx) => ({
            optionId: o.optionId ?? null,
            optionText: o.optionText,
            optionOrder: idx + 1,
            isCorrect: o.isCorrect,
          })),
        });
        setActionSuccess('Question updated successfully.');
        const updated = await getAdminQuestionDetail(questionId);
        setExistingQuestion(updated);
      } else {
        // POST create question
        if (!chapterId) {
          throw new Error('Chapter ID is missing. Please select a chapter first.');
        }

        const res = await createAdminQuestion(chapterId, {
          topicId: data.topicId,
          questionText: data.questionText,
          difficulty: data.difficulty,
          language: data.language,
          explanation: data.explanation || null,
          saveMode,
          options: data.options.map((o, idx) => ({
            optionText: o.optionText,
            optionOrder: idx + 1,
            isCorrect: o.isCorrect,
          })),
        });

        const newCount = sessionCreatedCount + 1;
        setSessionCreatedCount(newCount);

        setActionSuccess(
          `Question #${res.questionId} saved as ${saveMode}! (${newCount} added this session). Ready for the next question below, or click Done when finished.`
        );

        // Keep current topic, difficulty, and language for next question
        const keepTopicId = data.topicId ?? null;
        const keepDifficulty = data.difficulty;
        const keepLanguage = data.language;

        reset({
          questionText: '',
          difficulty: keepDifficulty,
          language: keepLanguage,
          explanation: '',
          topicId: keepTopicId,
          options: [
            { optionText: '', isCorrect: true },
            { optionText: '', isCorrect: false },
            { optionText: '', isCorrect: false },
            { optionText: '', isCorrect: false },
          ],
        });

        // Focus question text textarea for instant next question entry
        setTimeout(() => {
          questionTextRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
          questionTextRef.current?.focus();
        }, 100);
      }
    } catch (err) {
      const handled = applyFormFieldErrors(err, setError);
      if (!handled) {
        setPageError(getErrorMessage(err));
      }
    } finally {
      setSaving(false);
    }
  }

  // Done Handler (for continuous question creation)
  function handleDone() {
    const hasUnsavedContent =
      Boolean(watchedQuestionText?.trim()) ||
      watchedOptions.some(o => Boolean(o.optionText?.trim()));

    if (isDirty && hasUnsavedContent) {
      setUnsavedDoneConfirm(true);
    } else {
      navigate(chapterId ? `/admin/question-bank?chapterId=${chapterId}` : '/admin/question-bank');
    }
  }

  // Answered question locking
  const isAnsweredLocked = Boolean(isEditMode && existingQuestion && existingQuestion.answerCount > 0);

  // Archive and create replacement action
  async function handleArchiveAndCreateReplacement() {
    if (!existingQuestion || !questionId) return;
    setArchiving(true);
    try {
      await updateAdminQuestionStatus(questionId, { status: 'ARCHIVED' });
      // Navigate to create form with pre-filled state (via location state or params)
      navigate(`/admin/question-bank/new?chapterId=${chapterId || ''}`, {
        state: {
          prefill: {
            questionText: existingQuestion.questionText,
            difficulty: existingQuestion.difficulty,
            language: existingQuestion.language,
            explanation: existingQuestion.explanation,
            topicId: existingQuestion.topicId,
            options: existingQuestion.options.map(o => ({
              optionText: o.text,
              isCorrect: o.isCorrect,
            })),
          },
        },
      });
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setArchiving(false);
      setArchiveAndReplaceConfirm(false);
    }
  }

  if (loadingInitial) {
    return (
      <div className="py-24 text-center text-muted-foreground text-xs">
        <Loader2 size={24} className="animate-spin mx-auto mb-2 text-primary" />
        <span>Loading question details…</span>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Top Navigation */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => {
              if (!isEditMode && (sessionCreatedCount > 0 || isDirty)) {
                handleDone();
              } else {
                navigate(-1);
              }
            }}
            className="p-2 rounded-xl border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
            title="Go back"
          >
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className="text-xl font-black text-foreground tracking-tight">
              {isEditMode ? `Edit Question #${questionId}` : 'Author New Question'}
            </h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              {isEditMode
                ? 'Update wording, options, difficulty, and topics.'
                : 'Compose options, select the correct answer, and keep adding questions for this chapter.'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {!isEditMode && (
            <button
              type="button"
              onClick={handleDone}
              className={`px-4 py-2 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 shadow-xs ${
                sessionCreatedCount > 0
                  ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                  : 'border border-border text-foreground hover:bg-muted'
              }`}
            >
              <CheckCircle2 size={14} />
              <span>{sessionCreatedCount > 0 ? `Done (${sessionCreatedCount} added)` : 'Done'}</span>
            </button>
          )}

          {isEditMode && existingQuestion && (
            <div className="flex items-center gap-2">
              <StatusBadge status={existingQuestion.status} size="md" />
              <SourceBadge source={existingQuestion.source} size="md" />
            </div>
          )}
        </div>
      </div>

      {/* Target Chapter Information & Selection Banner */}
      {!isEditMode && (
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="h-9 w-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary shrink-0">
                <BookOpen size={18} />
              </div>
              <div>
                <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider block">
                  Target Chapter
                </span>
                <span className="text-sm font-extrabold text-foreground">
                  {currentChapter?.name || (chapterId ? `Chapter #${chapterId}` : 'No Chapter Selected')}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowChangeChapter(prev => !prev)}
              className="text-xs font-semibold text-primary hover:underline"
            >
              {showChangeChapter ? 'Close Chapter Selector' : chapterId ? 'Change Chapter' : 'Select Chapter'}
            </button>
          </div>

          {(showChangeChapter || !chapterId) && (
            <div className="pt-3 border-t border-border/60">
              <p className="text-[11px] text-muted-foreground mb-2">
                All questions added in this session will belong to this chapter:
              </p>
              <ChapterPicker
                allowAllChapters={false}
                allowAllSubjects={false}
                syncWithUrl={true}
                selectedChapterId={chapterId}
                onSelectionChange={(newChId) => {
                  if (newChId && newChId !== chapterId) {
                    setChapterId(newChId);
                    setSearchParams({ chapterId: String(newChId) });
                    setValue('topicId', null, { shouldDirty: true });
                    setShowChangeChapter(false);
                  }
                }}
              />
            </div>
          )}
        </div>
      )}

      {/* Session progress indicator */}
      {!isEditMode && sessionCreatedCount > 0 && (
        <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-xs flex flex-wrap items-center justify-between gap-3 text-emerald-950 dark:text-emerald-200 animate-in fade-in duration-200">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 size={18} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
            <div>
              <p className="font-bold text-foreground">
                Continuous Question Creation Active
              </p>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                Added <strong className="text-foreground">{sessionCreatedCount}</strong> question{sessionCreatedCount !== 1 ? 's' : ''} in this batch. Keep entering new questions or click Done when finished.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleDone}
            className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs transition shadow-xs flex items-center gap-1.5"
          >
            <CheckCircle2 size={13} />
            <span>Finish &amp; Go to Question Bank ({sessionCreatedCount})</span>
          </button>
        </div>
      )}

      {actionSuccess && (
        <FeedbackAlert type="success" message={actionSuccess} onClose={() => setActionSuccess(null)} />
      )}
      {pageError && (
        <FeedbackAlert type="error" message={pageError} onClose={() => setPageError(null)} />
      )}

      {/* Answer count locked warning banner */}
      {isAnsweredLocked && (
        <div className="rounded-2xl border border-indigo-500/30 bg-indigo-500/10 p-4 text-xs text-indigo-950 dark:text-indigo-200 space-y-2">
          <div className="flex items-start gap-2.5">
            <Lock size={18} className="shrink-0 text-indigo-600 dark:text-indigo-400 mt-0.5" />
            <div className="leading-relaxed">
              <strong className="font-bold text-foreground">Option Immutability Enforced: </strong>
              This question already has <span className="font-bold text-foreground">{existingQuestion?.answerCount}</span> student answers. You can fix typos in the question text and explanation, but to change the correct option or add/remove options, you must archive this question and create a replacement.
            </div>
          </div>
          <div className="pt-2 pl-7 flex items-center gap-3">
            <button
              type="button"
              onClick={() => setArchiveAndReplaceConfirm(true)}
              className="px-3 py-1.5 rounded-xl bg-indigo-600 text-white font-semibold hover:bg-indigo-700 transition flex items-center gap-1.5 shadow-xs"
            >
              <Archive size={13} />
              <span>Archive and create replacement</span>
            </button>
          </div>
        </div>
      )}

      {/* Editing approved note */}
      {isEditMode && existingQuestion?.status === 'APPROVED' && (
        <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-xs text-emerald-800 dark:text-emerald-300 flex items-center gap-2">
          <CheckCircle2 size={15} />
          <span>This question is currently <strong>APPROVED</strong>. Saving changes will keep it approved and live.</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Main Form (2 cols) */}
        <form className="lg:col-span-2 space-y-5">
          <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-5">
            {/* Question Text */}
            <div>
              <label htmlFor="q-text" className="text-xs font-bold text-foreground mb-1 block">
                Question Text <span className="text-destructive">*</span>
              </label>
              {(() => {
                const { ref, ...rest } = register('questionText');
                return (
                  <textarea
                    id="q-text"
                    rows={3}
                    placeholder="Enter the question text…"
                    {...rest}
                    ref={(e) => {
                      ref(e);
                      questionTextRef.current = e;
                    }}
                    className="w-full rounded-xl border border-border bg-background p-3 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                  />
                );
              })()}
              {errors.questionText && (
                <p className="text-[11px] text-destructive mt-1">{errors.questionText.message}</p>
              )}
            </div>

            {/* Options List */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <label className="text-xs font-bold text-foreground block">
                    Answer Options (2 to 6) <span className="text-destructive">*</span>
                  </label>
                  <p className="text-[11px] text-muted-foreground mt-0.5">
                    Click the radio button to select the correct answer.
                  </p>
                </div>

                {!isAnsweredLocked && fields.length < 6 && (
                  <button
                    type="button"
                    onClick={() => append({ optionText: '', isCorrect: false })}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-lg border border-border text-xs font-semibold text-primary hover:bg-primary/5 transition"
                  >
                    <Plus size={13} />
                    <span>Add Option</span>
                  </button>
                )}
              </div>

              {errors.options?.root && (
                <p className="text-[11px] text-destructive">{errors.options.root.message}</p>
              )}
              {errors.options?.message && (
                <p className="text-[11px] text-destructive">{errors.options.message}</p>
              )}

              <div className="space-y-2.5">
                {fields.map((field, index) => {
                  const isCorrect = watchedOptions?.[index]?.isCorrect;

                  return (
                    <div
                      key={field.id}
                      className={`flex items-center gap-3 p-3 rounded-xl border transition ${
                        isCorrect
                          ? 'border-emerald-500/40 bg-emerald-500/5 dark:bg-emerald-500/10'
                          : 'border-border bg-background/50'
                      }`}
                    >
                      {/* Correct Option Radio */}
                      <button
                        type="button"
                        disabled={isAnsweredLocked}
                        onClick={() => setCorrectOption(index)}
                        className={`h-5 w-5 rounded-full border flex items-center justify-center transition shrink-0 ${
                          isCorrect
                            ? 'border-emerald-600 bg-emerald-600 text-white'
                            : 'border-border hover:border-emerald-500/60'
                        } disabled:cursor-not-allowed`}
                        title={isCorrect ? 'Correct option' : 'Mark as correct option'}
                      >
                        {isCorrect && <span className="h-2 w-2 rounded-full bg-white" />}
                      </button>

                      <span className="text-xs font-mono font-bold text-muted-foreground w-4">
                        {String.fromCharCode(65 + index)}.
                      </span>

                      {/* Option Text Input */}
                      <div className="flex-1">
                        <input
                          type="text"
                          placeholder={`Option ${String.fromCharCode(65 + index)} text…`}
                          {...register(`options.${index}.optionText`)}
                          className="w-full rounded-lg border border-border bg-background px-3 py-1.5 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                        />
                        {errors.options?.[index]?.optionText && (
                          <p className="text-[10px] text-destructive mt-0.5">
                            {errors.options[index]?.optionText?.message}
                          </p>
                        )}
                      </div>

                      {/* Delete Option button */}
                      {!isAnsweredLocked && fields.length > 2 && (
                        <button
                          type="button"
                          onClick={() => remove(index)}
                          className="p-1.5 rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition shrink-0"
                          title="Remove option"
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Explanation */}
            <div>
              <label htmlFor="q-explanation" className="text-xs font-bold text-foreground mb-1 block">
                Explanation <span className="text-muted-foreground font-normal">(optional)</span>
              </label>
              <textarea
                id="q-explanation"
                rows={3}
                placeholder="Explain why the correct answer is right and why distractors are wrong…"
                {...register('explanation')}
                className="w-full rounded-xl border border-border bg-background p-3 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              />
            </div>
          </div>

          {/* Form Actions */}
          <div className="rounded-2xl border border-border bg-card p-4 shadow-sm flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleDone}
                className={`px-4 py-2 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 ${
                  sessionCreatedCount > 0
                    ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs'
                    : 'border border-border text-muted-foreground hover:bg-muted'
                }`}
              >
                <CheckCircle2 size={14} />
                <span>
                  {sessionCreatedCount > 0
                    ? `Done (${sessionCreatedCount} added)`
                    : isEditMode
                    ? 'Cancel'
                    : 'Done / Cancel'}
                </span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              {!isEditMode && (
                <button
                  type="button"
                  disabled={saving || !chapterId}
                  onClick={handleSubmit(data => onSaveSubmit(data, 'PENDING'))}
                  className="px-4 py-2 rounded-xl border border-border bg-card text-foreground text-xs font-semibold hover:bg-muted transition flex items-center gap-1.5 disabled:opacity-50"
                  title="Save question as pending and start next question"
                >
                  {saving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                  <span>Save as Pending</span>
                </button>
              )}

              <button
                type="button"
                disabled={saving || (!isEditMode && !chapterId)}
                onClick={handleSubmit(data => onSaveSubmit(data, 'APPROVED'))}
                className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs flex items-center gap-1.5 disabled:opacity-50"
                title={isEditMode ? 'Save changes' : 'Save & approve question and start next question'}
              >
                {saving ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : (
                  <CheckCircle2 size={14} />
                )}
                <span>{isEditMode ? 'Save Changes' : 'Save & Approve'}</span>
              </button>
            </div>
          </div>
        </form>

        {/* Settings & Read-Only Metadata (1 col) */}
        <div className="space-y-5">
          {/* Categorization & Metadata */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
            <h3 className="text-xs font-bold text-foreground uppercase tracking-wider">
              Categorization
            </h3>

            {/* Topic Select */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold text-foreground">
                  Topic <span className="text-[10px] text-amber-600 font-normal">(Required for Approved)</span>
                </label>
                {chapterId && !showNewTopicInput && (
                  <button
                    type="button"
                    onClick={() => setShowNewTopicInput(true)}
                    className="text-[11px] font-semibold text-primary hover:underline flex items-center gap-0.5"
                  >
                    <Plus size={11} />
                    <span>New Topic</span>
                  </button>
                )}
              </div>

              {showNewTopicInput ? (
                <div className="flex items-center gap-1.5 my-1">
                  <input
                    type="text"
                    placeholder="Topic name…"
                    value={newTopicName}
                    onChange={e => setNewTopicName(e.target.value)}
                    className="w-full rounded-lg border border-border bg-background px-2.5 py-1 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                  />
                  <button
                    type="button"
                    disabled={creatingTopic || !newTopicName.trim()}
                    onClick={handleQuickCreateTopic}
                    className="px-2.5 py-1 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition disabled:opacity-50 shrink-0"
                  >
                    {creatingTopic ? <Loader2 size={12} className="animate-spin" /> : 'Create'}
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowNewTopicInput(false); setNewTopicName(''); }}
                    className="px-1 text-muted-foreground hover:text-foreground text-xs"
                  >
                    Cancel
                  </button>
                </div>
              ) : null}

              <select
                value={watchedTopicId ?? ''}
                onChange={e => setValue('topicId', e.target.value ? Number(e.target.value) : null, { shouldDirty: true })}
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              >
                <option value="">No Topic (General Chapter Question)</option>
                {topics.map(t => (
                  <option key={t.topicId} value={t.topicId} disabled={t.status === 'INACTIVE'}>
                    {t.name} {t.status === 'INACTIVE' ? '(Inactive)' : ''}
                  </option>
                ))}
              </select>
              {errors.topicId && (
                <p className="text-[11px] text-destructive mt-1">{errors.topicId.message}</p>
              )}
            </div>

            {/* Difficulty */}
            <div>
              <label className="text-xs font-semibold text-foreground mb-1 block">Difficulty</label>
              <select
                {...register('difficulty')}
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>

            {/* Language */}
            <div>
              <label className="text-xs font-semibold text-foreground mb-1 block">Language</label>
              <select
                {...register('language')}
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
              >
                <option value="EN">English</option>
                <option value="SI">Sinhala</option>
                <option value="TA">Tamil</option>
              </select>
            </div>
          </div>

          {/* Read-Only Side Panel (Edit Mode) */}
          {isEditMode && existingQuestion && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-3.5 text-xs">
              <h3 className="font-bold text-foreground uppercase tracking-wider text-[11px]">
                Question Metadata
              </h3>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Status:</span>
                <StatusBadge status={existingQuestion.status} />
              </div>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Source:</span>
                <SourceBadge source={existingQuestion.source} />
              </div>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Author:</span>
                <span className="font-medium text-foreground">
                  {existingQuestion.createdByName || `Admin #${existingQuestion.createdBy ?? '—'}`}
                </span>
              </div>

              {existingQuestion.approvedBy && (
                <div className="flex items-center justify-between py-1 border-b border-border/60">
                  <span className="text-muted-foreground">Approved by:</span>
                  <span className="font-medium text-foreground">
                    {existingQuestion.approvedByName || `Admin #${existingQuestion.approvedBy}`}
                  </span>
                </div>
              )}

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Student Answers:</span>
                <span className="font-semibold text-foreground">{existingQuestion.answerCount}</span>
              </div>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Correct Rate:</span>
                <span className="font-semibold text-foreground">
                  {existingQuestion.correctRate !== null ? `${existingQuestion.correctRate}%` : '—'}
                </span>
              </div>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Open Reports:</span>
                {existingQuestion.openReportCount > 0 ? (
                  <Link
                    to={`/admin/question-reports?questionId=${existingQuestion.questionId}`}
                    className="font-bold text-rose-600 dark:text-rose-400 hover:underline inline-flex items-center gap-1"
                  >
                    <Flag size={12} />
                    <span>{existingQuestion.openReportCount} reports</span>
                  </Link>
                ) : (
                  <span className="text-muted-foreground">0</span>
                )}
              </div>

              {/* AI Details if present */}
              {existingQuestion.sourceExcerpt && (
                <div className="pt-2">
                  <span className="text-muted-foreground block text-[11px] font-semibold mb-1">
                    Source Excerpt:
                  </span>
                  <p className="p-2.5 rounded-xl border border-border bg-muted/40 text-[11px] text-muted-foreground italic leading-relaxed">
                    "{existingQuestion.sourceExcerpt}"
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Archive and Replace Confirmation Dialog */}
      {archiveAndReplaceConfirm && (
        <ConfirmDialog
          isOpen={true}
          title="Archive Question and Create Replacement?"
          description="This action will mark the current question as ARCHIVED (preserving existing student attempt records) and open the creation form pre-filled with this question's text and options so you can safely modify the answers."
          confirmLabel="Archive and Continue"
          confirmVariant="primary"
          isLoading={archiving}
          onConfirm={handleArchiveAndCreateReplacement}
          onCancel={() => setArchiveAndReplaceConfirm(false)}
        />
      )}

      {/* Unsaved Changes on Done Confirmation Dialog */}
      {unsavedDoneConfirm && (
        <ConfirmDialog
          isOpen={true}
          title="Unsaved Question in Progress"
          description={
            sessionCreatedCount > 0
              ? `You have typed content for another question that has not been saved yet. You've already saved ${sessionCreatedCount} question${sessionCreatedCount !== 1 ? 's' : ''} in this batch. Discard this unfinished question and finish?`
              : "You have typed content for a question that has not been saved yet. Discard changes and return to the question bank?"
          }
          confirmLabel="Discard & Exit"
          confirmVariant="destructive"
          onConfirm={() => {
            setUnsavedDoneConfirm(false);
            navigate(chapterId ? `/admin/question-bank?chapterId=${chapterId}` : '/admin/question-bank');
          }}
          onCancel={() => setUnsavedDoneConfirm(false)}
        />
      )}
    </div>
  );
}
