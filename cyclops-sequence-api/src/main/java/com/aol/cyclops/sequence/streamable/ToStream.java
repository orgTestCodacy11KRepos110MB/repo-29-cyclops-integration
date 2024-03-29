package com.aol.cyclops.sequence.streamable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.aol.cyclops.invokedynamic.InvokeDynamic;
import com.aol.cyclops.objects.AsDecomposable;
import com.aol.cyclops.sequence.ReversedIterator;
import com.aol.cyclops.sequence.SeqUtils;
import com.aol.cyclops.sequence.SequenceM;

public interface ToStream<T> extends Iterable<T>{
	default Iterator<T> iterator(){
		return stream().iterator();
	}
	default  Object getStreamable(){
		return this;
	}
	default SequenceM<T> reveresedSequenceM(){
		return SequenceM.fromStream(reveresedStream());
	}
	/**
	 * @return SequenceM from this Streamable
	 */
	default SequenceM<T> sequenceM(){
		return SequenceM.fromStream(stream());
	}
	default Stream<T> reveresedStream(){
		Object streamable = getStreamable();
		if(streamable instanceof List){
			return StreamSupport.stream(new ReversedIterator((List)streamable).spliterator(),false);
		}
		if(streamable instanceof Object[]){
			List arrayList = Arrays.asList((Object[])streamable);
			return StreamSupport.stream(new ReversedIterator(arrayList).spliterator(),false);
		}
		return SeqUtils.reverse(stream());
	}
	default boolean isEmpty(){
		return this.sequenceM().isEmpty();
	}
	/**
	 * @return New Stream
	 */
	default Stream<T> stream(){
		Object streamable = getStreamable();
		if(streamable instanceof Stream)
			return (Stream)streamable;
		if(streamable instanceof Iterable)
			return StreamSupport.stream(((Iterable)streamable).spliterator(), false);
		return  new InvokeDynamic().stream(streamable).orElseGet( ()->
								(Stream)StreamSupport.stream(AsDecomposable.asDecomposable(streamable)
												.unapply()
												.spliterator(),
													false));
	}
	
}
