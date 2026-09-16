import type { components, paths } from './generated/api';

/**
 * Re-exports the generated OpenAPI types under stable local names, so
 * feature code imports from here rather than reaching into `generated/`
 * directly. Regenerate the source with `npm run api:generate` (requires the
 * backend running locally) whenever the backend's API contract changes.
 */
export type ApiPaths = paths;
export type ApiSchemas = components['schemas'];
