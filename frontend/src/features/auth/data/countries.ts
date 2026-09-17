import { allCountries } from 'country-region-data';

export const countryNames: string[] = allCountries.map(([name]) => name);

const regionsByCountry = new Map<string, string[]>(
  allCountries.map(([name, , regions]) => [name, regions.map(([regionName]) => regionName)]),
);

export function regionsForCountry(countryName: string): string[] {
  return regionsByCountry.get(countryName) ?? [];
}
