import { Component } from "react";
import { COPY } from "../useMode";

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      const mode = localStorage.getItem("mode") || "goblin";
      const copy = COPY[mode];

      return (
        <div className="mx-auto mt-16 max-w-md rounded-[28px] border border-black/10 bg-white/70 p-8 text-center">
          <h2 className="mb-2 text-2xl font-black uppercase tracking-wide">
            {copy.error.title}
          </h2>
          <p className="mb-6 text-sm text-black/60">
            {copy.error.description}
          </p>
          <a
            href="/"
            className="btn inline-block rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white"
          >
            {copy.error.homeButton}
          </a>
        </div>
      );
    }

    return this.props.children;
  }
}
