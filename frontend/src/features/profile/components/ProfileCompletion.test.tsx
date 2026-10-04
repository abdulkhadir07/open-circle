import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { ProfileCompletion } from './ProfileCompletion';

describe('ProfileCompletion', () => {
  it('renders nothing once bio, interests and photo are all set', () => {
    const { container } = renderWithProviders(
      <ProfileCompletion hasBio hasInterests hasPhoto onEditProfile={() => {}} />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('lists what is missing and opens editing from the action buttons', async () => {
    const onEditProfile = vi.fn<() => void>();
    renderWithProviders(
      <ProfileCompletion
        hasBio={false}
        hasInterests={false}
        hasPhoto
        onEditProfile={onEditProfile}
      />,
    );

    expect(screen.getByText('Complete your profile')).toBeInTheDocument();
    expect(screen.getByText('1 of 3 done')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Add a bio' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add interests' }));
    expect(onEditProfile).toHaveBeenCalledTimes(2);
  });

  it('hints at clicking the avatar when the photo is missing', () => {
    renderWithProviders(
      <ProfileCompletion hasBio hasInterests hasPhoto={false} onEditProfile={() => {}} />,
    );

    expect(screen.getByText('Click your photo above to upload one.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add a bio' })).not.toBeInTheDocument();
  });
});
