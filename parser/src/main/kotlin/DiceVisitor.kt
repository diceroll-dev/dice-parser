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


interface DiceVisitor<T> {

    fun visit(diceExpression: DiceExpression): T {
        return when (diceExpression) {
            is NumberExpression -> visit(diceExpression)
            is MathExpression -> visit(diceExpression)
            is NDice -> visit(diceExpression)
            is DiceX -> visit(diceExpression)
            is FudgeDice -> visit(diceExpression)
            is ExplodingDice -> visit(diceExpression)
            is ExplodingAddDice -> visit(diceExpression)
            is CompoundingDice -> visit(diceExpression)
            is KeepDice -> visit(diceExpression)
            is KeepLowDice -> visit(diceExpression)
            is NegativeDiceExpression -> visit(diceExpression)
            is SortedDiceExpression -> visit(diceExpression)
            is MinDiceExpression -> visit(diceExpression)
            is MaxDiceExpression -> visit(diceExpression)
            is CustomDice -> visit(diceExpression)
            is TargetPoolExpression -> visit(diceExpression)
            else -> throw NotImplementedError("Could not visit unknown type: ${diceExpression::class}")
        }
    }

    fun visit(value: NumberExpression): T

    fun visit(mathExpressionResult: MathExpression): T

    fun visit(nDice: NDice): T

    fun visit(diceX: DiceX): T

    fun visit(fudgeDice: FudgeDice): T

    fun visit(keepDice: KeepDice): T

    fun visit(explodingDice: ExplodingDice): T

    fun visit(explodingAddDice: ExplodingAddDice): T

    fun visit(compoundingDice: CompoundingDice): T

    fun visit(keepLowDice: KeepLowDice): T

    fun visit(negativeDiceExpression: NegativeDiceExpression): T

    fun visit(sortedDiceExpression: SortedDiceExpression): T

    fun visit(minDiceExpression: MinDiceExpression): T

    fun visit(maxDiceExpression: MaxDiceExpression): T
    fun visit(customDice: CustomDice): T
    fun visit(targetPoolExpression: TargetPoolExpression): T
}