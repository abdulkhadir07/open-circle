```md
# Project Brief

## Overview

OpenCircle is a location-based social app where users discover posts based on location instead of followers.

The app has two main content types:

1. Invite Posts
2. Banter Posts

Invite posts are temporary and expire after 24 hours. Banter posts are discussion-based and do not expire.

## Main User Flow

1. A user signs up and sets their city, state/region, and country.
2. The user creates an invite post.
3. The user chooses visibility: city, state/region, country, or global.
4. Other users who match the visibility rules can see the post.
5. A viewer can click Engage.
6. The poster can accept, decline, or hold the engagement request.
7. If accepted, both users are moved into a chat room.
8. After the interaction, users rate each other.

## Feed Rules

### Local Feed

The Local feed shows posts visible to the user's location.

Example:

A user in San Francisco, California, United States can see:

- city posts for San Francisco
- state/region posts for California
- country posts for United States

### Global Feed

The Global feed shows posts marked as global.

## Invite Post Rules

Invite posts are for real-world or direct social engagement.

Examples:

- "I just moved to San Francisco from The Gambia. Any Gambians here want to hang out?"
- "Wanna hang out next Saturday at the beach?"
- "Anyone studying Java at SFSU tonight?"

Rules:

- Invite posts expire after 24 hours.
- The poster can close a post early.
- A user can request to engage.
- The poster decides who gets accepted.
- Accepted users enter a chat room.
- Invite posts can be single invite or group invite.

## Group Invite Rules

A group invite has a maximum number of accepted users.

Example:

```text
Group invite: 5
Invites left: 4
```

When the poster accepts a request, the number of invites left decreases.

When no invites are left, the post closes automatically.

## Banter Rules

Banter posts are for public discussion.

Example:

```text
Who agrees Messi is better than Pele and why?
```

Rules:

- Banter posts do not expire.
- Banter posts can be local or global.
- Users can reply publicly.
- Replies belong to the original banter post.

## Database Structure

Main tables:

```text
users
invite_posts
engagement_requests
chat_rooms
chat_participants
chat_messages
banter_posts
banter_replies
ratings
reports
```

## API Structure

### Users

```text
POST /api/auth/signup
POST /api/auth/login
GET  /api/users/me
GET  /api/users/{id}
```

### Invite Posts

```text
POST /api/invites
GET  /api/invites/{id}
GET  /api/feed/local
GET  /api/feed/global
POST /api/invites/{id}/close
DELETE /api/invites/{id}
```

### Engagement Requests

```text
POST /api/invites/{id}/engagements
GET  /api/invites/{id}/engagements
POST /api/engagements/{id}/accept
POST /api/engagements/{id}/decline
POST /api/engagements/{id}/hold
POST /api/invites/{id}/engagements/decline-all
```

### Chat

```text
GET  /api/chats
GET  /api/chats/{id}/messages
POST /api/chats/{id}/messages
```

### Banter

```text
POST /api/banter
GET  /api/banter/local
GET  /api/banter/global
GET  /api/banter/{id}
POST /api/banter/{id}/replies
DELETE /api/banter/{id}
```

### Ratings

```text
POST /api/engagements/{id}/ratings
GET  /api/users/{id}/reputation
```

## Testing Plan

### Unit Tests

Unit tests will cover business logic such as:

- invite expiration logic
- location visibility logic
- group invite slot decrease logic
- engagement accept/decline/hold logic
- badge calculation logic

### Integration Tests

Integration tests will cover API and database behavior such as:

- creating a user
- creating an invite post
- fetching the local feed
- hiding city-only posts from users outside the city
- requesting engagement
- accepting engagement and creating a chat room

## CI/CD Plan

GitHub Actions will run on every push and pull request.

Pipeline steps:

```text
checkout code
set up Java
run backend tests
set up Node
run frontend tests
build frontend
```

## Resume Talking Points

This project demonstrates:

- Java Spring Boot backend development
- REST API design
- PostgreSQL relational database design
- authentication and authorization
- location-based feed filtering
- business rule enforcement
- unit and integration testing
- CI/CD using GitHub Actions
- full-stack React and Java development
```