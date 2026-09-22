import type { RatingTrigger } from '../api/contracts';

const TRIGGER_COPY: Record<RatingTrigger, string> = {
  PARTICIPANT_EXIT: 'You both left the chat',
  CHAT_INACTIVITY: "The chat's gone quiet",
  MAX_DURATION: "It's been two weeks",
};

export function copyForTrigger(trigger: RatingTrigger): string {
  return TRIGGER_COPY[trigger];
}
