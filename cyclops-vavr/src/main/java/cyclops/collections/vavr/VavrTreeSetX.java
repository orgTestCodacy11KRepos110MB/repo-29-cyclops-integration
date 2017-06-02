package cyclops.collections.vavr;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyPOrderedSetX;
import com.aol.cyclops2.types.Unwrapable;
import cyclops.collections.immutable.OrderedSetX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.POrderedSet;


import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VavrTreeSetX<T> extends AbstractSet<T>implements POrderedSet<T>, Unwrapable {

    public static <T> OrderedSetX<T> copyFromCollection(CollectionX<T> vec, Comparator<T> comp) {

        return VavrTreeSetX.empty(comp)
                .plusAll(vec);

    }

    @Override
    public <R> R unwrap() {
        return (R)set;
    }

    /**
     * Create a LazyPOrderedSetX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPOrderedSetX
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> fromStream(Stream<T> stream) {
        return new LazyPOrderedSetX<T>(null,ReactiveSeq.fromStream(stream), toPOrderedSet());
    }

    /**
     * Create a LazyPOrderedSetX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPOrderedSetX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyPOrderedSetX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPOrderedSetX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyPOrderedSetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T extends Comparable<? super T>> LazyPOrderedSetX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPOrderedSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                     .limit(limit));
    }

    /**
     * Create a LazyPOrderedSetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                     .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * POrderedSet<Integer> q = VavrTreeSetX.<Integer>toPOrderedSet()
                                      .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for POrderedSet
     */
    public static <T extends Comparable<? super T>> Reducer<POrderedSet<T>> toPOrderedSet() {
        return Reducer.<POrderedSet<T>> of(VavrTreeSetX.emptyPOrderedSet(), (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                           (final T x) -> VavrTreeSetX.singleton(x));
    }
    public static <T> Reducer<POrderedSet<T>> toPOrderedSet(Comparator<? super T> comparator) {
        return Reducer.<POrderedSet<T>> of(VavrTreeSetX.emptyPOrderedSet(comparator), (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                           (final T x) -> VavrTreeSetX.singleton(comparator,x));
    }
    public static <T extends Comparable<? super T>> VavrTreeSetX<T> emptyPOrderedSet() {
        return new VavrTreeSetX<T>(TreeSet.empty());
    }
    public static <T> VavrTreeSetX<T> emptyPOrderedSet(Comparator<? super T> comparator) {
        return new VavrTreeSetX<T>(TreeSet.empty(comparator));
    }
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> empty() {
        return fromPOrderedSet(new VavrTreeSetX<T>(
                TreeSet.empty()),toPOrderedSet());
    }
    private static <T> LazyPOrderedSetX<T> fromPOrderedSet(POrderedSet<T> ordered, Reducer<POrderedSet<T>> reducer) {
        return  new LazyPOrderedSetX<T>(ordered,null,reducer);
    }
    public static <T> LazyPOrderedSetX<T> empty(Comparator<? super T> comparator) {
        return fromPOrderedSet(new VavrTreeSetX<T>(
                TreeSet.empty(comparator)),toPOrderedSet(comparator));
    }
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> singleton(T t) {
        return fromPOrderedSet(new VavrTreeSetX<>(
                TreeSet.of(t)),toPOrderedSet());
    }
    public static <T>  LazyPOrderedSetX<T> singleton(Comparator<? super T> comparator,T t) {
        return fromPOrderedSet(new VavrTreeSetX<T>(
                TreeSet.of(comparator,t)),toPOrderedSet(comparator));
       
    }

    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> of(T... t) {
        return fromPOrderedSet(new VavrTreeSetX<>(
                                        TreeSet.of(t)),toPOrderedSet());
    }

    public static <T> LazyPOrderedSetX<T> ofAll(SortedSet<T> set) {
        return fromPOrderedSet(new VavrTreeSetX<>(set), toPOrderedSet(set.comparator()));
    }
    public static <T> LazyPOrderedSetX<T> POrderedSet(SortedSet<T> set) {
        return fromPOrderedSet(new VavrTreeSetX<>(set), toPOrderedSet(set.comparator()));
    }

    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> POrderedSet(T... elements) {
        return fromPOrderedSet(of(elements), toPOrderedSet());
    }

    @Wither
    private final SortedSet<T> set;

    @Override
    public POrderedSet<T> plus(T e) {
        return withSet(set.add(e));
    }

    @Override
    public POrderedSet<T> plusAll(Collection<? extends T> l) {
        return withSet(set.addAll(l));
    }

    @Override
    public POrderedSet<T> minus(Object e) {
        return withSet(set.remove((T) e));
    }

    @Override
    public POrderedSet<T> minusAll(Collection<?> l) {
        return withSet(set.removeAll((Collection) l));
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public T get(int index) {
        if (index > set.size() || index < 0)
            throw new IndexOutOfBoundsException();
        T result = set.get();
        for (int i = 0; i < index; i++) {
            result = set.get();
        }
        return result;
    }

    @Override
    public int indexOf(Object o) {
        return set.toStream()
                  .zipWithIndex()
                  .find(t -> t._1.equals(o))
                  .map(t -> t._2)
                  .getOrElse(-1)
                  .intValue();
    }

}