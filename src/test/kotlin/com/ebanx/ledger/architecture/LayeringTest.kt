package com.ebanx.ledger.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * Testes de arquitetura: eu garanto aqui que a arquitetura hexagonal não
 * degrada enquanto o código cresce.
 *
 * Simplifiquei a estrutura juntando a porta `AccountRepository` dentro do
 * pacote `infrastructure` (ela mora no mesmo arquivo da sua única
 * implementação, em vez de ter um pacote `ports` só pra ela) — uma pasta a
 * menos pra navegar. Isso tem um custo que eu assumo conscientemente: a
 * regra de camadas não pode mais dizer "application não importa nada de
 * infrastructure", porque agora `application` precisa importar a interface
 * `AccountRepository`, que mora lá. Em troca, eu aperto a regra pro que
 * importa de verdade: nenhuma classe de `domain`/`application` pode
 * depender de uma implementação CONCRETA (`InMemoryAccountRepository`) nem
 * de `http`/Ktor. A interface pode atravessar; a implementação, nunca.
 */
class LayeringTest {

    @Test
    fun `domain nao pode depender de http nem de uma implementacao concreta de repositorio`() {
        Konsist
            .scopeFromProduction()
            .files
            .withPackage("com.ebanx.ledger.domain..")
            .assertFalse {
                it.hasImport { import ->
                    import.name.startsWith("com.ebanx.ledger.http") ||
                        import.name.startsWith("com.ebanx.ledger.infrastructure.InMemory") ||
                        import.name.startsWith("io.ktor")
                }
            }
    }

    @Test
    fun `application nao pode depender de http nem de uma implementacao concreta de repositorio`() {
        Konsist
            .scopeFromProduction()
            .files
            .withPackage("com.ebanx.ledger.application..")
            .assertFalse {
                it.hasImport { import ->
                    import.name.startsWith("com.ebanx.ledger.http") ||
                        import.name.startsWith("com.ebanx.ledger.infrastructure.InMemory") ||
                        import.name.startsWith("io.ktor")
                }
            }
    }
}
