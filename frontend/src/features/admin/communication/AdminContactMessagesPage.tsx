import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  getAdminContactMessages,
  getAdminContactMessage,
  updateContactMessageStatus,
  type AdminContactMessageListItem,
  type AdminContactMessageDetail,
  type ContactMessageStatus,
} from '../../../api/communicationApi';
import type { PageResponse } from '../../../api/classApi';
import {
  Mail,
  Search,
  Filter,
  Loader2,
  Phone,
  User,
  CheckCircle2,
  Archive,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  X,
  MessageSquare,
  RotateCcw,
} from 'lucide-react';

export default function AdminContactMessagesPage() {
  const [searchParams] = useSearchParams();
  const [pageData, setPageData] = useState<PageResponse<AdminContactMessageListItem> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<ContactMessageStatus | ''>(
    (searchParams.get('status') as ContactMessageStatus) || ''
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selected message detail modal
  const [selectedMessage, setSelectedMessage] = useState<AdminContactMessageDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(false);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQ(searchQuery);
      setCurrentPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  useEffect(() => {
    loadMessages(currentPage, statusFilter || undefined, debouncedQ || undefined);
  }, [currentPage, statusFilter, debouncedQ]);

  async function loadMessages(page: number, status?: ContactMessageStatus, q?: string) {
    setLoading(true);
    setError(null);
    try {
      const data = await getAdminContactMessages(status, q, page, 15);
      setPageData(data);
    } catch {
      setError('Failed to load contact messages');
    } finally {
      setLoading(false);
    }
  }

  // Opening an UNREAD message automatically marks it READ by calling the status endpoint from frontend
  async function handleOpenMessage(item: AdminContactMessageListItem) {
    setLoadingDetail(true);
    try {
      const detail = await getAdminContactMessage(item.id);
      setSelectedMessage(detail);

      if (detail.status === 'UNREAD') {
        const updated = await updateContactMessageStatus(detail.id, 'READ');
        setSelectedMessage(updated);
        // Refresh list silently to reflect read status
        loadMessages(currentPage, statusFilter || undefined, debouncedQ || undefined);
      }
    } catch {
      alert('Unable to load message details');
    } finally {
      setLoadingDetail(false);
    }
  }

  async function handleUpdateStatus(newStatus: ContactMessageStatus) {
    if (!selectedMessage) return;
    setActionInProgress(true);
    try {
      const updated = await updateContactMessageStatus(selectedMessage.id, newStatus);
      setSelectedMessage(updated);
      loadMessages(currentPage, statusFilter || undefined, debouncedQ || undefined);
    } catch (err: unknown) {
      const anyErr = err as { message?: string };
      alert(anyErr?.message || 'Failed to update message status');
    } finally {
      setActionInProgress(false);
    }
  }

  function getStatusBadge(status: ContactMessageStatus) {
    switch (status) {
      case 'UNREAD':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/15 text-amber-800 dark:text-amber-200 border border-amber-500/30">
            UNREAD
          </span>
        );
      case 'READ':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-500/10 text-sky-700 dark:text-sky-300 border border-sky-500/20">
            READ
          </span>
        );
      case 'RESPONDED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border border-emerald-500/20">
            RESPONDED
          </span>
        );
      case 'ARCHIVED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-muted text-muted-foreground border border-border">
            ARCHIVED
          </span>
        );
    }
  }

  function formatDate(iso: string) {
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  }

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <MessageSquare size={20} className="text-primary" />
            <span>Contact Messages &amp; Inquiries</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            View student and guest inquiries, update resolution status, and reply directly via email.
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        {/* Status Filter */}
        <div className="flex items-center gap-2 flex-wrap">
          <Filter size={14} className="text-muted-foreground" />
          <span className="text-xs text-muted-foreground font-medium">Status:</span>
          <div className="flex items-center gap-1.5">
            {(['', 'UNREAD', 'READ', 'RESPONDED', 'ARCHIVED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                onClick={() => {
                  setStatusFilter(st);
                  setCurrentPage(0);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition ${
                  statusFilter === st
                    ? 'bg-primary text-primary-foreground font-semibold shadow-xs'
                    : 'bg-card border border-border text-muted-foreground hover:bg-muted hover:text-foreground'
                }`}
              >
                {st === '' ? 'All' : st}
              </button>
            ))}
          </div>
        </div>

        {/* Search input */}
        <div className="relative w-full md:w-64">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search name or email…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-border bg-card pl-8 pr-3 py-1.5 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 shadow-xs"
          />
        </div>
      </div>

      {/* Messages Table */}
      <div className="bg-card rounded-2xl border border-border overflow-hidden shadow-xs">
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-2 text-muted-foreground">
            <Loader2 size={24} className="animate-spin text-primary" />
            <span className="text-xs">Loading contact messages…</span>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-xs text-destructive">{error}</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="py-16 text-center text-xs text-muted-foreground space-y-2">
            <Mail size={24} className="mx-auto opacity-40" />
            <p>No contact messages found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-muted/50 border-b border-border text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Sender</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Message Excerpt</th>
                  <th className="py-3 px-4">Received At</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {pageData.content.map((item) => (
                  <tr
                    key={item.id}
                    id={`contact-msg-row-${item.id}`}
                    className={`hover:bg-muted/30 transition cursor-pointer ${
                      item.status === 'UNREAD' ? 'bg-amber-500/5 font-medium' : ''
                    }`}
                    onClick={() => handleOpenMessage(item)}
                  >
                    <td className="py-3.5 px-4">
                      <div>
                        <div className="font-semibold text-foreground flex items-center gap-1.5">
                          <span>{item.name}</span>
                          {item.status === 'UNREAD' && (
                            <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                          )}
                        </div>
                        <div className="text-[11px] text-muted-foreground">{item.email}</div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4">{getStatusBadge(item.status)}</td>
                    <td className="py-3.5 px-4 max-w-xs text-muted-foreground truncate">
                      {item.messageExcerpt}
                    </td>
                    <td className="py-3.5 px-4 text-muted-foreground whitespace-nowrap">
                      {formatDate(item.createdAt)}
                    </td>
                    <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                      <button
                        type="button"
                        id={`btn-view-msg-${item.id}`}
                        onClick={() => handleOpenMessage(item)}
                        className="px-2.5 py-1 rounded-lg border border-border hover:bg-muted text-foreground transition text-xs font-medium inline-flex items-center gap-1"
                      >
                        <span>View</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {pageData && pageData.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-card/60">
            <span className="text-[11px] text-muted-foreground">
              Showing page {pageData.number + 1} of {pageData.totalPages} ({pageData.totalElements} total)
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={pageData.first}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="p-1 rounded-lg border border-border disabled:opacity-40 text-foreground hover:bg-muted transition"
              >
                <ChevronLeft size={14} />
              </button>
              <button
                type="button"
                disabled={pageData.last}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="p-1 rounded-lg border border-border disabled:opacity-40 text-foreground hover:bg-muted transition"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Message Detail Modal */}
      {(selectedMessage || loadingDetail) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-card rounded-3xl border border-border p-6 max-w-xl w-full shadow-2xl animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col">
            {loadingDetail || !selectedMessage ? (
              <div className="py-20 flex flex-col items-center justify-center gap-2 text-muted-foreground">
                <Loader2 size={24} className="animate-spin text-primary" />
                <span className="text-xs">Loading message…</span>
              </div>
            ) : (
              <>
                {/* Header */}
                <div className="flex items-start justify-between pb-4 border-b border-border">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-foreground">Message from {selectedMessage.name}</h3>
                      {getStatusBadge(selectedMessage.status)}
                    </div>
                    <p className="text-[11px] text-muted-foreground">
                      Received on {formatDate(selectedMessage.createdAt)}
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={() => setSelectedMessage(null)}
                    className="p-1 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground"
                  >
                    <X size={16} />
                  </button>
                </div>

                {/* Sender Details */}
                <div className="py-4 space-y-2 border-b border-border text-xs">
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <User size={13} className="text-primary" />
                    <span className="font-semibold text-foreground">{selectedMessage.name}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <Mail size={13} className="text-primary" />
                    {/* mailto: link */}
                    <a
                      href={`mailto:${selectedMessage.email}?subject=Reply:%20Inquiry%20on%20AL%20Learning%20Platform`}
                      id="link-sender-email"
                      className="text-primary font-medium hover:underline inline-flex items-center gap-1"
                    >
                      <span>{selectedMessage.email}</span>
                      <ExternalLink size={11} />
                    </a>
                  </div>

                  {selectedMessage.phone && (
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <Phone size={13} className="text-primary" />
                      <span>{selectedMessage.phone}</span>
                    </div>
                  )}
                </div>

                {/* Full Message Body (line breaks preserved) */}
                <div className="flex-1 overflow-y-auto py-4">
                  <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                    Message Content:
                  </p>
                  <div
                    id="contact-message-body"
                    className="bg-muted/40 rounded-2xl border border-border/80 p-4 text-xs sm:text-sm text-foreground/90 whitespace-pre-wrap leading-relaxed select-text font-sans"
                  >
                    {selectedMessage.message}
                  </div>
                </div>

                {/* Action Controls */}
                <div className="pt-4 border-t border-border flex items-center justify-between gap-2 flex-wrap">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {/* Mark as Responded */}
                    {selectedMessage.status !== 'RESPONDED' && (
                      <button
                        type="button"
                        id="btn-mark-responded"
                        disabled={actionInProgress}
                        onClick={() => handleUpdateStatus('RESPONDED')}
                        className="px-3 py-1.5 rounded-xl bg-emerald-600 text-white text-xs font-semibold hover:bg-emerald-700 disabled:opacity-50 transition inline-flex items-center gap-1 shadow-xs"
                      >
                        <CheckCircle2 size={13} />
                        <span>Mark as Responded</span>
                      </button>
                    )}

                    {/* Mark as Unread */}
                    {selectedMessage.status !== 'UNREAD' && (
                      <button
                        type="button"
                        id="btn-mark-unread"
                        disabled={actionInProgress}
                        onClick={() => handleUpdateStatus('UNREAD')}
                        className="px-3 py-1.5 rounded-xl border border-border text-foreground text-xs font-medium hover:bg-muted disabled:opacity-50 transition inline-flex items-center gap-1"
                      >
                        <RotateCcw size={13} />
                        <span>Mark as Unread</span>
                      </button>
                    )}

                    {/* Archive */}
                    {selectedMessage.status !== 'ARCHIVED' && (
                      <button
                        type="button"
                        id="btn-archive-msg"
                        disabled={actionInProgress}
                        onClick={() => handleUpdateStatus('ARCHIVED')}
                        className="px-3 py-1.5 rounded-xl border border-border text-muted-foreground text-xs font-medium hover:bg-muted disabled:opacity-50 transition inline-flex items-center gap-1"
                      >
                        <Archive size={13} />
                        <span>Archive</span>
                      </button>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => setSelectedMessage(null)}
                    className="px-3 py-1.5 rounded-xl border border-border text-xs font-medium text-muted-foreground hover:bg-muted transition"
                  >
                    Close
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
