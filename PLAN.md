# EBANX Ledger Engine — My implementation plan

## Stack I chose
- Kotlin + Ktor (lightweight HTTP API)
- In-memory storage (`ConcurrentHashMap`) — no database, no Kafka
- JUnit 5 + Konsist (architecture tests) + Ktor test-host (integration tests)
- Gradle Kotlin DSL, with the wrapper checked into the repo
- GitHub Actions as the CI pipeline

## How I work here
- **TDD**: on every story I write the test before the implementation, see
  it fail (red), implement the minimum to pass (green), and only then
  consider refactoring (refactor). The commits reflect this — every story
  arrives with test and implementation together, never implementation
  without a test.
- **Hexagonal architecture**: `domain` and `application` are the center
  (pure business rule, no Ktor). The `AccountRepository` port lives inside
  `infrastructure`, next to its single implementation — I simplified it
  this way to avoid a `ports` package with a single file. `http` and
  `infrastructure` are adapters — they know the center, the center never
  knows them.
- **Architecture tests (Konsist)**: I don't rely on discipline alone — I
  wrote a test (`LayeringTest`) that breaks the build if I (or anyone
  after me) imports `io.ktor` or a CONCRETE infrastructure implementation
  (`InMemoryAccountRepository`) inside `domain`/`application` — the
  interface may cross, the implementation never. This becomes my guard
  rail against the hexagonal architecture degrading over time.
- **SRP first**: each use case solves a single operation (Deposit,
  Withdraw, Transfer, Reset, GetBalance). No generic "AccountService"
  doing everything.
- **Low cyclomatic complexity**: I avoid more than 2 nested decisions per
  function. When I need more, I extract a function or move the decision
  into the domain object itself (e.g. `Account` decides whether it can
  withdraw).
- **No premature abstraction**: the only interface I create ahead of time
  is `AccountRepository` (a minimal port), because the assignment already
  warns "we may ask for modifications" — swapping storage is the most
  likely change they'd ask for. Beyond that, I go concrete first and only
  abstract when something genuinely duplicates.
- **Dynamic tests**: I use JUnit 5's `@TestFactory`/`DynamicTest` in
  `EventDispatcherTest`, where the cases are independent of each other (a
  table of deposit amounts, each with its own in-memory repository) —
  that's exactly the scenario where `DynamicTest` fits well. The
  assignment's full scenario sequence (`EventScenariosTest`), on the other
  hand, I write as a single sequential test: the steps are ordered and
  dependent (one step's balance affects the next, against the same test
  server), and forcing that into `DynamicTest` would require recreating
  the application at every step, losing the state.

## Why I don't use Kafka or DynamoDB
The assignment is explicit: "Durability IS NOT a requirement" and "do not
try to anticipate anything that is not part of the spec". Async messaging
would solve a problem that doesn't exist here (there's no consumer, no
service decoupling). DynamoDB stays isolated behind `AccountRepository` —
if they ask for durability later, I only swap the implementation, without
touching the domain, use cases, or HTTP.

## How I guarantee consistency under concurrency (critical point)
My first version of `DepositUseCase`/`WithdrawUseCase` did
`repository.findById(id)` and then `repository.save(newAccount)` as two
separate steps. `ConcurrentHashMap` protects the data structure, but
doesn't protect that read → decide → write sequence: two concurrent
withdraw requests on the same account could read the same stale balance
and both overwrite each other (lost update), or both see sufficient funds
at read time and both withdraws go through, leaving the account negative
— the exact bug `InsufficientFundsException` exists to prevent.

I fixed it by replacing `findById`+`save` with `AccountRepository.update(id,
mutate)`, implemented with `ConcurrentHashMap.compute`: reading,
transforming and writing as one single atomic operation per key. Two
concurrent calls for the same account are serialized by `ConcurrentHashMap`
itself — the second one only decides after the first has finished
writing. As a bonus, if `mutate` throws (insufficient funds), nothing is
written — the rollback is automatic.

I proved this with a real concurrency test (not just reading the code):
200 threads depositing 1 simultaneously on the same account, all released
at once by a `CountDownLatch`, and the final balance has to be exactly
200. Without the fix, this test would be flaky (a variable result, less
than 200); with it, it's deterministic.

`TransferUseCase` is still two separate atomic updates (withdraw from the
origin, then deposit into the destination) — each individually atomic and
consistent, but the transfer as a whole isn't a single atomic transaction
across the two accounts. This is a conscious choice, not an oversight:
since `deposit` never fails, if the origin's `withdraw` succeeds, the
destination's `deposit` will always complete — the only window is a
concurrent reader being able to see, for a fraction of a second, the
origin already debited and the destination not yet credited (the money
"in transit"), without ever creating or destroying balance. Closing that
window would need a global lock covering both accounts, which would
serialize every transfer in the system — a trade-off I wouldn't make
without the assignment explicitly asking for durability or transactional
isolation.

## An invalid (zero or negative) amount isn't treated as if it were valid
Reviewing EBANX checklist item 10 ("common bugs": invalid values not
handled, inconsistent state after operations), I noticed that
`Account.withdraw` only checked `amount > balance` — a withdraw with a
negative `amount` passes that check with no problem (`-5 > 10` is false)
and the resulting `balance - amount` *adds* to the balance, meaning a
negative withdraw behaved like an unlimited deposit. I fixed it by adding
`requirePositive` at the three value entry points (`deposit`, `withdraw`,
`openWith`), throwing `InvalidAmountException` for zero or negative,
mapped to `400` in `StatusPages` — the same reasoning as
`InsufficientFundsException`, just for malformed input instead of
insufficient state.

## Stories (each one = 1 small, testable commit, in first person)

1. **chore**: bring up the Kotlin/Ktor skeleton + hexagonal architecture +
   CI + architecture test — done.
2. **feat**: domain — immutable `Account`, with `deposit`/`withdraw`.
3. **feat**: `AccountRepository` (port) + `InMemoryAccountRepository`.
4. **feat**: `DepositUseCase` use case.
5. **feat**: `WithdrawUseCase` use case.
6. **feat**: `TransferUseCase` use case (composition of withdraw+deposit).
7. **feat**: `ResetUseCase` and `GetBalanceUseCase` use cases.
8. **feat**: `POST /event` HTTP route.
9. **feat**: `GET /balance` HTTP route.
10. **feat**: `POST /reset` HTTP route.
11. **test**: dynamic suite reproducing all the assignment's scenarios.
12. **docs**: README with build/run/deploy instructions and architecture
    decisions.
13. **chore**: public deployment (Docker + a free platform) to run the
    EBANX automated suite against the URL.
14. **refactor**: merge the `AccountRepository` port into `infrastructure`
    (one less folder — see the note above on the cost of this
    simplification).
15. **feat**: `Account.withdraw` now throws `InsufficientFundsException`
    when the withdraw asks for more than the account has — this invariant
    wasn't in the assignment's examples, but the EBANX checklist makes it
    explicit that it's expected ("withdraws reduce the balance, respecting
    insufficient funds"). It lives in the domain, not the use case,
    because it's a rule about what an `Account` is, not an orchestration
    rule — `Transfer` gets the same protection for free, through
    composition, without me repeating the `if`. On the HTTP side, I map
    it to 400 (the account exists, it just has no funds — it's not a
    404).

## Definition of done per story
I only consider a story closed when: the test I wrote for it fails before
the implementation (I saw the red), passes afterward (green), the full
`./gradlew test` stays green (including the architecture tests), and I
haven't introduced anything the next stories don't yet ask for.
