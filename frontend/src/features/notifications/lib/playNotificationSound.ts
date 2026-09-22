type AudioContextConstructor = typeof AudioContext;

let sharedContext: AudioContext | null = null;

function getAudioContext(): AudioContext | null {
  if (typeof window === 'undefined') return null;

  const AudioContextClass: AudioContextConstructor | undefined =
    window.AudioContext ??
    (window as unknown as { webkitAudioContext?: AudioContextConstructor }).webkitAudioContext;
  if (!AudioContextClass) return null;

  sharedContext ??= new AudioContextClass();
  return sharedContext;
}

function playTone(context: AudioContext, frequency: number, startTime: number, duration: number) {
  const oscillator = context.createOscillator();
  const gain = context.createGain();
  oscillator.type = 'sine';
  oscillator.frequency.value = frequency;
  oscillator.connect(gain);
  gain.connect(context.destination);

  // Quick fade in/out so each tone doesn't click at its start/end edges.
  gain.gain.setValueAtTime(0, startTime);
  gain.gain.linearRampToValueAtTime(0.2, startTime + 0.02);
  gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

  oscillator.start(startTime);
  oscillator.stop(startTime + duration);
}

function scheduleChime(context: AudioContext): void {
  const now = context.currentTime;
  playTone(context, 880, now, 0.12);
  playTone(context, 1318.5, now + 0.1, 0.18);
}

/**
 * A synthesized two-note chime rather than a shipped audio file, so there's
 * no binary asset or licensing to manage. Silently does nothing where the
 * Web Audio API isn't available (e.g. in tests, or a locked-down browser).
 *
 * Always resumes before scheduling, even when `state` already reads
 * "running" — resuming an already-running context is a documented no-op, and
 * this avoids depending on `state` being reported accurately at the instant
 * we check it (observed unreliable in Safari, e.g. after the tab was
 * backgrounded). A context that's still suspended after resume() — most
 * often a per-site autoplay/media setting blocking audio outright, which no
 * amount of in-page unlocking can get around — logs instead of failing
 * silently, so it's diagnosable from the console.
 */
export function playNotificationSound(): void {
  const context = getAudioContext();
  if (!context) return;

  context
    .resume()
    .then(() => scheduleChime(context))
    .catch((error: unknown) => {
      console.error('Failed to play notification sound', error);
    });
}

/**
 * Browsers keep a freshly created AudioContext suspended until the page has
 * had a user gesture — a notification arriving over the WebSocket before
 * the user has clicked or typed anything would otherwise play nothing, with
 * no error. Call this from the first pointerdown/keydown on the page so the
 * context is already running by the time a real notification shows up.
 */
export function unlockAudioContext(): void {
  const context = getAudioContext();
  if (context?.state === 'suspended') {
    void context.resume().catch(() => undefined);
  }
}
