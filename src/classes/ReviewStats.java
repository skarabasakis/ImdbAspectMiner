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
import java.util.HashMap;
import java.util.Iterator;
import lexicon.Ratings;


/**
 * Stores, aggregates and retrieves statistical data on the word length of the reviews in the
 * corpus.
 * 
 * @author Stelios Karabasakis
 */
public class ReviewStats implements Serializable {
	
	private static final long						serialVersionUID	= 7361056987594437171L;

	private Integer									currentReviewId;
	private Integer									currentReviewRating;
	private Counter									currentReviewCounter;
	
	// Maps review id's to their corresponding lengths, grouped by rating
	private ArrayList<HashMap<Integer, Integer>>	reviewLengths;
	
	/**
	 * Constructor for class ReviewStats
	 */
	public ReviewStats()
	{
		currentReviewCounter = new Counter();

		reviewLengths = new ArrayList<HashMap<Integer, Integer>>(Ratings.capacity());
		for (int pos = 0 ; pos < Ratings.capacity() ; pos++) {
			reviewLengths.add(new HashMap<Integer, Integer>());
		}
	}
	
	/**
	 * Import review information into the {@link ReviewStats} object. The information remains staged
	 * until {@link setCurrent} is called again, at which point it is overwritten. In order to
	 * permanently store the review information into the object, follow-up with a call to
	 * {@link storeCurrent}
	 * 
	 * @param reviewId
	 *            The id number of the review to import
	 * @param rating
	 *            The rating of the review to import
	 * @return A reference to counter object to count review length with.
	 */
	public Counter setCurrent(int reviewId, int rating)
	{
		currentReviewId = reviewId;
		currentReviewRating = rating;
		
		return currentReviewCounter;
	}
	
	/**
	 * Get a counter
	 * 
	 * @return A counter object to count review length with
	 */
	public Counter getCurrent()
	{
		return currentReviewCounter;
	}

	public boolean storeCurrent()
	{
		if (currentReviewId != 0) {
			reviewLengths.get(currentReviewRating).put(currentReviewId, currentReviewCounter.get());
			resetCurrent();
			return true;
		}
		else
			return false;
	}
	
	public void resetCurrent()
	{
		currentReviewId = 0;
		currentReviewRating = 0;
		currentReviewCounter.reset();
	}
	
	/**
	 * @param rating
	 * @return
	 */
	public int getTotalReviews(int rating)
	{
		return reviewLengths.get(rating).keySet().size();
	}

	public int getTotalReviews()
	{
		int total_reviews = 0;
		
		for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
			total_reviews += getTotalReviews(rating);
		}
		
		return total_reviews;
	}

	public int getTotalLength(int rating) {
		int total_length = 0;

		Iterator<Integer> i = reviewLengths.get(rating).values().iterator();
		while ( i.hasNext() ) {
			total_length += i.next();
		}
		
		return total_length;
	}
	
	public int getTotalLength() {
		int total_length = 0;

		for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
			total_length += getTotalLength(rating);
		}
		
		return total_length;
	}
	
	public ArrayList<Float> getRatingWeights()
	{
		int total_length = getTotalLength();
		ArrayList<Float> rating_weights = new ArrayList<Float>(Ratings.N_RATINGS);
		Float min_weight = Float.MAX_VALUE;
		for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
			Float current_weight = total_length / (float)getTotalLength(rating);
			rating_weights.add(current_weight);
			min_weight = Math.min(min_weight, current_weight);
		}
		

		ArrayList<Float> rating_weights_normalized = new ArrayList<Float>(Ratings.N_RATINGS);
		for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
			rating_weights_normalized.add(rating_weights.get(rating - 1) / min_weight);
		}

		return rating_weights_normalized;
	}


}
