package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.compiler.ast.BooleanValue
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ListValue
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ir.EnumTypeSpec
import com.apollographql.apollo.compiler.ir.EnumValueSpec
import com.apollographql.apollo.compiler.ir.InputObjectTypeSpec
import com.apollographql.apollo.compiler.ir.InputValueSpec
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
                                type = intRef.copy(isOptional = false)
                        ),
                        InputValueSpec(
                                name = "nullableIntFieldWithDefaultValue",
                                doc = "for test purposes only",
                                type = intRef.copy(optionalType = Input::class),
                                defaultValue = IntValue(BigInteger.valueOf(10))
                        ),
                        InputValueSpec(
                                name = "commentary",
                                doc = "Comment about the movie, optional",
                                type = stringRef.copy(optionalType = Input::class)
                        ),
                        InputValueSpec(
                                name = "favoriteColor",
                                doc = "Favorite color",
                                type = colorInputRef.copy(isOptional = false)
                        ),
                        InputValueSpec(
                                name = "enumWithDefaultValue",
                                doc = "for test purposes only",
                                type = episodeRef.copy(optionalType = Input::class),
                                defaultValue = EnumValue("JEDI")
                        ),
                        InputValueSpec(
                                name = "nullableEnum",
                                doc = "for test purposes only",
                                type = episodeRef.copy(optionalType = Input::class)
                        ),
                        InputValueSpec(
                                name = "listOfCustomScalar",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(dateRef)),
                                defaultValue = ListValue(listOf(
                                        StringValue("1984-06-21"),
                                        StringValue("1984-11-21")
                                ))
                        ),
                        InputValueSpec(
                                name = "customScalar",
                                doc = "for test purposes only",
                                type = dateRef.copy(optionalType = Input::class),
                                defaultValue = StringValue("1984-06-21")
                        ),
                        InputValueSpec(
                                name = "listOfEnums",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(episodeRef))
                        ),
                        InputValueSpec(
                                name = "listOfInt",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(intRef)),
                                defaultValue = ListValue(listOf(
                                        IntValue(BigInteger.valueOf(1)),
                                        IntValue(BigInteger.valueOf(2)),
                                        IntValue(BigInteger.valueOf(3))
                                ))
                        ),
                        InputValueSpec(
                                name = "listOfString",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(stringRef)),
                                defaultValue = ListValue(listOf(
                                        StringValue("test1"),
                                        StringValue("test2"),
                                        StringValue("test3")
                                ))
                        ),
                        InputValueSpec(
                                name = "booleanWithDefaultValue",
                                doc = "for test purposes only",
                                type = booleanRef.copy(optionalType = Input::class),
                                defaultValue = BooleanValue(true)
                        ),
                        InputValueSpec(
                                name = "listOfListOfString",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(
                                                listRef.copy(
                                                        isOptional = false,
                                                        parameters = listOf(
                                                                stringRef.copy(isOptional = false)
                                                        ))))
                        ),
                        InputValueSpec(
                                name = "listOfListOfEnum",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(
                                                listRef.copy(
                                                        isOptional = false,
                                                        parameters = listOf(
                                                                episodeRef.copy(isOptional = false)
                                                        ))))
                        ),
                        InputValueSpec(
                                name = "listOfListOfCustom",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(
                                                listRef.copy(
                                                        isOptional = false,
                                                        parameters = listOf(
                                                                dateRef.copy(isOptional = false)
                                                        ))))
                        ),
                        InputValueSpec(
                                name = "listOfListOfObject",
                                doc = "for test purposes only",
                                type = listRef.copy(
                                        optionalType = Input::class,
                                        parameters = listOf(
                                                listRef.copy(
                                                        isOptional = false,
                                                        parameters = listOf(
                                                                colorInputRef.copy(isOptional = false)
                                                        ))))
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
            data class ReviewInput(
                val stars: Int,
                val nullableIntFieldWithDefaultValue: Input<Int> = Input.fromNullable(10),
                val commentary: Input<String> = Input.absent,
                val favoriteColor: ColorInput,
                val enumWithDefaultValue: Input<Episode> = Input.fromNullable(Episode.JEDI),
                val nullableEnum: Input<Episode> = Input.absent,
                val listOfCustomScalar: Input<List<CustomType.DATE?>> = Input.absent,
                val customScalar: Input<CustomType.DATE> = Input.absent,
                val listOfEnums: Input<List<Episode?>> = Input.absent,
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
                val listOfListOfString: Input<List<List<String>>> = Input.absent,
                val listOfListOfEnum: Input<List<List<Episode>>> = Input.absent,
                val listOfListOfCustom: Input<List<List<CustomType.DATE>>> = Input.absent,
                val listOfListOfObject: Input<List<List<ColorInput>>> = Input.absent
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
                                    _writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value?.rawValue())
                                }
                                if (nullableEnum.defined) {
                                    _writer.writeString("nullableEnum", nullableEnum.value?.rawValue())
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
                                        listOfEnums.value?.forEach { _itemWriter.writeString(it?.rawValue()) }
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
                                                it.forEach { _itemWriter.writeString(it.rawValue()) }
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
        val spec = EnumTypeSpec(
                name = "Episode",
                doc = "The episodes in the Star Wars trilogy.",
                values = listOf(
                        EnumValueSpec(
                                name = "NEWHOPE",
                                doc = "Star Wars Episode IV: A New Hope, released in 1977."
                        ),
                        EnumValueSpec(
                                name = "EMPIRE",
                                doc = "Star Wars Episode V: The Empire Strikes Back, released in 1980."
                        ),
                        EnumValueSpec(
                                name = "jedi",
                                propertyName = "JEDI",
                                doc = "Star Wars Episode VI: Return of the Jedi, released in 1983."
                        )
                )
        )

        assertThat(spec.typeSpec(ClassName("", spec.name)).code()).isEqualTo("""
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
                 * Auto-generated constant for unknown enum values.
                 */
                _UNKNOWN("_UNKNOWN");

                companion object {
                    fun safeValueOf(rawValue: String) = Episode.values().find { it.rawValue == rawValue } ?: Episode._UNKNOWN
                }
            }
        """.trimIndent())
    }
}