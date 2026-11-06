# AGENTS.md — EBANX Ledger Engine

Regras pra qualquer agente (humano ou IA) que trabalhe neste repo. Um
pedido explícito da pessoa vence sobre qualquer regra aqui, mas avise
antes: diga contra qual regra está indo e por quê.

**Contexto do projeto**: take-home "Ledger Engine" da EBANX — API de
saldo e eventos (deposit/withdraw/transfer), em memória, simples. Specs
em [`docs/spec.md`](./docs/spec.md); o porquê da arquitetura escolhida
e as convenções específicas dela (camadas, onde cada coisa mora) estão
no [`README.md`](./README.md) — este arquivo não repete isso, só traz
regras de comportamento válidas pra qualquer arquitetura.

## Princípios

- **Simplicidade antes de sofisticação.** Entre duas soluções que
  atendem ao requisito, use a mais simples.
- **TDD.** Nenhuma implementação sem teste que falhou antes (red →
  green → refactor).
- **SRP.** Um caso de uso resolve uma operação só. Sem classe "Service"
  genérica.
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
- **Sem magic number nem string solta.** Número ou string repetido ou
  sem significado óbvio no contexto ganha uma constante nomeada (ex.:
  `ERROR_RESPONSE_BODY` em vez de `"0"` espalhado em três lugares).
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
- Misturar regra de negócio com código de transporte (HTTP,
  serialização). São responsabilidades diferentes, não misture.
- Comitar sem `./gradlew test` verde.
- Usar `--amend`, `push --force` ou pular hooks sem a pessoa pedir.
- Tomar decisão de arquitetura (stack, framework, deploy) sem
  confirmação explícita da pessoa, mesmo com recomendação forte.

## Convenções

- Commits: Conventional Commits (`feat`, `fix`, `refactor`, `test`,
  `docs`, `chore`) + descrição curta em primeira pessoa. Uma história =
  um commit pequeno e testável.
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
