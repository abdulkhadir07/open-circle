// Deliberately not under ['invite-posts']: posting an invite invalidates that prefix, and every
// refetch of insights is an AI call.
export const aiQueryKeys = {
  feedInsights: ['ai', 'feed-insights'] as const,
};
