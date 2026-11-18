import { apiFetch } from './authApi';

export interface MyVideoSummary {
  productId: number;
  slug: string;
  name: string;
  thumbnailUrl?: string | null;
  thumbnail_url?: string | null;
  durationSeconds?: number | null;
  duration_seconds?: number | null;
  description?: string | null;
  purchasedAt?: string;
  purchased_at?: string;
  hasAttachmentPdf?: boolean;
}

export interface MyVideoDetail {
  productId: number;
  slug: string;
  name: string;
  thumbnailUrl?: string | null;
  thumbnail_url?: string | null;
  durationSeconds?: number | null;
  duration_seconds?: number | null;
  description?: string | null;
  purchasedAt?: string;
  purchased_at?: string;
  embedUrl: string;
  expiresAt: string;
  hasAttachmentPdf?: boolean;
  attachmentPdfName?: string | null;
  attachmentPdfSize?: number | null;
}

export interface AdminPlaybackResponse {
  embedUrl: string;
  expiresAt: string;
}

export const videoApi = {
  /**
   * Fetches the list of video courses owned by the authenticated student.
   */
  async getMyVideos(): Promise<MyVideoSummary[]> {
    return apiFetch<MyVideoSummary[]>('/api/my/videos', { method: 'GET' });
  },

  /**
   * Fetches video details and freshly signed embed URL for playback.
   */
  async getMyVideoPlayback(productId: number): Promise<MyVideoDetail> {
    try {
      return await apiFetch<MyVideoDetail>(`/api/my/videos/${productId}`, {
        method: 'GET',
        headers: {
          'Cache-Control': 'no-cache',
        },
      });
    } catch (err: any) {
      if (err.status === 403) {
        const error = new Error(err.message || 'VIDEO_NOT_OWNED');
        (error as any).status = 403;
        (error as any).code = 'VIDEO_NOT_OWNED';
        throw error;
      }
      if (err.status === 503) {
        const error = new Error(err.message || 'VIDEO_PLAYBACK_NOT_CONFIGURED');
        (error as any).status = 503;
        (error as any).code = 'VIDEO_PLAYBACK_NOT_CONFIGURED';
        throw error;
      }
      throw err;
    }
  },

  /**
   * Admin test playback preview endpoint.
   */
  async getAdminPlayback(productId: number): Promise<AdminPlaybackResponse> {
    return apiFetch<AdminPlaybackResponse>(`/api/admin/products/${productId}/playback`, { method: 'GET' });
  },

  /**
   * Downloads the supplementary lecture notes PDF for an owned video.
   */
  async downloadVideoNotesPdf(productId: number, fallbackFilename = 'lecture_notes.pdf'): Promise<void> {
    const token = sessionStorage.getItem('al_session_token');
    const res = await fetch(`/api/my/videos/${productId}/notes-pdf`, {
      method: 'GET',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to download PDF');
    }

    // Try to get filename from Content-Disposition
    let filename = fallbackFilename;
    const disposition = res.headers.get('Content-Disposition');
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename=["']?([^"']+)["']?/);
      if (match && match[1]) {
        filename = match[1];
      }
    }

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },
};
