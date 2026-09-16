import { BrowserRouter } from 'react-router-dom';
import { AppErrorBoundary } from './AppErrorBoundary';
import { AppProviders } from './providers';
import { AppRoutes } from './routes';

export function App() {
  return (
    <AppErrorBoundary>
      <AppProviders>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AppProviders>
    </AppErrorBoundary>
  );
}
