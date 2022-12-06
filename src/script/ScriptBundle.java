package script;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.foreign.MemoryLayout;

import foreign.CLib;
import foreign.NFInfer;
import parsec.spec.DetInteger;
import parsec.spec.InfixEval;
import parsec.spec.Parser;
import parsec.variety.LogicVariety;

import static parsec.bundle.CharacterBundle.*;
import static parsec.bundle.DoubleBundle.*;
import static parsec.spec.Parser.*;

public interface ScriptBundle {

    static record Text(String value) {
        @Override
        public String toString() {
            return value;
        }
    }

    Parser<String> name = string(Character::isJavaIdentifierStart, Character::isJavaIdentifierPart);

    Parser<String> textBody = string("\\'")
            .or(character(x -> x != '\'').map(x -> String.valueOf(x)))
            .asterisk();

    Parser<Text> text = between(textBody, token('\'')).map(Text::new);

    LogicVariety<Supplier<Object>, Double> variety = new LogicVariety<Supplier<Object>, Double>() {

        @Override
        public Supplier<Object> fromNumber(Double d) {
            return () -> d;
        }

        @Override
        public Double toNumber(Supplier<Object> x) {
            return switch (x.get()) {
                case Double d -> d;
                case Boolean b -> b ? 1d : 0d;
                case String s -> (Double) Context.query(s);
                default -> throw new IllegalArgumentException(
                        "Unexpected value: " + x + " to double");
            };
        }

        @Override
        public Supplier<Object> fromBoolean(Boolean b) {
            return () -> b;
        }

        @Override
        public Boolean toBoolean(Supplier<Object> x) {
            return switch (x.get()) {
                case Double d -> d != 0;
                case Boolean b -> b;
                case String s -> (Boolean) Context.query(s);
                default -> throw new IllegalArgumentException(
                        "Unexpected value: " + x + " to boolean");
            };
        }

        int a = 0;

        @Override
        public Parser<Supplier<Object>> primary() {
            System.out.println(a++ + " ");
            return invokeExpr()
                    .or(name.map(x -> () -> x))
                    .or(decimal.map(x -> () -> x))
                    .or(text.map(x -> () -> x));
        }

        @Override
        public Boolean eq(Supplier<Object> x, Supplier<Object> y) {
            return switch (x.get()) {
                case Double d -> d.equals(toNumber(y));
                case Boolean b -> b.equals(toBoolean(y));
                case String s -> s.equals(y.toString());
                default -> x.equals(y);
            };
        }

        @Override
        public Boolean lt(Supplier<Object> x, Supplier<Object> y) {
            return toNumber(x) < toNumber(y);
        }

        public Supplier<Object> assign(Supplier<Object> x, Supplier<Object> y) {
            return switch (x.get()) {
                case String s -> () -> Context.assign(s, y.get());
                default -> throw new IllegalArgumentException(
                        "Unexpected value: " + x + " to assign");
            };
        }

        public Parser<Character> assignInf = between(token('='));
        public Parser<Function<Supplier<Object>, Supplier<Object>>> assignExt = extend(
                assignInf, () -> this.assignExpr, this::assign);
        public Parser<Supplier<Object>> assignExpr = reduce(condExpr, assignExt.many());

        @Override
        public Parser<Supplier<Object>> condFlow() {
            return assignExpr;
        }

        static final HashMap<String, MemoryLayout> itaniumGCC = new HashMap<>();
        static {
            itaniumGCC.put("i", CLib.Int);
            itaniumGCC.put("l", CLib.Long);
            itaniumGCC.put("f", CLib.Float);
            itaniumGCC.put("d", CLib.Double);
            itaniumGCC.put("_", CLib.Addr /* address */);
        }

        public Object invoke(String name, List<String> params, String res,
                List<Object> args) {
            var val = new NFInfer(name, itaniumGCC.get(res))
                    .invokeOrCalm(args.toArray());
            return val;
        }

        public Parser<Supplier<Object>> invokeExpr() {
            var parser = name.skip(between(colon))
                    .follow(between(name).some())
                    .skip(between(string("->")))
                    .follow(name)
                    .follow(() -> left(spaceEx, assignExpr).some());
            return parser.map(tree -> (Supplier<Object>) () -> {
                var tuple1 = tree.first();
                var tuple2 = tuple1.first();
                var name = tuple2.first();
                var params = tuple2.second();
                var res = tuple1.second();
                var stream = tree.second().stream();
                var argsv = stream.map(x -> Context.eval(x.get())).toList();
                // System.out.println("argv: " + argsv);
                return invoke(name, params, res, argsv);
            });
        }

        @Override
        public Parser<Supplier<Object>> expr() {
            return assignExpr.map(x -> () -> Context.eval(x.get()));
        }

        @Override
        public Supplier<Object> applyInt(Supplier<Object> x, Supplier<Object> y, InfixEval<Integer> operator) {
            var integer = operator.eval(toNumber(x).intValue(), toNumber(y).intValue());
            return fromNumber(integer.doubleValue());
        }

        @Override
        public Supplier<Object> negate(Supplier<Object> x) {
            return fromNumber(-toNumber(x));
        }

        @Override
        public Supplier<Object> factorial(Supplier<Object> x) {
            return fromNumber(DetInteger.factorial(toNumber(x).intValue())
                    .doubleValue());
        }

        @Override
        public Supplier<Object> pow(Supplier<Object> x, Supplier<Object> y) {
            return fromNumber(Math.pow(toNumber(x), toNumber(y)));
        }

        @Override
        public Supplier<Object> mul(Supplier<Object> x, Supplier<Object> y) {
            return fromNumber(toNumber(x) * toNumber(y));
        }

        @Override
        public Supplier<Object> div(Supplier<Object> x, Supplier<Object> y) {
            return fromNumber(toNumber(x) / toNumber(y));
        }

        @Override
        public Supplier<Object> mod(Supplier<Object> x, Supplier<Object> y) {
            return fromNumber(toNumber(x) % toNumber(y));
        }

        @Override
        public Supplier<Object> add(Supplier<Object> x, Supplier<Object> y) {
            return switch (x.get()) {
                case Text t -> () -> new Text(t.value + y.get());
                default -> fromNumber(toNumber(x) + toNumber(y));
            };
        }

        @Override
        public Supplier<Object> sub(Supplier<Object> x, Supplier<Object> y) {
            return fromNumber(toNumber(x) - toNumber(y));
        }

        @Override
        public Supplier<Object> lsh(Supplier<Object> x, Supplier<Object> y) {
            return applyInt(x, y, (a, b) -> a >> b);
        }

        @Override
        public Supplier<Object> rsh(Supplier<Object> x, Supplier<Object> y) {
            return applyInt(x, y, (a, b) -> a << b);
        }
    };

    Parser<Supplier<Object>> expr = variety.expr();

    class Context {
        public static final HashMap<String, Object> constants, variables;

        static {
            constants = new HashMap<String, Object>();
            constants.put("e", Math.E);
            constants.put("π", Math.PI);
            constants.put("pi", Math.PI);

            variables = new HashMap<String, Object>();

            // variables.put("true", true);
            // variables.put("false", false);
        }

        public static boolean isUndefined(Object value) {
            return value.equals("undefined");
        }

        public static Object query(String identifier) {
            return switch (identifier) {
                case String v when variables.containsKey(v) -> variables.get(v);
                case String c when constants.containsKey(c) -> constants.get(c);
                default -> "undefined";
            };
        }

        public static Object eval(Object valueOrRef) {
            return switch (valueOrRef) {
                case Text text -> text.value;
                case String identifier -> query(identifier);
                default -> valueOrRef;
            };
        }

        public static Object assign(String identifier, Object value) {
            if (constants.containsKey(identifier))
                throw new RuntimeException(
                        "Assignment to constant `" + identifier + "`.");
            variables.put(identifier, eval(value));
            return value;
        }

    }

}
