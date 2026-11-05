package com.ebanx.ledger.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * Testes de arquitetura (história 1, guarda-trilho para todas as próximas):
 * eu garanto aqui que a arquitetura hexagonal não degrada enquanto o código
 * cresce.
 *
 * Regra central: domain e application não podem depender de infrastructure
 * nem de http. A dependência só pode ir "de fora pra dentro" — http e
 * infrastructure conhecem domain/application; o inverso, nunca.
 */
class LayeringTest {

    @Test
    fun `domain nao pode depender de http nem de infrastructure`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("com.ebanx.ledger.domain..")
            .assertFalse {
                it.hasImport { import ->
                    import.name.startsWith("com.ebanx.ledger.http") ||
                        import.name.startsWith("com.ebanx.ledger.infrastructure") ||
                        import.name.startsWith("io.ktor")
                }
            }
    }

    @Test
    fun `application nao pode depender de http nem de infrastructure`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("com.ebanx.ledger.application..")
            .assertFalse {
                it.hasImport { import ->
                    import.name.startsWith("com.ebanx.ledger.http") ||
                        import.name.startsWith("com.ebanx.ledger.infrastructure") ||
                        import.name.startsWith("io.ktor")
                }
            }
    }

    @Test
    fun `ports nao pode depender de infrastructure nem de http`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("com.ebanx.ledger.ports..")
            .assertFalse {
                it.hasImport { import ->
                    import.name.startsWith("com.ebanx.ledger.http") ||
                        import.name.startsWith("com.ebanx.ledger.infrastructure") ||
                        import.name.startsWith("io.ktor")
                }
            }
    }
}
