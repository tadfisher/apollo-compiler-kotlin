package com.apollographql.apollo.compiler.validation

import com.apollographql.apollo.compiler.util.StringUtil

interface Violation {
    val errorMessage: String

    companion object {
        fun didYouMean(suggestions: List<String>, text: String = "Did you mean"): String {
            return if (suggestions.isNotEmpty()) {
                " $text ${StringUtil.quotedOrList(suggestions)}?"
            } else {
                ""
            }
        }
    }
}