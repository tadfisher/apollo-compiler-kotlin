package com.apollographql.apollo.compiler.schema

import com.apollographql.apollo.compiler.ast.AstNode
import com.apollographql.apollo.compiler.ast.Directive as AstDirective
import com.apollographql.apollo.compiler.validation.Violation

interface Type {
    val namedType: Type
    get() {
        fun loop(type: Type): Type {
            return when (type) {
                is OptionInputType<*> -> loop(type.ofType)
                is OptionType<*> -> loop(type.ofType)
                is ListInputType<*> -> loop(type.ofType)
                is ListType<*> -> loop(type.ofType)
                is Named -> type
                else -> throw IllegalStateException("Expected named type, but got: ${type::class.simpleName}")
            }
        }
        return loop(this)
    }
}

interface InputType<in T> : Type

val InputType<*>.isOptional: Boolean get() = this is OptionInputType<*>
val InputType<*>.isList: Boolean get() = this is ListInputType<*>
val InputType<*>.isNamed: Boolean get() = !(isOptional && isList)
val InputType<*>.nonOptionalType: InputType<*> get() = (this as? OptionInputType<*>)?.ofType ?: this
val InputType<*>.namedInputType: InputType<*> get() = namedType as InputType<*>

interface OutputType<in T> : Type
interface LeafType : Type, Named, HasAstInfo
interface CompositeType<T> : Type, Named {
    abstract override val name: String

    // fun typeOf<C>(value: Any, schema: Schema<C, *>): ObjectType<C, *>
}

interface AbstractType : Type, Named {
//    TODO fun <C> typeOf(value: Any, schema: Schema<C, *>): ObjectType<C, *> =
//            schema.possibleTypes
}

interface NullableType
interface UnmodifiedType

interface Named {
    val name: String
    val description: String?

    fun rename(newName: String): Named

    companion object {
        private val nameRegexp = """^[_a-zA-Z][_a-zA-Z0-9]*$""".toRegex()

        fun isValidName(name: String): Boolean = nameRegexp.matches(name)
    }
}

interface HasArguments {
    val arguments: List<Argument<*>>
}

interface HasDeprecation {
    val deprecationReason: String?
}

interface HasAstInfo {
    val astDirectives: List<AstDirective>
    val astNodes: List<AstNode>
}

data class ScalarType<T>(
        override val name: String,
        override val description: String? = null,
        // coerceUserInput: (Any) -> Either<Violation, T>
        // coerceOutput: (T, Set<MarshallerCapability) -> Any
        // coerceInput: (Value) -> Either<Violation, T>
        // complexity: Double
        // scalarInfo: Set<ScalarValueInfo>
        override val astDirectives: List<AstDirective> = emptyList(),
        override val astNodes: List<AstNode> = emptyList()
) : InputType<T>, OutputType<T>, LeafType, NullableType, UnmodifiedType, Named {
    override fun rename(newName: String): Named = copy(name = newName)
}

data class ListType<T>(val ofType: OutputType<T>) : OutputType<List<T>>, NullableType
data class ListInputType<T>(val ofType: InputType<T>) : InputType<List<T>>, NullableType

data class OptionType<T>(val ofType: OutputType<T>) : OutputType<T?>
data class OptionInputType<T>(val ofType: InputType<T>) : InputType<T?>

abstract class Either<out A, out B> private constructor() {
    class Left<A>(val value: A): Either<A, Nothing>()
    class Right<B>(val value: B): Either<Nothing, B>()
}

data class Schema<C, V>(
        val query: ObjectType<C, V>,
        val mutation: ObjectType<C, V>? = null,
        val subscription: ObjectType<C, V>? = null,
        val additionalTypes: List<Type> = emptyList(),
        //val directives: List<Directive>
        val astDirectives: List<AstDirective> = emptyList(),
        val astNodes: List<AstNode> = emptyList()
) {
//    val implementations: Map<String, List<ObjectType<*, *>>> by lazy {
//        fun findConcreteTypes(type: ObjectLikeType<*, *>): List<ObjectType<*, *>> {
//            return when(type) {
//
//            }
//        }
//    }
//
//    val possibleTypes: Map<String, List<ObjectType<*, *>>> by lazy {
//        // TODO
//        emptyMap()
//    }

}