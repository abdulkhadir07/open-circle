import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  type UIEvent,
} from 'react';
import { cn } from '@/lib/utils';

/**
 * Fixed thumb size in px, deliberately decoupled from how much content there is.
 * Native scrollbars can't do this (thumb length is always proportional to content),
 * so this draws its own thumb and maps drag distance to scroll position manually.
 */
const THUMB_SIZE = 32;

type ScrollAreaProps = {
  children: ReactNode;
  className?: string;
  onScroll?: (event: UIEvent<HTMLDivElement>) => void;
};

export const ScrollArea = forwardRef<HTMLDivElement, ScrollAreaProps>(function ScrollArea(
  { children, className, onScroll },
  forwardedRef,
) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [thumb, setThumb] = useState({ top: 0, visible: false });
  const dragRef = useRef<{ pointerY: number; scrollTop: number } | null>(null);

  const setRefs = useCallback(
    (node: HTMLDivElement | null) => {
      containerRef.current = node;
      if (typeof forwardedRef === 'function') forwardedRef(node);
      else if (forwardedRef) forwardedRef.current = node;
    },
    [forwardedRef],
  );

  const updateThumb = useCallback(() => {
    const el = containerRef.current;
    if (!el) return;

    const maxScrollTop = el.scrollHeight - el.clientHeight;
    if (maxScrollTop <= 0) {
      setThumb({ top: 0, visible: false });
      return;
    }

    const maxThumbTop = el.clientHeight - THUMB_SIZE;
    setThumb({ top: (el.scrollTop / maxScrollTop) * maxThumbTop, visible: true });
  }, []);

  // Re-measures whenever the content or the viewport itself changes size, not just on
  // scroll — e.g. a new message arriving can make the thumb appear without any scrolling.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    updateThumb();

    const observer = new ResizeObserver(updateThumb);
    observer.observe(el);
    if (el.firstElementChild) observer.observe(el.firstElementChild);

    return () => observer.disconnect();
  }, [updateThumb]);

  function handleScroll(event: UIEvent<HTMLDivElement>) {
    updateThumb();
    onScroll?.(event);
  }

  function handleThumbPointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    const el = containerRef.current;
    if (!el) return;

    dragRef.current = { pointerY: event.clientY, scrollTop: el.scrollTop };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handleThumbPointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current;
    const el = containerRef.current;
    if (!drag || !el) return;

    const maxScrollTop = el.scrollHeight - el.clientHeight;
    const maxThumbTop = el.clientHeight - THUMB_SIZE;
    if (maxThumbTop <= 0) return;

    const deltaY = event.clientY - drag.pointerY;
    el.scrollTop = drag.scrollTop + (deltaY / maxThumbTop) * maxScrollTop;
  }

  function handleThumbPointerUp(event: ReactPointerEvent<HTMLDivElement>) {
    dragRef.current = null;
    event.currentTarget.releasePointerCapture(event.pointerId);
  }

  return (
    <div className="relative h-full">
      <div
        ref={setRefs}
        onScroll={handleScroll}
        className={cn('h-full scrollbar-none overflow-y-auto', className)}
      >
        {children}
      </div>
      {thumb.visible ? (
        <div
          onPointerDown={handleThumbPointerDown}
          onPointerMove={handleThumbPointerMove}
          onPointerUp={handleThumbPointerUp}
          className="bg-muted-foreground/40 hover:bg-muted-foreground/60 absolute right-1 w-1.5 touch-none rounded-full transition-colors select-none"
          style={{ top: thumb.top, height: THUMB_SIZE }}
        />
      ) : null}
    </div>
  );
});
