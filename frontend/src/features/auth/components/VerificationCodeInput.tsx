import { OTPInput, REGEXP_ONLY_DIGITS, type SlotProps } from 'input-otp';
import { cn } from '@/lib/utils';

function CodeSlot({ char, hasFakeCaret, isActive }: SlotProps) {
  return (
    <div
      className={cn(
        'border-input bg-background relative flex aspect-square min-w-0 flex-1 items-center justify-center rounded-md border text-lg font-semibold shadow-xs transition-[border-color,box-shadow]',
        isActive && 'border-ring ring-ring/20 ring-3',
      )}
    >
      {char}
      {hasFakeCaret ? (
        <span className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <span className="bg-foreground h-5 w-px animate-pulse" />
        </span>
      ) : null}
    </div>
  );
}

type VerificationCodeInputProps = {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  invalid?: boolean;
  describedBy?: string;
};

export function VerificationCodeInput({
  id,
  value,
  onChange,
  invalid,
  describedBy,
}: VerificationCodeInputProps) {
  return (
    <OTPInput
      id={id}
      value={value}
      onChange={onChange}
      maxLength={6}
      pattern={REGEXP_ONLY_DIGITS}
      inputMode="numeric"
      autoComplete="one-time-code"
      aria-label="Verification code"
      aria-invalid={invalid}
      aria-describedby={describedBy}
      containerClassName="w-full"
      render={({ slots }) => (
        <div className="flex w-full gap-2">
          {slots.map((slot, index) => (
            <CodeSlot key={index} {...slot} />
          ))}
        </div>
      )}
    />
  );
}
