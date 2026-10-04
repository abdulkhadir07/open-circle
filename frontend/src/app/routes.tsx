import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { NotFoundPage } from '@/components/layout/NotFoundPage';
import { AuthLoadingScreen } from '@/features/auth/components/AuthLoadingScreen';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { PublicOnlyRoute } from '@/features/auth/components/PublicOnlyRoute';

const RootPage = lazy(() => import('./RootPage').then((module) => ({ default: module.RootPage })));
const LoginPage = lazy(() =>
  import('@/features/auth/pages/LoginPage').then((module) => ({ default: module.LoginPage })),
);
const SignupPage = lazy(() =>
  import('@/features/auth/pages/SignupPage').then((module) => ({ default: module.SignupPage })),
);
const VerifyEmailPage = lazy(() =>
  import('@/features/auth/pages/VerifyEmailPage').then((module) => ({
    default: module.VerifyEmailPage,
  })),
);
const ForgotPasswordPage = lazy(() =>
  import('@/features/auth/pages/ForgotPasswordPage').then((module) => ({
    default: module.ForgotPasswordPage,
  })),
);
const ResetPasswordPage = lazy(() =>
  import('@/features/auth/pages/ResetPasswordPage').then((module) => ({
    default: module.ResetPasswordPage,
  })),
);
const NewPasswordPage = lazy(() =>
  import('@/features/auth/pages/NewPasswordPage').then((module) => ({
    default: module.NewPasswordPage,
  })),
);
const NewPostPage = lazy(() =>
  import('@/features/invite-posts/pages/NewPostPage').then((module) => ({
    default: module.NewPostPage,
  })),
);
const EngagementRequestsPage = lazy(() =>
  import('@/features/engagement-requests/pages/EngagementRequestsPage').then((module) => ({
    default: module.EngagementRequestsPage,
  })),
);
const ChatRoomsPage = lazy(() =>
  import('@/features/chat/pages/ChatRoomsPage').then((module) => ({
    default: module.ChatRoomsPage,
  })),
);
const ChatRoomPage = lazy(() =>
  import('@/features/chat/pages/ChatRoomPage').then((module) => ({ default: module.ChatRoomPage })),
);
const NotificationsPage = lazy(() =>
  import('@/features/notifications/pages/NotificationsPage').then((module) => ({
    default: module.NotificationsPage,
  })),
);
const BanterPage = lazy(() =>
  import('@/features/banter/pages/BanterPage').then((module) => ({
    default: module.BanterPage,
  })),
);
const RatingsPage = lazy(() =>
  import('@/features/ratings/pages/RatingsPage').then((module) => ({
    default: module.RatingsPage,
  })),
);
const ScoreboardPage = lazy(() =>
  import('@/features/scoreboard/pages/ScoreboardPage').then((module) => ({
    default: module.ScoreboardPage,
  })),
);
const HallOfFamePage = lazy(() =>
  import('@/features/scoreboard/pages/HallOfFamePage').then((module) => ({
    default: module.HallOfFamePage,
  })),
);
const ProfilePage = lazy(() =>
  import('@/features/profile/pages/ProfilePage').then((module) => ({
    default: module.ProfilePage,
  })),
);
const SettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/SettingsPage').then((module) => ({
    default: module.SettingsPage,
  })),
);
const PasswordSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/PasswordSettingsPage').then((module) => ({
    default: module.PasswordSettingsPage,
  })),
);
const EmailSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/EmailSettingsPage').then((module) => ({
    default: module.EmailSettingsPage,
  })),
);
const PinSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/PinSettingsPage').then((module) => ({
    default: module.PinSettingsPage,
  })),
);
const SessionsSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/SessionsSettingsPage').then((module) => ({
    default: module.SessionsSettingsPage,
  })),
);
const AppearanceSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/AppearanceSettingsPage').then((module) => ({
    default: module.AppearanceSettingsPage,
  })),
);
const PersonalInfoSettingsPage = lazy(() =>
  import('@/features/accountsettings/pages/PersonalInfoSettingsPage').then((module) => ({
    default: module.PersonalInfoSettingsPage,
  })),
);

export function AppRoutes() {
  return (
    <Suspense fallback={<AuthLoadingScreen />}>
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/reset-password/new-password" element={<NewPasswordPage />} />
        </Route>
        <Route path="/" element={<RootPage />} />
        <Route element={<ProtectedRoute />}>
          <Route
            path="/new"
            element={
              <AppLayout>
                <NewPostPage />
              </AppLayout>
            }
          />
          <Route
            path="/requests"
            element={
              <AppLayout>
                <EngagementRequestsPage />
              </AppLayout>
            }
          />
          <Route
            path="/chats"
            element={
              <AppLayout>
                <ChatRoomsPage />
              </AppLayout>
            }
          />
          <Route
            path="/chats/:roomId"
            element={
              <AppLayout fill>
                <ChatRoomPage />
              </AppLayout>
            }
          />
          <Route
            path="/notifications"
            element={
              <AppLayout>
                <NotificationsPage />
              </AppLayout>
            }
          />
          <Route
            path="/banter"
            element={
              <AppLayout>
                <BanterPage />
              </AppLayout>
            }
          />
          <Route
            path="/ratings"
            element={
              <AppLayout>
                <RatingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/scoreboard"
            element={
              <AppLayout>
                <ScoreboardPage />
              </AppLayout>
            }
          />
          <Route
            path="/scoreboard/hall-of-fame"
            element={
              <AppLayout>
                <HallOfFamePage />
              </AppLayout>
            }
          />
          <Route
            path="/profile/:userId"
            element={
              <AppLayout>
                <ProfilePage />
              </AppLayout>
            }
          />
          <Route
            path="/settings"
            element={
              <AppLayout>
                <SettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/password"
            element={
              <AppLayout>
                <PasswordSettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/email"
            element={
              <AppLayout>
                <EmailSettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/pin"
            element={
              <AppLayout>
                <PinSettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/sessions"
            element={
              <AppLayout>
                <SessionsSettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/appearance"
            element={
              <AppLayout>
                <AppearanceSettingsPage />
              </AppLayout>
            }
          />
          <Route
            path="/settings/personal-info"
            element={
              <AppLayout>
                <PersonalInfoSettingsPage />
              </AppLayout>
            }
          />
        </Route>
        <Route
          path="*"
          element={
            <AppLayout>
              <NotFoundPage />
            </AppLayout>
          }
        />
      </Routes>
    </Suspense>
  );
}
