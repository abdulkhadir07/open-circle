import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { SignupPage } from './SignupPage';

type User = ReturnType<typeof renderWithProviders>['user'];

async function fillAboutYou(user: User) {
  await user.type(screen.getByLabelText('First name'), 'Maya');
  await user.type(screen.getByLabelText('Last name'), 'Chen');
  await user.click(screen.getByRole('option', { name: '12' }));
  await user.click(screen.getByRole('option', { name: 'May' }));
  await user.click(screen.getByRole('option', { name: '1994' }));
  await user.click(screen.getByRole('button', { name: 'Continue' }));
}

async function fillContact(user: User, email = 'maya@student.sfsu.edu') {
  await user.type(screen.getByLabelText('Email'), email);
  await user.type(screen.getByLabelText('Phone number'), '+1 415 555 0100');
  await user.click(screen.getByRole('button', { name: 'Continue' }));
}

async function fillThroughFinalStep(user: User) {
  await fillAboutYou(user);
  await fillContact(user);
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
  it('validates each step, preserves back-navigation values, and submits only on step three', async () => {
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
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();

    await fillAboutYou(user);
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
    expect(signupCalls).toBe(0);

    await user.type(screen.getByLabelText('Email'), 'maya@student.sfsu.edu');
    await user.type(screen.getByLabelText('Phone number'), '+1 415 555 0100');
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByLabelText('First name')).toHaveValue('Maya');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByLabelText('Email')).toHaveValue('maya@student.sfsu.edu');

    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('Step 3 of 3')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Password'), 'open-circle-strong');
    await user.type(screen.getByLabelText('Confirm password'), 'open-circle-strong');
    expect(signupCalls).toBe(0);

    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByText('Verification route')).toBeInTheDocument();
    expect(signupCalls).toBe(1);
    // No location is collected: accounts belong to a campus, taken from the email domain.
    expect(signupBody).toEqual({
      firstName: 'Maya',
      lastName: 'Chen',
      dateOfBirth: '1994-05-12',
      email: 'maya@student.sfsu.edu',
      phoneNumber: '+1 415 555 0100',
      password: 'open-circle-strong',
    });
  });

  it('has no location step', async () => {
    const { user } = renderSignup();
    await fillAboutYou(user);
    await fillContact(user);

    expect(screen.queryByLabelText('Country')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('City')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('asks for a school email and rejects non-.edu addresses', async () => {
    const { user } = renderSignup();
    await fillAboutYou(user);

    expect(screen.getByText('Use your school (.edu) email')).toBeInTheDocument();

    await fillContact(user, 'maya@gmail.com');
    expect(
      await screen.findByText('Use your school email address (it must end in .edu)'),
    ).toBeInTheDocument();
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
  });

  it('accepts a school email on a subdomain', async () => {
    const { user } = renderSignup();
    await fillAboutYou(user);
    await fillContact(user, 'maya@mail.cs.sfsu.edu');

    expect(await screen.findByText('Step 3 of 3')).toBeInTheDocument();
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
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
  });

  it('rejects non-letter characters in first and last name', async () => {
    const { user } = renderSignup();

    await user.type(screen.getByLabelText('First name'), 'Maya1');
    await user.type(screen.getByLabelText('Last name'), 'Chen-Lee');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('First name can only contain letters')).toBeInTheDocument();
    expect(screen.getByText('Last name can only contain letters')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();
  });

  it('requires a full date of birth before leaving the first step', async () => {
    const { user } = renderSignup();

    await user.type(screen.getByLabelText('First name'), 'Maya');
    await user.type(screen.getByLabelText('Last name'), 'Chen');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByText('Date of birth is required')).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();

    await user.click(screen.getByRole('option', { name: '12' }));
    await user.click(screen.getByRole('option', { name: 'May' }));
    await user.click(screen.getByRole('option', { name: '1994' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
  });

  it('blocks letters in a phone number before leaving the contact step', async () => {
    const { user } = renderSignup();
    await fillAboutYou(user);

    await user.type(screen.getByLabelText('Email'), 'maya@student.sfsu.edu');
    await user.type(screen.getByLabelText('Phone number'), 'not a phone number');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('Enter a valid phone number')).toBeInTheDocument();
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
  });
});
