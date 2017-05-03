package com.aol.cyclops.reactor.hkt.typeclasses.instances;
import static com.aol.cyclops.reactor.hkt.MonoKind.widen;

import static cyclops.function.Lambda.l1;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.aol.cyclops.reactor.Monos;
import com.aol.cyclops.reactor.hkt.MonoKind;
import com.aol.cyclops2.hkt.Higher;
import cyclops.async.Future;
import cyclops.control.Maybe;
import cyclops.function.Fn1;
import cyclops.function.Monoid;
import org.junit.Test;


import reactor.core.publisher.Mono;

public class MonosTest {

    @Test
    public void unit(){
        
        MonoKind<String> opt = Monos.Instances.unit()
                                            .unit("hello")
                                            .convert(MonoKind::narrowK);
        
        assertThat(opt.toFuture().join(),equalTo(Future.ofResult("hello").join()));
    }
    @Test
    public void functor(){
        
        MonoKind<Integer> opt = Monos.Instances.unit()
                                     .unit("hello")
                                     .apply(h-> Monos.Instances.functor().map((String v) ->v.length(), h))
                                     .convert(MonoKind::narrowK);
        
        assertThat(opt.toFuture().join(),equalTo(Future.ofResult("hello".length()).join()));
    }
    @Test
    public void apSimple(){
        Monos.Instances.applicative()
            .ap(widen(Future.ofResult(l1(this::multiplyByTwo))),widen(Future.ofResult(1)));
    }
    private int multiplyByTwo(int x){
        return x*2;
    }
    @Test
    public void applicative(){
        
        MonoKind<Fn1<Integer,Integer>> optFn = Monos.Instances.unit().unit(l1((Integer i) ->i*2)).convert(MonoKind::narrowK);
        
        MonoKind<Integer> opt = Monos.Instances.unit()
                                     .unit("hello")
                                     .apply(h-> Monos.Instances.functor().map((String v) ->v.length(), h))
                                     .apply(h-> Monos.Instances.applicative().ap(optFn, h))
                                     .convert(MonoKind::narrowK);
        
        assertThat(opt.toFuture().join(),equalTo(Future.ofResult("hello".length()*2).join()));
    }
    @Test
    public void monadSimple(){
       MonoKind<Integer> opt  = Monos.Instances.monad()
                                            .<Integer,Integer>flatMap(i->widen(Future.ofResult(i*2)), widen(Future.ofResult(3)))
                                            .convert(MonoKind::narrowK);
    }
    @Test
    public void monad(){
        
        MonoKind<Integer> opt = Monos.Instances.unit()
                                     .unit("hello")
                                     .apply(h-> Monos.Instances.monad().flatMap((String v) -> Monos.Instances.unit().unit(v.length()), h))
                                     .convert(MonoKind::narrowK);
        
        assertThat(opt.toFuture().join(),equalTo(Future.ofResult("hello".length()).join()));
    }
    @Test
    public void monadZeroFilter(){
        
        MonoKind<String> opt = Monos.Instances.unit()
                                     .unit("hello")
                                     .apply(h-> Monos.Instances.monadZero().filter((String t)->t.startsWith("he"), h))
                                     .convert(MonoKind::narrowK);
        
        assertThat(opt.toFuture().join(),equalTo(Future.ofResult("hello").join()));
    }
    @Test
    public void monadZeroFilterOut(){
        
        MonoKind<String> opt = Monos.Instances.unit()
                                     .unit("hello")
                                     .apply(h-> Monos.Instances.monadZero().filter((String t)->!t.startsWith("he"), h))
                                     .convert(MonoKind::narrowK);
        
        assertTrue(opt.block()==null);
    }
    
    @Test
    public void monadPlus(){
        MonoKind<Integer> opt = Monos.Instances.<Integer>monadPlus()
                                      .plus(MonoKind.widen(Mono.empty()), MonoKind.widen(Mono.just(10)))
                                      .convert(MonoKind::narrowK);
        assertTrue(opt.block()==null);
    }
    @Test
    public void monadPlusNonEmpty(){
        
        Monoid<MonoKind<Integer>> m = Monoid.of(MonoKind.widen(Mono.empty()), (a, b)->a.toFuture().isDone() ? b : a);
        MonoKind<Integer> opt = Monos.Instances.<Integer>monadPlus(m)
                                      .plus(MonoKind.widen(Mono.just(5)), MonoKind.widen(Mono.just(10)))
                                      .convert(MonoKind::narrowK);
        assertThat(opt.block(),equalTo(10));
    }
    @Test
    public void  foldLeft(){
        int sum  = Monos.Instances.foldable()
                        .foldLeft(0, (a,b)->a+b, MonoKind.widen(Future.ofResult(4)));
        
        assertThat(sum,equalTo(4));
    }
    @Test
    public void  foldRight(){
        int sum  = Monos.Instances.foldable()
                        .foldRight(0, (a,b)->a+b, MonoKind.widen(Future.ofResult(1)));
        
        assertThat(sum,equalTo(1));
    }
    @Test
    public void traverse(){
       Maybe<Higher<MonoKind.µ, Integer>> res = Monos.Instances.traverse()
                                                                 .traverseA(Maybe.Instances.applicative(), (Integer a)->Maybe.just(a*2), MonoKind.just(1))
                                                                 .convert(Maybe::narrowK);
       
       
       assertThat(res.map(h->h.convert(MonoKind::narrowK).block()),
                  equalTo(Maybe.just(Mono.just(2).block())));
    }
    
}
