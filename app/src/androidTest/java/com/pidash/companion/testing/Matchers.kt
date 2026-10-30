package com.pidash.companion.testing

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/** Matches nodes whose test tag starts with [prefix] (e.g. all `spot_card_*` items). */
fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("TestTag starts with '$prefix'") {
    it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

/** Matches a custom semantics property value — the standard way to assert Canvas-drawn state. */
fun <T> hasSemantics(key: SemanticsPropertyKey<T>, value: T) = SemanticsMatcher.expectValue(key, value)

val SemanticsNode.testTag: String? get() = config.getOrNull(SemanticsProperties.TestTag)

/** Human-readable identity for failure messages. */
fun SemanticsNode.describe(): String = "${testTag ?: "<no tag>"} @ $boundsInRoot"
