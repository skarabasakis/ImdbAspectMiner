// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package classes;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Simple Integer counter class
 * 
 * @author Stelios Karabasakis
 */
public class Counter extends IntegerWrp implements Serializable {
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6316195657074239798L;
	
	/**
	 * Constructor for class Counter. New Counter has a value of zero;
	 */
	public Counter()
	{
		super();
	}
	
	/**
	 * Increment the counter value by 1
	 */
	public void increment()
	{
		value++;
	}
	
	public static ArrayList<Integer> toIntegerArray(ArrayList<Counter> counters) {
		int size = counters.size();
		ArrayList<Integer> integers = new ArrayList<Integer>(size);
		for (int i = 0 ; i < size ; i++) {
			integers.add(i, counters.get(i).get());
		}
		return integers;
	}

}
