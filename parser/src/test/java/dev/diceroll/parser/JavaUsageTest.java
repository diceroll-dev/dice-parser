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
package dev.diceroll.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaUsageTest {

    /**
     * All the testing is done in Kotlin, this Java test makes sure the Java usage works, i.e. Dice.roll(...)
     */
    @Test
    void simpleUsageTest() {
        assertThat(Dice.roll("2d6")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void labelUsageTest() {
        assertThat(Dice.detailedRoll("1d8[slashing]").getExpression().getLabel()).isEqualTo("slashing");
        assertThat(Dice.detailedRoll("1d8[slashing]").getLabel()).isEqualTo("slashing");
    }

    @Test
    void existingConstructorsTest() {
        NumberExpression three = new NumberExpression(3);
        assertThat(three.getLabel()).isNull();
        assertThat(three.description()).isEqualTo("3");
        assertThat(new NumberExpression(3, "STR").description()).isEqualTo("3[STR]");
    }

    @Test
    void customExpressionTest() {
        // a Java implementation written before labels existed only implements description()
        DiceExpression custom = new DiceExpression() {
            @Override
            public String description() {
                return "custom";
            }
        };
        assertThat(custom.getLabel()).isNull();
    }

    @Test
    void customVisitorTest() {
        DiceVisitor<Integer> visitor = new PreLabelVisitor();
        DiceExpression group = new GroupExpression(new NumberExpression(3), "x");
        assertThat(visitor.visit(group)).isEqualTo(3);
    }

    @Test
    void unlabelledGroupTest() {
        DiceExpression group = new GroupExpression(new NumberExpression(3));
        assertThat(group.getLabel()).isNull();
        assertThat(group.description()).isEqualTo("(3)");
        assertThat(new GroupExpression(new NumberExpression(3), null).getLabel()).isNull();
    }

    /**
     * A Java visitor written before labels existed, it does not implement visit(GroupExpression).
     */
    private static class PreLabelVisitor implements DiceVisitor<Integer> {
        @Override
        public Integer visit(NumberExpression value) {
            return value.getValue();
        }

        @Override
        public Integer visit(MathExpression mathExpressionResult) {
            return 0;
        }

        @Override
        public Integer visit(NDice nDice) {
            return 0;
        }

        @Override
        public Integer visit(DiceX diceX) {
            return 0;
        }

        @Override
        public Integer visit(FudgeDice fudgeDice) {
            return 0;
        }

        @Override
        public Integer visit(KeepDice keepDice) {
            return 0;
        }

        @Override
        public Integer visit(ExplodingDice explodingDice) {
            return 0;
        }

        @Override
        public Integer visit(CompoundingDice compoundingDice) {
            return 0;
        }

        @Override
        public Integer visit(TargetPoolDice targetPoolDice) {
            return 0;
        }

        @Override
        public Integer visit(KeepLowDice keepLowDice) {
            return 0;
        }

        @Override
        public Integer visit(NegativeDiceExpression negativeDiceExpression) {
            return 0;
        }

        @Override
        public Integer visit(SortedDiceExpression sortedDiceExpression) {
            return 0;
        }

        @Override
        public Integer visit(MinDiceExpression minDiceExpression) {
            return 0;
        }

        @Override
        public Integer visit(MaxDiceExpression maxDiceExpression) {
            return 0;
        }
    }
}
