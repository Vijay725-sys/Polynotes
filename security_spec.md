# PolyNotes Firestore Security Specification

## 1. Data Invariants
1. **Per-User Profile Isolation (`/users/{userId}`)**:
   - Every profile document is keyed by the authenticated Firebase user's `uid` (`request.auth.uid == userId`).
   - `userId` and `createdAt` are immutable after creation.
   - `bookmarkedIds` is a bounded list of strings (`size <= 50`).
   - `completedTopics` is a bounded list of strings (`size <= 100`).
2. **Per-User Cloud Resource Isolation (`/users/{userId}/resources/{resourceId}`)**:
   - Every custom study resource document is nested under `/users/{userId}/resources/{resourceId}` and carries `userId == request.auth.uid`.
   - `userId` and `createdAt` are immutable after creation.
   - All string fields enforce explicit minimum and maximum `.size()` bounds.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `GET /users/alice_123` with `request.auth == null` -> Rejected (`PERMISSION_DENIED`).
2. **Unauthenticated Write**: `SET /users/alice_123` with `request.auth == null` -> Rejected (`PERMISSION_DENIED`).
3. **Cross-User Profile Read**: `GET /users/alice_123` authenticated as `bob_456` -> Rejected (`PERMISSION_DENIED`).
4. **Cross-User Profile Update**: `UPDATE /users/alice_123` authenticated as `bob_456` -> Rejected (`PERMISSION_DENIED`).
5. **Identity Spoofing on Create**: `CREATE /users/alice_123` with `userId: "bob_456"` -> Rejected (`PERMISSION_DENIED`).
6. **Shadow Update / Ghost Field Injection**: `UPDATE /users/alice_123` adding `isAdmin: true` -> Rejected via `affectedKeys().hasOnly(...)`.
7. **Immutable Field Mutation**: `UPDATE /users/alice_123` modifying `createdAt` or `userId` -> Rejected (`incoming().createdAt == existing().createdAt`).
8. **Enum Poisoning**: `CREATE /users/alice_123` with `board: "INVALID_BOARD"` -> Rejected (`data.board in ['UBTER', 'BTEUP', 'HSBTE', 'MSBTE']`).
9. **Semester Out of Bounds**: `UPDATE /users/alice_123` with `semester: 99` -> Rejected (`semester >= 1 && semester <= 6`).
10. **Array Overflow Attack**: `UPDATE /users/alice_123` with 60 `bookmarkedIds` -> Rejected (`bookmarkedIds.size() <= 50`).
11. **Cross-User Resource Read**: `GET /users/alice_123/resources/res_1` authenticated as `bob_456` -> Rejected (`PERMISSION_DENIED`).
12. **Cross-User Resource Delete**: `DELETE /users/alice_123/resources/res_1` authenticated as `bob_456` -> Rejected (`PERMISSION_DENIED`).
