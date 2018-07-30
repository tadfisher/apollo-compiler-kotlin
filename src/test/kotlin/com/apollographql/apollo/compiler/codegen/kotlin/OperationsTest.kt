package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter.ListWriter
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
                                parameters = emptyList()
                        ),
                        defaultValue = null
                )
        ))
        val code = spec.typeSpec(ClassName("", "Variables"))
        val fileSpec = FileSpec.get("", code)
        assertThat(fileSpec.toString().dropImports().trim()).isEqualTo("""
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

    @Test
    fun `writes scalar variable`() {
        val ref = TypeRef(
                name = "String",
                jvmName = String::class.qualifiedName!!,
                kind = TypeKind.STRING,
                isOptional = false,
                parameters = emptyList()
        )
        val code = ref.writeScalarCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional scalar variable`() {
        val ref = TypeRef(
                name = "String",
                jvmName = String::class.qualifiedName!!,
                kind = TypeKind.STRING,
                isOptional = true,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value)
            }
        """.trimIndent())
    }

    @Test
    fun `writes enum variable`() {
        val ref = TypeRef(
                name = "Enum",
                jvmName = Enum::class.qualifiedName!!,
                kind = TypeKind.ENUM,
                isOptional = false,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName.rawValue())
        """.trimIndent())
    }

    @Test
    fun `writes optional enum variable`() {
        val ref = TypeRef(
                name = "Enum",
                jvmName = Enum::class.qualifiedName!!,
                kind = TypeKind.ENUM,
                isOptional = true,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value?.rawValue())
            }
        """.trimIndent())
    }

    @Test
    fun `writes object variable`() {
        val ref = TypeRef(
                name = "ColorInput",
                jvmName = ColorInput::class.qualifiedName!!,
                kind = TypeKind.OBJECT,
                isOptional = false,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeObject("varName", varName.marshaller())
        """.trimIndent())
    }

    @Test
    fun `writes optional object variable`() {
        val ref = TypeRef(
                name = "ColorInput",
                jvmName = ColorInput::class.qualifiedName!!,
                kind = TypeKind.OBJECT,
                isOptional = true,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeObject("varName", varName.value?.marshaller())
            }
        """.trimIndent())
    }

    @Test
    fun `writes list of scalars variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "Int",
                        jvmName = Int::class.qualifiedName!!,
                        kind = TypeKind.INT,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeInt(it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes optional list of scalars variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = true,
                parameters = listOf(TypeRef(
                        name = "Int",
                        jvmName = Int::class.qualifiedName!!,
                        kind = TypeKind.INT,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                    varName.value?.forEach { _itemWriter.writeInt(it) }
                })
            }
        """.trimIndent())
    }

    @Test
    fun `writes list of enums variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "Enum",
                        jvmName = Enum::class.qualifiedName!!,
                        kind = TypeKind.ENUM,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it.rawValue()) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional enums variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "Enum",
                        jvmName = Enum::class.qualifiedName!!,
                        kind = TypeKind.ENUM,
                        isOptional = true,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it?.rawValue()) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of objects variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "ColorInput",
                        jvmName = ColorInput::class.qualifiedName!!,
                        kind = TypeKind.OBJECT,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it.marshaller()) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional objects variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "ColorInput",
                        jvmName = ColorInput::class.qualifiedName!!,
                        kind = TypeKind.OBJECT,
                        isOptional = true,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it?.marshaller()) }
            })
        """.trimIndent())
    }


    @Test
    fun `writes list of lists variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "List",
                        jvmName = List::class.qualifiedName!!,
                        kind = TypeKind.LIST,
                        isOptional = false,
                        parameters = listOf(TypeRef(
                                name = "Int",
                                jvmName = Int::class.qualifiedName!!,
                                kind = TypeKind.INT,
                                isOptional = false,
                                parameters = emptyList()
                        ))
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach {
                    _itemWriter.writeList(${ListWriter::class.qualifiedName} { _itemWriter ->
                        it.forEach { _itemWriter.writeInt(it) }
                    })
                }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of custom scalars variable`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = CustomType.DATE::class.qualifiedName!!,
                        jvmName = CustomType.DATE.javaType().canonicalName,
                        kind = TypeKind.CUSTOM,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeCustom(${CustomType.DATE::class.qualifiedName}, it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes custom scalar variable`() {
        val ref = TypeRef(
                name = CustomType.DATE::class.qualifiedName!!,
                jvmName = CustomType.DATE.javaType().canonicalName,
                kind = TypeKind.CUSTOM,
                isOptional = false,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeCustom("varName", ${CustomType.DATE::class.qualifiedName}, varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional custom scalar variable`() {
        val ref = TypeRef(
                name = CustomType.DATE::class.qualifiedName!!,
                jvmName = CustomType.DATE.javaType().canonicalName,
                kind = TypeKind.CUSTOM,
                isOptional = true,
                parameters = emptyList()
        )
        val code = ref.writeValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeCustom("varName", ${CustomType.DATE::class.qualifiedName}, varName.value)
            }
        """.trimIndent())
    }
}

enum class Enum { ONE, TWO, THREE, FOUR }

enum class CustomType : ScalarType {
    ID {
        override fun typeName() = "ID"
        override fun javaType() = String::class.java
    },
    DATE {
        override fun typeName() = "Date"
        override fun javaType() = Date::class.java
    }
}

data class ColorInput(val red: Int, val green: Int, val blue: Int) {
    fun marshaller(): InputFieldMarshaller = InputFieldMarshaller { writer ->
        writer.writeInt("red", red)
        writer.writeInt("green", green)
        writer.writeInt("blue", blue)
    }
}

private fun String.dropImports() =
        lines().dropWhile { it.startsWith("import") || it.isBlank() }.joinToString("\n")

