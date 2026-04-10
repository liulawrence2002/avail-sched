import { Component } from "react";

import { COPY } from "../useMode";

import Card from "./Card";

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
        <div className="loading-shell">
          <Card variant="strong" className="max-w-xl text-center">
            <span className="eyebrow">Application error</span>
            <h2 className="display-title display-title-lg mt-4 text-[2.7rem]">{copy.error.title}</h2>
            <p className="section-kicker mx-auto mt-4 max-w-lg">{copy.error.description}</p>
            <a href="/" className="btn btn-primary mt-8 inline-flex rounded-full px-6 py-3 text-sm font-semibold">
              {copy.error.homeButton}
            </a>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}
