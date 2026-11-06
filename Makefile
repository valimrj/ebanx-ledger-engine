.PHONY: run test build expose clean

run:
	./gradlew run

test:
	./gradlew test

build:
	./gradlew build

# Sobe a aplicação, abre um túnel ngrok em :8080 e imprime a URL pública
# pronta pra colar em https://ipkiss.ebanx.ninja/. Ver README.
expose:
	@./scripts/expose.sh

clean:
	./gradlew clean
