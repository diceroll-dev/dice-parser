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

        // an operator is not inside a label when no `]` follows it before the next `[`, so label text like
        // `1d6 + 2` or `min/max` never splits an expression
        private val NOT_IN_LABEL = "(?![^\\[]*\\])"

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
        private val TARGET_POOL_PARENS = "$LPAREN$N_DICE_FACE(?<operation>[+-]?)(?<modifier>$INT)$RPAREN(?<operator>[$LESS_THEN_EQUAL$GREATER_THEN_EQUAL$EQUAL])(?<target>$INT)" // (4d10+2)>6
        private val NESTED = "(?<LEFT>.*)$LPAREN$NOT_IN_LABEL(?<NESTED>.*)$RPAREN$NOT_IN_LABEL(?<RIGHT>.*)".toRegex() // 10(2) or (2) or 10(2)4
        private val MUL = "(?<left>.+)\\*$NOT_IN_LABEL(?<right>.+)".toRegex() // exp * exp
        private val DIV = "(?<left>.+)/$NOT_IN_LABEL(?<right>.+)".toRegex() // exp / exp
        private val ADD = "(?<left>.+)\\+$NOT_IN_LABEL(?<right>.+)".toRegex() // exp + exp
        private val SUB = "(?<left>.+)-$NOT_IN_LABEL(?<right>.+)".toRegex() // exp - exp
        private val NEGATIVE = "-.+".toRegex() // -
        private val SORT = "(.+)(asc|desc)".toRegex() // sorting
        private val MIN = "(?<left>.+)min$NOT_IN_LABEL(?<right>.+)".toRegex() // 10min1d6
        private val MAX = "(?<left>.+)max$NOT_IN_LABEL(?<right>.+)".toRegex() // 10max1d6

        // optional trailing label, only appended to the leaf patterns in the parsers map, e.g. 1d8[slashing]
        private val LABEL = "\\s*(?:\\[(?<label>[^\\[\\]]*)\\])?"
        private val LABEL_TEXT = "\\[(?<label>[^\\[\\]]*)\\]".toRegex() // any well-formed [text]
        private val ONLY_LABEL = "^\\s*\\[(?<label>[^\\[\\]]*)\\]\\s*$".toRegex() // the right side of (1d6 + 2)[fire]
        private const val LABEL_PLACEMENT_HINT = "; a label must be the last suffix of a die, number or group, e.g. 4d6k3[STR] or 2d6[x] asc, and a term takes at most one label"
    }

    private val parsers: LinkedHashMap<Regex, (MatchResult) -> DiceExpression> = linkedMapOf(
            SORT to this::visitSort,
            (N_DICE_FACE + LABEL).toRegex() to this::visitNDiceFace,
            (KEEP_DICE.pattern + LABEL).toRegex() to this::visitKeepDice,
            (KEEP_LOW_DICE.pattern + LABEL).toRegex() to this::visitKeepLowDice,
            (DICE_FACE + LABEL).toRegex() to this::visitDiceFace,
            (DICE_FACE_X + LABEL).toRegex() to this::visitDiceFaceX,
            (N_DICE_FACE_X + LABEL).toRegex() to this::visitNDiceFaceX,
            (FUDGE_DICE + LABEL).toRegex() to this::visitFudgeDice,
            (N_FUDGE_DICE + LABEL).toRegex() to this::visitNFudgeDice,
            (DOT_FUDGE_DICE + LABEL).toRegex() to this::visitFudgeDiceDot,
            (N_DOT_FUDGE_DICE + LABEL).toRegex() to this::visitNFudgeDiceDot,
            (COMPOUND_DICE + LABEL).toRegex() to this::visitCompoundDice,
            (COMPOUND_DICE_TARGET + LABEL).toRegex() to this::visitCompoundDiceTarget,
            (EXPLODE_DICE + LABEL).toRegex() to this::visitExplode,
            (EXPLODE_DICE_TARGET + LABEL).toRegex() to this::visitExplodeTarget,
            (TARGET_POOL + LABEL).toRegex() to this::visitTargetPool,
            (TARGET_POOL_PARENS + LABEL).toRegex() to this::visitTargetPoolMod,
            MIN to this::visitMin,
            MAX to this::visitMax,
            NESTED to this::visitNested,
            ADD to this::visitAdd,
            SUB to this::visitSubtract,
            MUL to this::visitMultiply,
            DIV to this::visitDivide,
            NEGATIVE to this::visitNegative,
            ("(?<value>$INT)" + LABEL).toRegex() to this::visitInt
    )

    fun parse(expression: String): DiceExpression {

        val trimmedExpression = expression.trim()
        val result = parsers.filter { it.key.matches(trimmedExpression) }
                .mapKeys { it.key.matchEntire(trimmedExpression) }
                .filterKeys { it != null }
                .map { Pair(it.key!!, it.value) }
                .firstOrNull()
                ?: throw noMatch(expression)

        return result.second.invoke(result.first)
    }

    /**
     * Builds the exception for a fragment no parser matched. The label placement hint is only added when the
     * fragment has a label and would match with its labels removed, e.g. `4d6[STR]k3` or `1d6[a][b]`, but not
     * `4w6[a]` or a malformed bracket like `1d6[a[b]]`.
     */
    private fun noMatch(expression: String): ParseException {
        val hint = if (LABEL_TEXT.containsMatchIn(expression) && matchesWithoutLabels(expression)) LABEL_PLACEMENT_HINT else ""
        return ParseException("Failed to parse expression '$expression'$hint")
    }

    private fun matchesWithoutLabels(expression: String): Boolean {
        val unlabelled = LABEL_TEXT.replace(expression, "").trim()
        if (unlabelled.contains('[') || unlabelled.contains(']')) {
            // a bracket left over after removing every well-formed label is malformed, not misplaced
            return false
        }
        // a fragment made only of labels, e.g. the [a][b] of (1d6)[a][b], is a misplaced label too
        return unlabelled.isBlank() || parsers.keys.any { it.matches(unlabelled) }
    }

    /**
     * Returns `true` when [expression] matches one of the supported dice expression forms and every `[...]` label in
     * it follows the label rules (letters, combining marks, decimal digits, symbols and emoji, spaces and
     * - _ ' . , : / + ( ) # & ! ?, at most 64 Unicode code points), `false` otherwise. Unbalanced or nested label brackets (e.g. `1d6[a`) are
     * `false` too. This never throws.
     */
    fun validExpression(expression: String): Boolean {
        val trimmedExpression = expression.trim()
        if (parsers.keys.none { it.matches(trimmedExpression) }) {
            return false
        }
        val unlabelled = LABEL_TEXT.replace(trimmedExpression, "")
        if (unlabelled.contains('[') || unlabelled.contains(']')) {
            return false
        }
        return LABEL_TEXT.findAll(trimmedExpression).all { labelViolation(it.groups["label"]!!.value) == null }
    }

    /**
     * Returns the trimmed label of a leaf match, or `null` when it has none or it is blank.
     *
     * @throws ParseException if the label breaks the label rules
     */
    private fun labelFrom(match: MatchResult): String? {
        val rawLabel = match.groups["label"]?.value ?: return null
        return checkedLabel(rawLabel, match.value)
    }

    private fun checkedLabel(rawLabel: String, fragment: String): String? {
        val violation = labelViolation(rawLabel)
        if (violation != null) {
            throw ParseException("Failed to parse expression '$fragment', invalid label '$rawLabel', $violation")
        }
        return normalizeLabel(rawLabel)
    }

    private fun groupIfLabelled(expression: DiceExpression, label: String?): DiceExpression {
        return if (label == null) expression else GroupExpression(expression, label)
    }

    private fun visitInt(match: MatchResult): DiceExpression {
        return NumberExpression(match.groups["value"]!!.value.toInt(), labelFrom(match))
    }

    private fun visitNested(match: MatchResult): DiceExpression {

        // three parts
        // left ( middle ) right
        // middle is non null, the others could be empty
        val nested = parse(match.groupValues[2])

        // a right side of exactly one label, e.g. (1d6 + 2)[fire], labels the group and is then treated as empty
        val onlyLabel = ONLY_LABEL.matchEntire(match.groupValues[3])
        val middle = if (onlyLabel != null) {
            groupIfLabelled(nested, checkedLabel(onlyLabel.groups["label"]!!.value, match.value))
        } else {
            nested
        }
        val right = if (onlyLabel != null) "" else match.groupValues[3]

        val left: DiceExpression = if (match.groupValues[1].isNotEmpty()) {
            MultiplyExpression(parse(match.groupValues[1]), middle)
        } else {
            middle
        }

        return if (right.isNotEmpty()) {
            MultiplyExpression(left, parse(right))
        } else {
            left
        }
    }

    private fun visitDiceFace(match: MatchResult): DiceExpression {
        return NDice(match.groupValues[1].toInt(), 1, labelFrom(match))
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

        return NDice(numberOfFaces, numberOfDice, labelFrom(match))
    }

    private fun visitDiceFaceX(match: MatchResult): DiceExpression {
        val numberOfFaces = match.groupValues[1]
        return groupIfLabelled(MultiplyExpression(visitDiceFace(numberOfFaces), visitDiceFace(numberOfFaces)), labelFrom(match))
    }

    private fun visitNDiceFaceX(match: MatchResult): DiceExpression {
        val numberOfFaces = match.groupValues[2].toInt()
        val numberOfDice = match.groupValues[1].ifEmpty { "1" }.toInt()
        return groupIfLabelled(MultiplyExpression(NDice(numberOfFaces, numberOfDice), NDice(numberOfFaces, numberOfDice)), labelFrom(match))
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
        return FudgeDice(label = labelFrom(match))
    }

    private fun visitNFudgeDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        return FudgeDice(numberOfDice, label = labelFrom(match))
    }

    private fun visitFudgeDiceDot(match: MatchResult): DiceExpression {
        val weight = match.groupValues[1].toInt()
        return fudgeRoll(weight, label = labelFrom(match))
    }

    private fun visitNFudgeDiceDot(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val weight = match.groupValues[2].toInt()
        return FudgeDice(numberOfDice, 6, weight, labelFrom(match))
    }

    private fun fudgeRoll(weight: Int, sides: Int = 6, label: String? = null): DiceExpression {
        return FudgeDice(1, weight, label = label)
    }

    private fun visitKeepDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val numberToKeep = match.groupValues[3].toInt()

        return KeepDice(numberOfFaces, numberOfDice, numberToKeep, labelFrom(match))
    }

    private fun visitKeepLowDice(match: MatchResult): DiceExpression {
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val numberToKeep = match.groupValues[3].toInt()

        return KeepLowDice(numberOfFaces, numberOfDice, numberToKeep, labelFrom(match))
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

        return rollTargetPool(numberOfDice, diceFace, comp, targetNumber, labelFrom(match))
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

        return rollTargetPool(numberOfDice, diceFace, comp, targetNumber, labelFrom(match))
    }

    private fun rollTargetPool(numberOfDice: Int, numberOfFaces: Int, comp: String, targetNumber: Int, label: String?): DiceExpression {
        return TargetPoolDice(numberOfFaces, numberOfDice, comparisonFrom(comp), targetNumber, label = label)
    }

    private fun visitCompoundDice(match: MatchResult): DiceExpression { // 3d6!!
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()

        return CompoundingDice(numberOfFaces, numberOfDice, label = labelFrom(match))
    }

    private fun visitCompoundDiceTarget(match: MatchResult): DiceExpression { // 3d6!!<5
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val comp = match.groupValues[3].ifEmpty { "=" }
        val target = match.groupValues[4].toInt()

        return CompoundingDice(numberOfFaces, numberOfDice, comparisonFrom(comp), target, labelFrom(match))
    }


    private fun visitExplode(match: MatchResult): DiceExpression { // 3d6!
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()

        return ExplodingDice(numberOfFaces, numberOfDice, label = labelFrom(match))
    }

    private fun visitExplodeTarget(match: MatchResult): DiceExpression { // 3d6!>5 or 3d6!5
        val numberOfDice = match.groupValues[1].toInt()
        val numberOfFaces = match.groupValues[2].toInt()
        val comp = match.groupValues[3].ifEmpty { "=" }
        val target = match.groupValues[4].toInt()

        return ExplodingDice(numberOfFaces, numberOfDice, comparisonFrom(comp), target, labelFrom(match))
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
