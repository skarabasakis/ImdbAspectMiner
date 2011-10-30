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
package lexicon;

import java.io.Serializable;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Frequency implements Serializable, Comparable<Frequency> {
	
	private static final long	serialVersionUID	= -6223807322608341232L;

	protected Integer	classId;
	protected Integer	frequency;
	
	
	/**
	 * Constructor for class Frequency
	 */
	public Frequency(int class_id)
	{
		classId = new Integer(class_id);
		frequency = 0;
	}
	
	protected Frequency(Frequency freq)
	{
		classId = freq.class_id();
		frequency = freq.frequency();
	}

	public void addObservation()
	{
		frequency++;
	}

	public void addObservation(Integer obs_weight)
	{
		frequency += obs_weight;
	}
	
	public Integer frequency()
	{
		return frequency;
	}
	
	public int class_id()
	{
		return classId;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Frequency o)
	{
		return frequency() - o.frequency();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Frequency obj)
	{
		return frequency() == obj.frequency();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "freq(" + classId + ") = " + frequency;
	}

}
