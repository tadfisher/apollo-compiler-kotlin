package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.ir.ArgumentSpec
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.FragmentSpreadSpec
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
                                type = floatRef
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                doc = "A string.",
                                type = stringRef.copy(isOptional = false)
                        )
                )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            /**
             * @param number A number.
             * @param string A string.
             */
            data class Data(val number: Double?, val string: String) {
                @delegate:Transient
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
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                _reader.readDouble(RESPONSE_FIELDS[0]),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null")
                            )}
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
                                )
                        ),
                        ResponseFieldSpec(
                                name = "optionalNumber",
                                type = floatRef.copy(
                                        isOptional = true,
                                        optionalType = Optional::class
                                )
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                type = stringRef.copy(isOptional = false)
                        )
                )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            data class Data internal constructor(
                val number: Double,
                val optionalNumber: Optional<Double>,
                val string: String
            ) {
                @delegate:Transient
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
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                Utils.checkNotNull(_reader.readDouble(RESPONSE_FIELDS[0]), "number == null"),
                                _reader.readDouble(RESPONSE_FIELDS[1]),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[2]), "string == null")
                            )}
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
                                                        type = stringRef
                                                ),
                                                ResponseFieldSpec(
                                                        name = "height",
                                                        type = floatRef,
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
                                type = floatRef
                        ),
                        ResponseFieldSpec(
                                name = "string",
                                type = stringRef.copy(isOptional = false)
                        ),
                        ResponseFieldSpec(
                                name = "unit",
                                type = unitRef
                        ),
                        ResponseFieldSpec(
                                name = "heroWithReview",
                                type = heroRef
                        ),
                        ResponseFieldSpec(
                                name = "list",
                                type = listRef
                        ),
                        ResponseFieldSpec(
                                name = "custom",
                                type = customRef
                        ),
                        ResponseFieldSpec(
                                name = "fragments",
                                type = fragmentsRef
                        )
                )
        )

        assertThat(spec.responseMapperPropertySpec(ClassName("", "Data")).code()).isEqualTo("""
            @JvmField
            val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                        _reader.readDouble(RESPONSE_FIELDS[0]),
                        Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null"),
                        _reader.readString(RESPONSE_FIELDS[2])?.let {
                            Unit.safeValueOf(it)
                        },
                        _reader.readObject(RESPONSE_FIELDS[3], ResponseReader.ObjectReader<HeroWithReview> {
                            HeroWithReview.MAPPER.map(it)
                        }),
                        _reader.readList(RESPONSE_FIELDS[4], ResponseReader.ListReader<List<String?>> { _itemReader ->
                            _itemReader.readString()
                        }),
                        _reader.readCustomType(RESPONSE_FIELDS[5] as ResponseField.CustomTypeField),
                        Utils.checkNotNull(_reader.readConditional(RESPONSE_FIELDS[6], ResponseReader.ConditionalTypeReader<Fragments> { _conditionalType, _reader ->
                            Fragments.MAPPER.map(_reader, _conditionalType)
                        }), "fragments == null")
                    )}
        """.trimIndent())
    }

    @Test
    fun `emits response marshaller`() {
        val spec = SelectionSetSpec(
                fields = listOf(
                        ResponseFieldSpec(
                                name = "number",
                                type = floatRef
                        ),
                        ResponseFieldSpec(
                                name = "unit",
                                type = unitRef
                        ),
                        ResponseFieldSpec(
                                name = "heroWithReview",
                                type = heroRef
                        ),
                        ResponseFieldSpec(
                                name = "list",
                                type = listRef
                        ),
                        ResponseFieldSpec(
                                name = "custom",
                                type = customRef
                        ),
                        ResponseFieldSpec(
                                name = "fragments",
                                type = fragmentsRef
                        )
                )
        )

        assertThat(spec.responseMarshallerPropertySpec().code()).isEqualTo("""
            @delegate:Transient
            internal val _marshaller: ResponseFieldMarshaller by lazy {
                        ResponseFieldMarshaller { _writer ->
                            _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                            _writer.writeString("RESPONSE_FIELDS[1]", unit?.rawValue())
                            _writer.writeObject("RESPONSE_FIELDS[2]", heroWithReview?._marshaller)
                            _writer.writeList("RESPONSE_FIELDS[3]", ResponseWriter.ListWriter { _itemWriter ->
                                list?.forEach { _itemWriter.writeString(it) }
                            })
                            _writer.writeCustom("RESPONSE_FIELDS[4]", CustomType.CUSTOM, custom)
                            fragments._marshaller.marshal(_writer)
                        }
                    }
        """.trimIndent())
    }

    @Test
    fun `emits fragments type`() {
        val humanFragmentSpec = FragmentSpec(
                name = "HumanDetails",
                source = """
                    fragment HumanDetails on Human {
                      name
                      height
                    }
                """.trimIndent(),
                selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(name = "name", type = stringRef),
                        ResponseFieldSpec(name = "height", type = floatRef)
                )),
                typeCondition = TypeRef(name = "Human", kind = TypeKind.OBJECT)
        )

        val droidFragmentSpec = FragmentSpec(
                name = "DroidDetails",
                source = """
                    fragment DroidDetails on Droid {
                      name
                      primaryFunction
                    }
                """.trimIndent(),
                selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(name = "name", type = stringRef),
                        ResponseFieldSpec(name = "primaryFunction", type = stringRef)
                ))
        )

        val spec = SelectionSetSpec(fragmentSpreads = listOf(
                FragmentSpreadSpec(humanFragmentSpec),
                FragmentSpreadSpec(droidFragmentSpec)
        ))

        assertThat(spec.fragmentsTypeSpec().code()).isEqualTo("""
            data class Fragments(val humanDetails: HumanDetails?, val droidDetails: DroidDetails?) {
                @delegate:Transient
                val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                humanDetails?._marshaller.marshal(_writer)
                                droidDetails?._marshaller.marshal(_writer)
                            }
                        }

                companion object {
                    @JvmField
                    val MAPPER: FragmentResponseFieldMapper<Fragments> =
                            FragmentResponseFieldMapper<Fragments> { _reader, _conditionalType -> Fragments(
                                HumanDetails.MAPPER.takeIf (_conditionalType in HumanDetails.POSSIBLE_TYPES)?.map(_reader),
                                DroidDetails.MAPPER.takeIf (_conditionalType in DroidDetails.POSSIBLE_TYPES)?.map(_reader)
                            )}
                }
            }
        """.trimIndent())
    }
}