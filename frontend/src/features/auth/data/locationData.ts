import type { ICity, ICountry, IState } from '@countrystatecity/countries-browser';

export type CountryOption = {
  code: string;
  name: string;
};

export type RegionOption = {
  code: string;
  name: string;
};

export type CityOption = {
  id: number;
  name: string;
};

const basePath = import.meta.env.BASE_URL.endsWith('/')
  ? import.meta.env.BASE_URL
  : `${import.meta.env.BASE_URL}/`;
const locationDataPath = `${basePath}location-data/data`;

async function loadJson<T>(path: string): Promise<T> {
  const response = await fetch(`${locationDataPath}/${path}`, {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`Location data request failed with status ${response.status}`);
  }

  return (await response.json()) as T;
}

function uniqueByName<T extends { name: string }>(options: T[]): T[] {
  const names = new Set<string>();
  return options
    .filter((option) => {
      if (names.has(option.name)) return false;
      names.add(option.name);
      return true;
    })
    .sort((left, right) => left.name.localeCompare(right.name));
}

export async function loadCountries(): Promise<CountryOption[]> {
  const countries = await loadJson<ICountry[]>('countries.json');
  return uniqueByName(countries.map(({ iso2, name }) => ({ code: iso2, name })));
}

export async function loadRegions(countryCode: string): Promise<RegionOption[]> {
  const regions = await loadJson<IState[]>(`states/${encodeURIComponent(countryCode)}.json`);
  return uniqueByName(regions.map(({ iso2, name }) => ({ code: iso2, name })));
}

export async function loadCities(countryCode: string, regionCode: string): Promise<CityOption[]> {
  const cities = await loadJson<ICity[]>(
    `cities/${encodeURIComponent(countryCode)}-${encodeURIComponent(regionCode)}.json`,
  );
  return uniqueByName(cities.map(({ id, name }) => ({ id, name })));
}
