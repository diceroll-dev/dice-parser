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

import dev.diceroll.parser.AddExpression
import dev.diceroll.parser.DiceRollingVisitor
import dev.diceroll.parser.DiceX
import dev.diceroll.parser.GroupExpression
import dev.diceroll.parser.KeepDice
import dev.diceroll.parser.MinDiceExpression
import dev.diceroll.parser.MultiplyExpression
import dev.diceroll.parser.NDice
import dev.diceroll.parser.NegativeDiceExpression
import dev.diceroll.parser.NumberExpression
import dev.diceroll.parser.ParseException
import dev.diceroll.parser.ResultTree
import dev.diceroll.parser.SortedDiceExpression
import org.testng.Assert.assertThrows
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

class RegexDiceTest {

    @Test
    fun basicInts() {
        expect(42) { parse("42") }
        expect(2) { parse("(2)") }
        expect(20) { parse("10(2)") }
    }

    @Test
    fun add() {
        expect(3) { parse("1 +2") }
        expect(3) { parse("(1+2)") }
        expect(9) { parse("(1+ 2)3") }
    }

    @Test
    fun subtract() {
        expect(-1) { parse("1-2") }
        expect(1) { parse("(2-1)") }
        expect(-3) { parse("(1-2)3") }
    }

    @Test
    fun multiply() {
        expect(4) { parse("2 * 2") }
        expect(2) { parse("(2*1)") }
        expect(18) { parse("(3*2)3") }
    }

    @Test
    fun divide() {
        expect(2) { parse("4/2") }
        expect(3) { parse("7 / 2") }
        expect(2) { parse("(4/2)") }
        expect(6) { parse("(4/2)3") }
    }

    @Test
    fun rollTest() {
        expect(6) { parse("d6") }
        expect(12) { parse("2d6") }
        expect(18) { parse("(1+2)d6") }
    }

    @Test
    fun overflowRoll() {
        assertThrows(ArithmeticException::class.java) {
            parse("1000000d1000000")
        }
    }

    @Test
    fun overflowSum() {
        assertThrows(ArithmeticException::class.java) {
            parse(Int.MAX_VALUE.toString() + "+" + Int.MAX_VALUE.toString())
        }
    }


    @Test
    fun complexRoll() {
        expect(12) { parse("2(d6)") }
        expect(24) { parse("2(2d6)") }
    }

    @Test
    fun adding() {
        expect(14) { parse("2d6+2") }
    }

    @Test
    fun subtracting() {
        expect(10) { parse("2d6-2") }
    }

    @Test
    fun keep() {
        expect(12) { parse("3d6k2") }
//        expect(12) { parse("3d6(k2)") }
    }

    @Test
    fun chainThemTogether() {
        expect(20) { parse("2d6+4d2") }
        expect(16) { parse("2d6+4d2k2") }
        expect(66) { parse("d6+10*d6") }
        expect(66) { parse("d6*10+d6") }
        expect(38) { parse("2d6+2d8+10") }
        expect(280) { parse("d10x+2d6x+d6x") }
    }

    @Test
    fun d10xTest() {
        expect(100) { parse("d10x") }
        expect(36) { parse("d6x") }
        expect(144) { parse("2d6x") }
    }

    @Test
    fun fudge() {
        expect(1) { parse("dF") }
        expect(4) { parse("4dF") }
    }

    @Test
    fun dotFudge() {
        expect(1) { parse("dF.1", rolls(3)) }
        expect(2) { parse("4dF.1", rolls(2, 4, 6, 6)) } // 0,0,1,1
        expect(3) { parse("3dF.3") }
    }

    @Test
    fun targetPool() {
        expect(4) { parse("4d8=8") }
        expect(0) { parse("4d8=7") }
        expect(4) { parse("4d8>6") }
        expect(0) { parse("4d8<6") }
        expect(4) { parse("(4d8-2)<6") }
        expect(4) { parse("(4d8-2)>6") }
    }

    @Test
    fun explode() {
        expect(23) { parse("4d6!", rolls(2, 6, 6, 5, 3, 1)) }
        expect(25) { parse("4d6!>5", rolls(2, 6, 6, 5, 3, 1, 2)) }
        expect(19) { parse("4d6!<1", rolls(2, 6, 6, 1, 4)) }
    }

    @Test
    fun compound() {
        expect(58) { parse("4d6!!", rolls(2, 6, 6, 5, 2, 4, 5, 1, 2, 4, 5, 6, 1, 2, 3, 4)) }
        expect(27) { parse("2d6!!>5", rolls(2, 6, 6, 5, 3, 1, 2, 2)) }
        expect(11) { parse("2d6!!<1", rolls(2, 1, 6, 2)) }
    }

    @Test
    fun valid() {
        expect(true, "4d6!! should be valid") { RegexDice().validExpression("4d6!!") }
        expect(false, "4w6!! should be invalid") { RegexDice().validExpression("4w6!!") }
    }

    @Test
    fun keepLow() {
        expect(2) { parse("4d6l1", rolls(2, 6, 6, 5)) }
        expect(7) { parse("4d6l2", rolls(2, 6, 6, 5)) }
        expect(2) { parse("2d20l1", rolls(2, 19)) }
    }

    @Test
    fun negative() {
        expect(-2) { parse("-4d6l1", rolls(2, 6, 6, 5)) }
        expect(-2) { parse("-d6", rolls(2)) }
        expect(-2) { parse("-2", rolls()) }
        expect(-5) { parse("-2d6", rolls(2, 3)) }
        expect(-7) { parse("-2d6-2", rolls(2, 3)) }
        expect(-3) { parse("-2d6+2", rolls(2, 3)) }
        expect(-10) { parse("-2d6*2", rolls(2, 3)) }
        expect(listOf(-2, -3, 2)) { getResults("-2d6+2", rolls(2, 3)) }
    }

    @Test
    fun sort() {
        expect(listOf(-3, -2, 2)) { getResults("-2d6+2asc", rolls(2, 3)) }
        expect(-3) { parse("-2d6+2asc", rolls(2, 3)) }
        expect(listOf(2, -2, -3)) { getResults("-2d6+2desc", rolls(2, 3)) }
        expect(-3) { parse("-2d6+2desc", rolls(2, 3)) }
        expect(listOf(-3, -2, 4)) { getResults("-2d6-4asc", rolls(2, 3)) }
        expect(listOf(-2)) { getResults("-2asc", rolls()) }
        expect(listOf(-2, 2)) { getResults("-2 + 2 asc", rolls()) }
    }

    @Test
    fun min() {
        expect(-3) { parse("-3min-4d6l1", rolls(2, 6, 6, 5)) }
        expect(-2) { parse("3min-4d6l1", rolls(2, 6, 6, 5)) }
        expect(2) { parse("2 min 100", rolls()) }
        expect(2) { parse("2 min 2", rolls()) }
        expect(2) { parse("2 min 100 + 2d6", rolls(2, 6)) }
        expect(2) { parse("2 min (100 + 2d6)", rolls(2, 6)) }
        expect(2) { parse("(100 + 2d6) min 2 ", rolls(2, 6)) }
        expect(4) { parse("(100 + 2d6) min (2 *2)", rolls(2, 6)) }
    }

    @Test
    fun max() {
        expect(-2) { parse("-3max-4d6l1", rolls(2, 6, 6, 5)) }
        expect(3) { parse("3max-4d6l1", rolls(2, 6, 6, 5)) }
        expect(100) { parse("2 max 100", rolls(2, 6)) }
        expect(2) { parse("2 max 2", rolls(2, 6)) }
        expect(108) { parse("2 max 100 + 2d6", rolls(2, 6)) }
        expect(108) { parse("2 max (100 + 2d6)", rolls(2, 6)) }
        expect(108) { parse("(100 + 2d6) max 2", rolls(2, 6)) }
        expect(108) { parse("(100 + 2d6) max (2 *2)", rolls(2, 6)) }
    }

    @Test
    fun labelOnDice() {
        val expression = RegexDice().parse("1d8[slashing]")
        assertIs<NDice>(expression)
        assertEquals("slashing", expression.label)
        expect(parse("1d8")) { parse("1d8[slashing]") }
        assertEquals("slashing", detailedRoll("1d8[slashing]").label)

        assertEquals("Admin", RegexDice().parse("1d6[Admin]").label)
        expect(6) { parse("1d6[Admin]") }

        val keep = RegexDice().parse("4d6k3[STR]")
        assertIs<KeepDice>(keep)
        assertEquals("STR", keep.label)
        expect(parse("4d6k3")) { parse("4d6k3[STR]") }
        assertEquals("STR", detailedRoll("4d6k3[STR]").label)
    }

    @Test
    fun labelOnNumber() {
        val expression = RegexDice().parse("3[STR]")
        assertIs<NumberExpression>(expression)
        assertEquals("STR", expression.label)
        expect(3) { parse("3[STR]") }
        assertEquals("STR", detailedRoll("3[STR]").label)

        assertEquals("Sneak-attack", RegexDice().parse("2[Sneak-attack]").label)
        expect(2) { parse("2[Sneak-attack]") }
    }

    @Test
    fun labelOnNegative() {
        val expression = RegexDice().parse("-3[Sapped STR]")
        assertIs<NegativeDiceExpression>(expression)
        assertEquals("Sapped STR", expression.label)
        assertEquals("Sapped STR", expression.value.label)
        expect(parse("-3")) { parse("-3[Sapped STR]") }
        assertEquals("Sapped STR", detailedRoll("-3[Sapped STR]").label)
    }

    @Test
    fun labelWithWhitespace() {
        assertEquals("slashing", RegexDice().parse("1d8 [slashing]").label)
        assertEquals("slashing", RegexDice().parse("1d8[ slashing ]").label)
        expect(parse("1d8")) { parse("1d8 [slashing]") }
    }

    @Test
    fun labelOnGroup() {
        val expression = RegexDice().parse("(1d6 + 2)[fire]")
        assertIs<GroupExpression>(expression)
        assertEquals("fire", expression.label)
        expect(parse("(1d6 + 2)")) { parse("(1d6 + 2)[fire]") }
        val resultTree = detailedRoll("(1d6 + 2)[fire]")
        assertEquals("fire", resultTree.label)
        assertNull(resultTree.results.single().label)

        val single = RegexDice().parse("(1d6)[fire]")
        assertIs<GroupExpression>(single)
        assertEquals("fire", single.label)
        assertIs<NDice>(single.value)
        expect(6) { parse("(1d6)[fire]") }
        assertEquals("fire", detailedRoll("(1d6)[fire]").label)
    }

    @Test
    fun labelOnGroupAndInnerDice() {
        val expression = RegexDice().parse("(1d6[a])[b]")
        assertIs<GroupExpression>(expression)
        assertEquals("b", expression.label)
        assertEquals("a", expression.value.label)
        expect(6) { parse("(1d6[a])[b]") }
    }

    @Test
    fun labelOnGroupWithMultiplier() {
        val expression = RegexDice().parse("2(1d6)[x]")
        assertIs<MultiplyExpression>(expression)
        assertNull(expression.label)
        assertIs<NumberExpression>(expression.left)
        val group = assertIs<GroupExpression>(expression.right)
        assertEquals("x", group.label)
        assertIs<NDice>(group.value)
        expect(parse("2(1d6)")) { parse("2(1d6)[x]") }
    }

    @Test
    fun labelOnGroupInMin() {
        val expression = RegexDice().parse("2 min (1d8)[x]")
        assertIs<MinDiceExpression>(expression)
        val group = assertIs<GroupExpression>(expression.right)
        assertEquals("x", group.label)
        val resultTree = detailedRoll("2 min (1d8)[x]", rolls(1))
        expect(1) { resultTree.value }
        assertEquals("x", resultTree.label)
    }

    @Test
    fun labelOnMultiplyDice() {
        val expression = RegexDice().parse("2d6x[a]")
        assertIs<GroupExpression>(expression)
        assertIs<MultiplyExpression>(expression.value)
        expect(parse("2d6x")) { parse("2d6x[a]") }
        assertEquals("a", detailedRoll("2d6x[a]").label)
    }

    @Test
    fun labelThenSort() {
        val expression = RegexDice().parse("2d6[x] asc")
        assertIs<SortedDiceExpression>(expression)
        assertEquals("x", expression.value.label)
        assertEquals("x", expression.label)
        expect(listOf(2, 5)) { getResults("2d6[x] asc", rolls(5, 2)) }
        assertEquals("x", detailedRoll("2d6[x] asc").label)
    }

    @Test
    fun sortKeywordAsLabel() {
        // square brackets only ever mean a label, so this is an unsorted roll labelled asc
        val expression = RegexDice().parse("10d10[asc]")
        val dice = assertIs<NDice>(expression)
        assertEquals("asc", dice.label)
        assertEquals(10, dice.numberOfDice)
        expect(listOf(5, 2, 9, 1, 7, 3, 10, 4, 8, 6)) { getResults("10d10[asc]", rolls(5, 2, 9, 1, 7, 3, 10, 4, 8, 6)) }
        assertEquals("asc", detailedRoll("10d10[asc]").label)
    }

    @Test
    fun labelsOnEachTerm() {
        val expression = "1d20 + 2[Prof] + 3[STR] + 1d4[Bless]"
        expect(parse("1d20 + 2 + 3 + 1d4")) { parse(expression) }
        expect(29) { parse(expression) }

        val root = detailedRoll(expression)
        assertNull(root.label)
        // ((1d20 + 2[Prof]) + 3[STR]) + 1d4[Bless]
        assertEquals("Bless", root.results[1].label)
        assertEquals("STR", root.results[0].results[1].label)
        assertEquals("Prof", root.results[0].results[0].results[1].label)
        assertNull(root.results[0].results[0].results[0].label)
    }

    @Test
    fun emptyLabel() {
        val expression = RegexDice().parse("1d6[]")
        assertIs<NDice>(expression)
        assertNull(expression.label)
        assertNull(RegexDice().parse("1d6[   ]").label)
        expect(parse("1d6")) { parse("1d6[]") }
        assertEquals("d6", expression.description())

        val group = RegexDice().parse("(1d6)[]")
        assertIs<NDice>(group)
        assertNull(group.label)
    }

    @Test
    fun invalidLabels() {
        assertThrows(ParseException::class.java) { RegexDice().parse("4d6[STR]k3") }
        assertThrows(ParseException::class.java) { RegexDice().parse("2d6 asc[x]") }
        assertThrows(ParseException::class.java) { RegexDice().parse("1d6[a][b]") }
        assertThrows(ParseException::class.java) { RegexDice().parse("1d6[a") }
        assertThrows(ParseException::class.java) { RegexDice().parse("1d6]") }
        assertThrows(ParseException::class.java) { RegexDice().parse("1d6[a[b]]") }
        // malformed brackets simply fail to match, without the label placement hint
        listOf("1d6[a", "1d6]", "1d6[a[b]]").forEach { expression ->
            val error = assertFailsWith<ParseException>(expression) { RegexDice().parse(expression) }
            assertEquals("Failed to parse expression '$expression'", error.message)
        }
    }

    @Test
    fun validLabels() {
        expect(true) { RegexDice().validExpression("1d8[slashing]") }
        expect(true) { RegexDice().validExpression("1d6[]") }
        expect(false) { RegexDice().validExpression("1d6[a][b]") }
        expect(false) { RegexDice().validExpression("4d6[STR]k3") }
        expect(false) { RegexDice().validExpression("1d6[a") }
        expect(false) { RegexDice().validExpression("1d6]") }
        expect(false) { RegexDice().validExpression("1d6[a[b]]") }
    }

    @Test
    fun labelDescriptionRoundTrip() {
        val dice = RegexDice().parse("1d8[slashing]")
        assertEquals("d8[slashing]", dice.description())
        assertEquals("slashing", RegexDice().parse(dice.description()).label)

        val group = RegexDice().parse("(1d6 + 2)[fire]")
        assertEquals("(d6 + 2)[fire]", group.description())
        val reparsed = RegexDice().parse(group.description())
        assertIs<GroupExpression>(reparsed)
        assertEquals("fire", reparsed.label)
    }

    @Test
    fun labelOnEachLeafForm() {
        // form to the rolls it consumes, the same rolls are replayed for the labelled and unlabelled form
        val forms = linkedMapOf(
                "4d6l1" to intArrayOf(2, 6, 6, 5),
                "dF" to intArrayOf(5),
                "3dF" to intArrayOf(1, 4, 6),
                "dF.1" to intArrayOf(1),
                "2dF.1" to intArrayOf(2, 6),
                "3d6!" to intArrayOf(6, 2, 3, 4),
                "3d6!>5" to intArrayOf(5, 1, 2, 3),
                "3d6!!" to intArrayOf(6, 1, 2, 1, 2, 3),
                "3d6!!>5" to intArrayOf(5, 1, 2, 1, 1, 1),
                "4d10>6" to intArrayOf(7, 2, 9, 1),
                "(4d10+2)>6" to intArrayOf(7, 2, 9, 1),
                "d6x" to intArrayOf(3, 4),
                "2d6x" to intArrayOf(3, 4, 5, 6)
        )
        forms.forEach { (form, dice) ->
            val labelled = RegexDice().parse("$form[x]")
            assertEquals("x", labelled.label, form)
            assertTrue(labelled.description().endsWith("[x]"), "$form description was ${labelled.description()}")
            assertEquals(parse(form, dice.toMutableList()), parse("$form[x]", dice.toMutableList()), form)
            assertEquals("x", detailedRoll("$form[x]", dice.toMutableList()).label, form)
        }
    }

    @Test
    fun labelWithNotationCharacters() {
        val labels = listOf("+", "-", "/", "(", ")", ".", "!", "#", ",", ":",
                "d", "k", "l", "x", "F", "asc", "desc", "min", "max", "0", "42",
                "1d6 + 2", "(fire)", "2d6x!k3l1.5, dF: min/max #1 - desc asc")
        labels.forEach { label ->
            val dice = RegexDice().parse("1d6[$label] + 3")
            assertIs<AddExpression>(dice)
            assertEquals(label, dice.left.label)
            expect(parse("1d6 + 3"), label) { parse("1d6[$label] + 3") }

            assertEquals(label, RegexDice().parse("3[$label]").label)
            expect(3, label) { parse("3[$label]") }

            assertEquals(label, RegexDice().parse("(1d6 + 2)[$label]").label)
            expect(parse("(1d6 + 2)"), label) { parse("(1d6 + 2)[$label]") }
        }
        // letters with diacritics, U+00DC U+00EF U+00F6 U+00E9
        val accented = codePoint(0xDC) + "n" + codePoint(0xEF) + "c" + codePoint(0xF6) + "d" + codePoint(0xE9) + " 1"
        assertEquals(accented, RegexDice().parse("1d6[$accented]").label)
        // e followed by U+0301 combining acute accent, a \p{M} mark
        val combining = "e" + codePoint(0x301)
        assertEquals(combining, RegexDice().parse("1d6[$combining]").label)
        assertEquals("a".repeat(64), RegexDice().parse("1d6[${"a".repeat(64)}]").label)
    }

    @Test
    fun labelWithEmoji() {
        val fire = codePoint(0x1F525) // U+1F525 fire, a \p{So} symbol outside the BMP
        val group = RegexDice().parse("(2d8 +2)[$fire sword]")
        assertIs<GroupExpression>(group)
        assertEquals("$fire sword", group.label)
        assertEquals(parse("(2d8 +2)", rolls(3, 5)), parse("(2d8 +2)[$fire sword]", rolls(3, 5)))
        expect(true) { RegexDice().validExpression("(2d8 +2)[$fire sword]") }

        val die = RegexDice().parse("1d6[$fire]")
        assertIs<NDice>(die)
        assertEquals(fire, die.label)

        // U+2694 crossed swords, a BMP \p{So} symbol
        val swords = codePoint(0x2694)
        assertEquals(swords, RegexDice().parse("1d20[$swords]").label)

        // mage, medium skin tone, ZWJ, male sign, emoji presentation selector
        val maleMage = codePoint(0x1F9D9) + codePoint(0x1F3FD) + codePoint(0x200D) + codePoint(0x2642) + codePoint(0xFE0F)
        assertEquals(maleMage, RegexDice().parse("1d6[$maleMage]").label)
        expect(true) { RegexDice().validExpression("1d6[$maleMage]") }
        assertEquals(maleMage, NumberExpression(3, maleMage).label)
    }

    @Test
    fun labelWithDisallowedCharacters() {
        val noBreakSpace = codePoint(0xA0)
        val rightToLeftOverride = codePoint(0x202E)
        val zeroWidthSpace = codePoint(0x200B)
        val labels = listOf("*", "<", ">", "=", "a*b", "a<b", "a>b", "a=b",
                "a\tb",
                "a\nb",
                "a${noBreakSpace}b",
                rightToLeftOverride, "a${rightToLeftOverride}b",
                zeroWidthSpace, "a${zeroWidthSpace}b",
                "\"", "`", "\\", "{", "}", "|", "~", "^", "\$", "%", "@", ";",
                "a".repeat(65))
        labels.forEach { label ->
            assertFailsWith<ParseException>(label) { RegexDice().parse("1d6[$label]") }
            assertFailsWith<ParseException>(label) { RegexDice().parse("(1d6)[$label]") }
            expect(false, label) { RegexDice().validExpression("1d6[$label]") }
        }
    }

    @Test
    fun parseExceptionQuotesRawLabel() {
        val invalid = assertFailsWith<ParseException> { RegexDice().parse("1d6[ a*b ]") }
        assertTrue(invalid.message!!.contains("' a*b '"), invalid.message)

        val unmatched = assertFailsWith<ParseException> { RegexDice().parse("1d6[ a ][b]") }
        assertTrue(unmatched.message!!.contains("[ a ][b]"), unmatched.message)
    }

    @Test
    fun invalidLabelMessageNamesCharacterAndPosition() {
        // position counts code points from 1 in the untrimmed label text
        val star = assertFailsWith<ParseException> { RegexDice().parse("1d6[ a*b ]") }
        assertTrue(star.message!!.contains("U+002A at position 3 of the label"), star.message)
        assertTrue(star.message!!.contains("labels may only contain letters, combining marks, decimal digits, symbols and emoji, spaces and - _ ' . , : / + ( ) # & ! ?"), star.message)

        // the first disallowed code point is reported, U+1F525 is an allowed emoji outside the BMP (two chars)
        val fire = codePoint(0x1F525)
        val first = assertFailsWith<ParseException> { RegexDice().parse("1d6[<<a<]") }
        assertTrue(first.message!!.contains("U+003C at position 1 of the label"), first.message)
        val afterEmoji = assertFailsWith<ParseException> { RegexDice().parse("1d6[${fire}${fire}a<]") }
        assertTrue(afterEmoji.message!!.contains("U+003C at position 4 of the label"), afterEmoji.message)

        // a disallowed code point outside the BMP (U+F0000, private use) is named by its full code point
        val privateUse = codePoint(0xF0000)
        val supplementary = assertFailsWith<ParseException> { RegexDice().parse("1d6[ab$privateUse]") }
        assertTrue(supplementary.message!!.contains("U+F0000 at position 3 of the label"), supplementary.message)

        // U+1D49C is an allowed letter outside the BMP (two chars), so the star is code point 2, not char 3
        val scriptA = codePoint(0x1D49C)
        val afterScriptA = assertFailsWith<ParseException> { RegexDice().parse("1d6[${scriptA}*]") }
        assertTrue(afterScriptA.message!!.contains("U+002A at position 2 of the label"), afterScriptA.message)

        val tab = assertFailsWith<ParseException> { RegexDice().parse("1d6[a\tb]") }
        assertTrue(tab.message!!.contains("U+0009 at position 2 of the label"), tab.message)

        val tooLong = assertFailsWith<ParseException> { RegexDice().parse("1d6[${"a".repeat(65)}]") }
        assertTrue(tooLong.message!!.contains("64 Unicode code points, this one has 65"), tooLong.message)
        val longScriptA = assertFailsWith<ParseException> { RegexDice().parse("1d6[${scriptA.repeat(70)}]") }
        assertTrue(longScriptA.message!!.contains("this one has 70"), longScriptA.message)

        // characters are checked before the length
        val longWithStar = assertFailsWith<ParseException> { RegexDice().parse("1d6[${"a".repeat(65)}*]") }
        assertTrue(longWithStar.message!!.contains("U+002A at position 66 of the label"), longWithStar.message)

        val constructor = assertFailsWith<IllegalArgumentException> { NDice(6, 1, "a]b") }
        assertTrue(constructor.message!!.contains("U+005D at position 2 of the label"), constructor.message)
    }

    @Test
    fun misplacedLabelMessageHasHint() {
        val hint = "; a label must be the last suffix of a die, number or group, e.g. 4d6k3[STR] or 2d6[x] asc, and a term takes at most one label"
        listOf("4d6[STR]k3", "1d6[a][b]", "2d6 asc[x]", "(1d6)[a][b]", "1d6 + [a]").forEach { expression ->
            val error = assertFailsWith<ParseException>(expression) { RegexDice().parse(expression) }
            assertTrue(error.message!!.endsWith(hint), error.message)
        }
        val noLabel = assertFailsWith<ParseException> { RegexDice().parse("4w6") }
        assertEquals("Failed to parse expression '4w6'", noLabel.message)

        // these fail with or without their label, so the hint would mislead
        val unknownDie = assertFailsWith<ParseException> { RegexDice().parse("4w6[a]") }
        assertEquals("Failed to parse expression '4w6[a]'", unknownDie.message)
        val danglingOperator = assertFailsWith<ParseException> { RegexDice().parse("1d6[a] +") }
        assertEquals("Failed to parse expression '1d6[a] +'", danglingOperator.message)
    }

    @Test
    fun constructorNormalizesLabel() {
        assertNull(NDice(6, 1, "").label)
        assertNull(NDice(6, 1, "   ").label)
        assertEquals("a", NDice(6, 1, " a ").label)
        assertEquals("d6[a]", NDice(6, 1, " a ").description())
        assertEquals("STR", NumberExpression(3, " STR ").label)
        assertFailsWith<IllegalArgumentException> { NDice(6, 1, "a]b") }
        assertFailsWith<IllegalArgumentException> { NumberExpression(3, codePoint(0x200B)) }
        assertEquals(codePoint(0x1F525), NumberExpression(3, codePoint(0x1F525)).label)
        assertFailsWith<IllegalArgumentException> { NDice(6, 1, "a".repeat(65)) }
        assertFailsWith<IllegalArgumentException> { GroupExpression(NumberExpression(1), " ") }
        assertFailsWith<IllegalArgumentException> { GroupExpression(NumberExpression(1), "a*b") }
        assertEquals("fire", GroupExpression(NumberExpression(1), " fire ").label)
        assertEquals("d6X[a]", DiceX(6, 1, "a").description())
    }

    private fun detailedRoll(expression: String): ResultTree {
        return DiceRollingVisitor { it }.visit(RegexDice().parse(expression))
    }

    private fun detailedRoll(expression: String, staticRolls: MutableList<Int>): ResultTree {
        return DiceRollingVisitor { staticRolls.removeAt(0) }.visit(RegexDice().parse(expression))
    }

    private fun parse(expression: String): Int {
        return detailedRoll(expression).value
    }

    private fun parse(expression: String, staticRolls: MutableList<Int>): Int {
        return detailedRoll(expression, staticRolls).value
    }

    private fun getResults(expression: String, staticRolls: MutableList<Int>): List<Int> {
        return getBaseResults(detailedRoll(expression, staticRolls))
    }

    private fun getBaseResults(resultTree: ResultTree): List<Int> {
        return if (resultTree.results.isNotEmpty()) {
            resultTree.results.stream()
                    .flatMap { rt: ResultTree -> getBaseResults(rt).stream() }
                    .collect(Collectors.toList())
        } else listOf(resultTree.value)
    }

    private fun rolls(vararg values: Int): MutableList<Int> {
        return values.toMutableList()
    }

    // builds a non-ASCII test input from its code point, so this source file stays ASCII-only
    private fun codePoint(codePoint: Int): String {
        return String(Character.toChars(codePoint))
    }
}
