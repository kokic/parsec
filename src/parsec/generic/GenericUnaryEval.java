package parsec.generic;

public interface GenericUnaryEval<T, V> {
    V eval(T target);
}
