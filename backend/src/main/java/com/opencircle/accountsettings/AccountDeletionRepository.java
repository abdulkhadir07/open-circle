package com.opencircle.accountsettings;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class AccountDeletionRepository {

    private final JdbcTemplate jdbcTemplate;

    AccountDeletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<StoredAccountObject> findStoredObjects(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT s3_bucket, s3_object_key
                FROM profile_images
                WHERE user_id = ?

                UNION

                SELECT image.s3_bucket, image.s3_object_key
                FROM invite_post_images image
                JOIN invite_posts post ON post.id = image.invite_post_id
                WHERE image.uploader_id = ? OR post.poster_id = ?

                UNION

                SELECT attachment.s3_bucket, attachment.s3_object_key
                FROM chat_attachments attachment
                JOIN chat_messages message ON message.id = attachment.chat_message_id
                JOIN chat_rooms room ON room.id = attachment.chat_room_id
                LEFT JOIN invite_posts post ON post.id = room.invite_post_id
                WHERE attachment.uploader_id = ?
                   OR message.sender_id = ?
                   OR post.poster_id = ?
                """,
                (resultSet, rowNumber) -> new StoredAccountObject(
                        resultSet.getString("s3_bucket"),
                        resultSet.getString("s3_object_key")
                ),
                userId,
                userId,
                userId,
                userId,
                userId,
                userId
        );
    }

    void deleteNotificationsRelatedTo(UUID userId) {
        jdbcTemplate.update(
                """
                WITH deleted_resources AS (
                    SELECT id
                    FROM invite_posts
                    WHERE poster_id = ?

                    UNION

                    SELECT request.id
                    FROM engagement_requests request
                    LEFT JOIN invite_posts post ON post.id = request.invite_post_id
                    WHERE request.requester_id = ? OR post.poster_id = ?
                )
                DELETE FROM notifications
                WHERE actor_user_id = ?
                   OR resource_id IN (SELECT id FROM deleted_resources)
                   OR context_id IN (SELECT id FROM deleted_resources)
                """,
                userId,
                userId,
                userId,
                userId
        );
    }
}
