import Card from '../components/Card';

export default function TermsPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="subtle" shadow="glass">
        <h1 className="font-display text-3xl text-cream mb-6">Terms of Service</h1>
        <div className="space-y-6 text-silver leading-relaxed text-sm sm:text-base">
          <p>
            Welcome to <strong className="text-cream">Goblin Scheduler</strong>. By using this service, you agree to these terms. They're short because we believe in simplicity.
          </p>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">1. The Service</h2>
            <p>
              Goblin Scheduler provides a free, no-account group availability scheduling tool. We do our best to keep it running, but make no guarantees of uptime, availability, or that the goblins won't take a nap occasionally.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">2. Acceptable Use</h2>
            <p>Please don't use Goblin Scheduler for:</p>
            <ul className="space-y-2 list-disc list-inside mt-2">
              <li>Illegal activities</li>
              <li>Spam, harassment, or abuse</li>
              <li>Uploading malicious content</li>
              <li>Automated scraping or API abuse</li>
            </ul>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">3. Content</h2>
            <p>
              You retain ownership of content you create. We do not claim any rights over your event titles, descriptions, or other data. We may need to process and store this data to provide the service.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">4. No Warranty</h2>
            <p>
              This service is provided "as is" without warranties of any kind. We are not liable for missed meetings, scheduling conflicts, or goblin-related mishaps.
            </p>
          </section>

          <section>
            <h2 className="font-display text-xl text-cream mb-3">5. Changes</h2>
            <p>
              We may update these terms from time to time. Continued use after changes means you accept the new terms.
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
