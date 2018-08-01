package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.ir.ArgumentSpec
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test
import java.math.BigInteger

class SelectionsTest {

    @Test
    fun `emits data class`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "number",
                                doc = "A number.",
                                type = floatRef,
                                responseType = ResponseField.Type.DOUBLE
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                doc = "A string.",
                                type = stringRef.copy(isOptional = false),
                                responseType = ResponseField.Type.STRING
                        )
                )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            /**
             * @param number A number.
             * @param string A string.
             */
            data class Data(val number: Double?, val string: String) {
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                                _writer.writeString("RESPONSE_FIELDS[1]", string)
                            }
                        }

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forDouble("number", "number", null, true, emptyList()),
                                ResponseField.forString("string", "string", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                                val number: Double? = _reader.readDouble(RESPONSE_FIELDS[0])
                                val string: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null")
                                Data(number, string)
                            }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits data class with optionals`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "number",
                                type = floatRef.copy(
                                        isOptional = false,
                                        optionalType = Optional::class
                                ),
                                responseType = ResponseField.Type.DOUBLE
                        ),
                        ResponseFieldSpec(
                                name = "optionalNumber",
                                type = floatRef.copy(
                                        isOptional = true,
                                        optionalType = Optional::class
                                ),
                                responseType = ResponseField.Type.DOUBLE
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                type = stringRef.copy(isOptional = false),
                                responseType = ResponseField.Type.STRING
                        )
                )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            data class Data internal constructor(
                val number: Double,
                val optionalNumber: Optional<Double>,
                val string: String
            ) {
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                                _writer.writeDouble("RESPONSE_FIELDS[1]", optionalNumber.get())
                                _writer.writeString("RESPONSE_FIELDS[2]", string)
                            }
                        }

                constructor(
                    number: Double,
                    optionalNumber: Double?,
                    string: String
                ) : this(number, Optional.fromNullable(optionalNumber), string)

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forDouble("number", "number", null, false, emptyList()),
                                ResponseField.forDouble("optionalNumber", "optionalNumber", null, true, emptyList()),
                                ResponseField.forString("string", "string", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                                val number: Double = Utils.checkNotNull(_reader.readDouble(RESPONSE_FIELDS[0]), "number == null")
                                val optionalNumber: Double? = _reader.readDouble(RESPONSE_FIELDS[1])
                                val string: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[2]), "string == null")
                                Data(number, optionalNumber, string)
                            }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits response fields`() {
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

        assertThat(spec.responseFieldsPropertySpec().code()).isEqualTo("""
            @JvmField
            internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
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
    fun `emits response mapper`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "number",
                                type = floatRef,
                                responseType = ResponseField.Type.DOUBLE
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                type = stringRef.copy(isOptional = false),
                                responseType = ResponseField.Type.STRING
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

        assertThat(spec.responseMapperPropertySpec(ClassName("", "Data")).code()).isEqualTo("""
            @JvmField
            val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                        val number: Double? = _reader.readDouble(RESPONSE_FIELDS[0])
                        val string: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null")
                        val unit: Unit? = _reader.readString(RESPONSE_FIELDS[2])?.let {
                            Unit.safeValueOf(it)
                        }
                        val heroWithReview: HeroWithReview? = _reader.readObject(RESPONSE_FIELDS[3], ResponseReader.ObjectReader<HeroWithReview> {
                            HeroWithReview.MAPPER.map(it)
                        })
                        val list: List<String?>? = _reader.readList(RESPONSE_FIELDS[4], ResponseReader.ListReader<List<String?>> { _itemReader ->
                            _itemReader.readString()
                        })
                        val custom: CustomType.CUSTOM? = _reader.readCustomType(RESPONSE_FIELDS[5] as ResponseField.CustomTypeField)
                        Data(number, string, unit, heroWithReview, list, custom)
                    }
        """.trimIndent())
    }

    @Test
    fun `emits response marshaller`() {
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

        assertThat(spec.responseMarshallerPropertySpec().code()).isEqualTo("""
            internal val _marshaller: ResponseFieldMarshaller by lazy {
                        ResponseFieldMarshaller { _writer ->
                            _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                            _writer.writeString("RESPONSE_FIELDS[1]", unit?.rawValue())
                            _writer.writeObject("RESPONSE_FIELDS[2]", heroWithReview?._marshaller)
                            _writer.writeList("RESPONSE_FIELDS[3]", ResponseWriter.ListWriter { _itemWriter ->
                                list?.forEach { _itemWriter.writeString(it) }
                            })
                            _writer.writeCustom("RESPONSE_FIELDS[4]", CustomType.CUSTOM, custom)
                        }
                    }
        """.trimIndent())
    }
}