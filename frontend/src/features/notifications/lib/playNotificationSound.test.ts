import { afterEach, describe, expect, it, vi } from 'vitest';

function createFakeAudioContext(initialState: 'running' | 'suspended' = 'running') {
  const oscillators: {
    type: string;
    frequency: { value: number };
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
    connect: ReturnType<typeof vi.fn>;
  }[] = [];
  const gains: {
    gain: {
      setValueAtTime: ReturnType<typeof vi.fn>;
      linearRampToValueAtTime: ReturnType<typeof vi.fn>;
      exponentialRampToValueAtTime: ReturnType<typeof vi.fn>;
    };
    connect: ReturnType<typeof vi.fn>;
  }[] = [];
  const instances: FakeAudioContext[] = [];

  class FakeAudioContext {
    currentTime = 0;
    state: 'running' | 'suspended' = initialState;
    destination = {};
    resume = vi.fn<() => Promise<void>>().mockResolvedValue(undefined);

    constructor() {
      instances.push(this);
    }

    createOscillator() {
      const oscillator = {
        type: 'sine',
        frequency: { value: 0 },
        start: vi.fn<(when?: number) => void>(),
        stop: vi.fn<(when?: number) => void>(),
        connect: vi.fn<(destination: unknown) => void>(),
      };
      oscillators.push(oscillator);
      return oscillator;
    }

    createGain() {
      const gain = {
        gain: {
          setValueAtTime: vi.fn<(value: number, startTime: number) => void>(),
          linearRampToValueAtTime: vi.fn<(value: number, endTime: number) => void>(),
          exponentialRampToValueAtTime: vi.fn<(value: number, endTime: number) => void>(),
        },
        connect: vi.fn<(destination: unknown) => void>(),
      };
      gains.push(gain);
      return gain;
    }
  }

  return { FakeAudioContext, oscillators, gains, instances };
}

// Each test imports the module fresh (via resetModules) since it caches a
// single AudioContext at module scope — reusing that cache across tests
// would leak state (e.g. a later test seeing an earlier test's fake context).
async function importFresh() {
  vi.resetModules();
  return import('./playNotificationSound');
}

describe('playNotificationSound', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('does nothing when the Web Audio API is unavailable', async () => {
    const { playNotificationSound } = await importFresh();

    expect(() => playNotificationSound()).not.toThrow();
  });

  it('resumes the context, then plays two connected, started oscillators', async () => {
    const { FakeAudioContext, instances, oscillators, gains } = createFakeAudioContext();
    vi.stubGlobal('AudioContext', FakeAudioContext);
    const { playNotificationSound } = await importFresh();

    playNotificationSound();

    expect(instances[0]!.resume).toHaveBeenCalled();
    await instances[0]!.resume.mock.results[0]!.value;

    expect(oscillators).toHaveLength(2);
    expect(gains).toHaveLength(2);
    oscillators.forEach((oscillator, index) => {
      expect(oscillator.connect).toHaveBeenCalledWith(gains[index]);
      expect(oscillator.start).toHaveBeenCalled();
      expect(oscillator.stop).toHaveBeenCalled();
    });
    gains.forEach((gain) => {
      expect(gain.connect).toHaveBeenCalled();
      expect(gain.gain.setValueAtTime).toHaveBeenCalled();
      expect(gain.gain.exponentialRampToValueAtTime).toHaveBeenCalled();
    });
  });

  it('plays once resume resolves, for a context that started out suspended', async () => {
    const { FakeAudioContext, instances, oscillators } = createFakeAudioContext('suspended');
    vi.stubGlobal('AudioContext', FakeAudioContext);
    const { playNotificationSound } = await importFresh();

    playNotificationSound();

    // Nothing scheduled yet — resume() hasn't resolved.
    expect(oscillators).toHaveLength(0);

    await instances[0]!.resume.mock.results[0]!.value;

    expect(oscillators).toHaveLength(2);
  });

  it('reuses the same audio context across repeated calls', async () => {
    const { FakeAudioContext, instances } = createFakeAudioContext();
    vi.stubGlobal('AudioContext', FakeAudioContext);
    const { playNotificationSound } = await importFresh();

    playNotificationSound();
    await instances[0]!.resume.mock.results[0]!.value;
    playNotificationSound();

    expect(instances).toHaveLength(1);
  });

  it('logs instead of throwing when the context never resumes', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    class StuckAudioContext {
      currentTime = 0;
      state: 'running' | 'suspended' = 'suspended';
      destination = {};
      resume = vi
        .fn<() => Promise<void>>()
        .mockRejectedValue(new Error('blocked by autoplay policy'));
      createOscillator = vi.fn<() => never>();
      createGain = vi.fn<() => never>();
    }
    vi.stubGlobal('AudioContext', StuckAudioContext);
    const { playNotificationSound } = await importFresh();

    playNotificationSound();
    await vi.waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Failed to play notification sound',
      expect.any(Error),
    );
    consoleErrorSpy.mockRestore();
  });
});

describe('unlockAudioContext', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('does nothing when the Web Audio API is unavailable', async () => {
    const { unlockAudioContext } = await importFresh();

    expect(() => unlockAudioContext()).not.toThrow();
  });

  it('resumes a suspended context', async () => {
    const { FakeAudioContext, instances } = createFakeAudioContext('suspended');
    vi.stubGlobal('AudioContext', FakeAudioContext);
    const { unlockAudioContext } = await importFresh();

    unlockAudioContext();

    expect(instances).toHaveLength(1);
    expect(instances[0]!.resume).toHaveBeenCalled();
  });

  it('does not resume an already-running context', async () => {
    const { FakeAudioContext, instances } = createFakeAudioContext('running');
    vi.stubGlobal('AudioContext', FakeAudioContext);
    const { unlockAudioContext } = await importFresh();

    unlockAudioContext();

    expect(instances).toHaveLength(1);
    expect(instances[0]!.resume).not.toHaveBeenCalled();
  });
});
