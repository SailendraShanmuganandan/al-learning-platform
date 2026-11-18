import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { videoApi, type MyVideoDetail } from '../../api/videoApi';
import { PageContainer } from '../../components/layout/PageContainer';
import { PageHeader } from '../../components/layout/PageHeader';
import {
  Loader2,
  AlertCircle,
  Clock,
  Lock,
  RefreshCw,
  FileText,
  Download,
} from 'lucide-react';

function formatDuration(seconds: number | null | undefined): string {
  if (!seconds) return '';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatExpiry(iso: string | null | undefined): string {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString('en-LK', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function MyVideoPlayerPage() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();

  const [video, setVideo] = useState<MyVideoDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const productIdNum = productId ? parseInt(productId, 10) : null;

  function fetchPlayback(showRefreshing = false) {
    if (!productIdNum) return;

    if (showRefreshing) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);
    setErrorCode(null);

    videoApi
      .getMyVideoPlayback(productIdNum)
      .then((data) => {
        setVideo(data);
      })
      .catch((err: any) => {
        setError(err.message || 'Failed to load video player.');
        setErrorCode(err.code || null);
      })
      .finally(() => {
        setLoading(false);
        setRefreshing(false);
      });
  }

  useEffect(() => {
    if (!productIdNum) {
      navigate('/my-videos', { replace: true });
      return;
    }
    fetchPlayback(false);
  }, [productIdNum]); // eslint-disable-line react-hooks/exhaustive-deps

  const dur = video?.durationSeconds ?? video?.duration_seconds;
  const thumbnailUrl = video?.thumbnailUrl ?? video?.thumbnail_url;

  return (
    <PageContainer>
      <div className="space-y-6">
        <PageHeader
          title={video?.name || 'Video Player'}
          description="Watch your course lecture with adaptive streaming."
          breadcrumb={[
            { label: 'My Videos', path: '/my-videos' },
            { label: video?.name || 'Video Player' },
          ]}
        />

        {/* Loading */}
        {loading && (
          <div className="py-24 flex flex-col items-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading video player…</p>
          </div>
        )}

        {/* Error: not owned */}
        {!loading && errorCode === 'VIDEO_NOT_OWNED' && (
          <div className="rounded-3xl border border-border bg-card p-16 text-center space-y-4">
            <div className="mx-auto h-16 w-16 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <Lock size={32} />
            </div>
            <h1 className="text-lg font-bold text-foreground">You haven't purchased this video</h1>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              You haven't purchased this video yet. Purchase it from our catalog to start watching.
            </p>
            <div className="flex items-center justify-center gap-3">
              <Link
                to={video?.slug ? `/products/${video.slug}` : '/products'}
                id="go-to-product-btn"
                className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-5 py-2.5 text-sm font-semibold hover:opacity-90 transition"
              >
                View Product Page
              </Link>
              <Link
                to="/my-videos"
                className="inline-flex items-center gap-2 rounded-xl border border-border px-5 py-2.5 text-sm font-medium hover:bg-muted transition"
              >
                Back to My Videos
              </Link>
            </div>
          </div>
        )}

        {/* Error: provider not configured */}
        {!loading && errorCode === 'VIDEO_PLAYBACK_NOT_CONFIGURED' && (
          <div className="rounded-2xl border border-amber-300/40 bg-amber-50 dark:bg-amber-950/20 p-8 flex items-start gap-4">
            <AlertCircle size={20} className="text-amber-500 flex-shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="text-sm font-semibold text-foreground">Playback Unavailable</p>
              <p className="text-xs text-muted-foreground">
                The video player is not configured at the moment (VIDEO_PLAYBACK_NOT_CONFIGURED). Please try again later or contact support.
              </p>
            </div>
          </div>
        )}

        {/* Generic error */}
        {!loading && error && errorCode !== 'VIDEO_NOT_OWNED' && errorCode !== 'VIDEO_PLAYBACK_NOT_CONFIGURED' && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 flex items-center gap-3 text-destructive text-sm">
            <AlertCircle size={18} />
            {error}
          </div>
        )}

        {/* Video player */}
        {!loading && video && (
          <div className="space-y-6">
            {/* Title bar */}
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1">
                <h1 className="text-xl font-extrabold tracking-tight text-foreground">{video.name}</h1>
                {dur && (
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Clock size={12} />
                    {formatDuration(dur)}
                  </div>
                )}
              </div>

              {/* Reload player button (re-signs the embed URL) */}
              <button
                type="button"
                id="reload-player-btn"
                disabled={refreshing}
                onClick={() => fetchPlayback(true)}
                className="flex items-center gap-1.5 rounded-xl border border-border px-3 py-2 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-50 transition"
                title="Reload player with fresh signed token"
              >
                <RefreshCw size={13} className={refreshing ? 'animate-spin' : ''} />
                {refreshing ? 'Reloading…' : 'Reload player'}
              </button>
            </div>

            {/* Embed player */}
            <div className="rounded-2xl overflow-hidden border border-border bg-black shadow-lg aspect-video relative">
              {refreshing ? (
                <div className="absolute inset-0 flex items-center justify-center bg-black">
                  <Loader2 size={36} className="animate-spin text-white/60" />
                </div>
              ) : (
                <iframe
                  id="video-embed-player"
                  src={video.embedUrl}
                  className="w-full h-full"
                  allow="accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture"
                  allowFullScreen
                  title={video.name}
                />
              )}
            </div>

            {/* Token expiry notice */}
            {video.expiresAt && (
              <p className="text-[11px] text-muted-foreground/60 text-right">
                Player link valid until {formatExpiry(video.expiresAt)} — click Refresh for a new link
              </p>
            )}

            {/* Description */}
            {video.description && (
              <section className="rounded-2xl border border-border bg-card p-5 space-y-2">
                <h2 className="text-sm font-semibold text-foreground">About this Video</h2>
                <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                  {video.description}
                </p>
              </section>
            )}

            {/* Thumbnail (shown when no description, gives context) */}
            {!video.description && thumbnailUrl && (
              <div className="rounded-2xl border border-border overflow-hidden max-w-sm">
                <img src={thumbnailUrl} alt={video.name} className="w-full object-cover" />
              </div>
            )}

            {/* PDF Lecture Notes Download */}
            {video.hasAttachmentPdf && (
              <div className="rounded-2xl border border-border bg-card p-4 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                    <FileText size={20} className="text-primary" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">Lecture Notes</p>
                    <p className="text-xs text-muted-foreground">
                      {video.attachmentPdfName ?? 'PDF Notes'}
                      {video.attachmentPdfSize
                        ? ` · ${(video.attachmentPdfSize / 1024 / 1024).toFixed(2)} MB`
                        : ''}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    videoApi.downloadVideoNotesPdf(
                      video.productId,
                      video.attachmentPdfName ?? 'lecture_notes.pdf'
                    )
                  }
                  className="inline-flex items-center gap-2 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 transition shrink-0"
                >
                  <Download size={14} />
                  Download PDF
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
