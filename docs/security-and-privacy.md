# Security and Privacy

| Control | Implementation |
|---|---|
| Passwords | BCrypt strength 12 (`SecurityConfig.passwordEncoder`) |
| Authentication | Stateless JWT HS256, 30-minute expiry. The secret comes from the `JWT_SECRET` env var and must be at least 32 bytes, otherwise the app refuses to start. |
| Logout | The token `jti` is stored in `revoked_tokens` until it expires. Expired entries are cleaned daily. |
| Fresh user state | The JWT filter reloads the user on every request, so disabling an account or changing a role takes effect immediately. |
| No self-registration | Only STATE_ADMIN and DISTRICT_ADMIN create accounts. A DISTRICT_ADMIN can only create volunteers and in-charges in their own district. |
| Role checks | `@PreAuthorize` on every controller method |
| District scoping | `AccessPolicy` + scoped repository queries. Out-of-scope records return **404** so their existence isn't revealed. |
| Record ownership | An in-charge can act only on shelters where `incharge_id = me` |
| Sensitive fields | `specialNeed` is omitted for VOLUNTEER. Public endpoints have their own DTOs with only safe fields. |
| Public family finder | Returns only `{shelterName, district}`, at most 5 distinct results. Same shape when nothing matches. Exact normalised match only, so the list can't be browsed. Rate limited to 10 per minute per IP. |
| Login brute force | Rate limited to 5 per minute per IP. The error message is the same for every failure. |
| Input validation | Bean Validation on every DTO, plus DB CHECK constraints |
| Errors | One `ApiError` format with no stack traces or SQL. `server.error.include-*` is set to never. |
| Logging | No names or ages are logged. `audit_logs` stores ids and actions only. |
| CORS | Only the origins in `CORS_ALLOWED_ORIGINS` |
| Headers | Spring Security: nosniff, frame DENY, no-referrer. Nginx adds a Content-Security-Policy. |
| HTTPS | Terminated at the reverse proxy in production (see deployment.md). The app honours `X-Forwarded-*`. |
| Secrets | `.env` is git-ignored. `.env.example` holds placeholders only. |
| Database | Not exposed outside the Docker network (no `ports:` in compose) |

## Known limitations (honest)

- The rate limiter is in-memory, so it works per instance. Use a gateway or Redis for multiple instances.
- The JWT is kept in `sessionStorage`. That's acceptable for this demo; httpOnly cookies with CSRF protection would be stronger.
- The demo password `Demo@1234` exists only in the `dev` profile's data. Production starts with an empty database. Create the first state admin with a one-off SQL insert using a BCrypt hash; see deployment.md.
