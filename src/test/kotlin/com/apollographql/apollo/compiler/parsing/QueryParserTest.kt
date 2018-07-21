package com.apollographql.apollo.compiler.parsing

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.antlr.v4.runtime.CharStreams
import org.junit.Test

class QueryParserTest {
    @Test
    fun `parses query`() {
        val parser = """
            {
              hero {
                id
                name
              }
            }
        """.trimIndent().queryParser()

        with (parser) {
            assertThat(document()).isNotNull()
            document().definition().forEach { ctx ->
                ctx.operationDefinition()
            }
        }
    }
}