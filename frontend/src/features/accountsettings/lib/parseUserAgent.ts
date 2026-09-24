export type ParsedUserAgent = {
  browser: string;
  os: string | null;
};

const BROWSERS: { pattern: RegExp; name: string }[] = [
  { pattern: /Edg\//, name: 'Edge' },
  { pattern: /OPR\//, name: 'Opera' },
  { pattern: /Firefox\//, name: 'Firefox' },
  { pattern: /CriOS\//, name: 'Chrome' },
  { pattern: /Chrome\//, name: 'Chrome' },
  { pattern: /Version\/.*Safari\//, name: 'Safari' },
  { pattern: /Safari\//, name: 'Safari' },
];

const OPERATING_SYSTEMS: { pattern: RegExp; name: string }[] = [
  { pattern: /iPhone|iPad|iPod/, name: 'iOS' },
  { pattern: /Android/, name: 'Android' },
  { pattern: /Mac OS X/, name: 'macOS' },
  { pattern: /Windows/, name: 'Windows' },
  { pattern: /Linux/, name: 'Linux' },
];

export function parseUserAgent(userAgent: string | null): ParsedUserAgent {
  if (!userAgent) {
    return { browser: 'Unknown device', os: null };
  }

  const browser = BROWSERS.find(({ pattern }) => pattern.test(userAgent))?.name ?? null;
  const os = OPERATING_SYSTEMS.find(({ pattern }) => pattern.test(userAgent))?.name ?? null;

  if (!browser && !os) {
    return { browser: 'Unknown device', os: null };
  }

  return { browser: browser ?? 'Unknown browser', os };
}

export function formatUserAgentLabel(userAgent: string | null): string {
  const { browser, os } = parseUserAgent(userAgent);
  return os ? `${browser} on ${os}` : browser;
}
