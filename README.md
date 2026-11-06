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
(Konsist, garantindo que a arquitetura hexagonal não degrade) e testes de
integração (as rotas HTTP, incluindo a sequência exata de cenários do
enunciado).

## Por que Ktor, e não Spring Boot

Ktor é o framework da JetBrains pra servidores e clientes HTTP em Kotlin.
Eu escolhi ele no lugar do Spring Boot de propósito, não por familiaridade:

- **Kotlin de verdade, não Kotlin por cima.** Ele foi escrito do zero em
  Kotlin, então usa DSLs, extension functions e coroutines de forma
  nativa — o código de rotas fica curto e legível (`routing { post("/event")
  { ... } }` lê como Kotlin puro, não como uma classe anotada).
- **Assíncrono por padrão, sem a cerimônia.** As coroutines dispensam
  callbacks e reactive streams: o código tem cara de síncrono, mas não
  bloqueia threads — útil com muitas conexões simultâneas, sem precisar
  aprender uma API reativa.
- **Leve e modular.** Eu só instalo o que realmente uso —
  `ContentNegotiation` pra JSON e `StatusPages` pro mapeamento de erro
  (ver `Application.kt` e `Routing.kt`) — como plugins, não um framework
  que liga tudo por padrão. O startup é rápido e o consumo de memória
  fica baixo, o que confirmei rodando `./gradlew run` localmente.
- **Pouca "mágica".** Não depende de reflection nem de anotações em
  excesso, então o fluxo da aplicação é explícito e fácil de seguir e
  depurar. A rota que expõe `POST /event` é uma função de 3 linhas: ler
  a requisição, chamar o `EventDispatcher`, responder. Um equivalente em
  Spring (`@RestController` + `@PostMapping`) não é mais simples aqui —
  ele troca essas 3 linhas por anotações processadas via reflection, um
  container de DI e um ciclo de vida de bean que eu não preciso pra 3
  rotas. O mapeamento de erro segue a mesma lógica: `StatusPages` mapeia
  exceções de domínio pra status HTTP como uma função explícita só, em
  vez de uma camada `@ExceptionHandler`/`@ControllerAdvice`.
- **Flexibilidade de engine.** Eu rodo em Netty, mas o Ktor também
  suporta CIO, Jetty ou Tomcat dependendo da necessidade do projeto.
- **Testes simples.** O `testApplication` sobe a aplicação em memória,
  sem precisar de um servidor real — é exatamente isso que
  `EventScenariosTest` e `ApplicationTest` usam pra exercitar as rotas
  HTTP reais.
- **Bate direto com o critério "simplicidade" do enunciado.** O
  checklist da EBANX pede "evite over-engineering" e diz "não é
  necessário aplicar padrões de design complexos". Um container de DI,
  injeção via `@Autowired`, Spring Data que eu nem usaria (tudo aqui é
  em memória) — é ferramenta pesada pra um problema pequeno.

Quando vale a pena o Ktor: microsserviços, APIs enxutas, projetos Kotlin
Multiplatform, ou quando baixo overhead e controle explícito importam
mais do que um framework "tudo incluso". Quando talvez não seja a
melhor opção: projetos grandes que dependem muito do ecossistema Spring
(Spring Data, Security, Batch, integrações prontas), ou times que já
dominam Spring Boot — nesses casos, o Spring ainda entrega mais coisa
pronta, e o ganho do Ktor pode não compensar a troca. O Ktor também
exige que eu monte mais coisa por conta própria, como injeção de
dependência (geralmente com Koin) e estrutura de projeto — aqui eu
mantive a DI manual (wiring direto por construtor em `Routing.kt`), já
que o grafo de objetos é pequeno o suficiente pra não precisar de um
framework pra isso.

Isso não quer dizer que Spring é ruim — pra uma API maior, com vários
módulos, Spring Data/Security/Cloud compensam o peso. Pra esse
take-home, Ktor é a ferramenta do tamanho certo.

## Minhas decisões de arquitetura

Eu escrevi o raciocínio completo em [`docs/spec.md`](./docs/spec.md) —
minha especificação no formato SDD (requisitos funcionais e
não-funcionais, fora de escopo, critérios de aceite) — e aqui no README
(por que Kotlin + Ktor, por que sem Kafka/DynamoDB). O histórico de como
TDD guiou cada história e por que a sequência de cenários do enunciado é
um teste sequencial e não um `@TestFactory` também está no `docs/spec.md`.

Resumindo a estrutura:

```
src/main/kotlin/com/ebanx/ledger/
├── domain/          # Account — regra de negócio pura, sem Ktor
├── application/     # um caso de uso por operação (SRP)
├── infrastructure/  # AccountRepository (a porta) + InMemoryAccountRepository (a única impl)
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

As regras de negócio que eu aplico pra `withdraw` e `transfer` (a segunda
reusa a primeira): a conta de origem precisa existir (`404` se não
existir) e precisa ter saldo suficiente (`400` se não tiver — essa
invariante vive em `Account.withdraw`, não é só validação de borda na
rota). Eu também rejeito valor de depósito/saque zero ou negativo com
`400` — sem essa checagem, um saque negativo passaria pela validação de
saldo e se comportaria como um depósito sem limite.

## Consistência sob concorrência

`AccountRepository.update(id, mutate)` lê, transforma e grava como uma
única operação atômica (via `ConcurrentHashMap.compute`) — não como um
`findById` seguido de um `save` separado, que deixaria uma janela pra duas
requisições concorrentes na mesma conta perderem uma atualização ou as
duas passarem um saque que deveria falhar. Isso tem um teste real provando
(200 threads depositando 1 ao mesmo tempo, saldo final tem que ser 200,
determinístico). Detalhes e o trade-off que assumo em transferência (cada
perna atômica, mas as duas pernas não são uma transação atômica única)
estão no [`docs/spec.md`](./docs/spec.md).

## CI

Configurei pra rodar `./gradlew test` no GitHub Actions a cada push/PR
(`.github/workflows/ci.yml`).
