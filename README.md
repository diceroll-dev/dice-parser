[<img src="https://avatars0.githubusercontent.com/u/56687116?s=400&u=a1a754aad591efe43f8d00e768a87e67f6d3aead" align="right" height="64px"/>](https://github.com/diceroll-dev/dice-parser/)
[![Maven Central](https://img.shields.io/maven-central/v/dev.diceroll/dice-parser.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22dev.diceroll%22%20a%3A%22dice-parser%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Dice Notation Parser for Kotlin and Java

Java Usage:

```java
import dev.diceroll.parse.Dice;
...

// evaluate an expression and return an integer 
        int result=Dice.roll("2d6");

// or

// evaluate an expression and return a tree, which contains the values of the individual dice rolled 
        ResultTree resultTree=Dice.detailedRoll("2d6");
        int result=resultTree.value();
```

Kotlin Usage:

```kotlin
import dev.diceroll.parser.roll
import dev.diceroll.parser.detailedRoll

...

// evaluate an expression and return an integer 
val result: Int = roll("2d6")

// or

// evaluate an expression and return a tree, which contains the values of the individual dice rolled 
val resultTree: ResultTree = detailedRoll("2d6")
val result: Int = resultTree.value()
```

## Supported Notation

| Name                         | Notation                                           | Example        | Description                                                                                                                                              |
|------------------------------|----------------------------------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
|                              |                                                    |                |                                                                                                                                                          |
| Single Die                   | `d<numberOfFaces>`                                 | `d6`           | roll one, six-sided die                                                                                                                                  |
| Multiple Dice                | `<numberOfDice>d<numberOfFaces>`                   | `3d20`         | roll three, twenty-sided dice                                                                                                                            |
| Custom Dice                  | `d[</face1/face2/face3...>]`                       | `d[2/4/8/16]`  | roll a die with four sides that show a 2, 4, 8 and a 16                                                                                                  |
| Multiple Custom Dice         | `<numberOfDice>d[</face1/face2/face3...>]`         | `3d[2/4/8/16]` | roll three dice, with four sides that show a 2, 4, 8 and a 16                                                                                            |
| Keep Dice                    | `<numberOfDice>d<numberOfFaces>k<numberOfDiceKept>` | `3d6k2`        | keeps the the highest values out of three, six-sided dice                                                                                                |
| Keep Low Dice                | `<numberOfDice>d<numberOfFaces>l<numberOfDiceKept>` | `3d6l2`        | keeps the the lowest values out of three, six-sided dice                                                                                                 |
| Multiply Dice                | `<numberOfDice>d<numberOfFaces>X`                  | `4d10X`        | multiplies the result of `4d10 * 4d10`                                                                                                                   |
| Fudge Dice                   | `dF`                                               | `dF`           | roles a single "fudge" die (a six sided die, 1/3 chance of `-1`, 1/3 chance of `0`, and 1/3 chance of `1`)                                               |
| Multiple Fudge Dice          | `<numberOfDice>dF`                                 | `3dF`          | roles multiple fudge dice                                                                                                                                |
| Weighted Fudge Die           | `dF.<weight>`                                      | `dF.1`         | A weighted fudge die with 1/6 chance of a `1`, `2/3` chance of a `0` and 1/6 chance of a `-1`                                                            |
| Multiple Weighted Fudge Dice | `<numberOfDice>dF.<weight>`                        | `2dF.1`        | multiple weighted fudge dice.                                                                                                                            |
| Exploding Dice               | `<numberOfDice>d<numberOfFaces>!`                  | `4d6!`         | any time the max value of a die is rolled, that die is re-rolled and added to the dice set total. A reroll will be represented as two dice result        |
| Exploding Dice (Target)      | `<numberOfDice>d<numberOfFaces>!><target>`         | `3d6!>5`       | Same as exploding dice, but re-roll on values greater than or equal to the target (note, less than works too)                                            |
| Exploding Add Dice           | `<numberOfDice>d<numberOfFaces>^>`                 | `3d6^`         | any time the max value of a die is rolled, that die is re-rolled and added to the die previous resul total. A reroll will be represented as single value |
| Compounding Dice             | `<numberOfDice>d<numberOfFaces>!!`                 | `3d6!!`        | similar to exploding dice, but ALL dice are re-rolled                                                                                                    | 
| Compounding Dice (Target)    | `<numberOfDice>d<numberOfFaces>!!><target>`        | `3d6!!>5`      | similar as exploding dice (target), but all dice are re-rolled and added.                                                                                |
| Target Pool Dice             | `<numberOfDice>d<numberOfFaces>[>,<,=]<target>`    | `3d6=6`        | counts the number of dice that match the target (NOTE: greater & less than also match equals, i.e `>=` and `<=`).                                        | 
| Integer                      | `<int>`                                            | `42`           | typically used in math operations, i.e. `2d4+2`                                                                                                          |
| Math                         | `<left> <operation> <right>`                       |
| Add                          | `<left> + <right>`                                 | `2d6 + 2`      |                                                                                                                                                          |
| Subtract                     | `<left> - <right>`                                 | `2 - 1`        |                                                                                                                                                          |
| Multiply                     | `<left> * <right>`                                 | `1d4 * 2d6`    |                                                                                                                                                          |
| Divide                       | `<left> / <right>`                                 | `4 / 2`        |                                                                                                                                                          |
| Negative                     | `-<diceExpression>`                                | `-1d6`         | multiplies the result of the dice expression with -1                                                                                                     |
| Order                        | `<diceExpression>[asc, desc]`                      | `10d10asc`     | ordering the results of the dice ascending (`asc`) or descending (`desc`)                                                                                |
| Min/Max                      | `<diceExpression>[min, max]<diceExpression>`       | `2d6min(1d6+3)` | returns the minimum or maximum of two dice expressions, e.g. `2d6min(1d6+3)` returns the smaller value of `2d6` and `1d6+3`                              |

## Dependency Information

Maven:

```xml

<dependency>
    <groupId>dev.diceroll</groupId>
    <artifactId>dice-parser</artifactId>
    <version>${version}</version>
</dependency>
```

Gradle

```
implementation 'dev.diceroll:dice-parser:${version}'
```
