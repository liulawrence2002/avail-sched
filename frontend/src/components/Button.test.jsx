import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Button from './Button.jsx';

describe('Button', () => {
  it('renders with default props', () => {
    render(<Button>Click me</Button>);
    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('type', 'button');
  });

  it('shows loading spinner when loading=true', () => {
    render(<Button loading>Loading</Button>);
    const button = screen.getByRole('button', { name: /loading/i });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    // The spinner svg is aria-hidden, so we find it by querySelector
    const svg = button.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('shows success state when success=true', () => {
    render(<Button success>Success</Button>);
    const button = screen.getByRole('button', { name: /success/i });
    expect(button).toBeInTheDocument();
    // Check success icon svg is present (not loading, so it should be the checkmark)
    const svg = button.querySelectorAll('svg');
    expect(svg.length).toBeGreaterThan(0);
  });

  it('calls onClick when clicked', async () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Click me</Button>);
    const button = screen.getByRole('button', { name: /click me/i });
    await userEvent.click(button);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when loading', () => {
    render(<Button loading>Loading</Button>);
    const button = screen.getByRole('button', { name: /loading/i });
    expect(button).toBeDisabled();
  });

  it('is disabled when disabled', () => {
    render(<Button disabled>Disabled</Button>);
    const button = screen.getByRole('button', { name: /disabled/i });
    expect(button).toBeDisabled();
  });

  it('does not call onClick when disabled', async () => {
    const handleClick = vi.fn();
    render(<Button disabled onClick={handleClick}>Disabled</Button>);
    const button = screen.getByRole('button', { name: /disabled/i });
    await userEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('does not call onClick when loading', async () => {
    const handleClick = vi.fn();
    render(<Button loading onClick={handleClick}>Loading</Button>);
    const button = screen.getByRole('button', { name: /loading/i });
    await userEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('uses custom aria-label', () => {
    render(<Button ariaLabel="Submit form">Go</Button>);
    const button = screen.getByRole('button', { name: /submit form/i });
    expect(button).toBeInTheDocument();
  });

  it('shows error icon when error=true', () => {
    render(<Button error>Error</Button>);
    const button = screen.getByRole('button', { name: /error/i });
    expect(button).toBeInTheDocument();
    const svg = button.querySelectorAll('svg');
    expect(svg.length).toBeGreaterThan(0);
  });
});
