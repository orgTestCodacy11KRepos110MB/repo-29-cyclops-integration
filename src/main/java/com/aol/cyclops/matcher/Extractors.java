package com.aol.cyclops.matcher;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;

public class Extractors {
	
	
	
	public final static <R> PatternMatcher.Extractor<Iterable<R>,R> _(int pos){
		return  (Iterable<R> it)-> {
			
			return (R)Seq.seq(it).skip(pos).limit(1).peek(System.out::println).collect(Collectors.toList()).get(0);
		};
		
	}
	
	public final static PatternMatcher.Extractor collectionHead= (list) ->  {
			if(!(list instanceof Collection)) 
				return false;
			return  head((Collection)list); 
	};
	
	
	public static <T> T head(Collection<T> collection) {
		return collection.iterator().next();
	}
}