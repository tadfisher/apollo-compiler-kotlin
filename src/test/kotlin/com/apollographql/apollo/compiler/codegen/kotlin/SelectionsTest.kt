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

    private val heroRef = TypeRef(
            name = "HeroWithReview",
            jvmName = "HeroWithReview",
            kind = TypeKind.OBJECT,
            isOptional = true,
            parameters = emptyList()
    )

    private val episodeRef = TypeRef(
            name = "Episode",
            jvmName = "Episode",
            kind = TypeKind.ENUM,
            isOptional = false,
            parameters = emptyList()
    )

    private val unitRef = TypeRef(
            name = "Unit",
            jvmName = Unit::class.qualifiedName!!,
            kind = TypeKind.ENUM,
            isOptional = false,
            parameters = emptyList()
    )

    private val stringRef = TypeRef(
            name = "String",
            jvmName = String::class.qualifiedName!!,
            kind = TypeKind.STRING,
            isOptional = true,
            parameters = emptyList()
    )

    private val floatRef = TypeRef(
            name = "Float",
            jvmName = Double::class.qualifiedName!!,
            kind = TypeKind.DOUBLE,
            isOptional = true,
            parameters = emptyList()
    )

    private val reviewRef = TypeRef(
            name = "Review",
            jvmName = "Review",
            kind = TypeKind.OBJECT,
            isOptional = true,
            parameters = emptyList()
    )
}