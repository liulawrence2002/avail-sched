import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import PublicEventPage from './PublicEventPage.jsx';
import { AppStateProvider } from '../hooks/useAppState.jsx';

// Mock the API functions
vi.mock('../api.js', async () => {
  return {
    getEvent: vi.fn(),
    joinParticipant: vi.fn(),
    getAvailability: vi.fn(),
    saveAvailability: vi.fn(),
  };
});

import { getEvent, joinParticipant, getAvailability, saveAvailability } from '../api.js';

describe('PublicEventPage', () => {
  const mockEvent = {
    title: 'Dragon Raid',
    description: 'A raid to defeat the dragon',
    timezone: 'America/New_York',
    durationMinutes: 60,
    candidateSlotsUtc: [
      '2024-08-01T09:00:00Z',
      '2024-08-01T10:00:00Z',
      '2024-08-01T11:00:00Z',
    ],
  };

  // Mock localStorage
  let storage = {};

  beforeEach(() => {
    storage = {};
    global.localStorage = {
      getItem: vi.fn((key) => storage[key] || null),
      setItem: vi.fn((key, value) => { storage[key] = value; }),
      removeItem: vi.fn((key) => { delete storage[key]; }),
    };
    vi.clearAllMocks();
    // Default mock so availability effect doesn't crash
    getAvailability.mockResolvedValue({ ok: true, data: { items: [] }, error: null });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function renderPage(publicId = 'evt-123') {
    return render(
      <BrowserRouter>
        <AppStateProvider>
          <Routes>
            <Route path="/e/:publicId" element={<PublicEventPage />} />
            <Route path="/e/:publicId/results" element={<div>Results Page</div>} />
            <Route path="/create" element={<div>Create Page</div>} />
          </Routes>
        </AppStateProvider>
      </BrowserRouter>,
      { wrapper: undefined }
    );
  }

  it('renders slots from getEvent candidateSlotsUtc', async () => {
    storage[`goblin_participant_evt-123`] = JSON.stringify('tok_abc123');
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    getAvailability.mockResolvedValue({ ok: true, data: { items: [] }, error: null });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    expect(await screen.findByText(/dragon raid/i)).toBeInTheDocument();
    expect(await screen.findByText(/mark your availability/i)).toBeInTheDocument();
    // All slots should render (formatSlotLocal produces date/time strings)
    const slotButtons = await screen.findAllByRole('button', { name: /aug/i });
    expect(slotButtons.length).toBeGreaterThanOrEqual(3);
  });

  it('shows error when event fails to load', async () => {
    getEvent.mockResolvedValue({
      ok: false,
      error: { message: 'Event not found' },
    });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alert).toHaveTextContent(/event not found/i);
  });

  it('join step renders and validates empty name', async () => {
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    expect(await screen.findByText(/join this event/i)).toBeInTheDocument();
    const joinBtn = screen.getByRole('button', { name: /join.*mark availability/i });
    await userEvent.click(joinBtn);
    expect(await screen.findByText(/please enter your name/i)).toBeInTheDocument();
  });

  it('join participant reads participantToken and moves to availability step', async () => {
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    joinParticipant.mockResolvedValue({
      ok: true,
      data: { participantToken: 'tok_abc123' },
      error: null,
    });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    expect(await screen.findByText(/join this event/i)).toBeInTheDocument();
    const nameInput = screen.getByLabelText(/your name/i);
    await userEvent.type(nameInput, 'Gandalf');

    const joinBtn = screen.getByRole('button', { name: /join.*mark availability/i });
    await userEvent.click(joinBtn);

    await waitFor(() => {
      expect(joinParticipant).toHaveBeenCalledWith('evt-123', {
        displayName: 'Gandalf',
        email: undefined,
      });
    });

    // After joining, should be on availability step
    expect(await screen.findByText(/mark your availability/i)).toBeInTheDocument();
  });

  it('clicking a slot cycles weight states', async () => {
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    joinParticipant.mockResolvedValue({
      ok: true,
      data: { participantToken: 'tok_abc123' },
      error: null,
    });
    getAvailability.mockResolvedValue({ ok: true, data: { items: [] }, error: null });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    // Join first
    expect(await screen.findByText(/join this event/i)).toBeInTheDocument();
    const nameInput = screen.getByLabelText(/your name/i);
    await userEvent.type(nameInput, 'Gandalf');
    const joinBtn = screen.getByRole('button', { name: /join.*mark availability/i });
    await userEvent.click(joinBtn);

    // Wait for availability step
    expect(await screen.findByText(/mark your availability/i)).toBeInTheDocument();

    // Find slots and click the first one
    const slotButtons = await screen.findAllByRole('button', { name: /aug/i });
    expect(slotButtons.length).toBeGreaterThanOrEqual(3);

    const firstSlot = slotButtons[0];
    // Initial state is "Nope"
    expect(firstSlot.textContent).toContain('Nope');

    // Click once → cycles to "Definitely"
    await userEvent.click(firstSlot);
    await waitFor(() => {
      expect(firstSlot.textContent).toContain('Definitely');
    });

    // Click again → cycles to "Probably"
    await userEvent.click(firstSlot);
    await waitFor(() => {
      expect(firstSlot.textContent).toContain('Probably');
    });

    // Click again → cycles to "If I must"
    await userEvent.click(firstSlot);
    await waitFor(() => {
      expect(firstSlot.textContent).toContain('If I must');
    });

    // Click again → cycles back to "Nope"
    await userEvent.click(firstSlot);
    await waitFor(() => {
      expect(firstSlot.textContent).toContain('Nope');
    });
  });

  it('save availability sends correct payload', async () => {
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    joinParticipant.mockResolvedValue({
      ok: true,
      data: { participantToken: 'tok_abc123' },
      error: null,
    });
    getAvailability.mockResolvedValue({ ok: true, data: { items: [] }, error: null });
    saveAvailability.mockResolvedValue({ ok: true, data: { saved: true }, error: null });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    // Join
    expect(await screen.findByText(/join this event/i)).toBeInTheDocument();
    const nameInput = screen.getByLabelText(/your name/i);
    await userEvent.type(nameInput, 'Gandalf');
    const joinBtn = screen.getByRole('button', { name: /join.*mark availability/i });
    await userEvent.click(joinBtn);

    // Wait for availability step
    expect(await screen.findByText(/mark your availability/i)).toBeInTheDocument();

    // Set one slot to "Definitely"
    const slotButtons = await screen.findAllByRole('button', { name: /aug/i });
    await userEvent.click(slotButtons[0]);
    await waitFor(() => {
      expect(slotButtons[0].textContent).toContain('Definitely');
    });

    // Save
    const saveBtn = screen.getByRole('button', { name: /save availability/i });
    await userEvent.click(saveBtn);

    await waitFor(() => {
      expect(saveAvailability).toHaveBeenCalledWith(
        'evt-123',
        'tok_abc123',
        expect.objectContaining({
          items: expect.arrayContaining([
            expect.objectContaining({ slotStartUtc: '2024-08-01T09:00:00Z', weight: 1.0 }),
          ]),
        })
      );
    });

    // Success message should appear
    expect(await screen.findByText(/availability saved/i)).toBeInTheDocument();
  });

  it('navigates to results page via skip button', async () => {
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    expect(await screen.findByText(/join this event/i)).toBeInTheDocument();
    const skipBtn = screen.getByRole('button', { name: /skip to results/i });
    await userEvent.click(skipBtn);

    expect(await screen.findByText(/results page/i)).toBeInTheDocument();
  });

  it('loads previous availability from token', async () => {
    // Pre-populate localStorage with a token
    storage[`goblin_participant_evt-123`] = JSON.stringify('tok_prev123');
    getEvent.mockResolvedValue({ ok: true, data: mockEvent, error: null });
    getAvailability.mockResolvedValue({
      ok: true,
      data: {
        items: [
          { slotStartUtc: '2024-08-01T09:00:00Z', weight: 1.0 },
          { slotStartUtc: '2024-08-01T10:00:00Z', weight: 0.3 },
        ],
      },
    });
    window.history.pushState({}, '', '/e/evt-123');

    renderPage();

    // Should skip join step and go directly to availability
    expect(await screen.findByText(/mark your availability/i)).toBeInTheDocument();
    await waitFor(() => {
      expect(getAvailability).toHaveBeenCalledWith('evt-123', 'tok_prev123');
    });
  });
});
