import { expect, test, type Page } from '@playwright/test';

const user = {
  id: '11111111-1111-4111-8111-111111111111',
  username: 'maya.chen',
  firstName: 'Maya',
  lastName: 'Chen',
  email: 'maya@example.com',
  phoneNumber: '+1 415 555 0100',
  dateOfBirth: '1994-05-12',
  city: 'San Francisco',
  stateRegion: 'California',
  country: 'United States',
  role: 'USER',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function apiError(status: number, message: string, path: string) {
  return {
    timestamp: new Date().toISOString(),
    status,
    error: status === 401 ? 'UNAUTHORIZED' : 'ERROR',
    message,
    path,
    fieldErrors: {},
  };
}

async function mockAuthApi(page: Page) {
  await page.route(/\/api\/(?:auth|users)\//, async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (path === '/api/auth/refresh') {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify(apiError(401, 'No active session', path)),
      });
      return;
    }
    if (path === '/api/auth/signup') {
      await route.fulfill({ status: 201, json: { user } });
      return;
    }
    if (path === '/api/auth/verify-email') {
      await route.fulfill({ status: 200, json: { token: 'verified-token', user } });
      return;
    }

    await route.fulfill({ status: 404, json: apiError(404, 'Not found', path) });
  });
}

test('signup and email verification complete the browser auth journey', async ({
  page,
}, testInfo) => {
  await mockAuthApi(page);
  await page.goto('/signup');

  await expect(page.getByRole('heading', { name: 'Create your circle' })).toBeVisible();
  await page.getByLabel('First name').fill('Maya');
  await page.getByLabel('Last name').fill('Chen');
  await page.getByRole('option', { name: '12', exact: true }).click();
  await page.getByRole('option', { name: 'May', exact: true }).click();
  await page.getByRole('option', { name: '1994', exact: true }).click();
  await page.getByRole('button', { name: 'Continue' }).click();

  await page.getByLabel('Email').fill('maya@example.com');
  await page.getByLabel('Phone number').fill('+1 415 555 0100');
  await page.getByRole('button', { name: 'Continue' }).click();

  await page.getByLabel('Country').fill('United States');
  await page.getByRole('option', { name: 'United States', exact: true }).click();
  await page.getByLabel('State or region').fill('California');
  await page.getByRole('option', { name: 'California', exact: true }).click();
  await page.getByLabel('City').fill('San Francisco');
  await page.getByRole('option', { name: 'San Francisco', exact: true }).click();
  await page.getByRole('button', { name: 'Continue' }).click();

  await page.getByLabel('Password', { exact: true }).fill('open-circle-strong');
  await page.getByLabel('Confirm password', { exact: true }).fill('open-circle-strong');
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByRole('heading', { name: 'Verify your email' })).toBeVisible();
  await expect(page.getByLabel('Email')).toHaveValue('maya@example.com');
  await page.getByLabel('Verification code').fill('123456');
  await page.getByRole('button', { name: 'Verify email' }).click();

  // The mocked user has no locationVerifiedAt — matching a real fresh
  // signup, where location verification is a separate, later action — so
  // the authenticated landing state is this prompt, not the feed itself.
  await expect(page.getByRole('heading', { name: 'Verify your location' })).toBeVisible();
  const widths = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    client: document.documentElement.clientWidth,
  }));
  expect(widths.scroll).toBeLessThanOrEqual(widths.client);
  await page.screenshot({
    path: testInfo.outputPath('authenticated-location-prompt.png'),
    fullPage: true,
  });
});

test('login remains usable without horizontal overflow', async ({ page }, testInfo) => {
  await mockAuthApi(page);
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await expect(page.getByRole('img')).toBeVisible();
  await expect(page.locator('[data-slot="auth-content"]')).toHaveCSS('opacity', '1');

  const widths = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    client: document.documentElement.clientWidth,
  }));
  expect(widths.scroll).toBeLessThanOrEqual(widths.client);
  await page.screenshot({ path: testInfo.outputPath('login.png'), fullPage: true });
});
