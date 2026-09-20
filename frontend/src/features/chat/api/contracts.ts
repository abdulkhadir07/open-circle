import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

export type ProfileImage = z.infer<typeof profileImageSchema>;

const participantSchema = z
  .object({
    userId: z.string().min(1),
    username: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
    active: z.boolean(),
    joinedAt: z.string().min(1),
    left: z.boolean(),
    leftAt: z.string().nullish(),
    removed: z.boolean(),
    removedAt: z.string().nullish(),
    removedByUserId: z.string().nullish(),
    removedByUsername: z.string().nullish(),
    removedByProfileImage: profileImageSchema.nullish(),
  })
  .passthrough();

export type ChatParticipant = z.infer<typeof participantSchema>;

const chatRoomSchema = z
  .object({
    id: z.string().min(1),
    invitePostId: z.string().min(1),
    invitePostContent: z.string().min(1),
    status: z.enum(['ACTIVE', 'CLOSED']),
    saved: z.boolean(),
    savedAt: z.string().nullish(),
    savedByUserId: z.string().nullish(),
    savedByUsername: z.string().nullish(),
    savedByProfileImage: profileImageSchema.nullish(),
    autoCloseAt: z.string().nullish(),
    closed: z.boolean(),
    closedAt: z.string().nullish(),
    hiddenForCurrentUser: z.boolean(),
    participants: z.array(participantSchema),
    createdAt: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

export type ChatRoom = z.infer<typeof chatRoomSchema>;

const chatRoomListSchema = z.array(chatRoomSchema);

const attachmentSchema = z
  .object({
    id: z.string().min(1),
    originalFilename: z.string().min(1),
    contentType: z.string().min(1),
    fileSizeBytes: z.number(),
  })
  .passthrough();

export type ChatAttachment = z.infer<typeof attachmentSchema>;

const chatMessageSchema = z
  .object({
    id: z.string().min(1),
    roomId: z.string().min(1),
    senderId: z.string().min(1),
    senderUsername: z.string().min(1),
    senderProfileImage: profileImageSchema.nullish(),
    type: z.enum(['TEXT', 'ATTACHMENT', 'PARTICIPANT_LEFT']),
    body: z.string().nullish(),
    attachment: attachmentSchema.nullish(),
    createdAt: z.string().min(1),
  })
  .passthrough();

export type ChatMessage = z.infer<typeof chatMessageSchema>;

const chatMessageListSchema = z.array(chatMessageSchema);

export function parseChatRoom(value: unknown): ChatRoom {
  return parseWithContract(chatRoomSchema, value);
}

export function parseChatRoomList(value: unknown): ChatRoom[] {
  return parseWithContract(chatRoomListSchema, value);
}

export function parseChatMessage(value: unknown): ChatMessage {
  return parseWithContract(chatMessageSchema, value);
}

export function parseChatMessageList(value: unknown): ChatMessage[] {
  return parseWithContract(chatMessageListSchema, value);
}
