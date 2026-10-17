# EventHub Auth Service

## Tenant roles

Public registration creates a `CUSTOMER` account only. Tenant IDs and roles submitted to the public registration endpoint are not used to grant access. A user can hold multiple tenant roles; the JWT contains global `roles` and a `tenantMemberships` claim. For tenant-scoped staff operations, send `X-Tenant-ID`; the API Gateway checks the membership and forwards the verified tenant and its roles to internal services.

After the Auth Service has run with the membership schema, provision the first `PLATFORM_ADMIN` through a controlled database operation. First register the intended account through the public endpoint, then run this statement using the production database's authorized SQL session, replacing the email:

```sql
INSERT INTO user_global_roles (user_id, global_role)
SELECT id, 'PLATFORM_ADMIN'
FROM users
WHERE email = 'platform-admin@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM user_global_roles existing
      WHERE existing.user_id = users.id
        AND existing.global_role = 'PLATFORM_ADMIN'
  );
```

Verify the inserted role before allowing the account to administer tenants. The user must log in again to receive a token with the new role. Keep this as a one-time operation; do not expose public role assignment or leave bootstrap credentials in application configuration.

A `PLATFORM_ADMIN` can grant tenant-scoped roles to an existing user through `PUT /api/v1/auth/admin/users/{userId}/tenant-roles` with a body such as:

```json
{
  "tenantId": "carrera-tulua-2026",
  "roles": ["EVENT_ORGANIZER"]
}
```

Valid tenant roles are `TENANT_ADMIN` and `EVENT_ORGANIZER`. The user must log in again after the grant to receive updated membership claims.

Remove all tenant roles from a user with `DELETE /api/v1/auth/admin/users/{userId}/tenant-roles/{tenantId}`. The API Gateway checks protected access tokens against the user's current enabled status, global roles, and tenant memberships through the internal `POST /internal/v1/authz/validate` endpoint. A role removal or account disablement therefore invalidates existing tokens immediately; a role grant requires a new login to obtain updated claims.

The internal validation endpoint requires the existing `X-Gateway-Secret` header enforced by `GatewayAccessFilter`. It returns `204` for a current token and `401` for a missing, invalid, expired, or stale token. Keep Auth Service reachable only through trusted internal networking and configure the gateway and Auth Service with the same `gateway.internal-secret`.

## Database migrations

Auth Service uses Flyway migrations in `src/main/resources/db/migration`; Hibernate validates the schema and does not update it. V1 creates the `users` table, and V2 creates the global-role and tenant-membership tables. An empty database is migrated automatically on startup.

Before upgrading an existing database, take a backup and inspect its schema using the authorized database operator workflow:

- If only `users` exists, verify its ID, email, password, role, and enabled columns, primary key, and unique email constraint match V1. Baseline that database at Flyway version 1 so Flyway applies V2.
- If `users`, `user_global_roles`, and `user_tenant_roles` already exist from Hibernate schema updates, verify all columns and primary/foreign-key constraints match V1 and V2, then baseline at version 2. Do not run V2 against tables that already exist.
- Do not enable automatic baselining or select a baseline version from the application. An unexpected schema must stop deployment for review.

The legacy `users.role` value is retained and is not copied into the new role tables: older tenant roles did not identify a tenant and cannot safely be converted into tenant memberships. Existing accounts without a row in `user_global_roles` are treated as `CUSTOMER` by Auth Service. After migration, provision the first `PLATFORM_ADMIN` using the controlled SQL operation above, then use the admin API to grant explicit tenant roles. Review any legacy privileged accounts and reassign their memberships to the correct tenant before relying on tenant-scoped access.
