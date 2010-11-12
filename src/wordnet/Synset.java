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
package wordnet;

import indexing.PosTag.PosCategory;
import java.io.Serializable;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
@SuppressWarnings("serial")
public class Synset implements Serializable {
	
	public enum SynsetCategory {
		NONE, V, N, J, R
	}
	
	private SynsetCategory		pos;
	private long				offset;
	private static final int	MAX_OFFSET	= 100000000;

	public Synset(PosCategory poscat)
	{
		pos = convertPosCategory(poscat);
		offset = 0;
	}

	/**
	 * Constructor for class Synset
	 */
	public Synset(PosCategory poscat, long offset)
	{
		pos = convertPosCategory(poscat);
		this.offset = offset;
	}
	
	private SynsetCategory convertPosCategory(PosCategory poscat)
	{
		switch (poscat) {
			case V:
				return SynsetCategory.V;
			case N:
				return SynsetCategory.N;
			case J:
				return SynsetCategory.J;
			case R:
				return SynsetCategory.R;
			default:
				return SynsetCategory.NONE;
		}
	}
	
	private String getSynsetCategoryString()
	{
		switch (pos) {
			case V:
				return "v";
			case N:
				return "n";
			case J:
				return "a";
			case R:
				return "r";
			default:
				return "none";
		}
	}
	
	
	/**
	 * @return the pos
	 */
	public SynsetCategory getPos()
	{
		return pos;
	}
	
	
	/**
	 * @return the offset
	 */
	public long getOffset()
	{
		return offset;
	}
	
	public boolean hasOffset()
	{
		return offset != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getSynsetCategoryString() + Long.toString(offset, 8);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		Synset other = (Synset)obj;
		return getPos() == other.getPos() && getOffset() == other.getOffset();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return pos.ordinal() * MAX_OFFSET + (int)offset;
	}
}
