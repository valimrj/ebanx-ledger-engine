# Especificação — EBANX Ledger Engine

> Documento vivo, no formato SDD (Spec-Driven Development): descreve o
> que o sistema precisa fazer antes de descrever como eu fiz. Decisões de
> implementação que não mudam o comportamento externo (como eu organizei
> os pacotes, por que escolhi Ktor) ficam no [`README.md`](../README.md).
> Este arquivo nasceu do meu `PLAN.md` original, reestruturado como spec.

## 1. Contexto

Este é a minha solução para o take-home assignment de "Ledger Engine" da
EBANX: uma API HTTP simples de saldo e eventos bancários (depósito,
saque, transferência), totalmente em memória, sem persistência real —
porque o enunciado diz explicitamente que durabilidade não é requisito.

## 2. Objetivo

Expor uma API que:
- registre eventos de depósito, saque e transferência entre contas;
- responda consultas de saldo sem nenhum efeito colateral;
- mantenha o estado internamente consistente mesmo sob concorrência;
- seja simples o bastante pra qualquer pessoa entender em poucos minutos.

## 3. Requisitos funcionais

Escrevo os cenários no formato Dado/Quando/Então porque é o que guiou
meus testes de integração (`EventScenariosTest`) — cada cenário abaixo
tem um teste correspondente.

### RF-01 — Resetar o estado
```
Dado qualquer estado atual do sistema
Quando eu chamo POST /reset
Então todas as contas são removidas
E a resposta é 200 OK
```

### RF-02 — Consultar saldo de conta inexistente
```
Dado que a conta "1234" nunca recebeu nenhum evento
Quando eu chamo GET /balance?account_id=1234
Então a resposta é 404
E o corpo é o texto "0"
```

### RF-03 — Depositar em conta nova
```
Dado que a conta "100" não existe
Quando eu chamo POST /event com {"type": "deposit", "destination": "100", "amount": 10}
Então a conta "100" é criada com saldo 10
E a resposta é 201 Created
E o corpo é {"destination": {"id": "100", "balance": 10}}
```

### RF-04 — Depositar em conta existente
```
Dado que a conta "100" tem saldo 10
Quando eu deposito 10 na conta "100"
Então o saldo da conta "100" passa a ser 20
E a resposta é 201 Created
```

### RF-05 — Consultar saldo de conta existente
```
Dado que a conta "100" tem saldo 20
Quando eu chamo GET /balance?account_id=100
Então a resposta é 200 OK
E o corpo é o texto "20"
E chamar essa consulta de novo não altera o saldo (GET não tem efeito colateral)
```

### RF-06 — Sacar de conta inexistente
```
Dado que a conta "200" não existe
Quando eu chamo POST /event com {"type": "withdraw", "origin": "200", "amount": 10}
Então a resposta é 404
E o corpo é o texto "0"
```

### RF-07 — Sacar de conta existente, com saldo suficiente
```
Dado que a conta "100" tem saldo 20
Quando eu saco 5 da conta "100"
Então o saldo da conta "100" passa a ser 15
E a resposta é 201 Created
```

### RF-08 — Sacar mais do que o saldo disponível
```
Dado que a conta "400" tem saldo 10
Quando eu tento sacar 11 da conta "400"
Então a resposta é 400
E o corpo é o texto "0"
E o saldo da conta "400" continua 10 (não houve alteração parcial)
```

### RF-09 — Transferir de uma conta existente pra uma conta nova ou existente
```
Dado que a conta "100" tem saldo 15
Quando eu transfiro 15 da conta "100" pra conta "300"
Então o saldo da conta "100" passa a ser 0
E o saldo da conta "300" passa a ser 15 (criada se não existia, somada se existia)
E a resposta é 201 Created
E o corpo tem origin e destination com os dois saldos finais
```

### RF-10 — Transferir de uma conta inexistente
```
Dado que a conta "200" não existe
Quando eu tento transferir dela pra qualquer outra conta
Então a resposta é 404
E o corpo é o texto "0"
E a conta de destino não é tocada
```

### RF-11 — Rejeitar valor zero ou negativo
```
Dado qualquer conta, existente ou não
Quando eu chamo POST /event com amount <= 0, pra deposit, withdraw ou transfer
Então a resposta é 400
E o corpo é o texto "0"
E nenhuma conta envolvida tem o saldo alterado
```
Esse requisito não está nos exemplos literais do enunciado, mas o
checklist de avaliação da EBANX cita "valores inválidos não tratados"
como um bug comum a evitar — então eu tratei como requisito funcional,
não como cenário extra.

## 4. Requisitos não-funcionais

- **RNF-01 — Consistência sob concorrência.** Duas requisições
  concorrentes na mesma conta nunca podem resultar em saldo incorreto
  (lost update) nem em duas operações que deveriam ser mutuamente
  exclusivas passando as duas (ex.: dois saques que juntos excedem o
  saldo). Validado por teste real de concorrência (200 threads).
- **RNF-02 — Sem efeito colateral em leitura.** `GET /balance` nunca
  altera estado, chamado uma vez ou cem vezes.
- **RNF-03 — Simplicidade.** Nenhuma dependência, padrão de design ou
  camada de abstração que o enunciado não peça. Ver `AGENTS.md` pros
  guardrails específicos.
- **RNF-04 — Separação de responsabilidades.** Lógica de negócio não
  pode depender de framework HTTP nem de uma implementação concreta de
  armazenamento — garantido por teste de arquitetura (`LayeringTest`),
  não só por convenção.
- **RNF-05 — Testabilidade.** Todo requisito funcional (RF-01 a RF-11)
  tem pelo menos um teste automatizado cobrindo o comportamento real
  (estado, não só o retorno da chamada).

## 5. Fora de escopo

Decisão consciente de não fazer, porque o enunciado não pede e eu evito
antecipar cenário fora da especificação:
- Persistência real (banco de dados) — tudo em memória.
- Mensageria assíncrona (Kafka ou similar) — não há consumidor nem
  desacoplamento de serviço nesse problema.
- Autenticação/autorização — o enunciado não define usuários nem
  permissões.
- Transação atômica cross-conta em `transfer` (ver detalhe no README,
  seção "Consistência sob concorrência") — cada perna é atômica
  isoladamente, a operação completa não é uma transação distribuída.
- Paginação, múltiplas moedas, juros, taxas — nenhum desses aparece no
  enunciado.

## 6. Decisões de arquitetura

Resumo; o raciocínio completo de cada uma está no
[`README.md`](../README.md):
- Kotlin + Ktor no lugar de Spring Boot (seção "Por que Ktor, e não
  Spring Boot" do README).
- Arquitetura hexagonal: `domain`/`application` no centro, `http`/
  `infrastructure` como adapters.
- `AccountRepository.update(id, mutate)` como única forma de escrever
  estado — ler, decidir e gravar como uma operação atômica.
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
2. **feat**: domínio — `Account` imutável, com `deposit`/`withdraw`.
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
19. **docs**: este documento — PLAN.md reestruturado como spec SDD.
