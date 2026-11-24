/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticLossRisk
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.PositioningStrategy

/**
 * A positioning strategy for the KMP parse tree.
 *
 * TODO: implement positioning for the KMP parse tree, KT-91322
 */
open class KmpPositioningStrategy : PositioningStrategy() {
    override fun markDiagnostic(diagnostic: KtDiagnosticWithSource): List<TextRange> = TODO("KT-91322")

    @DiagnosticLossRisk
    override fun isValid(element: KtSourceElement): Boolean = TODO("KT-91322")
}
