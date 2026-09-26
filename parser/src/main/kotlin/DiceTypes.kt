/*
 * Copyright 2020-Present Dice Parser
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.diceroll.parser

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class NDice @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int = 1, label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val prefix = when (numberOfDice) {
            1 -> ""
            else -> numberOfDice
        }
        return "${prefix}d${numberOfFaces}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class DiceX @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int, label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val prefix = when (numberOfDice) {
            1 -> ""
            else -> numberOfDice
        }
        return "${prefix}d${numberOfFaces}X${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class FudgeDice @JvmOverloads constructor(val numberOfDice: Int = 1, val numberOfFaces: Int = 6, val weight: Int = Math.floorDiv(numberOfFaces, 3), label: String? = null) : DiceExpression {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val extra = if (weight != Math.floorDiv(numberOfFaces, 3)) {
            ".${weight}"
        } else {
            ""
        }
        return "${numberOfDice}dF${extra}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class KeepDice @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int, val numberToKeep: Int, label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        return "${numberOfDice}d${numberOfFaces}k${numberToKeep}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class KeepLowDice @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int, val numberToKeep: Int, label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        return "${numberOfDice}d${numberOfFaces}l${numberToKeep}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class ExplodingDice @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int, val comparison: Comparison = Comparison.EQUAL_TO, val target: Int = numberOfFaces, label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val extra = if (numberOfFaces == target && comparison == Comparison.EQUAL_TO) {
            ""
        } else {
            "${comparison.description}${target}"
        }
        return "${numberOfDice}d${numberOfFaces}!${extra}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class CompoundingDice @JvmOverloads constructor(numberOfFaces: Int, numberOfDice: Int,
                      val comparison: Comparison = Comparison.EQUAL_TO,
                      val target: Int = numberOfFaces,
                      label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val extra = if (numberOfFaces == target && comparison == Comparison.EQUAL_TO) {
            ""
        } else {
            "${comparison.description}${target}"
        }
        return "${numberOfDice}d${numberOfFaces}!!${extra}${labelSuffix(label)}"
    }
}

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class TargetPoolDice @JvmOverloads constructor(numberOfFaces: Int,
                     numberOfDice: Int,
                     val comparison: Comparison,
                     val target: Int,
                     val modifier: Int = 0,
                     label: String? = null) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        val pool = if (modifier == 0) {
            "${numberOfDice}d${numberOfFaces}${comparison.description}${target}"
        } else {
            val extra = if (modifier > 0) {
                "+"
            } else ""
            "(${numberOfDice}d${numberOfFaces}${extra}${modifier})${comparison.description}${target}"
        }
        return pool + labelSuffix(label)
    }
}

open class MathExpression(val left: DiceExpression, val operation: Operation, val right: DiceExpression) : DiceExpression {
    override fun description(): String {
        return "${left.description()} ${operation.description} ${right.description()}"
    }
}

class AddExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.ADD, right)
class SubtractExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.SUBTRACT, right)
class MultiplyExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.MULTIPLY, right)
class DivideExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.DIVIDE, right)

/**
 * @throws IllegalArgumentException if [label] breaks the label rules (see [DiceExpression.label])
 */
class NumberExpression @JvmOverloads constructor(val value: Int, label: String? = null) : DiceExpression {
    override val label: String? = normalizeLabel(label)

    override fun description(): String {
        return value.toString() + labelSuffix(label)
    }
}

class NegativeDiceExpression(val value: DiceExpression) : DiceExpression {
    override val label: String?
        get() = value.label

    override fun description(): String {
        return "-" + value.description()
    }
}

class SortedDiceExpression(val value: DiceExpression, val sortAscending: Boolean) : DiceExpression {
    override val label: String?
        get() = value.label

    override fun description(): String {
        val order = if (sortAscending) "asc" else "desc"
        return value.description() + " " + order
    }
}

class MinDiceExpression(val left: DiceExpression, val right: DiceExpression) : DiceExpression {
    override fun description(): String {
        return "min(" + left.description() + "," + right + ")"
    }
}

class MaxDiceExpression(val left: DiceExpression, val right: DiceExpression) : DiceExpression {
    override fun description(): String {
        return "max(" + left.description() + "," + right + ")"
    }
}

/**
 * A labelled parenthesised group, e.g. `(1d6 + 2)[fire]`. Unlabelled parentheses do not produce a node.
 *
 * @throws IllegalArgumentException if [label] is blank or breaks the label rules (see [DiceExpression.label])
 */
class GroupExpression(val value: DiceExpression, label: String) : DiceExpression {
    override val label: String = requireNotNull(normalizeLabel(label)) { "A group label cannot be blank" }

    override fun description(): String {
        return "(${value.description()})[$label]"
    }
}

interface DiceExpression {
    fun description(): String

    /**
     * The optional label of this expression, e.g. `slashing` for `1d8[slashing]`, or `null` when there is none.
     *
     * The label is caller-supplied text and is NOT escaped. For labels from this library's parser and classes,
     * surrounding whitespace is trimmed, and the label is at most 64 Unicode code points of letters, combining marks,
     * decimal digits, symbols and emoji (`\p{So}`, skin-tone modifiers and the zero-width joiner of emoji sequences),
     * spaces and `- _ ' . , : / + ( ) # & ! ?`. Custom [DiceExpression] implementations may return
     * anything. Escape or validate it for the target context (HTML, Markdown, logs, ...) before rendering, logging,
     * or using it as an identifier or path. [ParseException] messages quote the input verbatim and must be escaped too.
     */
    val label: String?
        get() = null
}

abstract class BaseDiceExpression(val numberOfFaces: Int, val numberOfDice: Int) : DiceExpression

private fun labelSuffix(label: String?): String {
    return if (label == null) "" else "[$label]"
}

private const val MAX_LABEL_LENGTH = 64

// one allowed code point: a letter, combining mark, decimal digit, other symbol (\p{So}, e.g. emoji), emoji skin-tone
// modifier U+1F3FB..U+1F3FF, U+200D zero-width joiner (for emoji ZWJ sequences), U+0020 space
// or one of - _ ' . , : / + ( ) # & ! ?
// U+FE0F (emoji presentation selector) is already a \p{M} mark. \p{Sk} as a whole is NOT allowed, it holds ` and ^.
private val ALLOWED_LABEL_CODE_POINT = Regex("[\\p{L}\\p{M}\\p{Nd}\\p{So}\\x{1F3FB}-\\x{1F3FF}\\x{200D} \\-_'.,:/+()#&!?]")

/**
 * The single home of the label rules, shared by the parser and [normalizeLabel].
 * Returns why [label] is not allowed after its surrounding whitespace is trimmed, or `null` when it is allowed
 * (a blank label is allowed and means no label). Characters are checked before the length.
 * Positions count Unicode code points from 1 in the untrimmed [label].
 */
@JvmSynthetic
internal fun labelViolation(label: String): String? {
    val trimmed = label.trim()
    val leading = label.length - label.trimStart().length
    var index = 0
    while (index < trimmed.length) {
        val codePoint = trimmed.codePointAt(index)
        if (!ALLOWED_LABEL_CODE_POINT.matches(String(Character.toChars(codePoint)))) {
            val position = label.codePointCount(0, leading + index) + 1
            return "character ${"U+%04X".format(codePoint)} at position $position of the label is not allowed, " +
                    "labels may only contain letters, combining marks, decimal digits, symbols and emoji, spaces and - _ ' . , : / + ( ) # & ! ?"
        }
        index += Character.charCount(codePoint)
    }
    val length = trimmed.codePointCount(0, trimmed.length)
    if (length > MAX_LABEL_LENGTH) {
        return "labels are limited to $MAX_LABEL_LENGTH Unicode code points, this one has $length"
    }
    return null
}

/**
 * Trims the surrounding whitespace of [label], a blank label becomes `null`.
 *
 * @throws IllegalArgumentException if the trimmed label breaks the label rules
 */
@JvmSynthetic
internal fun normalizeLabel(label: String?): String? {
    if (label == null) {
        return null
    }
    val violation = labelViolation(label)
    require(violation == null) { "Invalid label '$label', $violation" }
    return label.trim().ifEmpty { null }
}
