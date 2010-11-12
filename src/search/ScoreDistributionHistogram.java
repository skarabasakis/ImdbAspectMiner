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
package search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import search.SentimentClassification.Polarity;
import search.SentimentClassification.Subjectivity;
import classes.ReviewStats;
import config.Globals;



/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ScoreDistributionHistogram {
	
	// private Synset synset;
	private ArrayList<Frequency>	frequencies;
	
	/**
	 * Constructor for class ScoreDistributionHistogram
	 */
	public ScoreDistributionHistogram()
	{
		for (int i = Ratings.MIN_RATING ; i <= Ratings.MAX_RATING ; i++) {
			frequencies.add(new Frequency(i));
		}
	}

	public void addObeservation(int class_num, int weight)
	{
		frequencies.get(class_num).addObservation(weight);
	}
	
	public ArrayList<RelativeFrequency> getRelativeFrequencies(ReviewStats reviewStats) {
		ArrayList<Float> classWeights = reviewStats.getRatingWeights();
		ArrayList<RelativeFrequency> relativeFrequencies = new ArrayList<RelativeFrequency>();
		for (int class_num = Ratings.MIN_RATING; class_num <= Ratings.MAX_RATING; class_num++) {
			relativeFrequencies.add(new RelativeFrequency(frequencies.get(class_num), classWeights.get(class_num)));
		}
		return relativeFrequencies;
	}
	
	private ArrayList<RelativeFrequency> filterFrequenciesByPolarity(ArrayList<RelativeFrequency> all_frequencies, Polarity polarity) {
		ArrayList<RelativeFrequency> polarity_frequencies = new ArrayList<RelativeFrequency>();
		
		Iterator<RelativeFrequency> i = all_frequencies.iterator();
		while (i.hasNext()) {
			RelativeFrequency current_relative_frequency = i.next();
			if (current_relative_frequency.class_id() >= Ratings.MIN.get(polarity) &&
				current_relative_frequency.class_id() <= Ratings.MAX.get(polarity)) {
				polarity_frequencies.add(current_relative_frequency);
			}
		}
		
		return polarity_frequencies;
	}
	
	private ArrayList<RelativeFrequency> filterFrequenciesByBound(ArrayList<RelativeFrequency> all_frequencies, Float freq_bound) {
		ArrayList<RelativeFrequency> boundary_frequencies = new ArrayList<RelativeFrequency>();
		
		Iterator<RelativeFrequency> i = all_frequencies.iterator();
		while (i.hasNext()) {
			RelativeFrequency current_relative_frequency = i.next();
			if (current_relative_frequency.relFrequency() > freq_bound) {
				boundary_frequencies.add(current_relative_frequency);
			}
		}
		
		return boundary_frequencies;
	}
	
/*	private int terms(int class_num)
	{
		return frequencies[class_num].frequency();
	}
	
	private int totalTerms()
	{
		return frequencies[0].frequency();
	}
	
	private int corpusLength(int class_num, ReviewStats reviewStats)
	{
		return reviewStats.getTotalLength(class_num);
	}
	
	private int totalCorpusLength(ReviewStats reviewStats)
	{
		return reviewStats.getTotalLength();
	}
*/
	private Float getRelativeFrequencySum(ArrayList<RelativeFrequency> frequencies)
	{
		float frequencies_total = 0F;
		for (int index = 0 ; index <= frequencies.size() - 1 ; index++) {
			frequencies_total += frequencies.get(index).relFrequency();
		}
		return frequencies_total;
	}
	
	private Float getRelativeFrequencyWeightedSum(ArrayList<RelativeFrequency> frequencies)
	{
		float frequencies_total = 0F;
		for (int index = 0 ; index <= frequencies.size() - 1 ; index++) {
			RelativeFrequency current_frequency = frequencies.get(index);
			frequencies_total += current_frequency.relFrequency() * current_frequency.class_id();
		}
		return frequencies_total;
	}
	
	private Subjectivity classifySubjectivity(ArrayList<RelativeFrequency> all_frequencies, Float freq_avg)
	{
		// Find minimum and maximum frequency
		Float min_freq = 0.0F, max_freq = 0.0F;
		Iterator<RelativeFrequency> i = all_frequencies.iterator();
		while (i.hasNext()) {
			Float current_freq = i.next().relFrequency();
			min_freq = current_freq < min_freq ? current_freq : min_freq;
			max_freq = current_freq > max_freq ? current_freq : max_freq;
		}
		
		// As per our subjectivity model, a term is considered to be objective when the range of the
		// absolute frequencies is lower than the average observations per class multiplied by a
		// certain tolerance factor between 0 and 1.
		float range_threshold = Globals.Parameters.ObjectivityTolerance * freq_avg;
		return max_freq - min_freq < range_threshold ? Subjectivity.OBJECTIVE : Subjectivity.SUBJECTIVE;
	}
	
	private Float calculatePolarityScore(ArrayList<RelativeFrequency> dominant_polarity_frequencies, Float freq_sum, Float freq_avg)
	{
		// As per our polarity model, the most frequent ratings, i.e. the ratings whose frequencies
		// lie above the objectivity threshold define whether a term is positive or
		// negative
		// Or alternatively, the skewness of the frequency distribution, i.e. whether the most
		// observations concentrate on the left or the right side, defines the polarity
		return getRelativeFrequencySum(dominant_polarity_frequencies) / freq_sum;
	}
	
	private Float calculateIntensityScore(ArrayList<RelativeFrequency> dominant_polarity_frequencies, Float freq_sum, Float freq_avg)
	{
		// As per our intensity model, the most frequent rating defines whether a term is positive or negative.
		// Alternatively, the  average rating of the most common C% of observations that fall within the selected polarity
		// defines the intensity
		return getRelativeFrequencyWeightedSum(dominant_polarity_frequencies) / getRelativeFrequencySum(dominant_polarity_frequencies);
	}
	
	public SentimentClassification classify(ReviewStats reviewStats)
	{
		SentimentClassification sentiment = new SentimentClassification();
		ArrayList<RelativeFrequency> relativeFrequencies = getRelativeFrequencies(reviewStats);
		Float freq_sum = getRelativeFrequencySum(relativeFrequencies);
		Float freq_avg = freq_sum / Ratings.N_RATINGS;

		HashMap<Polarity, ArrayList<RelativeFrequency>> relativeFrequenciesByPolarity = new HashMap<Polarity, ArrayList<RelativeFrequency>>();
		for (Polarity polarity : Polarity.values()) {
			relativeFrequenciesByPolarity.put(polarity, filterFrequenciesByPolarity(relativeFrequencies, polarity));
		}
		
		// Classify subjectivity
		// As per our subjectivity model, a term is considered to be objective when the range of the
		// absolute frequencies is lower than the average observations per class multiplied by a
		// certain tolerance factor between 0 and 1.
		sentiment.setSubjectivity(classifySubjectivity(relativeFrequencies, freq_avg));
		
		// Calculate polarity and intensity scores
		float freq_bound = ( 1 + Globals.Parameters.PolarityTolerance ) * freq_avg;
		for (Polarity polarity : Polarity.values()) {
			ArrayList<RelativeFrequency> dominant_frequencies = filterFrequenciesByBound(relativeFrequenciesByPolarity.get(polarity), freq_bound);
			sentiment.setPolarity(polarity, calculatePolarityScore(dominant_frequencies, freq_sum, freq_avg));
			sentiment.setIntensity(polarity, calculateIntensityScore(dominant_frequencies, freq_sum, freq_avg));
		}
		
		return sentiment;
	}
}
