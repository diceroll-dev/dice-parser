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
@file:JvmName("Dice")
package dev.diceroll.parser

import dev.diceroll.parser.impl.RegexDice
import kotlin.Int

fun roll(expression: String): Int {
    return detailedRoll(expression).value
}

fun detailedRoll(expression: String): ResultTree {
    val diceExpression = DiceUtil.instance.parse(expression)
    return DiceRollingVisitor().visit(diceExpression)
}

fun debug(resultTree: ResultTree): String {
    val stringBuilder = StringBuilder()
    debug(resultTree, stringBuilder)
    return stringBuilder.toString()
}
private fun debug(resultTree: ResultTree, stringBuilder: StringBuilder, prefix: String = "") {

    stringBuilder.append(prefix)
    stringBuilder.append(resultTree.expression.description() + " = " + resultTree.value).append("\n")

    resultTree.results.forEach {
        debug(it, stringBuilder, "${prefix}--")
    }
}

internal class DiceUtil {
    companion object {
        val instance = RegexDice()
    }
}