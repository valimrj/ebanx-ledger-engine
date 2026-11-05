# EBANX Ledger Engine

API de saldo/eventos (deposit, withdraw, transfer) pro take-home assignment
da EBANX. Sem persistência — tudo em memória, como o enunciado pede.

## Como eu rodo localmente

```bash
./gradlew run
```

A API sobe em `http://localhost:8080`.

## Como eu rodo os testes

```bash
./gradlew test
```

Isso roda testes unitários (domínio e casos de uso), testes de arquitetura
(Konsist, garantindo que a arquitetura hexagonal não degrade) e testes de
integração (as rotas HTTP, incluindo a sequência exata de cenários do
enunciado).

## Minhas decisões de arquitetura

Eu escrevi o raciocínio completo em [`PLAN.md`](./PLAN.md): por que Kotlin +
Ktor, por que sem Kafka/DynamoDB, como TDD guiou cada história, como os
testes de arquitetura protegem a fronteira hexagonal, e por que a sequência
de cenários do enunciado é um teste sequencial e não um `@TestFactory`.

Resumindo a estrutura:

```
src/main/kotlin/com/ebanx/ledger/
├── domain/         # Account — regra de negocio pura, sem Ktor
├── application/     # um caso de uso por operacao (SRP)
├── ports/           # AccountRepository — a unica interface antecipada
├── infrastructure/  # InMemoryAccountRepository
└── http/            # DTOs, EventDispatcher, rotas Ktor
```

A dependência só pode ir "de fora pra dentro": `http` e `infrastructure`
conhecem `domain`/`application`; o inverso nunca — e isso é verificado
automaticamente pelo `LayeringTest`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reset` | Limpa todo o estado |
| `GET` | `/balance?account_id=:id` | Saldo da conta (texto plano); `404 0` se não existir |
| `POST` | `/event` | `{"type": "deposit"\|"withdraw"\|"transfer", ...}` |

## CI

Todo push/PR roda `./gradlew test` no GitHub Actions
(`.github/workflows/ci.yml`).
