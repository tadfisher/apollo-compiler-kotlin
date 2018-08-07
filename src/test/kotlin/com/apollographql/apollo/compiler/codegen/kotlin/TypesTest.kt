package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.InputFieldWriter.ListWriter
import com.apollographql.apollo.compiler.ir.ListTypeRef
import com.apollographql.apollo.compiler.ir.OptionalType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TypesTest {

    @Test
    fun `writes scalar value`() {
        val ref = stringRef.required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional scalar value`() {
        val ref = stringRef.optional(OptionalType.INPUT)
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value)
            }
        """.trimIndent())
    }

    @Test
    fun `writes enum value`() {
        val ref = episodeRef.required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeString("varName", varName.rawValue)
        """.trimIndent())
    }

    @Test
    fun `writes optional enum value`() {
        val ref = episodeRef.optional(OptionalType.INPUT)
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeString("varName", varName.value?.rawValue)
            }
        """.trimIndent())
    }

    @Test
    fun `writes object value`() {
        val ref = colorInputRef.required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeObject("varName", varName._marshaller)
        """.trimIndent())
    }

    @Test
    fun `writes optional object value`() {
        val ref = colorInputRef.optional(OptionalType.INPUT)
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeObject("varName", varName.value?._marshaller)
            }
        """.trimIndent())
    }

    @Test
    fun `writes list of scalars value`() {
        val ref = ListTypeRef(intRef.required()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeInt(it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes optional list of scalars value`() {
        val ref = ListTypeRef(intRef.required()).optional(OptionalType.INPUT)
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
        val ref = ListTypeRef(episodeRef.required()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it.rawValue) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional enums value`() {
        val ref = ListTypeRef(episodeRef.nullable()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeString(it?.rawValue) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of objects value`() {
        val ref = ListTypeRef(colorInputRef.required()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it._marshaller) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of optional objects value`() {
        val ref = ListTypeRef(colorInputRef.nullable()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeObject(it?._marshaller) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes list of lists value`() {
        val ref = ListTypeRef(ListTypeRef(intRef.required()).required()).required()
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
        val ref = ListTypeRef(customDateRef.required()).required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeList("varName", ${ListWriter::class.qualifiedName} { _itemWriter ->
                varName.forEach { _itemWriter.writeCustom(com.example.types.CustomType.DATE, it) }
            })
        """.trimIndent())
    }

    @Test
    fun `writes custom scalar value`() {
        val ref = customDateRef.required()
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            _writer.writeCustom("varName", com.example.types.CustomType.DATE, varName)
        """.trimIndent())
    }

    @Test
    fun `writes optional custom scalar value`() {
        val ref = customDateRef.optional(OptionalType.INPUT)
        val code = ref.writeInputFieldValueCode("varName")
        assertThat(code.toString().trim()).isEqualTo("""
            if (varName.defined) {
                _writer.writeCustom("varName", com.example.types.CustomType.DATE, varName.value)
            }
        """.trimIndent())
    }
}
