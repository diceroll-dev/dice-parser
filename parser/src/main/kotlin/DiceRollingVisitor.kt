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

import kotlin.random.Random

private val defaultRandomGenerator: (Int) -> Int = { numberOfFaces: Int ->
    Random.nextInt(numberOfFaces) + 1
}

class DiceRollingVisitor(private val randomGenerator: (Int) -> Int) : DiceVisitor<ResultTree> {

    constructor(): this(defaultRandomGenerator)

    private fun random(numberOfFaces: Int): Int {
        return randomGenerator.invoke(numberOfFaces)
    }

    override fun visit(value: NumberExpression): ResultTree {
        return ResultTree(value, value.value)
    }

    override fun visit(mathExpressionResult: MathExpression): ResultTree {

        val left = visit(mathExpressionResult.left)
        val right = visit(mathExpressionResult.right)

        val result = when(mathExpressionResult.operation) {
            Operation.ADD -> left.value + right.value
            Operation.SUBTRACT -> left.value - right.value
            Operation.MULTIPLY -> left.value * right.value
            Operation.DIVIDE -> left.value / right.value
        }

        return ResultTree(mathExpressionResult, result, listOf(left, right))
    }

    override fun visit(nDice: NDice): ResultTree {
        val values = IntRange(1, nDice.numberOfDice)
                .map { random(nDice.numberOfFaces) }
                .map { ResultTree(NDice(nDice.numberOfFaces), it)}

        return ResultTree(nDice, values.map { it.value }.sum(), values)
    }

    override fun visit(diceX: DiceX): ResultTree {
        val nDice = NDice(diceX.numberOfFaces, diceX.numberOfDice)
        val left = visit(nDice)
        val right = visit(nDice)

        return ResultTree(diceX, left.value * right.value, listOf(left, right))
    }

    override fun visit(fudgeDice: FudgeDice): ResultTree {
        val values = IntRange(1, fudgeDice.numberOfDice)
                .map { doFudgeRoll(fudgeDice.numberOfFaces, fudgeDice.weight) }
                .map { ResultTree(FudgeDice(1, fudgeDice.numberOfFaces, fudgeDice.weight), it)}

        return ResultTree(fudgeDice, values.map { it.value }.sum(), values)
    }

    override fun visit(keepDice: KeepDice): ResultTree {
        val values = IntRange(1, keepDice.numberOfDice)
                .map { random(keepDice.numberOfFaces) }
                .map { ResultTree(NDice(keepDice.numberOfFaces), it)}

        return ResultTree(keepDice, values.map { it.value }.sorted().reversed().take(keepDice.numberToKeep).sum(), values)
    }

    override fun visit(keepLowDice: KeepLowDice): ResultTree {
        val values = IntRange(1, keepLowDice.numberOfDice)
                .map { random(keepLowDice.numberOfFaces) }
                .map { ResultTree(NDice(keepLowDice.numberOfFaces), it)}

        return ResultTree(keepLowDice, values.map { it.value }.sorted().take(keepLowDice.numberToKeep).sum(), values)
    }

    override fun visit(explodingDice: ExplodingDice): ResultTree {
        val values = explodeRoll(explodingDice.numberOfDice, explodingDice.numberOfFaces, predicate(explodingDice.comparison, explodingDice.target))
                .map { ResultTree(NDice(explodingDice.numberOfFaces), it)}
        return ResultTree(explodingDice, values.map { it.value }.sum(), values)
    }

    override fun visit(compoundingDice: CompoundingDice): ResultTree {

        val values = compoundRoll(compoundingDice.numberOfDice, compoundingDice.numberOfFaces, predicate(compoundingDice.comparison, compoundingDice.target), 100)
                .map { ResultTree(NDice(compoundingDice.numberOfFaces), it)}
        return ResultTree(compoundingDice, values.map { it.value }.sum(), values)
    }

    override fun visit(targetPoolDice: TargetPoolDice): ResultTree {
        val values = IntRange(1, targetPoolDice.numberOfDice)
                .map { random(targetPoolDice.numberOfFaces) }
                .map { ResultTree(NDice(targetPoolDice.numberOfFaces), it)}
        val value = values.map { it.value }.filter(predicate(targetPoolDice.comparison, targetPoolDice.target)).count()
        return ResultTree(targetPoolDice, value, values)
    }

    private fun explodeRoll(numberOfDice: Int, numberOfFaces: Int, predicate: (Int) -> Boolean): List<Int> {

        return IntRange(1, numberOfDice)
                .flatMap { recursiveRoll(numberOfFaces, predicate, 50) }
    }

    private fun recursiveRoll(numberOfFaces: Int, predicate: (Int) -> Boolean, maxRolls: Int): List<Int> {

        check(maxRolls >= 0) {
            "Dice exploded too many times in a role, one of the following likely " +
                    "happened, your random number generator isn't actually random, you tried to explode a 'd1', " +
                    "or you just happened to be really lucky"
        }

        val result = random(numberOfFaces)
        return if (predicate.invoke(result)) {
            recursiveRoll(numberOfFaces, predicate, maxRolls - 1) + result
        } else listOf(result)
    }

    private fun compoundRoll(numberOfDice: Int, numberOfFaces: Int, predicate: (Int) -> Boolean, maxRolls: Int): List<Int> {

        check(maxRolls >= 0) {
            "Dice compound too many times in a role, one of the following likely " +
                    "happened, your random number generator isn't actually random, you tried to explode a 'd1', " +
                    "or you just happened to be really lucky"
        }

        val dice = IntRange(1, numberOfDice)
                .map { random(numberOfFaces) }

        // re-roll everything for each die that matches the predicate
        val anotherRole = dice
                .filter(predicate)
                .map { compoundRoll(numberOfDice, numberOfFaces, predicate, maxRolls - 1) }
                .flatten()

        return dice + anotherRole
    }

    private fun predicate(comparison: Comparison, target: Int): (Int) -> Boolean {
        return when (comparison) {
            Comparison.GREATER_THAN -> {{ it >= target }}
            Comparison.LESS_THAN -> {{ it <= target }}
            Comparison.EQUAL_TO -> {{ it == target }}
        }
    }

    private fun doFudgeRoll(sides: Int = 6, weight: Int = sides/3): Int {
        val random = random(sides)
        return when {
            random > sides - weight -> { 1 }
            random > sides - weight * 2 -> { -1 }
            else -> 0
        }
    }
}