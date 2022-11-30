package parsec.records;

public record Tuple<F, S> (F first, S second) {

    @Override
    public String toString() {
        return String.format("(%s, %s)", first, second);
    }
}