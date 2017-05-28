package cyclops.companion.vavr;


import cyclops.conversion.vavr.FromCyclopsReact;
import cyclops.conversion.vavr.ToCyclopsReact;
import cyclops.monads.VavrWitness;
import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.types.Value;
import com.aol.cyclops2.types.anyM.AnyMValue;
import cyclops.collections.mutable.ListX;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.monads.AnyM;
import cyclops.stream.ReactiveSeq;
import javaslang.control.Try;
import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for working with JDK Tryals
 *
 * @author johnmcclean
 *
 */
@UtilityClass
public class Trys {

    public static <T> AnyMValue<VavrWitness.tryType,T> anyM(Try<T> tryType) {
        return AnyM.ofValue(tryType, VavrWitness.tryType.INSTANCE);
    }
    /**
     * Perform a For Comprehension over a Try, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Trys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Trys.forEach4;
     *
    forEach4(Try.just(1),
    a-> Try.just(a+1),
    (a,b) -> Try.<Integer>just(a+b),
    a                  (a,b,c) -> Try.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param value3 Nested Try
     * @param value4 Nested Try
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Try<R> forEach4(Try<? extends T1> value1,
                                                                 Function<? super T1, ? extends Try<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Try<R2>> value3,
                                                                 Fn3<? super T1, ? super R1, ? super R2, ? extends Try<R3>> value4,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Try<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Try<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     *
     * Perform a For Comprehension over a Try, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Trys.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Trys.forEach4;
     *
     *  forEach4(Try.just(1),
    a-> Try.just(a+1),
    (a,b) -> Try.<Integer>just(a+b),
    (a,b,c) -> Try.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param value3 Nested Try
     * @param value4 Nested Try
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Try<R> forEach4(Try<? extends T1> value1,
                                                                 Function<? super T1, ? extends Try<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Try<R2>> value3,
                                                                 Fn3<? super T1, ? super R1, ? super R2, ? extends Try<R3>> value4,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Try<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Try<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Try, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Trys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Trys.forEach3;
     *
    forEach3(Try.just(1),
    a-> Try.just(a+1),
    (a,b) -> Try.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param value3 Nested Try
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Try<R> forEach3(Try<? extends T1> value1,
                                                         Function<? super T1, ? extends Try<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Try<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Try<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });

    }

    /**
     *
     * Perform a For Comprehension over a Try, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Trys.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Trys.forEach3;
     *
     *  forEach3(Try.just(1),
    a-> Try.just(a+1),
    (a,b) -> Try.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param value3 Nested Try
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Try<R> forEach3(Try<? extends T1> value1,
                                                         Function<? super T1, ? extends Try<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Try<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Try<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });

    }

    /**
     * Perform a For Comprehension over a Try, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Trys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Trys.forEach;
     *
    forEach(Try.just(1),
    a-> Try.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T, R1, R> Try<R> forEach2(Try<? extends T> value1, Function<? super T, Try<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });



    }

    /**
     *
     * Perform a For Comprehension over a Try, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Trys.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Trys.forEach;
     *
     *  forEach(Try.just(1),
    a-> Try.just(a+1),
    (a,b) -> Try.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Try
     * @param value2 Nested Try
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Try with a combined value generated by the yielding function
     */
    public static <T, R1, R> Try<R> forEach2(Try<? extends T> value1, Function<? super T, ? extends Try<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Try<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });




    }
    /**
     * Sequence operation, take a Collection of Trys and turn it into a Try with a Collection
     * By constrast with {@link Trys#sequencePresent(CollectionX)}, if any Trys are empty the result
     * is an empty Try
     *
     * <pre>
     * {@code
     *
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();
     *
     *  Try<ListX<Integer>> opts = Trys.sequence(ListX.of(just, none, Try.of(1)));
    //Try.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Maybe with a List of values
     */
    public static <T> Try<ListX<T>> sequence(final CollectionX<Try<T>> opts) {
        return sequence(opts.stream()).map(s -> s.toListX());

    }
    /**
     * Sequence operation, take a Collection of Trys and turn it into a Try with a Collection
     * Only successes are retained. By constrast with {@link Trys#sequence(CollectionX)} Try#empty types are
     * tolerated and ignored.
     *
     * <pre>
     * {@code
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();
     *
     * Try<ListX<Integer>> maybes = Trys.sequencePresent(ListX.of(just, none, Try.of(1)));
    //Try.of(ListX.of(10, 1));
     * }
     * </pre>
     *
     * @param opts Trys to Sequence
     * @return Try with a List of values
     */
    public static <T> Try<ListX<T>> sequencePresent(final CollectionX<Try<T>> opts) {
        return sequence(opts.stream().filter(Try::isSuccess)).map(s->s.toListX());
    }
    /**
     * Sequence operation, take a Collection of Trys and turn it into a Try with a Collection
     * By constrast with {@link Trys#sequencePresent(CollectionX)} if any Try types are empty
     * the return type will be an empty Try
     *
     * <pre>
     * {@code
     *
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();
     *
     *  Try<ListX<Integer>> maybes = Trys.sequence(ListX.of(just, none, Try.of(1)));
    //Try.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Try with a List of values
     */
    public static <T> Try<ReactiveSeq<T>> sequence(final Stream<Try<T>> opts) {
        return AnyM.sequence(opts.map(Trys::anyM), VavrWitness.tryType.INSTANCE)
                .map(ReactiveSeq::fromStream)
                .to(VavrWitness::tryType);

    }
    /**
     * Accummulating operation using the supplied Reducer (@see cyclops2.Reducers). A typical use case is to accumulate into a Persistent Collection type.
     * Accumulates the present results, ignores empty Trys.
     *
     * <pre>
     * {@code
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();

     * Try<PersistentSetX<Integer>> opts = Try.accumulateJust(ListX.of(just, none, Try.of(1)), Reducers.toPersistentSetX());
    //Try.of(PersistentSetX.of(10, 1)));
     *
     * }
     * </pre>
     *
     * @param tryTypeals Trys to accumulate
     * @param reducer Reducer to accumulate values with
     * @return Try with reduced value
     */
    public static <T, R> Try<R> accumulatePresent(final CollectionX<Try<T>> tryTypeals, final Reducer<R> reducer) {
        return sequencePresent(tryTypeals).map(s -> s.mapReduce(reducer));
    }
    /**
     * Accumulate the results only from those Trys which have a value present, using the supplied mapping function to
     * convert the data from each Try before reducing them using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();

     *  Try<String> opts = Try.accumulateJust(ListX.of(just, none, Try.of(1)), i -> "" + i,
    Monoids.stringConcat);
    //Try.of("101")
     *
     * }
     * </pre>
     *
     * @param tryTypeals Trys to accumulate
     * @param mapper Mapping function to be applied to the result of each Try
     * @param reducer Monoid to combine values from each Try
     * @return Try with reduced value
     */
    public static <T, R> Try<R> accumulatePresent(final CollectionX<Try<T>> tryTypeals, final Function<? super T, R> mapper,
                                                     final Monoid<R> reducer) {
        return sequencePresent(tryTypeals).map(s -> s.map(mapper)
                .reduce(reducer));
    }
    /**
     * Accumulate the results only from those Trys which have a value present, using the
     * supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Try<Integer> just = Try.of(10);
    Try<Integer> none = Try.empty();

     *  Try<String> opts = Try.accumulateJust(Monoids.stringConcat,ListX.of(just, none, Try.of(1)),
    );
    //Try.of("101")
     *
     * }
     * </pre>
     *
     * @param tryTypeals Trys to accumulate
     * @param reducer Monoid to combine values from each Try
     * @return Try with reduced value
     */
    public static <T> Try<T> accumulatePresent(final Monoid<T> reducer, final CollectionX<Try<T>> tryTypeals) {
        return sequencePresent(tryTypeals).map(s -> s
                .reduce(reducer));
    }

    /**
     * Combine an Try with the provided value using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Trys.combine(Try.of(10),Maybe.just(20), this::add)
     *  //Try[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Try to combine with a value
     * @param v Value to combine
     * @param fn Combining function
     * @return Try combined with supplied value
     */
    public static <T1, T2, R> Try<R> combine(final Try<? extends T1> f, final Value<? extends T2> v,
                                                final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.toTry(ToCyclopsReact.toTry(f)
                .combine(v, fn)));
    }
    /**
     * Combine an Try with the provided Try using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Trys.combine(Try.of(10),Try.of(20), this::add)
     *  //Try[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param f Try to combine with a value
     * @param v Try to combine
     * @param fn Combining function
     * @return Try combined with supplied value, or empty Try if no value present
     */
    public static <T1, T2, R> Try<R> combine(final Try<? extends T1> f, final Try<? extends T2> v,
                                                final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return combine(f,ToCyclopsReact.toTry(v),fn);
    }

    /**
     * Combine an Try with the provided Iterable (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Trys.zip(Try.of(10),Arrays.asList(20), this::add)
     *  //Try[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Try to combine with first element in Iterable (if present)
     * @param v Iterable to combine
     * @param fn Combining function
     * @return Try combined with supplied Iterable, or empty Try if no value present
     */
    public static <T1, T2, R> Try<R> zip(final Try<? extends T1> f, final Iterable<? extends T2> v,
                                            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.toTry(ToCyclopsReact.toTry(f)
                .zip(v, fn)));
    }

    /**
     * Combine an Try with the provided Publisher (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Trys.zip(Flux.just(10),Try.of(10), this::add)
     *  //Try[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param p Publisher to combine
     * @param f  Try to combine with
     * @param fn Combining function
     * @return Try combined with supplied Publisher, or empty Try if no value present
     */
    public static <T1, T2, R> Try<R> zip(final Publisher<? extends T2> p, final Try<? extends T1> f,
                                            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.toTry(ToCyclopsReact.toTry(f)
                .zipP(p, fn)));
    }
    /**
     * Narrow covariant type parameter
     *
     * @param tryTypeal Try with covariant type parameter
     * @return Narrowed Try
     */
    public static <T> Try<T> narrow(final Try<? extends T> tryTypeal) {
        return (Try<T>) tryTypeal;
    }

    

}
