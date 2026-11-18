import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { videoApi, type MyVideoSummary } from '../../api/videoApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  Video,
  Loader2,
  AlertCircle,
  Clock,
  PlayCircle,
  ShoppingBag,
  FileText,
} from 'lucide-react';

function formatDuration(seconds: number | null | undefined): string {
  if (!seconds) return '';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('en-LK', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export default function MyVideosPage() {
  const [videos, setVideos] = useState<MyVideoSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    videoApi
      .getMyVideos()
      .then((data) => {
        if (!cancelled) setVideos(data);
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message || 'Failed to load your videos.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <PageContainer>
      <div className="space-y-6">
        <PageHeader
          title="My Video Library"
          description="Your purchased video courses and lessons."
        />

        {/* Loading */}
        {loading && (
          <div className="py-24 flex flex-col items-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading your library…</p>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 flex items-center gap-3 text-destructive text-sm">
            <AlertCircle size={18} />
            {error}
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && videos.length === 0 && (
          <div className="rounded-3xl border border-border bg-card p-16 text-center space-y-4">
            <div className="mx-auto h-16 w-16 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <Video size={32} />
            </div>
            <h2 className="text-lg font-bold text-foreground">No videos yet</h2>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              You haven't purchased any video courses yet. Browse our catalog to find courses that interest you.
            </p>
            <Link
              to="/products"
              id="browse-products-link"
              className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-sm font-semibold hover:opacity-90 transition"
            >
              <ShoppingBag size={15} />
              Browse Notes &amp; Videos
            </Link>
          </div>
        )}

        {/* Video grid */}
        {!loading && !error && videos.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {videos.map((video) => {
              const dur = video.durationSeconds ?? video.duration_seconds;
              const purchasedAt = video.purchasedAt ?? video.purchased_at;
              const thumbnailUrl = video.thumbnailUrl ?? video.thumbnail_url;

              return (
                <Link
                  key={video.productId}
                  to={`/my-videos/${video.productId}`}
                  id={`my-video-card-${video.productId}`}
                  className="group rounded-2xl border border-border bg-card overflow-hidden hover:border-primary/40 hover:shadow-md transition-all duration-200"
                >
                  {/* Thumbnail */}
                  <div className="aspect-video bg-muted relative overflow-hidden">
                    {thumbnailUrl ? (
                      <img
                        src={thumbnailUrl}
                        alt={video.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <Video size={40} className="text-muted-foreground/30" />
                      </div>
                    )}
                    {/* Play overlay */}
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-black/20">
                      <div className="rounded-full bg-white/90 p-3 shadow-lg">
                        <PlayCircle size={28} className="text-primary" />
                      </div>
                    </div>
                    {/* Duration badge */}
                    {dur && (
                      <div className="absolute bottom-2 right-2 flex items-center gap-1 rounded-md bg-black/70 text-white px-2 py-0.5 text-[11px] font-medium">
                        <Clock size={10} />
                        {formatDuration(dur)}
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="p-4 space-y-2">
                    <h3 className="text-sm font-semibold text-foreground line-clamp-2 group-hover:text-primary transition">
                      {video.name}
                    </h3>
                    {video.description && (
                      <p className="text-xs text-muted-foreground line-clamp-2">{video.description}</p>
                    )}
                    {purchasedAt && (
                      <p className="text-[10px] text-muted-foreground/70">
                        Purchased {formatDate(purchasedAt)}
                      </p>
                    )}
                    {video.hasAttachmentPdf && (
                      <span className="inline-flex items-center gap-1 text-[10px] text-primary font-medium">
                        <FileText size={10} /> PDF Notes Included
                      </span>
                    )}
                    <div className="pt-1">
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-primary">
                        <PlayCircle size={12} />
                        Watch Now
                      </span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
