# EBANX Ledger Engine

A balance/events API (deposit, withdraw, transfer) for the EBANX take-home
assignment. No persistence — everything in memory, as the assignment asks.

## How I run it locally

```bash
./gradlew run
```

The API comes up on `http://localhost:8080`.

## How I run the tests

```bash
./gradlew test
```

This runs unit tests (domain and use cases), architecture tests (Konsist,
guaranteeing the hexagonal architecture doesn't degrade) and integration
tests (the HTTP routes, including the exact scenario sequence from the
assignment).

## Why Ktor, not Spring Boot

I picked Ktor over Spring Boot on purpose, not out of familiarity:

- **Less magic, more explicit control.** The route that exposes
  `POST /event` is a 3-line function in [`Routing.kt`](./src/main/kotlin/com/ebanx/ledger/http/Routing.kt):
  read the request, call `EventDispatcher`, respond. A Spring equivalent
  (`@RestController` + `@PostMapping`) isn't actually simpler here — it
  trades those 3 lines for annotations processed by reflection, a DI
  container, and a bean lifecycle I don't need for 3 routes.
- **Startup and footprint.** Ktor with Netty boots in ~1-2s with a handful
  of dependencies. Spring Boot boots the whole Spring context —
  auto-configuration scanning, bean creation — real overhead for a
  3-endpoint API with no database.
- **It matches the assignment's own "simplicity" criterion.** The EBANX
  checklist asks to "avoid over-engineering" and says "it's not necessary
  to apply complex design patterns". A DI container, constructor/field
  injection via `@Autowired`, Spring Data I wouldn't even use (everything
  here is in memory) — that's heavy tooling for a small problem.
- **Error mapping lives in one visible place.** `StatusPages` (installed
  in `Routing.kt`) maps domain exceptions to HTTP status codes as one
  explicit function. The Spring equivalent (`@ExceptionHandler`/
  `@ControllerAdvice`) works too, but it's another layer of
  annotation-driven magic to follow.

This isn't a claim that Spring is bad — for a larger API with several
modules, Spring Data/Security/Cloud earn their weight. For this
take-home, Ktor is the right-sized tool.

## My architecture decisions

I wrote the full reasoning in [`PLAN.md`](./PLAN.md): why Kotlin + Ktor,
why no Kafka/DynamoDB, how TDD guided each story, how the architecture
tests protect the hexagonal boundary, and why the assignment's scenario
sequence is a sequential test and not a `@TestFactory`.

Summarizing the structure:

```
src/main/kotlin/com/ebanx/ledger/
├── domain/          # Account — pure business rule, no Ktor
├── application/     # one use case per operation (SRP)
├── infrastructure/  # AccountRepository (the port) + InMemoryAccountRepository (the only impl)
└── http/            # DTOs, EventDispatcher, Ktor routes
```

I simplified things by putting the `AccountRepository` port in the same
package as its single implementation, instead of a separate `ports`
package just for one interface — one less folder. `domain`/`application`
may import the interface, but never the concrete implementation
(`InMemoryAccountRepository`) nor anything from `http`/Ktor — this finer
rule is what `LayeringTest` checks automatically.

## Endpoints

| Method | Route | Description |
|---|---|---|
| `POST` | `/reset` | Clears all state |
| `GET` | `/balance?account_id=:id` | Account balance (plain text); `404 0` if it doesn't exist |
| `POST` | `/event` | `{"type": "deposit"\|"withdraw"\|"transfer", ...}` |

Business rules that apply to `withdraw` and `transfer` (the second reuses
the first): the origin account must exist (`404` if it doesn't) and must
have sufficient funds (`400` if it doesn't — this invariant lives in
`Account.withdraw`, it's not just edge validation in the route). A zero or
negative deposit/withdraw amount is also rejected with `400` — without
that check, a negative withdraw would pass the balance validation and
behave like an unlimited deposit.

## Consistency under concurrency

`AccountRepository.update(id, mutate)` reads, transforms and writes as
one single atomic operation (via `ConcurrentHashMap.compute`) — not as a
`findById` followed by a separate `save`, which would leave a window for
two concurrent requests on the same account to lose an update, or for
both to pass a withdraw that should have failed. This has a real test
proving it (200 threads depositing 1 at the same time, final balance has
to be 200, deterministically). Details and the trade-off I accept on
transfers (each leg atomic, but the two legs aren't a single atomic
transaction) are in [`PLAN.md`](./PLAN.md).

## CI

Every push/PR runs `./gradlew test` on GitHub Actions
(`.github/workflows/ci.yml`).
