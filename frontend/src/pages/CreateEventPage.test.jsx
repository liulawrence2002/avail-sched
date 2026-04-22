import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import CreateEventPage from './CreateEventPage.jsx';
import { AppStateProvider } from '../hooks/useAppState.jsx';

// Mock the createEvent API
vi.mock('../api.js', async () => {
  const actual = await vi.importActual('../api.js');
  return {
    ...actual,
    createEvent: vi.fn(),
  };
});

import { createEvent } from '../api.js';

describe('CreateEventPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  function renderPage() {
    return render(
      <BrowserRouter>
        <AppStateProvider>
          <CreateEventPage />
        </AppStateProvider>
      </BrowserRouter>
    );
  }

  it('renders step 1 (title/description)', () => {
    renderPage();
    expect(screen.getByText(/what's the occasion/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/event title/i)).toBeInTheDocument();
    expect(screen.getByText(/description/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
  });

  it('validation: shows error when title is empty', async () => {
    renderPage();
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/event title is required/i)).toBeInTheDocument();
  });

  it('validation: shows error when title is too short', async () => {
    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'A');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/title must be at least 2 characters/i)).toBeInTheDocument();
  });

  it('step progression works: valid title advances to step 2', async () => {
    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'Dragon Raid');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/set the schedule/i)).toBeInTheDocument();
  });

  it('can go back from step 2 to step 1', async () => {
    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'Dragon Raid');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/set the schedule/i)).toBeInTheDocument();
    const backBtn = screen.getByRole('button', { name: /back/i });
    await userEvent.click(backBtn);
    expect(await screen.findByText(/what's the occasion/i)).toBeInTheDocument();
  });

  it('shows validation errors for schedule fields', async () => {
    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'Dragon Raid');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/set the schedule/i)).toBeInTheDocument();

    // Click Create Event without filling schedule fields
    const createBtn = screen.getByRole('button', { name: /create event/i });
    await userEvent.click(createBtn);
    expect(await screen.findByText(/start date is required/i)).toBeInTheDocument();
    expect(await screen.findByText(/end date is required/i)).toBeInTheDocument();
  });

  it('submits the form and navigates on success', async () => {
    createEvent.mockResolvedValue({
      ok: true,
      data: { hostToken: 'host_abc123' },
      error: null,
    });

    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'Dragon Raid');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/set the schedule/i)).toBeInTheDocument();

    // Fill schedule fields
    const startDateInput = screen.getByLabelText(/start date/i);
    const endDateInput = screen.getByLabelText(/end date/i);
    fireEvent.change(startDateInput, { target: { value: '2024-08-01' } });
    fireEvent.change(endDateInput, { target: { value: '2024-08-03' } });

    const createBtn = screen.getByRole('button', { name: /create event/i });
    await userEvent.click(createBtn);

    await waitFor(() => {
      expect(createEvent).toHaveBeenCalledTimes(1);
      expect(createEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Dragon Raid',
          startDate: '2024-08-01',
          endDate: '2024-08-03',
          timezone: 'America/New_York',
          slotMinutes: 30,
          durationMinutes: 60,
          dailyStartTime: '09:00',
          dailyEndTime: '18:00',
          resultsVisibility: 'aggregate_public',
        })
      );
    });
  });

  it('shows error message when API fails', async () => {
    createEvent.mockResolvedValue({
      ok: false,
      error: { message: 'Server exploded' },
    });

    renderPage();
    const titleInput = screen.getByLabelText(/event title/i);
    await userEvent.type(titleInput, 'Dragon Raid');
    const nextBtn = screen.getByRole('button', { name: /next/i });
    await userEvent.click(nextBtn);
    expect(await screen.findByText(/set the schedule/i)).toBeInTheDocument();

    const startDateInput = screen.getByLabelText(/start date/i);
    const endDateInput = screen.getByLabelText(/end date/i);
    fireEvent.change(startDateInput, { target: { value: '2024-08-01' } });
    fireEvent.change(endDateInput, { target: { value: '2024-08-03' } });

    const createBtn = screen.getByRole('button', { name: /create event/i });
    await userEvent.click(createBtn);

    expect(await screen.findByText(/server exploded/i)).toBeInTheDocument();
  });
});
