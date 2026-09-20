import { zodResolver } from '@hookform/resolvers/zod';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { ArrowLeft, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Controller, useForm, useWatch, type FieldPath } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import type { SignupRequest } from '../api/contracts';
import { ApiError } from '@/lib/api/errors';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { DateOfBirthField } from '../components/DateOfBirthField';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { PasswordField } from '../components/PasswordField';
import { SearchableSelectField } from '../components/SearchableSelectField';
import { SignupProgress } from '../components/SignupProgress';
import { useCities, useCountries, useRegions } from '../hooks/useLocationData';
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

function LocationLoadFailure({
  message,
  retrying,
  onRetry,
}: {
  message: string;
  retrying: boolean;
  onRetry: () => void;
}) {
  return (
    <div
      role="alert"
      className="text-destructive flex items-center justify-between gap-3 text-xs leading-5"
    >
      <span>{message}</span>
      <Button type="button" variant="ghost" size="xs" disabled={retrying} onClick={onRetry}>
        <RefreshCw aria-hidden="true" className={retrying ? 'animate-spin' : undefined} />
        Retry
      </Button>
    </div>
  );
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
  const selectedStateRegion = useWatch({ control, name: 'stateRegion' });
  const selectedCity = useWatch({ control, name: 'city' });
  const countriesQuery = useCountries();
  const countries = countriesQuery.data ?? [];
  const selectedCountryOption = countries.find(({ name }) => name === selectedCountry);
  const selectedCountryCode = selectedCountryOption?.code;
  const regionsQuery = useRegions(selectedCountryCode);
  const regions = regionsQuery.data ?? [];
  const selectedRegionOption = regions.find(({ name }) => name === selectedStateRegion);
  const selectedRegionCode = selectedRegionOption?.code;
  const citiesQuery = useCities(selectedCountryCode, selectedRegionCode);
  const cities = citiesQuery.data ?? [];
  const locationDataBusy =
    (countriesQuery.isFetching && countriesQuery.data === undefined) ||
    (Boolean(selectedCountryCode) && regionsQuery.isFetching && regionsQuery.data === undefined) ||
    (Boolean(selectedRegionCode) && citiesQuery.isFetching && citiesQuery.data === undefined);

  function changeCountry(next: string, onChange: (value: string) => void) {
    if (next !== selectedCountry) {
      setValue('stateRegion', '', { shouldDirty: true, shouldValidate: false });
      setValue('city', '', { shouldDirty: true, shouldValidate: false });
      clearErrors(['country', 'stateRegion', 'city']);
    }
    onChange(next);
  }

  function changeRegion(next: string, onChange: (value: string) => void) {
    if (next !== selectedStateRegion) {
      setValue('city', '', { shouldDirty: true, shouldValidate: false });
      clearErrors(['stateRegion', 'city']);
    }
    onChange(next);
  }

  function validateLocationSelections() {
    let valid = true;

    if (countriesQuery.isSuccess && selectedCountry && !selectedCountryOption) {
      setError('country', { message: 'Select a country from the list' });
      valid = false;
    }

    if (
      selectedCountryOption &&
      regionsQuery.isSuccess &&
      regions.length > 0 &&
      !selectedRegionOption
    ) {
      setError('stateRegion', { message: 'Select a state or region from the list' });
      valid = false;
    }

    if (
      selectedRegionOption &&
      citiesQuery.isSuccess &&
      cities.length > 0 &&
      !cities.some(({ name }) => name === selectedCity)
    ) {
      setError('city', { message: 'Select a city from the list' });
      valid = false;
    }

    return valid;
  }

  async function goForward() {
    if (locationDataBusy) return;
    clearErrors('root.server');
    const fields = signupStepFields[currentStep] as readonly FieldPath<SignupFormValues>[];
    const fieldsValid = await trigger(fields);
    const selectionsValid = currentStep !== 2 || validateLocationSelections();
    const valid = fieldsValid && selectionsValid;
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
                  {countriesQuery.isError ? (
                    <div className="space-y-2">
                      <LocationLoadFailure
                        message="The country list could not load. You can enter your location manually."
                        retrying={countriesQuery.isFetching}
                        onRetry={() => void countriesQuery.refetch()}
                      />
                      <Controller
                        name="country"
                        control={control}
                        render={({ field }) => (
                          <AuthFormField
                            id="country"
                            label="Country"
                            autoComplete="country-name"
                            value={field.value}
                            onBlur={field.onBlur}
                            onChange={(event) => changeCountry(event.target.value, field.onChange)}
                            error={errors.country?.message}
                          />
                        )}
                      />
                    </div>
                  ) : (
                    <Controller
                      name="country"
                      control={control}
                      render={({ field }) => (
                        <SearchableSelectField
                          id="country"
                          label="Country"
                          autoComplete="country-name"
                          options={countries.map(({ name }) => name)}
                          value={field.value}
                          onChange={(next) => changeCountry(next, field.onChange)}
                          loading={countriesQuery.isPending}
                          loadingMessage="Loading countries"
                          emptyMessage="No countries available"
                          error={errors.country?.message}
                        />
                      )}
                    />
                  )}
                  <div className="grid gap-5 sm:grid-cols-2">
                    <Controller
                      name="stateRegion"
                      control={control}
                      render={({ field }) => {
                        if (countriesQuery.isError) {
                          return (
                            <AuthFormField
                              id="state-region"
                              label="State or region"
                              autoComplete="address-level1"
                              value={field.value}
                              onBlur={field.onBlur}
                              onChange={(event) => changeRegion(event.target.value, field.onChange)}
                              hint="Enter if applicable"
                              error={errors.stateRegion?.message}
                            />
                          );
                        }

                        if (selectedCountryCode && regionsQuery.isError) {
                          return (
                            <div className="space-y-2">
                              <LocationLoadFailure
                                message="The region list could not load. Enter it manually if applicable."
                                retrying={regionsQuery.isFetching}
                                onRetry={() => void regionsQuery.refetch()}
                              />
                              <AuthFormField
                                id="state-region"
                                label="State or region"
                                autoComplete="address-level1"
                                value={field.value}
                                onBlur={field.onBlur}
                                onChange={(event) =>
                                  changeRegion(event.target.value, field.onChange)
                                }
                                hint="Enter if applicable"
                                error={errors.stateRegion?.message}
                              />
                            </div>
                          );
                        }

                        const noRegions = regionsQuery.isSuccess && regions.length === 0;
                        return (
                          <SearchableSelectField
                            id="state-region"
                            label="State or region"
                            autoComplete="address-level1"
                            options={regions.map(({ name }) => name)}
                            value={field.value}
                            onChange={(next) => changeRegion(next, field.onChange)}
                            disabled={!selectedCountryCode || noRegions}
                            loading={Boolean(selectedCountryCode) && regionsQuery.isPending}
                            loadingMessage="Loading states and regions"
                            emptyMessage="No states or regions available"
                            hint={
                              !selectedCountryCode
                                ? 'Choose a country first'
                                : noRegions
                                  ? 'No state or region is required for this country'
                                  : undefined
                            }
                            error={errors.stateRegion?.message}
                          />
                        );
                      }}
                    />
                    <Controller
                      name="city"
                      control={control}
                      render={({ field }) => {
                        const manualCity =
                          countriesQuery.isError ||
                          regionsQuery.isError ||
                          citiesQuery.isError ||
                          (regionsQuery.isSuccess && regions.length === 0) ||
                          (citiesQuery.isSuccess && cities.length === 0);

                        if (manualCity) {
                          const noListedCities =
                            citiesQuery.isSuccess &&
                            Boolean(selectedRegionCode) &&
                            cities.length === 0;
                          return (
                            <div className="space-y-2">
                              {citiesQuery.isError && selectedRegionCode ? (
                                <LocationLoadFailure
                                  message="The city list could not load. You can enter your city manually."
                                  retrying={citiesQuery.isFetching}
                                  onRetry={() => void citiesQuery.refetch()}
                                />
                              ) : null}
                              <AuthFormField
                                id="city"
                                label="City"
                                autoComplete="address-level2"
                                value={field.value}
                                onBlur={field.onBlur}
                                onChange={field.onChange}
                                hint={
                                  noListedCities
                                    ? 'No listed cities were found; enter your city'
                                    : undefined
                                }
                                error={errors.city?.message}
                              />
                            </div>
                          );
                        }

                        return (
                          <SearchableSelectField
                            id="city"
                            label="City"
                            autoComplete="address-level2"
                            options={cities.map(({ name }) => name)}
                            value={field.value}
                            onChange={(next) => {
                              field.onChange(next);
                              clearErrors('city');
                            }}
                            disabled={!selectedRegionCode}
                            loading={Boolean(selectedRegionCode) && citiesQuery.isPending}
                            loadingMessage="Loading cities"
                            emptyMessage="No cities available"
                            hint={
                              !selectedRegionCode ? 'Choose a state or region first' : undefined
                            }
                            error={errors.city?.message}
                          />
                        );
                      }}
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
            disabled={signup.isPending || (currentStep === 2 && locationDataBusy)}
          >
            {currentStep === signupStepFields.length - 1 ? 'Create account' : 'Continue'}
          </AuthSubmitButton>
        </div>
      </form>
    </AuthLayout>
  );
}
