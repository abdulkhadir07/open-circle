import { useQuery } from '@tanstack/react-query';
import { authQueryKeys } from '../api/queryKeys';
import { loadCities, loadCountries, loadRegions } from '../data/locationData';

const locationQueryOptions = {
  staleTime: Number.POSITIVE_INFINITY,
  gcTime: 30 * 60 * 1000,
};

export function useCountries() {
  return useQuery({
    queryKey: authQueryKeys.location.countries,
    queryFn: loadCountries,
    ...locationQueryOptions,
  });
}

export function useRegions(countryCode?: string) {
  return useQuery({
    queryKey: authQueryKeys.location.regions(countryCode ?? ''),
    queryFn: () => loadRegions(countryCode ?? ''),
    enabled: Boolean(countryCode),
    ...locationQueryOptions,
  });
}

export function useCities(countryCode?: string, regionCode?: string) {
  return useQuery({
    queryKey: authQueryKeys.location.cities(countryCode ?? '', regionCode ?? ''),
    queryFn: () => loadCities(countryCode ?? '', regionCode ?? ''),
    enabled: Boolean(countryCode && regionCode),
    ...locationQueryOptions,
  });
}
