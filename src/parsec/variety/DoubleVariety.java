package parsec.variety;

import parsec.spec.DetInteger;
import parsec.spec.InfixEval;

public abstract class DoubleVariety<T> extends ArithmeticVariety<T> {

    @SuppressWarnings("unchecked")
    public T fromDouble(Double x) {
        return (T) x;
    }

    public abstract Double toDouble(T x);

    @Override
    public T negate(T x) {
        return fromDouble(-toDouble(x));
    }

    @Override
    public T factorial(T x) {
        return fromDouble(DetInteger.factorial(toDouble(x).intValue())
                .doubleValue());
    }

    @Override
    public T pow(T x, T y) {
        return fromDouble(Math.pow(toDouble(x), toDouble(y)));
    }

    @Override
    public T mul(T x, T y) {
        return fromDouble(toDouble(x) * toDouble(y));
    }

    @Override
    public T div(T x, T y) {
        return fromDouble(toDouble(x) / toDouble(y));
    }

    @Override
    public T mod(T x, T y) {
        return fromDouble(toDouble(x) % toDouble(y));
    }

    @Override
    public T add(T x, T y) {
        return fromDouble(toDouble(x) + toDouble(y));
    }

    @Override
    public T sub(T x, T y) {
        return fromDouble(toDouble(x) - toDouble(y));
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
        var integer = operator.eval(toDouble(x).intValue(), toDouble(y).intValue());
        return fromDouble((double) integer);
    }
}
