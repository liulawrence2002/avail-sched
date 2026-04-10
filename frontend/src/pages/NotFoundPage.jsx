import { Link } from "react-router-dom";

import Card from "../components/Card";

export default function NotFoundPage({ mode }) {
  const note =
    mode === "goblin"
      ? "The page wandered off, probably following a suspicious snack trail."
      : "The page you asked for is not here anymore.";

  return (
    <div className="loading-shell py-16">
      <Card variant="strong" className="max-w-xl text-center">
        <span className="eyebrow">404</span>
        <h1 className="display-title display-title-lg mt-4 text-[3.2rem]">This page slipped out of orbit.</h1>
        <p className="section-kicker mx-auto mt-4 max-w-lg">{note}</p>
        <Link to="/" className="btn btn-primary mt-8 inline-flex rounded-full px-6 py-3 text-sm font-semibold">
          Back to Home
        </Link>
      </Card>
    </div>
  );
}
