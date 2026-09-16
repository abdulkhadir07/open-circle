import { Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { NotFoundPage } from '@/components/layout/NotFoundPage';
import { HomePage } from './HomePage';

/**
 * Declarative route tree (React Router v7 Declarative Mode — TanStack Query
 * owns data loading, not router loaders). Public/protected route boundaries
 * are added in feature/frontend-auth once the auth store exists.
 */
export function AppRoutes() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppLayout>
  );
}
