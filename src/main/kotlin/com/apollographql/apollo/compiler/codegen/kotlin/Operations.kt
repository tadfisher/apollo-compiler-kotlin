package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.OperationType.*
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

fun OperationSpec.typeSpec(packageName: String): TypeSpec {
//    val dataType =
  //  val wrappedType = dataType // TODO optional wrapping
//    val variablesType = variables.typeSpec()

    val className = ClassName(packageName, name.capitalize())
    val dataType = className.nestedClass("Data")
    val variablesType = if (variables.isNotEmpty()) {
        className.nestedClass("Variables")
    } else {
        Operation.Variables::class.asClassName()
    }

    return TypeSpec.classBuilder(name)
            .addSuperinterface(operation.typeName(dataType, dataType, variablesType))
            .build()
}

fun OperationType.typeName(
        data: TypeName,
        wrapped: TypeName,
        variables: TypeName
): TypeName = when (this) {
    QUERY -> Query::class.asClassName()
    MUTATION -> Mutation::class.asClassName()
    SUBSCRIPTION -> Subscription::class.asClassName()
}.parameterizedBy(data, wrapped, variables)

fun OperationVariablesSpec.typeSpec(): TypeSpec {
    TODO()
}