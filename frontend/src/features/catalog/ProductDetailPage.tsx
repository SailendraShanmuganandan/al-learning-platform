import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import {
  getPublicProductBySlug,
  type PublicProductDetailResponse,
  type PublicChapterRef,
} from '../../api/productApi';
import type { ApiError } from '../../api/authApi';
import {
  Video,
  FileText,
  Loader2,
  AlertCircle,
  ChevronRight,
  Clock,
  BookMarked,
  ArrowLeft,
  ShoppingCart,
  Plus,
  Minus,
  Play,
} from 'lucide-react';
import { videoApi } from '../../api/videoApi';

function formatLKR(price: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
  }).format(price);
}

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

export default function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const { addItem } = useCart();

  const [product, setProduct] = useState<PublicProductDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  // Cart action state
  const [quantity, setQuantity] = useState<number>(1);
  const [adding, setAdding] = useState<boolean>(false);
  const [cartError, setCartError] = useState<string | null>(null);
  const [cartSuccess, setCartSuccess] = useState<boolean>(false);
  const [isOwnedVideo, setIsOwnedVideo] = useState<boolean>(false);

  useEffect(() => {
    if (!slug) { navigate('/products', { replace: true }); return; }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotFound(false);

    getPublicProductBySlug(slug)
      .then(p => {
        if (!cancelled) {
          setProduct(p);
          if (p.type === 'VIDEO' && isAuthenticated && user?.role === 'STUDENT') {
            videoApi.getMyVideos()
              .then(ownedList => {
                if (!cancelled) {
                  const owned = ownedList.some(v => v.productId === p.id);
                  setIsOwnedVideo(owned);
                }
              })
              .catch(() => {});
          }
        }
      })
      .catch((err: ApiError) => {
        if (!cancelled) {
          if (err?.status === 404) setNotFound(true);
          else setError(err?.message ?? 'Failed to load product.');
        }
      })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [slug, navigate, isAuthenticated, user]);

  const isNote = product?.type === 'PHYSICAL_NOTE';
  const isVideo = product?.type === 'VIDEO';
  const isOutOfStock = isNote && product?.inStock === false;

  async function handleAddToCart() {
    if (!product) return;

    if (!isAuthenticated) {
      navigate(`/login?returnUrl=/products/${product.slug}`);
      return;
    }

    setAdding(true);
    setCartError(null);
    setCartSuccess(false);

    try {
      await addItem(product.id, isVideo ? 1 : quantity);
      setCartSuccess(true);
    } catch (err: any) {
      const code = err?.error || err?.code;
      if (code === 'OUT_OF_STOCK') {
        setCartError('Sorry, this note is currently out of stock.');
      } else if (code === 'INSUFFICIENT_STOCK') {
        setCartError(err?.message || 'Requested quantity exceeds available stock.');
      } else if (code === 'ALREADY_IN_CART') {
        setCartError('This video course is already in your shopping cart.');
      } else if (code === 'VIDEO_ALREADY_OWNED') {
        setCartError('You already own this video course.');
      } else {
        setCartError(err?.message || 'Failed to add product to cart.');
      }
    } finally {
      setAdding(false);
    }
  }

  return (
    <PageContainer>
      <div className="space-y-8">
        {/* Back link */}
        <Link
          to="/products"
          className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition"
        >
          <ArrowLeft size={13} />
          Back to Notes &amp; Videos
        </Link>

        {/* Loading */}
        {loading && (
          <div className="py-24 flex flex-col items-center gap-3">
            <Loader2 size={32} className="animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading product…</p>
          </div>
        )}

        {/* Not found */}
        {!loading && notFound && (
          <div className="rounded-3xl border border-border bg-card p-12 text-center space-y-4">
            <div className="mx-auto h-14 w-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
              <AlertCircle size={28} />
            </div>
            <h1 className="text-lg font-bold text-foreground">Product Not Found</h1>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              This product doesn't exist or is no longer available.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-1.5 rounded-xl bg-primary text-primary-foreground px-4 py-2 text-xs font-semibold hover:opacity-90 transition"
            >
              Browse all products
            </Link>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-6 flex items-center gap-3 text-destructive text-sm">
            <AlertCircle size={18} />
            {error}
          </div>
        )}

        {/* Product detail */}
        {!loading && product && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left: thumbnail + purchase area */}
            <div className="space-y-4">
              {/* Thumbnail */}
              <div className="aspect-video rounded-2xl overflow-hidden bg-muted border border-border">
                {product.thumbnailUrl ? (
                  <img
                    src={product.thumbnailUrl}
                    alt={product.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    {isVideo ? (
                      <Video size={48} className="text-muted-foreground/30" />
                    ) : (
                      <FileText size={48} className="text-muted-foreground/30" />
                    )}
                  </div>
                )}
              </div>

              {/* Purchase card */}
              <div className="rounded-2xl border border-border bg-card p-5 space-y-4">
                {/* Type badge */}
                <div className="flex items-center gap-2">
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold uppercase tracking-wide ${
                    isVideo
                      ? 'bg-primary/10 text-primary border border-primary/20'
                      : 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20'
                  }`}>
                    {isVideo ? <Video size={11} /> : <FileText size={11} />}
                    {isVideo ? 'Video Course' : 'Physical Notes'}
                  </span>
                </div>

                {/* Price */}
                <div>
                  <div className="text-2xl font-extrabold text-primary">
                    {formatLKR(product.price)}
                  </div>
                  {isNote && product.inStock === false && (
                    <p className="text-xs font-medium text-destructive mt-1">Currently out of stock</p>
                  )}
                  {isNote && product.inStock === true && (
                    <p className="text-xs font-medium text-emerald-500 mt-1">In stock</p>
                  )}
                </div>

                {/* Type-specific details */}
                {isNote && product.pageCount && (
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <FileText size={13} />
                    <span>{product.pageCount} pages</span>
                  </div>
                )}
                {isVideo && product.durationSeconds && (
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Clock size={13} />
                    <span>{formatDuration(product.durationSeconds)}</span>
                  </div>
                )}

                {/* PDF Notes badge */}
                {isVideo && product.hasAttachmentPdf && (
                  <div className="flex items-center gap-2 rounded-xl border border-primary/20 bg-primary/5 px-4 py-2.5 text-sm">
                    <FileText size={16} className="text-primary shrink-0" />
                    <div>
                      <span className="font-semibold text-foreground">Includes Supplementary Lecture Notes</span>
                      {product.attachmentPdfName && (
                        <span className="text-xs text-muted-foreground ml-2">({product.attachmentPdfName})</span>
                      )}
                    </div>
                  </div>
                )}

                {/* ── Add to Cart Actions or Watch Now ── */}
                {isVideo && isOwnedVideo ? (
                  <div className="space-y-3 pt-2">
                    <div className="rounded-xl border border-purple-500/30 bg-purple-500/10 p-4 text-center space-y-3">
                      <p className="text-xs font-semibold text-purple-900 dark:text-purple-300 flex items-center justify-center gap-1.5">
                        <Video size={16} />
                        <span>You own this video course</span>
                      </p>
                      <Link
                        to={`/my-videos/${product.id}`}
                        id="watch-now-btn"
                        className="w-full flex items-center justify-center gap-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white py-3 text-sm font-semibold transition shadow-sm"
                      >
                        <Play size={16} />
                        <span>Watch now</span>
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-3 pt-2">
                    {isNote && !isOutOfStock && (
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-muted-foreground">Quantity</span>
                        <div className="flex items-center rounded-lg border border-border bg-card">
                          <button
                            type="button"
                            id="detail-qty-decrease"
                            disabled={quantity <= 1 || adding}
                            onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                            className="p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition"
                            aria-label="Decrease quantity"
                          >
                            <Minus size={13} />
                          </button>
                          <span
                            id="detail-qty-value"
                            className="w-8 text-center text-xs font-semibold text-foreground select-none"
                          >
                            {quantity}
                          </span>
                          <button
                            type="button"
                            id="detail-qty-increase"
                            disabled={adding}
                            onClick={() => setQuantity((q) => q + 1)}
                            className="p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition"
                            aria-label="Increase quantity"
                          >
                            <Plus size={13} />
                          </button>
                        </div>
                      </div>
                    )}

                    <button
                      type="button"
                      id="add-to-cart-btn"
                      disabled={adding || isOutOfStock}
                      onClick={handleAddToCart}
                      className="w-full flex items-center justify-center gap-2 rounded-xl bg-primary text-primary-foreground py-3 text-sm font-semibold hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition shadow-sm"
                    >
                      {adding ? (
                        <>
                          <Loader2 size={16} className="animate-spin" />
                          <span>Adding to Cart...</span>
                        </>
                      ) : isOutOfStock ? (
                        <span>Out of Stock</span>
                      ) : (
                        <>
                          <ShoppingCart size={16} />
                          <span>Add to Cart</span>
                        </>
                      )}
                    </button>

                    {/* Error & Success Feedback */}
                    {cartError && (
                      <div
                        id="add-to-cart-error"
                        className="p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs flex items-center justify-between gap-2"
                      >
                        <div className="flex items-center gap-2">
                          <AlertCircle size={14} className="flex-shrink-0" />
                          <span>{cartError}</span>
                        </div>
                        {(cartError.includes('already own') || cartError.includes('VIDEO_ALREADY_OWNED')) && (
                          <Link
                            to={`/my-videos/${product.id}`}
                            className="underline font-semibold hover:opacity-80 shrink-0"
                          >
                            Watch it now
                          </Link>
                        )}
                      </div>
                    )}

                    {cartSuccess && (
                      <div
                        id="add-to-cart-success"
                        className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center justify-between"
                      >
                        <span className="font-medium">Added to cart!</span>
                        <Link to="/cart" id="view-cart-link" className="underline font-semibold hover:opacity-80">
                          View Cart
                        </Link>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Right: details */}
            <div className="lg:col-span-2 space-y-6">
              {/* Title */}
              <div>
                <h1 className="text-2xl font-extrabold tracking-tight text-foreground">{product.name}</h1>
              </div>

              {/* Description */}
              {product.description && (
                <section aria-labelledby="desc-heading">
                  <h2 id="desc-heading" className="text-sm font-semibold text-foreground mb-2">Description</h2>
                  <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                    {product.description}
                  </p>
                </section>
              )}

              {/* Video description */}
              {isVideo && product.videoDescription && (
                <section aria-labelledby="video-desc-heading">
                  <h2 id="video-desc-heading" className="text-sm font-semibold text-foreground mb-2">About this Video</h2>
                  <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                    {product.videoDescription}
                  </p>
                </section>
              )}

              {/* Linked chapters */}
              <section aria-labelledby="chapters-heading">
                <h2 id="chapters-heading" className="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
                  <BookMarked size={15} className="text-muted-foreground" />
                  Covered Chapters
                </h2>
                {product.chapters.length > 0 ? (
                  <div className="space-y-2">
                    {product.chapters.map((ch: PublicChapterRef) => (
                      <Link
                        key={ch.chapterId}
                        to={`/chapters/${ch.chapterId}`}
                        id={`chapter-link-${ch.chapterId}`}
                        className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 hover:border-primary/30 hover:bg-primary/5 transition group"
                      >
                        <div>
                          <span className="text-[10px] text-muted-foreground">{ch.subjectName}</span>
                          <p className="text-sm font-medium text-foreground group-hover:text-primary transition">
                            {ch.name}
                          </p>
                        </div>
                        <ChevronRight size={14} className="text-muted-foreground group-hover:text-primary transition" />
                      </Link>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-muted-foreground italic">
                    No chapters are currently linked to this product.
                  </p>
                )}
              </section>
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
