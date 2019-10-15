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

import org.testng.Assert;
import org.testng.annotations.Test;

public class JavaUsageTest {

    /**
     * All the testing is done in Kotlin, this Java test makes sure the Java usage works, i.e. Dice.roll(...)
     */
    @Test
    public void simpleUsageTest() {
        Assert.assertTrue(Dice.roll("2d6") >= 2,"Expected a value >2");
    }
}