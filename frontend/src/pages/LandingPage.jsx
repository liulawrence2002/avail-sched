import { useNavigate } from 'react-router-dom';
import CinematicHeroBackground from '../components/CinematicHeroBackground';
import Button from '../components/Button';
import Card from '../components/Card';

export default function LandingPage() {
  const navigate = useNavigate();

  const features = [
    {
      icon: '⚡',
      title: 'Zero Sign-up',
      desc: 'No accounts, no passwords, no friction. Create and share in seconds.',
    },
    {
      icon: '🤝',
      title: 'Group Friendly',
      desc: 'One link for your whole crew. Everyone marks availability, you pick the winner.',
    },
    {
      icon: '🔒',
      title: 'Privacy First',
      desc: 'Minimal data, no tracking, no ads. We store only what scheduling requires.',
    },
    {
      icon: '📅',
      title: 'ICS Export',
      desc: 'Download calendar files that work with every major calendar app.',
    },
  ];

  const steps = [
    {
      num: '01',
      title: 'Create your event',
      desc: 'Set a title, choose your time range, and generate a shareable link instantly.',
    },
    {
      num: '02',
      title: 'Share with your crew',
      desc: 'Send the link via text, email, Slack, or carrier pigeon — whatever works.',
    },
    {
      num: '03',
      title: 'Pick the winner',
      desc: 'See aggregate availability at a glance, lock in the best slot, export to calendar.',
    },
  ];

  return (
    <div className="relative">
      {/* ── Hero Section ── */}
      <section className="relative min-h-[80vh] flex items-center justify-center overflow-hidden">
        <CinematicHeroBackground />

        <div className="relative z-10 max-w-3xl mx-auto px-4 sm:px-6 text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-gold/[0.07] border border-gold/15 text-gold text-xs font-medium mb-6 sm:mb-8 tracking-wide">
            <span aria-hidden="true">🦎</span>
            <span>Free. No accounts. No catch.</span>
          </div>

          {/* Headline — tighter, more controlled */}
          <h1
            className="font-display text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold text-cream mb-4 sm:mb-5 leading-[1.1] tracking-tight text-balance"
            style={{ textShadow: '0 2px 24px rgba(10,10,15,0.6)' }}
          >
            Find when your
            <br />
            <span className="shimmer-text">crew can meet</span>
          </h1>

          {/* Subtitle — more neutral, less cinematic */}
          <p
            className="text-base sm:text-lg md:text-xl text-silver max-w-xl mx-auto mb-8 sm:mb-10 leading-relaxed"
            style={{ textShadow: '0 1px 10px rgba(10,10,15,0.5)' }}
          >
            Create an event, share the link, and let everyone mark their availability.
            <span className="text-cream-muted"> The goblins handle the rest.</span>
          </p>

          {/* CTA — tighter, more product-like */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <Button
              variant="primary"
              size="lg"
              onClick={() => navigate('/create')}
              className="w-full sm:w-auto min-w-[180px]"
            >
              <span>Create an Event</span>
              <span aria-hidden="true" className="ml-0.5">→</span>
            </Button>
            <Button
              variant="ghost"
              size="lg"
              onClick={() => {
                const el = document.getElementById('how-it-works');
                el?.scrollIntoView({ behavior: 'smooth' });
              }}
              className="w-full sm:w-auto"
            >
              How it works
            </Button>
          </div>

          {/* Trust line */}
          <p className="mt-5 text-xs text-silver-dim">
            No credit card. No registration. Takes 30 seconds.
          </p>
        </div>
      </section>

      {/* ── Features Section — compact, structured, left-aligned ── */}
      <section className="relative z-10 py-16 sm:py-20">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="mb-10 sm:mb-12">
            <h2 className="font-display text-2xl sm:text-3xl text-cream mb-2 tracking-tight">
              Why Goblin Scheduler
            </h2>
            <p className="text-silver text-sm sm:text-base">
              Scheduling should not feel like herding cats. Or goblins.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {features.map((f, i) => (
              <Card
                key={i}
                padding="md"
                border="default"
                className="flex items-start gap-4"
              >
                <span className="text-xl flex-shrink-0 mt-0.5 select-none" aria-hidden="true">
                  {f.icon}
                </span>
                <div>
                  <h3 className="text-sm font-semibold text-cream mb-1 leading-snug">
                    {f.title}
                  </h3>
                  <p className="text-sm text-silver leading-relaxed">
                    {f.desc}
                  </p>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* ── How It Works — horizontal structured rows ── */}
      <section
        id="how-it-works"
        className="relative z-10 py-16 sm:py-20 border-t border-white/[0.06]"
      >
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="mb-10 sm:mb-12">
            <h2 className="font-display text-2xl sm:text-3xl text-cream mb-2 tracking-tight">
              How it works
            </h2>
            <p className="text-silver text-sm sm:text-base">
              Three steps. No headaches.
            </p>
          </div>

          <div className="space-y-2">
            {steps.map((step, i) => (
              <Card
                key={i}
                padding="md"
                border="default"
                className="flex items-center gap-4 sm:gap-6"
              >
                {/* Step number */}
                <div className="flex-shrink-0 w-9 h-9 rounded-lg bg-gold/[0.07] border border-gold/15 flex items-center justify-center">
                  <span className="text-xs font-semibold text-gold tracking-wide">
                    {step.num}
                  </span>
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0 grid grid-cols-1 sm:grid-cols-[200px_1fr] gap-1 sm:gap-6 items-center">
                  <h3 className="text-sm font-semibold text-cream leading-snug">
                    {step.title}
                  </h3>
                  <p className="text-sm text-silver leading-relaxed">
                    {step.desc}
                  </p>
                </div>
              </Card>
            ))}
          </div>

          <div className="mt-10 flex justify-start">
            <Button variant="primary" size="md" onClick={() => navigate('/create')}>
              Get Started — It&apos;s Free
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
