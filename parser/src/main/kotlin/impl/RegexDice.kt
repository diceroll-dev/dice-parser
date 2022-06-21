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
import java.util.stream.Collectors

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
        private val SLASH = "/"
        private val CARET = "\\^"

        private val DICE_FACE = "$D(?<FACES>$INT)" // d6
        private val N_DICE_FACE = "(?<numberOfDice>$INT)$DICE_FACE" // 2d6
        private val DICE_FACE_X = "$DICE_FACE$X" // d6
        private val N_DICE_FACE_X = "(?<numberOfDice>$INT)$DICE_FACE$X" // 2d6
        private val FUDGE_DICE = "$D$F" // dF
        private val N_FUDGE_DICE = "(?<numberOfDice>$INT)$D$F" // 2dF
        private val DOT_FUDGE_DICE = "$FUDGE_DICE$DOT(?<weight>$INT)" // dF.1
        private val N_DOT_FUDGE_DICE = "$N_FUDGE_DICE$DOT(?<weight>$INT)" // 2dF.1
        private val COMPOUND_DICE = "$N_DICE_FACE$BANG$BANG" // 3d6!!
        private val COMPOUND_DICE_TARGET =
            "$COMPOUND_DICE(?<comp>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL]?)(?<target>$INT)" // 3d6!!>5 or 3d6!!5
        private val EXPLODE_DICE = "$N_DICE_FACE$BANG" // 3d6!
        private val EXPLODE_ADD_DICE = "$N_DICE_FACE$CARET" // 3d6^
        private val EXPLODE_DICE_TARGET =
            "$EXPLODE_DICE(?<comp>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL]?)(?<target>$INT)" // 3d6!>5 or 3d6!5
        private val KEEP_DICE = Regex("$N_DICE_FACE$K(?<keep>$INT)") // 4d6k2
        private val KEEP_LOW_DICE = Regex("$N_DICE_FACE$L(?<keep>$INT)") // 4d6k2
        private val TARGET_POOL =
            "(?<left>.+)(?<operator>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL])(?<target>$INT)"
        private val NESTED = "(?<LEFT>.*)$LPAREN(?<NESTED>.*)$RPAREN(?<RIGHT>.*)".toRegex() // 10(2) or (2) or 10(2)4
        private val MUL = "(?<left>.+)\\*(?<right>.+)".toRegex() // exp * exp
        private val DIV = "(?<left>.+)/(?<right>.+)".toRegex() // exp / exp
        private val ADD = "(?<left>.+)\\+(?<right>.+)".toRegex() // exp + exp
        private val SUB = "(?<left>.+)-(?<right>.+)".toRegex() // exp - exp
        private val NEGATIVE = "-.+".toRegex() // -
        private val SORT = "(.+)(asc|desc)".toRegex() // sorting
        private val MIN = "(?<left>.+)min(?<right>.+)".toRegex() // 10min1d6
        private val MAX = "(?<left>.+)max(?<right>.+)".toRegex() // 10max1d6
        private val CUSTOM_DIE = "d\\[(?<diceSides>$INT($SLASH$INT)+)]".toRegex() // d[1/2/3/4]
        private val N_CUSTOM_DIE = "(?<numberOfDice>$INT)$CUSTOM_DIE" // 3d[1/2/3/4]
    }

    private val parsers = linkedMapOf(
        SORT to this::visitSort,
        N_DICE_FACE.toRegex() to this::visitNDiceFace,
        N_CUSTOM_DIE.toRegex() to this::visitNCustomDice,
        KEEP_DICE to this::visitKeepDice,
        KEEP_LOW_DICE to this::visitKeepLowDice,
        DICE_FACE.toRegex() to this::visitDiceFace,
        CUSTOM_DIE to this::visitCustomDice,
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
        EXPLODE_ADD_DICE.toRegex() to this::visitExplodeAdd,
        TARGET_POOL.toRegex() to this::visitTargetFilter,
        MIN to this::visitMin,
        MAX to this::visitMax,
        NESTED to this::visitNested,
        ADD to this::visitAdd,
        SUB to this::visitSubtract,
        MUL to this::visitMultiply,
        DIV to this::visitDivide,
        NEGATIVE to this::visitNegative,
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

    private fun visitCustomDice(match: MatchResult): DiceExpression {
        val faces = match.groupValues[1].split(SLASH).stream()
            .map { s -> s.trim() }
            .filter { s -> s != null }
            .filter { s -> s.toIntOrNull() != null }
            .map { s -> s.toInt() }
            .collect(Collectors.toList())
        return CustomDice(1, faces)
    }

    private fun visitNCustomDice(match: MatchResult): DiceExpression {
        val faces = match.groupValues[2].split(SLASH).stream()
            .map { s -> s.trim() }
            .filter { s -> s.toIntOrNull() != null }
            .map { s -> s.toInt() }
            .collect(Collectors.toList())
        val numberOfDice = match.groupValues[1].ifEmpty { "1" }.toInt()

        return CustomDice(numberOfDice, faces)
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

    private fun comparisonFrom(text: String): Comparison {
        return when (text) {
            Comparison.GREATER_EQUAL_THAN.description -> Comparison.GREATER_EQUAL_THAN
            Comparison.LESS_EQUAL_THAN.description -> Comparison.LESS_EQUAL_THAN
            Comparison.EQUAL_TO.description -> Comparison.EQUAL_TO
            else -> throw IllegalArgumentException("Could not parse Comparison operator from '${text}'")
        }
    }

    private fun visitTargetFilter(match: MatchResult): DiceExpression {
        val expression = match.groupValues[1]
        val comp = match.groupValues[2] // <, >, =
        val targetNumber = match.groupValues[3].toInt()

        return TargetPoolExpression(parse(expression), comparisonFrom(comp), targetNumber)
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

    private fun visitExplodeAdd(match: MatchResult): DiceExpression { // 3d6^
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()

        return ExplodingAddDice(numberOfFaces, numberOfDice)
    }

    private fun visitExplodeTarget(match: MatchResult): DiceExpression { // 3d6!>5 or 3d6!5
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val comp = match.groupValues[3].ifEmpty { "=" }
        val target = match.groupValues[4].toInt()

        return ExplodingDice(numberOfFaces, numberOfDice, comparisonFrom(comp), target)
    }

    private fun visitNegative(match: MatchResult): DiceExpression { // -1, -1d6
        return NegativeDiceExpression(parse(match.value.trim().substring(1)))
    }

    private fun visitSort(match: MatchResult): DiceExpression { // asc or desc
        val diceExpression = match.groupValues[1]
        val order = match.groupValues[2]
        return SortedDiceExpression(parse(diceExpression), order == "asc")
    }

    private fun visitMin(match: MatchResult): DiceExpression { //10min1d6
        val left = match.groupValues[1]
        val right = match.groupValues[2]
        return MinDiceExpression(parse(left), parse(right))
    }

    private fun visitMax(match: MatchResult): DiceExpression { // 10max1d6
        val left = match.groupValues[1]
        val right = match.groupValues[2]
        return MaxDiceExpression(parse(left), parse(right))
    }
}