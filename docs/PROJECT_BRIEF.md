# OpenCircle Project Brief

## Document Status

- **Product:** OpenCircle
- **Status:** Implemented and deployed
- **Production frontend:** [open-circle-puce.vercel.app](https://open-circle-puce.vercel.app)
- **Production API:** [backend-production-1f35.up.railway.app](https://backend-production-1f35.up.railway.app)

## Product Overview

OpenCircle is a location based social platform that helps people discover activities and make plans with others nearby.

Users publish temporary invite posts, choose who can see them, review requests from interested people, and coordinate accepted activities through real time chat. OpenCircle also supports persistent community discussions, profiles, ratings, notifications, and annual community recognition.

The product does not require users to build a follower network before participating.

## Product Goals

OpenCircle is designed to:

- Make discovering nearby activities simple
- Help users turn ideas into real plans
- Give users control over who joins their activities
- Support both individual and group invitations
- Encourage trustworthy interactions through ratings and reputation
- Protect personal information through limited location exposure
- Provide clear account and privacy controls

## Target Users

OpenCircle is intended for people who want to:

- Find others interested in nearby activities
- Organize casual plans or group events
- Meet people after moving to a new city
- Find study, fitness, travel, food, or entertainment companions
- Participate in local or global community discussions

## Core User Journey

1. A user creates an account and verifies their email.
2. The user chooses a unique username and provides profile information.
3. The user sets and verifies their location.
4. The user browses local or global invite posts.
5. The user publishes an invite for an activity.
6. Other users request to join the invite.
7. The invite owner accepts, declines, or holds each request.
8. Accepted users coordinate through a chat room.
9. Participants can rate each other after the interaction.
10. Ratings and completed interactions contribute to community reputation and scoring.

## Authentication and Accounts

Users can:

- Register with an email address, username, password, name, date of birth, and location
- Verify their email using a six digit code
- Sign in and maintain a secure authenticated session
- Request a password reset code
- Change their password
- Change and verify their email address
- Review and revoke active sessions
- Sign out from all devices
- Delete their account immediately

Authentication responses must not reveal whether an email address belongs to an existing account.

Deleting an account has no grace period. Account owned information and associated stored files are removed as part of the deletion process.

## Profiles

Each user has a profile containing:

- Username
- First and last name
- Profile picture
- Biography
- Interests
- Broad location
- Open invite posts
- Reputation information
- Community awards

Users can edit their own profile and username. Other authenticated users can view public profile information and profile pictures.

## Location

Location determines which local invite posts a user can discover.

Supported visibility levels are:

- City
- State or region
- Country
- Global

A user can see a location scoped invite only when their verified location matches the scope selected by the invite owner.

The application stores broad location information for discovery. Device coordinates are used during location verification and are not presented publicly as a user’s exact location.

## Invite Posts

Invite posts represent temporary plans or activities.

An invite post includes:

- Activity description
- Single person or group invite type
- Group capacity when applicable
- Visibility scope
- Up to five topic tags
- Up to four images
- Creation and expiration timestamps

Invite posts expire automatically after 24 hours.

The invite owner can:

- Edit the invite
- Delete the invite
- Review engagement requests
- Accept, decline, or hold requests
- Manage accepted participants

Single person invites have a capacity of one. Group invites must have a capacity of at least two and close when no spaces remain.

Invite images must be JPEG, PNG, or WebP files and cannot exceed 5 MB each.

## Feeds

OpenCircle provides two primary invite feeds.

### Local Feed

The local feed contains active invites that match the user’s verified city, state or region, or country.

### Global Feed

The global feed contains active invites published for the global audience.

Expired invites are excluded from both feeds.

Users can browse by audience and use topic tags to discover relevant plans.

## Engagement Requests

An authenticated user can request to join an active invite they do not own.

An engagement request can be:

- Pending
- Accepted
- Declined
- Held
- Withdrawn

The requester can withdraw a pending request. The invite owner controls acceptance and cannot exceed the invite’s remaining capacity.

Accepting a request grants the participant access to the related chat experience.

## Chat

Accepted engagement requests create or update a chat room for the invite.

Chat supports:

- Real time messages
- Participant presence
- Image and PDF attachments
- Saved chats
- Hidden chats protected by a PIN
- Leaving a chat
- Participant removal by the invite owner

Chat attachments cannot exceed 10 MB. Supported attachment formats are JPEG, PNG, WebP, and PDF.

Private attachments are stored in S3 and accessed through temporary signed URLs.

## Banter

Banter provides persistent community discussions that do not expire with invite posts.

Users can:

- Publish local or global discussions
- Reply to discussions
- Like and unlike discussions
- Delete discussions they own
- Delete replies they own

Banter is intended for ongoing public conversation rather than activity coordination.

## Notifications

Users receive notifications for relevant account and community events, including:

- Engagement request activity
- Invite expiration
- Chat activity
- Rating availability
- Other supported account events

Notifications are delivered through the API and updated in real time through WebSocket connections.

Users can mark individual notifications or all notifications as read.

## Ratings and Reputation

Users can rate eligible participants after an interaction.

The rating system:

- Restricts ratings to eligible completed interactions
- Prevents duplicate ratings for the same interaction
- Displays received ratings and reputation
- Contributes to community scoring and recognition

Ratings are intended to help users make informed decisions when reviewing future engagement requests.

## Scores and Awards

OpenCircle maintains annual community scores based on qualifying activity and reputation signals.

Users can view:

- Their current score
- The community scoreboard
- Annual Hall of Fame results
- Awards displayed on user profiles

Scoring and awards encourage positive participation without requiring a follower count.

## AI Assistance

OpenCircle can use Gemini to provide:

- Structured invite drafts from casual descriptions
- Feed insights based on available invites and profile interests

AI requests are rate limited. When Gemini is unavailable or not configured, the application uses deterministic built in fallback rules so core workflows remain available.

## Account Settings

Account settings include:

- Personal information
- Password management
- Email management
- Hidden chat PIN
- Active sessions
- Appearance preferences
- Immediate account deletion

Sensitive actions require appropriate authentication and confirmation.

## Security Requirements

OpenCircle must:

- Hash passwords and security PINs using BCrypt
- Use short lived signed access tokens
- Store refresh sessions in secure HTTP only cookies
- Support session revocation
- Restrict cross origin requests to approved frontend origins
- Validate all request data on the backend
- Enforce upload type and size limits
- Use temporary signed URLs for private S3 files
- Expire email verification and password reset codes
- Limit verification and reset attempts
- Avoid exposing account existence through recovery endpoints
- Return controlled JSON errors without exposing credentials or internal details

## Privacy Requirements

OpenCircle must:

- Display broad location information rather than exact coordinates
- Restrict private chats to authorized participants
- Restrict profile and invite modifications to their owners
- Restrict stored file access through expiring URLs
- Remove account owned data and files during immediate account deletion
- Avoid logging passwords, authentication tokens, verification codes, or private message content

## Operational Requirements

The production system consists of:

- React frontend deployed through Vercel
- Spring Boot backend deployed through Railway
- Railway PostgreSQL database
- AWS S3 private file storage
- Brevo transactional email
- WebSocket communication using STOMP
- Flyway managed database migrations
- GitHub Actions continuous integration

The backend exposes a health endpoint for deployment monitoring.

## Quality Requirements

Every change should maintain:

- Responsive desktop and mobile behavior
- Accessible labels and keyboard navigation
- Consistent API error responses
- Backend unit and integration coverage
- Frontend unit and component coverage
- Browser coverage for critical authentication flows
- Successful formatting, linting, tests, and production builds
- Compatibility with direct frontend route navigation

## Current Status

The following systems are implemented and operational:

- Registration and email verification
- Login, logout, sessions, and password recovery
- User profiles and profile pictures
- Location verification
- Local and global invite feeds
- Invite creation, editing, deletion, tags, and images
- Engagement request management
- Real time chat and attachments
- Notifications
- Banter discussions
- Ratings and reputation
- Annual scoring and awards
- Account settings
- Immediate account deletion
- AI assisted invite drafting and feed insights
- Production deployment and continuous integration

## Supporting Documentation

- [Repository README](../README.md)
- Local Swagger UI: `http://localhost:8080/swagger-ui.html`
- Local OpenAPI document: `http://localhost:8080/v3/api-docs`
