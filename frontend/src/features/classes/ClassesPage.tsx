import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  getPublicClasses,
  type PublicClassResponse,
  type ClassType,
  type PageResponse,
} from '../../api/classApi';
import { getPublicSubjects, type SubjectResponse } from '../../api/subjectApi';
import {
  GraduationCap,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Loader2,
  AlertCircle,
  ArrowRight,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';

function formatLKR(fee: number): string {
  return `LKR ${Number(fee).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function getClassTypeBadge(type: ClassType) {
  switch (type) {
    case 'PHYSICAL':
      return {
        label: 'Physical Class',
        style: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
      };
    case 'ONLINE':
      return {
        label: 'Online Class',
        style: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20',
      };
    case 'HYBRID':
      return {
        label: 'Hybrid Class',
        style: 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20',
      };
    default:
      return {
        label: type,
        style: 'bg-muted text-muted-foreground border-border',
      };
  }
}

export default function ClassesPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [data, setData] = useState<PageResponse<PublicClassResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const subjectIdParam = searchParams.get('subjectId');
  const typeParam = (searchParams.get('class_type') as ClassType) || '';
  const pageParam = Math.max(0, parseInt(searchParams.get('page') || '0', 10));

  useEffect(() => {
    getPublicSubjects()
      .then(setSubjects)
      .catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function fetchClasses() {
      setLoading(true);
      setError(null);
      try {
        const res = await getPublicClasses({
          page: pageParam,
          size: 9,
          subjectId: subjectIdParam ? parseInt(subjectIdParam, 10) : undefined,
          class_type: typeParam || undefined,
        });
        if (!cancelled) {
          setData(res);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const e = err as { message?: string };
          setError(e?.message || 'Failed to load classes.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    fetchClasses();
    return () => {
      cancelled = true;
    };
  }, [subjectIdParam, typeParam, pageParam]);

  function updateFilter(updates: { subjectId?: string | null; class_type?: string | null; page?: number | null }) {
    const next = new URLSearchParams(searchParams);
    if (updates.subjectId !== undefined) {
      if (updates.subjectId) next.set('subjectId', updates.subjectId);
      else next.delete('subjectId');
      next.delete('page');
    }
    if (updates.class_type !== undefined) {
      if (updates.class_type) next.set('class_type', updates.class_type);
      else next.delete('class_type');
      next.delete('page');
    }
    if (updates.page !== undefined) {
      if (updates.page !== null && updates.page > 0) next.set('page', updates.page.toString());
      else next.delete('page');
    }
    setSearchParams(next);
  }

  return (
    <PageContainer>
      <PageHeader
        title="Tuition & Masterclasses"
        description="Explore active physical, online, and hybrid tuition classes conducted by top A/L teachers. Register online to reserve your seat."
      />

      {/* Filter Bar */}
      <div className="flex flex-wrap gap-3 mb-8 items-center bg-card p-4 rounded-2xl border border-border shadow-xs">
        {/* Subject Filter */}
        <div className="flex items-center gap-2 min-w-[200px]">
          <label htmlFor="class-subject-filter" className="text-xs font-medium text-muted-foreground">
            Subject:
          </label>
          <select
            id="class-subject-filter"
            value={subjectIdParam || ''}
            onChange={(e) => updateFilter({ subjectId: e.target.value || null })}
            className="w-full rounded-xl border border-border bg-background text-sm text-foreground px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer"
          >
            <option value="">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.name}
              </option>
            ))}
          </select>
        </div>

        {/* Type Filter */}
        <div className="flex items-center gap-2 min-w-[180px]">
          <label htmlFor="class-type-filter" className="text-xs font-medium text-muted-foreground">
            Type:
          </label>
          <select
            id="class-type-filter"
            value={typeParam}
            onChange={(e) => updateFilter({ class_type: e.target.value || null })}
            className="w-full rounded-xl border border-border bg-background text-sm text-foreground px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary/30 cursor-pointer"
          >
            <option value="">All Types</option>
            <option value="PHYSICAL">Physical</option>
            <option value="ONLINE">Online</option>
            <option value="HYBRID">Hybrid</option>
          </select>
        </div>

        {/* Clear Filters */}
        {(subjectIdParam || typeParam) && (
          <button
            type="button"
            onClick={() => {
              setSearchParams({});
            }}
            className="text-xs font-medium text-muted-foreground hover:text-destructive transition px-3 py-2 rounded-xl border border-border hover:border-destructive/30"
          >
            Clear Filters
          </button>
        )}

        {data && (
          <div className="ml-auto text-xs text-muted-foreground">
            Showing <span className="font-medium text-foreground">{data.totalElements}</span> class{data.totalElements === 1 ? '' : 'es'}
          </div>
        )}
      </div>

      {/* Loading State */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
          <Loader2 size={32} className="animate-spin text-primary mb-3" />
          <p className="text-sm">Loading available classes...</p>
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="flex items-center gap-3 p-4 rounded-2xl bg-destructive/10 border border-destructive/20 text-destructive text-sm max-w-xl mx-auto my-12">
          <AlertCircle size={20} className="shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && data && data.content.length === 0 && (
        <div className="text-center py-20 border border-dashed border-border rounded-3xl bg-card/40 max-w-xl mx-auto my-8">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground mb-4">
            <GraduationCap size={28} />
          </div>
          <h3 className="text-lg font-bold text-foreground">No classes found</h3>
          <p className="text-sm text-muted-foreground mt-1 max-w-sm mx-auto">
            {subjectIdParam || typeParam
              ? 'Try adjusting your subject or class type filters to find what you are looking for.'
              : 'There are currently no active classes scheduled. Please check back soon!'}
          </p>
          {(subjectIdParam || typeParam) && (
            <button
              type="button"
              onClick={() => setSearchParams({})}
              className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold shadow-xs hover:opacity-90 transition"
            >
              Reset Filters
            </button>
          )}
        </div>
      )}

      {/* Classes Grid */}
      {!loading && !error && data && data.content.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {data.content.map((cls) => {
            const badge = getClassTypeBadge(cls.classType);
            return (
              <Link
                key={cls.classId}
                to={`/classes/${cls.classId}`}
                id={`class-card-${cls.classId}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-6 shadow-xs hover:shadow-lg hover:border-primary/40 transition-all duration-200"
              >
                <div className="flex items-start justify-between gap-3 mb-3">
                  <span className="text-xs font-semibold text-primary uppercase tracking-wide">
                    {cls.subject.name}
                  </span>
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${badge.style}`}
                  >
                    {badge.label}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-foreground group-hover:text-primary transition line-clamp-1 mb-2">
                  {cls.name}
                </h3>

                {cls.description && (
                  <p className="text-xs text-muted-foreground line-clamp-2 mb-4">
                    {cls.description}
                  </p>
                )}

                <div className="mt-auto space-y-2.5 pt-4 border-t border-border/60">
                  <div className="flex items-start gap-2 text-xs text-muted-foreground">
                    <Calendar size={14} className="shrink-0 mt-0.5 text-primary/70" />
                    <span className="line-clamp-2 font-medium">{cls.scheduleDescription}</span>
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <div>
                      <span className="text-[10px] uppercase font-semibold text-muted-foreground tracking-wider block">
                        Monthly Fee
                      </span>
                      <span className="text-base font-extrabold text-foreground">
                        {formatLKR(cls.fee)}
                      </span>
                    </div>

                    <span className="inline-flex items-center gap-1 text-xs font-semibold text-primary group-hover:translate-x-0.5 transition-transform">
                      Details
                      <ArrowRight size={13} />
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {!loading && !error && data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-10">
          <button
            type="button"
            disabled={data.first}
            onClick={() => updateFilter({ page: pageParam - 1 })}
            className="inline-flex items-center gap-1 px-3 py-2 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition"
          >
            <ChevronLeft size={14} />
            Previous
          </button>

          <span className="text-xs font-medium text-muted-foreground px-3">
            Page {data.number + 1} of {data.totalPages}
          </span>

          <button
            type="button"
            disabled={data.last}
            onClick={() => updateFilter({ page: pageParam + 1 })}
            className="inline-flex items-center gap-1 px-3 py-2 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition"
          >
            Next
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </PageContainer>
  );
}
