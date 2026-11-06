package com.ebanx.ledger.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

class LayeringTest {

    @Test
    fun `domain must not depend on http nor on a concrete repository implementation`() {
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
    fun `application must not depend on http nor on a concrete repository implementation`() {
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
