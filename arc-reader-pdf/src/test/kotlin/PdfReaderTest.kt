// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.extensions.ReadPdfException
import org.eclipse.lmos.arc.agents.dsl.extensions.pdf
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KClass

class PdfReaderTest {

    @Test
    fun `test reading local pdf file`() {
        val result = dslContext().pdf(File("src/test/resources/test.pdf").toURI().toString()).getOrThrow()
        assertThat(result.trim()).isEqualTo(
            """Page Not Found
We could not find what you were looking for.
Please contact the owner of the site that linked you to the original URL and let them know their
link is broken.""",
        )
    }

    @Test
    fun `test reading error`() {
        val result = dslContext().pdf(File("src/test/resources/missing.pdf").toURI().toString())
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(ReadPdfException::class.java)
    }

    private fun dslContext() = BasicDSLContext(object : BeanProvider {
        override suspend fun <T : Any> provide(bean: KClass<T>): T {
            error("err")
        }
    })
}
