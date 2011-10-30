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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import classes.Counter;
import classes.ReviewStats;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class RatingHistogram implements Serializable {
	
	private static final long							serialVersionUID	= -8045655706117881329L;
	
	private Counter										uniqueDocuments;
	// private ArrayList<RelativeFrequency> frequencies;
	private LinkedHashMap<Integer, RelativeFrequency>	frequencies;
	private LinkedList<RelativeFrequency>				frequencyRanking	= null;
	private boolean										isRanked			= false;
	
	/* Results Cache */
	// private boolean cacheIsValid = false;
	// private ArrayList<Integer> rankedRatings = null;

	/**
	 * Constructor for class RatingHistogram
	 */
	public RatingHistogram(ReviewStats reviewStats)
	{
		uniqueDocuments = new Counter();

		ArrayList<Float> classWeights = reviewStats.getRatingWeights();
		frequencies = new LinkedHashMap<Integer, RelativeFrequency>(Ratings.capacity());
		for (int class_id = Ratings.MIN_RATING ; class_id <= Ratings.MAX_RATING ; class_id++) {
			frequencies.put(class_id, new RelativeFrequency(class_id, classWeights.get(class_id - 1)));
		}
	}

	public void incrementUniqueDocuments()
	{
		uniqueDocuments.increment();
	}
	
	/**
	 * @return the uniqueDocuments
	 */
	public int getUniqueDocuments()
	{
		return uniqueDocuments.get();
	}

	public void addObeservation(int class_num, int weight)
	{
		frequencies.get(class_num).addObservation(weight);
		isRanked = false;
	}
	
	public int getTotalObservations()
	{
		Integer total_frequency = 0;
		for (int i = Ratings.MIN_RATING ; i <= Ratings.MAX_RATING ; i++) {
			total_frequency += frequencies.get(i).frequency();
		}
		return total_frequency;
	}

	public void rankRatingsByFrequency()
	{
		frequencyRanking = new LinkedList<RelativeFrequency>(frequencies.values());
		Collections.sort(frequencyRanking);
		Collections.reverse(frequencyRanking);
		isRanked = true;
	}

	/**************************/
	

	public Float getFreq(int class_num)
	{
		return frequencies.get(class_num).relFrequency();
	}
	
	public Float getMinFreq()
	{
		return getFreq(getRatingByRank(Ratings.N_RATINGS));
	}
	
	public Float getMaxFreq()
	{
		return getFreq(getRatingByRank(1));
	}

	public int getRatingByRank(int rank)
	{
		if (!isRanked) {
			rankRatingsByFrequency();
		}

		return frequencyRanking.get(rank - 1).class_id();
	}

	private Set<Integer> allClasses()
	{
		return frequencies.keySet();
	}

	public Float getFreqSum()
	{
		return getFreqSum(allClasses());
	}
	
	public Float getFreqSum(Set<Integer> classlist)
	{
		Float freq_sum = 0F;
		for (Integer class_id : classlist) {
			freq_sum += frequencies.get(class_id).relFrequency();
		}
		return freq_sum;
	}
	
	public Float getFreqAvg()
	{
		return getFreqAvg(allClasses());
	}
	
	public Float getFreqAvg(Set<Integer> classlist)
	{
		return getFreqSum(classlist) / classlist.size();
	}

	public Float getWeightedAvgRating(Set<Integer> classlist)
	{
		float freq_weighted_sum = 0F;
		float freq_sum = 0F;
		for (Integer class_id : classlist) {
			float class_id_freq = frequencies.get(class_id).relFrequency();
			freq_sum += class_id_freq;
			freq_weighted_sum += class_id_freq * class_id;
		}
		return freq_weighted_sum / freq_sum;
	}

	public Set<Integer> filterRatings(float freqCutoff, Set<Integer> classlist)
	{
		Iterator<Integer> iter = classlist.iterator();
		while ( iter.hasNext() ) {
			if (frequencies.get(iter.next()).relFrequency() < freqCutoff) {
				iter.remove();
			}
		}
		return classlist;
	}

	/*****************************/
	
	/*
	 * private Float classifySubjectivity(ArrayList<RelativeFrequency> all_frequencies, Float
	 * freq_avg) { // Find minimum and maximum frequency Float min_freq = 0.0F, max_freq = 0.0F;
	 * Iterator<RelativeFrequency> i = all_frequencies.iterator(); while ( i.hasNext() ) { Float
	 * current_freq = i.next().relFrequency(); min_freq = current_freq < min_freq ? current_freq :
	 * min_freq; max_freq = current_freq > max_freq ? current_freq : max_freq; } // As per our
	 * subjectivity model, a term is considered to be objective when the range of the // absolute
	 * frequencies is lower than the average observations per class multiplied by a // certain
	 * tolerance factor between 0 and 1. float range_threshold =
	 * Globals.Parameters.ObjectivityTolerance * freq_avg; // return max_freq - min_freq <
	 * range_threshold // ?Subjectivity.OBJECTIVE:Subjectivity.SUBJECTIVE; return (max_freq -
	 * min_freq) / range_threshold; } public Float getRangeThreshold(ReviewStats reviewStats) { //
	 * Find average, minimum and maximum frequency Float avg_freq = 0.0F, min_freq = 0.0F, max_freq
	 * = 0.0F; Iterator<RelativeFrequency> i = getRelativeFrequencies(reviewStats).iterator(); while
	 * ( i.hasNext() ) { Float current_freq = i.next().relFrequency(); avg_freq += current_freq;
	 * min_freq = current_freq < min_freq ? current_freq : min_freq; max_freq = current_freq >
	 * max_freq ? current_freq : max_freq; } avg_freq /= Ratings.N_RATINGS; return (max_freq -
	 * min_freq) / avg_freq; } private Float calculatePolarityScore(ArrayList<RelativeFrequency>
	 * dominant_polarity_frequencies, Float freq_sum, Float freq_avg) { // As per our polarity
	 * model, the most frequent ratings, i.e. the ratings whose frequencies // lie above the
	 * objectivity threshold define whether a term is positive or // negative // Or alternatively,
	 * the skewness of the frequency distribution, i.e. whether the most // observations concentrate
	 * on the left or the right side, defines the polarity return
	 * getRelativeFrequencySum(dominant_polarity_frequencies) / freq_sum; } private Float
	 * calculateIntensityScore(ArrayList<RelativeFrequency> dominant_polarity_frequencies, Float
	 * freq_sum, Float freq_avg) { // As per our intensity model, the most frequent rating defines
	 * whether a term is positive // or negative. // Alternatively, the average rating of the most
	 * common C% of observations that fall within // the selected polarity // defines the intensity
	 * return getRelativeFrequencyWeightedSum(dominant_polarity_frequencies) /
	 * getRelativeFrequencySum(dominant_polarity_frequencies); } public TermSentiment
	 * classify(ReviewStats reviewStats) { TermSentiment termSentiment = new TermSentiment();
	 * ArrayList<RelativeFrequency> relativeFrequencies = getRelativeFrequencies(reviewStats); Float
	 * freq_sum = getRelativeFrequencySum(relativeFrequencies); Float freq_avg = freq_sum /
	 * Ratings.N_RATINGS; HashMap<Polarity, ArrayList<RelativeFrequency>>
	 * relativeFrequenciesByPolarity = new HashMap<Polarity, ArrayList<RelativeFrequency>>(); for
	 * (Polarity polarity : Polarity.values()) { relativeFrequenciesByPolarity.put(polarity,
	 * filterFrequenciesByPolarity(relativeFrequencies, polarity)); } // Classify subjectivity // As
	 * per our subjectivity model, a term is considered to be objective when the range of the //
	 * absolute frequencies is lower than the average observations per class multiplied by a //
	 * certain tolerance factor between 0 and 1.
	 * termSentiment.setSubjectivityScore(classifySubjectivity(relativeFrequencies, freq_avg)); //
	 * Calculate polarity and intensity scores float freq_cutoff = ( 1 +
	 * Globals.Parameters.PNFrequencyCutoff ) * freq_avg; for (Polarity polarity :
	 * Polarity.values()) { ArrayList<RelativeFrequency> dominant_frequencies =
	 * filterFrequenciesByBound(relativeFrequenciesByPolarity.get(polarity), freq_cutoff);
	 * termSentiment.addSentiment(new Sentiment(calculatePolarityScore(dominant_frequencies,
	 * freq_sum, freq_avg), calculateIntensityScore(dominant_frequencies, freq_sum, freq_avg))) }
	 * return termSentiment; }
	 */
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String str = getUniqueDocuments() + " docs\n";
		for (int i = 1 ; i <= Ratings.N_RATINGS ; i++) {
			str += frequencies.get(i) + "\n";
		}
		str += "\n";
		
		return str;
	}
	
	public String prettyPrintHeader()
	{
		String header = "";
		for (int i = Ratings.MIN_RATING ; i <= Ratings.MAX_RATING ; i++) {
			header += i + "\t";
		}
		return header;
	}
	
	public String prettyPrintFreq()
	{
		String histogram_str = "";
		for (int i = Ratings.MIN_RATING ; i <= Ratings.MAX_RATING ; i++) {
			histogram_str += frequencies.get(i).frequency() + ",";
		}
		return histogram_str;
	}
	
	public String prettyPrintRelFreq(ReviewStats reviewStats)
	{
		String histogram_str = "";
		for (int i = Ratings.MIN_RATING ; i <= Ratings.MAX_RATING ; i++) {
			histogram_str += frequencies.get(i).relFrequency() + ",";
		}
		return histogram_str;
	}
}
