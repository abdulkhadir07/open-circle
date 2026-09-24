import { describe, expect, it } from 'vitest';
import { formatUserAgentLabel } from './parseUserAgent';

describe('formatUserAgentLabel', () => {
  it('identifies Chrome on macOS', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
      ),
    ).toBe('Chrome on macOS');
  });

  it('identifies Safari on macOS, not Chrome', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15',
      ),
    ).toBe('Safari on macOS');
  });

  it('identifies Safari on iOS', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1',
      ),
    ).toBe('Safari on iOS');
  });

  it('identifies Chrome on Android', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36',
      ),
    ).toBe('Chrome on Android');
  });

  it('identifies Firefox on Windows', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0',
      ),
    ).toBe('Firefox on Windows');
  });

  it('identifies Edge on Windows, not Chrome', () => {
    expect(
      formatUserAgentLabel(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0',
      ),
    ).toBe('Edge on Windows');
  });

  it('falls back to a generic label for an unrecognized user agent', () => {
    expect(formatUserAgentLabel('SomeInternalTool/1.0')).toBe('Unknown device');
  });

  it('falls back to a generic label when the user agent is missing', () => {
    expect(formatUserAgentLabel(null)).toBe('Unknown device');
  });
});
