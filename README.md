# 🤖 Blog Post AI Agent

> **Autonomous blog post generation with Spring Boot 4 + Embabel AI + Database Persistence**
> 
> A production-ready AI agent that writes, reviews, and stores high-quality blog posts — accessible via CLI shell and REST API.

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Embabel](https://img.shields.io/badge/Embabel-0.4.0--SNAPSHOT-blue)](https://embabel.com)
[![H2](https://img.shields.io/badge/Database-H2%20%2F%20PostgreSQL-blue?logo=postgresql)](https://h2database.com)
[![Tests](https://img.shields.io/badge/Tests-32%20Passing-success?logo=junit5)](/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 📖 Table of Contents

- [What Is This Project?](#-what-is-this-project)
- [How It Works](#-how-it-works)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Database Schema](#-database-schema)
- [REST API Reference](#-rest-api-reference)
- [Shell Commands](#-shell-commands)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Running Tests](#-running-tests)
- [Benefits](#-benefits)
- [Upgrade to PostgreSQL](#-upgrade-to-postgresql)
- [Design Decisions](#-design-decisions)

---

## 🎯 What Is This Project?

This project is a **fully autonomous AI blog post agent** built with:
- **Spring Boot 4** as the application framework
- **Embabel Agent Framework** to orchestrate multi-step AI workflows
- **Spring AI** to integrate with OpenAI and Anthropic LLMs
- **Spring Data JPA + H2** for zero-config database persistence
- **Spring Shell** for an interactive CLI
- **Spring MVC** for a REST API

### The Problem It Solves

Manually writing high-quality, SEO-optimised blog posts is:
- ❌ Time-consuming
- ❌ Inconsistent in quality
- ❌ Hard to scale

This agent automates the **entire pipeline**: draft → review → persist → expose via API — in one command.

---

## ⚙️ How It Works

The agent follows a **two-step AI pipeline** orchestrated by Embabel:

```
User provides topic
       │
       ▼
┌─────────────────────────────┐
│  Step 1: writeDraft()       │
│  Persona: Expert Technical  │
│  Writer (10+ years exp.)    │
│  Output: BlogDraft          │
│  • SEO-friendly title       │
│  • ~800 words in Markdown   │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│  Step 2: reviewAndSave()    │
│  Persona: Content Editor &  │
│  SEO Specialist             │
│  Output: ReviewedBlogPost   │
│  • Refined title            │
│  • Polished content         │
│  • Editorial feedback       │
└──────────────┬──────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
  Saved to DB     Saved as .md file
  (blog_posts     (blog-posts/
   table)          directory)
```

Each AI step uses a different **persona** (Writer vs Editor) to simulate a real editorial workflow.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Presentation Layer                      │
│                                                           │
│  BlogPostShell          BlogPostController                │
│  (Spring Shell CLI)     (REST API)                        │
│  • blog                 • POST /generate                  │
│  • list-posts           • GET  /                          │
│  • search-posts         • GET  /{id}                      │
│                         • GET  /search?q=                 │
│                         • DELETE /{id}                    │
└────────────┬────────────────────────┬─────────────────────┘
             │                        │
             ▼                        ▼
┌──────────────────────────────────────────────────────────┐
│                    Service Layer                          │
│       BlogPostService (interface)                         │
│       BlogPostServiceImpl                                 │
│       • generateAndPersist(topic)                         │
│       • findAll(pageable)                                 │
│       • findById(uuid)                                    │
│       • search(keyword)                                   │
│       • deleteById(uuid)                                  │
└────────────────────────┬─────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          ▼                             ▼
┌──────────────────┐       ┌─────────────────────────────┐
│   Agent Layer    │       │     Repository Layer         │
│                  │       │                              │
│  BlogPostAgent   │       │  BlogPostRepository          │
│  (Embabel)       │       │  (Spring Data JPA)           │
│  writeDraft()    │       │  findAll(pageable)           │
│  reviewAndSave() │       │  findByStatus(status)        │
└──────────────────┘       │  findByTopicContaining(kw)   │
                           └──────────────┬───────────────┘
                                          │
                           ┌──────────────▼───────────────┐
                           │       Domain Layer            │
                           │  BlogPostEntity (JPA)         │
                           │  BlogPostStatus (enum)        │
                           └──────────────────────────────┘
                                          │
                                 H2 (dev) / PostgreSQL (prod)
```

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 25 | Language |
| **Spring Boot** | 4.0.6 | Application framework |
| **Embabel Agent** | 0.4.0-SNAPSHOT | AI agent orchestration |
| **Spring AI** | 1.1.4 | LLM integration |
| **Spring Data JPA** | (Boot-managed) | Database persistence |
| **Hibernate** | (Boot-managed) | ORM / DDL generation |
| **H2 Database** | (Boot-managed) | In-memory DB (dev) |
| **Spring MVC** | (Boot-managed) | REST API |
| **Spring Shell** | (Boot-managed) | Interactive CLI |
| **OpenAI / Anthropic** | via Spring AI | LLM providers |
| **JUnit 5 + Mockito** | (Boot-managed) | Testing |

---

## 📁 Project Structure

```
blog-post/
├── src/
│   ├── main/
│   │   ├── java/org/systemverge/blogpost/blogpost/
│   │   │   ├── BlogPostApplication.java          # Entry point
│   │   │   ├── agent/
│   │   │   │   └── BlogPostAgent.java            # AI agent (writer + reviewer)
│   │   │   ├── config/
│   │   │   │   └── JacksonConfig.java            # Spring Boot 4 Jackson fix
│   │   │   ├── domain/
│   │   │   │   ├── BlogPostEntity.java           # JPA entity
│   │   │   │   └── BlogPostStatus.java           # DRAFT / REVIEWED enum
│   │   │   ├── repository/
│   │   │   │   └── BlogPostRepository.java       # Spring Data JPA repository
│   │   │   ├── service/
│   │   │   │   ├── BlogPostService.java          # Service interface
│   │   │   │   └── BlogPostServiceImpl.java      # Service implementation
│   │   │   ├── shell/
│   │   │   │   └── BlogPostShell.java            # CLI commands
│   │   │   └── web/
│   │   │       ├── BlogPostController.java       # REST controller
│   │   │       └── dto/
│   │   │           └── GenerateBlogPostRequest.java
│   │   └── resources/
│   │       └── application.yaml                  # App configuration
│   └── test/
│       ├── java/org/systemverge/blogpost/blogpost/
│       │   ├── BlogPostApplicationTests.java     # JPA + context integration tests
│       │   ├── agent/
│       │   │   ├── BlogPostAgentTest.java        # Agent unit tests
│       │   │   └── BlogPostAgentIntegrationTest.java
│       │   └── service/
│       │       └── BlogPostServiceImplTest.java  # Service unit tests
│       └── resources/
│           └── application.yaml                  # Test configuration
├── blog-posts/                                   # Generated .md files (auto-created)
├── pom.xml
└── README.md
```

---

## 🗄️ Database Schema

```sql
CREATE TABLE blog_posts (
    id         UUID         PRIMARY KEY,       -- Auto-generated UUID
    topic      VARCHAR(500) NOT NULL,          -- Original user prompt
    title      VARCHAR(500) NOT NULL,          -- AI-generated SEO title
    content    TEXT         NOT NULL,          -- Markdown blog post body
    feedback   TEXT,                           -- AI editorial notes
    status     VARCHAR(20)  NOT NULL,          -- 'DRAFT' or 'REVIEWED'
    created_at TIMESTAMP    NOT NULL,          -- Auto-set on insert
    updated_at TIMESTAMP    NOT NULL           -- Auto-set on update
);
```

Schema is **auto-created by Hibernate** on startup (`ddl-auto: create-drop`).  
It is **PostgreSQL-compatible** — switch databases by changing only `application.yaml`.

---

## 🌐 REST API Reference

Base URL: `http://localhost:8080/api/v1/blog-posts`

### Generate a Blog Post
```http
POST /api/v1/blog-posts/generate
Content-Type: application/json

{
  "topic": "Spring Boot 4 new features and improvements"
}
```
**Response:** `201 Created` — Full `BlogPostEntity` JSON

---

### Get All Posts (Paginated)
```http
GET /api/v1/blog-posts?page=0&size=10
```
**Response:** `200 OK` — Spring `Page<BlogPostEntity>`

---

### Get Post by ID
```http
GET /api/v1/blog-posts/{uuid}
```
**Response:** `200 OK` or `404 Not Found`

---

### Filter by Status
```http
GET /api/v1/blog-posts/status/REVIEWED
GET /api/v1/blog-posts/status/DRAFT
```

---

### Search by Topic Keyword
```http
GET /api/v1/blog-posts/search?q=docker
```
Case-insensitive keyword search on the original topic.

---

### Delete a Post
```http
DELETE /api/v1/blog-posts/{uuid}
```
**Response:** `204 No Content`

---

### Example cURL Commands

```bash
# Generate
curl -X POST http://localhost:8080/api/v1/blog-posts/generate \
  -H "Content-Type: application/json" \
  -d '{"topic": "Getting started with Docker and containers"}'

# List all
curl "http://localhost:8080/api/v1/blog-posts?page=0&size=5"

# Search
curl "http://localhost:8080/api/v1/blog-posts/search?q=kubernetes"
```

---

## 💻 Shell Commands

Start the app and interact via the built-in CLI:

```
shell:> blog --topic "Spring Boot 4 new features"
```
Runs the full AI pipeline → saves to DB → prints the reviewed post.

```
shell:> list-posts
```
Lists recent posts (page 0, 5 per page).

```
shell:> list-posts --page 1 --size 10
```
Paginated list with custom page size.

```
shell:> search-posts --keyword "docker"
```
Case-insensitive topic keyword search.

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 25 |
| Maven (or use `./mvnw`) | 3.9+ |
| OpenAI API Key **or** Anthropic API Key | Any |

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/your-username/blog-post.git
cd blog-post
```

### Step 2 — Set Your API Key

**Option A — OpenAI:**
```bash
export OPENAI_API_KEY=sk-proj-...
```

**Option B — Anthropic:**
```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

> 💡 You only need **one** key. The agent will use whichever is configured.

### Step 3 — Run the Application

```bash
# Make sure you use Java 25
export JAVA_HOME=/path/to/jdk-25

./mvnw spring-boot:run
```

You will see the Spring Shell prompt appear:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v4.0.6)

shell:>
```

### Step 4 — Generate Your First Blog Post

```
shell:> blog --topic "Introduction to Spring AI"
```

The agent will:
1. 🖊️ Write a ~800 word draft
2. ✅ Review and polish it
3. 💾 Save to H2 database
4. 📄 Save as a `.md` file in `blog-posts/`
5. 🖥️ Print the result to your terminal

### Step 5 — View in H2 Console (optional)

Open your browser at: **http://localhost:8080/h2-console**

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:blogdb` |
| Username | `sa` |
| Password | _(leave empty)_ |

---

## ⚙️ Configuration

`src/main/resources/application.yaml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}       # Set via env variable
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}    # Set via env variable

  datasource:
    url: jdbc:h2:mem:blogdb;MODE=PostgreSQL
    username: sa

  jpa:
    hibernate:
      ddl-auto: create-drop             # Auto-creates schema

  h2:
    console:
      enabled: true                     # http://localhost:8080/h2-console

blog-post:
  word-count: 800         # Target draft word count
  review-word-count: 200  # Target review summary word count
  output-dir: blog-posts  # Directory for .md file output
```

### Tuning the AI Output

| Property | Default | Description |
|---|---|---|
| `blog-post.word-count` | `800` | Draft target word count |
| `blog-post.review-word-count` | `200` | Review improvement length |
| `blog-post.output-dir` | `blog-posts` | Folder for saved `.md` files |

---

## 🧪 Running Tests

```bash
export JAVA_HOME=/path/to/jdk-25
./mvnw clean test
```

### Test Results

```
[INFO] Tests run:  4  BlogPostApplicationTests     ✅ (JPA schema + repository queries)
[INFO] Tests run:  5  BlogPostAgentIntegrationTest ✅ (full 2-step agent workflow)
[INFO] Tests run: 10  BlogPostAgentTest            ✅ (prompt construction, file save)
[INFO] Tests run: 13  BlogPostServiceImplTest      ✅ (service layer, entity builder)
────────────────────────────────────────────────────
[INFO] Tests run: 32, Failures: 0, Errors: 0  →  BUILD SUCCESS
```

### Test Design

| Test Class | Strategy | What It Tests |
|---|---|---|
| `BlogPostAgentTest` | `FakeOperationContext` | Prompt content, file saving, record fields |
| `BlogPostAgentIntegrationTest` | `FakeOperationContext` | Full draft→review workflow, data flow |
| `BlogPostServiceImplTest` | Mockito + Seam pattern | Repository interactions, entity builder |
| `BlogPostApplicationTests` | `@SpringBootTest(NONE)` | JPA schema, all 3 repository query methods |

> **No real LLM calls in tests.** Embabel's `FakeOperationContext` stubs all AI responses, keeping tests fast, deterministic, and free.

---

## 💡 Benefits

### For Developers

| Benefit | Detail |
|---|---|
| 🏗️ **Clean Architecture** | Domain → Repository → Service → Presentation; each layer isolated |
| 🔌 **Pluggable LLM** | Switch between OpenAI and Anthropic via config, no code changes |
| 🗄️ **Portable Database** | H2 for dev, PostgreSQL for prod — same code, different YAML |
| 🧪 **Fully Testable** | 32 tests, zero live LLM calls, Seam pattern for service isolation |
| 📈 **Production-Ready** | UUID PKs, pagination, validation, error handling, audit timestamps |

### For Content Teams

| Benefit | Detail |
|---|---|
| ⚡ **Speed** | Full draft + review in under 60 seconds |
| 📐 **Consistency** | Same editorial persona applied to every post |
| 🗃️ **Searchable History** | All posts stored in DB, queryable by topic |
| 🔌 **API-first** | Integrate with any CMS or publishing workflow via REST |
| 📄 **Dual Output** | Both database record and Markdown file for every post |

---

## 🐘 Upgrade to PostgreSQL

No Java code changes needed — only `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/blogdb
    username: postgres
    password: yourpassword
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate   # Use Flyway for migrations in prod
```

Add the PostgreSQL driver to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 🧠 Design Decisions

| Decision | Rationale |
|---|---|
| **`BlogPostAgent` left unchanged** | Agent is pure AI orchestration; DB is a service concern |
| **`protected invokeAgent()` seam** | Unit-testable without static mocking or live LLM |
| **Builder pattern on `BlogPostEntity`** | Required fields enforced at construction; immutable after save |
| **`@PrePersist` / `@PreUpdate`** | Audit timestamps managed automatically by JPA lifecycle |
| **UUID primary key** | Globally unique; safe to expose in REST URLs |
| **`@Transactional(readOnly=true)` default** | Performance optimization for all read paths |
| **`H2 MODE=PostgreSQL`** | SQL dialect-compatible from day one |
| **Pageable with size cap (100)** | Prevents unbounded result sets in production |

---

## 📚 References & Inspiration

- 📺 [Tutorial Part 1 — Dan Vega](https://youtu.be/G5VDQCZu6t0?si=17q80gIP1SQ6dH-o)
- 📺 [Tutorial Part 2 — Dan Vega](https://youtu.be/2mGr7kdstJs?si=TjcVvhXlAb8nyFjb)
- 🔗 [Original GitHub Repository](https://github.com/danvega/blog-agent)
- 📖 [Embabel Agent Framework](https://embabel.com)
- 📖 [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- 📖 [Spring Boot 4 Release Notes](https://spring.io/projects/spring-boot)

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

**Built with ❤️ using Spring Boot 4 + Embabel AI**

*Write once, review twice, persist forever.*

</div>
