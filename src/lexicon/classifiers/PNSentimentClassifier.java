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

import java.util.Set;
import lexicon.RatingHistogram;
import lexicon.RatingWindow;
import lexicon.Ratings;
import lexicon.TermSentiment;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class PNSentimentClassifier extends SentimentClassifier {
	
	/**
	 * Constructor for class PNSentimentClassifier
	 * 
	 * @param minimumObservations
	 */
	public PNSentimentClassifier()
	{
		super();
		name = "pn";
	}
	
	/*
	 * (non-Javadoc)
	 * @see lexicon.classifiers.SentimentClassifier#classify(lexicon.RatingHistogram)
	 */
	@Override
	public TermSentiment classify(RatingHistogram histogram)
	{
		TermSentiment termSentiment = super.classify(histogram);

		// Calculate polarity and intensity scores
		float freq_cutoff = ( 1 + Globals.Parameters.PNFrequencyCutoff ) * histogram.getFreqAvg();
		for (Polarity polarity : Polarity.values()) {
			Set<Integer> polarity_ratings = histogram.filterRatings(freq_cutoff, new RatingWindow(Ratings.MIN
				.get(polarity), Ratings.MAX.get(polarity)).asSet());
			if (polarity_ratings.isEmpty() == false) {
				Float score = histogram.getFreqSum(histogram.filterRatings(freq_cutoff, polarity_ratings))
					/ histogram.getFreqSum();
				Float rating = histogram.getWeightedAvgRating(polarity_ratings);
				termSentiment.addSentiment(new Sentiment(score, rating));
			}
		}
		
		return termSentiment;
	}

}
