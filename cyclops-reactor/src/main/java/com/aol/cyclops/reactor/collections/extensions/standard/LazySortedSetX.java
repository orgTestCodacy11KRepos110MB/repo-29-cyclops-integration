package com.aol.cyclops.reactor.collections.extensions.standard;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Streamable;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.data.collections.extensions.standard.SortedSetX;
import com.aol.cyclops.reactor.Fluxes;
import com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX;
import com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;
import reactor.core.publisher.Flux;

/**
 * An extended SortedSet type {@see java.util.SortedSet}
 * Extended SortedSet operations execute lazily e.g.
 * <pre>
 * {@code 
 *    LazySortedSetX<Integer> q = LazySortedSetX.of(1,2,3)
 *                                              .map(i->i*2);
 * }
 * </pre>
 * The map operation above is not executed immediately. It will only be executed when (if) the data inside the
 * SortedSet is accessed. This allows lazy operations to be chained and executed more efficiently e.g.
 * 
 * <pre>
 * {@code 
 *    LazySortedSetX<Integer> q = LazySortedSetX.of(1,2,3)
 *                                      .map(i->i*2);
 *                                      .filter(i->i<5);
 * }
 * </pre>
 * 
 * The operation above is more efficient than the equivalent operation with a SortedSetX.
 * 
 * NB. Because LazySortedSetX transform operations are performed Lazily, they may result in a different result 
 * than SortedSetX operations that are performed eagerly. For example a sequence of map operations that result in
 * duplicate keys, may result in a different SortedSet being produced.
 * 
 * @author johnmcclean
 *
 * @param <T> the type of elements held in this collection
 */
@AllArgsConstructor(access=AccessLevel.PRIVATE)
public class LazySortedSetX<T> extends AbstractFluentCollectionX<T>implements SortedSetX<T> {
    private final LazyFluentCollection<T, SortedSet<T>> lazy;
    @Getter @Wither
    private final Collector<T, ?, SortedSet<T>> collector;

    @Override
    public LazySortedSetX<T> plusLoop(int max, IntFunction<T> value){
       return (LazySortedSetX<T>)super.plusLoop(max, value);
    }
    @Override
    public LazySortedSetX<T> plusLoop(Supplier<Optional<T>> supplier){
       return (LazySortedSetX<T>)super.plusLoop(supplier);
    }
    /**
     * Create a LazySortedSetX from a Stream
     * 
     * @param stream to construct a LazySortedSetX from
     * @return LazySortedSetX
     */
    public static <T> LazySortedSetX<T> fromStreamS(Stream<T> stream) {
        return new LazySortedSetX<T>(
                                     Flux.from(ReactiveSeq.fromStream(stream)));
    }

    /**
     * Create a LazySortedSetX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range LazySortedSetX
     */
    public static LazySortedSetX<Integer> range(int start, int end) {
        return fromStreamS(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazySortedSetX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range LazySortedSetX
     */
    public static LazySortedSetX<Long> rangeLong(long start, long end) {
        return fromStreamS(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a LazySortedSetX
     * 
     * <pre>
     * {@code 
     *  LazySortedSetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return LazySortedSetX generated by unfolder function
     */
    public static <U, T> LazySortedSetX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStreamS(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazySortedSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return LazySortedSetX generated from the provided Supplier
     */
    public static <T> LazySortedSetX<T> generate(long limit, Supplier<T> s) {

        return fromStreamS(ReactiveSeq.generate(s)
                                      .limit(limit));
    }

    /**
     * Create a LazySortedSetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazySortedSetX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStreamS(ReactiveSeq.iterate(seed, f)
                                      .limit(limit));
    }

    /**
     * @return A collector that generates a LazySortedSetX
     */
    public static <T> Collector<T, ?, LazySortedSetX<T>> lazyListXCollector() {
        return Collectors.toCollection(() -> LazySortedSetX.of());
    }

    /**
     * @return An empty LazySortedSetX
     */
    public static <T> LazySortedSetX<T> empty() {
        return fromIterable((List<T>) ListX.<T> defaultCollector()
                                           .supplier()
                                           .get());
    }

    /**
     * Create a LazySortedSetX from the specified values
     * <pre>
     * {@code 
     *     ListX<Integer> lazy = LazySortedSetX.of(1,2,3,4,5);
     *     
     *     //lazily map List
     *     ListX<String> mapped = lazy.map(i->"mapped " +i); 
     *     
     *     String value = mapped.get(0); //transformation triggered now
     * }
     * </pre>
     * 
     * @param values To populate LazySortedSetX with
     * @return LazySortedSetX
     */
    @SafeVarargs
    public static <T> LazySortedSetX<T> of(T... values) {
        List<T> res = (List<T>) ListX.<T> defaultCollector()
                                     .supplier()
                                     .get();
        for (T v : values)
            res.add(v);
        return fromIterable(res);
    }

    /**
     * Construct a LazySortedSetX with a single value
     * <pre>
     * {@code 
     *    ListX<Integer> lazy = LazySortedSetX.singleton(5);
     *    
     * }
     * </pre>
     * 
     * 
     * @param value To populate LazySortedSetX with
     * @return LazySortedSetX with a single value
     */
    public static <T> LazySortedSetX<T> singleton(T value) {
        return LazySortedSetX.<T> of(value);
    }

    /**
     * Construct a LazySortedSetX from an Publisher
     * 
     * @param publisher
     *            to construct LazySortedSetX from
     * @return ListX
     */
    public static <T> LazySortedSetX<T> fromPublisher(Publisher<? extends T> publisher) {
        return fromStreamS(ReactiveSeq.fromPublisher((Publisher<T>) publisher));
    }

    /**
     * Construct LazySortedSetX from an Iterable
     * 
     * @param it to construct LazySortedSetX from
     * @return LazySortedSetX from Iterable
     */
    public static <T> LazySortedSetX<T> fromIterable(Iterable<T> it) {
        return fromIterable(SortedSetX.<T> defaultCollector(), it);
    }

    /**
     * Construct a LazySortedSetX from an Iterable, using the specified Collector.
     * 
     * @param collector To generate Lists from, this can be used to create mutable vs immutable Lists (for example), or control List type (ArrayList, LinkedList)
     * @param it Iterable to construct LazySortedSetX from
     * @return Newly constructed LazySortedSetX
     */
    public static <T> LazySortedSetX<T> fromIterable(Collector<T, ?, SortedSet<T>> collector, Iterable<T> it) {
        if (it instanceof LazySortedSetX)
            return (LazySortedSetX<T>) it;

        if (it instanceof SortedSet)
            return new LazySortedSetX<T>(
                                         (SortedSet<T>) it, collector);
        return new LazySortedSetX<T>(
                                     Flux.fromIterable(it), collector);
    }

    private LazySortedSetX(SortedSet<T> list, Collector<T, ?, SortedSet<T>> collector) {
        this.lazy = new LazyCollection<>(
                                         list, null, collector);
        this.collector = collector;
    }

    private LazySortedSetX(SortedSet<T> list) {
        this.collector = SortedSetX.defaultCollector();
        this.lazy = new LazyCollection<T, SortedSet<T>>(
                                                        list, null, collector);
    }

    private LazySortedSetX(Flux<T> stream, Collector<T, ?, SortedSet<T>> collector) {

        this.collector = collector;
        this.lazy = new LazyCollection<>(
                                         null, stream, collector);
    }

    private LazySortedSetX(Flux<T> stream) {

        this.collector = SortedSetX.defaultCollector();
        this.lazy = new LazyCollection<>(
                                         null, stream, collector);
    }

    private LazySortedSetX() {
        this.collector = SortedSetX.defaultCollector();
        this.lazy = new LazyCollection<>(
                                         (SortedSet) this.collector.supplier()
                                                                   .get(),
                                         null, collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        getSortedSet().forEach(action);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return getSortedSet().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        return getSortedSet().size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object e) {
        return getSortedSet().contains(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return getSortedSet().equals(o);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return getSortedSet().isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getSortedSet().hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        return getSortedSet().toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return getSortedSet().removeAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return getSortedSet().toArray(a);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(T e) {
        return getSortedSet().add(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        return getSortedSet().remove(o);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return getSortedSet().containsAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        return getSortedSet().addAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return getSortedSet().retainAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#clear()
     */
    @Override
    public void clear() {
        getSortedSet().clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSortedSet().toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jooq.lambda.Collectable#collect(java.util.stream.Collector)
     */
    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jooq.lambda.Collectable#count()
     */
    @Override
    public long count() {
        return this.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#removeIf(java.util.function.Predicate)
     */
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return getSortedSet().removeIf(filter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public Stream<T> parallelStream() {
        return getSortedSet().parallelStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#spliterator()
     */
    @Override
    public Spliterator<T> spliterator() {
        return getSortedSet().spliterator();
    }

    private SortedSet<T> getSortedSet() {
        return lazy.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#stream(reactor.core.publisher.Flux)
     */
    @Override
    public <X> LazySortedSetX<X> stream(Flux<X> stream) {
        return new LazySortedSetX<X>(
                                     stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.persistent.PBagX#stream()
     */
    @Override
    public ReactiveSeq<T> stream() {
        return ReactiveSeq.fromStream(lazy.get()
                                          .stream());
    }

    @Override
    public Flux<T> flux() {
        return lazy.flux();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#combine(
     * java.util.function.BiPredicate, java.util.function.BinaryOperator)
     */
    @Override
    public LazySortedSetX<T> combine(BiPredicate<? super T, ? super T> predicate, BinaryOperator<T> op) {

        return (LazySortedSetX<T>) super.combine(predicate, op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#reverse()
     */
    @Override
    public LazySortedSetX<T> reverse() {

        return (LazySortedSetX<T>) super.reverse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#filter(
     * java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> filter(Predicate<? super T> pred) {

        return (LazySortedSetX<T>) super.filter(pred);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#map(java.
     * util.function.Function)
     */
    @Override
    public <R> LazySortedSetX<R> map(Function<? super T, ? extends R> mapper) {

        return (LazySortedSetX<R>) super.map(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#flatMap(
     * java.util.function.Function)
     */
    @Override
    public <R> LazySortedSetX<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return (LazySortedSetX<R>) super.flatMap(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#limit(
     * long)
     */
    @Override
    public LazySortedSetX<T> limit(long num) {
        return (LazySortedSetX<T>) super.limit(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#skip(
     * long)
     */
    @Override
    public LazySortedSetX<T> skip(long num) {
        return (LazySortedSetX<T>) super.skip(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#takeRight
     * (int)
     */
    @Override
    public LazySortedSetX<T> takeRight(int num) {
        return (LazySortedSetX<T>) super.takeRight(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#dropRight
     * (int)
     */
    @Override
    public LazySortedSetX<T> dropRight(int num) {
        return (LazySortedSetX<T>) super.dropRight(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#takeWhile
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> takeWhile(Predicate<? super T> p) {
        return (LazySortedSetX<T>) super.takeWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#dropWhile
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> dropWhile(Predicate<? super T> p) {
        return (LazySortedSetX<T>) super.dropWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#takeUntil
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> takeUntil(Predicate<? super T> p) {
        return (LazySortedSetX<T>) super.takeUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#dropUntil
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> dropUntil(Predicate<? super T> p) {
        return (LazySortedSetX<T>) super.dropUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * trampoline(java.util.function.Function)
     */
    @Override
    public <R> LazySortedSetX<R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (LazySortedSetX<R>) super.trampoline(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#slice(
     * long, long)
     */
    @Override
    public LazySortedSetX<T> slice(long from, long to) {
        return (LazySortedSetX<T>) super.slice(from, to);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#grouped(
     * int)
     */
    @Override
    public LazySortedSetX<ListX<T>> grouped(int groupSize) {

        return (LazySortedSetX<ListX<T>>) super.grouped(groupSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#grouped(
     * java.util.function.Function, java.util.stream.Collector)
     */
    @Override
    public <K, A, D> LazySortedSetX<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {

        return (LazySortedSetX) super.grouped(classifier, downstream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#zip(java.
     * lang.Iterable)
     */
    @Override
    public <U> LazySortedSetX<Tuple2<T, U>> zip(Iterable<? extends U> other) {

        return (LazySortedSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#zip(java.
     * lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazySortedSetX<R> zip(Iterable<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazySortedSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#sliding(
     * int)
     */
    @Override
    public LazySortedSetX<ListX<T>> sliding(int windowSize) {

        return (LazySortedSetX<ListX<T>>) super.sliding(windowSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#sliding(
     * int, int)
     */
    @Override
    public LazySortedSetX<ListX<T>> sliding(int windowSize, int increment) {

        return (LazySortedSetX<ListX<T>>) super.sliding(windowSize, increment);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#scanLeft(
     * com.aol.cyclops.Monoid)
     */
    @Override
    public LazySortedSetX<T> scanLeft(Monoid<T> monoid) {

        return (LazySortedSetX<T>) super.scanLeft(monoid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#scanLeft(
     * java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <U> LazySortedSetX<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {

        return (LazySortedSetX<U>) super.scanLeft(seed, function);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#scanRight
     * (com.aol.cyclops.Monoid)
     */
    @Override
    public LazySortedSetX<T> scanRight(Monoid<T> monoid) {

        return (LazySortedSetX<T>) super.scanRight(monoid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#scanRight
     * (java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <U> LazySortedSetX<U> scanRight(U identity, BiFunction<? super T, ? super U, ? extends U> combiner) {

        return (LazySortedSetX<U>) super.scanRight(identity, combiner);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#sorted(
     * java.util.function.Function)
     */
    @Override
    public <U extends Comparable<? super U>> LazySortedSetX<T> sorted(Function<? super T, ? extends U> function) {

        return (LazySortedSetX<T>) super.sorted(function);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#plus(java
     * .lang.Object)
     */
    @Override
    public LazySortedSetX<T> plus(T e) {

        return (LazySortedSetX<T>) super.plus(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#plusAll(
     * java.util.Collection)
     */
    @Override
    public LazySortedSetX<T> plusAll(Collection<? extends T> list) {

        return (LazySortedSetX<T>) super.plusAll(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#minus(
     * java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> minus(Object e) {

        return (LazySortedSetX<T>) super.minus(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#minusAll(
     * java.util.Collection)
     */
    @Override
    public LazySortedSetX<T> minusAll(Collection<?> list) {

        return (LazySortedSetX<T>) super.minusAll(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#plusLazy(
     * java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> plusLazy(T e) {

        return (LazySortedSetX<T>) super.plus(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * plusAllLazy(java.util.Collection)
     */
    @Override
    public LazySortedSetX<T> plusAllLazy(Collection<? extends T> list) {

        return (LazySortedSetX<T>) super.plusAll(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#minusLazy
     * (java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> minusLazy(Object e) {

        return (LazySortedSetX<T>) super.minus(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * minusAllLazy(java.util.Collection)
     */
    @Override
    public LazySortedSetX<T> minusAllLazy(Collection<?> list) {

        return (LazySortedSetX<T>) super.minusAll(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#cycle(
     * int)
     */
    @Override
    public LazyListX<T> cycle(int times) {
        return LazyListX.fromPublisher(this.flux()
                                           .repeat(times));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#cycle(com
     * .aol.cyclops.Monoid, int)
     */
    @Override
    public LazyListX<T> cycle(Monoid<T> m, int times) {
        return LazyListX.fromPublisher(Fluxes.cycle(flux(), m, times));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * cycleWhile(java.util.function.Predicate)
     */
    @Override
    public LazyListX<T> cycleWhile(Predicate<? super T> predicate) {

        return LazyListX.fromPublisher(Fluxes.cycleWhile(flux(), predicate));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * cycleUntil(java.util.function.Predicate)
     */
    @Override
    public LazyListX<T> cycleUntil(Predicate<? super T> predicate) {

        return LazyListX.fromPublisher(Fluxes.cycleUntil(flux(), predicate));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#zip(org.
     * jooq.lambda.Seq)
     */
    @Override
    public <U> LazySortedSetX<Tuple2<T, U>> zip(Seq<? extends U> other) {

        return (LazySortedSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#zip3(java
     * .util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    public <S, U> LazySortedSetX<Tuple3<T, S, U>> zip3(Stream<? extends S> second, Stream<? extends U> third) {

        return (LazySortedSetX) super.zip3(second, third);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#zip4(java
     * .util.stream.Stream, java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    public <T2, T3, T4> LazySortedSetX<Tuple4<T, T2, T3, T4>> zip4(Stream<? extends T2> second,
            Stream<? extends T3> third, Stream<? extends T4> fourth) {

        return (LazySortedSetX) super.zip4(second, third, fourth);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * zipWithIndex()
     */
    @Override
    public LazySortedSetX<Tuple2<T, Long>> zipWithIndex() {

        return (LazySortedSetX<Tuple2<T, Long>>) super.zipWithIndex();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#distinct(
     * )
     */
    @Override
    public LazySortedSetX<T> distinct() {

        return (LazySortedSetX<T>) super.distinct();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#sorted()
     */
    @Override
    public LazySortedSetX<T> sorted() {

        return (LazySortedSetX<T>) super.sorted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#sorted(
     * java.util.Comparator)
     */
    @Override
    public LazySortedSetX<T> sorted(Comparator<? super T> c) {

        return (LazySortedSetX<T>) super.sorted(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#skipWhile
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> skipWhile(Predicate<? super T> p) {

        return (LazySortedSetX<T>) super.skipWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#skipUntil
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> skipUntil(Predicate<? super T> p) {

        return (LazySortedSetX<T>) super.skipUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * limitWhile(java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> limitWhile(Predicate<? super T> p) {

        return (LazySortedSetX<T>) super.limitWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * limitUntil(java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> limitUntil(Predicate<? super T> p) {

        return (LazySortedSetX<T>) super.limitUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * intersperse(java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> intersperse(T value) {

        return (LazySortedSetX<T>) super.intersperse(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#shuffle()
     */
    @Override
    public LazySortedSetX<T> shuffle() {

        return (LazySortedSetX<T>) super.shuffle();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#skipLast(
     * int)
     */
    @Override
    public LazySortedSetX<T> skipLast(int num) {

        return (LazySortedSetX<T>) super.skipLast(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#limitLast
     * (int)
     */
    @Override
    public LazySortedSetX<T> limitLast(int num) {

        return (LazySortedSetX<T>) super.limitLast(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#onEmpty(
     * java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> onEmpty(T value) {

        return (LazySortedSetX<T>) super.onEmpty(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * onEmptyGet(java.util.function.Supplier)
     */
    @Override
    public LazySortedSetX<T> onEmptyGet(Supplier<? extends T> supplier) {

        return (LazySortedSetX<T>) super.onEmptyGet(supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    public <X extends Throwable> LazySortedSetX<T> onEmptyThrow(Supplier<? extends X> supplier) {

        return (LazySortedSetX<T>) super.onEmptyThrow(supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#shuffle(
     * java.util.Random)
     */
    @Override
    public LazySortedSetX<T> shuffle(Random random) {

        return (LazySortedSetX<T>) super.shuffle(random);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#ofType(
     * java.lang.Class)
     */
    @Override
    public <U> LazySortedSetX<U> ofType(Class<? extends U> type) {

        return (LazySortedSetX<U>) super.ofType(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#filterNot
     * (java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<T> filterNot(Predicate<? super T> fn) {

        return (LazySortedSetX<T>) super.filterNot(fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#notNull()
     */
    @Override
    public LazySortedSetX<T> notNull() {

        return (LazySortedSetX<T>) super.notNull();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#removeAll
     * (java.util.stream.Stream)
     */
    @Override
    public LazySortedSetX<T> removeAll(Stream<? extends T> stream) {

        return (LazySortedSetX<T>) (super.removeAll(stream));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#removeAll
     * (org.jooq.lambda.Seq)
     */
    @Override
    public LazySortedSetX<T> removeAll(Seq<? extends T> stream) {

        return (LazySortedSetX<T>) super.removeAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#removeAll
     * (java.lang.Iterable)
     */
    @Override
    public LazySortedSetX<T> removeAll(Iterable<? extends T> it) {

        return (LazySortedSetX<T>) super.removeAll(it);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#removeAll
     * (java.lang.Object[])
     */
    @Override
    public LazySortedSetX<T> removeAll(T... values) {

        return (LazySortedSetX<T>) super.removeAll(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#retainAll
     * (java.lang.Iterable)
     */
    @Override
    public LazySortedSetX<T> retainAll(Iterable<? extends T> it) {

        return (LazySortedSetX<T>) super.retainAll(it);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#retainAll
     * (java.util.stream.Stream)
     */
    @Override
    public LazySortedSetX<T> retainAll(Stream<? extends T> stream) {

        return (LazySortedSetX<T>) super.retainAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#retainAll
     * (org.jooq.lambda.Seq)
     */
    @Override
    public LazySortedSetX<T> retainAll(Seq<? extends T> stream) {

        return (LazySortedSetX<T>) super.retainAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#retainAll
     * (java.lang.Object[])
     */
    @Override
    public LazySortedSetX<T> retainAll(T... values) {

        return (LazySortedSetX<T>) super.retainAll(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#cast(java
     * .lang.Class)
     */
    @Override
    public <U> LazySortedSetX<U> cast(Class<? extends U> type) {

        return (LazySortedSetX<U>) super.cast(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    public <R> LazySortedSetX<R> patternMatch(Function<CheckValue1<T, R>, CheckValue1<T, R>> case1,
            Supplier<? extends R> otherwise) {

        return (LazySortedSetX<R>) super.patternMatch(case1, otherwise);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.SortedSetX#grouped(
     * int, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazySortedSetX<C> grouped(int size, Supplier<C> supplier) {

        return (LazySortedSetX<C>) super.grouped(size, supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * groupedUntil(java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<ListX<T>> groupedUntil(Predicate<? super T> predicate) {

        return (LazySortedSetX<ListX<T>>) super.groupedUntil(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * groupedWhile(java.util.function.Predicate)
     */
    @Override
    public LazySortedSetX<ListX<T>> groupedWhile(Predicate<? super T> predicate) {

        return (LazySortedSetX<ListX<T>>) super.groupedWhile(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * groupedWhile(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazySortedSetX<C> groupedWhile(Predicate<? super T> predicate,
            Supplier<C> factory) {

        return (LazySortedSetX<C>) super.groupedWhile(predicate, factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * groupedUntil(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazySortedSetX<C> groupedUntil(Predicate<? super T> predicate,
            Supplier<C> factory) {

        return (LazySortedSetX<C>) super.groupedUntil(predicate, factory);
    }

    /** SortedSetX methods **/

    /*
     * Makes a defensive copy of this SortedSetX replacing the value at i with
     * the specified element (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.collections.extensions.standard.MutableSequenceX#with(
     * int, java.lang.Object)
     */
    public LazySortedSetX<T> with(int i, T element) {
        return stream(Fluxes.insertAt(Fluxes.deleteBetween(flux(), i, i + 1), i, element));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX
     * #unit(java.util.Collection)
     */
    @Override
    public <R> LazySortedSetX<R> unit(Collection<R> col) {
        return LazySortedSetX.fromIterable(col);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Unit#unit(java.lang.Object)
     */
    @Override
    public <R> LazySortedSetX<R> unit(R value) {
        return LazySortedSetX.singleton(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#unitIterator(java.util.Iterator)
     */
    @Override
    public <R> LazySortedSetX<R> unitIterator(Iterator<R> it) {
        return LazySortedSetX.fromIterable(() -> it);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX
     * #plusInOrder(java.lang.Object)
     */
    @Override
    public LazySortedSetX<T> plusInOrder(T e) {

        return (LazySortedSetX<T>) super.plusInOrder(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#from(java.util.Collection)
     */
    @Override
    public <T1> LazySortedSetX<T1> from(Collection<T1> c) {
        if (c instanceof List)
            return new LazySortedSetX<T1>(
                                          (SortedSet) c, (Collector) collector);
        return new LazySortedSetX<T1>(
                                      (SortedSet) c.stream()
                                                   .collect(SortedSetX.defaultCollector()),
                                      (Collector) this.collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedStatefullyUntil(java.util.function.
     * BiPredicate)
     */
    @Override
    public LazySortedSetX<ListX<T>> groupedStatefullyUntil(BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (LazySortedSetX<ListX<T>>) super.groupedStatefullyUntil(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#peek(java.util.function.Consumer)
     */
    @Override
    public LazySortedSetX<T> peek(Consumer<? super T> c) {

        return (LazySortedSetX) super.peek(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(org.jooq.lambda.Seq,
     * java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazySortedSetX<R> zip(Seq<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazySortedSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.stream.Stream,
     * java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazySortedSetX<R> zip(Stream<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazySortedSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.stream.Stream)
     */
    @Override
    public <U> LazySortedSetX<Tuple2<T, U>> zip(Stream<? extends U> other) {

        return (LazySortedSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.function.BiFunction,
     * org.reactivestreams.Publisher)
     */
    @Override
    public <T2, R> LazySortedSetX<R> zip(BiFunction<? super T, ? super T2, ? extends R> fn,
            Publisher<? extends T2> publisher) {

        return (LazySortedSetX<R>) super.zip(fn, publisher);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * fromStream(java.util.stream.Stream)
     */
    @Override
    public <X> LazySortedSetX<X> fromStream(Stream<X> stream) {
        SortedSet<X> list = (SortedSet<X>) stream.collect((Collector) getCollector());
        return new LazySortedSetX<X>(
                                     list, (Collector) getCollector());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.SortedSetX#
     * onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    public LazySortedSetX<T> onEmptySwitch(Supplier<? extends SortedSet<T>> supplier) {
        return stream(Fluxes.onEmptySwitch(flux(), () -> Flux.fromIterable(supplier.get())));
    }

    /**
     * @return
     * @see java.util.SortedSet#comparator()
     */
    public Comparator<? super T> comparator() {
        return getSortedSet().comparator();
    }

    /**
     * @param fromElement
     * @param toElement
     * @return
     * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
     */
    public SortedSetX<T> subSet(T fromElement, T toElement) {
        return new LazySortedSetX<>(
                                    getSortedSet().subSet(fromElement, toElement), this.collector);
    }

    /**
     * @param toElement
     * @return
     * @see java.util.SortedSet#headSet(java.lang.Object)
     */
    public SortedSetX<T> headSet(T toElement) {
        return new LazySortedSetX<>(
                                    getSortedSet().headSet(toElement), this.collector);
    }

    /**
     * @param fromElement
     * @return
     * @see java.util.SortedSet#tailSet(java.lang.Object)
     */
    public SortedSet<T> tailSet(T fromElement) {
        return new LazySortedSetX<>(
                                    getSortedSet().tailSet(fromElement), this.collector);
    }

    /**
     * @return
     * @see java.util.SortedSet#first()
     */
    public T first() {
        return getSortedSet().first();
    }

    /**
     * @return
     * @see java.util.SortedSet#last()
     */
    public T last() {
        return getSortedSet().last();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#permutations()
     */
    @Override
    public LazySortedSetX<ReactiveSeq<T>> permutations() {
        return stream(Flux.from(Streamable.fromPublisher(flux())
                                          .permutations()
                                          .map(s -> s.stream()))
                          .map(Comparables::comparable));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#combinations(int)
     */
    @Override
    public SortedSetX<ReactiveSeq<T>> combinations(int size) {
        return stream(Flux.from(Streamable.fromPublisher(flux())
                                          .combinations(size)
                                          .map(s -> s.stream()))
                          .map(Comparables::comparable));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#combinations()
     */
    @Override
    public SortedSetX<ReactiveSeq<T>> combinations() {
        return stream(Flux.from(Streamable.fromPublisher(flux())
                                          .combinations()
                                          .map(s -> s.stream()))
                          .map(Comparables::comparable));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#grouped(java.util.function.Function)
     */
    @Override
    public <K> LazySortedSetX<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier) {
        Flux<Tuple2<K, Seq<T>>> flux = Fluxes.grouped(flux(), classifier);
        Flux f = flux.map(t -> t.map2(Comparables::comparable));
        return (LazySortedSetX) stream(f);
    }

    static class Comparables {

        static <T, R extends ReactiveSeq<T> & Comparable<T>> R comparable(Seq<T> seq) {
            return comparable(ReactiveSeq.fromStream(seq));
        }

        @SuppressWarnings("unchecked")

        static <T, R extends ReactiveSeq<T> & Comparable<T>> R comparable(ReactiveSeq<T> seq) {
            Method compareTo = Stream.of(Comparable.class.getMethods())
                                     .filter(m -> m.getName()
                                                   .equals("compareTo"))
                                     .findFirst()
                                     .get();

            return (R) Proxy.newProxyInstance(SortedSetX.class.getClassLoader(),
                                              new Class[] { ReactiveSeq.class, Comparable.class },
                                              (proxy, method, args) -> {
                                                  if (compareTo.equals(method))
                                                      return Objects.compare(System.identityHashCode(seq),
                                                                             System.identityHashCode(args[0]),
                                                                             Comparator.naturalOrder());
                                                  else
                                                      return method.invoke(seq, args);
                                              });

        }
    }
    
    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX#materialize()
     */
    @Override
    public LazySortedSetX<T> materialize() {
       this.lazy.get();
       return this;
    }

}
