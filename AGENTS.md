# AGENTS.md — EBANX Ledger Engine

Este arquivo é a constituição do projeto: regras que valem pra qualquer
agente (humano ou IA) que trabalhe neste repositório. Se um pedido da
pessoa conflitar com algo aqui, o pedido explícito dela vence — mas
avise antes de agir, dizendo contra qual regra está indo e por quê.

## 1. Missão do projeto

Resolver o take-home assignment "Ledger Engine" da EBANX: uma API de
saldo e eventos bancários (deposit, withdraw, transfer), em memória, com
lógica de negócio real, testada e simples. O documento de referência pro
que "pronto" significa é [`docs/spec.md`](./docs/spec.md); o README
explica como rodar e as decisões técnicas.

## 2. Princípios inegociáveis

1. **Simplicidade antes de sofisticação.** O enunciado da EBANX avalia
   explicitamente a capacidade de resolver bem sem complicar. Entre duas
   soluções que atendem ao requisito, escolha a mais simples, sempre.
2. **TDD.** Não escreva nenhuma linha de implementação sem um teste que
   falhou antes dela (red → green → refactor). Pular isso é violar a
   constituição, não só um estilo.
3. **Arquitetura hexagonal, mas sem rigidez de manual.** `domain` e
   `application` nunca importam `io.ktor` nem a implementação concreta
   de repositório — isso é garantido por teste (`LayeringTest`), não só
   por convenção. Fora essa regra, não crie camada nova sem necessidade
   comprovada.
4. **SRP de verdade.** Um caso de uso resolve uma operação. Nenhuma
   classe "Service" genérica fazendo várias coisas.
5. **Primeira pessoa na documentação de autoria e nos commits.** README,
   `docs/spec.md` e mensagens de commit devem ser escritos na primeira
   pessoa de quem fez o trabalho — porque descrevem trabalho feito. Este
   arquivo é a exceção: é instrução pra quem vem depois, então fala em
   modo diretivo ("faça", "não faça"), não em primeira pessoa.
6. **Idioma**: documentação (`README.md`, `docs/`, `AGENTS.md`) em
   português. Código, comentários no código, KDoc e nomes de teste em
   inglês — mantenha o código acessível a qualquer revisor, independente
   do idioma, e a documentação de decisão na língua mais natural pra
   explicar o raciocínio.

## 3. Guardrails — o que NUNCA fazer aqui

- **Não antecipe cenário fora do enunciado.** Se o enunciado ou o
  checklist de avaliação não cobre um caso, não implemente validação ou
  comportamento especulativo pra ele. (Já foi revertido uma vez neste
  projeto: `MissingEventFieldException`/`UnknownEventTypeException`
  foram removidas por serem especulativas e sem teste.)
- **Não adicione dependência sem necessidade comprovada.** Kafka e
  DynamoDB foram descartados conscientemente — o enunciado diz que
  durabilidade não é requisito. Qualquer plugin/biblioteca nova precisa
  justificar seu peso (o `io.ktor.plugin` já foi removido por trazer
  tarefas de build — fat jar, imagem Docker — nunca usadas).
- **Não separe `findById` + `save` pra estado mutável.** Toda escrita de
  conta deve passar por `AccountRepository.update(id, mutate)`, que é
  atômica. Separar leitura e escrita em dois passos reintroduz lost
  update — já aconteceu uma vez nesse projeto e foi corrigido.
- **Não deixe GET alterar estado.** Nenhuma rota de consulta pode, em
  nenhuma circunstância, escrever no repositório. Isso tem teste
  dedicado (`GET balance repeatedly never changes the balance`).
- **Não misture lógica de negócio com a camada HTTP.** Um handler Ktor
  só lê a requisição e delega pro caso de uso certo. Validação de regra
  de negócio (saldo insuficiente, valor inválido) vive no domínio, não
  na rota.
- **Não comite sem rodar `./gradlew test` antes e ver `BUILD
  SUCCESSFUL`.** Nenhum commit principal pode deixar o projeto num
  estado que não compila ou com teste quebrado.
- **Não use `--amend`, `push --force`, nem pule hooks** a menos que a
  pessoa peça explicitamente.
- **Não trate "a IA sugeriu" como autorização.** Toda decisão de
  arquitetura relevante (stack, trocar Ktor por outro framework,
  Kafka/DynamoDB, deploy) precisa de confirmação explícita da pessoa
  antes de ser implementada, mesmo com uma recomendação forte.

## 4. Padrão de mercado a seguir

- **Commits no estilo Conventional Commits**: `feat`, `fix`, `refactor`,
  `test`, `docs`, `chore`, seguidos de uma descrição curta — na primeira
  pessoa de quem fez a mudança, como já é a convenção deste repo.
- **Uma história = um commit pequeno e testável**, nunca um commit
  gigante misturando várias mudanças não relacionadas.
- **Testes de arquitetura (Konsist) como guarda-trilho automatizado**,
  não só como documentação de intenção — é o padrão de mercado pra
  projetos que levam arquitetura hexagonal/limpa a sério.
- **CI obrigatório**: todo push/PR deve rodar a suíte completa de testes
  (`.github/workflows/ci.yml`). Nenhuma mudança é considerada "pronta"
  sem o CI verde.
- **Nomenclatura de exceção de domínio**: exceções que representam
  regra de negócio (`InsufficientFundsException`, `InvalidAmountException`)
  vivem em `domain`; exceções de orquestração/lookup
  (`AccountNotFoundException`) vivem em `application`. Mantenha essa
  distinção em qualquer exceção nova.

## 5. O que fazer antes de qualquer mudança nova

1. Leia [`docs/spec.md`](./docs/spec.md) pra ver se o comportamento já
   está especificado. Se não estiver, pergunte antes de implementar —
   não assuma.
2. Escreva o teste que falha primeiro.
3. Implemente o mínimo pra passar.
4. Rode `./gradlew test` completo.
5. Atualize `docs/spec.md` (se mudou requisito) ou `README.md` (se
   mudou decisão técnica) no mesmo commit da mudança de código, nunca
   depois.
6. Comite pequeno, estilo Conventional Commits, na primeira pessoa de
   quem fez a mudança.

## 6. Quando algo aqui conflitar com um pedido direto

Siga o pedido direto da pessoa, mas avise antes: "isso vai contra a
regra X do AGENTS.md porque Y — quer que eu siga mesmo assim?". Nunca
quebre um guardrail silenciosamente.
