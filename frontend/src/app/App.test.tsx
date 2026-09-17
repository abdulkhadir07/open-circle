import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App', () => {
  it('bootstraps the session and renders the protected home route', async () => {
    render(<App />);
    expect(await screen.findByRole('heading', { name: /welcome back, maya/i })).toBeInTheDocument();
  });
});
