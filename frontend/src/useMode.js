import { useEffect, useMemo, useState } from "react";

const COPY = {
  serious: {
    toggle: "Switch to Goblin Mode",
    hero: "Find the best time without the coordination mess.",
    subhero: "Create an event, collect availability, and lock a time with no accounts required.",
    cta: "Create a Hangout",
  },
  goblin: {
    toggle: "Switch to Serious Mode",
    hero: "Convince the whole cave to hang out at the same time.",
    subhero: "Summon a scheduling link, collect snack-bribed maybes, and crown a winning timeslot.",
    cta: "Start the Shenanigans",
  },
};

export function useMode() {
  const [mode, setMode] = useState(() => localStorage.getItem("mode") || "goblin");

  useEffect(() => {
    localStorage.setItem("mode", mode);
  }, [mode]);

  const copy = useMemo(() => COPY[mode], [mode]);

  return {
    mode,
    copy,
    toggleMode: () => setMode((current) => (current === "goblin" ? "serious" : "goblin")),
  };
}

