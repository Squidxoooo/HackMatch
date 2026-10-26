# HackMatch

HackMatch is a beginner-friendly BTech Java OOP mini-project. It helps a student browse fictional demo hackathons, enter preferences, and receive transparent rule-based recommendations. It is deliberately not an AI or machine-learning system.

The application flow is:

```text
HTML/CSS/JavaScript frontend
        ↓ HTTP + JSON
Core Java HttpServer API
        ↓ JDBC PreparedStatements
MariaDB database
```

## Technology used

- Frontend: vanilla HTML, CSS, and JavaScript in `frontend/`
- Backend: Core Java 17+ and `com.sun.net.httpserver.HttpServer`
- Database: MariaDB
- Connectivity: JDBC with MariaDB Connector/J
- Build tool: Maven, only to download and bundle the JDBC driver

No Spring Boot, Hibernate, JPA, authentication framework, or external AI service is used.

## Final folder structure

```text
HackMatch/
├── .env.example                         # safe, tracked environment-variable template
├── .gitignore
├── README.md
├── database/
│   └── schema.sql                        # tables and fictional demo rows
├── backend/
│   ├── pom.xml                           # JDBC driver and runnable-JAR build
│   └── src/main/java/com/hackmatch/
│       ├── Main.java                     # HTTP server and API routing
│       ├── DBConnection.java             # JDBC connection helper
│       ├── HackathonDAO.java             # hackathon SQL queries
│       ├── BookmarkDAO.java              # bookmark SQL operations
│       ├── Hackathon.java                # hackathon model
│       ├── StudentProfile.java           # incoming preferences model
│       ├── Bookmark.java                 # saved-hackathon model
│       ├── MatchResult.java              # recommendation score and reasons
│       ├── MatchCriterion.java           # matching interface
│       ├── SkillCriterion.java           # skills scoring rule
│       ├── InterestCriterion.java        # domain scoring rule
│       ├── ProblemStatementCriterion.java# keyword scoring rule
│       ├── MatchingService.java          # runs rules and ranks results
│       └── JsonUtil.java                 # small JSON reader/writer for this API
└── frontend/                             # existing frontend prototype (unchanged)
```

## Database setup on CachyOS

The commands below are for a local development database. Do not use a password from these examples in a public repository.

### 1. Install and start MariaDB

```bash
sudo pacman -S --needed mariadb maven jdk17-openjdk
```

On a newly installed MariaDB server only, initialise its data directory once. Skip this command if MariaDB already has data or was already initialised.

```bash
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
```

Start it now and enable it to start automatically later:

```bash
sudo systemctl enable --now mariadb.service
```

### 2. Create the schema and fictional demo data

From the project root:

```bash
sudo mariadb < database/schema.sql
```

`schema.sql` uses `CREATE ... IF NOT EXISTS` and `INSERT IGNORE` for the named hackathon demo rows. It never drops the database, tables, or user data. It also inserts a `Demo Student` profile when one does not already exist, so bookmark demos can use student ID `1` on a fresh database.

### 3. Create a least-privilege local application user

Open the MariaDB prompt:

```bash
sudo mariadb
```

At the `MariaDB [(none)]>` prompt, enter the following SQL. Choose a strong password yourself; the placeholder is intentionally not a real credential.

```sql
CREATE USER IF NOT EXISTS 'hackmatch_app'@'localhost'
  IDENTIFIED BY 'choose-a-strong-local-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON hackmatch.*
  TO 'hackmatch_app'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

The application only needs normal data permissions. It does not need `CREATE`, `DROP`, `ALTER`, or root access.

### 4. Configure database credentials safely

`.env` is ignored by Git. Create it from the safe template, then edit it locally:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Set `HM_DB_PASSWORD` in `.env` to the password selected in the SQL command above. Before building or running, load the variables into the current terminal:

```bash
set -a
source .env
set +a
```

The default local URL is `jdbc:mariadb://localhost:3306/hackmatch?sslMode=disable`. `sslMode=disable` is appropriate only for this local college-project setup; do not copy that choice to a remote production database.

## Build and run

From the project root, after the database setup and environment-variable steps:

```bash
mvn -f backend/pom.xml clean package
java --add-modules jdk.httpserver -jar backend/target/hackmatch-backend-1.0.0.jar
```

The first Maven command downloads MariaDB Connector/J and creates a runnable JAR containing it. The API listens on `http://localhost:8080/api` unless `HM_PORT` is changed in `.env`.

In a second terminal, the existing frontend can be served on the CORS-approved port:

```bash
cd frontend
python -m http.server 5500
```

Open `http://localhost:5500` in the browser.

## HTTP API

All responses are JSON. CORS permits requests from `http://localhost:5500` and allows `GET`, `POST`, `DELETE`, and `OPTIONS`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Backend status and database connection status |
| `GET` | `/api/hackathons` | List hackathons from MariaDB |
| `GET` | `/api/hackathons/{id}` | Get one hackathon |
| `POST` | `/api/recommendations` | Calculate and rank recommendations |
| `GET` | `/api/bookmarks?studentId=1` | List a student's saved hackathons |
| `POST` | `/api/bookmarks` | Create one bookmark |
| `DELETE` | `/api/bookmarks/{id}` | Remove one bookmark |

### `curl` examples

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/hackathons
curl http://localhost:8080/api/hackathons/1
```

```bash
curl -X POST http://localhost:8080/api/recommendations \
  -H 'Content-Type: application/json' \
  -d '{
    "skills": ["Java", "SQL", "JavaScript"],
    "interests": ["FinTech", "Open Innovation"],
    "experienceLevel": "Intermediate",
    "minPrize": 50000,
    "teamSize": 3
  }'
```

```bash
curl 'http://localhost:8080/api/bookmarks?studentId=1'
curl -X POST http://localhost:8080/api/bookmarks \
  -H 'Content-Type: application/json' \
  -d '{"studentId":1,"hackathonId":3}'
curl -X DELETE http://localhost:8080/api/bookmarks/1
```

`POST /api/bookmarks` returns `201 Created`; posting the same student/hackathon pair again returns `409 Conflict`. A missing or invalid ID/body returns a useful `4xx` error. Database failures return `500` without exposing a password.

## Matching algorithm (out of 100)

`MatchingService` runs the three `MatchCriterion` implementations through one interface, then applies three small remaining rules. Expired registration deadlines are excluded before scoring. Results are sorted highest score first, then by name for a deterministic tie-break.

| Rule | Maximum | Simple calculation |
| --- | ---: | --- |
| Skills | 30 | `30 × matched required skills ÷ total required skills` |
| Interests / domain | 25 | 25 when one selected interest equals the hackathon domain |
| Problem-statement keywords | 20 | `20 × matched meaningful preference keywords ÷ total preference keywords` |
| Prize preference | 10 | 10 when prize meets the minimum; otherwise proportional to `prize ÷ preferred minimum` |
| Deadline suitability | 10 | 10 for 14+ days, 7 for 7–13 days, 4 for 0–6 days left |
| Team-size compatibility | 5 | 5 when the preferred size fits the minimum–maximum range |
| **Total** | **100** | `MatchResult` also caps the result at 100 |

For example, the API returns a frontend-compatible recommendation object like:

```json
{
  "hackathonId": 3,
  "score": 85,
  "reasons": [
    "Matching skills: Java, SQL, JavaScript.",
    "Your interest in FinTech matches the hackathon domain.",
    "Prize of INR 200000 meets your minimum preference."
  ]
}
```

The score is rule-based, reproducible, and easy to trace in a viva. It is not AI or machine learning.

## OOP concepts for the viva

| OOP / Java concept | Where it appears | Simple explanation |
| --- | --- | --- |
| Classes and objects | `Hackathon`, `StudentProfile`, `MatchResult`, `Bookmark` | Each class models one real project concept; DAOs and services create and use their objects. |
| Constructors | All four model classes | Constructors put an object into a valid initial state. `HackathonDAO` uses the full `Hackathon` constructor for a database row. |
| Encapsulation | Private fields plus getters/setters in `Hackathon` and `StudentProfile` | Other classes cannot change fields directly. Lists are copied or exposed read-only to protect object state. |
| Interface / abstraction | `MatchCriterion` | It describes one scoring operation without deciding how it is calculated. |
| Polymorphism and overriding | `SkillCriterion`, `InterestCriterion`, `ProblemStatementCriterion` | Each overrides `calculateScore`; `MatchingService` calls all of them through a `List<MatchCriterion>`. |
| Collections | `ArrayList`, `List`, `Map` throughout | Lists hold skills, reasons, hackathons, and recommendations. |
| Exception handling | `Main`, `DBConnection`, DAOs | Invalid JSON becomes `400`, missing records become `404`, duplicate bookmarks become `409`, and SQL problems become `500`. |
| JDBC and SQL | `DBConnection`, `HackathonDAO`, `BookmarkDAO` | `Connection`, `PreparedStatement`, and `ResultSet` read/write MariaDB safely. |

Method overloading is not included because it would not make this small project clearer. The project demonstrates the required OOP ideas naturally instead of forcing extra inheritance or patterns.

## How the existing frontend connects

The frontend was intentionally not changed. It already has `API_BASE_URL = "http://localhost:8080/api"` and functions whose response shapes are now supported by the backend.

After the API is running and tested, open [frontend/script.js](/home/prithvi/HackMatch/frontend/script.js) and change only this existing flag:

```javascript
const USE_BACKEND = true;
```

Then:

- `getHackathons()` calls `GET /api/hackathons`. The backend returns the same camelCase fields it renders: `teamMin`, `teamMax`, `skills`, `problemStatement`, `registrationLink`, and `status`.
- `getRecommendations(preferences)` calls `POST /api/recommendations`. The backend returns the existing `{ hackathonId, score, reasons }` shape.
- Login remains frontend demo mode; there are deliberately no authentication endpoints.
- Bookmarks remain in `localStorage` until a future frontend task replaces `loadBookmarks()`/`saveBookmarks()` with the bookmark API. The API is already ready and returns bookmark objects containing `id`, `studentId`, `createdAt`, and `hackathon`.

## Troubleshooting

- **`mvn: command not found`**: run `sudo pacman -S --needed maven jdk17-openjdk`.
- **MariaDB service will not start on a fresh install**: initialise it once using the `mariadb-install-db` command above, then run `sudo systemctl status mariadb.service` for details.
- **`Access denied` or health shows `database: unavailable`**: confirm MariaDB is running, check `.env`, source it again in the same terminal, and make sure the username/password match the SQL setup step.
- **`MariaDB JDBC driver is missing`**: run `mvn -f backend/pom.xml clean package` again; the driver is bundled in the generated JAR.
- **Port 8080 is already in use**: inspect it with `ss -ltnp | rg ':8080'`, stop the other process, or set `HM_PORT="8081"` in `.env`.
- **Browser CORS error**: serve the frontend from exactly `http://localhost:5500`, not by double-clicking `index.html` or using another port.
- **No hackathons returned**: run `sudo mariadb < database/schema.sql` again. The seeding statements are safe for the named demo rows.
