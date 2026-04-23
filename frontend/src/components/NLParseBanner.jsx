import { useState } from 'react';
import { parseEventText } from '../api';
import Button from './Button';

/**
 * Natural language event creation banner.
 * Shown at the top of CreateEventPage Step 1.
 * Parses text like "Team lunch Tuesday-Thursday 12-2pm, 30 min slots"
 * and populates the form.
 */
export default function NLParseBanner({ onParsed, aiAvailable }) {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  if (aiAvailable === false) return null;

  const handleParse = async () => {
    if (!text.trim()) return;
    setLoading(true);
    setError(null);
    setSuccess(false);

    const res = await parseEventText(text.trim());
    setLoading(false);

    if (!res.ok) {
      setError(res.error?.message || 'Failed to parse');
      return;
    }

    if (!res.data?.available) {
      setError('AI parsing is not available');
      return;
    }

    const parsed = res.data.parsed;
    if (parsed && Object.keys(parsed).length > 0) {
      onParsed(parsed);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } else {
      setError('Could not extract event details. Try being more specific.');
    }
  };

  return (
    <div className="bg-charcoal/30 border border-gold/10 rounded-xl p-4 mb-6">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gold">Describe your event in plain English</span>
        <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-gold/15 text-gold uppercase tracking-widest border border-gold/20">
          AI
        </span>
      </div>
      <div className="flex gap-2">
        <input
          type="text"
          value={text}
          onChange={(e) => { setText(e.target.value); setError(null); setSuccess(false); }}
          onKeyDown={(e) => { if (e.key === 'Enter') handleParse(); }}
          placeholder='e.g., "Team lunch next Tuesday-Thursday 12-2pm, 30 min slots"'
          className="flex-1 px-4 py-2.5 rounded-lg bg-charcoal/60 border border-white/10 text-cream text-sm placeholder-silver-dim/60 focus:outline-none focus:border-gold/50 transition-colors"
        />
        <Button
          variant="goldGhost"
          size="sm"
          onClick={handleParse}
          loading={loading}
          disabled={!text.trim()}
        >
          {success ? 'Filled!' : 'Parse'}
        </Button>
      </div>
      {error && (
        <p className="text-xs text-crimson mt-2">{error}</p>
      )}
      {success && (
        <p className="text-xs text-jade mt-2">Event details extracted and filled into the form below.</p>
      )}
    </div>
  );
}
