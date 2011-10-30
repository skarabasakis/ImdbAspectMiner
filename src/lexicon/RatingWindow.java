// The MIT License
//
// Copyright (c) 2011 Stelios Karabasakis
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

import java.util.HashSet;
import java.util.Set;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class RatingWindow {
	
	private static final int	UNDEFINED	= -1;
	protected int				min_rating;
	protected int				max_rating;
	
	/**
	 * Constructor for class RatingWindow
	 */
	public RatingWindow()
	{
		min_rating = max_rating = UNDEFINED;
	}
	
	public RatingWindow(int min_rating, int max_rating)
	{
		this.min_rating = min_rating;
		this.max_rating = max_rating;
	}

	private boolean isUndefined()
	{
		return min_rating == UNDEFINED && max_rating == UNDEFINED;
	}
	
	private void checkBounds(int rating)
	{
		if (rating < Ratings.MIN_RATING && rating > Ratings.MAX_RATING)
			throw new IndexOutOfBoundsException("Rating " + rating + "is out of accepted rating bounds ("
				+ Ratings.MIN_RATING + "," + Ratings.MAX_RATING + ")");
	}

	public boolean contains(int rating)
	{
		return !isUndefined() && rating >= min_rating && rating <= max_rating;
	}
	
	public boolean canInclude(int rating)
	{
		// TODO Currently does not account for ratings within range
		return isUndefined() || rating == min_rating - 1 || rating == max_rating + 1;
	}
	
	public void include(int rating)
	{
		checkBounds(rating);
		if (isUndefined()) {
			min_rating = max_rating = rating;
		}
		else if (rating < min_rating) {
			min_rating = rating;
		}
		else {
			max_rating = rating;
		}

	}
	
	
	public boolean canBeMergedWith(RatingWindow window)
	{
		// TODO Currently does not account for overlapping windows
		return min_rating == window.max_rating + 1 || max_rating == window.min_rating - 1;
	}
	
	public RatingWindow mergeWith(RatingWindow window)
	{
		min_rating = Math.min(min_rating, window.min_rating);
		max_rating = Math.max(max_rating, window.max_rating);
		return this;
	}


	public int width()
	{
		return isUndefined() ? 0 : 1 + max_rating - min_rating;
	}
	
	public Set<Integer> asSet()
	{
		if (isUndefined())
			return new HashSet<Integer>();
		else {
			HashSet<Integer> set = new HashSet<Integer>(width());
			for (int rating = min_rating ; rating <= max_rating ; rating ++) {
				set.add(rating);
			}
			return set;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object window)
	{
		return min_rating == ((RatingWindow)window).min_rating && max_rating == ((RatingWindow)window).max_rating;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "[" + min_rating + "-" + max_rating + "]";
	}
}
