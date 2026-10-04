import type { InfiniteData } from '@tanstack/react-query';
import type { Banter, BanterPage } from '../api/contracts';

type BanterPages = InfiniteData<BanterPage>;

/** Applies `change` to the banter with this id wherever it appears in the cached pages. */
function mapBanter(
  data: BanterPages | undefined,
  banterId: string,
  change: (banter: Banter) => Banter,
): BanterPages | undefined {
  if (!data) return data;
  return {
    ...data,
    pages: data.pages.map((page) => ({
      ...page,
      items: page.items.map((banter) => (banter.id === banterId ? change(banter) : banter)),
    })),
  };
}

/** Flips the like state straight away, before the server has answered. */
export function applyLike(data: BanterPages | undefined, banterId: string, liked: boolean) {
  return mapBanter(data, banterId, (banter) => {
    if (banter.likedByMe === liked) return banter;
    return {
      ...banter,
      likedByMe: liked,
      likeCount: Math.max(0, banter.likeCount + (liked ? 1 : -1)),
    };
  });
}

/** Replaces the guessed numbers with what the server says. */
export function setLikeState(
  data: BanterPages | undefined,
  banterId: string,
  state: { likeCount: number; likedByMe: boolean },
) {
  return mapBanter(data, banterId, (banter) => ({ ...banter, ...state }));
}

export function adjustReplyCount(data: BanterPages | undefined, banterId: string, delta: number) {
  return mapBanter(data, banterId, (banter) => ({
    ...banter,
    replyCount: Math.max(0, banter.replyCount + delta),
  }));
}

export function removeBanter(data: BanterPages | undefined, banterId: string) {
  if (!data) return data;
  return {
    ...data,
    pages: data.pages.map((page) => ({
      ...page,
      items: page.items.filter((banter) => banter.id !== banterId),
      totalElements: Math.max(
        0,
        page.totalElements - (page.items.some((banter) => banter.id === banterId) ? 1 : 0),
      ),
    })),
  };
}
