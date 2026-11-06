# Especificação - EBANX Ledger Engine

> Documento vivo, no formato SDD (Spec-Driven Development): descreve o
> que o sistema precisa fazer (requisitos funcionais e não-funcionais,
> endpoints, consistência sob concorrência) e, na seção 6, por que fiz
> as escolhas de arquitetura que fiz. Regra de comportamento pra quem
> trabalha no repo está no [`AGENTS.md`](../AGENTS.md); instruções de
> build/run estão no [`README.md`](../README.md). Este arquivo nasceu
> do meu `PLAN.md` original, reestruturado como spec.

## 1. Contexto

Este é a minha solução para o take-home assignment de "Ledger Engine" da
EBANX: uma API HTTP simples de saldo e eventos bancários (depósito,
saque, transferência), totalmente em memória, sem persistência real -
porque o enunciado diz explicitamente que durabilidade não é requisito.

## 2. Objetivo

Expor uma API que:
- registre eventos de depósito, saque e transferência entre contas;
- responda consultas de saldo sem nenhum efeito colateral;
- mantenha o estado internamente consistente mesmo sob concorrência;
- seja simples o bastante pra qualquer pessoa entender em poucos minutos.

## 3. Requisitos funcionais

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reset` | Limpa todo o estado |
| `GET` | `/balance?account_id=:id` | Saldo da conta (texto plano); `404 0` se não existir |
| `POST` | `/event` | `{"type": "deposit"\|"withdraw"\|"transfer", ...}` |

Escrevo os cenários no formato Dado/Quando/Então porque é o que guiou
meus testes de integração (`EventScenariosTest`) - cada cenário abaixo
tem um teste correspondente.

### RF-01 - Resetar o estado
```
Dado qualquer estado atual do sistema
Quando eu chamo POST /reset
Então todas as contas são removidas
E a resposta é 200 OK
```

### RF-02 - Consultar saldo de conta inexistente
```
Dado que a conta "1234" nunca recebeu nenhum evento
Quando eu chamo GET /balance?account_id=1234
Então a resposta é 404
E o corpo é o texto "0"
```

### RF-03 - Depositar em conta nova
```
Dado que a conta "100" não existe
Quando eu chamo POST /event com {"type": "deposit", "destination": "100", "amount": 10}
Então a conta "100" é criada com saldo 10
E a resposta é 201 Created
E o corpo é {"destination": {"id": "100", "balance": 10}}
```

### RF-04 - Depositar em conta existente
```
Dado que a conta "100" tem saldo 10
Quando eu deposito 10 na conta "100"
Então o saldo da conta "100" passa a ser 20
E a resposta é 201 Created
```

### RF-05 - Consultar saldo de conta existente
```
Dado que a conta "100" tem saldo 20
Quando eu chamo GET /balance?account_id=100
Então a resposta é 200 OK
E o corpo é o texto "20"
E chamar essa consulta de novo não altera o saldo (GET não tem efeito colateral)
```

### RF-06 - Sacar de conta inexistente
```
Dado que a conta "200" não existe
Quando eu chamo POST /event com {"type": "withdraw", "origin": "200", "amount": 10}
Então a resposta é 404
E o corpo é o texto "0"
```

### RF-07 - Sacar de conta existente, com saldo suficiente
```
Dado que a conta "100" tem saldo 20
Quando eu saco 5 da conta "100"
Então o saldo da conta "100" passa a ser 15
E a resposta é 201 Created
```

### RF-08 - Sacar mais do que o saldo disponível
```
Dado que a conta "400" tem saldo 10
Quando eu tento sacar 11 da conta "400"
Então a resposta é 400
E o corpo é o texto "0"
E o saldo da conta "400" continua 10 (não houve alteração parcial)
```
RF-06/07/08 valem igual pra `transfer` (que reusa o saque na conta de
origem). A invariante de saldo suficiente vive em `Account.withdraw`,
nunca como validação de borda na rota - é regra de domínio, não
checagem de HTTP.

### RF-09 - Transferir de uma conta existente pra uma conta nova ou existente
```
Dado que a conta "100" tem saldo 15
Quando eu transfiro 15 da conta "100" pra conta "300"
Então o saldo da conta "100" passa a ser 0
E o saldo da conta "300" passa a ser 15 (criada se não existia, somada se existia)
E a resposta é 201 Created
E o corpo tem origin e destination com os dois saldos finais
```

### RF-10 - Transferir de uma conta inexistente
```
Dado que a conta "200" não existe
Quando eu tento transferir dela pra qualquer outra conta
Então a resposta é 404
E o corpo é o texto "0"
E a conta de destino não é tocada
```

### RF-11 - Rejeitar valor zero ou negativo
```
Dado qualquer conta, existente ou não
Quando eu chamo POST /event com amount <= 0, pra deposit, withdraw ou transfer
Então a resposta é 400
E o corpo é o texto "0"
E nenhuma conta envolvida tem o saldo alterado
```
Esse requisito não está nos exemplos literais do enunciado, mas o
checklist de avaliação da EBANX cita "valores inválidos não tratados"
como um bug comum a evitar - então eu tratei como requisito funcional,
não como cenário extra. Sem essa checagem, um saque negativo passaria
pela validação de saldo (`amount > balance` é falso pra um valor
negativo) e se comportaria como um depósito sem limite.

## 4. Requisitos não-funcionais

- **RNF-01 - Consistência sob concorrência.** Duas requisições
  concorrentes na mesma conta nunca podem resultar em saldo incorreto
  (lost update) nem em duas operações que deveriam ser mutuamente
  exclusivas passando as duas (ex.: dois saques que juntos excedem o
  saldo). Garantido por `AccountRepository.update(id, mutate)`, que lê,
  transforma e grava como uma única operação atômica (via
  `ConcurrentHashMap.compute`) - nunca como um `findById` seguido de um
  `save` separado, que deixaria essa janela aberta. Validado por teste
  real de concorrência (200 threads depositando 1 ao mesmo tempo; saldo
  final tem que ser 200, determinístico). Trade-off assumido em
  `transfer`: cada perna (`withdraw` + `deposit`) é atômica
  isoladamente, mas as duas pernas juntas não formam uma transação
  atômica única (ver seção 5, "Fora de escopo").
- **RNF-02 - Sem efeito colateral em leitura.** `GET /balance` nunca
  altera estado, chamado uma vez ou cem vezes.
- **RNF-03 - Simplicidade.** Nenhuma dependência, padrão de design ou
  camada de abstração que o enunciado não peça. Ver `AGENTS.md` pros
  guardrails específicos.
- **RNF-04 - Separação de responsabilidades.** Lógica de negócio não
  pode depender de framework HTTP nem de uma implementação concreta de
  armazenamento - garantido por teste de arquitetura (`LayeringTest`),
  não só por convenção.
- **RNF-05 - Testabilidade.** Todo requisito funcional (RF-01 a RF-11)
  tem pelo menos um teste automatizado cobrindo o comportamento real
  (estado, não só o retorno da chamada).

## 5. Fora de escopo

Decisão consciente de não fazer, porque o enunciado não pede e eu evito
antecipar cenário fora da especificação:
- Persistência real (banco de dados) - tudo em memória.
- Mensageria assíncrona (Kafka ou similar) - não há consumidor nem
  desacoplamento de serviço nesse problema.
- Autenticação/autorização - o enunciado não define usuários nem
  permissões.
- Transação atômica cross-conta em `transfer` (ver RNF-01) - cada
  perna é atômica isoladamente, a operação completa não é uma
  transação distribuída.
- Paginação, múltiplas moedas, juros, taxas - nenhum desses aparece no
  enunciado.

## 6. Decisões de arquitetura

### 6.1 Por que Kotlin + Ktor, e não Spring Boot

Escolhi Ktor no lugar do Spring Boot de propósito, não por
familiaridade:

- **Kotlin de verdade, não Kotlin por cima.** Ktor foi escrito do zero
  em Kotlin, então usa DSLs, extension functions e coroutines de forma
  nativa - o código de rotas fica curto e legível (`routing { post("/event")
  { ... } }` lê como Kotlin puro, não como uma classe anotada).
- **Assíncrono por padrão, sem a cerimônia.** As coroutines dispensam
  callbacks e reactive streams: o código tem cara de síncrono, mas não
  bloqueia threads - útil com muitas conexões simultâneas, sem precisar
  aprender uma API reativa.
- **Leve e modular.** Só instalo o que realmente uso -
  `ContentNegotiation` pra JSON e `StatusPages` pro mapeamento de erro
  (ver `Application.kt` e `Routing.kt`) - como plugins, não um framework
  que liga tudo por padrão.
- **Pouca "mágica".** Não depende de reflection nem de anotações em
  excesso, então o fluxo da aplicação é explícito e fácil de seguir e
  depurar. A rota que expõe `POST /event` é uma função de 3 linhas: ler
  a requisição, chamar o `EventDispatcher`, responder. Um equivalente em
  Spring (`@RestController` + `@PostMapping`) não é mais simples aqui -
  ele troca essas 3 linhas por anotações processadas via reflection, um
  container de DI e um ciclo de vida de bean que eu não preciso pra 3
  rotas.
- **Flexibilidade de engine.** Rodo em Netty, mas o Ktor também suporta
  CIO, Jetty ou Tomcat dependendo da necessidade do projeto.
- **Testes simples.** O `testApplication` sobe a aplicação em memória,
  sem precisar de um servidor real - é exatamente isso que
  `EventScenariosTest` e `ApplicationTest` usam pra exercitar as rotas
  HTTP reais.
- **Bate direto com o critério "simplicidade" do enunciado.** O
  checklist da EBANX pede "evite over-engineering" e diz "não é
  necessário aplicar padrões de design complexos". Um container de DI,
  injeção via `@Autowired`, Spring Data que eu nem usaria (tudo aqui é
  em memória) - é ferramenta pesada pra um problema pequeno.

Quando vale a pena o Ktor: microsserviços, APIs enxutas, projetos Kotlin
Multiplatform, ou quando baixo overhead e controle explícito importam
mais do que um framework "tudo incluso". Quando talvez não seja a
melhor opção: projetos grandes que dependem muito do ecossistema Spring
(Spring Data, Security, Batch, integrações prontas), ou times que já
dominam Spring Boot - nesses casos, o Spring ainda entrega mais coisa
pronta, e o ganho do Ktor pode não compensar a troca. O Ktor também
exige montar mais coisa por conta própria, como injeção de dependência
(geralmente com Koin) e estrutura de projeto - aqui mantive a DI manual
(wiring direto por construtor em `Routing.kt`), já que o grafo de
objetos é pequeno o suficiente pra não precisar de um framework pra
isso.

Isso não quer dizer que Spring é ruim - pra uma API maior, com vários
módulos, Spring Data/Security/Cloud compensam o peso. Pra esse
take-home, Ktor é a ferramenta do tamanho certo.

### 6.2 Separação em camadas

Uso "hexagonal" como rótulo aproximado do que fiz, não como
implementação completa: não tem uma porta (interface) separada por
caso de uso, nem uma pasta `adapters`. O enunciado pede simplicidade, e
ir atrás da forma "correta" de hexagonal (ou de Clean Architecture, que
tem o mesmo princípio de isolar o domínio por trás de uma inversão de
dependência, só que com mais camadas nomeadas - `entities`, `use-cases`,
`interface-adapters`) custaria mais indireção do que esse problema
justifica. O que de fato aplico, das duas, é só a regra de dependência:
`domain`/`application` não podem importar `io.ktor` nem a implementação
concreta do repositório - e essa regra, ao contrário do nome
"hexagonal" em si, é verificada por teste (`LayeringTest`), não só
documentada.

Estrutura:

```
src/main/kotlin/com/ebanx/ledger/
├── domain/          # Account - regra de negócio pura, sem Ktor
├── application/     # um caso de uso por operação (SRP)
├── infrastructure/  # AccountRepository (a porta) + InMemoryAccountRepository (a única impl)
├── exceptions/      # exceptions de domínio e de orquestração, num pacote só
└── http/            # DTOs, EventDispatcher, rotas Ktor
```

Juntei a porta `AccountRepository` no mesmo pacote da sua única
implementação, em vez de um pacote `ports` separado só pra uma
interface - uma pasta a menos. `domain`/`application` podem importar a
interface, mas nunca a implementação concreta (`InMemoryAccountRepository`)
nem nada de `http`/Ktor - essa regra mais fina é o que o `LayeringTest`
verifica automaticamente.

### 6.3 Outras decisões

- `AccountRepository.update(id, mutate)` como única forma de escrever
  estado - ver RNF-01 pro detalhe e pro teste que comprova isso.
- Testes de arquitetura (Konsist) em vez de só disciplina de equipe.

## 7. Critérios de aceite / Definition of Done

Um requisito funcional só é considerado pronto quando:
1. Existe um teste automatizado que falha antes da implementação (red)
   e passa depois (green).
2. `./gradlew test` completo continua verde, incluindo os testes de
   arquitetura.
3. O comportamento bate exatamente com o cenário Dado/Quando/Então
   descrito na seção 3 (status code E corpo da resposta, quando
   aplicável).
4. Nenhum requisito fora de escopo (seção 5) foi implementado de
   carona.

## 8. Glossário

| Termo | Significado |
|---|---|
| Conta | Identificada por uma string (`account_id`); criada implicitamente no primeiro depósito. |
| Evento | Uma operação de `deposit`, `withdraw` ou `transfer`, recebida via `POST /event`. |
| Saldo | Inteiro não-negativo associado a uma conta. |
| Origem (`origin`) | Conta de onde o valor sai, em `withdraw` e `transfer`. |
| Destino (`destination`) | Conta pra onde o valor vai, em `deposit` e `transfer`. |

## 9. Histórico de implementação

Cada item abaixo foi um commit pequeno e testável, nessa ordem:

1. **chore**: esqueleto Kotlin/Ktor + arquitetura hexagonal + CI + teste de arquitetura.
2. **feat**: domínio - `Account` imutável, com `deposit`/`withdraw`.
3. **feat**: `AccountRepository` (porta) + `InMemoryAccountRepository`.
4. **feat**: caso de uso `DepositUseCase`.
5. **feat**: caso de uso `WithdrawUseCase`.
6. **feat**: caso de uso `TransferUseCase` (composição de withdraw+deposit).
7. **feat**: casos de uso `ResetUseCase` e `GetBalanceUseCase`.
8. **feat**: rota HTTP `POST /event`.
9. **feat**: rota HTTP `GET /balance`.
10. **feat**: rota HTTP `POST /reset`.
11. **test**: suíte reproduzindo todos os cenários do enunciado (RF-03 a RF-10).
12. **docs**: README com instruções de build/run e decisões de arquitetura.
13. **refactor**: junto a porta `AccountRepository` dentro de `infrastructure`.
14. **feat**: `InsufficientFundsException` no domínio (RF-08).
15. **refactor**: removo o plugin `io.ktor.plugin`, sem uso real.
16. **fix**: `InvalidAmountException` pra valor zero/negativo (RF-11).
17. **docs**: traduzo código e comentários pra inglês; README continua em português.
18. **docs**: explico a escolha de Ktor no README.
19. **docs**: este documento - PLAN.md reestruturado como spec SDD.
