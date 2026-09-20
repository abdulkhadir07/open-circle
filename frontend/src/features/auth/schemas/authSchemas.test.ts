import { describe, expect, it } from 'vitest';
import { loginSchema } from './loginSchema';
import { signupSchema } from './signupSchema';
import { verificationSchema } from './verificationSchema';

const validSignup = {
  firstName: 'Maya',
  lastName: 'Chen',
  dateOfBirth: '1994-05-12',
  email: 'maya@example.com',
  phoneNumber: '+1 415 555 0100',
  country: 'United States',
  stateRegion: 'California',
  city: 'San Francisco',
  password: 'open-circle-strong',
  confirmPassword: 'open-circle-strong',
};

describe('auth schemas', () => {
  it('accepts a backend-compatible signup without inventing an age minimum', () => {
    const thisYear = new Date().getFullYear();
    expect(
      signupSchema.safeParse({ ...validSignup, dateOfBirth: `${thisYear - 1}-01-01` }).success,
    ).toBe(true);
  });

  it('rejects non-past birthdays, mismatched passwords, and oversized fields', () => {
    const result = signupSchema.safeParse({
      ...validSignup,
      firstName: 'a'.repeat(81),
      dateOfBirth: '2999-01-01',
      confirmPassword: 'different-password',
    });
    expect(result.success).toBe(false);
    if (result.success) return;
    const paths = result.error.issues.map((issue) => issue.path.join('.'));
    expect(paths).toEqual(expect.arrayContaining(['firstName', 'dateOfBirth', 'confirmPassword']));
  });

  it('accepts commonly formatted phone numbers and rejects letters or invalid digit counts', () => {
    expect(signupSchema.safeParse(validSignup).success).toBe(true);
    expect(
      signupSchema.safeParse({ ...validSignup, phoneNumber: '+44 (0) 20 7946 0958' }).success,
    ).toBe(true);
    expect(signupSchema.safeParse({ ...validSignup, phoneNumber: 'call-me-maybe' }).success).toBe(
      false,
    );
    expect(signupSchema.safeParse({ ...validSignup, phoneNumber: '12345' }).success).toBe(false);
  });

  it('allows state or region to be blank for countries without subdivisions', () => {
    expect(signupSchema.safeParse({ ...validSignup, stateRegion: '' }).success).toBe(true);
  });

  it('matches login and verification boundaries', () => {
    expect(loginSchema.safeParse({ email: 'bad', password: 'short' }).success).toBe(false);
    expect(
      verificationSchema.safeParse({ email: 'maya@example.com', code: '123456' }).success,
    ).toBe(true);
    expect(
      verificationSchema.safeParse({ email: 'maya@example.com', code: '12345a' }).success,
    ).toBe(false);
  });
});
