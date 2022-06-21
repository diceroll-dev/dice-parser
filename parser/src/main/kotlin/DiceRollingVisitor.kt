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

import java.util.stream.Collectors
import kotlin.random.Random

private val defaultRandomGenerator: (Int) -> Int = { numberOfFaces: Int ->
    Random.nextInt(numberOfFaces) + 1
}

class DiceRollingVisitor(private val randomGenerator: (Int) -> Int) : DiceVisitor<ResultTree> {

    constructor() : this(defaultRandomGenerator)

    private fun random(numberOfFaces: Int): Int {
        return randomGenerator.invoke(numberOfFaces)
    }

    override fun visit(value: NumberExpression): ResultTree {
        return ResultTree(value, value.value)
    }

    override fun visit(mathExpressionResult: MathExpression): ResultTree {

        val left = visit(mathExpressionResult.left)
        val right = visit(mathExpressionResult.right)

        val result = when (mathExpressionResult.operation) {
            Operation.ADD -> Math.addExact(left.value, right.value)
            Operation.SUBTRACT -> Math.subtractExact(left.value, right.value)
            Operation.MULTIPLY -> Math.multiplyExact(left.value, right.value)
            Operation.DIVIDE -> Math.floorDiv(left.value, right.value)
        }

        return ResultTree(mathExpressionResult, result, listOf(left, right))
    }

    override fun visit(negativeDiceExpression: NegativeDiceExpression): ResultTree {

        val resultTree = makeNegative(visit(negativeDiceExpression.value))

        return ResultTree(negativeDiceExpression, resultTree.value, resultTree.results)
    }

    private fun makeNegative(resultTree: ResultTree): ResultTree {
        val expression = resultTree.expression
        val value = Math.multiplyExact(resultTree.value, -1)
        val results = resultTree.results.stream()
            .map { r -> makeNegative(r) }
            .collect(Collectors.toList())
        return ResultTree(expression, value, results)
    }

    override fun visit(sortedDiceExpression: SortedDiceExpression): ResultTree {
        val resultTree = orderResultTree(visit(sortedDiceExpression.value), sortedDiceExpression.sortAscending)
        return ResultTree(sortedDiceExpression, resultTree.value, resultTree.results)
    }

    private fun orderResultTree(resultTree: ResultTree, ascending: Boolean): ResultTree {
        val orderComparator =
            if (ascending) Comparator.comparing(ResultTree::value) else Comparator.comparing(ResultTree::value)
                .reversed()
        val results = resultTree.results.stream()
            .map { r -> orderResultTree(r, ascending) }
            .sorted(orderComparator)
            .collect(Collectors.toList())
        return ResultTree(resultTree.expression, resultTree.value, results)
    }

    override fun visit(minDiceExpression: MinDiceExpression): ResultTree {
        val left = visit(minDiceExpression.left)
        val right = visit(minDiceExpression.right)
        val value = if (left.value > right.value) {
            right.value
        } else {
            left.value
        }
        return ResultTree(minDiceExpression, value, listOf(left, right))
    }

    override fun visit(targetPoolExpression: TargetPoolExpression): ResultTree {
        val left = visit(targetPoolExpression.left)
        val comparison = targetPoolExpression.comparison
        val target = targetPoolExpression.target
        val matchingSubResults = left.results
            .filter { predicate(comparison, target)(it.value) }
            .toList()
        return ResultTree(targetPoolExpression, matchingSubResults.size, matchingSubResults)
    }

    override fun visit(maxDiceExpression: MaxDiceExpression): ResultTree {
        val left = visit(maxDiceExpression.left)
        val right = visit(maxDiceExpression.right)
        val value = if (left.value > right.value) {
            left.value
        } else {
            right.value
        }
        return ResultTree(maxDiceExpression, value, listOf(left, right))
    }

    override fun visit(nDice: NDice): ResultTree {
        val values = IntRange(1, nDice.numberOfDice)
            .map { random(nDice.numberOfFaces) }
            .map { ResultTree(NDice(nDice.numberOfFaces), it) }

        return ResultTree(nDice, values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(), values)
    }

    override fun visit(diceX: DiceX): ResultTree {
        val nDice = NDice(diceX.numberOfFaces, diceX.numberOfDice)
        val left = visit(nDice)
        val right = visit(nDice)

        return ResultTree(diceX, Math.multiplyExact(left.value, right.value), listOf(left, right))
    }

    override fun visit(fudgeDice: FudgeDice): ResultTree {
        val values = IntRange(1, fudgeDice.numberOfDice)
            .map { doFudgeRoll(fudgeDice.numberOfFaces, fudgeDice.weight) }
            .map { ResultTree(FudgeDice(1, fudgeDice.numberOfFaces, fudgeDice.weight), it) }

        return ResultTree(
            fudgeDice,
            values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }

    override fun visit(keepDice: KeepDice): ResultTree {
        val values = IntRange(1, keepDice.numberOfDice)
            .map { random(keepDice.numberOfFaces) }
            .map { ResultTree(NDice(keepDice.numberOfFaces), it) }

        return ResultTree(
            keepDice,
            values.map { it.value }.sorted().reversed().take(keepDice.numberToKeep).stream()
                .reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }

    override fun visit(keepLowDice: KeepLowDice): ResultTree {
        val values = IntRange(1, keepLowDice.numberOfDice)
            .map { random(keepLowDice.numberOfFaces) }
            .map { ResultTree(NDice(keepLowDice.numberOfFaces), it) }

        return ResultTree(
            keepLowDice,
            values.map { it.value }.sorted().take(keepLowDice.numberToKeep).stream()
                .reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }

    override fun visit(explodingDice: ExplodingDice): ResultTree {
        val values = explodeRoll(
            explodingDice.numberOfDice,
            explodingDice.numberOfFaces,
            predicate(explodingDice.comparison, explodingDice.target)
        )
            .map { ResultTree(NDice(explodingDice.numberOfFaces), it) }
        return ResultTree(
            explodingDice,
            values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }

    override fun visit(explodingAddDice: ExplodingAddDice): ResultTree {
        val values = explodeRollAdd(
            explodingAddDice.numberOfDice,
            explodingAddDice.numberOfFaces,
            predicate(explodingAddDice.comparison, explodingAddDice.target)
        )
            .map { ResultTree(NDice(explodingAddDice.numberOfFaces), it) }
        return ResultTree(
            explodingAddDice,
            values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }

    override fun visit(compoundingDice: CompoundingDice): ResultTree {

        val values = compoundRoll(
            compoundingDice.numberOfDice,
            compoundingDice.numberOfFaces,
            predicate(compoundingDice.comparison, compoundingDice.target),
            100
        )
            .map { ResultTree(NDice(compoundingDice.numberOfFaces), it) }
        return ResultTree(
            compoundingDice,
            values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
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

    private fun explodeRollAdd(numberOfDice: Int, numberOfFaces: Int, predicate: (Int) -> Boolean): List<Int> {

        return IntRange(1, numberOfDice)
            .map { recursiveAddRoll(numberOfFaces, predicate, 50) }
    }

    private fun recursiveAddRoll(numberOfFaces: Int, predicate: (Int) -> Boolean, maxRolls: Int): Int {

        check(maxRolls >= 0) {
            "Dice exploded too many times in a role, one of the following likely " +
                    "happened, your random number generator isn't actually random, you tried to explode a 'd1', " +
                    "or you just happened to be really lucky"
        }

        val result = random(numberOfFaces)
        return if (predicate.invoke(result)) {
            recursiveAddRoll(numberOfFaces, predicate, maxRolls - 1) + result
        } else result
    }


    private fun compoundRoll(
        numberOfDice: Int,
        numberOfFaces: Int,
        predicate: (Int) -> Boolean,
        maxRolls: Int
    ): List<Int> {

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
            Comparison.GREATER_EQUAL_THAN -> {
                { it >= target }
            }
            Comparison.LESS_EQUAL_THAN -> {
                { it <= target }
            }
            Comparison.EQUAL_TO -> {
                { it == target }
            }
        }
    }

    private fun doFudgeRoll(sides: Int = 6, weight: Int = Math.floorDiv(sides, 3)): Int {
        val random = random(sides)
        return when {
            random > Math.subtractExact(sides, weight) -> {
                1
            }
            random > Math.subtractExact(sides, Math.multiplyExact(weight, 2)) -> {
                -1
            }
            else -> 0
        }
    }

    override fun visit(customDice: CustomDice): ResultTree {
        val values = IntRange(1, customDice.numberOfDice)
            .map { random(customDice.dieFaces.size) }
            .map { i -> customDice.dieFaces[i - 1] }
            .map { ResultTree(CustomDice(1, customDice.dieFaces), it) }

        return ResultTree(
            customDice,
            values.map { it.value }.stream().reduce { x, y -> Math.addExact(x, y) }.get(),
            values
        )
    }
}