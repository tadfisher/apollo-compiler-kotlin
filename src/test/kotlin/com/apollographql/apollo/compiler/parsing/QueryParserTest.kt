package com.apollographql.apollo.compiler.parsing

import org.junit.Test

class QueryParserTest {
    @Test
    fun `parses query`() {
        val parser = """
            query HeroQuery {
              hero {
                id
                name
              }
            }
        """.trimIndent().graphqlParser()

        val ast = parser.document().toAst()
        print(ast)
    }
}