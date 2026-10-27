# IES Eligibility Platform

A benefits eligibility determination service: one application, several programs,
one deterministic and fully auditable decision per program — plus an AI layer
that removes the repetitive work around the decision without ever making it.

Built as a portfolio project to work through the engineering problems in an
**Integrated Eligibility System**: the kind of platform that determines a
household's eligibility for food, cash and energy assistance from a single
application instead of running each program as a separate silo.

**Nothing in the code names a jurisdiction.** Programs are modelled by what they
are (`FOOD_ASSISTANCE`, `CASH_ASSISTANCE`, `ENERGY_ASSISTANCE`), not by any one
state's name for them, and one running instance serves many jurisdictions at
once — each with its own program names, its own numbers and, where it needs
them, its own rules. See
[the jurisdiction model](#the-jurisdiction-model-how-one-codebase-serves-many-states).

> **All data here is synthetic.** The poverty thresholds, program rules, policy
> text and hearing decisions were written for this demonstration. They are not
> the published federal or state figures. This is not production software and has
> never processed a real benefit claim.

**77 tests. `mvn test` needs no database, no broker and no API key.**

---

## The decision the whole design turns on

**The rules engine determines eligibility. AI never does.**

A determination has to be deterministic, reproducible and defensible — at a Fair
Hearing the agency must show exactly which rule produced which outcome, under
the policy in force on the date of decision. An LLM cannot be in that path.

So AI does the repetitive work *around* the determination:

| Layer | What it does | Can it decide a benefit? |
|---|---|---|
| **Rules engine** | Determines eligibility. No I/O, no randomness, full audit trail. | — it *is* the decision |
| **Notice drafting** | Rephrases the determination into plain language, cited to the rules that fired | **No** — output is rejected if it introduces a figure |
| **Policy Q&A** | Answers caseworker questions from indexed policy, with citations | **No** — advisory, read-only |
| **Precedent retrieval** | Surfaces similar prior Fair Hearing decisions | **No** — returns the passages, not a summary |

Every AI output is **grounded and checked**. The notice generator is handed only
the facts the engine produced, and the result is scanned for monetary figures
that are not in those facts — if it finds one, the notice is discarded rather
than stored. There is a test for exactly that.

---

## Architecture

```
                         POST /households
                                |
                                v
                        Household (JPA)
                                |
                   HouseholdSnapshot (immutable)
                                |
        JurisdictionRegistry.byCode(code)   <-- programs, local names, time zone
                                |
       ThresholdProvider.forDate(jurisdiction, asOf)
                                |         federal baseline + local overrides
                          RuleContext
                     (household, jurisdiction, thresholds)
                                |
                      DeterminationEngine
                     resolve: most specific rule set wins
                                |
        +-----------------------+-----------------------+
  FoodAssistance          CashAssistance          EnergyAssistance
   RuleSet                 RuleSet                 RuleSet
                                ^
                    NyCashAssistanceRuleSet  (jurisdictionCode = NY,
                                              overrides the baseline there)
        +-----------------------+-----------------------+
                                v
                   Determination + firedRules[]
                     (outcome + audit trail)
                          |            |
         DeterminationEvent            NoticeService
          -> Kafka (keyed by           -> LLM, grounded in firedRules
             household id)             -> grounding check
                                       -> Notice + citations

   /assist/policy      -> chunk -> embed -> top-k 20 -> rerank 4 -> cite
   /assist/precedent   -> same retrieval, passages returned verbatim
```

---

## The jurisdiction model — how one codebase serves many states

Integrated Eligibility Systems are built per state, and the usual result is a
codebase with a state's name welded through it: `SnapRuleSet`, `county`,
`ZoneId.of("America/New_York")`, resource limits as constants. The next state is
then a fork, and the two forks drift.

This project pushes all of that variation into **three abstractions**, so the
engine itself contains no jurisdiction and is never edited to add one.

| Abstraction | Answers | Varies by |
|---|---|---|
| `Jurisdiction` | which programs exist, what they are called locally, which time zone the as-of date is read in | configuration |
| `ThresholdProvider` | what the numbers are, effective-dated | configuration (federal baseline + per-jurisdiction overrides) |
| `ProgramRuleSet` | what the rules are | code, and only where a jurisdiction's rules genuinely differ |

### Most specific wins

A rule set with no jurisdiction code is the **federal baseline** and applies
everywhere. A rule set that names one applies there and outranks the baseline:

```java
public Optional<ProgramRuleSet> resolveRuleSet(Jurisdiction j, ProgramType type) {
    return ruleSets.stream()
            .filter(rs -> rs.programType() == type)
            .filter(rs -> rs.appliesTo(j))
            .max(comparingInt(ProgramRuleSet::specificity));
}
```

`NyCashAssistanceRuleSet` is the worked example in the repo. It reuses the
baseline income and resource rules untouched and substitutes exactly one: where
the federal cash rule **fails** a household with no dependent child, New York
refers it to Safety Net and returns `NEEDS_VERIFICATION`. A test asserts all
three halves of that — NY pends, the baseline refuses, and a third jurisdiction
with no variant registered still inherits the baseline.

### Numbers are configuration, not code

Poverty guidelines are issued federally and shared; resource limits are set
locally. So a jurisdiction declares **only what differs** and inherits the rest:

```yaml
ies:
  thresholds:
    overrides:
      NY:
        FOOD_RESOURCE_LIMIT: 3000      # baseline is 2750
```

A test proves this changes a real outcome — the same household, the same rule
class, eligible in one jurisdiction and not in the other — with no Java touched.
The override is narrow: it replaces the limits it names and leaves the poverty
guidelines, the other limits and every other jurisdiction alone.

### A program a jurisdiction does not run

`Jurisdiction.offeredPrograms()` comes from its configured program names, so a
jurisdiction that lists no energy program does not have one. Asking for it
returns **409**, and `determine` (all programs) runs only the two it offers.
Falling back to federal rules there would be the dangerous behaviour: it would
produce a determination for a benefit the agency cannot issue.

### Dates belong to the jurisdiction, not the server

```java
LocalDate asOf = LocalDate.ofInstant(clock.instant(), jurisdiction.zoneId());
```

A determination run at 7pm Eastern on 31 December is still a December
determination in New York while the server's UTC clock has already rolled into
January — and the threshold set in force differs across that boundary. A test
pins exactly that instant.

### Onboarding a new jurisdiction

1. **Add a configuration block.** Programs offered, their local names, the time
   zone:

   ```yaml
   ies:
     jurisdictions:
       TX:
         display-name: State of Texas
         zone-id: America/Chicago
         program-names:
           FOOD_ASSISTANCE: SNAP Food Benefits
           CASH_ASSISTANCE: Temporary Assistance for Needy Families (TANF)
   ```

2. **Override any numbers that differ** under `ies.thresholds.overrides`.

3. **Only if its rules differ**, add a `ProgramRuleSet` whose
   `jurisdictionCode()` returns the code, reusing the baseline rules it keeps
   and substituting the ones it does not. Nothing else changes: not the engine,
   not the API, not the other jurisdictions.

Steps 1 and 2 are a config change and a deploy. Step 3 is the only one that is
code, and it is scoped to the one rule that actually differs.

---

## Phase 1 — the deterministic core

### An immutable snapshot, not the entity

Rules evaluate a `HouseholdSnapshot` record, never a mutable JPA entity, and
receive it inside a `RuleContext` together with the thresholds in force. Rules
have no clock, no randomness and no I/O, so the same context always produces the
same result. A test asserts it.

### The engine never changes when a program is added

```java
public interface ProgramRuleSet {
    ProgramType programType();
    default Optional<String> jurisdictionCode() { return Optional.empty(); }
    List<EligibilityRule> rules();
    default List<EligibilityRule> shortCircuitRules() { return List.of(); }
    default boolean appliesTo(Jurisdiction j) { ... }
    default int specificity() { return jurisdictionCode().isPresent() ? 1 : 0; }
}
```

`DeterminationEngine` takes `List<ProgramRuleSet>`; Spring injects every
implementation on the classpath. Adding the energy program meant adding a class,
not editing the engine. Open/Closed, and Dependency Inversion because the engine
depends only on the abstraction.

### The audit trail

```json
{
  "jurisdictionCode": "NY",
  "programType": "FOOD_ASSISTANCE",
  "programName": "Supplemental Nutrition Assistance Program (SNAP)",
  "outcome": "INELIGIBLE",
  "ruleSetVersion": "2026.10.1/rules-baseline/thresholds-2026.1",
  "firedRules": [{
    "ruleId": "FOOD_GROSS_INCOME_TEST",
    "status": "FAIL",
    "reason": "Gross monthly income $3000.00 exceeds the 130% FPL limit of $2887.30 for a household of 3 (thresholds 2026.1).",
    "policyReference": "FOOD-GIT-130"
  }]
}
```

`ruleSetVersion` names three things: the engine, **which rule set decided**
(`rules-baseline` or `rules-NY`), and the threshold version. Without the middle
one an audit cannot tell a baseline decision from an overridden one.

**Categorical eligibility does not hide failures.** A household eligible via SSI
returns `ELIGIBLE` *and* the failing income test still appears in the trail.
Suppressing it would make the decision unreviewable.

---

## Phase 2 — events and grounded notices

### Determination events

Published keyed by household id, so every event for one household lands on the
same partition and stays ordered. The event carries the **reasons**, not just
the outcome — a consumer that prints the notice needs them, and asking back over
HTTP would couple it to this service.

Producer config is `acks=all` with `enable.idempotence`, against a topic with
`min.insync.replicas=2`. `acks=1` acknowledges on leader write and loses the
record if the leader dies before replication.

Send failures are **logged, not rethrown**: the determination is already
committed and legally valid, so failing the caller's request because a broker is
down would be the wrong trade. A real deployment pairs this with a transactional
outbox.

Publishing is behind an interface, so tests and local runs need no broker.

### Notice generation, and why it can be trusted

The system prompt is strict, but prompts are not a guarantee — so there is a
check after it:

```java
// Monetary figures in the generated text must appear in the grounded facts.
List<String> ungrounded = ungroundedFigures(body, facts);
if (!ungrounded.isEmpty()) throw new UngroundedNoticeException(ungrounded);
```

A notice stating a number the engine never produced is indefensible, so it is
**rejected rather than stored**, surfaced as `502` so it alerts rather than
reaching a caseworker. Tested with a deliberately hallucinating client.

The LLM sits behind `LlmClient`. The default `TemplateLlmClient` is
deterministic and offline, so the whole notice pipeline — prompt assembly,
grounding, validation, persistence — is tested without an API key.

---

## Phase 3 — retrieval for caseworkers

```
INGEST   document -> split (~150 words, 15% overlap, paragraph boundaries)
                  -> embed -> index (+ source, section, corpus)
                  -> delete-by-document first, so no stale chunks survive

QUERY    question -> embed -> top-k 20 (wide, cheap)
                  -> rerank to 4 (narrow, precise)   <-- most of the quality
                  -> prompt: system + passages + question
                  -> answer + citations
```

**Chunking** is the highest-leverage decision. Too small and the surrounding
context is lost; too large and the embedding becomes an average of several
topics and stops discriminating. Overlap exists so an answer straddling a
boundary is not cut in half — one chunk holding the condition and the next
holding the number destroys an answer that was present in the source.

**Retrieve wide, rerank narrow.** Embedding search encodes query and chunk
separately and compares vectors; it never sees them together. A reranker scores
the pair — far more accurate, far too slow over a whole corpus, perfectly
affordable over 20 candidates. `RerankerTest` asserts it promotes the passage
that *answers* the question above one that merely shares a topic.

**Retrieval quality and generation quality are measured separately.** If the
right chunk is not in the candidate set, no prompt work recovers the answer — so
`retrieve()` is usable and testable on its own.

**Corpora are isolated.** A policy question must not retrieve a hearing decision
and present it as policy; they carry different authority.

**Graceful degradation.** If the model is down, the passages are still returned.
A caseworker with the three most relevant policy sections and no summary is
better served than one with a `500`.

---

## Phase 4 — thresholds as data, and access control

### Effective-dated thresholds

Poverty guidelines are reissued annually and resource limits change by statute.
A determination made last year must still be explainable today, so thresholds
are **versioned data with an effective date**, not constants in rules:

```java
ThresholdSet inForce = thresholds.forDate(jurisdiction, household.asOf());
```

The determination records
`2026.10.1/rules-baseline/thresholds-2026.1`, pinning the engine, the rule set
and the policy version. Two tests cover this: the same income decides
differently under the 2025 and 2026 sets, and a household assessed as of 2025 is
re-decided under 2025 thresholds. A date with no set in force is **refused**
rather than guessed.

### Roles

| Role | May |
|---|---|
| `CASEWORKER` | Intake, run determinations, read everything, use `/assist` |
| `SUPERVISOR` | Also **issue notices** and **index policy material** |

Issuing a notice is separated because it is the act that **starts the appeal
clock**. Running a determination is reversible; sending a household a legal
notice is not. Indexing policy is restricted because it changes what every
caseworker is told.

> Authentication here is HTTP Basic over in-memory users — a demonstration, not
> a model for production auth.

---

## Running it

**Prerequisites:** JDK 21, Maven 3.9+, and Docker with Compose. Nothing else —
no API key, no cloud account.

```bash
git clone https://github.com/SaiPramod-CodeHUB/ies-eligibility-platform.git
cd ies-eligibility-platform
docker compose up --build
```

Compose brings up PostgreSQL 16 and the application together. First build pulls
images and dependencies, so allow a few minutes; afterwards it starts in seconds.

| | |
|---|---|
| API | http://localhost:8080 |
| OpenAPI UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Users | `caseworker` / `caseworker` · `supervisor` / `supervisor` |

Stop it with `docker compose down`, or `docker compose down -v` to drop the
database volume as well.

### Tests only — no database, no broker, no API key

```bash
mvn clean verify
```

77 tests against H2 and in-memory stand-ins for every external dependency.
This is the fastest way to see the engine behave without starting anything.

### Without Docker

The application itself needs only a reachable PostgreSQL. Point it at one and
run the jar:

```bash
mvn clean package
DB_URL=jdbc:postgresql://localhost:5432/ies DB_USER=ies DB_PASSWORD=ies \
  java -jar target/ies-eligibility-platform-0.1.0.jar
```

`DB_URL`, `DB_USER`, `DB_PASSWORD` and `IES_DEFAULT_JURISDICTION` are the only
environment variables that matter; all four have defaults in
`application.yml`.

### Worked example

```bash
AUTH='-u caseworker:caseworker'

# 1. a household of two, $1,500/mo earned, $500 resources
curl -s $AUTH -X POST localhost:8080/api/v1/households \
  -H 'Content-Type: application/json' -d '{
    "jurisdictionCode": "NY", "locality": "Albany County",
    "countableResources": 500.00, "heatingCostResponsible": true,
    "members": [
      {"firstName":"Ada","lastName":"Doe","dateOfBirth":"1990-05-01","disabled":false,"receivesSsi":false},
      {"firstName":"Kid","lastName":"Doe","dateOfBirth":"2020-03-01","disabled":false,"receivesSsi":false}
    ],
    "income": [{"type":"EARNED","monthlyGross":1500.00,"description":"wages"}]
  }'

# 2. determine every program the jurisdiction offers, with the full audit trail
curl -s $AUTH -X POST localhost:8080/api/v1/households/1/determine | jq

# 2b. one program. The response carries NY's own name for it; the same call
#     against a TX household returns "SNAP Food Benefits" instead.
curl -s $AUTH -X POST localhost:8080/api/v1/households/1/determine/FOOD_ASSISTANCE | jq

# 3. draft the notice (supervisor only)
curl -s -u supervisor:supervisor -X POST localhost:8080/api/v1/determinations/1/notice | jq

# 4. ask the policy manual a question — answered only from indexed policy, cited
curl -s $AUTH 'localhost:8080/api/v1/assist/policy?q=is+self-employment+income+countable' | jq

# 5. find similar prior Fair Hearing decisions
curl -s $AUTH 'localhost:8080/api/v1/assist/precedent?q=resource+limit+disabled+member' | jq
```

To publish determination events to a real broker:
`IES_EVENTS_PUBLISHER=kafka KAFKA_BOOTSTRAP=localhost:9092`

---

## API

| Method | Path | Role |
|---|---|---|
| `POST` | `/api/v1/households` | caseworker |
| `GET` | `/api/v1/households/{id}` | caseworker |
| `POST` | `/api/v1/households/{id}/determine` | caseworker |
| `POST` | `/api/v1/households/{id}/determine/{programType}` | caseworker |
| `GET` | `/api/v1/households/{id}/determinations` | caseworker |
| `GET` | `/api/v1/determinations/{id}` | caseworker |
| `POST` | `/api/v1/determinations/{id}/notice` | **supervisor** |
| `GET` | `/api/v1/determinations/{id}/notice` | caseworker |
| `GET` | `/api/v1/assist/policy?q=` | caseworker |
| `GET` | `/api/v1/assist/precedent?q=` | caseworker |
| `POST` | `/api/v1/assist/ingest` | **supervisor** |

---

## Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 — MVC, Data JPA, Security, Validation, Actuator |
| Messaging | Apache Kafka (spring-kafka), behind an interface |
| Database | PostgreSQL 16 · H2 in tests |
| AI | `LlmClient` / `EmbeddingClient` / `VectorStore` / `Reranker` abstractions with offline defaults |
| API docs | springdoc OpenAPI 3 |
| Tests | JUnit 5, AssertJ, Mockito, MockMvc, spring-security-test — **77 tests** |
| Container | Docker multi-stage, non-root, `MaxRAMPercentage` |
| Orchestration | Kubernetes manifests, correct readiness/liveness split |
| CI | Jenkinsfile (declarative pipeline) |

### Why the AI layers are interface-based

`LlmClient`, `EmbeddingClient`, `VectorStore` and `Reranker` each have a
deterministic local default and a documented swap point. That is deliberate:

- the pipelines are **fully tested** with no API key and no network
- the project **runs offline** for a demo
- the reranker's contribution is **measurable** rather than asserted

`HashingEmbeddingClient` is honestly labelled: it matches shared vocabulary, not
shared meaning, so it is a pipeline harness and not a semantic model. Swapping in
a hosted embedding model changes nothing else.

---

## Two real bugs found while building this

**Chunk id collision.** Ids were `corpus::source::index`, so two *sections* of
the same document collided and the second silently overwrote the first — the
index looked populated and half the corpus was missing. The section is now part
of the identity, and `sectionsOfTheSameDocumentDoNotCollide` is the regression
test.

**Upsert does not prevent stale chunks.** A re-indexed document that got shorter
left orphaned chunks behind, which kept being retrieved. Fixed with
delete-by-document before insert, covered by `reIndexRemovesOrphanedChunks`.
This is the most common way a retrieval system becomes quietly wrong: not by
hallucinating, but by being accurate about a superseded version.

## Other engineering notes

**`Set` not `List` for the JPA collections.** Hibernate treats a `List` mapping
as a bag and refuses to `JOIN FETCH` two of them —
`MultipleBagFetchException`. The engine reads members *and* income on every
determination, so fetching separately would be an N+1 on the hot path.

**Liveness does not touch the database.** Readiness gates traffic; liveness
restarts the pod. A liveness probe that checks a downstream dependency restarts
healthy pods when that dependency slows, turning a partial outage into a full
one.

**The clock is injected.** `Clock` is a bean, so tests pin a fixed instant and
assert determinism instead of tolerating time-dependent results.

**`MaxRAMPercentage` not a fixed `-Xmx`.** The JVM sizes its heap to the
container limit rather than the host's memory.

---

## What a production version would need

Being explicit about the gap, because a portfolio project that pretends to be
production-ready is worse than one that doesn't:

- Thresholds in the database, maintained by policy staff with an approval trail
- Transactional outbox so a determination event cannot be lost
- Real authentication against the agency directory; identity as a signed token
- pgvector or a dedicated vector database with an ANN index, not brute-force cosine
- A hosted embedding model and a cross-encoder reranker
- Document intake: extracting income from uploaded pay stubs
- Benefit *amount* calculation, not only the eligibility decision
- Per-tenant isolation enforced in the query, if ever multi-agency
- Jurisdiction configuration as data with change control, not YAML in the jar:
  a state's program names and limits change by statute, and they need the same
  approval trail as thresholds
- Policy corpora partitioned by jurisdiction, so `/assist` cannot cite one
  state's manual when answering for another — the single biggest gap left in
  the jurisdiction work here

---

Sai Pramod Reddy Gopireddy — [github.com/SaiPramod-CodeHUB](https://github.com/SaiPramod-CodeHUB)
