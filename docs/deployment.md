# Deployment

## 1. Local (Docker Compose)

```bash
cp .env.example .env              # edit: passwords + JWT_SECRET (openssl rand -base64 48)
docker compose up --build -d
docker compose ps                 # mysql "healthy", backend + frontend running
```

- App: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- Health: `docker compose exec backend bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' && echo up`

`SPRING_PROFILES_ACTIVE=dev` loads the DEMO data. Flyway runs automatically at start-up, applying V1 (schema) and V2 (demo data, dev only).

## 2. Without Docker (development)

```bash
# MySQL 8 running locally with database "aashraynet" and a user
cd backend && DB_USERNAME=... DB_PASSWORD=... mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm ci && npm run dev      # http://localhost:5173 (proxies /api to :8080)
```

## 3. Production (free/low-cost single VM)

**Ask before spending money.** No paid service is required by the code.

1. Get a small Linux VM (e.g. a free-tier instance) and install Docker.
2. Clone the repo. Create `.env` with strong secrets and `SPRING_PROFILES_ACTIVE=prod`.
3. HTTPS: put **Caddy** in front. It gets Let's Encrypt certificates automatically:
   ```
   your-domain.example {
       reverse_proxy localhost:8081
   }
   ```
   Then set `CORS_ALLOWED_ORIGINS=https://your-domain.example`.
4. Never add `ports:` to the `mysql` service.
5. Create the first state admin. The prod profile has no demo users:
   ```sql
   INSERT INTO districts(name) VALUES ('Puri');
   INSERT INTO users(full_name,email,password_hash,role,district_id)
   VALUES ('State Admin','you@example.com','<BCrypt hash of a strong password>','STATE_ADMIN',NULL);
   ```
6. Label the site clearly as **demonstration data**, unless it is connected to a verified official source.

**Deployment URL:** none yet. Nothing has been deployed or verified, so no live link is claimed.

## 4. Backup and restore

```bash
# backup (daily via cron)
docker compose exec -T mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction aashraynet' > backup-$(date +%F).sql
# restore
docker compose exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" aashraynet' < backup-2026-11-09.sql
```

Keep backups off the server, and test a restore at least once.

## 5. Migrations

- Never edit an applied migration. Add `V3__...sql`.
- Flyway records applied versions in `flyway_schema_history`.
- Hibernate is set to `ddl-auto: none`; Flyway owns the schema.
