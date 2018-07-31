package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.codegen.dropImports
import com.apollographql.apollo.compiler.codegen.wrapInFile
import com.apollographql.apollo.compiler.ir.ArgumentSpec
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigInteger

class SelectionsTest {
    @Test
    fun `emits response fields property`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "heroWithReview",
                                type = heroRef,
                                responseType = ResponseField.Type.OBJECT,
                                arguments = listOf(
                                        ArgumentSpec(
                                                name = "episode",
                                                value = VariableValue("episode"),
                                                type = episodeRef
                                        ),
                                        ArgumentSpec(
                                                name = "review",
                                                value = ObjectValue(listOf(
                                                        ObjectField("stars", VariableValue("stars")),
                                                        ObjectField("favoriteColor", ObjectValue(listOf(
                                                                ObjectField("red", IntValue(BigInteger.valueOf(0))),
                                                                ObjectField("green", VariableValue("greenValue")),
                                                                ObjectField("blue", IntValue(BigInteger.valueOf(0)))
                                                        )))
                                                )),
                                                type = reviewRef
                                        )
                                ),
                                selections = SelectionSetSpec(
                                        fields = listOf(
                                                ResponseFieldSpec(
                                                        name = "name",
                                                        type = stringRef,
                                                        responseType = ResponseField.Type.STRING
                                                ),
                                                ResponseFieldSpec(
                                                        name = "height",
                                                        type = floatRef,
                                                        responseType = ResponseField.Type.DOUBLE,
                                                        arguments = listOf(
                                                                ArgumentSpec(
                                                                        name = "unit",
                                                                        value = EnumValue("FOOT"),
                                                                        type = unitRef
                                                                ))))))))

        assertThat(spec.responseFieldsPropertySpec().wrapInFile().toString().trim().dropImports())
                .isEqualTo("""
                    @JvmField
                    internal val _responseFields: Array = arrayOf(
                                ResponseField.forObject("heroWithReview", "heroWithReview", mapOf(
                                    "episode" to mapOf(
                                        "kind" to "Variable",
                                        "variableName" to "episode"
                                    ),
                                    "review" to mapOf(
                                        "stars" to mapOf(
                                            "kind" to "Variable",
                                            "variableName" to "stars"
                                        ),
                                        "favoriteColor" to mapOf(
                                            "red" to 0,
                                            "green" to mapOf(
                                                "kind" to "Variable",
                                                "variableName" to "greenValue"
                                            ),
                                            "blue" to 0
                                        )
                                    )
                                ), true, emptyList())
                            )
                """.trimIndent())
    }

    @Test
    fun `emits response field marshaller function`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "number",
                                type = floatRef,
                                responseType = ResponseField.Type.DOUBLE
                        ),
                        ResponseFieldSpec(
                                name = "unit",
                                type = unitRef,
                                responseType = ResponseField.Type.ENUM
                        ),
                        ResponseFieldSpec(
                                name = "heroWithReview",
                                type = heroRef,
                                responseType = ResponseField.Type.OBJECT
                        ),
                        ResponseFieldSpec(
                                name = "list",
                                type = listRef,
                                responseType = ResponseField.Type.LIST
                        ),
                        ResponseFieldSpec(
                                name = "custom",
                                type = customRef,
                                responseType = ResponseField.Type.CUSTOM
                        )
                )
        )

        assertThat(spec.responseMarshallerFunSpec().wrapInFile().toString().dropImports().trim())
                .isEqualTo("""
                    override fun marshaller() = ResponseFieldMarshaller { _writer ->
                        _writer.writeDouble("responseFields[0]", number)
                        _writer.writeString("responseFields[1]", unit?.rawValue())
                        _writer.writeObject("responseFields[2]", heroWithReview?.marshaller())
                        _writer.writeList("responseFields[3]", ResponseWriter.ListWriter { _itemWriter ->
                            list?.forEach { _itemWriter.writeString(it) }
                        })
                        _writer.writeCustom("responseFields[4]", CustomType.CUSTOM, custom)
                    }
                """.trimIndent())
    }

    private val heroRef = TypeRef(
            name = "HeroWithReview",
            kind = TypeKind.OBJECT
    )

    private val episodeRef = TypeRef(
            name = "Episode",
            kind = TypeKind.ENUM
    )

    private val unitRef = TypeRef(
            name = "Unit",
            jvmName = Unit::class.qualifiedName!!,
            kind = TypeKind.ENUM
    )

    private val stringRef = TypeRef(
            name = "String",
            jvmName = String::class.qualifiedName!!,
            kind = TypeKind.STRING
    )

    private val floatRef = TypeRef(
            name = "Float",
            jvmName = Double::class.qualifiedName!!,
            kind = TypeKind.DOUBLE
    )

    private val listRef = TypeRef(
            name = "List",
            jvmName = List::class.qualifiedName!!,
            kind = TypeKind.LIST,
            isOptional = true,
            parameters = listOf(stringRef)
    )

    private val reviewRef = TypeRef(
            name = "Review",
            jvmName = "Review",
            kind = TypeKind.OBJECT
    )

    private val customRef = TypeRef(
            name = "CustomType.CUSTOM",
            kind = TypeKind.CUSTOM
    )
}