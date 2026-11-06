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
não estão aqui — ver [`docs/spec.md`](./docs/spec.md).

## Atalhos (Makefile)

```bash
make run      # ./gradlew run
make test     # ./gradlew test
make build    # ./gradlew build
make expose   # sobe a aplicação, abre o túnel ngrok, roda o roteiro de
              # cenários contra a URL pública e imprime o resultado + a URL
```

## Como testar com o ipkiss da EBANX

O [ipkiss](https://ipkiss.ebanx.ninja/) é a ferramenta da EBANX pra testar
a API de fora, então preciso expor minha porta local (`8080`) pra
internet. Uso o [ngrok](https://ngrok.com/) pra isso. Passo a passo:

1. **Garanta o ngrok instalado e autenticado** (só uma vez por máquina):
   `brew install ngrok` no Mac, crie uma conta grátis em ngrok.com,
   copie o authtoken no dashboard e rode `ngrok config add-authtoken
   <SEU_TOKEN>`.
2. **Rode o botão único**: `make expose`. Ele sobe a aplicação, abre o
   túnel ngrok, roda a mesma sequência de cenários que o ipkiss verifica
   (reset, consulta de saldo, deposit, withdraw, transfer, valor
   inválido) contra a URL pública, e imprime o resultado seguido da URL.
   Deixe esse terminal aberto.
3. **Confira o resultado impresso.** Se todos os cenários vierem `OK`, a
   aplicação já foi validada pela internet real, não só localmente —
   pode confiar na URL impressa em seguida.
4. **Cole a URL no ipkiss**: copie a URL pública impressa (algo como
   `https://xxxx.ngrok-free.app`), abra o
   [ipkiss](https://ipkiss.ebanx.ninja/), cole lá e clique em "Run
   tests!". Esse clique é manual — o ipkiss não tem API documentada nem
   formulário sem proteção CSRF, então não dá pra automatizar com
   segurança.
5. **Encerre quando terminar**: volte no terminal do `make expose` e
   aperte `Ctrl+C`. Isso derruba a aplicação e o túnel ngrok juntos.

Pra rodar os passos manualmente, sem o atalho: `./gradlew run` (ou
`make run`), depois `ngrok http 8080` em outro terminal.

## CI

Configurei pra rodar `./gradlew test` no GitHub Actions a cada push/PR
(`.github/workflows/ci.yml`).
