import Card from "../components/Card";

const TERMS_SECTIONS = [
  {
    title: "Use at your own discretion",
    body: "This product helps groups coordinate availability, but it does not guarantee attendance, reminder delivery, or third-party calendar behavior.",
  },
  {
    title: "Keep private links private",
    body: "Host links and participant tokens grant access to editing and finalization flows. Treat them like private URLs and avoid posting them publicly.",
  },
  {
    title: "Reasonable use",
    body: "Do not use the service for abusive traffic, illegal content, or attempts to interfere with other users or the platform.",
  },
  {
    title: "No warranty language placeholder",
    body: "If you plan to charge for the product, replace this lightweight page with terms reviewed for your business, jurisdiction, and payment model.",
  },
  {
    title: "Product changes",
    body: "Features may change over time as the product evolves. If pricing, account systems, or notifications are introduced later, the terms should be updated to match.",
  },
];

export default function TermsPage() {
  return (
    <div className="space-y-6">
      <Card variant="strong" className="space-y-4">
        <span className="eyebrow">Terms</span>
        <h1 className="display-title display-title-lg">Terms of use</h1>
        <p className="section-kicker">
          These lightweight terms give the product a real trust surface today and are designed to be replaced with business-ready legal copy before launch.
        </p>
      </Card>

      <Card className="space-y-5 legal-prose">
        {TERMS_SECTIONS.map((section) => (
          <section key={section.title} className="space-y-2">
            <h2>{section.title}</h2>
            <p>{section.body}</p>
          </section>
        ))}
      </Card>
    </div>
  );
}
