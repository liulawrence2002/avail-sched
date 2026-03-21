import { Link } from "react-router-dom";
import Card from "../components/Card";

export default function NotFoundPage() {
  return (
    <div className="flex items-center justify-center py-24">
      <Card className="max-w-md space-y-4 text-center">
        <h1 className="text-6xl font-black">404</h1>
        <p className="text-lg text-slate-700">
          This page doesn't exist. Maybe the goblins ate it.
        </p>
        <Link to="/" className="btn inline-block rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white">
          Back to Home
        </Link>
      </Card>
    </div>
  );
}
