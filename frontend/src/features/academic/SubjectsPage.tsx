import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPublicSubjects, type SubjectResponse } from '../../api/subjectApi';
import type { ApiError } from '../../api/authApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  BookOpen,
  Search,
  ArrowRight,
  Layers,
  Sparkles,
  Loader2,
} from 'lucide-react';

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getPublicSubjects();
        setSubjects(data);
      } catch (err) {
        const apiErr = err as ApiError;
        setError(apiErr?.message || 'Failed to load subjects.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const filtered = subjects.filter((s) => {
    const q = searchQuery.toLowerCase();
    return s.name.toLowerCase().includes(q) || (s.description && s.description.toLowerCase().includes(q));
  });

  return (
    <PageContainer>
      <div className="space-y-8">
        <PageHeader
          title="Curriculum & Subjects"
          description="Browse subjects and dive into chapters, video lessons, and revision quizzes."
        />

        {/* Hero Section */}
        <div className="relative rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-6 sm:p-8 overflow-hidden shadow-xs">
          <div className="max-w-2xl space-y-3 relative z-10">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <Sparkles size={13} />
              <span>G.C.E. Advanced Level Curriculum</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
              Explore Academic Subjects
            </h2>
            <p className="text-muted-foreground text-xs sm:text-sm leading-relaxed">
              Discover comprehensive subject streams, interactive past papers, and expert learning materials tailored for your A/L examination success.
            </p>
          </div>

          <div className="mt-5 max-w-md relative z-10">
            <div className="relative">
              <Search
                size={16}
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"
              />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search subjects by title or description..."
                className="w-full rounded-xl border border-border bg-background py-2.5 pl-10 pr-4 text-xs text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary shadow-xs transition"
              />
            </div>
          </div>
        </div>

        {/* Content */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-24 text-center">
            <Loader2 className="animate-spin text-primary" size={32} />
            <p className="mt-3 text-xs text-muted-foreground">Loading subjects...</p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center text-xs text-destructive">
            <p className="font-semibold">{error}</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-card/40 py-16 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-muted-foreground mb-3">
              <BookOpen size={20} />
            </div>
            <h3 className="font-semibold text-sm text-foreground">No subjects found</h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-sm">
              {searchQuery
                ? `We couldn't find any subject matching "${searchQuery}". Try different keywords.`
                : 'There are no active subjects published right now. Please check back later.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((subject) => (
              <Link
                key={subject.subjectId}
                to={`/subjects/${subject.subjectId}`}
                className="group relative flex flex-col justify-between rounded-2xl border border-border bg-card p-6 shadow-xs hover:border-primary/50 hover:shadow-md transition duration-200"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary group-hover:bg-primary group-hover:text-primary-foreground transition-colors duration-200">
                      <BookOpen size={20} />
                    </div>
                    <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                      <Layers size={11} />
                      <span>{(subject as any).chaptersCount != null ? `${(subject as any).chaptersCount} Chapters` : 'Syllabus'}</span>
                    </span>
                  </div>

                  <div>
                    <h3 className="text-base font-bold text-foreground group-hover:text-primary transition-colors">
                      {subject.name}
                    </h3>
                    <p className="mt-2 text-xs text-muted-foreground line-clamp-3 leading-relaxed">
                      {subject.description || 'Comprehensive study modules, video lessons, and revision quizzes are prepared for this subject.'}
                    </p>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
                  <span className="text-[11px]">A/L Level</span>
                  <div className="inline-flex items-center gap-1 font-semibold text-primary group-hover:translate-x-0.5 transition-transform">
                    <span>View Chapters</span>
                    <ArrowRight size={13} />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
