/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.diagnostics.PositioningStrategyKind
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds

/**
 * Stubs of the KMP parse tree implementations of all [PositioningStrategyKinds],
 * in the same order as in `LightTreePositioningStrategies` to simplify porting.
 *
 * TODO: implement the strategies based on LightTreePositioningStrategies, KT-91322
 */
@Suppress("UNUSED") // Most strategies are only accessed via `kindToStrategy`, where they are registered by `registerAs`
object KmpPositioningStrategies {
    val kindToStrategy: Map<PositioningStrategyKind, KmpPositioningStrategy>
        field: MutableMap<PositioningStrategyKind, KmpPositioningStrategy> = mutableMapOf()

    private fun <T : KmpPositioningStrategy> T.registerAs(kind: PositioningStrategyKind): T {
        require(kind !in kindToStrategy) { "${kind::class.simpleName} is already registered" }
        kindToStrategy[kind] = this
        return this
    }

    val DEFAULT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DEFAULT)

    val SYNTAX_ERROR: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SYNTAX_ERROR)

    val SUPERTYPES_LIST: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SUPERTYPES_LIST)

    val VAL_OR_VAR_NODE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VAL_OR_VAR_NODE)

    val COMPANION_OBJECT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.COMPANION_OBJECT)

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

    val DECLARATION_RETURN_TYPE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_RETURN_TYPE)

    val DECLARATION_START_TO_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_START_TO_NAME)

    val DECLARATION_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_NAME)

    val DECLARATION_NAME_ONLY: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_NAME_ONLY)

    val ACTUAL_DECLARATION_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ACTUAL_DECLARATION_NAME)

    val CONTEXT_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.CONTEXT_KEYWORD)

    val DECLARATION_SIGNATURE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE)

    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

    val DECLARATION_SIGNATURE_OR_DEFAULT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE_OR_DEFAULT)

    val VARIABLE_INITIALIZER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VARIABLE_INITIALIZER)

    val VISIBILITY_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VISIBILITY_MODIFIER)

    val MODALITY_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.MODALITY_MODIFIER)

    val ABSTRACT_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ABSTRACT_MODIFIER)

    val OPEN_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OPEN_MODIFIER)

    val OVERRIDE_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OVERRIDE_MODIFIER)

    val PRIVATE_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PRIVATE_MODIFIER)

    val LATEINIT_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.LATEINIT_MODIFIER)

    val VARIANCE_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VARIANCE_MODIFIER)

    val CONST_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.CONST_MODIFIER)

    val FUN_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.FUN_MODIFIER)

    val SUSPEND_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SUSPEND_MODIFIER)

    val INLINE_OR_VALUE_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INLINE_OR_VALUE_MODIFIER)

    val VALUE_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VALUE_MODIFIER)

    val INNER_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INNER_MODIFIER)

    val DATA_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DATA_MODIFIER)

    val OPERATOR_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OPERATOR_MODIFIER)

    val INFIX_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INFIX_MODIFIER)

    val ENUM_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ENUM_MODIFIER)

    val TAILREC_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TAILREC_MODIFIER)

    val EXTERNAL_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.EXTERNAL_MODIFIER)

    val EXPECT_ACTUAL_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.EXPECT_ACTUAL_MODIFIER)

    val OBJECT_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OBJECT_KEYWORD)

    val FIELD_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.FIELD_KEYWORD)

    val PROPERTY_DELEGATE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE)

    val PROPERTY_DELEGATE_BY_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE_BY_KEYWORD)

    val INLINE_PARAMETER_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INLINE_PARAMETER_MODIFIER)

    val INLINE_FUN_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INLINE_FUN_MODIFIER)

    val OPERATOR: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OPERATOR)

    val REFERENCE_OR_OPERATOR: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.REFERENCE_OR_OPERATOR)

    val PARAMETER_DEFAULT_VALUE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PARAMETER_DEFAULT_VALUE)

    val PARAMETERS_WITH_DEFAULT_VALUE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PARAMETERS_WITH_DEFAULT_VALUE)

    val PARAMETER_VARARG_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PARAMETER_VARARG_MODIFIER)

    val NAME_OF_NAMED_ARGUMENT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.NAME_OF_NAMED_ARGUMENT)

    val VALUE_ARGUMENTS_LIST: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.VALUE_ARGUMENTS_LIST)

    val DOT_BY_QUALIFIED: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DOT_BY_QUALIFIED)

    val SELECTOR_BY_QUALIFIED: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SELECTOR_BY_QUALIFIED)

    val FUN_INTERFACE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.FUN_INTERFACE)

    val REFERENCE_BY_QUALIFIED: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.REFERENCE_BY_QUALIFIED)

    val REFERENCED_NAME_BY_QUALIFIED: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.REFERENCED_NAME_BY_QUALIFIED)

    val RECEIVER_OF_DOT_QUALIFIED: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.RECEIVER_OF_DOT_QUALIFIED)

    val WHEN_EXPRESSION: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.WHEN_EXPRESSION)

    val WHEN_GUARD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.WHEN_GUARD)

    val IF_EXPRESSION: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.IF_EXPRESSION)

    val TRY_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TRY_KEYWORD)

    val ELSE_ENTRY: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ELSE_ENTRY)

    val ARRAY_ACCESS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ARRAY_ACCESS)

    val SAFE_ACCESS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SAFE_ACCESS)

    val AS_TYPE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.AS_TYPE)

    val USELESS_ELVIS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.USELESS_ELVIS)

    val USELESS_ELVIS_LEFT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.USELESS_ELVIS_LEFT)

    val RETURN_WITH_LABEL: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.RETURN_WITH_LABEL)

    val WHOLE_ELEMENT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.WHOLE_ELEMENT)

    val LONG_LITERAL_SUFFIX: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.LONG_LITERAL_SUFFIX)

    val REIFIED_MODIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.REIFIED_MODIFIER)

    val TYPE_PARAMETERS_LIST: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TYPE_PARAMETERS_LIST)

    val FUNCTION_TYPE_RECEIVER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.FUNCTION_TYPE_RECEIVER)

    val NAME_IDENTIFIER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.NAME_IDENTIFIER)

    val REDUNDANT_NULLABLE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.REDUNDANT_NULLABLE)

    val QUESTION_MARK_BY_TYPE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.QUESTION_MARK_BY_TYPE)

    val ANNOTATION_USE_SITE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.ANNOTATION_USE_SITE)

    val IMPORT_LAST_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.IMPORT_LAST_NAME)

    val IMPORT_LAST_BUT_ONE_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.IMPORT_LAST_BUT_ONE_NAME)

    val IMPORT_ALIAS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.IMPORT_ALIAS)

    val SPREAD_OPERATOR: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SPREAD_OPERATOR)

    val DECLARATION_WITH_BODY: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DECLARATION_WITH_BODY)

    val UNREACHABLE_CODE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.UNREACHABLE_CODE)

    val NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

    val LABEL: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.LABEL)

    val COMMAS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.COMMAS)

    val NON_FINAL_MODIFIER_OR_NAME: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.NON_FINAL_MODIFIER_OR_NAME)

    val DELEGATED_SUPERTYPE_BY_KEYWORD: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DELEGATED_SUPERTYPE_BY_KEYWORD)

    val CALL_ELEMENT_WITH_DOT: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.CALL_ELEMENT_WITH_DOT)

    val TYPEALIAS_TYPE_REFERENCE: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TYPEALIAS_TYPE_REFERENCE)

    val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC)

    val TYPE_ARGUMENT_LIST_OR_SELF: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_SELF)

    val TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER)

    val PACKAGE_DIRECTIVE_NAME_EXPRESSION: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.PACKAGE_DIRECTIVE_NAME_EXPRESSION)

    val OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS)

    val DESTRUCTURING_DECLARATION_ENTRIES: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DESTRUCTURING_DECLARATION_ENTRIES)

    val DEPRECATION: KmpPositioningStrategy = KmpPositioningStrategy()
        .registerAs(PositioningStrategyKinds.DEPRECATION)
}
