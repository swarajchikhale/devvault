# DevVault API Specification


## 1. Overview


The DevVault backend exposes a REST API consumed by the React frontend.


Base URL:


```text
/api
```
All API responses use JSON unless otherwise specified.

The API follows standard HTTP methods and status codes.

2. Authentication

Authentication endpoints are publicly accessible.

Protected endpoints require a valid JWT.

Authenticated requests must include:
```
Authorization: Bearer <JWT>```
3. Standard HTTP Status Codes

DevVault uses the following status codes:

Status	Meaning
200	Request successful
201	Resource created
204	Request successful with no response body
400	Invalid request
401	Authentication required or invalid
403	Access denied
404	Resource not found
409	Resource conflict
422	Validation failure
500	Internal server error
4. Standard Error Response

Errors should use a consistent structure.

Example:
```json
{
  "timestamp": "2026-08-17T19:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Snippet not found",
  "path": "/api/snippets/550e8400-e29b-41d4-a716-446655440000"
}
```
The API must not expose stack traces, database credentials, passwords, tokens, or other sensitive information.

5. Authentication API
5.1 Register

Creates a new user account.

Request
```
POST /api/auth/register
Content-Type: application/json```
```
{
  "username": "swaraj",
  "email": "swaraj@example.com",
  "password": "examplePassword"
}
```
Success
```
201 Created```
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "swaraj",
  "email": "swaraj@example.com"
}
```
The password must never be returned.

Possible Errors
```
400 Bad Request
409 Conflict
422 Unprocessable Entity```
6. Login

Authenticates an existing user.

Request
```
POST /api/auth/login
Content-Type: application/json```
```json
{
  "email": "swaraj@example.com",
  "password": "examplePassword"
}```
Success
```
200 OK```
```json
{
  "token": "<JWT>",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "swaraj",
    "email": "swaraj@example.com"
  }
}
```
Possible Errors
```
400 Bad Request
401 Unauthorized
```
7. Get Current User

Returns information about the authenticated user.

Request
```
GET /api/auth/me
Authorization: Bearer <JWT>```
Success
```
200 OK```
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "swaraj",
  "email": "swaraj@example.com"
}```
8. Notes API

All note endpoints require authentication.

8.1 Create Note
```
POST /api/notes
Authorization: Bearer <JWT>
Content-Type: application/json```
Request
```json
{
  "title": "Java Collections",
  "content": "ArrayList, LinkedList, HashMap...",
  "category": "Java"
}```
Success
```
201 Created```
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Java Collections",
  "content": "ArrayList, LinkedList, HashMap...",
  "category": "Java",
  "createdAt": "2026-08-17T19:00:00Z",
  "updatedAt": "2026-08-17T19:00:00Z"
}```
9. Get Notes

Returns notes belonging to the authenticated user.
```
GET /api/notes
Authorization: Bearer <JWT>```
Success
```
200 OK```
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Java Collections",
    "content": "ArrayList...",
    "category": "Java",
    "createdAt": "2026-08-17T19:00:00Z",
    "updatedAt": "2026-08-17T19:00:00Z"
  }
]```
10. Get Note

Returns one note.
```
GET /api/notes/{id}
Authorization: Bearer <JWT>```
Success
```
200 OK```
Errors
```
401 Unauthorized
404 Not Found```

If the note does not belong to the authenticated user, it must not be returned.

11. Update Note
```
PUT /api/notes/{id}
Authorization: Bearer <JWT>
Content-Type: application/json```
Request
```json
{
  "title": "Updated Java Collections",
  "content": "Updated content",
  "category": "Java"
}```
Success
```
200 OK```
Errors
```
400 Bad Request
401 Unauthorized
404 Not Found
422 Unprocessable Entity```

12. Delete Note
```
DELETE /api/notes/{id}
Authorization: Bearer <JWT>```
Success
```
204 No Content```
Errors
```
401 Unauthorized
404 Not Found```
13. Snippets API

All snippet endpoints require authentication.

13.1 Create Snippet
```
POST /api/snippets
Authorization: Bearer <JWT>
Content-Type: application/json```
Request
```json
{
  "title": "Read File in Java",
  "description": "Read the contents of a file",
  "code": "Files.readString(Path.of(\"file.txt\"));",
  "language": "java"
}```
Success
```
201 Created
```
14. Get Snippets
```
GET /api/snippets
Authorization: Bearer <JWT>
```
Returns snippets belonging to the authenticated user.

15. Get Snippet
```
GET /api/snippets/{id}
Authorization: Bearer <JWT>
```
Returns a single snippet if it belongs to the authenticated user.

16. Update Snippet
```
PUT /api/snippets/{id}
Authorization: Bearer <JWT>
Content-Type: application/json
```
Request
```json
{
  "title": "Read File in Java",
  "description": "Updated description",
  "code": "Files.readString(Path.of(\"example.txt\"));",
  "language": "java"
}```
Success
```
200 OK```
17. Delete Snippet
```
DELETE /api/snippets/{id}
Authorization: Bearer <JWT>```
Success
```
204 No Content```
18. Resources API

All resource endpoints require authentication.

18.1 Create Resource
```
POST /api/resources
Authorization: Bearer <JWT>
Content-Type: application/json```
Request
```json
{
  "title": "Spring Boot Documentation",
  "url": "https://spring.io/projects/spring-boot",
  "description": "Official Spring Boot documentation",
  "category": "Java"
}
```
Success
```
201 Created```
19. Get Resources
```
GET /api/resources
Authorization: Bearer <JWT>
```
Returns resources belonging to the authenticated user.

20. Get Resource
```
GET /api/resources/{id}
Authorization: Bearer <JWT>```
21. Update Resource
```
PUT /api/resources/{id}
Authorization: Bearer <JWT>
Content-Type: application/json```
Request
```json
{
  "title": "Spring Boot Documentation",
  "url": "https://spring.io/projects/spring-boot",
  "description": "Updated description",
  "category": "Backend"
}```
22. Delete Resource
```
DELETE /api/resources/{id}
Authorization: Bearer <JWT>```
Success
```
204 No Content```
23. Search API

The search API searches across the user's own notes, snippets, and resources.
```
GET /api/search?q=java
Authorization: Bearer <JWT>```
Example Response
```json
{
  "query": "java",
  "notes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Java Collections"
    }
  ],
  "snippets": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "title": "Read File in Java",
      "language": "java"
    }
  ],
  "resources": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "title": "Spring Boot Documentation",
      "url": "https://spring.io/projects/spring-boot"
    }
  ]
}
```
Search behavior may evolve as the application grows.

24. Pagination

Pagination is not required for the initial v0.1 implementation.

However, list endpoints should be designed so pagination can be introduced without breaking the API.

Future example:
```
GET /api/snippets?page=0&size=20```
25. Filtering

Filtering is also planned for future versions.

Potential examples:
```
GET /api/snippets?language=java
GET /api/resources?category=backend
GET /api/notes?category=database```

These filters should only be implemented when the corresponding product requirements are finalized.

26. API Versioning

The initial API will use:
```
/api
```
API versioning such as:
```
/api/v1
```
may be introduced when breaking changes become necessary.

We should avoid adding versioning complexity before it provides a real benefit.

27. Security Requirements

The API must enforce:

Authentication

Protected endpoints require a valid JWT.

Authorization

Users can only access their own private resources.

Password security

Passwords must be hashed using BCrypt.

Input validation

All incoming request data must be validated.

Error safety

Error responses must not expose:

Stack traces
Passwords
JWT secrets
Database credentials
Internal implementation details
Secrets

Secrets must be provided through environment variables or secure configuration.

Secrets must never be committed to Git.

28. API Design Principles

DevVault APIs should follow these principles:

Use standard HTTP methods.
Use meaningful HTTP status codes.
Return consistent JSON responses.
Validate all input.
Protect user-owned data.
Keep endpoints predictable.
Avoid unnecessary API complexity.
Document breaking changes.
Maintain backward compatibility when practical.
Keep the API easy for external contributors to understand.
29. Initial API Summary
```
Authentication
──────────────
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me


Notes
─────
GET    /api/notes
GET    /api/notes/{id}
POST   /api/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}


Snippets
────────
GET    /api/snippets
GET    /api/snippets/{id}
POST   /api/snippets
PUT    /api/snippets/{id}
DELETE /api/snippets/{id}


Resources
─────────
GET    /api/resources
GET    /api/resources/{id}
POST   /api/resources
PUT    /api/resources/{id}
DELETE /api/resources/{id}


Search
──────
GET    /api/search?q={query}```
30. API Evolution

The API will evolve as DevVault grows.

Potential future APIs include:
```
/api/tags
/api/favorites
/api/public/snippets
/api/community/resources
/api/tools
/api/readme
```
These endpoints should only be introduced when the corresponding features are properly designed.




