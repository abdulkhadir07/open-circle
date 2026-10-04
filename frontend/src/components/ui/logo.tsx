export function Logo({ size = 32 }: { size?: number }) {
  return (
    <span
      aria-hidden="true"
      className="relative inline-flex shrink-0 items-center justify-center rounded-[30%] shadow-sm"
      style={{
        width: size,
        height: size,
        background: 'linear-gradient(135deg, var(--primary), #a855f7 55%, var(--gold))',
      }}
    >
      <span
        className="rounded-full border-white"
        style={{
          width: size * 0.48,
          height: size * 0.48,
          borderWidth: Math.max(2, size * 0.09),
        }}
      />
      <span
        className="bg-gold absolute rounded-full"
        style={{
          width: size * 0.16,
          height: size * 0.16,
          top: size * 0.2,
          right: size * 0.2,
        }}
      />
    </span>
  );
}
