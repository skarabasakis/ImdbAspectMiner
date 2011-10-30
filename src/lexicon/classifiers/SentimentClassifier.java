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

import lexicon.RatingHistogram;
import lexicon.Ratings;
import lexicon.TermSentiment;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public abstract class SentimentClassifier {
	
	private int		minimum_observations;		// Number of observations threshold for objective
												// (topical) terms
	private double	minimum_observations_log;
	
	/**
	 * Constructor for class SentimentClassifier
	 */
	public SentimentClassifier()
	{
		minimum_observations = 2 * Ratings.N_RATINGS;
		minimum_observations_log = Math.log(minimum_observations);
	}


	public static boolean isClassifiable(RatingHistogram histogram)
	{
		return histogram.getTotalObservations() >= Globals.LexiconParameters.minimumSynsetInstances //
			&& histogram.getUniqueDocuments() > 1;
	}

	public Float getSubjectivityScore(RatingHistogram histogram)
	{
		// Frequency range provides a measure of dispersion of frequencies in the histogram, i.e.
		// indicated how widely the frequencies vary. It is normalized by the histogram's maximum
		// frequency, so that range measures from different histograms can be comparable
		float freq_range = 1 - histogram.getMinFreq() / histogram.getMaxFreq();
		
		// Calculating log(totalObservations, minimumObservations) and pruning negative values
		double observations_log = (float)(Math.log(histogram.getTotalObservations()) / minimum_observations_log);
		observations_log = observations_log > 0 ? observations_log : 0;
		
		return (float)(observations_log * freq_range);
	}
	
	public TermSentiment classify(RatingHistogram histogram)
	{
		TermSentiment sentiment = new TermSentiment();
		sentiment.setDocuments(histogram.getUniqueDocuments());
		sentiment.setObservations(histogram.getTotalObservations());
		sentiment.setSubjectivityScore(getSubjectivityScore(histogram));
		return sentiment;
	}
	
	protected String	name	= null;

	public String name()
	{
		return "sc_" + name;
	}
	
	public static final SentimentClassifier[]	ALL	= { new PeakSentimentClassifier(),
		new PNSentimentClassifier(),
		new WidestWindowSentimentClassifier()		};
}
