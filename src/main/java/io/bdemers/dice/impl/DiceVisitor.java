package io.bdemers.dice.impl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Optional;
import java.util.Random;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class DiceVisitor extends DiceImplBaseVisitor<Integer> {

    private final UnaryOperator<Integer> randomGenerator;

    public DiceVisitor() {
        this.randomGenerator = randomFunction();
    }

    public DiceVisitor(UnaryOperator<Integer> randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    static int maxRoll(String expression) {
        return roll(expression, sides -> sides);
    }

    static int roll(String expression, UnaryOperator<Integer> randomGenerator) {
        CharStream stream = CharStreams.fromString(expression);
        DiceImplLexer lexer = new DiceImplLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        DiceImplParser parser = new DiceImplParser(tokenStream);
        ParseTree tree = parser.expression();
        return new DiceVisitor(randomGenerator).visit(tree);
    }

    public static int roll(String expression) {
        return roll(expression, randomFunction());
    }

    private static UnaryOperator<Integer> randomFunction() {
        Random random = new Random();
        return sides -> random.nextInt(sides) + 1;
    }

    private Integer parseInt(Token token, int defaultValue) {
        return Optional.ofNullable(token)
                .map(Token::getText)
                .map(Integer::valueOf)
                .orElse(defaultValue);
    }

    @Override
    public Integer visitInt(DiceImplParser.IntContext ctx) {
        String value = ctx.INT().getText();
        return Integer.valueOf(value);
    }

    private Integer parseInt(Token token) {
        return parseInt(token, 0);
    }

    @Override
    public Integer visitAdd(DiceImplParser.AddContext ctx) {
        return visit(ctx.left) + visit(ctx.right);
    }

    @Override
    public Integer visitSubtract(DiceImplParser.SubtractContext ctx) {
        return visit(ctx.left) - visit(ctx.right);
    }

    @Override
    public Integer visitDivide(DiceImplParser.DivideContext ctx) {
        return visit(ctx.left) / visit(ctx.right);
    }

    @Override
    public Integer visitMultiply(DiceImplParser.MultiplyContext ctx) {
        return visit(ctx.left) * visit(ctx.right);
    }

    @Override
    public Integer visitParenthesis(DiceImplParser.ParenthesisContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Integer visitCombineParenthesis(DiceImplParser.CombineParenthesisContext ctx) {
        return visit(ctx.left) * visit(ctx.right);
    }

    @Override
    public Integer visitDiceFace(DiceImplParser.DiceFaceContext ctx) {
        return random(parseInt(ctx.numberOfSides));
    }

    @Override
    public Integer visitExpressionDice(DiceImplParser.ExpressionDiceContext ctx) {
        int numberOfDice = visit(ctx.left);
        return IntStream.rangeClosed(1,numberOfDice)
                .map(index -> visitDiceFace(ctx.diceFace()))
                .sum();
    }

    @Override
    public Integer visitNDice(DiceImplParser.NDiceContext ctx) {
        int numberOfDice = parseInt(ctx.nDiceFace().numberOfDice, 1);
        return IntStream.rangeClosed(1,numberOfDice)
                .map(index -> visitDiceFace(ctx.nDiceFace().diceFace()))
                .sum();
    }

    @Override
    public Integer visitKeepDice(DiceImplParser.KeepDiceContext ctx) {
        int numberOfDice = parseInt(ctx.nDiceFace().numberOfDice, 1);
        int numberToKeep = parseInt(ctx.numberOfDice);

        return IntStream.rangeClosed(1,numberOfDice)
                .map(index -> visitDiceFace(ctx.nDiceFace().diceFace()))
                .sorted()
                .limit(numberToKeep)
                .sum();
    }

    @Override
    public Integer visitD10x(DiceImplParser.D10xContext ctx) {
        return visitDiceFace(ctx.diceFace()) * visitDiceFace(ctx.diceFace());
    }

    @Override
    public Integer visitFudgeDice(DiceImplParser.FudgeDiceContext ctx) {
        int numberOfDice = parseInt(ctx.numberOfDice, 1);
        return IntStream.rangeClosed(1, numberOfDice)
                .map(index -> random(3) - 2)
                .sum();
    }

    @Override
    public Integer visitNDotFudge(DiceImplParser.NDotFudgeContext ctx) {
        int numberOfDice = parseInt(ctx.numberOfDice, 1);
        return IntStream.rangeClosed(1, numberOfDice)
                .map(index -> fudgeRole(parseInt(ctx.weight)))
                .sum();
    }

    @Override
    public Integer visitTargetPool(DiceImplParser.TargetPoolContext ctx) {
        int numberOfDice = parseInt(ctx.nDiceFace().numberOfDice);

        int targetNumber;

        // an odd block but, basically 2d8+2<6 == 2d8<4
        if (ctx.ADD() != null) {
            targetNumber = parseInt(ctx.targetNumber) - parseInt(ctx.modifier);
        } else if (ctx.SUB() != null) {
            targetNumber = parseInt(ctx.targetNumber) + parseInt(ctx.modifier);
        } else {
            targetNumber = parseInt(ctx.targetNumber);
        }

        return IntStream.rangeClosed(1, numberOfDice)
                .map(index -> visitDiceFace(ctx.nDiceFace().diceFace()))
                .filter(result -> {
                    if (ctx.EQUAL() != null) {
                        return result == targetNumber;
                    } else if (ctx.GREATER_THEN_EQUAL() != null) {
                        return result >= targetNumber;
                    } else {
                        return result <= targetNumber;
                    }})
                .map(it -> 1)
                .sum();
    }

    @Override
    public Integer visitExplode(DiceImplParser.ExplodeContext ctx) {
        return explodeRoll(ctx, value -> value == parseInt(ctx.diceFace().numberOfSides));
    }

    @Override
    public Integer visitNExplodeDice(DiceImplParser.NExplodeDiceContext ctx) {

        int right = visit(ctx.right);
        IntPredicate predicate;
        if (ctx.LESS_THEN_EQUAL() != null) {
            predicate = value -> value <= right;
        } else if (ctx.GREATER_THEN_EQUAL() != null) {
            predicate = value -> value >= right;
        } else { // EQUAL is the default
            predicate = value -> value == right;
        }
        return explodeRoll(ctx.explode(), predicate);
    }

    private int explodeRoll(DiceImplParser.ExplodeContext ctx, IntPredicate predicate) {
        int numberOfDice = parseInt(ctx.numberOfDice, 1);
        return IntStream.rangeClosed(1, numberOfDice)
                .map(index -> recursiveRoll(ctx.diceFace(), predicate, 50))
                .sum();
    }

    private int recursiveRoll(DiceImplParser.DiceFaceContext diceFace, IntPredicate predicate, int maxRolls) {

        if (maxRolls < 0) {
            throw new IllegalStateException("Dice exploded too many times in a role, one of the following likely " +
                    "happened, your random number generator isn't actually random, you tried to explode a 'd1', " +
                    "or you just happened to be really lucky");
        }

        int result = visitDiceFace(diceFace);
        if (predicate.test(result)) {
            return recursiveRoll(diceFace, predicate, maxRolls -1) + result;
        }
        return result;
    }

    @Override
    public Integer visitOpCompoundDice(DiceImplParser.OpCompoundDiceContext ctx) {
        int right = visit(ctx.right);
        int numberOfDice = parseInt(ctx.compound().numberOfDice, 1);

        IntPredicate predicate;
        if (ctx.LESS_THEN_EQUAL() != null) {
            predicate = value -> value <= right;
        } else if (ctx.GREATER_THEN_EQUAL() != null) {
            predicate = value -> value >= right;
        } else { // EQUAL is the default
            predicate = value -> value == right;
        }
        return compoundRoll(numberOfDice, ctx.compound().diceFace(), predicate, 100);
    }

    @Override
    public Integer visitCompound(DiceImplParser.CompoundContext ctx) {
        int numberOfDice = parseInt(ctx.numberOfDice, 1);
        IntPredicate predicate = value -> value == parseInt(ctx.diceFace().numberOfSides);
        return compoundRoll(numberOfDice, ctx.diceFace(), predicate, 100);
    }

    private int compoundRoll(int numberOfDice, DiceImplParser.DiceFaceContext diceFace, IntPredicate predicate, int maxRolls) {

        if (maxRolls < 0) {
            throw new IllegalStateException("Dice compound too many times in a role, one of the following likely " +
                    "happened, your random number generator isn't actually random, you tried to explode a 'd1', " +
                    "or you just happened to be really lucky");
        }

        return IntStream.rangeClosed(1, numberOfDice)
                .map(index -> visitDiceFace(diceFace))
                .map(result -> {
                    if (predicate.test(result)) {
                        return result + compoundRoll(numberOfDice, diceFace, predicate, maxRolls - 1);
                    }
                    return result;
                })
                .sum();
    }

    private int fudgeRole(int weight) {
        return fudgeRole(weight, 6);
    }

    private int fudgeRole(int weight, int sides) {
        int result = random(sides);
        if (result > sides - weight) return 1;
        if (result > sides - (weight*2) ) return -1;
        return 0;
    }

    private int random(int range) {
        return randomGenerator.apply(range);
    }
}
