package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.apollographql.apollo.compiler.ir.VariableSpec
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import org.junit.Test
import java.util.Date

class OperationsTest {

    @Test
    fun `writes variables type`() {
        val spec = OperationVariablesSpec(listOf(
                VariableSpec(
                        name = "enum",
                        type = TypeRef(
                                name = "Enum",
                                jvmName = Enum::class.qualifiedName!!,
                                kind = TypeKind.ENUM,
                                isOptional = false,
                                parameters = emptyList()
                        ),
                        defaultValue = EnumValue("THREE")
                ),
                VariableSpec(
                        name = "int",
                        type = TypeRef(
                                name = "Int",
                                jvmName = Int::class.qualifiedName!!,
                                kind = TypeKind.INT,
                                isOptional = true,
                                optionalType = Input::class,
                                parameters = emptyList()
                        ),
                        defaultValue = null
                ),
                VariableSpec(
                        name = "date",
                        type = TypeRef(
                                name = CustomType.DATE::class.qualifiedName!!,
                                jvmName = CustomType.DATE.javaType().canonicalName,
                                kind = TypeKind.CUSTOM,
                                isOptional = true,
                                optionalType = Input::class,
                                parameters = emptyList()
                        ),
                        defaultValue = null
                )
        ))
        assertThat(spec.typeSpec(ClassName("", "Variables")).code()).isEqualTo("""
            data class Variables(
                val enum: Enum,
                val int: Input<Int>,
                val date: Input<Date>
            ) : Operation.Variables {
                @delegate:Transient
                private val valueMap: Map<String, Any> by lazy {
                            listOfNotNull(
                                "enum" to enum,
                                ("int" to int).takeIf { int.defined },
                                ("date" to date).takeIf { date.defined }
                            ).toMap()
                        }

                override fun valueMap() = valueMap
                override fun marshaller() = InputFieldMarshaller { _writer ->
                    _writer.writeString("enum", enum.rawValue())
                    if (int.defined) {
                        _writer.writeInt("int", int.value)
                    }
                    if (date.defined) {
                        _writer.writeCustom("date", CustomType.DATE, date.value)
                    }
                }
            }
        """.trimIndent())
    }
}

private enum class Enum { ONE, TWO, THREE, FOUR }

private enum class CustomType : ScalarType {
    ID {
        override fun typeName() = "ID"
        override fun javaType() = String::class.java
    },
    DATE {
        override fun typeName() = "Date"
        override fun javaType() = Date::class.java
    }
}
