package com.aol.cyclops.sum.types;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import com.aol.cyclops.control.Eval;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.Maybe;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.control.Xor;
import com.aol.cyclops.types.BiFunctor;
import com.aol.cyclops.types.Combiner;
import com.aol.cyclops.types.Functor;
import com.aol.cyclops.types.To;
import com.aol.cyclops.types.Value;
import com.aol.cyclops.types.applicative.ApplicativeFunctor;
import com.aol.cyclops.types.stream.reactive.ValueSubscriber;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * A right biased Lazy Either4 type. map / flatMap operators are tail-call optimized
 * 
 * 
 * Can be one of 4 types
 * Left1
 * Left2
 * Left3
 * Right
 * 
 * 
 * 
 * @author johnmcclean
 *
 * @param <LT1> First type (Left type)
 * @param <LT2> Second type
 * @param <LT3> Third Type
 * @param <RT> Right type (operations are performed on this type if present)
 */
public interface Either4<LT1, LT2,LT3, RT> extends Functor<RT>, 
                                                            BiFunctor<LT3, RT>, 
                                                            To<Either4<LT1, LT2,LT3, RT>>, 
                                                            Supplier<RT>, 
                                                            ApplicativeFunctor<RT> {

    /**
     * Lazily construct a Right Either from the supplied publisher
     * <pre>
     * {@code 
     *   ReactiveSeq<Integer> stream =  ReactiveSeq.of(1,2,3);
        
         Either4<Throwable,String,String,Integer> either = Either4.fromPublisher(stream);
        
         //Either[1]
     * 
     * }
     * </pre>
     * @param pub Publisher to construct an Either from
     * @return Either constructed from the supplied Publisher
     */
    public static <T1,T2,T> Either4<Throwable, T1, T2, T> fromPublisher(final Publisher<T> pub) {
        final ValueSubscriber<T> sub = ValueSubscriber.subscriber();
        pub.subscribe(sub);
        Either4<Throwable, T1,T2, Xor<Throwable,T>> xor = Either4.rightEval(Eval.later(()->sub.toXor()));
        return  xor.flatMap(x->x.visit(Either4::left1,Either4::right));
    }
    /**
     * Construct a Right Either4 from the supplied Iterable
     * <pre>
     * {@code 
     *   List<Integer> list =  Arrays.asList(1,2,3);
        
         Either4<Throwable,String,Integer> future = Either4.fromIterable(list);
        
         //Either4[1]
     * 
     * }
     * </pre> 
     * @param iterable Iterable to construct an Either from
     * @return Either constructed from the supplied Iterable
     */
    public static <ST, T, T2,RT> Either4<ST, T,T2,RT> fromIterable(final Iterable<RT> iterable) {

        final Iterator<RT> it = iterable.iterator();
        return it.hasNext() ? Either4.right( it.next()) : Either4.left1(null);
    }
    
    /**
     * Construct a Either4#Right from an Eval
     * 
     * @param right Eval to construct Either4#Right from
     * @return Either4 right instance
     */
    public static <LT, M1,B, RT> Either4<LT, M1,B, RT> rightEval(final Eval<RT> right) {
        return new Right<>(
                           right);
    }

    /**
     * Construct a Either4#Left1 from an Eval
     * 
     * @param left Eval to construct Either4#Left1 from
     * @return Either4 Left1 instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> left1Eval(final Eval<LT> left) {
        return new Left1<>(
                          left);
    }

    /**
     * Construct a Either4#Right
     * 
     * @param right Value to store
     * @return Either4 Right instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> right(final RT right) {
        return new Right<>(
                           Eval.later(()->right));
    }

    /**
     * Construct a Either4#Left1
     * 
     * @param left Value to store
     * @return Left1 instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> left1(final LT left) {
        return new Left1<>(
                          Eval.now(left));
    }

    /**
     * Construct a Either4#Second
     * 
     * @param middle Value to store
     * @return Second instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> second(final M1 middle) {
        return new Left2<>(
                            Eval.now(middle));
    }
    /**
     * Construct a Either4#Third
     * 
     * @param middle Value to store
     * @return Third instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> third(final B middle) {
        return new Left3<>(
                            Eval.now(middle));
    }

    /**
     * Construct a Either4#Second from an Eval
     * 
     * @param second Eval to construct Either4#middle from
     * @return Either4 second instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> secondEval(final Eval<M1> middle) {
        return new Left2<>(
                            middle);
    }
    /**
     * Construct a Either4#Third from an Eval
     * 
     * @param third Eval to construct Either4#middle from
     * @return Either4 third instance
     */
    public static <LT, M1, B, RT> Either4<LT, M1, B, RT> thirdEval(final Eval<B> middle) {
        return new Left3<>(
                            middle);
    }
    default < RT1> Either4<LT1, LT2, LT3, RT1>  flatMapIterable(Function<? super RT, ? extends Iterable<? extends RT1>> mapper){
        return this.flatMap(a -> {
            return Either4.fromIterable(mapper.apply(a));

        });
    }
    default < RT1> Either4<LT1, LT2, LT3,RT1>  flatMapPublisher(Function<? super RT, ? extends Publisher<? extends RT1>> mapper){
        return this.flatMap(a -> {
            final Publisher<? extends RT1> publisher = mapper.apply(a);
            final ValueSubscriber<RT1> sub = ValueSubscriber.subscriber();

            publisher.subscribe(sub);
            return unit(sub.get());

        });
    }

    /**
     * Visit the types in this Either4, only one user supplied function is executed depending on the type
     * 
     * @param left1 Function to execute if this Either4 is a Left1 instance
     * @param left2 Function to execute if this Either4 is a Left2 instance
     * @param left3 Function to execute if this Either4 is a Left3 instance
     * @param right Function to execute if this Either4 is a right instance
     * @return Result of executed function
     */
    <R> R visit(final Function<? super LT1, ? extends R> left1, final Function<? super LT2, ? extends R> left2
            ,final Function<? super LT3, ? extends R> left3,
            final Function<? super RT, ? extends R> right);

    /**
     * Filter this Either4 resulting in a Maybe#none if it is not a Right instance or if the predicate does not
     * hold. Otherwise results in a Maybe containing the current value
     * 
     * @param test Predicate to apply to filter this Either4
     * @return Maybe containing the current value if this is a Right instance and the predicate holds, otherwise Maybe#none
     */
    Maybe<RT> filter(Predicate<? super RT> test);

    /**
     * Flattening transformation on this Either4. Contains an internal trampoline so will convert tail-recursive calls
     * to iteration.
     * 
     * @param mapper Mapping function
     * @return Mapped Either4
     */
    < RT1> Either4<LT1, LT2,LT3, RT1> flatMap(
            Function<? super RT, ? extends Either4<? extends LT1, ? extends LT2, ? extends LT3, ? extends RT1>> mapper);
    /**
     * @return Swap the third and the right types
     */
    Either4<LT1,LT2, RT, LT3> swap3();
    /**
     * @return Swap the second and the right types
     */
    Either4<LT1, RT,LT3, LT2> swap2();

    /**
     * @return Swap the right and left types
     */
    Either4<RT, LT2,LT3, LT1> swap1();

    /**
     * @return True if this either contains the right type
     */
    boolean isRight();

    /**
     * @return True if this either contains the left1 type
     */
    boolean isLeft1();

    /**
     * @return True if this either contains the left2 type
     */
    boolean isLeft2();
    /**
     * @return True if this either contains the left3 type
     */
    boolean isLeft3();

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bimap(java.util.function.Function,
     * java.util.function.Function)
     */
    @Override
    <R1, R2> Either4<LT1, LT2, R1, R2> bimap(Function<? super LT3, ? extends R1> fn1, 
                                                    Function<? super RT, ? extends R2> fn2);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#map(java.util.function.Function)
     */
    @Override
    <R> Either4<LT1,LT2,LT3, R> map(Function<? super RT, ? extends R> fn);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Combiner#combine(com.aol.cyclops.types.Value,
     * java.util.function.BiFunction)
     */
    @Override
    default <T2, R> Either4<LT1, LT2, LT3, R> combine(final Value<? extends T2> app,
            final BiFunction<? super RT, ? super T2, ? extends R> fn) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.combine(app, fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Combiner#combine(java.util.function.BinaryOperator,
     * com.aol.cyclops.types.Combiner)
     */
    @Override
    default Either4<LT1, LT2, LT3, RT> combine(final BinaryOperator<Combiner<RT>> combiner, final Combiner<RT> app) {

        return (Either4<LT1, LT2, LT3, RT>) ApplicativeFunctor.super.combine(combiner, app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq,
     * java.util.function.BiFunction)
     */
    @Override
    default <U, R> Either4<LT1, LT2, LT3, R> zip(final Seq<? extends U> other,
            final BiFunction<? super RT, ? super U, ? extends R> zipper) {
        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream,
     * java.util.function.BiFunction)
     */
    @Override
    default <U, R> Either4<LT1, LT2, LT3, R> zip(final Stream<? extends U> other,
            final BiFunction<? super RT, ? super U, ? extends R> zipper) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream)
     */
    @Override
    default <U> Either4<LT1, LT2, LT3, Tuple2<RT, U>> zip(final Stream<? extends U> other) {

        return (Either4) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq)
     */
    @Override
    default <U> Either4<LT1, LT2, LT3, Tuple2<RT, U>> zip(final Seq<? extends U> other) {

        return (Either4) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.lang.Iterable)
     */
    @Override
    default <U> Either4<LT1, LT2, LT3, Tuple2<RT, U>> zip(final Iterable<? extends U> other) {

        return (Either4) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Unit#unit(java.lang.Object)
     */
    @Override
    <T> Either4<LT1, LT2,LT3, T> unit(T unit);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.lang.
     * Iterable, java.util.function.BiFunction)
     */
    @Override
    default <T2, R> Either4<LT1, LT2, LT3, R> zip(final Iterable<? extends T2> app,
            final BiFunction<? super RT, ? super T2, ? extends R> fn) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.zip(app, fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.util.
     * function.BiFunction, org.reactivestreams.Publisher)
     */
    @Override
    default <T2, R> Either4<LT1, LT2,LT3, R> zip(final BiFunction<? super RT, ? super T2, ? extends R> fn,
            final Publisher<? extends T2> app) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.zip(fn, app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bipeek(java.util.function.Consumer,
     * java.util.function.Consumer)
     */
    @Override
    default Either4<LT1, LT2, LT3, RT> bipeek(final Consumer<? super LT3> c1, final Consumer<? super RT> c2) {

        return (Either4<LT1, LT2, LT3, RT>) BiFunctor.super.bipeek(c1, c2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bicast(java.lang.Class,
     * java.lang.Class)
     */
    @Override
    default <U1, U2> Either4<LT1, LT2,U1, U2> bicast(final Class<U1> type1, final Class<U2> type2) {

        return (Either4<LT1, LT2,U1, U2>) BiFunctor.super.bicast(type1, type2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.BiFunctor#bitrampoline(java.util.function.Function,
     * java.util.function.Function)
     */
    @Override
    default <R1, R2> Either4<LT1, LT2, R1, R2> bitrampoline(
            final Function<? super LT3, ? extends Trampoline<? extends R1>> mapper1,
            final Function<? super RT, ? extends Trampoline<? extends R2>> mapper2) {

        return (Either4<LT1,LT2, R1, R2>) BiFunctor.super.bitrampoline(mapper1, mapper2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#cast(java.lang.Class)
     */
    @Override
    default <U> Either4<LT1, LT2, LT3, U> cast(final Class<? extends U> type) {

        return (Either4<LT1, LT2, LT3, U>) ApplicativeFunctor.super.cast(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#peek(java.util.function.Consumer)
     */
    @Override
    default Either4<LT1, LT2, LT3, RT> peek(final Consumer<? super RT> c) {

        return (Either4<LT1, LT2, LT3, RT>) ApplicativeFunctor.super.peek(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Functor#trampoline(java.util.function.Function)
     */
    @Override
    default <R> Either4<LT1, LT2, LT3, R> trampoline(final Function<? super RT, ? extends Trampoline<? extends R>> mapper) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.trampoline(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Functor#patternMatch(java.util.function.Function,
     * java.util.function.Supplier)
     */
    @Override
    default <R> Either4<LT1, LT2, LT3, R> patternMatch(final Function<CheckValue1<RT, R>, CheckValue1<RT, R>> case1,
            final Supplier<? extends R> otherwise) {

        return (Either4<LT1, LT2, LT3, R>) ApplicativeFunctor.super.patternMatch(case1, otherwise);
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final @EqualsAndHashCode(of = { "value" }) 
    static class Lazy<ST, M,M2, PT> implements Either4<ST, M,M2, PT> {

        private final Eval<Either4<ST, M,M2, PT>> lazy;

        public Either4<ST, M,M2, PT> resolve() {
            return lazy.get()
                       .visit(Either4::left1, Either4::second,Either4::third, Either4::right);
        }

        private static <ST, M,M2, PT> Lazy<ST, M,M2, PT> lazy(final Eval<Either4<ST, M,M2, PT>> lazy) {
            return new Lazy<>(lazy);
        }

        @Override
        public <R> Either4<ST, M,M2, R> map(final Function<? super PT, ? extends R> mapper) {
            return lazy(Eval.later(() -> resolve().map(mapper)));
        }

        @Override
        public <RT1> Either4<ST, M,M2, RT1> flatMap(
                final Function<? super PT, ? extends Either4<? extends ST, ? extends M, ? extends M2,? extends RT1>> mapper) {
            return lazy(Eval.later(() -> resolve().flatMap(mapper)));
        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.fromEval(Eval.later(() -> resolve().filter(test)))
                        .flatMap(Function.identity());

        }

        @Override
        public PT get() {
            return trampoline().get();
        }

        private Either4<ST,M,M2,PT> trampoline(){
            Either4<ST,M,M2,PT> maybe = lazy.get();
            while (maybe instanceof Lazy) {
                maybe = ((Lazy<ST,M,M2,PT>) maybe).lazy.get();
            }
            return maybe;
        }
        @Override
        public ReactiveSeq<PT> stream() {

            return trampoline()
                       .stream();
        }

        @Override
        public Iterator<PT> iterator() {

            return trampoline()
                       .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {

            return trampoline()
                       .visit(present, absent);
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

            lazy.get()
                .subscribe(s);
        }

        @Override
        public boolean test(final PT t) {
            return trampoline()
                       .test(t);
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> first,
                final Function<? super M, ? extends R> second,
                final Function<? super M2, ? extends R> third,
                final Function<? super PT, ? extends R> primary) {

            return trampoline()
                       .visit(first, second,third, primary);
        }
        @Override
        public Either4<ST, M, PT, M2> swap3() {
            return lazy(Eval.later(() -> resolve().swap3()));
        }
        @Override
        public Either4<ST, PT, M2, M> swap2() {
            return lazy(Eval.later(() -> resolve().swap2()));
        }

        @Override
        public Either4<PT, M,M2, ST> swap1() {
            return lazy(Eval.later(() -> resolve().swap1()));
        }

        @Override
        public boolean isRight() {
            return trampoline()
                       .isRight();
        }

        @Override
        public boolean isLeft1() {
            return trampoline()
                       .isLeft1();
        }

        @Override
        public boolean isLeft2() {
            return trampoline()
                       .isLeft2();
        }
        @Override
        public boolean isLeft3() {
            return trampoline()
                       .isLeft3();
        }

        @Override
        public <R1, R2> Either4<ST, M,R1, R2> bimap(final Function<? super M2, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return lazy(Eval.later(() -> resolve().bimap(fn1, fn2)));
        }

        @Override
        public <T> Either4<ST, M, M2,T> unit(final T unit) {

            return Either4.right(unit);
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(of = { "value" })
    static class Right<ST, M,M2, PT> implements Either4<ST, M,M2, PT> {
        private final Eval<PT> value;

        @Override
        public <R> Either4<ST, M, M2, R> map(final Function<? super PT, ? extends R> fn) {
            return new Right<ST, M, M2, R>(
                                       value.map(fn));
        }

        @Override
        public Either4<ST, M,M2, PT> peek(final Consumer<? super PT> action) {
            return map(i -> {
                action.accept(i);
                return i;
            });

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.fromEval(Eval.later(() -> test.test(get()) ? Maybe.just(get()) : Maybe.<PT> none()))
                        .flatMap(Function.identity());

        }

        @Override
        public PT get() {
            return value.get();
        }

        @Override
        public <RT1> Either4<ST, M, M2, RT1> flatMap(
                final Function<? super PT, ? extends Either4<? extends ST, ? extends M, ? extends M2, ? extends RT1>> mapper) {
            final Eval<Either4<ST, M, M2, RT1>> e3 = (Eval<Either4<ST, M, M2, RT1>>) value.map(mapper);
            return new Lazy<>(
                              e3);
          

        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public boolean isLeft1() {
            return false;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either4.right[" + value + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid,  
                final Function<? super M2, ? extends R> mid2,final Function<? super PT, ? extends R> primary) {
            return primary.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either4<ST, M,M2, R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return new Right<>(
                               value.combine(app, fn));

        }

        @Override
        public <R1, R2> Either4<ST,M, R1, R2> bimap(final Function<? super M2, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either4<ST, M,R1, R2>) this.map(fn2);
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return value.stream();
        }

        @Override
        public Iterator<PT> iterator() {
            return value.iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return value.visit(present, absent);
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {
            value.subscribe(s);

        }

        @Override
        public boolean test(final PT t) {
            return value.test(t);
        }

        @Override
        public <T> Either4<ST, M, M2, T> unit(final T unit) {
            return Either4.right(unit);
        }
        @Override
        public Either4<ST,  M, PT, M2> swap3() {

            return  new Left3<>(value);
        }


        @Override
        public Either4<ST, PT, M2, M> swap2() {

            return  new Left2<>(value);
        }

        
        @Override
        public Either4<PT, M,M2, ST> swap1() {

            return new Left1<>(
                              value);
        }

        @Override
        public boolean isLeft2() {

            return false;
        }
        @Override
        public boolean isLeft3() {

            return false;
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(of = { "value" })
    static class Left1<ST, M, M2,PT> implements Either4<ST, M,M2, PT> {
        private final Eval<ST> value;

        @Override
        public <R> Either4<ST, M, M2, R> map(final Function<? super PT, ? extends R> fn) {
            return (Either4<ST, M, M2,R>) this;
        }

        @Override
        public Either4<ST, M, M2, PT> peek(final Consumer<? super PT> action) {
            return this;

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.none();

        }

        @Override
        public PT get() {
            throw new NoSuchElementException(
                                             "Attempt to access right value on a Left Either4");
        }

        @Override
        public <RT1> Either4<ST, M, M2, RT1> flatMap(
                final Function<? super PT, ? extends Either4<? extends ST, ? extends M,? extends M2, ? extends RT1>> mapper) {

            return (Either4) this;

        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public boolean isLeft1() {
            return true;
        }
        @Override
        public boolean isLeft3() {

            return false;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either4.left1[" + value + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid, 
                final Function<? super M2, ? extends R> mid2, 
                final Function<? super PT, ? extends R> primary) {
            return secondary.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either4<ST, M, M2, R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Either4<ST, M,M2, R>) this;

        }

        @Override
        public <R1, R2> Either4<ST, M,R1, R2> bimap(final Function<? super M2, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either4<ST,M, R1, R2>) this;
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return ReactiveSeq.empty();
        }

        @Override
        public Iterator<PT> iterator() {
            return Arrays.<PT> asList()
                         .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return absent.get();
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

        }

        @Override
        public boolean test(final PT t) {
            return false;
        }

        @Override
        public <T> Either4<ST, M,M2, T> unit(final T unit) {
            return Either4.right(unit);
        }
        @Override
        public Either4<ST, M,PT, M2> swap3() {

            return (Either4<ST, M,PT, M2>) this;
        }

        @Override
        public Either4<ST, PT,M2, M> swap2() {

            return (Either4<ST, PT,M2, M>) this;
        }

        @Override
        public Either4<PT, M,M2, ST> swap1() {

            return new Right<>(
                               value);
        }

        @Override
        public boolean isLeft2() {

            return false;
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(of = { "value" })
    static class Left2<ST, M,M2, PT> implements Either4<ST, M, M2, PT> {
        private final Eval<M> value;

        @Override
        public <R> Either4<ST, M, M2,R> map(final Function<? super PT, ? extends R> fn) {
            return (Either4<ST, M, M2,R>) this;
        }

        @Override
        public Either4<ST, M, M2, PT> peek(final Consumer<? super PT> action) {
            return this;

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.none();

        }

        @Override
        public PT get() {
            throw new NoSuchElementException(
                                             "Attempt to access right value on a Middle Either4");
        }

        @Override
        public <RT1> Either4<ST, M, M2,RT1> flatMap(
                final Function<? super PT, ? extends Either4<? extends ST, ? extends M, ? extends M2, ? extends RT1>> mapper) {

            return (Either4) this;

        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public boolean isLeft1() {
            return false;
        }
        

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either4.left2[" + value + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid1,
                final Function<? super M2, ? extends R> mid2, 
                final Function<? super PT, ? extends R> primary) {
            return mid1.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either4<ST, M, M2,R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Either4<ST, M, M2,R>) this;

        }

        @Override
        public <R1, R2> Either4<ST, M, R1, R2> bimap(final Function<? super M2, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either4<ST, M,R1, R2>) this;
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return ReactiveSeq.empty();
        }

        @Override
        public Iterator<PT> iterator() {
            return Arrays.<PT> asList()
                         .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return absent.get();
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

        }

        @Override
        public boolean test(final PT t) {
            return false;
        }

        @Override
        public <T> Either4<ST, M,M2, T> unit(final T unit) {
            return Either4.right(unit);
        }
        @Override
        public Either4<ST, M, PT,M2> swap3() {
            return (Either4<ST, M, PT,M2>) this;

        }
        @Override
        public Either4<ST, PT,M2, M> swap2() {
            return new Right<>(
                               value);

        }

        @Override
        public Either4<PT, M, M2,ST> swap1() {
            return (Either4<PT, M,M2, ST>) this;

        }

        @Override
        public boolean isLeft2() {

            return true;
        }
        @Override
        public boolean isLeft3() {

            return false;
        }

    }
    @AllArgsConstructor(access = AccessLevel.PRIVATE) 
    @EqualsAndHashCode(of = { "value" })
    static class Left3<ST, M,M2, PT> implements Either4<ST, M, M2, PT> {
        private final Eval<M2> value;

        @Override
        public <R> Either4<ST, M, M2,R> map(final Function<? super PT, ? extends R> fn) {
            return (Either4<ST, M, M2,R>) this;
        }

        @Override
        public Either4<ST, M, M2, PT> peek(final Consumer<? super PT> action) {
            return this;

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.none();

        }

        @Override
        public PT get() {
            throw new NoSuchElementException(
                                             "Attempt to access right value on a Middle Either4");
        }

        @Override
        public <RT1> Either4<ST, M, M2,RT1> flatMap(
                final Function<? super PT, ? extends Either4<? extends ST, ? extends M, ? extends M2, ? extends RT1>> mapper) {

            return (Either4) this;

        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public boolean isLeft1() {
            return false;
        }
        @Override
        public boolean isLeft3() {

            return true;
        }
        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either4.left3[" + value + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid1,
                final Function<? super M2, ? extends R> mid2, 
                final Function<? super PT, ? extends R> primary) {
            return mid2.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either4<ST, M, M2,R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Either4<ST, M, M2,R>) this;

        }

        @Override
        public <R1, R2> Either4<ST, M, R1, R2> bimap(final Function<? super M2, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either4<ST, M,R1, R2>) this;
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return ReactiveSeq.empty();
        }

        @Override
        public Iterator<PT> iterator() {
            return Arrays.<PT> asList()
                         .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return absent.get();
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

        }

        @Override
        public boolean test(final PT t) {
            return false;
        }

        @Override
        public <T> Either4<ST, M,M2, T> unit(final T unit) {
            return Either4.right(unit);
        }
        @Override
        public Either4<ST, M, PT,M2> swap3() {
            return new Right<>(
                    value);
            
        }
        @Override
        public Either4<ST, PT,M2, M> swap2() {
           return (Either4<ST, PT,M2, M>)this;

        }

        @Override
        public Either4<PT, M, M2,ST> swap1() {
            return (Either4<PT, M,M2, ST>) this;

        }

        @Override
        public boolean isLeft2() {

            return true;
        }

    }

}
