import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getPublicSubjectById, type SubjectResponse } from '../../api/subjectApi';
import { getPublicChapters, type ChapterResponse } from '../../api/chapterApi';
import type { ApiError } from '../../api/authApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  BookOpen,
  ArrowRight,
  Layers,
  Loader2,
  AlertCircle,
  ListOrdered,
} from 'lucide-react';

export default function SubjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [subject, setSubject] = useState<SubjectResponse | null>(null);
  const [chapters, setChapters] = useState<ChapterResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!id) {
      navigate('/subjects', { replace: true });
      return;
    }
    const numId = Number(id);
    if (isNaN(numId) || numId <= 0) {
      setNotFound(true);
      setLoading(false);
      return;
    }

    async function load() {
      setLoading(true);
      setError(null);
      setNotFound(false);
      try {
        const [subData, chapData] = await Promise.all([
          getPublicSubjectById(numId),
          getPublicChapters(numId).catch(() => []),
        ]);
        setSubject(subData);
        const sorted = [...chapData].sort(
          (a, b) => a.chapterOrder - b.chapterOrder
        );
        setChapters(sorted);
      } catch (err) {
        const apiErr = err as ApiError;
        if (apiErr?.status === 404) {
          setNotFound(true);
        } else {
          setError(apiErr?.message || 'Failed to load subject details.');
        }
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, navigate]);

  return (
    <PageContainer>
      <div className="space-y-8">
        <PageHeader
          title={subject?.name ?? 'Subject Details'}
          description={subject?.description ?? 'Explore subject chapters, notes and quizzes.'}
          breadcrumb={[
            { label: 'Subjects', path: '/subjects' },
            { label: subject?.name ?? 'Subject' },
          ]}
        />

        {/* Loading state */}
        {loading && (
          <div className="py-24 flex flex-col items-center justify-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading subject…</p>
          </div>
        )}

        {/* Not found */}
        {!loading && notFound && (
          <div className="rounded-3xl border border-border bg-card p-12 text-center space-y-4">
            <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <AlertCircle size={28} />
            </div>
            <h2 className="text-lg font-bold text-foreground">Subject Not Found</h2>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              This subject doesn't exist or is no longer available.
            </p>
            <Link
              to="/subjects"
              className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 transition"
            >
              Back to Subjects
            </Link>
          </div>
        )}

        {/* Error state */}
        {!loading && error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 text-center text-destructive text-sm">
            {error}
          </div>
        )}

        {/* Subject detail */}
        {!loading && subject && (
          <>
            {/* Subject hero */}
            <div className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 sm:p-8 shadow-xs space-y-3">
              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <Layers size={22} />
                </div>
                <div className="flex-1 min-w-0">
                  <h2 className="text-xl sm:text-2xl font-extrabold tracking-tight text-foreground">
                    {subject.name}
                  </h2>
                  {subject.description && (
                    <p className="mt-2 text-xs sm:text-sm text-muted-foreground leading-relaxed">
                      {subject.description}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Chapters section */}
            <section aria-labelledby="chapters-heading" className="space-y-4">
              <div className="flex items-center gap-2">
                <ListOrdered size={18} className="text-primary" />
                <h3
                  id="chapters-heading"
                  className="text-base font-bold text-foreground"
                >
                  Chapters
                </h3>
                <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
                  {chapters.length}
                </span>
              </div>

              {chapters.length === 0 ? (
                <div className="rounded-3xl border border-border bg-card p-10 text-center space-y-3">
                  <div className="mx-auto h-12 w-12 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
                    <BookOpen size={24} />
                  </div>
                  <h4 className="text-sm font-semibold text-foreground">No chapters yet</h4>
                  <p className="text-xs text-muted-foreground max-w-xs mx-auto">
                    Chapters for this subject haven't been published yet. Please check back later.
                  </p>
                </div>
              ) : (
                <ol className="space-y-3">
                  {chapters.map((chapter) => (
                    <li key={chapter.chapterId}>
                      <Link
                        to={`/chapters/${chapter.chapterId}`}
                        id={`chapter-card-${chapter.chapterId}`}
                        className="group flex items-center gap-4 rounded-2xl border border-border bg-card px-5 py-4 shadow-xs hover:shadow-md hover:border-primary/40 transition-all"
                      >
                        {/* Order badge */}
                        <div className="flex-none h-9 w-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center text-sm font-bold group-hover:bg-primary group-hover:text-primary-foreground transition-colors">
                          {chapter.chapterOrder}
                        </div>

                        <div className="flex-1 min-w-0">
                          <p className="font-semibold text-sm text-foreground group-hover:text-primary transition-colors truncate">
                            {chapter.name}
                          </p>
                          {chapter.description && (
                            <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">
                              {chapter.description}
                            </p>
                          )}
                        </div>

                        <ArrowRight
                          size={16}
                          className="flex-none text-muted-foreground group-hover:text-primary group-hover:translate-x-0.5 transition-all"
                        />
                      </Link>
                    </li>
                  ))}
                </ol>
              )}
            </section>
          </>
        )}
      </div>
    </PageContainer>
  );
}
