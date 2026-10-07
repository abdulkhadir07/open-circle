import { beforeEach, describe, expect, it } from 'vitest';
import {
  availableScopes,
  feedAudienceOptions,
  placeLine,
  postAudienceOptions,
  readStoredAudience,
  scopeNames,
  storeAudience,
} from './audience';

const austin = {
  verifiedCity: 'Austin',
  verifiedStateRegion: 'Texas',
  verifiedCountry: 'United States',
};
const banjul = { verifiedCity: 'Banjul', verifiedCountry: 'The Gambia' };

describe('audience', () => {
  it('names each scope after the place', () => {
    expect(scopeNames(austin)).toEqual({
      CITY: 'Austin',
      STATE_REGION: 'Texas',
      COUNTRY: 'United States',
      GLOBAL: 'Worldwide',
    });
  });

  it('drops the state scope for a place without one', () => {
    expect(availableScopes(austin)).toEqual(['CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL']);
    expect(availableScopes(banjul)).toEqual(['CITY', 'COUNTRY', 'GLOBAL']);
  });

  it('lists Nearby first in the feed options', () => {
    expect(feedAudienceOptions(banjul).map((option) => option.label)).toEqual([
      'Nearby',
      'Banjul',
      'The Gambia',
      'Worldwide',
    ]);
  });

  it('says who each posting audience reaches', () => {
    expect(postAudienceOptions(austin).map((option) => option.label)).toEqual([
      'Austin · your city',
      'Texas · your state',
      'United States · your country',
      'Worldwide · anyone',
    ]);
  });

  it('joins only the parts of the place that exist', () => {
    expect(placeLine(austin)).toBe('Austin, Texas, United States');
    expect(placeLine(banjul)).toBe('Banjul, The Gambia');
  });

  describe('remembering the choice', () => {
    beforeEach(() => window.localStorage.clear());

    it('round-trips a valid audience', () => {
      storeAudience('GLOBAL');
      expect(readStoredAudience()).toBe('GLOBAL');
    });

    it('ignores anything that is not an audience', () => {
      window.localStorage.setItem('opencircle.feed-audience', 'CAMPUS');
      expect(readStoredAudience()).toBeNull();
    });
  });
});
