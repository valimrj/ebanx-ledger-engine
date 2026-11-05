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
├── infrastructure/  # AccountRepository (a porta) + InMemoryAccountRepository (a unica impl)
└── http/            # DTOs, EventDispatcher, rotas Ktor
```

Simplifiquei juntando a porta `AccountRepository` no mesmo pacote da sua
única implementação, em vez de um pacote `ports` separado só pra uma
interface — uma pasta a menos. `domain`/`application` podem importar a
interface, mas nunca a implementação concreta (`InMemoryAccountRepository`)
nem nada de `http`/Ktor — essa regra mais fina é o que o `LayeringTest`
verifica automaticamente.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reset` | Limpa todo o estado |
| `GET` | `/balance?account_id=:id` | Saldo da conta (texto plano); `404 0` se não existir |
| `POST` | `/event` | `{"type": "deposit"\|"withdraw"\|"transfer", ...}` |

Regras de negócio que valem pra `withdraw` e `transfer` (a segunda reusa a
primeira): a conta de origem precisa existir (`404` se não existir) e
precisa ter saldo suficiente (`400` se não tiver — essa invariante vive em
`Account.withdraw`, não é só validação de borda na rota).

## CI

Todo push/PR roda `./gradlew test` no GitHub Actions
(`.github/workflows/ci.yml`).
