package com.apollographql.apollo.compiler.parsing

import com.apollographql.apollo.compiler.ast.AstLocation
import com.apollographql.apollo.compiler.ast.Document
import com.apollographql.apollo.compiler.ast.Field
import com.apollographql.apollo.compiler.ast.FloatValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.OperationDefinition
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.StringValue
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class ParserTest {
    @Test
    fun `parses query`() {
        val ast = """
            query HeroQuery {
              hero {
                id
                name
              }
            }
        """.trimIndent().parseGraphqlDocument(sourceId = "doc")

        val expected = Document(
                definitions = listOf(OperationDefinition(
                        operationType = OperationType.QUERY,
                        name = "HeroQuery",
                        selections = listOf(Field(
                                name = "hero",
                                selections = listOf(
                                        Field(name = "id", location = AstLocation("doc", 31, 3, 4)),
                                        Field(name = "name", location = AstLocation("doc", 38, 4, 4))
                                ),
                                location = AstLocation("doc", 20, 2, 2)
                        )),
                        location = AstLocation("doc", 0, 1, 0)
                )),
                location = AstLocation("doc", 0, 1, 0),
                sourceMapper = null
        )

        assertEquals(expected, ast.copy(sourceMapper = null))
    }

    @Test
    fun `parses int values`() {
        listOf(
                "1234",
                "-4",
                "9",
                "0",
                "784236564875237645762347623147574756321"
        ).forEach { input ->
            assertThat(input.parseGraphqlValue(true)).isEqualTo(IntValue(BigInteger(input)))
        }
    }

    @Test
    fun `parses float values`() {
        listOf(
                "4.123",
                "-4.123",
                "0.123",
                "123E4",
                "123e-4",
                "-1.123e4",
                "-1.123E4",
                "-1.123e+4",
                "-1.123e4567"
        ).forEach { input ->
            assertThat(input.parseGraphqlValue(true)).isEqualTo(FloatValue(BigDecimal(input)))
        }
    }

    @Test
    fun `parses block string values`() {
        val quote = "\"\"\""
        val input = """
            $quote
              hello,
                world
            $quote
        """.trimIndent()
        assertThat(input.parseGraphqlValue(true)).isEqualTo(StringValue(
                value = """
                    hello,
                      world
                """.trimIndent(),
                block = true,
                blockRawValue = input
        ))
    }
}
