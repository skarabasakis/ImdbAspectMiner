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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import util.AppLogger;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class TermSentiment implements Serializable {
	
	
	private static final long	serialVersionUID	= 2487302335516427866L;

	// public static enum Subjectivity {
	// OBJECTIVE, SUBJECTIVE
	// }
	
	public static enum Polarity {
		NEGATIVE, POSITIVE
	}
	
	public static enum Intensity {
		WEAKEST, WEAK, NORMAL, STRONG, STRONGEST
	}

	public static class Sentiment implements Serializable, Comparable<Sentiment> {

		private static final long	serialVersionUID	= -4388191972135431927L;
		
		private Float				score;
		private Polarity			polarity;
		private Intensity			intensity;
		

		/**
		 * Constructor for class TermSentiment.Sentiment
		 */
		public Sentiment()
		{
			score = 0.0f;
			polarity = Polarity.POSITIVE;
			intensity = Intensity.NORMAL;
		}

		/**
		 * Constructor for class TermSentiment.Sentiment
		 */
		public Sentiment(Float score, Polarity polarity)
		{
			this.score = score;
			this.polarity = polarity;
			intensity = Intensity.NORMAL;
		}
		
		public Sentiment(Float score, Polarity polarity, Intensity intensity)
		{
			this.score = score;
			this.polarity = polarity;
			this.intensity = intensity;
		}
		
		public Sentiment(Float score, Float rating)
		{
			this.score = score;
			int rounded_rating = Math.round(rating);
			try {
				polarity = ratingToPolarity(rounded_rating);
				intensity = ratingToIntensity(polarity, rounded_rating);
			} catch ( IndexOutOfBoundsException e ) {
				AppLogger.error.log(Level.SEVERE, e.getMessage());
			}
		}
		
		public Sentiment(Integer rating)
		{
			score = 0.0f;
			try {
				polarity = ratingToPolarity(rating);
				intensity = ratingToIntensity(polarity, rating);
			} catch ( IndexOutOfBoundsException e ) {
				AppLogger.error.log(Level.SEVERE, e.getMessage());
			}
		}

		private Polarity ratingToPolarity(int rating) throws IndexOutOfBoundsException
		{
			Polarity[] polarities = Polarity.values();
			for (Polarity polarity : polarities) {
				if (rating >= Ratings.MIN.get(polarity) && rating <= Ratings.MAX.get(polarity))
					return polarity;
			}
			throw new IndexOutOfBoundsException(
					"Cannot map rating to polarity. Given rating falls outside of specified rating scale ("
						+ Ratings.MIN_RATING + "-" + Ratings.MAX_RATING + ")");
		}
		
		private Intensity ratingToIntensity(Polarity polarity, int rating)
		{
			int intensity_level = -1;
			switch (polarity) {
				case POSITIVE:
					intensity_level = rating - Ratings.MIN.get(Polarity.POSITIVE);
					break;
				case NEGATIVE:
					intensity_level = Ratings.MAX.get(Polarity.NEGATIVE) - Math.round(rating);
					break;
			}
			return Intensity.values()[intensity_level];
		}

		/**
		 * @return the score
		 */
		public Float getScore()
		{
			return score;
		}
		
		
		/**
		 * @return the polarity
		 */
		public Polarity getPolarity()
		{
			return polarity;
		}
		
		
		/**
		 * @return the intensity
		 */
		public Intensity getIntensity()
		{
			return intensity;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Sentiment other)
		{
			return (int)((other.getScore() - score) * 1000);
		}
		
		public enum SentimentFormat {
			SIGNED_INTEGER, SIGNED_INTEGER_WZERO, RATING, RATING_WZERO, TEXT, INDEX_POINTER
		}
		
		private SentimentFormat	defaultSentimentFormat	= SentimentFormat.RATING;

		private int toRating(Polarity polarity, Intensity intensity)
		{
			switch (polarity) {
				case POSITIVE:
					return Ratings.MIN.get(Polarity.POSITIVE) + intensity.ordinal();
				case NEGATIVE:
					return Ratings.MAX.get(Polarity.NEGATIVE) - intensity.ordinal();
			}

			return 0;
		}

		public String toString()
		{
			return toString(defaultSentimentFormat, false);
		}
		
		
		public String toString(SentimentFormat format, boolean print_score)
		{
			StringBuilder str = new StringBuilder();
			if (print_score) {
				str.append(scoreFormat.format(score)).append(" ");
			}

			switch (format) {
				case SIGNED_INTEGER:
					str.append((polarity == Polarity.POSITIVE ? "+" : "-") + (intensity.ordinal() + 1));
					break;
				case SIGNED_INTEGER_WZERO:
					str.append(isNeutral() ? "0" : (polarity == Polarity.POSITIVE ? "+" : "-")
						+ (intensity.ordinal() + 1));
					return str.toString();
				case RATING:
					str.append(toRating(polarity, intensity));
					break;
				case RATING_WZERO:
					str.append(isNeutral() ? "0" : toRating(polarity, intensity));
					return str.toString();
				case TEXT:
					if (!isNeutral()) {
						str.append(intensity).append(" ").append(polarity);
					}
					else {
						str.append("NEUTRAL");
					}
					return str.toString();
				case INDEX_POINTER:
					str.append(Ratings.MIN.get(polarity) + intensity.ordinal() - Ratings.MIN_RATING);
					break;
			}
			
			return isNeutral() ? "-" : str.toString();
		}
		
		public Integer toNumber(SentimentFormat format)
		{
			Integer sentiment = toRating(polarity, intensity);
			switch (format) {
				case SIGNED_INTEGER:
					return (polarity == Polarity.POSITIVE ? 1 : -1) * (intensity.ordinal() + 1);
				case SIGNED_INTEGER_WZERO:
					return isNeutral() ? 0 : (polarity == Polarity.POSITIVE ? 1 : -1) * (intensity.ordinal() + 1);
				case RATING:
					return sentiment;
				case RATING_WZERO:
					return isNeutral() ? 0 : sentiment;
				case INDEX_POINTER:
					return sentiment - Ratings.MIN_RATING;
				default:
					return sentiment - Ratings.MIN.get(Polarity.POSITIVE);
			}
		}

		public boolean isNeutral()
		{
			return score == 0.0f;
		}

		public boolean equals(Sentiment s)
		{
			return s != null //
				&& (isNeutral() && s.isNeutral() //
				|| getPolarity() == s.getPolarity() && getIntensity() == s.getIntensity());
		}
		
		private static NumberFormat	floatFormat	= getFloatFormatInstance();
		
		private static NumberFormat getFloatFormatInstance()
		{
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(4);
			nf.setMinimumFractionDigits(1);
			return nf;
		}

		public static ArrayList<Sentiment> removeNeutral(ArrayList<Sentiment> sentiments)
		{
			Iterator<Sentiment> sentiments_iter = sentiments.iterator();
			while ( sentiments_iter.hasNext() ) {
				if (sentiments_iter.next().isNeutral()) {
					sentiments_iter.remove();
				}
			}
			
			return sentiments;
		}
		
		@SuppressWarnings("unchecked")
		public static ArrayList<Sentiment> filterPolarity(ArrayList<Sentiment> sentiments, Polarity polarity)
		{
			ArrayList<Sentiment> sentiments_pol = new ArrayList<Sentiment>(sentiments);
			Iterator<Sentiment> sentiments_iter = sentiments_pol.iterator();
			while ( sentiments_iter.hasNext() ) {
				Sentiment sent = sentiments_iter.next();
				if (sent.isNeutral() || sent.getPolarity() != polarity) {
					sentiments_iter.remove();
				}
			}
			
			return sentiments_pol;
		}

		public static Double mean(ArrayList<Sentiment> sentiments)
		{
			int rating_sum = 0;
			for (Sentiment sentiment : sentiments) {
				rating_sum += sentiment.toNumber(SentimentFormat.RATING_WZERO);
			}
			return 1.0 * rating_sum / sentiments.size();
		}
		
		private static Double stdev(ArrayList<Sentiment> sentiments, Double mean)
		{
			float meansquare_sum = 0.0f;
			for (Sentiment sentiment : sentiments) {
				double meandist = sentiment.toNumber(SentimentFormat.RATING_WZERO) - mean;
				meansquare_sum += meandist * meandist;
			}
			
			return Math.sqrt(meansquare_sum / sentiments.size());
		}
		
		public static Double stdev(ArrayList<Sentiment> sentiments)
		{
			return stdev(sentiments, mean(sentiments));
		}


		public static String printSummary(ArrayList<Sentiment> sentiments)
		{
			Double mean = mean(sentiments);
			Double stdev = stdev(sentiments, mean);
			
			StringBuilder summary = new StringBuilder();
			
			summary //
				.append(sentiments.size()).append(sentiments.size() == 1 ? " sample" : " samples").append('\t') //
				.append(stdevText(stdev)).append(' ').append(meanText(new Float(mean))).append('\t') //
				.append("[ m: " + floatFormat.format(mean) + "  s: " + floatFormat.format(stdev) + " ]").append('\t');
			
			return summary.toString();
		}
		
		/**
		 * @param (float)mean
		 * @return
		 */
		private static String meanText(Float mean)
		{
			return new Sentiment(1.0f, mean).toString(SentimentFormat.TEXT, false);
		}

		private enum Variability {
			UNANIMOUSLY, GENERALLY, MOSTLY, MIXED_MOSTLY;
			
			public String toString()
			{
				return name().replaceFirst("_", ", ");
			}
		}

		private static Variability[]	variability	= Variability.values();
		
		private static String stdevText(Double stdev)
		{
			int stdev_ratio = (int)Math.floor(variability.length * stdev / Ratings.STDEV);
			return variability[stdev_ratio].toString();
		}

		/**
		 * @param value
		 * @return
		 */
		public static Sentiment average(ArrayList<Sentiment> sentiments)
		{
			Float scoresum = 0.0f;
			sentiments = removeNeutral(sentiments);
			for (Sentiment sentiment : removeNeutral(sentiments)) {
				scoresum += sentiment.getScore();
			}
			return new Sentiment(scoresum / sentiments.size(), new Float(mean(sentiments)));
		}

	}
	
	private Float					subjectivityScore	= -1.0F;
	private ArrayList<Sentiment>	sentiment;
	
	/**
	 * Constructor for class Parameters
	 */
	public TermSentiment()
	{
		sentiment = new ArrayList<Sentiment>();
	}
	
	public TermSentiment(Sentiment... sentiment)
	{
		this.sentiment = new ArrayList<Sentiment>();
		for (Sentiment smnt : sentiment) {
			this.sentiment.add(smnt);
		}
	}

	/**
	 * Constructor for class TermSentiment
	 * 
	 * @param termSentiment
	 */
	public TermSentiment(TermSentiment termSentiment)
	{
		n_of_documents = termSentiment.n_of_documents;
		n_of_observations = termSentiment.n_of_observations;
		subjectivityScore = termSentiment.subjectivityScore;
		sentiment = new ArrayList<Sentiment>(termSentiment.sentiment);
	}
	
	public TermSentiment(TermSentiment termSentiment, Float score_thr)
	{
		n_of_documents = termSentiment.n_of_documents;
		n_of_observations = termSentiment.n_of_observations;
		subjectivityScore = termSentiment.subjectivityScore;
		sentiment = new ArrayList<Sentiment>(termSentiment.sentiment.subList(0, termSentiment
			.getSentimentRanks(score_thr)));
	}

	/**
	 * @return the subjectivity
	 */
	public Float getSubjectivityScore()
	{
		return subjectivityScore;
	}
	
	
	public void setSubjectivityScore(Float score)
	{
		subjectivityScore = score;
	}
	
	public int getSentimentRanks()
	{
		return sentiment.size();
	}
	
	public int getSentimentRanks(float score_thr)
	{
		int totalranks = getSentimentRanks();
		
		if (score_thr > 0) {
			for (int rank = 1 ; rank <= totalranks ; rank++) {
				if (getSentiment(rank).getScore() < score_thr)
					return rank - 1;
			}
		}
		
		return totalranks;
	}

	public Sentiment getSentiment()
	{
		return getSentiment(1);
	}
	
	public Sentiment getSentiment(int rank)
	{
		try {
			return sentiment.get(rank - 1);
		} catch ( IndexOutOfBoundsException e ) {
			return null;
		}
	}
	
	
	public int addSentiment(Sentiment sentiment)
	{
		int size = this.sentiment.size();
		this.sentiment.ensureCapacity(size + 1);
		for (int rank = 0 ; rank < size ; rank++) {
			if (sentiment.compareTo(this.sentiment.get(rank)) < 0) {
				this.sentiment.add(rank, sentiment);
				return rank + 1;
			}
		}
		
		// TermSentiment list is empty or new sentiment ranks last: Insert at the end of the
		// sentiment
		// list
		this.sentiment.add(sentiment);
		return size + 1;
	}
	
	public void fixSentimentOrder()
	{
		Collections.reverse(sentiment);
	}


	private static NumberFormat	scoreFormat	= getScoreFormatInstance();
	
	private static NumberFormat getScoreFormatInstance()
	{
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(0);
		return nf;
	}

	@Override
	public String toString()
	{
		String str = scoreFormat.format(subjectivityScore) + "\t";
		int total_ranks = getSentimentRanks();
		for (int rank = 1 ; rank <= total_ranks ; rank++) {
			str += (rank == 1 ? "" : " | ") + getSentiment(rank);
		}

		return str;
	}
	
	private Integer	n_of_documents;
	private Integer	n_of_observations;
	
	/**
	 * @param n_of_documents
	 *            the n_of_documents to set
	 */
	public void setDocuments(Integer n_of_documents)
	{
		this.n_of_documents = n_of_documents;
	}


	/**
	 * @return the n_of_documents
	 */
	public Integer getDocuments()
	{
		return n_of_documents;
	}
	
	
	/**
	 * @param n_of_observations
	 *            the n_of_observations to set
	 */
	public void setObservations(Integer n_of_observations)
	{
		this.n_of_observations = n_of_observations;
	}
	
	
	/**
	 * @return the n_of_observations
	 */
	public Integer getObservations()
	{
		return n_of_observations;
	}
	
	public static final TermSentiment	NEUTRAL_SENTIMENT	= new TermSentiment(new Sentiment());

}
