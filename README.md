# Dice Notation Parser for Kotlin and Java

Java Usage:

```java
import dev.diceroll.parse.Dice;
...

// evaluate an expression and return an integer 
int result = Dice.roll("2d6");

// or

// evaluate an expression and return a tree, which contains the values of the individual dice rolled 
ResultTree resultTree = Dice.detailedRoll("2d6");
int result = resultTree.value();
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

| Name | Notation | Example | Description |
|------|----------|---------|-------------|
|  |  |  |  |
| Single Die | `d<numberOfFaces>` | `d6` | roll one, six-sided die |
| Multiple Dice | `<numberOfDice>d<numberOfFaces>` | `3d20` | roll three, twenty-sided dice |
| Keep Dice | `<numberOfDice>d<numberOfFaces>k<numberOfDiceKept>` | `3d6k2` | keeps the the highest values out of three, six-sided dice |
| Multiply Dice | `<numberOfDice>d<numberOfFaces>X` | `4d10X` | multiplies the result of `4d10 * 4d10` |
| Fudge Dice | `dF` | `dF` | roles a single "fudge" die (a six sided die, 1/3 chance of `-1`, 1/3 chance of `0`, and 1/3 chance of `1`) |
| Multiple Fudge Dice | `<numberOfDice>dF` | `3dF` | roles multiple fudge dice |
| Weighted Fudge Die | `dF.<weight>` | `dF.1` | A weighted fudge die with 1/6 chance of a `1`, `2/3` chance of a `0` and 1/6 chance of a `-1` |
| Multiple Weighted Fudge Dice | `<numberOfDice>dF.<weight>` | `2dF.1` | multiple weighted fudge dice. |
| Exploding Dice | `<numberOfDice>d<numberOfFaces>!` | `4d6!` | any time the max value of a die is rolled, that die is re-rolled and added to the total |
| Exploding Dice (Target) | `<numberOfDice>d<numberOfFaces>!><target>` | `3d6!>5` | Same as exploding dice, but re-roll on values greater than or equal to the target (note, less than works too) |
| Compounding Dice | `<numberOfDice>d<numberOfFaces>!!` | `3d6!!` | similar to exploding dice, but ALL dice are re-rolled | 
| Compounding Dice (Target) | `<numberOfDice>d<numberOfFaces>!!><target>` | `3d6!!>5` | similar as exploding dice (target), but all dice are re-rolled and added. |
| Target Pool Dice | `<numberOfDice>d<numberOfFaces>[>,<,=]<target>` | `3d6=6` | counts the number of dice that match the target (NOTE: greater & less than also match equals, i.e `>=` and `<=`) | 
| Target Pool Dice (Expression) | `(<expression>)[>,<,=]<target>` | `(2d6+)>5` | A target pool roll, but where the expression is evaluated to the target. |
| Integer | `<int>` - typically used in math operations, i.e. `2d4+2` |
| Math | `<left> <operation> <right>` |
| Add | `<left> + <right>`  | `2d6 + 2` | |
| Subtract | `<left> - <right>` | `2 - 1` | |
| Multiply | `<left> * <right>` | `1d4 * 2d6` | |
| Divide | `<left> / <right>` | `4 / 2` | |

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