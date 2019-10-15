package io.bdemers.dice;

import io.bdemers.dice.impl.DiceImplLexer;
import io.bdemers.dice.impl.DiceImplParser;
import io.bdemers.dice.impl.DiceVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Dice {

    private static DiceVisitor DICE_VISITOR = new DiceVisitor();

    public static int roll(String expression) {
        CharStream stream = CharStreams.fromString(expression);
        DiceImplLexer lexer = new DiceImplLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        DiceImplParser parser = new DiceImplParser(tokenStream);
        ParseTree tree = parser.expression();
        return DICE_VISITOR.visit(tree);
    }
}
