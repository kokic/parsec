package script;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.foreign.MemoryLayout;

import foreign.CLib;
import foreign.NFInfer;
import parsec.spec.Parser;
import parsec.variety.LogicVariety;
import parsec.records.Tuple;

import static parsec.bundle.CharacterBundle.*;
import static parsec.bundle.DoubleBundle.*;
import static parsec.spec.Parser.*;

public interface ScriptBundle {

    static enum DataLayout {
        Text
    }
    

    Parser<String> name = string(Character::isJavaIdentifierStart, Character::isJavaIdentifierPart);

    Parser<String> textBody = string("\\'")
            .or(character(x -> x != '\'').map(x -> String.valueOf(x)))
            .asterisk();

    Parser<Tuple<DataLayout, String>> text = between(textBody, token('\''))
            .map(x -> Tuple.of(DataLayout.Text, x));

    LogicVariety<Supplier<Object>> variety = new LogicVariety<Supplier<Object>>() {

        // @Override
        // public Supplier<Object> add(Supplier<Object> x, Supplier<Object> y) {
        // if (!(x.get() instanceof Tuple<?, ?> t && t.first() instanceof DataLayout
        // layout))
        // return fromDouble(toDouble(x) + toDouble(y));
        // return switch (layout) {
        // case Text -> () -> "wa";
        // };
        // }

        @Override 
        public Supplier<Object> fromDouble(Double d) {
            return () -> d;
        }

        @Override
        public Double toDouble(Supplier<Object> x) {
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
            System.out.print(a++ + " ");
            return invokeExpr()
                    .or(name.map(x -> () -> x))
                    .or(decimal.map(x -> () -> x))
                    .or(text.map(x -> () -> x));
        }

        @Override
        public Boolean eq(Supplier<Object> x, Supplier<Object> y) {
            return switch (x.get()) {
                case Double d -> d.equals(toDouble(y));
                case Boolean b -> b.equals(toBoolean(y));
                case String s -> s.equals(y.toString());
                default -> x.equals(y);
            };
        }

        @Override
        public Boolean lt(Supplier<Object> x, Supplier<Object> y) {
            return toDouble(x) < toDouble(y);
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
                case Tuple<?, ?> tuple when tuple.first() instanceof DataLayout layout ->
                    switch (layout) {
                        case Text -> tuple.second();
                    };
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
