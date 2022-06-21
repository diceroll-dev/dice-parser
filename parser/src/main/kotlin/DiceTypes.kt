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

class CustomDice(val numberOfDice: Int = 1, val dieFaces: List<Int>) : DiceExpression {
    override fun description(): String {
        val prefix = when (numberOfDice) {
            1 -> ""
            else -> numberOfDice
        }
        return "${prefix}d${dieFaces}"
    }
}

class NDice(numberOfFaces: Int, numberOfDice: Int = 1) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        val prefix = when (numberOfDice) {
            1 -> ""
            else -> numberOfDice
        }
        return "${prefix}d${numberOfFaces}"
    }
}

class DiceX(numberOfFaces: Int, numberOfDice: Int) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        val prefix = when (numberOfDice) {
            1 -> ""
            else -> numberOfDice
        }
        return "${prefix}d${numberOfFaces}X"
    }
}

class FudgeDice(
    val numberOfDice: Int = 1,
    val numberOfFaces: Int = 6,
    val weight: Int = Math.floorDiv(numberOfFaces, 3)
) : DiceExpression {
    override fun description(): String {
        val extra = if (weight != Math.floorDiv(numberOfFaces, 3)) {
            ".${weight}"
        } else {
            ""
        }
        return "${numberOfDice}dF${extra}"
    }
}

class KeepDice(numberOfFaces: Int, numberOfDice: Int, val numberToKeep: Int) :
    BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        return "${numberOfDice}d${numberOfFaces}k${numberToKeep}"
    }
}

class KeepLowDice(numberOfFaces: Int, numberOfDice: Int, val numberToKeep: Int) :
    BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        return "${numberOfDice}d${numberOfFaces}l${numberToKeep}"
    }
}

class ExplodingDice(
    numberOfFaces: Int,
    numberOfDice: Int,
    val comparison: Comparison = Comparison.EQUAL_TO,
    val target: Int = numberOfFaces
) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        val extra = if (numberOfFaces == target && comparison == Comparison.EQUAL_TO) {
            ""
        } else {
            "${comparison.description}${target}"
        }
        return "${numberOfDice}d${numberOfFaces}!${extra}"
    }
}

class ExplodingAddDice(
    numberOfFaces: Int,
    numberOfDice: Int,
    val comparison: Comparison = Comparison.EQUAL_TO,
    val target: Int = numberOfFaces
) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        val extra = if (numberOfFaces == target && comparison == Comparison.EQUAL_TO) {
            ""
        } else {
            "${comparison.description}${target}"
        }
        return "${numberOfDice}d${numberOfFaces}^${extra}"
    }
}

class CompoundingDice(
    numberOfFaces: Int, numberOfDice: Int,
    val comparison: Comparison = Comparison.EQUAL_TO,
    val target: Int = numberOfFaces
) : BaseDiceExpression(numberOfFaces, numberOfDice) {
    override fun description(): String {
        val extra = if (numberOfFaces == target && comparison == Comparison.EQUAL_TO) {
            ""
        } else {
            "${comparison.description}${target}"
        }
        return "${numberOfDice}d${numberOfFaces}!!${extra}"
    }
}

open class MathExpression(val left: DiceExpression, val operation: Operation, val right: DiceExpression) :
    DiceExpression {
    override fun description(): String {
        return "${left.description()} ${operation.description} ${right.description()}"
    }
}

class AddExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.ADD, right)
class SubtractExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.SUBTRACT, right)
class MultiplyExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.MULTIPLY, right)
class DivideExpression(left: DiceExpression, right: DiceExpression) : MathExpression(left, Operation.DIVIDE, right)

class NumberExpression(val value: Int) : DiceExpression {
    override fun description(): String {
        return value.toString()
    }
}

class NegativeDiceExpression(val value: DiceExpression) : DiceExpression {
    override fun description(): String {
        return "-" + value.description()
    }
}

class SortedDiceExpression(val value: DiceExpression, val sortAscending: Boolean) : DiceExpression {
    override fun description(): String {
        val order = if (sortAscending) "asc" else "desc"
        return value.description() + " " + order
    }
}

class TargetPoolExpression(val left: DiceExpression, val comparison: Comparison, val target: Int) : DiceExpression {
    override fun description(): String {
        return left.description() + comparison + target
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

interface DiceExpression {
    fun description(): String
}

abstract class BaseDiceExpression(val numberOfFaces: Int, val numberOfDice: Int) : DiceExpression