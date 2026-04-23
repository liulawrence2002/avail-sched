import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getInsights } from '../api';
import Card from './Card';

const SEVERITY_STYLES = {
  success: 'border-emerald/30 bg-emerald/5',
  warning: 'border-gold/30 bg-gold/5',
  info: 'border-sapphire/30 bg-sapphire/5',
};

const SEVERITY_ICONS = {
  success: '✓',
  warning: '⚠',
  info: 'ℹ',
};

const SEVERITY_COLORS = {
  success: 'text-jade',
  warning: 'text-gold',
  info: 'text-sapphire-bright',
};

/**
 * Dashboard insights panel — shows actionable cards above the event list.
 * Purely algorithmic, no LLM cost.
 */
export default function InsightsPanel({ hostTokens }) {
  const navigate = useNavigate();
  const [insights, setInsights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dismissed, setDismissed] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('goblin_dismissed_insights') || '[]');
    } catch { return []; }
  });

  useEffect(() => {
    if (!hostTokens || hostTokens.length === 0) {
      setLoading(false);
      return;
    }
    getInsights(hostTokens).then((res) => {
      setLoading(false);
      if (res.ok) setInsights(res.data || []);
    });
  }, [hostTokens]);

  const dismiss = (idx) => {
    const key = `${insights[idx]?.type}-${insights[idx]?.publicId}`;
    const next = [...dismissed, key];
    setDismissed(next);
    localStorage.setItem('goblin_dismissed_insights', JSON.stringify(next));
  };

  const visible = insights.filter((ins) => {
    const key = `${ins.type}-${ins.publicId}`;
    return !dismissed.includes(key);
  });

  if (loading || visible.length === 0) return null;

  return (
    <div className="space-y-2 mb-6">
      <h2 className="text-sm font-medium text-silver-dim uppercase tracking-wider mb-2">Insights</h2>
      {visible.map((ins, i) => {
        const originalIdx = insights.indexOf(ins);
        return (
          <Card
            key={`${ins.type}-${ins.publicId}`}
            padding="sm"
            className={`${SEVERITY_STYLES[ins.severity] || ''} cursor-pointer`}
            onClick={() => ins.actionUrl && navigate(ins.actionUrl)}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-2">
                <span className={`text-lg ${SEVERITY_COLORS[ins.severity] || ''}`}>
                  {SEVERITY_ICONS[ins.severity] || ''}
                </span>
                <div>
                  <div className="text-sm font-medium text-cream">{ins.title}</div>
                  <div className="text-xs text-silver mt-0.5">{ins.message}</div>
                </div>
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                {ins.actionLabel && (
                  <span className="text-xs text-gold">{ins.actionLabel} →</span>
                )}
                <button
                  onClick={(e) => { e.stopPropagation(); dismiss(originalIdx); }}
                  className="text-xs text-silver-dim hover:text-cream transition-colors"
                  title="Dismiss"
                >
                  ✕
                </button>
              </div>
            </div>
          </Card>
        );
      })}
    </div>
  );
}
