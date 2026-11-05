# EBANX Ledger Engine — Meu plano de implementação

## Stack que escolhi
- Kotlin + Ktor (API HTTP leve)
- Armazenamento em memória (`ConcurrentHashMap`) — sem banco, sem Kafka
- JUnit 5 + Konsist (testes de arquitetura) + Ktor test-host (testes de integração)
- Gradle Kotlin DSL, com wrapper versionado no repo
- GitHub Actions como pipeline de CI

## Como eu trabalho aqui
- **TDD**: em cada história eu escrevo o teste antes da implementação, vejo ele
  falhar (red), implemento o mínimo pra passar (green) e só então considero
  refatorar (refactor). Os commits refletem isso — cada história chega com teste
  e implementação juntos, nunca implementação sem teste.
- **Arquitetura hexagonal**: `domain` e `application` são o centro (regra de
  negócio pura, sem Ktor). `ports` define as interfaces que o centro expõe pra
  fora. `infrastructure` e `http` são adapters — eles conhecem o centro, o
  centro nunca conhece eles.
- **Testes de arquitetura (Konsist)**: eu não confio só em disciplina — escrevi
  um teste (`LayeringTest`) que quebra o build se eu (ou alguém depois de mim)
  importar `io.ktor` ou `infrastructure` dentro de `domain`/`application`. Isso
  vira meu guarda-trilho pra não deixar a arquitetura hexagonal degradar com o
  tempo.
- **SRP em primeiro lugar**: cada caso de uso resolve uma única operação
  (Deposit, Withdraw, Transfer, Reset, GetBalance). Nenhum "AccountService"
  genérico fazendo tudo.
- **Complexidade ciclomática baixa**: evito mais de 2 decisões aninhadas por
  função. Quando preciso de mais, extraio função ou movo a decisão pra dentro
  do próprio objeto de domínio (ex.: `Account` decide se pode sacar).
- **Sem abstração prematura**: a única interface que crio antecipadamente é
  `AccountRepository` (porta mínima), porque o enunciado já avisa "we may ask
  for modifications" — troca de storage é a mudança mais provável de pedirem.
  Fora isso, eu concretizo primeiro e só abstraio quando duplicar de verdade.
- **Testes dinâmicos**: a suíte de integração que reproduz os cenários do
  enunciado (`EventScenariosTest`) usa `@TestFactory` do JUnit 5 — cada cenário
  do enunciado é um `DynamicTest` gerado a partir de uma lista de dados
  (request → status esperado → body esperado). Isso deixa claro, lendo a lista,
  quais casos eu cobri, e adicionar um cenário novo é só adicionar uma linha,
  não escrever um método de teste novo.

## Por que não uso Kafka nem DynamoDB
O enunciado é explícito: "Durability IS NOT a requirement" e "do not try to
anticipate anything that is not part of the spec". Mensageria assíncrona
resolveria um problema que não existe aqui (não há consumidor, não há
desacoplamento de serviços). DynamoDB fica isolado atrás de `AccountRepository`
— se me pedirem durabilidade depois, eu troco só a implementação, sem tocar em
domínio, casos de uso ou HTTP.

## Histórias (cada uma = 1 commit pequeno, testável, em primeira pessoa)

1. **chore**: subo o esqueleto Kotlin/Ktor + arquitetura hexagonal + CI + teste
   de arquitetura — feito.
2. **feat**: domínio — `Account` imutável, com `deposit`/`withdraw`.
3. **feat**: `AccountRepository` (porta) + `InMemoryAccountRepository`.
4. **feat**: caso de uso `DepositUseCase`.
5. **feat**: caso de uso `WithdrawUseCase`.
6. **feat**: caso de uso `TransferUseCase` (composição de withdraw+deposit).
7. **feat**: casos de uso `ResetUseCase` e `GetBalanceUseCase`.
8. **feat**: rota HTTP `POST /event`.
9. **feat**: rota HTTP `GET /balance`.
10. **feat**: rota HTTP `POST /reset`.
11. **test**: suíte dinâmica reproduzindo todos os cenários do enunciado.
12. **docs**: README com instruções de build/run/deploy e decisões de arquitetura.
13. **chore**: deploy público (Docker + plataforma gratuita) pra rodar o
    suite automatizado da EBANX contra a URL.

## Critério de pronto por história
Só considero uma história fechada quando: o teste que escrevi pra ela falha
antes da implementação (vi o red), passa depois (green), o `./gradlew test`
completo continua verde (incluindo os testes de arquitetura) e eu não
introduzi nada que as próximas histórias ainda não peçam.
