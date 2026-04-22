import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useLocalStorage } from '../hooks';
import Card from '../components/Card';
import Button from '../components/Button';

export default function SettingsPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [googleUserId, setGoogleUserId] = useLocalStorage('goblin_google_user_id', null);

  const googleStatus = searchParams.get('google');
  const userId = searchParams.get('userId');

  useEffect(() => {
    if (googleStatus === 'connected' && userId) {
      setGoogleUserId(userId);
    }
  }, [googleStatus, userId, setGoogleUserId]);

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="gold" shadow="stage">
        <h1 className="font-display text-2xl sm:text-3xl text-cream mb-6">Settings</h1>

        {googleStatus === 'connected' && (
          <div className="mb-6 p-4 rounded-xl bg-emerald/10 border border-emerald/20 text-jade">
            <div className="flex items-center gap-2 font-semibold mb-1">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              Google Calendar connected!
            </div>
            <p className="text-sm opacity-80">You can now add finalized events directly to your Google Calendar.</p>
          </div>
        )}

        {googleStatus === 'error' && (
          <div className="mb-6 p-4 rounded-xl bg-ruby/10 border border-ruby/20 text-crimson">
            <div className="flex items-center gap-2 font-semibold mb-1">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
              Failed to connect Google Calendar
            </div>
            <p className="text-sm opacity-80">Please try again or contact support.</p>
          </div>
        )}

        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 rounded-xl bg-charcoal/40 border border-white/5">
            <div>
              <div className="text-sm font-medium text-cream">Google Calendar</div>
              <div className="text-xs text-silver mt-0.5">
                {googleUserId ? `Connected (${googleUserId.substring(0, 8)}...)` : 'Not connected'}
              </div>
            </div>
            {googleUserId ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setGoogleUserId(null)}
              >
                Disconnect
              </Button>
            ) : (
              <Button
                variant="primary"
                size="sm"
                onClick={async () => {
                  const { getGoogleAuthUrl } = await import('../api.js');
                  const res = await getGoogleAuthUrl();
                  if (res.ok && res.data?.url) {
                    window.location.href = res.data.url;
                  }
                }}
              >
                Connect
              </Button>
            )}
          </div>
        </div>

        <div className="mt-8">
          <Button variant="ghost" onClick={() => navigate('/')}>
            ← Back to Dashboard
          </Button>
        </div>
      </Card>
    </div>
  );
}
