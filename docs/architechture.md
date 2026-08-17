# DevVault Architecture

## 1. Overview

DevVault is an open-source developer productivity and knowledge platform.

The application is designed as a modular monolith with a React frontend, Spring Boot backend, and PostgreSQL database.

The architecture prioritizes:

- Simplicity
- Maintainability
- Testability
- Security
- Clear separation of responsibilities
- Easy onboarding for open-source contributors
- Future extensibility

---

## 2. High-Level Architecture

```
                    ┌───────────────────┐
                    │      Browser      │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │   React Frontend  │
                    │                   │
                    │  Authentication   │
                    │  Dashboard        │
                    │  Notes            │
                    │  Snippets         │
                    │  Resources        │
                    │  Search            │
                    └─────────┬─────────┘
                              │
                         REST / JSON
                              │
                              ▼
                    ┌───────────────────┐
                    │   Spring Boot API │
                    │                   │
                    │ Authentication    │
                    │ Notes             │
                    │ Snippets          │
                    │ Resources         │
                    │ Search            │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │    PostgreSQL     │
                    │                   │
                    │ Users             │
                    │ Notes             │
                    │ Snippets          │
                    │ Resources         │
                    └───────────────────┘
```
3. Architecture Style

DevVault uses a modular monolith architecture.

The application is deployed as a single backend application, but its code is organized into independent functional modules.

Initial modules:
```
Authentication
Notes
Snippets
Resources
Search
```
Why a Modular Monolith?

A modular monolith provides:

Lower operational complexity than microservices
Easier local development
Easier testing
Simple deployment
Clear module boundaries
A lower barrier for new contributors

Microservices may be considered in the future only if the project reaches a scale where they provide a meaningful benefit.

4. Backend Architecture

The backend uses Spring Boot.

Backend modules are organized by feature rather than by global technical layers.

Example:
```
backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── devvault/
                    ├── auth/
                    ├── user/
                    ├── note/
                    ├── snippet/
                    ├── resource/
                    ├── search/
                    ├── common/
                    └── DevVaultApplication.java
```
Each feature can contain its own:
```
controller/
service/
repository/
entity/
dto/
mapper/
```

Example:
```
snippet/
├── controller/
│   └── SnippetController.java
├── service/
│   └── SnippetService.java
├── repository/
│   └── SnippetRepository.java
├── entity/
│   └── Snippet.java
├── dto/
│   ├── CreateSnippetRequest.java
│   └── SnippetResponse.java
└── mapper/
    └── SnippetMapper.java
```
This organization allows contributors to work on individual features without needing to understand the entire backend.

5. Frontend Architecture

The frontend uses React.

The frontend is also organized by feature.
```
frontend/
└── src/
    ├── components/
    ├── pages/
    ├── features/
    │   ├── auth/
    │   ├── notes/
    │   ├── snippets/
    │   ├── resources/
    │   └── search/
    ├── services/
    ├── hooks/
    ├── utils/
    ├── layouts/
    └── App.jsx
```
Feature-specific components and logic should remain inside their respective feature directories where practical.

Shared components should be placed in components/.

6. Communication

The frontend communicates with the backend using REST APIs.

Data is exchanged using JSON.

Example:
```
React
  │
  │ HTTP Request
  ▼
Spring Boot REST API
  │
  │ Database Operation
  ▼
PostgreSQL
```
The frontend should not directly communicate with PostgreSQL.

7. Authentication

DevVault uses token-based authentication.

The planned authentication flow is:
```
User
 │
 │ Email + Password
 ▼
React Frontend
 │
 │ POST /api/auth/login
 ▼
Spring Boot
 │
 ├── Find user
 ├── Verify password
 └── Generate JWT
          │
          ▼
React receives token
          │
          ▼
Authenticated requests
```
Authenticated API requests will use:
```
Authorization: Bearer <token>
```
Passwords must never be stored as plain text.

Passwords will be hashed using BCrypt.

8. Authorization

Users must only be able to access resources that belong to them unless a feature explicitly supports public sharing.

For example:
```
User A
  └── Note A


User B
  └── Note B
```
User A must not be able to access Note B by modifying an API request.

Authorization checks will therefore be performed on protected resources.

9. API Layer

The backend exposes REST endpoints.

Initial endpoint groups:
```
/api/auth
/api/notes
/api/snippets
/api/resources
/api/search
```
The API follows conventional HTTP methods:
```
GET       Read
POST      Create
PUT       Update
DELETE    Delete
```
Detailed endpoint definitions are maintained in docs/api.md.

10. Database Layer

PostgreSQL is the primary database.

Spring Data JPA will be used to interact with the database.

Initial entities:
```
User
Note
Snippet
Resource
```
Relationships and database constraints are documented in docs/database.md.

11. Error Handling

The API will use a consistent error response format.

Example:
```json
{
  "timestamp": "2026-08-17T19:00:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Snippet not found",
  "path": "/api/snippets/15"
}
```
The backend should avoid exposing internal implementation details or sensitive information in error responses.

12. Validation

Input received from clients must be validated before processing.

Examples include:

Required fields
String length
Valid email format
Valid URLs
Supported snippet languages
Request body validation

Validation failures should return clear and consistent API responses.

13. Testing Architecture

The backend will use:

JUnit
Mockito
Spring Boot Test

Testing levels may include:
```
Unit Tests
    ↓
Service Tests
    ↓
Controller Tests
    ↓
Integration Tests
```
The frontend will use React Testing Library and an appropriate JavaScript test runner.

Every major feature should have automated tests.

14. CI/CD

GitHub Actions will be used for continuous integration.

Pull requests should eventually run:
```
Backend Build
Backend Tests
Frontend Build
Frontend Tests
Lint / Quality Checks
```
A pull request should not be merged if required CI checks fail.

15. Security Principles

Security is a core architectural concern.

The project will follow principles including:

Never store plain-text passwords
Validate user input
Enforce authorization
Avoid exposing sensitive information
Keep dependencies updated
Protect authentication endpoints
Use secure configuration management
Avoid committing secrets to Git

Security-related contributions should follow [SECURITY.md](./SECURITY.md).

16. Open-Source Design Principles

DevVault is designed to be contributor-friendly.

The architecture should make it possible for contributors to work independently on modules such as:
```
Notes
Snippets
Resources
Search
Developer Tools
Authentication
Documentation
Testing
```
Feature boundaries should be kept clear.

Contributors should not need to understand the entire application to make a small change.

17. Future Extensibility

Future versions may introduce:

Tags
Public snippets
Resource sharing
Community resources
Developer utilities
README Generator
Plugin architecture
Public API
Additional authentication providers

These features should be added without unnecessarily complicating the initial architecture.

18. Architecture Decision Principle

When making architectural decisions, DevVault prioritizes:

Simplicity
Maintainability
Security
Testability
Contributor experience
Performance
Extensibility

Complexity should only be introduced when there is a clear benefit.



---


### Save it.


Then **don't create `database.md` yet**.


I want to do the database design carefully because that's where we need to decide things like:


- exact fields
- primary keys
- relationships
- indexes
- timestamps
- future tags
- deletion behavior


We'll then document that in `docs/database.md`.


After that we'll design the complete REST API in `docs/api.md`.


So the order is:


```txt
       ↓
database.md       ← next
       ↓
api.md             ← after database
       ↓
Spring Boot       ← only then s
```