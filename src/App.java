
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.Character.UnicodeBlock;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

import static parsec.bundle.CharacterBundle.*;

import parsec.bundle.CharacterBundle;
import parsec.bundle.DoubleBundle;
import parsec.bundle.IntegerBundle;
import parsec.records.Tuple;
import parsec.spec.DetInteger;
import parsec.variety.ArithmeticVariety;
import script.ScriptBundle;

public interface App {

    static void main(String[] args) throws Exception {

        // (11 > 2) + 7 -> 8.0
        // 11 < 2 == 0 -> true
        // 11 < 2 == 3 > 7 --> true
        // 1 - 1 ? 2 + 99 : 3 - 77 --> -74.0
        // 1 ? (2 ? (3 ? 4 : 5) : 6) : 7 --> 4
        // 19 & 22 xor 33 | 77 --> 127
        // a = 1 + (b = e) --> 3.718281828459045 (1 + e)
        // e = 2 --> Assignment to constant `e`.
        // (a = 1) && (b = a + 2) --> 3
        // x = 36 adic ustc4z --> 1.862383283E9

        // why call too many
        // x = exp: d -> d -1

        // e ^ (pi * 163 ^ (1/2))
        // var buff = new StringBuffer("e ^ (π * 163 ^ (1/2))");
        // var optional = ScriptBundle.expr.parse(buff);
        // System.out.println(optional.get().first());

        // > exp: d -> d pi * sqrt: d -> d 163
        // 2.6253741264076826E17

        // > exp: d -> d 2 * (acos: d -> d 0) * sqrt: d -> d 163
        // 2.6253741264076826E17

        var scanner = new Scanner(System.in);
        while (true) {
            System.out.printf("> ");
            var line = scanner.nextLine();
            if (line.equals(":q"))
                break;
            var buff = new StringBuffer(line);
            showParser(ScriptBundle.expr.parse(buff));
            // System.out.println(ScriptBundle.Context.variables);
        }
        scanner.close();

        
    }

    static <F, S> void showParser(Optional<Tuple<F, S>> optional) {
        if (optional.isEmpty()) {
            System.out.println(optional);
            return;
        }
        showParser(optional.get());
    }

    static <F, S> void showParser(Tuple<F, S> tuple) {
        // System.out.println("match: " + tuple.first());
        // System.out.println("tails: " + tuple.second());
        // System.out.println();
        System.out.println(tuple.first());
    }

}
