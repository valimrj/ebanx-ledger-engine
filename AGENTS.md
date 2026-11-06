# AGENTS.md — EBANX Ledger Engine

Regras pra qualquer agente (humano ou IA) que trabalhe neste repo. Um
pedido explícito da pessoa vence sobre qualquer regra aqui, mas avise
antes: diga contra qual regra está indo e por quê.

**Contexto do projeto**: take-home "Ledger Engine" da EBANX — API de
saldo e eventos (deposit/withdraw/transfer), em memória, simples. Specs
em [`docs/spec.md`](./docs/spec.md); decisões técnicas no
[`README.md`](./README.md).

## Princípios

- **Simplicidade antes de sofisticação.** Entre duas soluções que
  atendem ao requisito, use a mais simples.
- **TDD.** Nenhuma implementação sem teste que falhou antes (red →
  green → refactor).
- **SRP.** Um caso de uso resolve uma operação só. Sem classe "Service"
  genérica.
- **Hexagonal.** `domain`/`application` nunca importam `io.ktor` nem
  implementação concreta de repositório — garantido por
  `LayeringTest`, não por convenção.
- **Complexidade ciclomática baixa.** Evite abstração só pra reduzir
  linhas; prefira funções pequenas e poucas decisões aninhadas.
- **Sem abstração prematura.** Não crie interface, classe base ou
  generalização pra um caso de uso que ainda não existe.
- **DRY não é absoluto.** Duplicação pequena e isolada é mais barata
  que uma abstração errada só pra evitar repetir duas linhas.
- **SOLID quando fizer sentido, sem over-engineering.** Use os
  princípios pra guiar, não como checklist a cumprir à força.
- **Clean code em nomes.** Nome de variável e método deve deixar claro
  o que a coisa é/faz, sem precisar de comentário pra explicar.
- **Não force um design pattern no problema.** Deixe o padrão emergir
  da necessidade real, não o contrário.
- **Idioma**: documentação em português; código, comentários, KDoc e
  nomes de teste em inglês.
- **Voz**: README e `docs/spec.md` em primeira pessoa (descrevem
  trabalho feito); commits também. Este arquivo é regra, não relato —
  fala em modo diretivo.

## Nunca faça

- Implementar validação ou comportamento pra cenário fora do enunciado
  ou do checklist de avaliação.
- Adicionar dependência (banco, mensageria, framework) sem necessidade
  comprovada pelo enunciado.
- Separar leitura e escrita de conta em dois passos (`findById` +
  `save`). Toda escrita passa por `AccountRepository.update(id, mutate)`,
  atômica.
- Deixar uma rota `GET` escrever no repositório, em qualquer
  circunstância.
- Colocar regra de negócio no handler HTTP. Handler só lê a requisição e
  delega; validação de saldo/valor vive no domínio.
- Comitar sem `./gradlew test` verde.
- Usar `--amend`, `push --force` ou pular hooks sem a pessoa pedir.
- Tomar decisão de arquitetura (stack, framework, deploy) sem
  confirmação explícita da pessoa, mesmo com recomendação forte.

## Convenções

- Commits: Conventional Commits (`feat`, `fix`, `refactor`, `test`,
  `docs`, `chore`) + descrição curta em primeira pessoa. Uma história =
  um commit pequeno e testável.
- Exceção de regra de negócio (`InsufficientFundsException`,
  `InvalidAmountException`) vive em `domain`; exceção de
  orquestração/lookup (`AccountNotFoundException`) vive em
  `application`.
- CI roda `./gradlew test` em todo push/PR; nada é "pronto" sem CI
  verde.

## Antes de qualquer mudança

1. Confira `docs/spec.md` — se o comportamento não está especificado,
   pergunte antes de implementar.
2. Escreva o teste que falha.
3. Implemente o mínimo pra passar.
4. Rode `./gradlew test` completo.
5. Atualize `docs/spec.md` ou `README.md` no mesmo commit da mudança.
6. Comite pequeno.
