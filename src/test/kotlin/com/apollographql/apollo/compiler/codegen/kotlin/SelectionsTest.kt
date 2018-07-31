package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.Optional
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.junit.Test
import java.math.BigInteger

class SelectionsTest {

    @Test
    fun `emits data class`() {
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
                        )
                )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data"))
                .wrapInFile().toString().trim().dropImports()
        ).isEqualTo("""
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

        assertThat(spec.dataClassSpec(ClassName("", "Data"))
                .wrapInFile().toString().trim().dropImports()
        ).isEqualTo("""
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

        assertThat(spec.responseFieldsPropertySpec().wrapInFile().toString().trim().dropImports())
                .isEqualTo("""
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

        assertThat(spec.responseMapperPropertySpec(ClassName("", "Data"))
                .wrapInFile().toString().dropImports().trim()
        ).isEqualTo("""
            @JvmField
            val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                        val number: Double? = _reader.readDouble(RESPONSE_FIELDS[0])
                        val string: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null")
                        val unit: Unit? = _reader.readString(RESPONSE_FIELDS[2])?.let {
                            Unit.safeValueOf(it)
                        }
                        val heroWithReview: HeroWithReview? = _reader.readObject(RESPONSE_FIELDS[3], ResponseReader.ObjectReader<HeroWithReview> {
                            HeroWithReview.Mapper.map(it)
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

        assertThat(spec.responseMarshallerPropertySpec()
                .wrapInFile().toString().dropImports().trim()
        ).isEqualTo("""
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