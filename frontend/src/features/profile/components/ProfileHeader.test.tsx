import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { userProfile } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ProfileHeader } from './ProfileHeader';

describe('ProfileHeader', () => {
  it('copies the profile link and briefly shows Copied', async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText');
    renderWithProviders(
      <ProfileHeader
        profile={userProfile}
        isOwnProfile={false}
        editing={false}
        onEdit={() => {}}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Copy link' }));

    expect(writeText).toHaveBeenCalledWith(
      `${window.location.origin}/profile/${userProfile.userId}`,
    );
    expect(await screen.findByRole('button', { name: 'Copied' })).toBeInTheDocument();
  });

  it.each([
    { label: 'on someone else', isOwnProfile: false, editing: false, visible: false },
    { label: 'on your own profile', isOwnProfile: true, editing: false, visible: true },
    { label: 'while editing', isOwnProfile: true, editing: true, visible: false },
  ])('Edit profile button $label: visible=$visible', ({ isOwnProfile, editing, visible }) => {
    renderWithProviders(
      <ProfileHeader
        profile={userProfile}
        isOwnProfile={isOwnProfile}
        editing={editing}
        onEdit={() => {}}
      />,
    );

    expect(screen.queryByRole('button', { name: 'Edit profile' }) !== null).toBe(visible);
  });
});
