# AGENTS.md — EBANX Ledger Engine

Este arquivo é a constituição do projeto: regras que valem pra mim e pra
qualquer agente (humano ou IA) que trabalhe neste repositório depois de
mim. Se um pedido conflitar com algo aqui, o pedido explícito da pessoa
vence — mas ela deve saber que está indo contra uma regra que eu
escrevi de propósito, e eu devo dizer isso antes de agir.

## 1. Missão do projeto

Resolver o take-home assignment "Ledger Engine" da EBANX: uma API de
saldo e eventos bancários (deposit, withdraw, transfer), em memória, com
lógica de negócio real, testada e simples. O documento de referência pro
que "pronto" significa é [`docs/spec.md`](./docs/spec.md); o README
explica como rodar e minhas decisões técnicas.

## 2. Princípios inegociáveis

1. **Simplicidade antes de sofisticação.** O enunciado da EBANX avalia
   explicitamente a capacidade de resolver bem sem complicar. Entre duas
   soluções que atendem ao requisito, eu escolho a mais simples, sempre.
2. **TDD.** Nenhuma linha de implementação sem um teste que falhou antes
   dela (red → green → refactor). Se eu pular isso, estou violando a
   constituição, não só um estilo.
3. **Arquitetura hexagonal, mas sem rigidez de manual.** `domain` e
   `application` nunca importam `io.ktor` nem a implementação concreta
   de repositório — isso é garantido por teste (`LayeringTest`), não só
   por convenção. Fora essa regra, eu não crio camada nova sem
   necessidade comprovada.
4. **SRP de verdade.** Um caso de uso resolve uma operação. Nenhuma
   classe "Service" genérica fazendo várias coisas.
5. **Primeira pessoa na documentação e nos commits.** README, docs/,
   PLAN/spec e mensagens de commit são escritos como se eu mesmo tivesse
   feito o trabalho — porque fiz.
6. **Idioma**: documentação (`README.md`, `docs/`, `AGENTS.md`) em
   português. Código, comentários no código, KDoc e nomes de teste em
   inglês — essa foi uma decisão explícita de manter o código acessível
   a qualquer revisor, independente do idioma, e a documentação de
   decisão na língua em que eu penso e explico melhor.

## 3. Guardrails — o que eu NUNCA faço aqui

- **Não antecipo cenário fora do enunciado.** Se o enunciado ou o
  checklist de avaliação não cobre um caso, eu não implemento validação
  ou comportamento especulativo pra ele. (Já reverti isso uma vez:
  removi `MissingEventFieldException`/`UnknownEventTypeException` que
  eram especulativas e sem teste.)
- **Não adiciono dependência sem necessidade comprovada.** Kafka e
  DynamoDB foram descartados conscientemente — o enunciado diz que
  durabilidade não é requisito. Qualquer plugin/biblioteca nova precisa
  justificar seu peso (já removi o `io.ktor.plugin` por trazer tarefas
  de build — fat jar, imagem Docker — que eu nunca uso).
- **Não faço `findById` + `save` separados pra estado mutável.** Toda
  escrita de conta passa por `AccountRepository.update(id, mutate)`, que
  é atômica. Separar leitura e escrita em dois passos reintroduz lost
  update — já aconteceu uma vez nesse projeto e eu corrigi.
- **Não deixo GET alterar estado.** Nenhuma rota de consulta pode, em
  nenhuma circunstância, escrever no repositório. Isso tem teste
  dedicado (`GET balance repeatedly never changes the balance`).
- **Não misturo lógica de negócio com a camada HTTP.** Um handler Ktor
  só lê a requisição e delega pro caso de uso certo. Validação de regra
  de negócio (saldo insuficiente, valor inválido) vive no domínio, não
  na rota.
- **Não comito sem rodar `./gradlew test` antes e ver `BUILD
  SUCCESSFUL`.** Nenhum commit principal pode deixar o projeto num
  estado que não compila ou com teste quebrado.
- **Não uso `--amend`, `push --force`, nem pulo hooks** a menos que a
  pessoa peça explicitamente.
- **Não trato "a IA sugeriu" como autorização.** Toda decisão de
  arquitetura relevante (stack, trocar Ktor por outro framework,
  Kafka/DynamoDB, deploy) passa por confirmação explícita da pessoa
  antes de eu implementar, mesmo que eu tenha uma recomendação forte.

## 4. Padrão de mercado que eu sigo

- **Commits no estilo Conventional Commits**: `feat`, `fix`, `refactor`,
  `test`, `docs`, `chore`, seguidos de uma descrição curta no imperativo
  (ou na minha primeira pessoa, como já é a convenção deste repo).
- **Uma história = um commit pequeno e testável**, nunca um commit
  gigante misturando várias mudanças não relacionadas.
- **Testes de arquitetura (Konsist) como guarda-trilho automatizado**,
  não só como documentação de intenção — é o padrão de mercado pra
  projetos que levam arquitetura hexagonal/limpa a sério.
- **CI obrigatório**: todo push/PR roda a suíte completa de testes
  (`.github/workflows/ci.yml`). Nenhuma mudança é considerada "pronta"
  sem o CI verde.
- **Nomenclatura de exceção de domínio**: exceções que representam
  regra de negócio (`InsufficientFundsException`, `InvalidAmountException`)
  vivem em `domain`; exceções de orquestração/lookup
  (`AccountNotFoundException`) vivem em `application`. Essa distinção é
  deliberada e deve ser mantida em qualquer exceção nova.

## 5. O que fazer antes de qualquer mudança nova

1. Ler [`docs/spec.md`](./docs/spec.md) pra ver se o comportamento já
   está especificado. Se não estiver, perguntar antes de implementar —
   não assumir.
2. Escrever o teste que falha primeiro.
3. Implementar o mínimo pra passar.
4. Rodar `./gradlew test` completo.
5. Atualizar `docs/spec.md` (se mudou requisito) ou `README.md` (se
   mudou decisão técnica) no mesmo commit da mudança de código, nunca
   depois.
6. Commitar pequeno, em primeira pessoa, estilo Conventional Commits.

## 6. Quando algo aqui conflitar com um pedido direto

Eu sigo o pedido direto da pessoa, mas aviso antes: "isso vai contra a
regra X do AGENTS.md porque Y — quer que eu siga mesmo assim?". Eu nunca
quebro um guardrail silenciosamente.
