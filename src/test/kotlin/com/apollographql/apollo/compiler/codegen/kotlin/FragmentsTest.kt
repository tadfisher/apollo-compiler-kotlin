package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test

class FragmentsTest {
    @Test
    fun `emits simple fragment type`() {
        val spec = FragmentSpec(
                name = "HeroDetails",
                source = """
                    fragment HeroDetails on Character {
                      name
                    }
                """.trimIndent(),
                selections = SelectionSetSpec(fields = listOf(
                        typenameSpec,
                        ResponseFieldSpec(
                                name = "name",
                                doc = "The name of the character",
                                type = stringRef.copy(isOptional = false)
                        )
                )),
                possibleTypes = listOf("Human", "Droid")
        )

        assertThat(spec.typeSpec(ClassName("", spec.name)).code()).isEqualTo("""
            /**
             * @param name The name of the character
             */
            @Generated("Apollo GraphQL")
            data class HeroDetails(val __typename: String, val name: String) : GraphqlFragment {
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                _writer.writeString("RESPONSE_FIELDS[1]", name)
                            }
                        }

                /**
                 * @param name The name of the character
                 */
                constructor(name: String) : this("HeroDetails", name)

                override fun __typename(): String = __typename

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                ResponseField.forString("name", "name", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<HeroDetails> = ResponseFieldMapper<HeroDetails> { _reader ->
                                val __typename: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null")
                                val name: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "name == null")
                                HeroDetails(__typename, name)
                            }

                    const val FRAGMENT_DEFINITION: String = ""${'"'}
                            |fragment HeroDetails on Character {
                            |  name
                            |}
                            ""${'"'}.trimMargin()

                    val POSSIBLE_TYPES: List<String> = listOf("Human", "Droid")
                }
            }
        """.trimIndent())
    }
}