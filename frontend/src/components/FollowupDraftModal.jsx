import { useState } from 'react';
import { generateFollowup } from '../api';
import Button from './Button';

/**
 * Modal for generating and copy-pasting follow-up messages.
 * Supports "confirmation" and "thankyou" variants.
 */
export default function FollowupDraftModal({ hostToken, onClose }) {
  const [variant, setVariant] = useState('confirmation');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setResult(null);

    const res = await generateFollowup(hostToken, variant);
    setLoading(false);

    if (!res.ok) {
      setError(res.error?.message || 'Failed to generate');
      return;
    }

    if (!res.data?.available) {
      setError('AI is not available. Check that the API key is configured.');
      return;
    }

    setResult(res.data);
  };

  const handleCopy = async () => {
    if (!result) return;
    const text = `Subject: ${result.subject}\n\n${result.body}`;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-[#141416] border border-gold/20 rounded-2xl p-6 w-full max-w-lg shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-display font-bold text-gold">Draft Follow-up</h3>
          <button onClick={onClose} className="text-silver hover:text-cream transition-colors">✕</button>
        </div>

        {/* Variant selector */}
        <div className="flex gap-2 mb-4">
          <button
            onClick={() => { setVariant('confirmation'); setResult(null); }}
            className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
              variant === 'confirmation'
                ? 'bg-gold/15 text-gold border border-gold/30'
                : 'bg-charcoal/40 text-silver border border-white/5 hover:border-white/10'
            }`}
          >
            Confirmation
          </button>
          <button
            onClick={() => { setVariant('thankyou'); setResult(null); }}
            className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
              variant === 'thankyou'
                ? 'bg-gold/15 text-gold border border-gold/30'
                : 'bg-charcoal/40 text-silver border border-white/5 hover:border-white/10'
            }`}
          >
            Thank You
          </button>
        </div>

        {!result && (
          <div className="text-center py-6">
            <p className="text-sm text-silver mb-4">
              Generate a {variant === 'confirmation' ? 'confirmation email' : 'thank-you message'} ready to copy-paste.
            </p>
            <Button variant="primary" onClick={handleGenerate} loading={loading}>
              Generate Draft
            </Button>
          </div>
        )}

        {error && <p className="text-sm text-crimson text-center mb-4">{error}</p>}

        {result && (
          <div className="space-y-3">
            <div>
              <label className="text-xs text-silver-dim uppercase tracking-wider">Subject</label>
              <div className="mt-1 px-3 py-2 rounded-lg bg-charcoal/60 border border-white/10 text-sm text-cream">
                {result.subject}
              </div>
            </div>
            <div>
              <label className="text-xs text-silver-dim uppercase tracking-wider">Body</label>
              <div className="mt-1 px-3 py-2 rounded-lg bg-charcoal/60 border border-white/10 text-sm text-cream-muted whitespace-pre-wrap max-h-60 overflow-y-auto">
                {result.body}
              </div>
            </div>
            <div className="flex justify-between pt-2">
              <Button variant="ghost" size="sm" onClick={handleGenerate} loading={loading}>
                Regenerate
              </Button>
              <Button variant="primary" size="sm" onClick={handleCopy} success={copied}>
                {copied ? 'Copied!' : 'Copy to Clipboard'}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
