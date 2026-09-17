import { zodResolver } from '@hookform/resolvers/zod';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { ArrowLeft } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Controller, useForm, useWatch, type FieldPath } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import type { SignupRequest } from '../api/contracts';
import { ApiError } from '@/lib/api/errors';
import { countryNames, regionsForCountry } from '../data/countries';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { DateOfBirthField } from '../components/DateOfBirthField';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { PasswordField } from '../components/PasswordField';
import { SearchableSelectField } from '../components/SearchableSelectField';
import { SignupProgress } from '../components/SignupProgress';
import { useSignup } from '../hooks/useSignup';
import { signupSchema, signupStepFields, type SignupFormValues } from '../schemas/signupSchema';

const fieldStep: Record<keyof SignupFormValues, number> = {
  firstName: 0,
  lastName: 0,
  dateOfBirth: 0,
  email: 1,
  phoneNumber: 1,
  country: 2,
  stateRegion: 2,
  city: 2,
  password: 3,
  confirmPassword: 3,
};

function isSignupField(value: string): value is keyof SignupFormValues {
  return value in fieldStep;
}

function toSignupRequest(values: SignupFormValues): SignupRequest {
  return {
    firstName: values.firstName,
    lastName: values.lastName,
    dateOfBirth: values.dateOfBirth,
    email: values.email,
    phoneNumber: values.phoneNumber,
    country: values.country,
    stateRegion: values.stateRegion,
    city: values.city,
    password: values.password,
  };
}

export function SignupPage() {
  const navigate = useNavigate();
  const signup = useSignup();
  const reduceMotion = useReducedMotion();
  const [currentStep, setCurrentStep] = useState(0);
  const {
    register,
    control,
    trigger,
    handleSubmit,
    clearErrors,
    setError,
    setValue,
    formState: { errors },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    mode: 'onTouched',
    defaultValues: {
      firstName: '',
      lastName: '',
      dateOfBirth: '',
      email: '',
      phoneNumber: '',
      country: '',
      stateRegion: '',
      city: '',
      password: '',
      confirmPassword: '',
    },
  });

  const selectedCountry = useWatch({ control, name: 'country' });
  const stateOptions = useMemo(() => regionsForCountry(selectedCountry), [selectedCountry]);

  async function goForward() {
    clearErrors('root.server');
    const fields = signupStepFields[currentStep] as readonly FieldPath<SignupFormValues>[];
    const valid = await trigger(fields);
    if (valid) setCurrentStep((step) => Math.min(step + 1, signupStepFields.length - 1));
  }

  const submitSignup = handleSubmit(async (values) => {
    const request = toSignupRequest(values);
    try {
      await signup.mutateAsync(request);
      navigate('/verify-email', { replace: true, state: { email: request.email } });
    } catch (error) {
      if (error instanceof ApiError) {
        let earliestStep = error.status === 409 ? 1 : currentStep;
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          if (!isSignupField(field)) continue;
          setError(field, { message });
          earliestStep = Math.min(earliestStep, fieldStep[field]);
        }
        setCurrentStep(earliestStep);
      }

      setError('root.server', {
        message: error instanceof Error ? error.message : 'Unable to create your account.',
      });
    }
  });

  function handleFormSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (currentStep < signupStepFields.length - 1) {
      void goForward();
    } else {
      void submitSignup();
    }
  }

  return (
    <AuthLayout
      title="Create your circle"
      description="A few details, then you can start finding plans near you."
    >
      <SignupProgress currentStep={currentStep} />
      <form noValidate onSubmit={handleFormSubmit} className="space-y-6">
        <AuthFormError message={errors.root?.server?.message} />
        <motion.div layout transition={{ duration: reduceMotion ? 0 : 0.25 }}>
          <AnimatePresence mode="popLayout" initial={false}>
            <motion.div
              key={currentStep}
              initial={reduceMotion ? false : { opacity: 0, x: 16 }}
              animate={{ opacity: 1, x: 0 }}
              exit={reduceMotion ? undefined : { opacity: 0, x: -16 }}
              transition={{ duration: reduceMotion ? 0 : 0.2, ease: 'easeOut' }}
              className="space-y-5"
            >
              {currentStep === 0 ? (
                <>
                  <div className="grid gap-5 sm:grid-cols-2">
                    <AuthFormField
                      id="first-name"
                      label="First name"
                      autoComplete="given-name"
                      error={errors.firstName?.message}
                      {...register('firstName')}
                    />
                    <AuthFormField
                      id="last-name"
                      label="Last name"
                      autoComplete="family-name"
                      error={errors.lastName?.message}
                      {...register('lastName')}
                    />
                  </div>
                  <Controller
                    name="dateOfBirth"
                    control={control}
                    render={({ field }) => (
                      <DateOfBirthField
                        label="Date of birth"
                        value={field.value}
                        onChange={field.onChange}
                        error={errors.dateOfBirth?.message}
                      />
                    )}
                  />
                </>
              ) : null}

              {currentStep === 1 ? (
                <>
                  <AuthFormField
                    id="signup-email"
                    label="Email"
                    type="email"
                    autoComplete="email"
                    error={errors.email?.message}
                    {...register('email')}
                  />
                  <AuthFormField
                    id="phone-number"
                    label="Phone number"
                    type="tel"
                    inputMode="tel"
                    maxLength={30}
                    autoComplete="tel"
                    error={errors.phoneNumber?.message}
                    {...register('phoneNumber')}
                  />
                </>
              ) : null}

              {currentStep === 2 ? (
                <>
                  <Controller
                    name="country"
                    control={control}
                    render={({ field }) => (
                      <SearchableSelectField
                        id="country"
                        label="Country"
                        autoComplete="country-name"
                        options={countryNames}
                        value={field.value}
                        onChange={(next) => {
                          field.onChange(next);
                          setValue('stateRegion', '', { shouldDirty: true });
                        }}
                        error={errors.country?.message}
                      />
                    )}
                  />
                  <div className="grid gap-5 sm:grid-cols-2">
                    <Controller
                      name="stateRegion"
                      control={control}
                      render={({ field }) => (
                        <SearchableSelectField
                          id="state-region"
                          label="State or region"
                          autoComplete="address-level1"
                          options={stateOptions}
                          value={field.value}
                          onChange={field.onChange}
                          disabled={!selectedCountry}
                          hint={!selectedCountry ? 'Choose a country first' : undefined}
                          error={errors.stateRegion?.message}
                        />
                      )}
                    />
                    <AuthFormField
                      id="city"
                      label="City"
                      autoComplete="address-level2"
                      error={errors.city?.message}
                      {...register('city')}
                    />
                  </div>
                </>
              ) : null}

              {currentStep === 3 ? (
                <>
                  <PasswordField
                    id="signup-password"
                    label="Password"
                    autoComplete="new-password"
                    error={errors.password?.message}
                    {...register('password')}
                  />
                  <PasswordField
                    id="confirm-password"
                    label="Confirm password"
                    autoComplete="new-password"
                    error={errors.confirmPassword?.message}
                    {...register('confirmPassword')}
                  />
                  <p className="text-muted-foreground text-xs leading-5">Use 8 to 72 characters.</p>
                </>
              ) : null}
            </motion.div>
          </AnimatePresence>
        </motion.div>

        <div className="flex gap-3 pt-1">
          {currentStep > 0 ? (
            <Button
              type="button"
              variant="outline"
              className="h-11 px-4"
              onClick={() => {
                clearErrors('root.server');
                setCurrentStep((step) => step - 1);
              }}
            >
              <ArrowLeft aria-hidden="true" />
              Back
            </Button>
          ) : null}
          <AuthSubmitButton
            type="submit"
            className="h-11 flex-1 px-4"
            pending={signup.isPending}
            pendingLabel="Creating account"
          >
            {currentStep === signupStepFields.length - 1 ? 'Create account' : 'Continue'}
          </AuthSubmitButton>
        </div>
      </form>
    </AuthLayout>
  );
}
