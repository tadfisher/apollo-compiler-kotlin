package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.BooleanValue
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.FloatValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ListValue
import com.apollographql.apollo.compiler.ast.NullValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ast.VariableValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class ArgumentsTest {
    @Test
    fun `emits scalar argument value`() {
        with (IntValue(BigInteger.valueOf(33))) {
            assertThat(argumentValueCode().toString()).isEqualTo("33")
        }

        with (FloatValue(BigDecimal.valueOf(123.4))) {
            assertThat(argumentValueCode().toString()).isEqualTo("123.4")
        }

        with (StringValue("hello world")) {
            assertThat(argumentValueCode().toString()).isEqualTo("\"hello world\"")
        }

        with (BooleanValue(false)) {
            assertThat(argumentValueCode().toString()).isEqualTo("false")
        }
    }

    @Test
    fun `emits null argument value`() {
        assertThat(NullValue().argumentValueCode().toString()).isEqualTo("\"null\"")
    }

    @Test
    fun `emits enum argument value`() {
        assertThat(EnumValue("APOLLO").argumentValueCode().toString()).isEqualTo("\"APOLLO\"")
    }

    @Test
    fun `emits list argument value`() {
        with (ListValue(listOf("foo", "bar", "baz").map { StringValue(it) })) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                listOf(
                    "foo",
                    "bar",
                    "baz"
                )
            """.trimIndent())
        }

        with (ListValue(listOf(
                ListValue(listOf("foo", "bar", "baz").map { StringValue(it) }),
                ListValue(listOf(StringValue("qux")))
        ))) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                listOf(
                    listOf(
                        "foo",
                        "bar",
                        "baz"
                    ),
                    listOf(
                        "qux"
                    )
                )
            """.trimIndent())
        }

        with (ListValue(listOf(
                ObjectValue(listOf(
                        ObjectField("shape", StringValue("rectangle")),
                        ObjectField("area", FloatValue(BigDecimal.valueOf(44)))
                )),
                ObjectValue(listOf(
                        ObjectField("shape", StringValue("square")),
                        ObjectField("area", FloatValue(BigDecimal.valueOf(64)))
                ))
        ))) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                listOf(
                    mapOf(
                        "shape" to "rectangle",
                        "area" to 44.0
                    ),
                    mapOf(
                        "shape" to "square",
                        "area" to 64.0
                    )
                )
            """.trimIndent())
        }
    }

    @Test
    fun `emits object argument value`() {
        with (ObjectValue(listOf(
                ObjectField("shape", StringValue("rectangle")),
                ObjectField("area", FloatValue(BigDecimal.valueOf(44)))
        ))) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                mapOf(
                    "shape" to "rectangle",
                    "area" to 44.0
                )
            """.trimIndent())
        }

        with (ObjectValue(listOf(
                ObjectField("suits", ListValue(listOf("DIAMONDS", "CLUBS", "HEARTS", "SPADES").map { EnumValue(it) })),
                ObjectField("faces", ListValue(listOf("jack", "queen", "king", "ace").map { StringValue(it) }))
        ))) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                mapOf(
                    "suits" to listOf(
                        "DIAMONDS",
                        "CLUBS",
                        "HEARTS",
                        "SPADES"
                    ),
                    "faces" to listOf(
                        "jack",
                        "queen",
                        "king",
                        "ace"
                    )
                )
            """.trimIndent())
        }

        with (ObjectValue(listOf(
                ObjectField("polygons", ListValue(listOf(
                        ObjectValue(listOf(
                                ObjectField("shape", StringValue("rectangle")),
                                ObjectField("area", FloatValue(BigDecimal.valueOf(44)))
                        )),
                        ObjectValue(listOf(
                                ObjectField("shape", StringValue("square")),
                                ObjectField("area", FloatValue(BigDecimal.valueOf(64)))
                        ))
                )))
        ))) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                mapOf(
                    "polygons" to listOf(
                        mapOf(
                            "shape" to "rectangle",
                            "area" to 44.0
                        ),
                        mapOf(
                            "shape" to "square",
                            "area" to 64.0
                        )
                    )
                )
            """.trimIndent())
        }
    }

    @Test
    fun `emits variable argument value`() {
        with (VariableValue("varName")) {
            assertThat(argumentValueCode().toString()).isEqualTo("""
                mapOf(
                    "kind" to "Variable",
                    "variableName" to "varName"
                )
            """.trimIndent())
        }
    }
}