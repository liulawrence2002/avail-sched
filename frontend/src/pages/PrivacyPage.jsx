import Card from '../components/Card';

export default function PrivacyPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="subtle" shadow="glass">
        <h1 className="font-display text-3xl text-cream mb-6">Privacy Policy</h1>
        <div className="space-y-6 text-silver leading-relaxed text-sm sm:text-base">
          <p>
            Goblin Scheduler is built on a simple principle: <strong className="text-cream">we collect as little data as possible</strong>.
          </p>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">What We Collect</h2>
            <ul className="space-y-2 list-disc list-inside">
              <li>Event titles and descriptions (provided by you)</li>
              <li>Proposed time slots (provided by you)</li>
              <li>Participant display names (provided by participants)</li>
              <li>Optional participant email addresses (only if provided)</li>
              <li>Availability selections (yes/no per time slot)</li>
            </ul>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">What We Do Not Collect</h2>
            <ul className="space-y-2 list-disc list-inside">
              <li>Account passwords (we don't have accounts)</li>
              <li>IP addresses or location data</li>
              <li>Analytics or tracking cookies</li>
              <li>Third-party advertising identifiers</li>
              <li>Social media profiles or contact lists</li>
            </ul>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">How We Use Your Data</h2>
            <p>
              Event data is used solely to operate the scheduling service. We do not sell, rent, or share your data with third parties for marketing or any other purpose.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">Data Retention</h2>
            <p>
              Events are automatically deleted after a period of inactivity (typically 90 days). You may also manually delete an event from the host workspace.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">Local Storage</h2>
            <p>
              We store participant tokens in your browser's localStorage so you can return and edit your availability without re-entering your name. This data never leaves your device.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">Contact</h2>
            <p>
              Questions? The goblins are friendly. Reach out through the project repository or community channels.
            </p>
          </section>

          <p className="text-xs text-silver-dim pt-4 border-t border-white/5">
            Last updated: {new Date().toLocaleDateString()}
          </p>
        </div>
      </Card>
    </div>
  );
}
