import Card from "../components/Card";

const PRIVACY_SECTIONS = [
  {
    title: "What we store",
    body: "Event details, participant names, saved availability responses, and lightweight aggregate view and respondent counts are stored so the scheduling flow works.",
  },
  {
    title: "What we do not require",
    body: "This product does not require personal accounts to create or answer an event. Access is handled through private host and participant links.",
  },
  {
    title: "How links work",
    body: "Host links and participant tokens behave like bearer secrets. Anyone with the link can use it, so they should be shared carefully.",
  },
  {
    title: "Analytics",
    body: "The current product keeps only minimal product analytics hooks. If a production analytics provider is added later, this page should be updated to reflect that change.",
  },
  {
    title: "Contact",
    body: "If you plan to sell the product publicly, replace this page with your business contact details and any region-specific compliance language you need.",
  },
];

export default function PrivacyPage() {
  return (
    <div className="space-y-6">
      <Card variant="strong" className="space-y-4">
        <span className="eyebrow">Privacy</span>
        <h1 className="display-title display-title-lg">Privacy policy</h1>
        <p className="section-kicker">
          This project stores only the information needed to run shared scheduling pages and private host workflows.
        </p>
      </Card>

      <Card className="space-y-5 legal-prose">
        {PRIVACY_SECTIONS.map((section) => (
          <section key={section.title} className="space-y-2">
            <h2>{section.title}</h2>
            <p>{section.body}</p>
          </section>
        ))}
      </Card>
    </div>
  );
}
