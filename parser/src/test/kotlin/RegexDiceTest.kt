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

import dev.diceroll.parser.DiceRollingVisitor
import dev.diceroll.parser.ResultTree
import org.testng.Assert.assertThrows
import java.util.stream.Collectors
import kotlin.test.Test
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
        expect(4) { parse("4d8>8") }
        expect(4) { parse("4d8<8") }
        expect(0) { parse("4d8=7") }
        expect(4) { parse("4d8>6") }
        expect(0) { parse("4d8<6") }
        expect(1) { parse("(4d8-2)<6") } //each part of the subexpression is evaluated against the target (32 and 2)
        expect(1) { parse("(4d8-2)>6") } //each part of the subexpression is evaluated against the target (32 and 2)
        expect(3) { parse("(4d6!)>5", rolls(2, 6, 6, 5, 3, 1)) }
        expect(3) { parse("(4d6!>5)>5", rolls(2, 6, 6, 5, 3, 1, 2)) }
    }

    @Test
    fun explode() {
        expect(23) { parse("4d6!", rolls(2, 6, 6, 5, 3, 1)) }
        expect(25) { parse("4d6!>5", rolls(2, 6, 6, 5, 3, 1, 2)) }
        expect(3) { parse("(4d6!)>5", rolls(2, 6, 6, 5, 3, 1, 2)) }
        expect(19) { parse("4d6!<1", rolls(2, 6, 6, 1, 4)) }
        expect(0) { parse("(2d6!)>10", rolls(3, 6, 4)) }

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
        expect(listOf(2, 100)) { getResults("2 min 100", rolls()) }
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
        expect(listOf(2, 100)) { getResults("2 max 100", rolls()) }
        expect(2) { parse("2 max 2", rolls(2, 6)) }
        expect(108) { parse("2 max 100 + 2d6", rolls(2, 6)) }
        expect(108) { parse("2 max (100 + 2d6)", rolls(2, 6)) }
        expect(108) { parse("(100 + 2d6) max 2", rolls(2, 6)) }
        expect(108) { parse("(100 + 2d6) max (2 *2)", rolls(2, 6)) }
    }

    @Test
    fun customDie() {
        expect(2) { parse("d[1/1/1/2/2/3]", rolls(4)) }
        expect(3) { parse("2d[1/1/1/2/2/3]", rolls(4, 2)) }
        expect(9) { parse("2d[1/1/1/2/2/3] + 1d6", rolls(4, 2, 6)) }
    }

    @Test
    fun addExplode() {
        expect(5) { parse("1d6^", rolls(5)) }
        expect(10) { parse("1d6^", rolls(6, 4)) }
        expect(13) { parse("2d6^", rolls(3, 6, 4)) }
        expect(1) { parse("2d6^>10", rolls(3, 6, 4)) }
    }

    private fun parse(expression: String): Int {
        val diceExpression = RegexDice().parse(expression)
        val result = DiceRollingVisitor { it }.visit(diceExpression)
        return result.value
    }

    private fun parse(expression: String, staticRolls: MutableList<Int>): Int {
        val diceExpression = RegexDice().parse(expression)
        val result = DiceRollingVisitor { staticRolls.removeAt(0) }.visit(diceExpression)
        return result.value
    }

    private fun getResults(expression: String, staticRolls: MutableList<Int>): List<Int> {
        val diceExpression = RegexDice().parse(expression)
        return getBaseResults(DiceRollingVisitor { staticRolls.removeAt(0) }.visit(diceExpression))
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
}