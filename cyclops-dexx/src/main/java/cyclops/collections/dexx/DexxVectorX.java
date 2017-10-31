package cyclops.collections.dexx;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.oath.cyclops.data.collections.extensions.CollectionX;
import com.oath.cyclops.data.collections.extensions.lazy.immutable.LazyPersistentListX;
import com.oath.cyclops.types.Unwrapable;
import com.oath.cyclops.types.foldable.Evaluation;
import cyclops.collections.immutable.VectorX;
import cyclops.function.Reducer;
import cyclops.reactive.ReactiveSeq;
import cyclops.data.tuple.Tuple2;
import org.pcollections.PersistentList;


import com.github.andrewoma.dexx.collection.Builder;
import com.github.andrewoma.dexx.collection.Vector;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DexxVectorX<T> extends AbstractList<T> implements PersistentList<T>, Unwrapable {

    public static <T> VectorX<T> vectorX(ReactiveSeq<T> stream){
        return fromStream(stream);
    }
    @Override
    public <R> R unwrap() {
        return (R)vector;
    }
    public static <T> VectorX<T> copyFromCollection(CollectionX<T> vec) {

        return DexxVectorX.<T>empty()
                .plusAll(vec);

    }
    /**
     * Create a LazyPersistentListX from a Stream
     *
     * @param stream to construct a LazyQueueX from
     * @return LazyPersistentListX
     */
    public static <T> LazyPersistentListX<T> fromStream(Stream<T> stream) {
        Reducer<PersistentList<T>> r = toPersistentList();
        return new LazyPersistentListX<T>(null, ReactiveSeq.fromStream(stream),r, Evaluation.LAZY);
    }

    /**
     * Create a LazyPersistentListX that contains the Integers between start and end
     *
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPersistentListX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyPersistentListX that contains the Longs between start and end
     *
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPersistentListX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     *
     * <pre>
     * {@code
     *  LazyPersistentListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     *
     * //(1,2,3,4,5)
     *
     * }</pre>
     *
     * @param seed Initial value
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyPersistentListX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPersistentListX from the provided Supplier up to the provided limit number of times
     *
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyPersistentListX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                      .limit(limit));
    }

    /**
     * Create a LazyPersistentListX by iterative application of a function to an initial element up to the supplied limit number of times
     *
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyPersistentListX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                      .limit(limit));
    }

    /**
     * <pre>
     * {@code
     * PersistentList<Integer> q = JSPersistentList.<Integer>toPersistentList()
                                     .mapReduce(Stream.of(1,2,3,4));
     *
     * }
     * </pre>
     * @return Reducer for PersistentList
     */
    public static <T> Reducer<PersistentList<T>> toPersistentList() {
        return Reducer.<PersistentList<T>> of(DexxVectorX.emptyPersistentList(), (final PersistentList<T> a) -> b -> a.plusAll(b), (final T x) -> DexxVectorX.singleton(x));
    }
    public static <T> Builder<T,Vector<T>> builder(){
        return Vector.<T>factory().newBuilder();
    }
    public static <T> DexxVectorX<T> fromVector(Vector<T> vector){
        return new DexxVectorX<>(vector);
    }

    public static <T> DexxVectorX<T> emptyPersistentList(){
        return new DexxVectorX<>(Vector.empty());
    }

    public static <T> LazyPersistentListX<T> empty(){
        return fromPersistentList(new DexxVectorX<>(Vector.empty()), toPersistentList());
    }
    public static <T> PersistentList<T> singleton(T t){
        Builder<T, Vector<T>> builder = Vector.<T>factory().newBuilder();
        return fromPersistentList(new DexxVectorX<>(builder.add(t).build()), toPersistentList());
    }
    public static <T> LazyPersistentListX<T> of(T... t){
        Builder<T, Vector<T>> builder = Vector.<T>factory().newBuilder();
        for(T next : t){
            builder.add(next);
        }
        return fromPersistentList(new DexxVectorX<>(builder.build()), toPersistentList());
    }
    public static <T> LazyPersistentListX<T> PersistentList(Vector<T> q) {
        return fromPersistentList(new DexxVectorX<T>(q), toPersistentList());
    }
    private static <T> LazyPersistentListX<T> fromPersistentList(PersistentList<T> vec, Reducer<PersistentList<T>> pVectorReducer) {
        return new LazyPersistentListX<T>(vec,null, pVectorReducer,Evaluation.LAZY);
    }
    @SafeVarargs
    public static <T> LazyPersistentListX<T> PersistentList(T... elements){
        return fromPersistentList(of(elements),toPersistentList());
    }
    @Wither
    private final Vector<T> vector;

    public DexxVectorX<T> tail(){
        return withVector(vector.tail());
    }
    public T head(){
        return vector.get(0);
    }

    @Override
    public DexxVectorX<T> plus(T e) {
        return withVector(vector.append(e));
    }

    @Override
    public DexxVectorX<T> plusAll(Collection<? extends T> list) {
        Vector<T> vec = vector;
        for(T next :  list){
            vec = vec.append(next);
        }
        return withVector(vec);
     }

    private PersistentList<T> plusAllVec(Vector<? extends T> list) {
        Vector<T> vec = vector;
        for(T next :  list){
            vec = vec.append(next);
        }
        return withVector(vec);
     }

    @Override
    public DexxVectorX<T> with(int i, T e) {
        return withVector(vector.set(i,e));
    }

    @Override
    public PersistentList<T> plus(int i, T e) {
        if(i<0 || i>size())
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds - size : " + size());
        if(i==0)
            return withVector(vector.prepend(e));
        if(i==size()-1)
            return withVector(vector.append(e));

        return withVector(vector.take(i)).plus(e).plusAllVec(vector.drop(i));

    }

    @Override
    public PersistentList<T> plusAll(int i, Collection<? extends T> list) {
        return withVector(vector.take(i)).plusAll(list).plusAllVec(vector.drop(i));
    }

    @Override
    public PersistentList<T> minus(Object e) {
        return fromPersistentList(this,toPersistentList()).filter(i->!Objects.equals(i,e));
    }

    @Override
    public PersistentList<T> minusAll(Collection<?> list) {
        return (PersistentList<T>)fromPersistentList(this,toPersistentList()).removeAllI((Iterable<T>)list);
    }

    @Override
    public DexxVectorX<T> minus(int i) {
        if(i<0 || i>size())
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds - size : " + size());
        if(i==0)
            return withVector(vector.drop(1));

        return withVector(vector.take(i)).plusAll(withVector(vector.drop(i+1)));
    }

    @Override
    public DexxVectorX<T> subList(int start, int end) {
        return withVector(vector.drop(start).take(end-start));
    }

    @Override
    public T get(int index) {
        return vector.get(index);
    }

    @Override
    public int size() {
        return vector.size();
    }


}
