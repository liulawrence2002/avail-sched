import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { lookupEvents } from '../api.js';
import { useAppState } from '../hooks/useAppState';
import Card from '../components/Card';
import Button from '../components/Button';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';
import InsightsPanel from '../components/InsightsPanel';

const HOST_TOKENS_KEY = 'goblin_host_tokens';

function readHostTokens() {
  try {
    const raw = localStorage.getItem(HOST_TOKENS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveHostToken(hostToken, title) {
  const tokens = readHostTokens();
  const filtered = tokens.filter((t) => t.hostToken !== hostToken);
  filtered.unshift({ hostToken, title, savedAt: new Date().toISOString() });
  const trimmed = filtered.slice(0, 50);
  localStorage.setItem(HOST_TOKENS_KEY, JSON.stringify(trimmed));
}

function StatCard({ label, value, sub, color = 'gold' }) {
  const colorMap = {
    gold: 'text-gold',
    emerald: 'text-jade',
    sapphire: 'text-sapphire-bright',
    crimson: 'text-crimson',
  };
  return (
    <Card padding="md" border="subtle" className="flex flex-col">
      <span className="text-xs text-silver-dim uppercase tracking-wider">{label}</span>
      <span className={`text-2xl font-playfair font-bold ${colorMap[color]} mt-1`}>{value}</span>
      {sub && <span className="text-xs text-ink-400 mt-1">{sub}</span>}
    </Card>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { showError } = useAppState();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hostTokensList, setHostTokensList] = useState([]);

  useEffect(() => {
    const tokens = readHostTokens();
    const tokenList = tokens.map((t) => t.hostToken);
    setHostTokensList(tokenList);
    if (tokens.length === 0) {
      setLoading(false);
      return;
    }
    lookupEvents(tokenList).then((res) => {
      setLoading(false);
      if (res.ok) {
        setEvents(res.data || []);
      } else {
        showError(res.error?.message || 'Failed to load events');
      }
    });
  }, [showError]);

  const stats = (() => {
    const total = events.length;
    const finalized = events.filter((e) => e.finalized).length;
    const active = total - finalized;
    const withDeadline = events.filter((e) => e.deadline && !e.finalized).length;
    const avgResponse = total > 0
      ? Math.round(events.reduce((sum, e) => sum + (e.respondentCount || 0), 0) / total)
      : 0;
    return { total, finalized, active, withDeadline, avgResponse };
  })();

  const upcoming = events
    .filter((e) => e.finalized && e.finalizedAt)
    .sort((a, b) => new Date(b.finalizedAt) - new Date(a.finalizedAt))
    .slice(0, 3);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <LoadingState message="Summoning your events..." />
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <EmptyState
          icon="📭"
          title="No events yet"
          description="Events you create will appear here for easy access."
          action={
            <Button variant="primary" onClick={() => navigate('/create')}>
              Create Your First Event
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <h1 className="font-display text-2xl sm:text-3xl text-cream mb-6">Your Events</h1>

      {/* Insights */}
      <InsightsPanel hostTokens={hostTokensList} />

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
        <StatCard label="Total" value={stats.total} color="gold" />
        <StatCard label="Active" value={stats.active} sub="Awaiting responses" color="sapphire" />
        <StatCard label="Finalized" value={stats.finalized} color="emerald" />
        <StatCard label="With Deadline" value={stats.withDeadline} color="crimson" />
      </div>

      {/* Upcoming */}
      {upcoming.length > 0 && (
        <div className="mb-6">
          <h2 className="text-sm font-medium text-silver-dim uppercase tracking-wider mb-3">Recently Finalized</h2>
          <div className="space-y-2">
            {upcoming.map((evt) => (
              <Card
                key={evt.hostToken}
                padding="sm"
                border="emerald"
                className="cursor-pointer hover:border-emerald/40 transition-colors bg-emerald/5"
                onClick={() => navigate(`/host/${evt.hostToken}`)}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-cream">{evt.title}</span>
                  <span className="text-xs text-jade">Finalized</span>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Event list */}
      <div className="space-y-3">
        {events.map((evt) => (
          <Card
            key={evt.hostToken}
            padding="md"
            border="subtle"
            className="cursor-pointer hover:border-gold/30 transition-colors"
            onClick={() => navigate(`/host/${evt.hostToken}`)}
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-cream">{evt.title}</span>
                  {evt.finalized && (
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald/15 text-jade uppercase tracking-widest border border-emerald/20">
                      Finalized
                    </span>
                  )}
                  {evt.deadline && !evt.finalized && (
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-ruby/15 text-crimson uppercase tracking-widest border border-ruby/20">
                      Deadline Set
                    </span>
                  )}
                </div>
                <div className="text-xs text-silver-dim">
                  {evt.startDate} → {evt.endDate}
                  {evt.respondentCount > 0 && (
                    <span className="ml-2">· {evt.respondentCount} responded</span>
                  )}
                </div>
              </div>
              <span className="text-gold text-sm">→</span>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
