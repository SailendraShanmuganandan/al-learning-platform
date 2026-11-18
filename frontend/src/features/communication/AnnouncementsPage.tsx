import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPublishedAnnouncements, type AnnouncementListItemResponse } from '../../api/communicationApi';
import type { PageResponse } from '../../api/classApi';
import {
  Bell,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Loader2,
  ArrowRight,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';

export default function AnnouncementsPage() {
  const [pageData, setPageData] = useState<PageResponse<AnnouncementListItemResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadAnnouncements(currentPage);
  }, [currentPage]);

  async function loadAnnouncements(page: number) {
    setLoading(true);
    setError(null);
    try {
      const data = await getPublishedAnnouncements(page, 9);
      setPageData(data);
    } catch {
      setError('Unable to load announcements at this time. Please try again later.');
    } finally {
      setLoading(false);
    }
  }

  function formatDate(iso: string) {
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return iso;
    }
  }

  return (
    <PageContainer>
      <PageHeader
        title="Announcements"
        description="Stay up to date with class schedules, examination notices, platform updates, and revision milestones."
      />

      {/* Content list */}
      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center gap-3">
          <Loader2 size={32} className="animate-spin text-primary" />
          <p className="text-xs text-muted-foreground">Loading announcements…</p>
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 text-center text-destructive text-sm">
          {error}
        </div>
      ) : !pageData || pageData.content.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-border bg-card/50 p-12 text-center space-y-3">
          <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
            <Bell size={28} />
          </div>
          <h3 className="text-base font-semibold text-foreground">No announcements yet</h3>
          <p className="text-xs text-muted-foreground max-w-sm mx-auto">
            There are currently no published announcements. Please check back soon for the latest curriculum news.
          </p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {pageData.content.map((item) => (
              <Link
                key={item.id}
                to={`/announcements/${item.id}`}
                id={`announcement-card-${item.id}`}
                className="group rounded-2xl border border-border bg-card p-6 shadow-xs hover:border-primary/40 hover:shadow-md transition flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-[11px] font-medium text-muted-foreground">
                    <Calendar size={13} className="text-primary" />
                    <span>{formatDate(item.publishedAt)}</span>
                  </div>

                  <h2 className="text-base font-bold text-foreground group-hover:text-primary transition-colors line-clamp-2">
                    {item.title}
                  </h2>

                  <p className="text-xs text-muted-foreground line-clamp-4 leading-relaxed whitespace-pre-wrap">
                    {item.excerpt}
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-border flex items-center justify-between text-xs text-primary font-semibold">
                  <span>Read full announcement</span>
                  <ArrowRight size={13} className="group-hover:translate-x-0.5 transition-transform" />
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination Controls */}
          {pageData.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 pt-6">
              <button
                type="button"
                disabled={pageData.first}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <ChevronLeft size={14} />
                <span>Previous</span>
              </button>

              <span className="text-xs text-muted-foreground">
                Page {pageData.number + 1} of {pageData.totalPages}
              </span>

              <button
                type="button"
                disabled={pageData.last}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <span>Next</span>
                <ChevronRight size={14} />
              </button>
            </div>
          )}
        </>
      )}
    </PageContainer>
  );
}
