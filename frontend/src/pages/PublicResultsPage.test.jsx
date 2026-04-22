import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import PublicResultsPage from './PublicResultsPage.jsx';
import { AppStateProvider } from '../hooks/useAppState.jsx';

// Mock the API functions
vi.mock('../api.js', async () => {
  return {
    getPublicResults: vi.fn(),
    getEvent: vi.fn(),
  };
});

import { getPublicResults, getEvent } from '../api.js';

describe('PublicResultsPage', () => {
  const mockResults = {
    topSlots: [
      {
        slotStartUtc: '2024-08-01T09:00:00Z',
        score: 3.5,
        percentOfMax: 100,
        yesCount: 2,
        maybeCount: 1,
        bribeCount: 0,
        noCount: 0,
      },
      {
        slotStartUtc: '2024-08-01T10:00:00Z',
        score: 2.1,
        percentOfMax: 60,
        yesCount: 1,
        maybeCount: 1,
        bribeCount: 1,
        noCount: 0,
      },
    ],
    participantCount: 3,
    respondentCount: 2,
    finalSelection: null,
    timezone: 'America/New_York',
  };

  const mockEvent = {
    title: 'Dragon Raid',
    description: 'A raid to defeat the dragon',
    timezone: 'America/New_York',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function renderPage(publicId = 'evt-123') {
    return render(
      <BrowserRouter>
        <AppStateProvider>
          <Routes>
            <Route path="/e/:publicId/results" element={<PublicResultsPage />} />
            <Route path="/e/:publicId" element={<div>Event Page</div>} />
          </Routes>
        </AppStateProvider>
      </BrowserRouter>,
      { wrapper: undefined }
    );
  }

  it('renders slots with correct data from getPublicResults', async () => {
    getPublicResults.mockResolvedValue({ ok: true, data: mockResults, error: null });
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/dragon raid/i)).toBeInTheDocument();
    expect(await screen.findByText(/here's how everyone voted/i)).toBeInTheDocument();

    // Check participant counts
    expect(screen.getByText(/3 invited/i)).toBeInTheDocument();
    expect(screen.getByText(/2 responded/i)).toBeInTheDocument();

    // Check slot scores
    expect(screen.getByText('3.5')).toBeInTheDocument();
    expect(screen.getByText('2.1')).toBeInTheDocument();

    // Check counts
    expect(screen.getByText(/2 yes/i)).toBeInTheDocument();
    expect(screen.getByText(/1 yes/i)).toBeInTheDocument();
    expect(screen.getAllByText(/1 maybe/i)).toHaveLength(2);
    expect(screen.getByText(/1 possible/i)).toBeInTheDocument();

    // Best badge should appear on top slot (exact match to avoid description text)
    expect(screen.getAllByText(/^Best$/i)).toHaveLength(1);
  });

  it('shows empty state when no responses yet', async () => {
    getPublicResults.mockResolvedValue({
      ok: true,
      data: { topSlots: [], participantCount: 3, respondentCount: 0 },
      error: null,
    });
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/no responses yet/i)).toBeInTheDocument();
    expect(screen.getByText(/no one has marked their availability/i)).toBeInTheDocument();
  });

  it('shows error state when results fail to load', async () => {
    getPublicResults.mockResolvedValue({
      ok: false,
      error: { message: 'Results unavailable' },
    });
    getEvent.mockResolvedValue({ ok: false, error: { message: 'Event not found' } });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/could not load results/i)).toBeInTheDocument();
    expect(screen.getByText(/results unavailable/i)).toBeInTheDocument();
  });

  it('shows finalized banner when finalSelection is present', async () => {
    const finalizedResults = {
      ...mockResults,
      finalSelection: {
        slotStartUtc: '2024-08-01T09:00:00Z',
      },
    };
    getPublicResults.mockResolvedValue({ ok: true, data: finalizedResults, error: null });
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/finalized/i)).toBeInTheDocument();
    expect(screen.getByText(/the host selected/i)).toBeInTheDocument();
  });

  it('navigates back to event page', async () => {
    getPublicResults.mockResolvedValue({ ok: true, data: mockResults, error: null });
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/dragon raid/i)).toBeInTheDocument();
    const backBtn = screen.getByRole('button', { name: /back to event/i });
    await userEvent.click(backBtn);

    expect(await screen.findByText(/event page/i)).toBeInTheDocument();
  });

  it('uses event title from getEvent when available', async () => {
    getPublicResults.mockResolvedValue({ ok: true, data: mockResults, error: null });
    getEvent.mockResolvedValue({ ok: true, data: { ...mockEvent, title: 'Custom Event Title' }, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/custom event title/i)).toBeInTheDocument();
  });

  it('falls back to generic title when event fetch fails', async () => {
    getPublicResults.mockResolvedValue({ ok: true, data: mockResults, error: null });
    getEvent.mockResolvedValue({ ok: false, error: { message: 'Event not found' } });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(await screen.findByText(/event results/i)).toBeInTheDocument();
  });

  it('shows loading state initially', async () => {
    getPublicResults.mockResolvedValue(
      new Promise((resolve) => setTimeout(() => resolve({ ok: true, data: mockResults, error: null }), 100))
    );
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123/results');

    renderPage();

    expect(screen.getByText(/tallying up the goblin votes/i)).toBeInTheDocument();
  });
});
