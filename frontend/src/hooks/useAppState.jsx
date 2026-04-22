import { createContext, useContext, useState } from 'react';

const AppStateContext = createContext(null);

export function AppStateProvider({ children }) {
  const [globalError, setGlobalError] = useState(null);
  const [navTitle, setNavTitle] = useState('Goblin Scheduler');

  const showError = (err) => {
    const message = typeof err === 'string' ? err : err?.message || 'Something went wrong';
    setGlobalError(message);
    setTimeout(() => setGlobalError(null), 6000);
  };

  const clearError = () => setGlobalError(null);

  return (
    <AppStateContext.Provider value={{ globalError, showError, clearError, navTitle, setNavTitle }}>
      {children}
    </AppStateContext.Provider>
  );
}

export function useAppState() {
  const ctx = useContext(AppStateContext);
  if (!ctx) throw new Error('useAppState must be used within AppStateProvider');
  return ctx;
}
