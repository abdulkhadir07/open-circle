import { describe, expect, it } from 'vitest';
import { loginSchema } from './loginSchema';
import { signupSchema } from './signupSchema';
import { verificationSchema } from './verificationSchema';

const validSignup = {
  firstName: 'Maya',
  lastName: 'Chen',
  dateOfBirth: '1994-05-12',
  email: 'maya@student.sfsu.edu',
  phoneNumber: '+1 415 555 0100',
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

  it('requires a school (.edu) email and accepts subdomains', () => {
    expect(signupSchema.safeParse({ ...validSignup, email: 'maya@sfsu.edu' }).success).toBe(true);
    expect(signupSchema.safeParse({ ...validSignup, email: 'maya@mail.cs.sfsu.edu' }).success).toBe(
      true,
    );
    expect(signupSchema.safeParse({ ...validSignup, email: 'Maya@SFSU.EDU' }).success).toBe(true);

    for (const email of ['maya@gmail.com', 'maya@sfsu.edu.example.com', 'maya@edu']) {
      expect(signupSchema.safeParse({ ...validSignup, email }).success).toBe(false);
    }
  });

  it('no longer asks for a location', () => {
    const result = signupSchema.safeParse(validSignup);
    expect(result.success).toBe(true);
    expect(Object.keys(result.data ?? {})).not.toContain('city');
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
