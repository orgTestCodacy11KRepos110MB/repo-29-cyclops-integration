package com.aol.cyclops.reactor.collections.extensions.persistent;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
import org.pcollections.PCollection;
import org.pcollections.PQueue;
import org.pcollections.PSet;
import org.pcollections.PStack;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.Reducer;
import com.aol.cyclops.Reducers;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.data.collections.extensions.persistent.PSetX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.reactor.Fluxes;
import com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX;
import com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollection;
import com.aol.cyclops.reactor.collections.extensions.base.NativePlusLoop;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import reactor.core.publisher.Flux;

/**
 * An extended Set type {@see java.util.Set}
 * This makes use of PSet (@see org.pcollections.PSet) from PCollectons.
 * 
 * Extended Set operations execute lazily e.g.
 * <pre>
 * {@code 
 *    LazyPSetX<Integer> q = LazyPSetX.of(1,2,3)
 *                                    .map(i->i*2);
 * }
 * </pre>
 * The map operation above is not executed immediately. It will only be executed when (if) the data inside the
 * PSet is accessed. This allows lazy operations to be chained and executed more efficiently e.g.
 * 
 * <pre>
 * {@code 
 *    LazyPSetX<Integer> q = LazyPSetX.of(1,2,3)
 *                                    .map(i->i*2);
 *                                    .filter(i->i<5);
 * }
 * </pre>
 * 
 * The operation above is more efficient than the equivalent operation with a PSetX.

 * NB. Because LazyPSetX transform operations are performed Lazily, they may result in a different result 
 * than PSetX operations that are performed eagerly. For example a sequence of map operations that result in
 * duplicate keys, may result in a different Set being produced.
 * 
 * @author johnmcclean
 *
 * @param <T> the type of elements held in this collection
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LazyPSetX<T> extends AbstractFluentCollectionX<T>implements PSetX<T> {
    private final LazyFluentCollection<T, PSet<T>> lazy;
    @Getter
    private final Reducer<PSet<T>> collector;
    public static <T> LazyPSetX<T> fromPSet(PSet<T> list,Reducer<PSet<T>> collector){
        return new LazyPSetX<T>(list,collector);
    }
    
    @Override
    public LazyPSetX<T> plusLoop(int max, IntFunction<T> value){
        PSet<T> list = lazy.get();
        if(list instanceof NativePlusLoop){
            return (LazyPSetX<T>) ((NativePlusLoop)list).plusLoop(max, value);
        }else{
            return (LazyPSetX<T>) super.plusLoop(max, value);
        }
    }
    @Override
    public LazyPSetX<T> plusLoop(Supplier<Optional<T>> supplier){
        PSet<T> list = lazy.get();
        if(list instanceof NativePlusLoop){
            return (LazyPSetX<T>) ((NativePlusLoop)list).plusLoop(supplier);
        }else{
            return (LazyPSetX<T>) super.plusLoop(supplier);
        }
    }
    /**
     * Create a LazyPStackX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPStackX
     */
    public static <T> LazyPSetX<T> fromStreamS(Stream<T> stream) {
        return new LazyPSetX<T>(
                                Flux.from(ReactiveSeq.fromStream(stream)));
    }

    /**
     * Create a LazyPStackX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPSetX<Integer> range(int start, int end) {
        return fromStreamS(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyPStackX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPSetX<Long> rangeLong(long start, long end) {
        return fromStreamS(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyPStackX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyPSetX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStreamS(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPStackX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyPSetX<T> generate(long limit, Supplier<T> s) {

        return fromStreamS(ReactiveSeq.generate(s)
                                      .limit(limit));
    }

    /**
     * Create a LazyPStackX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyPSetX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStreamS(ReactiveSeq.iterate(seed, f)
                                      .limit(limit));
    }

    /**
     * @return A collector that generates a LazyPStackX
     */
    public static <T> Collector<T, ?, LazyPSetX<T>> lazyListXCollector() {
        return Collectors.toCollection(() -> LazyPSetX.of());
    }

    /**
     * @return An empty LazyPStackX
     */
    public static <T> LazyPSetX<T> empty() {
        return fromIterable((List<T>) ListX.<T> defaultCollector()
                                           .supplier()
                                           .get());
    }

    /**
     * Create a LazyPStackX from the specified values
     * <pre>
     * {@code 
     *     ListX<Integer> lazy = LazyPStackX.of(1,2,3,4,5);
     *     
     *     //lazily map List
     *     ListX<String> mapped = lazy.map(i->"mapped " +i); 
     *     
     *     String value = mapped.get(0); //transformation triggered now
     * }
     * </pre>
     * 
     * @param values To populate LazyPStackX with
     * @return LazyPStackX
     */
    @SafeVarargs
    public static <T> LazyPSetX<T> of(T... values) {
        List<T> res = (List<T>) ListX.<T> defaultCollector()
                                     .supplier()
                                     .get();
        for (T v : values)
            res.add(v);
        return fromIterable(res);
    }

    /**
     * Construct a LazyPStackX with a single value
     * <pre>
     * {@code 
     *    ListX<Integer> lazy = LazyPStackX.singleton(5);
     *    
     * }
     * </pre>
     * 
     * 
     * @param value To populate LazyPStackX with
     * @return LazyPStackX with a single value
     */
    public static <T> LazyPSetX<T> singleton(T value) {
        return LazyPSetX.<T> of(value);
    }

    /**
     * Construct a LazyPStackX from an Publisher
     * 
     * @param publisher
     *            to construct LazyPStackX from
     * @return ListX
     */
    public static <T> LazyPSetX<T> fromPublisher(Publisher<? extends T> publisher) {
        return fromStreamS(ReactiveSeq.fromPublisher((Publisher<T>) publisher));
    }

    /**
     * Construct LazyPStackX from an Iterable
     * 
     * @param it to construct LazyPStackX from
     * @return LazyPStackX from Iterable
     */
    public static <T> LazyPSetX<T> fromIterable(Iterable<T> it) {
        return fromIterable(Reducers.toPSet(), it);
    }

    /**
     * Construct a LazyPStackX from an Iterable, using the specified Collector.
     * 
     * @param collector To generate Lists from, this can be used to create mutable vs immutable Lists (for example), or control List type (ArrayList, LinkedList)
     * @param it Iterable to construct LazyPStackX from
     * @return Newly constructed LazyPStackX
     */
    public static <T> LazyPSetX<T> fromIterable(Reducer<PSet<T>> collector, Iterable<T> it) {
        if (it instanceof LazyPSetX)
            return (LazyPSetX<T>) it;

        if (it instanceof PSet)
            return new LazyPSetX<T>(
                                    (PSet<T>) it, collector);

        return new LazyPSetX<T>(
                                Flux.fromIterable(it), collector);
    }

    private LazyPSetX(PSet<T> list, Reducer<PSet<T>> collector) {
        this.lazy = new PersistentLazyCollection<T, PSet<T>>(
                                                             list, null, collector);
        this.collector = collector;
    }

    private LazyPSetX(boolean efficientOps, PSet<T> list, Reducer<PSet<T>> collector) {
        this.lazy = new PersistentLazyCollection<T, PSet<T>>(
                                                             list, null, collector);
        this.collector = collector;
    }

    private LazyPSetX(PSet<T> list) {
        this.collector = Reducers.toPSet();
        this.lazy = new PersistentLazyCollection<T, PSet<T>>(
                                                             list, null, Reducers.toPSet());
    }

    public LazyPSetX(Flux<T> stream, Reducer<PSet<T>> collector) {
        this.collector = collector;
        this.lazy = new PersistentLazyCollection<>(
                                                   null, stream, Reducers.toPSet());
    }

    private LazyPSetX(Flux<T> stream) {
        this.collector = Reducers.toPSet();
        this.lazy = new PersistentLazyCollection<>(
                                                   null, stream, collector);
    }

    private LazyPSetX() {
        this.collector = Reducers.toPSet();
        this.lazy = new PersistentLazyCollection<>(
                                                   (PSet) this.collector.zero(), null, collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        getSet().forEach(action);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return getSet().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        return getSet().size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object e) {
        return getSet().contains(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return getSet().equals(o);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return getSet().isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getSet().hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        return getSet().toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return getSet().removeAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return getSet().toArray(a);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(T e) {
        return getSet().add(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        return getSet().remove(o);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return getSet().containsAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        return getSet().addAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return getSet().retainAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#clear()
     */
    @Override
    public void clear() {
        getSet().clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSet().toString();
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
        return getSet().removeIf(filter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public Stream<T> parallelStream() {
        return getSet().parallelStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#spliterator()
     */
    @Override
    public Spliterator<T> spliterator() {
        return getSet().spliterator();
    }

    /**
     * @return PQueue
     */
    private PSet<T> getSet() {
        return lazy.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#stream(reactor.core.publisher.Flux)
     */
    @Override
    public <X> LazyPSetX<X> stream(Flux<X> stream) {
        return new LazyPSetX<X>(
                                stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#flux()
     */
    @Override
    public Flux<T> flux() {
        return lazy.flux();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#combine(java.util.function.BiPredicate,
     * java.util.function.BinaryOperator)
     */
    @Override
    public LazyPSetX<T> combine(BiPredicate<? super T, ? super T> predicate, BinaryOperator<T> op) {

        return (LazyPSetX<T>) super.combine(predicate, op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#reverse()
     */
    @Override
    public LazyPSetX<T> reverse() {

        return (LazyPSetX<T>) super.reverse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#filter(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> filter(Predicate<? super T> pred) {

        return (LazyPSetX<T>) super.filter(pred);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#map(java.util.function.Function)
     */
    @Override
    public <R> LazyPSetX<R> map(Function<? super T, ? extends R> mapper) {

        return (LazyPSetX<R>) super.map(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#flatMap(java.util.function.Function)
     */
    @Override
    public <R> LazyPSetX<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return (LazyPSetX<R>) super.flatMap(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#limit(long)
     */
    @Override
    public LazyPSetX<T> limit(long num) {
        return (LazyPSetX<T>) super.limit(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#skip(long)
     */
    @Override
    public LazyPSetX<T> skip(long num) {
        return (LazyPSetX<T>) super.skip(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#takeRight(int)
     */
    @Override
    public LazyPSetX<T> takeRight(int num) {
        return (LazyPSetX<T>) super.takeRight(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#dropRight(int)
     */
    @Override
    public LazyPSetX<T> dropRight(int num) {
        return (LazyPSetX<T>) super.dropRight(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#takeWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> takeWhile(Predicate<? super T> p) {
        return (LazyPSetX<T>) super.takeWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#dropWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> dropWhile(Predicate<? super T> p) {
        return (LazyPSetX<T>) super.dropWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#takeUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> takeUntil(Predicate<? super T> p) {
        return (LazyPSetX<T>) super.takeUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#dropUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> dropUntil(Predicate<? super T> p) {
        return (LazyPSetX<T>) super.dropUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#trampoline(java.util.function.Function)
     */
    @Override
    public <R> LazyPSetX<R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (LazyPSetX<R>) super.trampoline(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#slice(long, long)
     */
    @Override
    public LazyPSetX<T> slice(long from, long to) {
        return (LazyPSetX<T>) super.slice(from, to);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#grouped(int)
     */
    @Override
    public LazyPSetX<ListX<T>> grouped(int groupSize) {

        return (LazyPSetX<ListX<T>>) super.grouped(groupSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#grouped(java.util.function.Function,
     * java.util.stream.Collector)
     */
    @Override
    public <K, A, D> LazyPSetX<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {

        return (LazyPSetX) super.grouped(classifier, downstream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#grouped(java.util.function.Function)
     */
    @Override
    public <K> LazyPSetX<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier) {

        return (LazyPSetX) super.grouped(classifier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.lang.Iterable)
     */
    @Override
    public <U> LazyPSetX<Tuple2<T, U>> zip(Iterable<? extends U> other) {

        return (LazyPSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.lang.Iterable,
     * java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyPSetX<R> zip(Iterable<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazyPSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#sliding(int)
     */
    @Override
    public LazyPSetX<ListX<T>> sliding(int windowSize) {

        return (LazyPSetX<ListX<T>>) super.sliding(windowSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#sliding(int, int)
     */
    @Override
    public LazyPSetX<ListX<T>> sliding(int windowSize, int increment) {

        return (LazyPSetX<ListX<T>>) super.sliding(windowSize, increment);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#scanLeft(com.aol.cyclops.Monoid)
     */
    @Override
    public LazyPSetX<T> scanLeft(Monoid<T> monoid) {

        return (LazyPSetX<T>) super.scanLeft(monoid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#scanLeft(java.lang.Object,
     * java.util.function.BiFunction)
     */
    @Override
    public <U> LazyPSetX<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {

        return (LazyPSetX<U>) super.scanLeft(seed, function);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#scanRight(com.aol.cyclops.Monoid)
     */
    @Override
    public LazyPSetX<T> scanRight(Monoid<T> monoid) {

        return (LazyPSetX<T>) super.scanRight(monoid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#scanRight(java.lang.Object,
     * java.util.function.BiFunction)
     */
    @Override
    public <U> LazyPSetX<U> scanRight(U identity, BiFunction<? super T, ? super U, ? extends U> combiner) {

        return (LazyPSetX<U>) super.scanRight(identity, combiner);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#sorted(java.util.function.Function)
     */
    @Override
    public <U extends Comparable<? super U>> LazyPSetX<T> sorted(Function<? super T, ? extends U> function) {

        return (LazyPSetX<T>) super.sorted(function);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#plusLazy(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> plusLazy(T e) {

        return (LazyPSetX<T>) super.plusLazy(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#plusAllLazy(java.util.Collection)
     */
    @Override
    public LazyPSetX<T> plusAllLazy(Collection<? extends T> list) {

        return (LazyPSetX<T>) super.plusAllLazy(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#minusLazy(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> minusLazy(Object e) {

        return (LazyPSetX<T>) super.minusLazy(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#minusAllLazy(java.util.Collection)
     */
    @Override
    public LazyPSetX<T> minusAllLazy(Collection<?> list) {

        return (LazyPSetX<T>) super.minusAllLazy(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#cycle(int)
     */
    @Override
    public LazyPStackX<T> cycle(int times) {
        return LazyPStackX.fromPublisher(Flux.from(this.stream()
                                                       .cycle(times)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#cycle(com.aol.cyclops.Monoid, int)
     */
    @Override
    public LazyPStackX<T> cycle(Monoid<T> m, int times) {
        return LazyPStackX.fromPublisher(Flux.from(this.stream()
                                                       .cycle(m, times)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#cycleWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPStackX<T> cycleWhile(Predicate<? super T> predicate) {
        return LazyPStackX.fromPublisher(Flux.from(this.stream()
                                                       .cycleWhile(predicate)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#cycleUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPStackX<T> cycleUntil(Predicate<? super T> predicate) {
        return LazyPStackX.fromPublisher(Flux.from(this.stream()
                                                       .cycleUntil(predicate)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(org.jooq.lambda.Seq)
     */
    @Override
    public <U> LazyPSetX<Tuple2<T, U>> zip(Seq<? extends U> other) {

        return (LazyPSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip3(java.util.stream.Stream,
     * java.util.stream.Stream)
     */
    @Override
    public <S, U> LazyPSetX<Tuple3<T, S, U>> zip3(Stream<? extends S> second, Stream<? extends U> third) {

        return (LazyPSetX) super.zip3(second, third);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip4(java.util.stream.Stream,
     * java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    public <T2, T3, T4> LazyPSetX<Tuple4<T, T2, T3, T4>> zip4(Stream<? extends T2> second, Stream<? extends T3> third,
            Stream<? extends T4> fourth) {

        return (LazyPSetX) super.zip4(second, third, fourth);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zipWithIndex()
     */
    @Override
    public LazyPSetX<Tuple2<T, Long>> zipWithIndex() {

        return (LazyPSetX<Tuple2<T, Long>>) super.zipWithIndex();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#distinct()
     */
    @Override
    public LazyPSetX<T> distinct() {

        return (LazyPSetX<T>) super.distinct();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#sorted()
     */
    @Override
    public LazyPSetX<T> sorted() {

        return (LazyPSetX<T>) super.sorted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#sorted(java.util.Comparator)
     */
    @Override
    public LazyPSetX<T> sorted(Comparator<? super T> c) {

        return (LazyPSetX<T>) super.sorted(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#skipWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> skipWhile(Predicate<? super T> p) {

        return (LazyPSetX<T>) super.skipWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#skipUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> skipUntil(Predicate<? super T> p) {

        return (LazyPSetX<T>) super.skipUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#limitWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> limitWhile(Predicate<? super T> p) {

        return (LazyPSetX<T>) super.limitWhile(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#limitUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> limitUntil(Predicate<? super T> p) {

        return (LazyPSetX<T>) super.limitUntil(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#intersperse(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> intersperse(T value) {

        return (LazyPSetX<T>) super.intersperse(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#shuffle()
     */
    @Override
    public LazyPSetX<T> shuffle() {

        return (LazyPSetX<T>) super.shuffle();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#skipLast(int)
     */
    @Override
    public LazyPSetX<T> skipLast(int num) {

        return (LazyPSetX<T>) super.skipLast(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#limitLast(int)
     */
    @Override
    public LazyPSetX<T> limitLast(int num) {

        return (LazyPSetX<T>) super.limitLast(num);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#onEmpty(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> onEmpty(T value) {

        return (LazyPSetX<T>) super.onEmpty(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    public LazyPSetX<T> onEmptyGet(Supplier<? extends T> supplier) {

        return (LazyPSetX<T>) super.onEmptyGet(supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    public <X extends Throwable> LazyPSetX<T> onEmptyThrow(Supplier<? extends X> supplier) {

        return (LazyPSetX<T>) super.onEmptyThrow(supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#shuffle(java.util.Random)
     */
    @Override
    public LazyPSetX<T> shuffle(Random random) {

        return (LazyPSetX<T>) super.shuffle(random);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#ofType(java.lang.Class)
     */
    @Override
    public <U> LazyPSetX<U> ofType(Class<? extends U> type) {

        return (LazyPSetX<U>) super.ofType(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#filterNot(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<T> filterNot(Predicate<? super T> fn) {

        return (LazyPSetX<T>) super.filterNot(fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#notNull()
     */
    @Override
    public LazyPSetX<T> notNull() {

        return (LazyPSetX<T>) super.notNull();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#removeAll(java.util.stream.Stream)
     */
    @Override
    public LazyPSetX<T> removeAll(Stream<? extends T> stream) {

        return (LazyPSetX<T>) (super.removeAll(stream));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#removeAll(org.jooq.lambda.Seq)
     */
    @Override
    public LazyPSetX<T> removeAll(Seq<? extends T> stream) {

        return (LazyPSetX<T>) super.removeAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#removeAll(java.lang.Iterable)
     */
    @Override
    public LazyPSetX<T> removeAll(Iterable<? extends T> it) {

        return (LazyPSetX<T>) super.removeAll(it);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#removeAll(java.lang.Object[])
     */
    @Override
    public LazyPSetX<T> removeAll(T... values) {

        return (LazyPSetX<T>) super.removeAll(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#retainAll(java.lang.Iterable)
     */
    @Override
    public LazyPSetX<T> retainAll(Iterable<? extends T> it) {

        return (LazyPSetX<T>) super.retainAll(it);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#retainAll(java.util.stream.Stream)
     */
    @Override
    public LazyPSetX<T> retainAll(Stream<? extends T> stream) {

        return (LazyPSetX<T>) super.retainAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#retainAll(org.jooq.lambda.Seq)
     */
    @Override
    public LazyPSetX<T> retainAll(Seq<? extends T> stream) {

        return (LazyPSetX<T>) super.retainAll(stream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#retainAll(java.lang.Object[])
     */
    @Override
    public LazyPSetX<T> retainAll(T... values) {

        return (LazyPSetX<T>) super.retainAll(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#cast(java.lang.Class)
     */
    @Override
    public <U> LazyPSetX<U> cast(Class<? extends U> type) {

        return (LazyPSetX<U>) super.cast(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#patternMatch(java.util.function.Function,
     * java.util.function.Supplier)
     */
    @Override
    public <R> LazyPSetX<R> patternMatch(Function<CheckValue1<T, R>, CheckValue1<T, R>> case1,
            Supplier<? extends R> otherwise) {

        return (LazyPSetX<R>) super.patternMatch(case1, otherwise);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#permutations()
     */
    @Override
    public LazyPSetX<ReactiveSeq<T>> permutations() {

        return (LazyPSetX<ReactiveSeq<T>>) super.permutations();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#combinations(int)
     */
    @Override
    public LazyPSetX<ReactiveSeq<T>> combinations(int size) {

        return (LazyPSetX<ReactiveSeq<T>>) super.combinations(size);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#combinations()
     */
    @Override
    public LazyPSetX<ReactiveSeq<T>> combinations() {

        return (LazyPSetX<ReactiveSeq<T>>) super.combinations();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#grouped(int, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyPSetX<C> grouped(int size, Supplier<C> supplier) {

        return (LazyPSetX<C>) super.grouped(size, supplier);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedUntil(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<ListX<T>> groupedUntil(Predicate<? super T> predicate) {

        return (LazyPSetX<ListX<T>>) super.groupedUntil(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedWhile(java.util.function.Predicate)
     */
    @Override
    public LazyPSetX<ListX<T>> groupedWhile(Predicate<? super T> predicate) {

        return (LazyPSetX<ListX<T>>) super.groupedWhile(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedWhile(java.util.function.Predicate,
     * java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyPSetX<C> groupedWhile(Predicate<? super T> predicate,
            Supplier<C> factory) {

        return (LazyPSetX<C>) super.groupedWhile(predicate, factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedUntil(java.util.function.Predicate,
     * java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyPSetX<C> groupedUntil(Predicate<? super T> predicate,
            Supplier<C> factory) {

        return (LazyPSetX<C>) super.groupedUntil(predicate, factory);
    }

    /** PStackX methods **/

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#with(int,
     * java.lang.Object)
     */
    public LazyPSetX<T> with(int i, T element) {
        return stream(Fluxes.insertAt(Fluxes.deleteBetween(flux(), i, i + 1), i, element));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#groupedStatefullyUntil(java.util.function.
     * BiPredicate)
     */
    @Override
    public LazyPSetX<ListX<T>> groupedStatefullyUntil(BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (LazyPSetX<ListX<T>>) super.groupedStatefullyUntil(predicate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#peek(java.util.function.Consumer)
     */
    @Override
    public LazyPSetX<T> peek(Consumer<? super T> c) {

        return (LazyPSetX) super.peek(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(org.jooq.lambda.Seq,
     * java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyPSetX<R> zip(Seq<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazyPSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.stream.Stream,
     * java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyPSetX<R> zip(Stream<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (LazyPSetX<R>) super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.stream.Stream)
     */
    @Override
    public <U> LazyPSetX<Tuple2<T, U>> zip(Stream<? extends U> other) {

        return (LazyPSetX) super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#zip(java.util.function.BiFunction,
     * org.reactivestreams.Publisher)
     */
    @Override
    public <T2, R> LazyPSetX<R> zip(BiFunction<? super T, ? super T2, ? extends R> fn,
            Publisher<? extends T2> publisher) {

        return (LazyPSetX<R>) super.zip(fn, publisher);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.standard.ListX#onEmptySwitch(
     * java.util.function.Supplier)
     */
    @Override
    public LazyPSetX<T> onEmptySwitch(Supplier<? extends PSet<T>> supplier) {
        return stream(Fluxes.onEmptySwitch(flux(), () -> Flux.fromIterable(supplier.get())));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#unit(
     * Collection)
     */
    @Override
    public <R> LazyPSetX<R> unit(Collection<R> col) {

        return fromIterable(col);
    }

    @Override
    public <R> LazyPSetX<R> unit(R value) {
        return singleton(value);
    }

    @Override
    public <R> LazyPSetX<R> unitIterator(Iterator<R> it) {
        return fromIterable(() -> it);
    }

    @Override
    public <R> LazyPSetX<R> emptyUnit() {

        return LazyPSetX.<R> empty();
    }

    /**
     * @return This converted to PVector
     */
    public LazyPSetX<T> toPVector() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX
     * #plusInOrder(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> plusInOrder(T e) {
        return plus(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.data.collections.extensions.CollectionX#stream()
     */
    @Override
    public ReactiveSeq<T> stream() {

        return ReactiveSeq.fromIterable(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#from(java.util.Collection)
     */
    @Override
    public <X> LazyPSetX<X> from(Collection<X> col) {
        return fromIterable(col);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.data.collections.extensions.persistent.PQueueX#monoid()
     */
    @Override
    public <T> Reducer<PSet<T>> monoid() {

        return Reducers.toPSet();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pcollections.MapPSet#plus(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> plus(T e) {
        return new LazyPSetX<T>(
                                getSet().plus(e), this.collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pcollections.MapPSet#plusAll(java.util.Collection)
     */
    @Override
    public LazyPSetX<T> plusAll(Collection<? extends T> list) {
        return new LazyPSetX<T>(
                                getSet().plusAll(list), this.collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#minus(java.lang.Object)
     */
    @Override
    public LazyPSetX<T> minus(Object e) {
        PCollection<T> res = getSet().minus(e);
        return LazyPSetX.fromIterable(this.collector, res);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.reactor.collections.extensions.base.
     * AbstractFluentCollectionX#minusAll(java.util.Collection)
     */
    public LazyPSetX<T> minusAll(Collection<?> list) {
        PCollection<T> res = getSet().minusAll(list);
        return LazyPSetX.fromIterable(this.collector, res);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX#materialize()
     */
    @Override
    public LazyPSetX<T> materialize() {
       this.lazy.get();
       return this;
    }
}
