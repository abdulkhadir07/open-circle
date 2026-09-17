import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { SignupPage } from './SignupPage';

async function selectFromCombobox(
  user: ReturnType<typeof renderWithProviders>['user'],
  label: string,
  query: string,
) {
  await user.type(screen.getByLabelText(label), query);
  await user.click(await screen.findByRole('option', { name: query }));
}

async function fillThroughFinalStep(user: ReturnType<typeof renderWithProviders>['user']) {
  await user.type(screen.getByLabelText('First name'), 'Maya');
  await user.type(screen.getByLabelText('Last name'), 'Chen');
  await user.click(screen.getByRole('option', { name: '12' }));
  await user.click(screen.getByRole('option', { name: 'May' }));
  await user.click(screen.getByRole('option', { name: '1994' }));
  await user.click(screen.getByRole('button', { name: 'Continue' }));

  await user.type(screen.getByLabelText('Email'), 'maya@example.com');
  await user.type(screen.getByLabelText('Phone number'), '+1 415 555 0100');
  await user.click(screen.getByRole('button', { name: 'Continue' }));

  await selectFromCombobox(user, 'Country', 'United States');
  await selectFromCombobox(user, 'State or region', 'California');
  await user.type(screen.getByLabelText('City'), 'San Francisco');
  await user.click(screen.getByRole('button', { name: 'Continue' }));

  await user.type(screen.getByLabelText('Password'), 'open-circle-strong');
  await user.type(screen.getByLabelText('Confirm password'), 'open-circle-strong');
}

function renderSignup() {
  return renderWithProviders(
    <Routes>
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/verify-email" element={<p>Verification route</p>} />
    </Routes>,
    { initialEntries: ['/signup'] },
  );
}

describe('SignupPage', () => {
  it('validates each step, preserves back-navigation values, and submits only on step four', async () => {
    let signupCalls = 0;
    let signupBody: unknown;
    server.use(
      http.post('*/api/auth/signup', async ({ request }) => {
        signupCalls += 1;
        signupBody = await request.json();
        return HttpResponse.json({ user: authUser }, { status: 201 });
      }),
    );
    const { user } = renderSignup();

    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByText('First name is required')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();

    await user.type(screen.getByLabelText('First name'), 'Maya');
    await user.type(screen.getByLabelText('Last name'), 'Chen');
    await user.click(screen.getByRole('option', { name: '12' }));
    await user.click(screen.getByRole('option', { name: 'May' }));
    await user.click(screen.getByRole('option', { name: '1994' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
    expect(signupCalls).toBe(0);

    await user.type(screen.getByLabelText('Email'), 'maya@example.com');
    await user.type(screen.getByLabelText('Phone number'), '+1 415 555 0100');
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByLabelText('First name')).toHaveValue('Maya');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByLabelText('Email')).toHaveValue('maya@example.com');

    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await selectFromCombobox(user, 'Country', 'United States');
    await selectFromCombobox(user, 'State or region', 'California');
    await user.type(screen.getByLabelText('City'), 'San Francisco');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.type(screen.getByLabelText('Password'), 'open-circle-strong');
    await user.type(screen.getByLabelText('Confirm password'), 'open-circle-strong');
    expect(signupCalls).toBe(0);

    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByText('Verification route')).toBeInTheDocument();
    expect(signupCalls).toBe(1);
    expect(signupBody).toEqual({
      firstName: 'Maya',
      lastName: 'Chen',
      dateOfBirth: '1994-05-12',
      email: 'maya@example.com',
      phoneNumber: '+1 415 555 0100',
      country: 'United States',
      stateRegion: 'California',
      city: 'San Francisco',
      password: 'open-circle-strong',
    });
  });

  it('returns a signup conflict to the contact step with the safe backend message', async () => {
    server.use(
      http.post('*/api/auth/signup', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 409,
            error: 'CONFLICT',
            message: 'Email is already in use',
            path: '/api/auth/signup',
            fieldErrors: {},
          },
          { status: 409 },
        ),
      ),
    );
    const { user } = renderSignup();
    await fillThroughFinalStep(user);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Email is already in use')).toBeInTheDocument();
    expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
  });

  it('rejects non-letter characters in first and last name', async () => {
    const { user } = renderSignup();

    await user.type(screen.getByLabelText('First name'), 'Maya1');
    await user.type(screen.getByLabelText('Last name'), 'Chen-Lee');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('First name can only contain letters')).toBeInTheDocument();
    expect(screen.getByText('Last name can only contain letters')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();
  });

  it('requires a full date of birth before leaving the first step', async () => {
    const { user } = renderSignup();

    await user.type(screen.getByLabelText('First name'), 'Maya');
    await user.type(screen.getByLabelText('Last name'), 'Chen');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByText('Date of birth is required')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 4')).toBeInTheDocument();

    await user.click(screen.getByRole('option', { name: '12' }));
    await user.click(screen.getByRole('option', { name: 'May' }));
    await user.click(screen.getByRole('option', { name: '1994' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
  });

  async function reachLocationStep(user: ReturnType<typeof renderWithProviders>['user']) {
    await user.type(screen.getByLabelText('First name'), 'Maya');
    await user.type(screen.getByLabelText('Last name'), 'Chen');
    await user.click(screen.getByRole('option', { name: '12' }));
    await user.click(screen.getByRole('option', { name: 'May' }));
    await user.click(screen.getByRole('option', { name: '1994' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    await user.type(screen.getByLabelText('Email'), 'maya@example.com');
    await user.type(screen.getByLabelText('Phone number'), '+1 415 555 0100');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
  }

  it('only accepts a country chosen from the list, not arbitrary typed text', async () => {
    const { user } = renderSignup();
    await reachLocationStep(user);

    await user.type(screen.getByLabelText('Country'), 'Not A Real Country');
    await user.tab();
    expect(screen.getByLabelText('Country')).toHaveValue('');

    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByText('Country is required')).toBeInTheDocument();
  });

  it('resets the state or region field, and its options, when the country changes', async () => {
    const { user } = renderSignup();
    await reachLocationStep(user);

    await selectFromCombobox(user, 'Country', 'Canada');
    await selectFromCombobox(user, 'State or region', 'Ontario');
    expect(screen.getByLabelText('State or region')).toHaveValue('Ontario');

    await selectFromCombobox(user, 'Country', 'United States');
    expect(screen.getByLabelText('State or region')).toHaveValue('');

    await user.type(screen.getByLabelText('State or region'), 'Ontario');
    expect(screen.queryByRole('option', { name: 'Ontario' })).not.toBeInTheDocument();
  });

  it('blocks letters in a phone number before leaving the contact step', async () => {
    const { user } = renderSignup();

    await user.type(screen.getByLabelText('First name'), 'Maya');
    await user.type(screen.getByLabelText('Last name'), 'Chen');
    await user.click(screen.getByRole('option', { name: '12' }));
    await user.click(screen.getByRole('option', { name: 'May' }));
    await user.click(screen.getByRole('option', { name: '1994' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    await user.type(screen.getByLabelText('Email'), 'maya@example.com');
    await user.type(screen.getByLabelText('Phone number'), 'not a phone number');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('Enter a valid phone number')).toBeInTheDocument();
    expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
  });
});
