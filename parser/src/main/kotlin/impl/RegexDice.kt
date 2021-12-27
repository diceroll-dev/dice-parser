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
package dev.diceroll.parser.impl

import dev.diceroll.parser.*

class RegexDice {

    companion object {

        private val INT = "[0-9]+"
        private val D = "[dD]"
        private val X = "[xX]"
        private val K = "[kK]"
        private val L = "[lL]"
        private val F = "[fF]"
        private val DOT = "\\."
        private val LPAREN = "\\("
        private val RPAREN = "\\)"
        private val LESS_THEN_EQUAL = "\\<"
        private val GREATER_THEN_EQUAL = "\\>"
        private val EQUAL = "="
        private val BANG = "!"

        private val DICE_FACE = "$D(?<FACES>$INT)" // d6
        private val N_DICE_FACE = "(?<numberOfDice>$INT)$DICE_FACE" // 2d6
        private val DICE_FACE_X = "$DICE_FACE$X" // d6
        private val N_DICE_FACE_X = "(?<numberOfDice>$INT)$DICE_FACE$X" // 2d6
        private val FUDGE_DICE = "$D$F" // dF
        private val N_FUDGE_DICE = "(?<numberOfDice>$INT)$D$F" // 2dF
        private val DOT_FUDGE_DICE = "$FUDGE_DICE$DOT(?<weight>$INT)" // dF.1
        private val N_DOT_FUDGE_DICE = "$N_FUDGE_DICE$DOT(?<weight>$INT)" // 2dF.1
        private val COMPOUND_DICE = "$N_DICE_FACE$BANG$BANG" // 3d6!!
        private val COMPOUND_DICE_TARGET = "$COMPOUND_DICE(?<comp>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL]?)(?<target>$INT)" // 3d6!!>5 or 3d6!!5
        private val EXPLODE_DICE = "$N_DICE_FACE$BANG" // 3d6!
        private val EXPLODE_DICE_TARGET = "$EXPLODE_DICE(?<comp>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL]?)(?<target>$INT)" // 3d6!>5 or 3d6!5
        private val KEEP_DICE = Regex("$N_DICE_FACE$K(?<keep>$INT)") // 4d6k2
        private val KEEP_LOW_DICE = Regex("$N_DICE_FACE$L(?<keep>$INT)") // 4d6k2
        private val TARGET_POOL = "$N_DICE_FACE(?<operator>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL])(?<target>$INT)" // 4d10>6
        private val TARGET_POOL_PARENS = "$LPAREN$N_DICE_FACE(?<operation>[\\+-]?)(?<modifier>$INT)$RPAREN(?<operator>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL])(?<target>$INT)" // (4d10+2)>6
        private val NESTED = "(?<LEFT>.*)$LPAREN(?<NESTED>.*)$RPAREN(?<RIGHT>.*)".toRegex() // 10(2) or (2) or 10(2)4
        private val MUL = "(?<left>.*)\\*(?<right>.*)".toRegex() // exp * exp
        private val DIV = "(?<left>.*)/(?<right>.*)".toRegex() // exp / exp
        private val ADD = "(?<left>.*)\\+(?<right>.*)".toRegex() // exp + exp
        private val SUB = "(?<left>.*)-(?<right>.*)".toRegex() // exp - exp
    }

    val parsers = linkedMapOf(

            N_DICE_FACE.toRegex() to this::visitNDiceFace,
            KEEP_DICE to this::visitKeepDice,
            KEEP_LOW_DICE to this::visitKeepLowDice,
            DICE_FACE.toRegex() to this::visitDiceFace,
            DICE_FACE_X.toRegex() to this::visitDiceFaceX,
            N_DICE_FACE_X.toRegex() to this::visitNDiceFaceX,
            FUDGE_DICE.toRegex() to this::visitFudgeDice,
            N_FUDGE_DICE.toRegex() to this::visitNFudgeDice,
            DOT_FUDGE_DICE.toRegex() to this::visitFudgeDiceDot,
            N_DOT_FUDGE_DICE.toRegex() to this::visitNFudgeDiceDot,
            COMPOUND_DICE.toRegex() to this::visitCompoundDice,
            COMPOUND_DICE_TARGET.toRegex() to this::visitCompoundDiceTarget,
            EXPLODE_DICE.toRegex() to this::visitExplode,
            EXPLODE_DICE_TARGET.toRegex() to this::visitExplodeTarget,
            TARGET_POOL.toRegex() to this::visitTargetPool,
            TARGET_POOL_PARENS.toRegex() to this::visitTargetPoolMod,
            NESTED to this::visitNested,
            ADD to this::visitAdd,
            SUB to this::visitSubtract,
            MUL to this::visitMultiply,
            DIV to this::visitDivide,
            INT.toRegex() to this::visitInt
    )

    fun parse(expression: String): DiceExpression {

        val trimmedExpression = expression.trim()
        val result = parsers.filter { it.key.matches(trimmedExpression) }
                .mapKeys { it.key.matchEntire(trimmedExpression) }
                .filterKeys { it != null }
                .map { Pair(it.key!!, it.value) }
                .firstOrNull()
                ?: throw ParseException("Failed to parse expression '$expression'")

        return result.second.invoke(result.first)
    }


    fun validExpression(expression: String): Boolean {
        val trimmedExpression = expression.trim()
        return parsers.filter { it.key.matches(trimmedExpression) }
                .filterKeys { true }
                .isNotEmpty()
    }

    private fun visitInt(match: MatchResult): DiceExpression {
        return NumberExpression(match.value.toInt())
    }

    private fun visitNested(match: MatchResult): DiceExpression {

        // three parts
        // left ( middle ) right
        // middle is non null, the others could be empty
        val middle = parse(match.groupValues[2])

        val left: DiceExpression = if (match.groupValues[1].isNotEmpty()) {
            MultiplyExpression(parse(match.groupValues[1]), middle)
        } else {
            middle
        }

        return if (match.groupValues[3].isNotEmpty()) {
            MultiplyExpression(left, parse(match.groupValues[3]))
        } else {
            left
        }
    }

    private fun visitDiceFace(match: MatchResult): DiceExpression {
        return visitDiceFace(match.groupValues[1])
    }

    private fun visitDiceFace(numberOfFaces: String): DiceExpression {
        return visitDiceFace(numberOfFaces.toInt())
    }

    private fun visitDiceFace(numberOfFaces: Int): DiceExpression {
        return NDice(numberOfFaces)
    }

    private fun visitNDiceFace(match: MatchResult): DiceExpression {
        val numberOfFaces = match.groupValues[2].toInt()
        val numberOfDice = match.groupValues[1].ifEmpty { "1" }.toInt()

        return NDice(numberOfFaces, numberOfDice)
    }

    private fun visitDiceFaceX(match: MatchResult): DiceExpression {
        val numberOfFaces = match.groupValues[1]
        return MultiplyExpression(visitDiceFace(numberOfFaces), visitDiceFace(numberOfFaces))
    }

    private fun visitNDiceFaceX(match: MatchResult): DiceExpression {
        val numberOfFaces = match.groupValues[2].toInt()
        val numberOfDice = match.groupValues[1].ifEmpty { "1" }.toInt()
        return MultiplyExpression(NDice(numberOfFaces, numberOfDice), NDice(numberOfFaces, numberOfDice))
    }

    private fun visitAdd(match: MatchResult): DiceExpression {
        val left = parse(match.groupValues[1])
        val right = parse(match.groupValues[2])
        return AddExpression(left, right)
    }

    private fun visitSubtract(match: MatchResult): DiceExpression {
        val left = parse(match.groupValues[1])
        val right = parse(match.groupValues[2])
        return SubtractExpression(left, right)
    }

    private fun visitMultiply(match: MatchResult): DiceExpression {
        val left = parse(match.groupValues[1])
        val right = parse(match.groupValues[2])
        return MultiplyExpression(left, right)
    }

    private fun visitDivide(match: MatchResult): DiceExpression {
        val left = parse(match.groupValues[1])
        val right = parse(match.groupValues[2])
        return DivideExpression(left, right)
    }

    private fun visitFudgeDice(match: MatchResult): DiceExpression {
        return FudgeDice()
    }

    private fun visitNFudgeDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        return FudgeDice(numberOfDice)
    }

    private fun visitFudgeDiceDot(match: MatchResult): DiceExpression {
        val weight = match.groupValues[1].toInt()
        return fudgeRoll(weight)
    }

    private fun visitNFudgeDiceDot(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val weight = match.groupValues[2].toInt()
        return FudgeDice(numberOfDice, 6, weight)
    }

    private fun fudgeRoll(weight: Int, sides: Int = 6): DiceExpression {
        return FudgeDice(1, weight)
    }

    private fun visitKeepDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val numberToKeep = match.groupValues[3].toInt()

        return KeepDice(numberOfFaces, numberOfDice, numberToKeep)
    }

    private fun visitKeepLowDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val numberToKeep = match.groupValues[3].toInt()

        return KeepLowDice(numberOfFaces, numberOfDice, numberToKeep)
    }

    fun comparisonFrom(text: String): Comparison {
        return when (text) {
            Comparison.GREATER_THAN.description -> Comparison.GREATER_THAN
            Comparison.LESS_THAN.description -> Comparison.LESS_THAN
            Comparison.EQUAL_TO.description -> Comparison.EQUAL_TO
            else -> throw IllegalArgumentException("Could not parse Comparison operator from '${text}'")
        }
    }

    private fun visitTargetPool(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val diceFace = match.groupValues[2].toInt()
        val comp = match.groupValues[3] // <, >, =
        val targetNumber = match.groupValues[4].toInt()

        return rollTargetPool(numberOfDice, diceFace, comp, targetNumber)
    }

    private fun visitTargetPoolMod(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val diceFace = match.groupValues[2].toInt()
        val operation = match.groupValues[3] // +, -
        val modifier = match.groupValues[4].toInt()
        val comp = match.groupValues[5] // <, >, =
        var targetNumber = match.groupValues[6].toInt()

        // an odd block but, basically 2d8+2<6 == 2d8<4
        if (operation == "+") {
            targetNumber -= modifier
        } else if (operation == "-") {
            targetNumber += modifier
        }

        return rollTargetPool(numberOfDice, diceFace, comp, targetNumber)
    }

    private fun rollTargetPool(numberOfDice: Int, numberOfFaces: Int, comp: String, targetNumber: Int): DiceExpression {
        return TargetPoolDice(numberOfFaces, numberOfDice, comparisonFrom(comp), targetNumber)
    }

    private fun visitCompoundDice(match: MatchResult): DiceExpression { // 3d6!!
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()

        return CompoundingDice(numberOfFaces, numberOfDice)
    }

    private fun visitCompoundDiceTarget(match: MatchResult): DiceExpression { // 3d6!!<5
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val comp = match.groupValues[3].ifEmpty { "=" }
        val target = match.groupValues[4].toInt()

        return CompoundingDice(numberOfFaces, numberOfDice, comparisonFrom(comp), target)
    }


    private fun visitExplode(match: MatchResult): DiceExpression { // 3d6!
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()

        return ExplodingDice(numberOfFaces, numberOfDice)
    }

    private fun visitExplodeTarget(match: MatchResult): DiceExpression { // 3d6!>5 or 3d6!5
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val comp = match.groupValues[3].ifEmpty { "=" }
        val target = match.groupValues[4].toInt()

        return ExplodingDice(numberOfFaces, numberOfDice, comparisonFrom(comp), target)
    }
}