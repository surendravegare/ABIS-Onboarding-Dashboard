# ABiS Student Management System

Spring Boot 3 / Thymeleaf / MySQL application for managing students, with an admin dashboard and an audit log.

## Run it

Set these environment variables (in STS/Eclipse: *Run Configurations > Environment*), then start
`StudentManagementSystemApplication` or run `./mvnw spring-boot:run`.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_PASSWORD` | yes | - | MySQL password |
| `DB_USERNAME` | no | `root` | MySQL user |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | no | `localhost` / `3306` / `ABIS_db` | MySQL location |
| `ADMIN_USERNAME` | no | `admin` | First admin (only used when the `admins` table is empty) |
| `ADMIN_PASSWORD` | no | random | If empty, a random password is printed once in the console |
| `SERVER_ADDRESS` | no | `127.0.0.1` | Set to `0.0.0.0` to accept connections from the network |
| `DDL_AUTO` | no | `update` | Hibernate schema mode (`validate` is recommended in production) |

Admin passwords are stored as BCrypt hashes. Change yours from the dashboard (**Change Password**).
An old plain-text password is accepted once and is re-saved as a hash automatically.

## Security notes

- Every POST form carries a per-session token (`_csrf`); requests without it get 403.
- Deleting a student is POST-only.
- Login is limited to 5 wrong attempts per username (20 per address) per 15 minutes; public registration to 10 per address per hour. Counters are in memory and reset on restart.
- Photos: only JPG/PNG/GIF/WEBP (checked by file content), max 5 MB, visible to logged-in admins only.
- Student data is validated on the server (`StudentValidator`), including duplicate email/phone.

## Tests

`./mvnw test` - `contextLoads` needs a running MySQL and the environment variables above.
