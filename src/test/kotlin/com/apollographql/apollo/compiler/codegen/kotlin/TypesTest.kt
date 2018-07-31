package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldWriter.ListWriter
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

class TypesTest {

    @Test
    fun `writes scalar value`() {
        val ref = TypeRef(
                name = "String",
                kind = TypeKind.STRING,
                isOptional = false
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional scalar value`() {
        val ref = TypeRef(
                name = "String",
                kind = TypeKind.STRING,
                isOptional = true,
                optionalType = Input::class
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value)
            }
        """.trimIndent())
    }

    @Test
    fun `writes enum value`() {
        val ref = TypeRef(
                name = "Enum",
                kind = TypeKind.ENUM,
                isOptional = false
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName.rawValue())
        """.trimIndent())
    }

    @Test
    fun `writes optional enum value`() {
        val ref = TypeRef(
                name = "Enum",
                kind = TypeKind.ENUM,
                isOptional = true,
                optionalType = Input::class
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value?.rawValue())
            }
        """.trimIndent())
    }

    @Test
    fun `writes object value`() {
        val ref = TypeRef(
                name = "ColorInput",
                kind = TypeKind.OBJECT,
                isOptional = false
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeObject("varName", varName._marshaller)
        """.trimIndent())
    }

    @Test
    fun `writes optional object value`() {
        val ref = TypeRef(
                name = "ColorInput",
                kind = TypeKind.OBJECT,
                isOptional = true,
                optionalType = Input::class
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeObject("varName", varName.value?._marshaller)
            }
        """.trimIndent())
    }

    @Test
    fun `writes list of scalars value`() {
        val ref = TypeRef(
                name = "List",
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "Int",
                        kind = TypeKind.INT,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeInt(it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes optional list of scalars value`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = true,
                optionalType = Input::class,
                parameters = listOf(TypeRef(
                        name = "Int",
                        jvmName = Int::class.qualifiedName!!,
                        kind = TypeKind.INT,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                    varName.value?.forEach { _itemWriter.writeInt(it) }
                })
            }
        """.trimIndent())
    }

    @Test
    fun `writes list of enums value`() {
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
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it.rawValue()) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional enums value`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "Enum",
                        kind = TypeKind.ENUM,
                        isOptional = true,
                        optionalType = Input::class
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it?.rawValue()) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of objects value`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "ColorInput",
                        kind = TypeKind.OBJECT,
                        isOptional = false
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it._marshaller) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional objects value`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "ColorInput",
                        kind = TypeKind.OBJECT,
                        isOptional = true,
                        optionalType = Input::class
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it?._marshaller) }
            })
        """.trimIndent())
    }


    @Test
    fun `writes list of lists value`() {
        val ref = TypeRef(
                name = "List",
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "List",
                        kind = TypeKind.LIST,
                        isOptional = false,
                        parameters = listOf(TypeRef(
                                name = "Int",
                                kind = TypeKind.INT,
                                isOptional = false
                        ))
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
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
    fun `writes list of custom scalars value`() {
        val ref = TypeRef(
                name = "List",
                jvmName = List::class.qualifiedName!!,
                kind = TypeKind.LIST,
                isOptional = false,
                parameters = listOf(TypeRef(
                        name = "CustomType.DATE",
                        jvmName = Date::class.qualifiedName!!,
                        kind = TypeKind.CUSTOM,
                        isOptional = false,
                        parameters = emptyList()
                ))
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeCustom(CustomType.DATE, it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes custom scalar value`() {
        val ref = TypeRef(
                name = "CustomType.DATE",
                jvmName = Date::class.qualifiedName!!,
                kind = TypeKind.CUSTOM,
                isOptional = false
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeCustom("varName", CustomType.DATE, varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional custom scalar value`() {
        val ref = TypeRef(
                name = "CustomType.DATE",
                jvmName = Date::class.qualifiedName!!,
                kind = TypeKind.CUSTOM,
                isOptional = true,
                optionalType = Input::class,
                parameters = emptyList()
        )
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeCustom("varName", CustomType.DATE, varName.value)
            }
        """.trimIndent())
    }
}
