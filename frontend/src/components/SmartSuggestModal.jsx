import React, { useEffect, useState } from 'react';
import { getSuggestions } from '../api';
import Button from './Button';

function formatSlotLocal(isoString, timezone) {
  const d = new Date(isoString);
  const dateStr = d.toLocaleDateString('en-US', {
    timeZone: timezone,
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
  const timeStr = d.toLocaleTimeString('en-US', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
  });
  return { dateStr, timeStr };
}

export default function SmartSuggestModal({ hostToken, timezone, onClose, onPick }) {
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const res = await getSuggestions(hostToken);
        if (res.ok) {
          setSuggestions(res.data);
          setError(null);
        } else {
          setError('Failed to load suggestions');
        }
      } catch (e) {
        setError('Failed to load suggestions');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [hostToken]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-[#141416] border border-gold-500/20 rounded-2xl p-6 w-full max-w-lg shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-playfair font-bold text-gold-400">✨ Smart Suggest</h3>
          <button onClick={onClose} className="text-ink-400 hover:text-cream transition-colors">✕</button>
        </div>

        {loading ? (
          <div className="text-ink-400 text-center py-8">Analyzing availability...</div>
        ) : error ? (
          <div className="text-crimson text-center py-8">{error}</div>
        ) : suggestions.length === 0 ? (
          <div className="text-ink-400 text-center py-8">Not enough responses yet. Check back later!</div>
        ) : (
          <div className="space-y-3">
            {suggestions.map((s, i) => {
              const { dateStr, timeStr } = formatSlotLocal(s.slotStartUtc, timezone);
              const rankColor = i === 0 ? 'border-gold-500/40 bg-gold-500/10' : 'border-white/10 bg-charcoal/40';
              const confidenceColor = s.confidenceScore >= 85 ? 'text-jade' : s.confidenceScore >= 60 ? 'text-gold' : 'text-crimson';
              return (
                <div key={s.slotStartUtc} className={`border rounded-xl p-4 ${rankColor}`}>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-bold text-gold-400">#{i + 1}</span>
                      <span className="font-medium text-cream">{dateStr}</span>
                      <span className="text-sm text-silver">{timeStr}</span>
                    </div>
                    <span className={`text-sm font-bold ${confidenceColor}`}>{s.confidenceScore}% match</span>
                  </div>
                  <p className="text-sm text-ink-300 mb-3 leading-relaxed">{s.reasoning}</p>
                  <div className="flex items-center gap-3 text-xs text-ink-400 mb-3">
                    <span className="text-jade">✓ {s.yesCount} definitely</span>
                    <span className="text-sapphire-bright">~ {s.maybeCount} probably</span>
                    <span className="text-gold">? {s.bribeCount} if needed</span>
                    {s.noCount > 0 && <span className="text-crimson">✕ {s.noCount} no</span>}
                  </div>
                  <Button variant={i === 0 ? 'primary' : 'ghost'} size="sm" onClick={() => onPick(s.slotStartUtc)}>
                    Pick This Slot
                  </Button>
                </div>
              );
            })}
          </div>
        )}

        <div className="mt-4 pt-4 border-t border-white/5 text-xs text-ink-500">
          Suggestions are based on participant availability weights and response patterns.
        </div>
      </div>
    </div>
  );
}
