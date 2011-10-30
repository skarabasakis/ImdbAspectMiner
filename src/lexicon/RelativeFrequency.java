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
public class RelativeFrequency extends Frequency implements Serializable, Comparable<Frequency> {
	
	private static final long	serialVersionUID	= 7930021210327664260L;

	public Float	weightFactor;
	
	// documentsPerRatingWeight
	// ????

	/**
	 * Constructor for class RelativeFrequency
	 * @param classId
	 */
	public RelativeFrequency(int classId, Float weight)
	{
		super(classId);
		weightFactor = weight;
	}
	
	public RelativeFrequency(Frequency freq, Float weight)
	{
		super(freq);
		weightFactor = weight;
	}
	
	public void setWeight(Float weight)
	{
		weightFactor = weight;
	}

	public Float relFrequency()
	{
		return weightFactor * frequency();
	}
	
	public Float relFrequency(Float rel_frequency_total, Integer normalization_factor)
	{
		return relFrequency() * normalization_factor / rel_frequency_total;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Frequency o)
	{
		float diff = relFrequency() - ((RelativeFrequency)o).relFrequency();
		return (int)(diff + Math.signum(diff));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Frequency obj)
	{
		return relFrequency() == ((RelativeFrequency)obj).relFrequency();
	}
	
	/*
	 * (non-Javadoc)
	 * @see lexicon.Frequency#toString()
	 */
	@Override
	public String toString()
	{
		return "relf(" + classId + ") = " + relFrequency();
	}

}
