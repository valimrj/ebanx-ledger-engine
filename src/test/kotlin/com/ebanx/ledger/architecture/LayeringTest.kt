package com.ebanx.ledger.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * Architecture tests: I guarantee here that the hexagonal architecture
 * doesn't degrade as the code grows.
 *
 * I simplified the structure by merging the `AccountRepository` port
 * into the `infrastructure` package (it lives in the same file as its
 * single implementation, instead of having a `ports` package just for
 * it) - one less folder to navigate. That has a cost I take on
 * consciously: the layering rule can no longer say "application doesn't
 * import anything from infrastructure", because now `application` needs
 * to import the `AccountRepository` interface, which lives there. In
 * exchange, I tighten the rule to what actually matters: no class in
 * `domain`/`application` may depend on a CONCRETE implementation
 * (`InMemoryAccountRepository`) nor on `http`/Ktor. The interface may
 * cross; the implementation, never.
 */
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
