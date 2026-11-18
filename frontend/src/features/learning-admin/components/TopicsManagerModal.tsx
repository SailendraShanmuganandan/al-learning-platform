import { useState, useEffect } from 'react';
import {
  getAdminTopics,
  createAdminTopic,
  updateAdminTopic,
} from '../api';
import type { TopicResponse } from '../types';
import { getErrorMessage } from '../errorHelper';
import { StatusBadge } from './StatusBadge';
import { FeedbackAlert } from './FeedbackAlert';
import {
  X,
  Plus,
  Edit2,
  Check,
  Power,
  Loader2,
  FolderTree,
  AlertCircle,
} from 'lucide-react';

interface TopicsManagerModalProps {
  isOpen: boolean;
  chapterId: number;
  chapterName?: string;
  onClose: () => void;
  onTopicsUpdated?: () => void;
}

export function TopicsManagerModal({
  isOpen,
  chapterId,
  chapterName,
  onClose,
  onTopicsUpdated,
}: TopicsManagerModalProps) {
  const [topics, setTopics] = useState<TopicResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // New topic input
  const [newTopicName, setNewTopicName] = useState('');
  const [creating, setCreating] = useState(false);

  // Inline rename state
  const [editingTopicId, setEditingTopicId] = useState<number | null>(null);
  const [renameText, setRenameText] = useState('');
  const [savingRename, setSavingRename] = useState(false);

  // Toggling status state
  const [togglingId, setTogglingId] = useState<number | null>(null);

  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!isOpen || !chapterId) return;
    let cancelled = false;
    async function load() {
      try {
        const data = await getAdminTopics(chapterId);
        if (!cancelled) setTopics(data);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [isOpen, chapterId, refreshKey]);

  const refreshTopics = () => {
    setLoading(true);
    setRefreshKey(k => k + 1);
  };

  if (!isOpen) return null;

  async function handleCreateTopic(e: React.FormEvent) {
    e.preventDefault();
    if (!newTopicName.trim()) return;

    setCreating(true);
    setError(null);
    setSuccess(null);
    try {
      await createAdminTopic(chapterId, { name: newTopicName.trim() });
      setNewTopicName('');
      setSuccess('Topic created successfully.');
      refreshTopics();
      onTopicsUpdated?.();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  function startRename(topic: TopicResponse) {
    setEditingTopicId(topic.topicId);
    setRenameText(topic.name);
    setError(null);
  }

  async function handleSaveRename(topicId: number) {
    if (!renameText.trim()) return;
    setSavingRename(true);
    setError(null);
    try {
      await updateAdminTopic(topicId, { name: renameText.trim() });
      setEditingTopicId(null);
      setSuccess('Topic renamed successfully.');
      refreshTopics();
      onTopicsUpdated?.();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSavingRename(false);
    }
  }

  async function handleToggleStatus(topic: TopicResponse) {
    const nextStatus = topic.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setTogglingId(topic.topicId);
    setError(null);
    try {
      await updateAdminTopic(topic.topicId, { status: nextStatus });
      setSuccess(`Topic marked as ${nextStatus}.`);
      refreshTopics();
      onTopicsUpdated?.();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setTogglingId(null);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-card rounded-3xl border border-border p-6 max-w-xl w-full shadow-2xl animate-in zoom-in-95 duration-150 flex flex-col max-h-[85vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-3 border-b border-border">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <FolderTree size={18} />
            </div>
            <div>
              <h3 className="text-sm font-bold text-foreground">Manage Topics</h3>
              <p className="text-xs text-muted-foreground">
                {chapterName ? `Chapter: ${chapterName}` : `Chapter #${chapterId}`}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition"
          >
            <X size={16} />
          </button>
        </div>

        {/* Alerts */}
        <div className="pt-4 space-y-2">
          {success && <FeedbackAlert type="success" message={success} onClose={() => setSuccess(null)} />}
          {error && <FeedbackAlert type="error" message={error} onClose={() => setError(null)} />}
        </div>

        {/* Add new topic form */}
        <form onSubmit={handleCreateTopic} className="pt-4 flex items-center gap-2">
          <input
            type="text"
            required
            placeholder="New topic name…"
            value={newTopicName}
            onChange={e => setNewTopicName(e.target.value)}
            disabled={creating}
            className="flex-1 rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={creating || !newTopicName.trim()}
            className="px-3.5 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition shadow-xs flex items-center gap-1 disabled:opacity-50 shrink-0"
          >
            {creating ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
            <span>Add Topic</span>
          </button>
        </form>

        {/* Topics List */}
        <div className="mt-4 flex-1 overflow-y-auto rounded-2xl border border-border bg-muted/20 divide-y divide-border">
          {loading ? (
            <div className="p-8 text-center text-muted-foreground text-xs">
              <Loader2 size={20} className="animate-spin mx-auto mb-2 text-primary" />
              <span>Loading topics…</span>
            </div>
          ) : topics.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-xs">
              <p className="font-semibold text-foreground">No topics in this chapter</p>
              <p className="mt-1">Add a topic above to categorize chapter questions.</p>
            </div>
          ) : (
            topics.map(topic => {
              const isInactive = topic.status === 'INACTIVE';
              const isEditing = editingTopicId === topic.topicId;

              return (
                <div
                  key={topic.topicId}
                  className={`p-3 flex items-center justify-between gap-3 text-xs transition ${
                    isInactive ? 'opacity-60 bg-muted/40' : 'hover:bg-muted/30'
                  }`}
                >
                  <div className="flex-1 min-w-0">
                    {isEditing ? (
                      <div className="flex items-center gap-1.5">
                        <input
                          type="text"
                          value={renameText}
                          onChange={e => setRenameText(e.target.value)}
                          disabled={savingRename}
                          className="w-full rounded-lg border border-border bg-background px-2.5 py-1 text-xs text-foreground focus:ring-2 focus:ring-primary/40 focus:outline-hidden"
                          autoFocus
                          onKeyDown={e => {
                            if (e.key === 'Enter') handleSaveRename(topic.topicId);
                            if (e.key === 'Escape') setEditingTopicId(null);
                          }}
                        />
                        <button
                          type="button"
                          onClick={() => handleSaveRename(topic.topicId)}
                          disabled={savingRename || !renameText.trim()}
                          className="p-1 rounded-md bg-emerald-600 text-white hover:bg-emerald-700 transition"
                          title="Save Rename"
                        >
                          <Check size={13} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditingTopicId(null)}
                          disabled={savingRename}
                          className="p-1 rounded-md border border-border text-muted-foreground hover:bg-muted transition"
                          title="Cancel"
                        >
                          <X size={13} />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-foreground truncate">{topic.name}</span>
                        <StatusBadge status={topic.status} size="sm" />
                        <span className="text-[11px] text-muted-foreground">
                          ({topic.questionCount} question{topic.questionCount === 1 ? '' : 's'})
                        </span>
                        {isInactive && (
                          <span
                            title="Inactive topics cannot be chosen when authoring new questions."
                            className="text-[10px] text-amber-600 dark:text-amber-400 font-medium inline-flex items-center gap-0.5"
                          >
                            <AlertCircle size={10} />
                            <span>Cannot be chosen</span>
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {!isEditing && (
                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        type="button"
                        onClick={() => startRename(topic)}
                        className="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground hover:bg-muted transition"
                        title="Rename Topic"
                      >
                        <Edit2 size={12} />
                      </button>

                      <button
                        type="button"
                        disabled={togglingId === topic.topicId}
                        onClick={() => handleToggleStatus(topic)}
                        className={`p-1.5 rounded-lg border transition ${
                          isInactive
                            ? 'border-emerald-500/20 text-emerald-600 hover:bg-emerald-500/10'
                            : 'border-zinc-500/20 text-zinc-500 hover:bg-zinc-500/10'
                        }`}
                        title={isInactive ? 'Activate Topic' : 'Deactivate Topic'}
                      >
                        {togglingId === topic.topicId ? (
                          <Loader2 size={12} className="animate-spin" />
                        ) : (
                          <Power size={12} />
                        )}
                      </button>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="pt-4 border-t border-border mt-4 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="py-2 px-4 rounded-xl border border-border text-xs font-medium text-foreground hover:bg-muted transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
