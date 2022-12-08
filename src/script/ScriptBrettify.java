package script;

import parsec.generic.GenericInfixEval;
import parsec.records.Tuple;
import parsec.spec.Parser;

import static parsec.bundle.CharacterBundle.*;
import static parsec.spec.Parser.*;

import java.util.function.Function;

public interface ScriptBrettify {

    public static void main(String[] args) {
        System.out.println(brettify.parse("""

                    function foo(ass, b, c) {
                        # just for test
                    }
                """));
    }

    Parser<String> string = between(ScriptBundle.textBody, token('\''));

    Parser<String> assignExpr = ScriptBundle.name;
    Parser<String> variables = reduce(between(assignExpr), comma, (x, y) -> x + ", " + y);
    Parser<String> function = left(string("function "), between(ScriptBundle.name))
            .follow(between(token('('), variables.or(string("")), token(')')))
            .map(new Function<Tuple<String, String>, String>() {
                @Override
                public String apply(Tuple<String, String> x) {
                    var name = x.first();
                    var params = x.second();
                    return name + " = (" + params + ") => ";
                }
            });

    Parser<String> brettify = string.map(x -> "`" + x + "`")
            .or(function)

            .or(any.map(x -> String.valueOf(x)))
            .asterisk();

    static <T, I> Parser<T> reduce(Parser<T> side, Parser<I> operator,
            GenericInfixEval<T, T, T> f) {
        return Parser.reduce(side, Parser.extend(operator, side, f).many());
    }

}
