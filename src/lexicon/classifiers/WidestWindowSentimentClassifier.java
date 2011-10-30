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
package lexicon.classifiers;

import java.util.Iterator;
import java.util.LinkedList;
import lexicon.RatingHistogram;
import lexicon.RatingWindow;
import lexicon.Ratings;
import lexicon.TermSentiment;
import lexicon.TermSentiment.Sentiment;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class WidestWindowSentimentClassifier extends SentimentClassifier {
	
	public static final float	COVERAGE_THRESHOLD_BASE	= 0.5F;

	/**
	 * Constructor for class WidestWindowSentimentClassifier
	 * 
	 * @param minimumObservations
	 */
	public WidestWindowSentimentClassifier()
	{
		super();
		name = "ww";
	}
	
	/*
	 * (non-Javadoc)
	 * @see lexicon.classifiers.SentimentClassifier#classify(lexicon.RatingHistogram)
	 */
	@Override
	public TermSentiment classify(RatingHistogram histogram)
	{
		LinkedList<RatingWindow> sentiment_windows = new LinkedList<RatingWindow>();
		Float freq_sum = 0F;
		Float sentiment_windows_coverage = 0F;
		boolean minimum_coverage_satisfied = false;
		boolean done = false;
		
		int current_rank = 0;
		do {
			++current_rank;
			int current_rating = histogram.getRatingByRank(current_rank);
			
			RatingWindow current_rating_sentiment_window = null;
			boolean included = false;

			Iterator<RatingWindow> sentiment_windows_iterator = sentiment_windows.iterator();
			while ( sentiment_windows_iterator.hasNext() ) {
				RatingWindow sentiment_window = sentiment_windows_iterator.next();
				
				// If this rating is not part of a window yet, check if it is adjacent to one of the
				// existing windows, and if it is, expand the sentiment window to include the new
				// rating
				if (!included) {
					if (sentiment_window.canInclude(current_rating)) {
						current_rating_sentiment_window = sentiment_window;
						current_rating_sentiment_window.include(current_rating);
						freq_sum += histogram.getFreq(current_rating);
						sentiment_windows_coverage = freq_sum / histogram.getFreqSum();
						included = true;
					}
				}
				// If this rating has already been added to a window, check if the window it was
				// added to can be merged with one the remaining sentiment windows and merge
				// wherever possible
				else {
					if (sentiment_windows_coverage <= requiredSentimentWindowsCoverage(sentiment_windows.size())) {
						if (current_rating_sentiment_window.canBeMergedWith(sentiment_window)) {
							current_rating_sentiment_window.mergeWith(sentiment_window);
							sentiment_windows_iterator.remove();
						}
					}
					else {
						break;
					}
				}
			}
			
			// If this rating could not be added to any of the already existing windows, create anew
			// window for it and add it to the list of sentiment windows
			if (!included) {
				if (sentiment_windows_coverage <= requiredSentimentWindowsCoverage(sentiment_windows.size())) {
					current_rating_sentiment_window = new RatingWindow(current_rating, current_rating);
					sentiment_windows.addFirst(current_rating_sentiment_window);
					freq_sum += histogram.getFreq(current_rating);
					sentiment_windows_coverage = freq_sum / histogram.getFreqSum();
					included = true;
				}
				else {
					done = true;
				}
			}
			
		} while ( !done && current_rank < Ratings.N_RATINGS );
		

		TermSentiment sentiments = super.classify(histogram);
		for (RatingWindow sentiment_window : sentiment_windows) {
			sentiments.addSentiment(new Sentiment(histogram.getFreqSum(sentiment_window.asSet())
				/ histogram.getFreqSum(), histogram.getWeightedAvgRating(sentiment_window.asSet())));
		}

		return sentiments;
	}
	
	
	/**
	 * @param size
	 * @return
	 */
	private Float requiredSentimentWindowsCoverageThr(int n_of_windows, float coverage_threshold)
	{
		Float coverage_sum = 0F;
		for (int p = 1 ; p <= n_of_windows ; p++) {
			Float coverage_factor = 1F;
			for (int q = 1 ; q <= p ; q++) {
				coverage_factor *= coverage_threshold;
			}
		}
		
		return coverage_sum;
	}
	
	private Float requiredSentimentWindowsCoverage(int n_of_windows)
	{
		switch (n_of_windows) {
			case 0:
				return 0F;
			case 1:
				return 0.5F;
			case 2:
				return 0.75F;
			case 3:
				return 0.875F;
			default:
				return requiredSentimentWindowsCoverageThr(n_of_windows, COVERAGE_THRESHOLD_BASE);
		}
	}
}
