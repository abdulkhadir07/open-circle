import { Component, type ErrorInfo, type ReactNode } from 'react';

type Props = {
  children: ReactNode;
};

type State = {
  error: Error | null;
};

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled application error', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div role="alert" className="flex flex-col items-center gap-4 py-16 text-center">
          <h1 className="text-foreground text-2xl font-semibold">Something went wrong</h1>
          <p className="text-muted-foreground">
            Please refresh the page. If the problem continues, try again later.
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="text-primary underline underline-offset-4"
          >
            Reload
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
