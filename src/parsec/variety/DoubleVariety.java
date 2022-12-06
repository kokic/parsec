package parsec.variety;

import parsec.spec.DetInteger;
import parsec.spec.InfixEval;

public abstract class DoubleVariety<T> extends ArithmeticVariety<T, Double> {

    @SuppressWarnings("unchecked")
    public T fromNumber(Double x) {
        return (T) x;
    }

    public abstract Double toNumber(T x);

    @Override
    public T negate(T x) {
        return fromNumber(-toNumber(x));
    }

    @Override
    public T factorial(T x) {
        return fromNumber(DetInteger.factorial(toNumber(x).intValue())
                .doubleValue());
    }

    @Override
    public T pow(T x, T y) {
        return fromNumber(Math.pow(toNumber(x), toNumber(y)));
    }

    @Override
    public T mul(T x, T y) {
        return fromNumber(toNumber(x) * toNumber(y));
    }

    @Override
    public T div(T x, T y) {
        return fromNumber(toNumber(x) / toNumber(y));
    }

    @Override
    public T mod(T x, T y) {
        return fromNumber(toNumber(x) % toNumber(y));
    }

    @Override
    public T add(T x, T y) {
        return fromNumber(toNumber(x) + toNumber(y));
    }

    @Override
    public T sub(T x, T y) {
        return fromNumber(toNumber(x) - toNumber(y));
    }

    @Override
    public T lsh(T x, T y) {
        return embedInt(x, y, (a, b) -> a >> b);
    }

    @Override
    public T rsh(T x, T y) {
        return embedInt(x, y, (a, b) -> a << b);
    }

    public T embedInt(T x, T y, InfixEval<Integer> operator) {
        var integer = operator.eval(toNumber(x).intValue(), toNumber(y).intValue());
        return fromNumber((double) integer);
    }
}
