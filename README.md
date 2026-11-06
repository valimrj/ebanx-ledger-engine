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
internet. Uso o [ngrok](https://ngrok.com/) pra isso:

1. Suba a aplicação: `./gradlew run` (ou `make run`).
2. Instale o ngrok (`brew install ngrok` no Mac), crie uma conta grátis
   em ngrok.com, copie seu authtoken no dashboard e rode uma vez:
   `ngrok config add-authtoken <SEU_TOKEN>`.
3. Com a aplicação no ar, em outro terminal: `ngrok http 8080`. O ngrok
   imprime uma URL pública (`https://....ngrok-free.app`) que redireciona
   pra `localhost:8080`. Deixe esse terminal aberto — fechar encerra o
   túnel.
4. Teste a URL antes de usar no ipkiss: `curl <URL>/balance?account_id=1`
   deve responder `404` com corpo `0`.
5. Cole essa URL no [ipkiss](https://ipkiss.ebanx.ninja/) e rode o teste.
   Se ele reaproveita estado entre execuções, chamo `POST /reset` antes
   pra garantir estado limpo.

`make expose` faz os passos 1, 3 e 4 por mim: sobe a aplicação, abre o
túnel, roda contra a URL pública a mesma sequência de cenários que o
ipkiss verifica (reset, consulta de saldo, deposit, withdraw, transfer,
valor inválido) e só então imprime o resultado e a URL — só preciso ter
feito o passo 2 (instalar/autenticar o ngrok) uma vez antes. O passo 5
(colar a URL no ipkiss e clicar em "Run tests!") continua manual: o
ipkiss não tem API documentada nem formulário sem proteção CSRF, então
não dá pra automatizar esse clique de forma confiável. `Ctrl+C` encerra
a aplicação e o túnel.

## CI

Configurei pra rodar `./gradlew test` no GitHub Actions a cada push/PR
(`.github/workflows/ci.yml`).
