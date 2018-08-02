package com.apollographql.apollo.compiler.ir

interface WithDoc {
    val doc: String
}

interface PropertyWithDoc : WithDoc {
    val propertyName: String
}