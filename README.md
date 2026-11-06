# EBANX Ledger Engine

Minha solução pro take-home assignment da EBANX: uma API de saldo/eventos
(deposit, withdraw, transfer). Fiz sem persistência — tudo em memória,
como o enunciado pede.

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
(Konsist, garantindo que a separação de camadas não degrade) e testes de
integração (as rotas HTTP, incluindo a sequência exata de cenários do
enunciado).

Decisões de arquitetura (por que Ktor, por que essa separação em
camadas) e regras de negócio (endpoints, consistência sob concorrência)
não estão aqui — ver [`docs/spec.md`](./docs/spec.md) e
[`AGENTS.md`](./AGENTS.md).

## CI

Configurei pra rodar `./gradlew test` no GitHub Actions a cada push/PR
(`.github/workflows/ci.yml`).
