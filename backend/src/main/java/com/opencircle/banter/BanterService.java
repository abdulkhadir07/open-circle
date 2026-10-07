package com.opencircle.banter;

import com.opencircle.user.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class BanterService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BanterRepository banters;
    private final BanterReplyRepository replies;
    private final BanterLikeRepository likes;
    private final Clock clock;

    BanterService(
            BanterRepository banters,
            BanterReplyRepository replies,
            BanterLikeRepository likes,
            Clock clock
    ) {
        this.banters = banters;
        this.replies = replies;
        this.likes = likes;
        this.clock = clock;
    }

    // Returns one page of the shared board with like and reply counts.
    @Transactional(readOnly = true)
    BanterBoard getBoard(AppUser viewer, String sortValue, int page, int size) {
        BanterSort sort = BanterSort.parse(sortValue);

        if (page < 0) {
            throw new InvalidBanterRequestException("Page must be zero or greater");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidBanterRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Banter> result = sort == BanterSort.HOT
                ? banters.findHot(pageRequest)
                : banters.findNewest(pageRequest);

        List<UUID> ids = result.getContent().stream().map(Banter::getId).toList();
        Map<UUID, Long> likeCounts = counts(ids, likes.countByBanterIds(ids));
        Map<UUID, Long> replyCounts = counts(ids, replies.countByBanterIds(ids));
        Set<UUID> liked = ids.isEmpty() ? Set.of() : likes.findLikedBanterIds(viewer.getId(), ids);

        List<BanterView> items = result.getContent().stream()
                .map(banter -> new BanterView(
                        banter,
                        likeCounts.getOrDefault(banter.getId(), 0L),
                        replyCounts.getOrDefault(banter.getId(), 0L),
                        liked.contains(banter.getId())
                ))
                .toList();

        return new BanterBoard(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    BanterView create(AppUser author, String content) {
        Banter banter;
        try {
            banter = new Banter(author, content, Instant.now(clock));
        } catch (IllegalArgumentException exception) {
            throw new InvalidBanterRequestException(exception.getMessage());
        }

        return new BanterView(banters.save(banter), 0, 0, false);
    }

    @Transactional
    void delete(AppUser viewer, UUID banterId) {
        Banter banter = requireBanter(viewer, banterId);

        if (!banter.getAuthor().getId().equals(viewer.getId())) {
            throw new BanterForbiddenException();
        }

        banters.delete(banter);
    }

    @Transactional
    BanterLikeResponse like(AppUser viewer, UUID banterId) {
        requireBanter(viewer, banterId);

        likes.insertIfAbsent(UUID.randomUUID(), banterId, viewer.getId(), Instant.now(clock));

        return new BanterLikeResponse(likes.countByBanterId(banterId), true);
    }

    @Transactional
    BanterLikeResponse unlike(AppUser viewer, UUID banterId) {
        requireBanter(viewer, banterId);

        likes.deleteByBanterIdAndUserId(banterId, viewer.getId());

        return new BanterLikeResponse(likes.countByBanterId(banterId), false);
    }

    @Transactional(readOnly = true)
    List<BanterReply> getReplies(AppUser viewer, UUID banterId) {
        requireBanter(viewer, banterId);

        return replies.findThread(banterId);
    }

    @Transactional
    BanterReply reply(AppUser author, UUID banterId, String content) {
        Banter banter = requireBanter(author, banterId);

        BanterReply reply;
        try {
            reply = new BanterReply(banter, author, content, Instant.now(clock));
        } catch (IllegalArgumentException exception) {
            throw new InvalidBanterRequestException(exception.getMessage());
        }

        return replies.save(reply);
    }

    @Transactional
    void deleteReply(AppUser viewer, UUID banterId, UUID replyId) {
        requireBanter(viewer, banterId);

        BanterReply reply = replies.findInBanter(replyId, banterId)
                .orElseThrow(BanterNotFoundException::new);

        if (!reply.getAuthor().getId().equals(viewer.getId())) {
            throw new BanterForbiddenException();
        }

        replies.delete(reply);
    }

    private Banter requireBanter(AppUser viewer, UUID banterId) {
        return banters.findWithAuthor(banterId)
                .orElseThrow(BanterNotFoundException::new);
    }

    private Map<UUID, Long> counts(List<UUID> ids, List<BanterCount> rows) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return rows.stream().collect(Collectors.toMap(
                BanterCount::getBanterId,
                BanterCount::getTotal,
                Long::sum,
                HashMap::new
        ));
    }
}
