package io.bdemers.dice.impl;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DiceVisitorTest {

    @Test
    public void quickTest() {

        assertThat(parse("42"), is(42));
        assertThat(parse("(2)"), is(2));
        assertThat(parse("10(2)"), is(20));
    }

    @Test
    public void rollTest() {
        assertThat(parse("d6"), is(6));
        assertThat(parse("2d6"), is(12));
        assertThat(parse("(1+2)d6"), is(18));
    }

    @Test
    public void gettingComplexRoll() {
        assertThat(parse("2(d6)"), is(12));
        assertThat(parse("2(2d6)"), is(24));
    }

    @Test
    public void adding() {
        assertThat(parse("2d6+2"), is(14));
    }

    @Test
    public void subtracting() {
        assertThat(parse("2d6-2"), is(10));
    }

    @Test
    public void keep() {
        assertThat(parse("3d6k2"), is(12));
        assertThat(parse("3d6(k2)"), is(12));
    }

    @Test
    public void chainThemTogether() {
        assertThat(parse("2d6+4d2"), is(20));
        assertThat(parse("2d6+4d2(k2)"), is(16));
        assertThat(parse("d6*10+d6"), is(66));
    }

    @Test
    public void d10xTest() {
        assertThat(parse("d10x"), is(100));
        assertThat(parse("d6x"), is(36));
    }

    @Test
    public void fudge() {
        assertThat(parse("dF"), is(1));
        assertThat(parse("4dF"), is(4));
    }

    @Test
    public void dotFudge() {
        assertThat(parse("dF.1"), is(1));
        assertThat(parse("4dF.1"), is(4));
        assertThat(parse("3dF.3"), is(3));
    }

    @Test
    public void targetPool() {
        assertThat(parse("4d8=8"), is(4));
        assertThat(parse("4d8=7"), is(0));
        assertThat(parse("4d8>6"), is(4));
        assertThat(parse("4d8<6"), is(0));
        assertThat(parse("(4d8-2)<6"), is(4));
    }

    @Test
    public void explode() {
        assertThat(parse("4d6!", rolls(2, 6, 6, 5, 3, 1)), is(23));
        assertThat(parse("4d6!>5", rolls(2, 6, 6, 5, 3, 1, 2)), is(25));
        assertThat(parse("4d6!<1", rolls(2, 6, 6, 1, 4)), is(19));
    }

    @Test
    public void compound() {
        assertThat(parse("4d6!!", rolls(2, 6, 6, 5, 2, 4, 5, 1, 2, 4, 5, 6, 1, 2, 3, 4)), is(58));
        assertThat(parse("2d6!!>5", rolls(2, 6, 6, 5, 3, 1, 2, 2)), is(27));
        assertThat(parse("2d6!!<1", rolls(2, 1, 6, 2)), is(11));
    }

    @Test(enabled = false)
    public void random() {

        double average = IntStream.rangeClosed(1, 10000)
                .map(index -> DiceVisitor.roll("1d20"))
                .average().getAsDouble();

        assertThat(average, is(closeTo(10.5, 0.1)));
        // This could still fail, not sure what the best option to test this really is
    }

    int parse(String expression) {
        return DiceVisitor.maxRoll(expression);
    }

    static Queue<Integer> rolls(int ... values) {
        Queue<Integer> stack = new LinkedList<>();
        Arrays.stream(values)
            .forEach(stack::add);
        return stack;
    }

    int parse(String expression, Queue<Integer> staticRolls) {
        return DiceVisitor.roll(expression, sides -> staticRolls.remove());
    }
}
