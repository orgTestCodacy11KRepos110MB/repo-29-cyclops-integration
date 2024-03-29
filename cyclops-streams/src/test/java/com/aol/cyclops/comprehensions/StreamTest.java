package com.aol.cyclops.comprehensions;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.val;

import org.jooq.lambda.Seq;
import org.junit.Ignore;
import org.junit.Test;

import com.aol.cyclops.comprehensions.donotation.UntypedDo;
import com.aol.cyclops.comprehensions.donotation.typed.Do;
import com.aol.cyclops.sequence.SequenceM;

public class StreamTest {
	
	@Test
	public void arrayStream() {
		
		List<String> res = Do.addValues(new String[]{"hello world","hello"}) 
							.yield( v1->  v1 + "1")
							.asSequence().toList();
		List<String> expected = Arrays.asList("hello world1", "hello1");
		
		
		
		assertThat(expected, equalTo( res));
	}
	@Test
	public void stringStream() {
		
		List<String> res = Do.add("hello world") 
							.yield( v-> ""+ v + "1").<String>toSequence().toList();
		List<String> expected = Arrays.asList("h1", "e1", "l1", "l1", "o1",  " 1", "w1", "o1", "r1", 
				"l1", "d1");
		
		
		
		assertThat(expected,equalTo( res));
	}
	@Test
	public void stringStreamWithNull() {
		
		SequenceM<String> res =  Do.add(  "hello world") 
							.add((Iterable<String>)null)
							.yield( v1-> v2-> ""+ v1 + v2)
							.unwrap();
		List<String> expected = Arrays.asList();
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test @Ignore
	public void urlStream() throws MalformedURLException {
		val url = new URL("http://www.aol.com");
		SequenceM<String> res = Do.add (url) 
									 .yield( v1->  v1 + "New line!").unwrap();
		List<String> expected = Arrays.asList("h1", "e1", "l1", "l1", "o1",  " 1", "w1", "o1", "r1", 
				"l1", "d1");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void bufferedReaderStream() {
		
		SequenceM<String> res = Do.add(  new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                               	.getResourceAsStream("input2.file")))) 
                              .yield( v -> ""+ v + "*").unwrap();
		List<String> expected = Arrays.asList("line 1*","line 2*","line 3*");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void urlStream2() {
		URL url =this.getClass().getClassLoader().getResource("input2.file");
		SequenceM<String> res = Do.add (url) 
							.yield( v-> ""+ v+ "*").unwrap();
		List<String> expected = Arrays.asList("line 1*","line 2*","line 3*");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void fileStream2() {
		URL url =this.getClass().getClassLoader().getResource("input2.file");
		File file = new File(url.getFile());
		SequenceM<String> res = Do.add(  file) 
							 .yield( v-> ""+ v + "*").unwrap();
		List<String> expected = Arrays.asList("line 1*","line 2*","line 3*");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void enumStream() {
		
		SequenceM<String> res = UntypedDo.add ( MyEnum.class) 
									 .yield( v -> ""+ v + "*");
		List<String> expected = Arrays.asList("FIRST*","SECOND*","THIRD*");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void iterableStream() {
		
		SequenceM<String> res = Do.add(  new MyIterable()) 
							.yield( v->  v + "*").unwrap();
		List<String> expected = Arrays.asList("hello*","world*");
		
		
		
		assertThat(expected, equalTo( res.toList()));
	}
	@Test
	public void mapStream() {
		
		Map<String,Integer> m = new HashMap<>();
		m.put("hello",10);
		List<String> res = Do.addStream(m.entrySet().stream()) 
								.yield( v-> ""+ v + "*")
								.asSequence()
								.toList();
		List<String> expected = Arrays.asList("hello=10*");
		
		
		
		
		assertThat(expected, equalTo( res));
	}
	
	static enum MyEnum{FIRST, SECOND, THIRD}
	
	static class MyIterable implements Iterable<String>{

		@Override
		public Iterator iterator() {
			return Arrays.asList("hello","world").iterator();
		}
		
	}
}
