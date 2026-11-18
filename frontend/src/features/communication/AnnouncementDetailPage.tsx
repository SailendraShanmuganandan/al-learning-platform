import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPublishedAnnouncement, type AnnouncementDetailResponse } from '../../api/communicationApi';
import {
  Bell,
  Calendar,
  ChevronLeft,
  Loader2,
  AlertCircle,
} from 'lucide-react';
import { PageContainer } from '../../components/layout/PageContainer';

export default function AnnouncementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [announcement, setAnnouncement] = useState<AnnouncementDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadAnnouncement(Number(id));
    }
  }, [id]);

  async function loadAnnouncement(announcementId: number) {
    setLoading(true);
    setError(null);
    try {
      const data = await getPublishedAnnouncement(announcementId);
      setAnnouncement(data);
    } catch {
      setError('Announcement not found or no longer available.');
    } finally {
      setLoading(false);
    }
  }

  function formatDate(iso: string) {
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return iso;
    }
  }

  return (
    <PageContainer>
      {loading ? (
        <div className="py-24 flex flex-col items-center justify-center gap-3">
          <Loader2 size={32} className="animate-spin text-primary" />
          <p className="text-xs text-muted-foreground">Loading announcement…</p>
        </div>
      ) : error || !announcement ? (
        <div className="rounded-3xl border border-destructive/30 bg-destructive/10 p-8 text-center space-y-4 max-w-md mx-auto my-12">
          <AlertCircle size={32} className="mx-auto text-destructive" />
          <h2 className="text-lg font-bold text-destructive">Announcement Not Found</h2>
          <p className="text-xs text-muted-foreground">
            {error || 'This announcement may have been archived or removed.'}
          </p>
          <Link
            to="/announcements"
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:opacity-90 transition"
          >
            <ChevronLeft size={14} />
            Return to Announcements
          </Link>
        </div>
      ) : (
        <article className="space-y-8 animate-in fade-in duration-200">
          {/* Header info */}
          <div className="space-y-4 border-b border-border pb-6">
            <div className="flex items-center gap-2 text-xs text-primary font-semibold">
              <Bell size={14} />
              <span>Curriculum &amp; Platform Notice</span>
            </div>

            <h1
              id="announcement-title"
              className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-foreground leading-tight"
            >
              {announcement.title}
            </h1>

            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Calendar size={14} className="text-muted-foreground/70" />
              <span>Published on {formatDate(announcement.publishedAt)}</span>
            </div>
          </div>

          {/* Plain text content with preserved line breaks (never HTML) */}
          <div
            id="announcement-content"
            className="bg-card rounded-2xl border border-border p-6 sm:p-8 shadow-xs text-foreground/90 font-sans text-sm sm:text-base leading-relaxed whitespace-pre-wrap select-text"
          >
            {announcement.content}
          </div>

          {/* Back action */}
          <div className="pt-4 flex items-center justify-between">
            <Link
              to="/announcements"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
            >
              <ChevronLeft size={14} />
              Back to all announcements
            </Link>
            <Link
              to="/contact"
              className="text-xs text-muted-foreground hover:text-foreground transition"
            >
              Have questions regarding this notice? Contact us &rarr;
            </Link>
          </div>
        </article>
      )}
    </PageContainer>
  );
}
