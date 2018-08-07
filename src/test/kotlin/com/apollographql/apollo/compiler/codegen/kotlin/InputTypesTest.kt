package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.BooleanValue
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ListValue
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ir.CustomTypesSpec
import com.apollographql.apollo.compiler.ir.InputObjectTypeSpec
import com.apollographql.apollo.compiler.ir.InputValueSpec
import com.apollographql.apollo.compiler.ir.OptionalType
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test
import java.math.BigInteger

class InputTypesTest {

    @Test
    fun `emits input object type`() {
        val spec = InputObjectTypeSpec(
            name = "ReviewInput",
            doc = "The input object sent when someone is creating a new review",
            values = listOf(
                InputValueSpec(
                    name = "stars",
                    doc = "0-5 stars",
                    type = intRef.required()
                ),
                InputValueSpec(
                    name = "nullableIntFieldWithDefaultValue",
                    doc = "for test purposes only",
                    type = intRef.optional(OptionalType.INPUT),
                    defaultValue = IntValue(BigInteger.valueOf(10))
                ),
                InputValueSpec(
                    name = "commentary",
                    doc = "Comment about the movie, optional",
                    type = stringRef.optional(OptionalType.INPUT)
                ),
                InputValueSpec(
                    name = "favoriteColor",
                    doc = "Favorite color",
                    type = colorInputRef.required()
                ),
                InputValueSpec(
                    name = "enumWithDefaultValue",
                    doc = "for test purposes only",
                    type = episodeRef.optional(OptionalType.INPUT),
                    defaultValue = EnumValue("JEDI")
                ),
                InputValueSpec(
                    name = "nullableEnum",
                    doc = "for test purposes only",
                    type = episodeRef.optional(OptionalType.INPUT)
                ),
                InputValueSpec(
                    name = "listOfCustomScalar",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(customDateRef),
                    defaultValue = ListValue(listOf(
                        StringValue("1984-06-21"),
                        StringValue("1984-11-21")
                    ))
                ),
                InputValueSpec(
                    name = "customScalar",
                    doc = "for test purposes only",
                    type = customDateRef.optional(OptionalType.INPUT),
                    defaultValue = StringValue("1984-06-21")
                ),
                InputValueSpec(
                    name = "listOfEnums",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(episodeRef)
                ),
                InputValueSpec(
                    name = "listOfInt",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(intRef),
                    defaultValue = ListValue(listOf(
                        IntValue(BigInteger.valueOf(1)),
                        IntValue(BigInteger.valueOf(2)),
                        IntValue(BigInteger.valueOf(3))
                    ))
                ),
                InputValueSpec(
                    name = "listOfString",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(stringRef),
                    defaultValue = ListValue(listOf(
                        StringValue("test1"),
                        StringValue("test2"),
                        StringValue("test3")
                    ))
                ),
                InputValueSpec(
                    name = "booleanWithDefaultValue",
                    doc = "for test purposes only",
                    type = booleanRef.optional(OptionalType.INPUT),
                    defaultValue = BooleanValue(true)
                ),
                InputValueSpec(
                    name = "listOfListOfString",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(listRef.required().of(stringRef.required()))
                ),
                InputValueSpec(
                    name = "listOfListOfEnum",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(listRef.required().of(episodeRef.required()))
                ),
                InputValueSpec(
                    name = "listOfListOfCustom",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(listRef.required().of(customDateRef.required()))
                ),
                InputValueSpec(
                    name = "listOfListOfObject",
                    doc = "for test purposes only",
                    type = listRef.optional(OptionalType.INPUT).of(listRef.required().of(colorInputRef.required()))
                )))
        assertThat(spec.typeSpec(ClassName("", "ReviewInput")).code()).isEqualTo("""
            /**
             * @param stars 0-5 stars
             * @param nullableIntFieldWithDefaultValue for test purposes only
             * @param commentary Comment about the movie, optional
             * @param favoriteColor Favorite color
             * @param enumWithDefaultValue for test purposes only
             * @param nullableEnum for test purposes only
             * @param listOfCustomScalar for test purposes only
             * @param customScalar for test purposes only
             * @param listOfEnums for test purposes only
             * @param listOfInt for test purposes only
             * @param listOfString for test purposes only
             * @param booleanWithDefaultValue for test purposes only
             * @param listOfListOfString for test purposes only
             * @param listOfListOfEnum for test purposes only
             * @param listOfListOfCustom for test purposes only
             * @param listOfListOfObject for test purposes only
             */
            @Generated("Apollo GraphQL")
            data class ReviewInput(
                val stars: Int,
                val nullableIntFieldWithDefaultValue: Input<Int> = Input.fromNullable(10),
                val commentary: Input<String> = Input.absent(),
                val favoriteColor: ColorInput,
                val enumWithDefaultValue: Input<Episode> = Input.fromNullable(Episode.JEDI),
                val nullableEnum: Input<Episode> = Input.absent(),
                val listOfCustomScalar: Input<List<ZonedDateTime?>> = Input.absent(),
                val customScalar: Input<ZonedDateTime> = Input.absent(),
                val listOfEnums: Input<List<Episode?>> = Input.absent(),
                val listOfInt: Input<List<Int?>> = Input.fromNullable(listOf(
                            1,
                            2,
                            3
                        )),
                val listOfString: Input<List<String?>> = Input.fromNullable(listOf(
                            "test1",
                            "test2",
                            "test3"
                        )),
                val booleanWithDefaultValue: Input<Boolean> = Input.fromNullable(true),
                val listOfListOfString: Input<List<List<String>>> = Input.absent(),
                val listOfListOfEnum: Input<List<List<Episode>>> = Input.absent(),
                val listOfListOfCustom: Input<List<List<ZonedDateTime>>> = Input.absent(),
                val listOfListOfObject: Input<List<List<ColorInput>>> = Input.absent()
            ) {
                @delegate:Transient
                internal val _marshaller: InputFieldMarshaller by lazy {
                            InputFieldMarshaller { _writer ->
                                _writer.writeInt("stars", stars)
                                if (nullableIntFieldWithDefaultValue.defined) {
                                    _writer.writeInt("nullableIntFieldWithDefaultValue", nullableIntFieldWithDefaultValue.value)
                                }
                                if (commentary.defined) {
                                    _writer.writeString("commentary", commentary.value)
                                }
                                _writer.writeObject("favoriteColor", favoriteColor._marshaller)
                                if (enumWithDefaultValue.defined) {
                                    _writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value?.rawValue)
                                }
                                if (nullableEnum.defined) {
                                    _writer.writeString("nullableEnum", nullableEnum.value?.rawValue)
                                }
                                if (listOfCustomScalar.defined) {
                                    _writer.writeList("listOfCustomScalar", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfCustomScalar.value?.forEach { _itemWriter.writeCustom(CustomType.DATE, it) }
                                    })
                                }
                                if (customScalar.defined) {
                                    _writer.writeCustom("customScalar", CustomType.DATE, customScalar.value)
                                }
                                if (listOfEnums.defined) {
                                    _writer.writeList("listOfEnums", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfEnums.value?.forEach { _itemWriter.writeString(it?.rawValue) }
                                    })
                                }
                                if (listOfInt.defined) {
                                    _writer.writeList("listOfInt", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfInt.value?.forEach { _itemWriter.writeInt(it) }
                                    })
                                }
                                if (listOfString.defined) {
                                    _writer.writeList("listOfString", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfString.value?.forEach { _itemWriter.writeString(it) }
                                    })
                                }
                                if (booleanWithDefaultValue.defined) {
                                    _writer.writeBoolean("booleanWithDefaultValue", booleanWithDefaultValue.value)
                                }
                                if (listOfListOfString.defined) {
                                    _writer.writeList("listOfListOfString", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfListOfString.value?.forEach {
                                            _itemWriter.writeList(InputFieldWriter.ListWriter { _itemWriter ->
                                                it.forEach { _itemWriter.writeString(it) }
                                            })
                                        }
                                    })
                                }
                                if (listOfListOfEnum.defined) {
                                    _writer.writeList("listOfListOfEnum", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfListOfEnum.value?.forEach {
                                            _itemWriter.writeList(InputFieldWriter.ListWriter { _itemWriter ->
                                                it.forEach { _itemWriter.writeString(it.rawValue) }
                                            })
                                        }
                                    })
                                }
                                if (listOfListOfCustom.defined) {
                                    _writer.writeList("listOfListOfCustom", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfListOfCustom.value?.forEach {
                                            _itemWriter.writeList(InputFieldWriter.ListWriter { _itemWriter ->
                                                it.forEach { _itemWriter.writeCustom(CustomType.DATE, it) }
                                            })
                                        }
                                    })
                                }
                                if (listOfListOfObject.defined) {
                                    _writer.writeList("listOfListOfObject", InputFieldWriter.ListWriter { _itemWriter ->
                                        listOfListOfObject.value?.forEach {
                                            _itemWriter.writeList(InputFieldWriter.ListWriter { _itemWriter ->
                                                it.forEach { _itemWriter.writeObject(it._marshaller) }
                                            })
                                        }
                                    })
                                }
                            }
                        }
            }
        """.trimIndent())
    }

    @Test
    fun `emits enum type`() {
        assertThat(episodeSpec.typeSpec(ClassName("", episodeSpec.name)).code()).isEqualTo("""
            /**
             * The episodes in the Star Wars trilogy.
             */
            @Generated("Apollo GraphQL")
            enum class Episode(val rawValue: String) {
                /**
                 * Star Wars Episode IV: A New Hope, released in 1977.
                 */
                NEWHOPE("NEWHOPE"),

                /**
                 * Star Wars Episode V: The Empire Strikes Back, released in 1980.
                 */
                EMPIRE("EMPIRE"),

                /**
                 * Star Wars Episode VI: Return of the Jedi, released in 1983.
                 */
                JEDI("jedi"),

                /**
                 * Star Wars Episode I: The Phantom Menace, released in 1997.
                 */
                @Deprecated("We don't talk about the prequels.")
                PHANTOM_MENACE("PHANTOM_MENACE"),

                /**
                 * Auto-generated constant for unknown enum values.
                 */
                _UNKNOWN("_UNKNOWN");

                companion object {
                    fun safeValueOf(rawValue: String) = Episode.values().find { it.rawValue == rawValue } ?: Episode._UNKNOWN
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits custom types enum`() {
        val spec = CustomTypesSpec(listOf(customIdSpec, customDateSpec, customUrlSpec))
        assertThat(spec.typeSpec().code("com.example.types")).isEqualTo("""
            @Generated("Apollo GraphQL")
            enum class CustomType : ScalarType {
                ID {
                    override fun typeName(): String = "ID"

                    override fun javaType(): Class<*> = java.lang.String::class.java
                },

                DATE {
                    override fun typeName(): String = "Date"

                    override fun javaType(): Class<*> = ZonedDateTime::class.java
                },

                URL {
                    override fun typeName(): String = "URL"

                    override fun javaType(): Class<*> = HttpUrl::class.java
                }
            }
        """.trimIndent())
    }
}