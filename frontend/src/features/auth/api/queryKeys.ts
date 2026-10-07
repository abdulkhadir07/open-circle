export const authQueryKeys = {
  all: ['auth'] as const,
  currentUser: ['auth', 'current-user'] as const,
  location: {
    countries: ['auth', 'location', 'countries'] as const,
    regions: (countryCode: string) => ['auth', 'location', 'regions', countryCode] as const,
    cities: (countryCode: string, regionCode: string) =>
      ['auth', 'location', 'cities', countryCode, regionCode] as const,
  },
};
