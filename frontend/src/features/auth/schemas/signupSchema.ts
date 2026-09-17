import { z } from 'zod';
import { countryNames, regionsForCountry } from '../data/countries';

const requiredText = (label: string, maximum: number) =>
  z.string().trim().min(1, `${label} is required`).max(maximum, `${label} is too long`);

const LETTERS_ONLY = /^[\p{L}]+$/u;

const personName = (label: string) =>
  requiredText(label, 80).regex(LETTERS_ONLY, `${label} can only contain letters`);

const phoneNumber = requiredText('Phone number', 30)
  .regex(/^\+?[0-9](?:[0-9\s()-]*[0-9])?$/, 'Enter a valid phone number')
  .refine((value) => {
    const digitCount = value.replace(/\D/g, '').length;
    return digitCount >= 7 && digitCount <= 15;
  }, 'Enter a valid phone number');

function todayAsLocalDate() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

// Native <input type="date"> used to guarantee real calendar dates (no Feb
// 30). The segmented month/day/year field doesn't, so this replaces that
// protection: round-trips the parts through Date and checks nothing rolled
// over (e.g. day 30 in February would otherwise silently become March 2).
function isValidCalendarDate(value: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const [, y, m, d] = match as unknown as [string, string, string, string];
  const year = Number(y);
  const month = Number(m);
  const day = Number(d);
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
  );
}

export const signupSchema = z
  .object({
    firstName: personName('First name'),
    lastName: personName('Last name'),
    dateOfBirth: z
      .string()
      .min(1, 'Date of birth is required')
      .refine(isValidCalendarDate, 'Enter a valid date')
      .refine((value) => value < todayAsLocalDate(), 'Date of birth must be in the past'),
    email: z.string().trim().min(1, 'Email is required').max(160).email('Enter a valid email'),
    phoneNumber,
    country: requiredText('Country', 80).refine(
      (value) => countryNames.includes(value),
      'Select a country from the list',
    ),
    stateRegion: requiredText('State or region', 80),
    city: requiredText('City', 80),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be 72 characters or fewer'),
    confirmPassword: z.string().min(1, 'Confirm your password'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })
  .refine((values) => regionsForCountry(values.country).includes(values.stateRegion), {
    message: 'Select a state or region from the list',
    path: ['stateRegion'],
  });

export type SignupFormValues = z.infer<typeof signupSchema>;

export const signupStepFields = [
  ['firstName', 'lastName', 'dateOfBirth'],
  ['email', 'phoneNumber'],
  ['country', 'stateRegion', 'city'],
  ['password', 'confirmPassword'],
] as const satisfies readonly (readonly (keyof SignupFormValues)[])[];
