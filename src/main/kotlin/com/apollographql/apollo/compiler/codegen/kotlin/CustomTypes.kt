package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.ClassName

class CustomTypes {
    companion object {
        fun className(typesPackage: String) = ClassName(typesPackage, "CustomType")
    }
}