import type { InfiniteData } from '@tanstack/react-query';
import { describe, expect, it } from 'vitest';
import { banter } from '@/test/mocks/fixtures';
import type { BanterPage } from '../api/contracts';
import { adjustReplyCount, applyLike, removeBanter, setLikeState } from './banterCache';

function pagesOf(...items: (typeof banter)[]): InfiniteData<BanterPage> {
  return {
    pageParams: [0],
    pages: [{ items, page: 0, size: 20, totalElements: items.length, totalPages: 1 }],
  };
}

const other = { ...banter, id: 'other-id', likeCount: 5 };

describe('banterCache', () => {
  it('applyLike flips the heart and bumps the count for just that banter', () => {
    const result = applyLike(pagesOf(banter, other), banter.id, true);

    expect(result?.pages[0]?.items[0]).toMatchObject({ likedByMe: true, likeCount: 3 });
    expect(result?.pages[0]?.items[1]).toBe(other);
  });

  it('applyLike on unlike lowers the count and never goes below zero', () => {
    const liked = { ...banter, likedByMe: true, likeCount: 0 };

    const result = applyLike(pagesOf(liked), banter.id, false);

    expect(result?.pages[0]?.items[0]).toMatchObject({ likedByMe: false, likeCount: 0 });
  });

  it('applyLike does nothing when the state already matches', () => {
    const data = pagesOf({ ...banter, likedByMe: true });

    expect(applyLike(data, banter.id, true)?.pages[0]?.items[0]).toBe(data.pages[0]?.items[0]);
  });

  it('setLikeState replaces the numbers with the server values', () => {
    const result = setLikeState(pagesOf(banter), banter.id, { likeCount: 9, likedByMe: true });

    expect(result?.pages[0]?.items[0]).toMatchObject({ likeCount: 9, likedByMe: true });
  });

  it('adjustReplyCount adds and removes one reply, never below zero', () => {
    expect(adjustReplyCount(pagesOf(banter), banter.id, 1)?.pages[0]?.items[0]?.replyCount).toBe(2);
    expect(
      adjustReplyCount(pagesOf({ ...banter, replyCount: 0 }), banter.id, -1)?.pages[0]?.items[0]
        ?.replyCount,
    ).toBe(0);
  });

  it('removeBanter drops it from every page and lowers the total', () => {
    const result = removeBanter(pagesOf(banter, other), banter.id);

    expect(result?.pages[0]?.items).toEqual([other]);
    expect(result?.pages[0]?.totalElements).toBe(1);
  });

  it('passes undefined through untouched', () => {
    expect(applyLike(undefined, banter.id, true)).toBeUndefined();
    expect(removeBanter(undefined, banter.id)).toBeUndefined();
  });
});
