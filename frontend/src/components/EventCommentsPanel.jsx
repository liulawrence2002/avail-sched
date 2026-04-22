import React, { useEffect, useRef, useState } from 'react';
import { getEventComments, postEventComment } from '../api';

export default function EventCommentsPanel({ publicId, participantToken, hostToken, title }) {
  const [comments, setComments] = useState([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState(null);
  const bottomRef = useRef(null);
  const isHost = !!hostToken;
  const token = hostToken || participantToken;

  async function load() {
    setLoading(true);
    try {
      const res = await getEventComments(publicId);
      setComments(res.data || []);
      setError(null);
    } catch (e) {
      setError('Failed to load comments');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // Refresh every 15 seconds
    const interval = setInterval(load, 15000);
    return () => clearInterval(interval);
  }, [publicId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [comments]);

  async function handleSubmit(e) {
    e.preventDefault();
    const text = content.trim();
    if (!text || !token) return;
    setPosting(true);
    try {
      const res = await postEventComment(publicId, token, text, isHost);
      if (res.data) {
        setComments(prev => [...prev, res.data]);
      }
      setContent('');
      setError(null);
    } catch (e) {
      setError('Failed to post comment');
    } finally {
      setPosting(false);
    }
  }

  function formatTime(iso) {
    const d = new Date(iso);
    return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
  }

  return (
    <div className="bg-[#141416] border border-gold-500/20 rounded-2xl p-6 shadow-lg">
      <h3 className="text-xl font-playfair font-bold text-gold-400 mb-4">
        💬 Discussion
      </h3>

      {loading ? (
        <div className="text-ink-400 text-center py-6">Loading comments...</div>
      ) : (
        <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2 mb-4">
          {comments.length === 0 ? (
            <div className="text-ink-400 text-center py-8 italic">
              No comments yet. Start the conversation!
            </div>
          ) : (
            comments.map(c => (
              <div key={c.id} className="flex gap-3">
                <div className="w-8 h-8 rounded-full bg-gold-500/20 flex items-center justify-center text-gold-400 text-sm font-bold shrink-0">
                  {c.authorName.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-ink-100">{c.authorName}</span>
                    <span className="text-xs text-ink-500">{formatTime(c.createdAt)}</span>
                  </div>
                  <p className="text-sm text-ink-300 mt-0.5 whitespace-pre-wrap">{c.content}</p>
                </div>
              </div>
            ))
          )}
          <div ref={bottomRef} />
        </div>
      )}

      {token && (
        <form onSubmit={handleSubmit} className="flex gap-2">
          <input
            type="text"
            value={content}
            onChange={e => setContent(e.target.value)}
            placeholder="Write a comment..."
            maxLength={4000}
            className="flex-1 bg-[#0f0f10] border border-gold-500/20 rounded-lg px-4 py-2.5 text-sm text-ink-100 placeholder-ink-500 focus:outline-none focus:border-gold-500/50 transition-colors"
          />
          <button
            type="submit"
            disabled={posting || !content.trim()}
            className="bg-gold-500 text-void-950 px-4 py-2.5 rounded-lg font-semibold text-sm hover:bg-gold-400 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          >
            {posting ? '...' : 'Send'}
          </button>
        </form>
      )}

      {error && <p className="text-red-400 text-xs mt-2">{error}</p>}
    </div>
  );
}
