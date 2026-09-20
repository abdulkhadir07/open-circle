import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourceRoot = path.join(
  frontendRoot,
  'node_modules/@countrystatecity/countries-browser/dist/data',
);
const targetRoot = path.join(frontendRoot, 'public/location-data/data');

async function readJson(filePath) {
  return JSON.parse(await readFile(filePath, 'utf8'));
}

async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, JSON.stringify(value));
}

await rm(path.dirname(targetRoot), { recursive: true, force: true });
await mkdir(targetRoot, { recursive: true });

await cp(path.join(sourceRoot, 'countries.json'), path.join(targetRoot, 'countries.json'));
await cp(path.join(sourceRoot, 'states'), path.join(targetRoot, 'states'), { recursive: true });
await cp(path.join(sourceRoot, 'cities'), path.join(targetRoot, 'cities'), { recursive: true });

const countries = await readJson(path.join(sourceRoot, 'countries.json'));
for (const country of countries) {
  const stateFile = path.join(targetRoot, 'states', `${country.iso2}.json`);
  let states;

  try {
    states = await readJson(stateFile);
  } catch (error) {
    if (error?.code !== 'ENOENT') throw error;
    states = [];
    await writeJson(stateFile, states);
  }

  for (const state of states) {
    const cityFile = path.join(targetRoot, 'cities', `${country.iso2}-${state.iso2}.json`);
    try {
      await readFile(cityFile);
    } catch (error) {
      if (error?.code !== 'ENOENT') throw error;
      await writeJson(cityFile, []);
    }
  }
}

await writeFile(
  path.join(path.dirname(targetRoot), 'NOTICE.txt'),
  [
    'Country, state, and city data is provided by Countries States Cities Database.',
    'Source: https://github.com/dr5hn/countries-states-cities-database',
    'License: Open Database License (ODbL) 1.0',
    '',
  ].join('\n'),
);
