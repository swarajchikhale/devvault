# DevVault Database Design

## 1. Overview

DevVault uses PostgreSQL as its primary relational database.

The database is responsible for storing user accounts and the developer content created by users.

Initial entities:

- Users
- Notes
- Snippets
- Resources

The database design prioritizes:

- Data integrity
- Clear relationships
- Simple queries
- Security
- Extensibility
- Maintainability

---

## 2. Entity Relationship Overview

```text
                    ┌───────────────┐
                    │     users     │
                    ├───────────────┤
                    │ id            │
                    │ username      │
                    │ email         │
                    │ password_hash │
                    │ created_at    │
                    │ updated_at    │
                    └───────┬───────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
      ┌────────────┐ ┌────────────┐ ┌──────────────┐
      │   notes    │ │  snippets  │ │  resources   │
      ├────────────┤ ├────────────┤ ├──────────────┤
      │ id         │ │ id         │ │ id           │
      │ user_id    │ │ user_id    │ │ user_id      │
      │ title      │ │ title      │ │ title        │
      │ content    │ │ description│ │ url          │
      │ category   │ │ code       │ │ description  │
      │ created_at │ │ language   │ │ category     │
      │ updated_at │ │ created_at │ │ created_at   │
      └────────────┘ │ updated_at │ │ updated_at   │
                     └────────────┘ └──────────────┘

Each note, snippet, and resource belongs to a user.

3. Users

The ```users``` table stores account information.

Table
users
Columns
Column	Type	Constraints	Description
id	UUID	PRIMARY KEY	Unique user identifier
username	VARCHAR(50)	NOT NULL, UNIQUE	User's display/login name
email	VARCHAR(255)	NOT NULL, UNIQUE	User's email
password_hash	VARCHAR(255)	NOT NULL	BCrypt password hash
created_at	TIMESTAMP	NOT NULL	Account creation time
updated_at	TIMESTAMP	NOT NULL	Last update time
Notes

Passwords must never be stored directly.

Only the BCrypt hash should be stored.

The email address must be unique.

The username must be unique.

4. Notes

The ```notes``` table stores user-created developer notes.

Table
notes
Columns
Column	Type	Constraints	Description
id	UUID	PRIMARY KEY	Unique note identifier
user_id	UUID	FOREIGN KEY, NOT NULL	Owner of the note
title	VARCHAR(200)	NOT NULL	Note title
content	TEXT	NOT NULL	Note content
category	VARCHAR(100)	NULL	Optional category
created_at	TIMESTAMP	NOT NULL	Creation time
updated_at	TIMESTAMP	NOT NULL	Last update time
Relationship
```
users 1 ─────────── N notes
```
One user can have many notes.

Each note belongs to exactly one user.

5. Snippets

The ```snippets``` table stores reusable code snippets.

Table
snippets
Columns
Column	Type	Constraints	Description
id	UUID	PRIMARY KEY	Unique snippet identifier
user_id	UUID	FOREIGN KEY, NOT NULL	Owner of the snippet
title	VARCHAR(200)	NOT NULL	Snippet title
description	TEXT	NULL	Optional explanation
code	TEXT	NOT NULL	Source code
language	VARCHAR(50)	NOT NULL	Programming language
created_at	TIMESTAMP	NOT NULL	Creation time
updated_at	TIMESTAMP	NOT NULL	Last update time
Relationship
```users 1 ─────────── N snippets```

One user can have many snippets.

Each snippet belongs to exactly one user.

6. Resources

The ```resources``` table stores useful developer links.

Table
resources
Columns
Column	Type	Constraints	Description
id	UUID	PRIMARY KEY	Unique resource identifier
user_id	UUID	FOREIGN KEY, NOT NULL	Owner of the resource
title	VARCHAR(200)	NOT NULL	Resource title
url	VARCHAR(2048)	NOT NULL	Resource URL
description	TEXT	NULL	Optional description
category	VARCHAR(100)	NULL	Optional category
created_at	TIMESTAMP	NOT NULL	Creation time
updated_at	TIMESTAMP	NOT NULL	Last update time
Relationship
```users 1 ─────────── N resources```

One user can have many resources.

Each resource belongs to exactly one user.

7. Primary Keys

DevVault will use UUIDs for primary keys.

Example:

550e8400-e29b-41d4-a716-446655440000
Why UUID?

UUIDs provide:

Globally unique identifiers
Less predictable resource IDs
Easier future distributed-system compatibility
No need to expose sequential database IDs

Sequential IDs such as:
```
1
2
3
4
```
will not be used for application entities.

8. Foreign Keys

Content tables reference the user who owns the content.

Example:
```
notes.user_id → users.id
snippets.user_id → users.id
resources.user_id → users.id
```
This allows the application to enforce ownership.

9. Ownership and Authorization

Every user-owned resource must be associated with an owner.

For example:
```
User A
 ├── Note A1
 ├── Note A2
 └── Snippet A1


User B
 ├── Note B1
 └── Resource B1
```
The backend must verify ownership before allowing:

Read
Update
Delete

operations.

A user must not be able to access another user's private resources by changing an ID in an API request.

10. Delete Behavior

For v0.1, user-owned content will use cascading deletion from the user.

Conceptually:
```
Delete User
     │
     ├── Delete Notes
     ├── Delete Snippets
     └── Delete Resources
```
However, user deletion will not be implemented until the authentication and account-management requirements are finalized.

The exact database cascade configuration will be validated during implementation.

11. Timestamps

All major entities should contain:
```
created_at
updated_at
```
These timestamps allow the application to:

Sort recent content
Display creation dates
Track modifications
Support future audit functionality

The backend should manage these values consistently.

12. Indexes

Indexes will be added where they provide meaningful query improvements.

Initial candidates:
```
users.email
users.username


notes.user_id
snippets.user_id
resources.user_id
```
Additional indexes will be considered when search and filtering requirements become clearer.

We should avoid creating unnecessary indexes because indexes increase storage and write overhead.

13. Categories

For v0.1, categories will be stored as simple strings.

Examples:
```
Java
Python
JavaScript
Database
Git
Backend
Frontend
DevOps
```
A dedicated category table may be introduced later if the requirements justify it.

14. Tags — Future

Tags are intentionally not part of the initial schema.

Future versions may introduce:
```
tags
-----
id
name


note_tags
----------
note_id
tag_id


snippet_tags
------------
snippet_id
tag_id


resource_tags
-------------
resource_id
tag_id
```
This will support flexible filtering and organization.

15. Public Content — Future

The initial version stores private user content.

Future versions may support public content.

For example:
```
snippets
---------
visibility
```
Possible values:
```
PRIVATE
PUBLIC
```
This should not be implemented until the sharing model and authorization rules are properly designed.

16. Community Content — Future

If DevVault evolves into a community platform, we may introduce additional concepts such as:
```
public_snippets
community_resources
favorites
votes
comments
contributors
```
These are intentionally outside the v0.1 database.

17. Data Validation

Database constraints should provide a final layer of data integrity.

Examples:

Required fields cannot be NULL.
User email must be unique.
Username must be unique.
Foreign keys must reference valid users.
URLs must be validated by the application.
String length limits must be respected.

Application-level validation should happen before database operations.

18. Migration Strategy

Database schema changes will be version-controlled.

A database migration tool such as Flyway will be considered for schema management during backend implementation.

The goal is to ensure that developers and CI environments can reproduce the same database schema.

19. Database Security

The application must:

Never store plain-text passwords.
Never commit database credentials.
Store database credentials in environment variables or secure configuration.
Use separate development and production credentials.
Avoid exposing database ports publicly unless necessary.
Use least-privilege database access where practical.

Secrets must never be committed to GitHub.

20. Initial Schema Summary
```
users
 │
 ├────────── notes
 │
 ├────────── snippets
 │
 └────────── resources
 ```
v0.1
```
Users
Notes
Snippets
Resources
```
Future
```
Tags
Public Content
Favorites
Votes
Community Resources
Comments
Contributor Profiles
```
The database should evolve only when a feature requires it.

21. Design Principles

The DevVault database should follow these principles:

Keep the initial schema simple.
Maintain referential integrity.
Protect user-owned data.
Avoid premature database complexity.
Use migrations for schema changes.
Add indexes based on actual query requirements.
Keep future extensibility in mind without implementing unnecessary tables.

```
### Save the file.


---


## Why we chose UUIDs


You might notice I changed our earlier rough design from numeric IDs to UUIDs.


That's intentional.


For an open-source web application, UUIDs are a reasonable default because they don't expose simple sequential resource identifiers.


So instead of:


```text
/api/notes/1
/api/notes/2
/api/notes/3
```
we'll eventually have IDs like:
```
/api/notes/550e8400-e29b-41d4-a716-446655440000
```
But remember: UUIDs alone don't provide security. The backend still has to verify that the authenticated user owns the resource.

One more important decision

I've also introduced Flyway as something we'll consider for database migrations.

When we actually create the Spring Boot project, we'll likely use it from the beginning rather than manually changing database tables.

That will make contributor setup much cleaner:
```
Fresh database
      ↓
Run application
      ↓
Flyway migrations
      ↓
Database ready
```