import { useState, useEffect, useRef } from 'react';
import { sendChatMessage, getChatHistory } from '../api';
import Button from './Button';

/**
 * AI Chat Assistant panel — slide-out panel for per-event chat.
 * Renders message bubbles with markdown-like formatting.
 */
export default function AIChatPanel({ hostToken, onClose }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const scrollRef = useRef(null);

  useEffect(() => {
    getChatHistory(hostToken).then((res) => {
      if (res.ok && res.data) {
        setMessages(res.data);
      }
      setHistoryLoaded(true);
    });
  }, [hostToken]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setLoading(true);

    const res = await sendChatMessage(hostToken, text);
    setLoading(false);

    if (res.ok && res.data) {
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.content }]);
    } else {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'Sorry, something went wrong. Please try again.' },
      ]);
    }
  };

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full sm:w-96 flex flex-col bg-[#111118] border-l border-white/10 shadow-2xl">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
        <div className="flex items-center gap-2">
          <span className="text-lg">🤖</span>
          <span className="font-display text-cream font-medium">AI Assistant</span>
          <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-gold/15 text-gold uppercase tracking-widest border border-gold/20">
            AI
          </span>
        </div>
        <button
          onClick={onClose}
          className="text-silver hover:text-cream transition-colors p-1"
        >
          ✕
        </button>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {!historyLoaded && (
          <div className="text-center text-silver-dim text-sm py-8">Loading...</div>
        )}
        {historyLoaded && messages.length === 0 && (
          <div className="text-center text-silver-dim text-sm py-8">
            <p className="mb-2">Ask me anything about your event:</p>
            <div className="space-y-1 text-xs">
              <p className="text-silver">"Who hasn't responded?"</p>
              <p className="text-silver">"Summarize availability"</p>
              <p className="text-silver">"Draft a follow-up message"</p>
              <p className="text-silver">"What's the best time slot?"</p>
            </div>
          </div>
        )}
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[85%] px-3 py-2 rounded-xl text-sm leading-relaxed whitespace-pre-wrap ${
                msg.role === 'user'
                  ? 'bg-gold/15 text-cream border border-gold/20'
                  : 'bg-charcoal/60 text-cream-muted border border-white/5'
              }`}
            >
              {msg.content}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-charcoal/60 border border-white/5 rounded-xl px-3 py-2 text-sm text-silver-dim">
              Thinking...
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="px-4 py-3 border-t border-white/10">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSend(); }}
            placeholder="Ask about your event..."
            className="flex-1 px-3 py-2 rounded-lg bg-charcoal/60 border border-white/10 text-cream text-sm placeholder-silver-dim/60 focus:outline-none focus:border-gold/50 transition-colors"
            disabled={loading}
          />
          <Button
            variant="primary"
            size="sm"
            onClick={handleSend}
            disabled={!input.trim() || loading}
          >
            Send
          </Button>
        </div>
      </div>
    </div>
  );
}
