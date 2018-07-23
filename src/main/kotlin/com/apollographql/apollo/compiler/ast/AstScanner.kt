package com.apollographql.apollo.compiler.ast

import java.util.Stack

class AstScanner(val document: Document) {
    private val fragmentSpreadsCache = linkedMapOf<SelectionContainer, List<FragmentSpread>>()
    private val recursivelyReferencedFragmentsCache =
            linkedMapOf<OperationDefinition, List<FragmentDefinition>>()

    fun getFragmentSpreads(astNode: SelectionContainer): List<FragmentSpread> {
        return fragmentSpreadsCache.getOrPut(astNode) {
            val spreads = mutableListOf<FragmentSpread>()
            val setsToVisit = Stack<Selection>()

            astNode.selections.forEach { setsToVisit.push(it) }

            while (setsToVisit.isNotEmpty()) {
                val set = setsToVisit.pop()

                when (set) {
                    is FragmentSpread -> spreads += set
                    is SelectionContainer -> set.selections.forEach { setsToVisit.push(it) }
                }
            }

            spreads.toList()
        }
    }

    fun getRecursivelyReferencedFragments(operation: OperationDefinition): List<FragmentDefinition> {
        return recursivelyReferencedFragmentsCache.getOrPut(operation) {
            val frags = mutableListOf<FragmentDefinition>()
            val collectedNames = mutableSetOf<String>()
            val nodesToVisit = Stack<SelectionContainer>()

            nodesToVisit.push(operation)

            while (nodesToVisit.isNotEmpty()) {
                val node = nodesToVisit.pop()
                val spreads = getFragmentSpreads(node)

                spreads.forEach { spread ->
                    val fragName = spread.name

                    if (!collectedNames.contains(fragName)) {
                        collectedNames += fragName

                        document.fragments[fragName]?.let { frag ->
                            frags += frag
                            nodesToVisit.push(frag)
                        }
                    }
                }
            }

            frags.toList()
        }
    }

    val separateOperations: Map<String?, Document>
        get() = document.operations.mapValues { (_, definition) -> separateOperation(definition) }

    fun separateOperation(definition: OperationDefinition): Document {
        val definitions = listOf(definition) +
                getRecursivelyReferencedFragments(definition).sortedBy { it.location?.line ?: 0 }
        return document.copy(definitions = definitions)
    }

    fun separateOperation(operationName: String?): Document? {
        return if (operationName.isNullOrEmpty() && document.operations.size == 1) {
            separateOperation(document.operations.values.first())
        } else {
            document.operations[operationName]?.let { separateOperation(it) }
        }
    }
}
