package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.BuiltinType
import com.apollographql.apollo.compiler.ir.BuiltinTypeRef
import com.apollographql.apollo.compiler.ir.CustomScalarTypeSpec
import com.apollographql.apollo.compiler.ir.CustomTypeRef
import com.apollographql.apollo.compiler.ir.EnumTypeRef
import com.apollographql.apollo.compiler.ir.EnumTypeSpec
import com.apollographql.apollo.compiler.ir.EnumValueSpec
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.FragmentTypeRef
import com.apollographql.apollo.compiler.ir.FragmentsWrapperSpec
import com.apollographql.apollo.compiler.ir.FragmentsWrapperTypeRef
import com.apollographql.apollo.compiler.ir.JavaTypeName
import com.apollographql.apollo.compiler.ir.ListTypeRef
import com.apollographql.apollo.compiler.ir.ObjectTypeRef
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun String.dropImports() =
        lines().dropWhile { it.startsWith("package") || it.startsWith("import") || it.isBlank() }.joinToString("\n")

fun TypeSpec.code(packageName: String = "") = FileSpec.get(packageName, this).toString().dropImports().trim()
fun FunSpec.code(packageName: String = "") =
        FileSpec.builder(packageName, "code").addFunction(this).build().toString().dropImports().trim()
fun PropertySpec.code(packageName: String = "") =
        FileSpec.builder(packageName, "code").addProperty(this).build().toString().dropImports().trim()

val episodeSpec = EnumTypeSpec(
    name = "Episode",
    javaType = JavaTypeName("com.example.types", "Episode"),
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
        ),
        EnumValueSpec(
            name = "PHANTOM_MENACE",
            doc = "Star Wars Episode I: The Phantom Menace, released in 1997.",
            deprecationReason = "We don't talk about the prequels."
        )
    )
)

val unitSpec = EnumTypeSpec(
    name = "Unit",
    javaType = JavaTypeName("com.example.types", "Unit"),
    values = listOf(
        EnumValueSpec("FOOT"),
        EnumValueSpec("METER")
    )
)

val characterRef = ObjectTypeRef("Character", JavaTypeName("com.example", "TestQuery.Character"))
val heroRef = ObjectTypeRef("Hero", JavaTypeName("com.example", "TestQuery.Hero"))
val episodeRef = EnumTypeRef(spec = episodeSpec)
val unitRef = EnumTypeRef(unitSpec)
val intRef = BuiltinTypeRef(BuiltinType.INT)
val booleanRef = BuiltinTypeRef(BuiltinType.BOOLEAN)
val stringRef = BuiltinTypeRef(BuiltinType.STRING)
val floatRef = BuiltinTypeRef(BuiltinType.FLOAT)
val idRef = BuiltinTypeRef(BuiltinType.ID).required()
val listRef = ListTypeRef(stringRef)
val colorInputRef = ObjectTypeRef("ColorInput", JavaTypeName("com.example.types", "ColorInput"))
val reviewRef = ObjectTypeRef("Review", JavaTypeName("com.example.types", "Review"))

val customIdSpec = CustomScalarTypeSpec("ID",
    javaType = JavaTypeName("java.lang", "String"),
    customTypeName = JavaTypeName("com.example.types", "CustomType.ID"))
val customDateSpec = CustomScalarTypeSpec("Date",
    javaType = JavaTypeName("org.threetenbp", "ZonedDateTime"),
    customTypeName = JavaTypeName("com.example.types", "CustomType.DATE"))
val customUrlSpec = CustomScalarTypeSpec("URL",
    javaType = JavaTypeName("okhttp3", "HttpUrl"),
    customTypeName = JavaTypeName("com.example.types", "CustomType.URL"))

val customIdRef = CustomTypeRef(customIdSpec)
val customDateRef = CustomTypeRef(customDateSpec)
val customUrlRef = CustomTypeRef(customUrlSpec)

val heroDetailsSpec = FragmentSpec(
    name = "HeroDetails",
    javaType = JavaTypeName("com.example.fragments", "HeroDetails"),
    source = """
        fragment HeroDetails on Character {
          name
        }
    """.trimIndent(),
    selections = SelectionSetSpec(fields = listOf(
        ResponseFieldSpec("name",
            doc = "The name of the character",
            type = stringRef.required())
    )).withTypename(),
    typeCondition = "Character",
    possibleTypes = listOf("Human", "Droid")
)

val heroDetailsRef = FragmentTypeRef(heroDetailsSpec).required()
val heroDetailsWrapperRef = FragmentsWrapperTypeRef(FragmentsWrapperSpec(listOf(
    ResponseFieldSpec("heroDetails", type = heroDetailsRef, typeConditions = heroDetailsSpec.possibleTypes))))