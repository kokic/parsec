package parsec.bundle;

import static parsec.bundle.CharacterBundle.*;
import static parsec.bundle.IntegerBundle.*;

import java.util.function.Function;

import parsec.spec.Parser;
import parsec.variety.DoubleVariety;

public interface DoubleBundle {

    Function<Integer, Double> intToDouble = x -> (double) x;

    Parser<Double> decimalExact = integer.follow(ddot).follow(integer)
            .map(x -> Double.parseDouble(x.first().first() + "." + x.second()));
    Parser<Double> decimal = decimalExact.or(integer.map(intToDouble));

    DoubleVariety<Double> variety = new DoubleVariety<Double>() {

        @Override
        public Double fromNumber(Double x) {
            return x;
        }

        @Override
        public Double toNumber(Double x) {
            return x;
        }

        @Override
        public Parser<Double> primary() {
            return decimal;
        }
        
    };
}
