package net.skds.wpo.util.tuples;

public class Tuple3<T1, T2, T3> {
    public final T1 first;
    public final T2 second;
    public final T3 third;

    public Tuple3(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}
