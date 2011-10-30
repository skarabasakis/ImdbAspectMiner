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

import lexicon.TermSentiment.Intensity;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import config.Globals;


/**
 * Provides a method for checking whether a {@link TermSentiment} object matches a set of specified
 * polarity criteria.
 * 
 * @author Stelios Karabasakis
 */
public class TermSentimentFilter {
	
	private Polarity	polarity;
	private boolean		strict;
	private boolean		primary;
	private Integer		minObservations;
	private Float		score;
	private Float		subjectivity;
	private Intensity	intensity;
	
	/**
	 * Constructor for class TermSentiment.SentimentFilter
	 */
	public TermSentimentFilter()
	{
		reset();
	}
	
	public TermSentimentFilter(Polarity polarity, boolean strict, boolean exclusive, Integer minObservations,
			Float score, Float subjectivity, Intensity intensity)
	{
		this.polarity = polarity;
		this.strict = strict;
		primary = exclusive;
		this.minObservations = minObservations;
		this.score = score;
		this.subjectivity = subjectivity;
		setIntensity(intensity);
	}
	
	
	public TermSentimentFilter set(Polarity polarity, boolean primary, boolean strict, Integer minObservations,
			Float score, Float subjectivity, Intensity intensity)
	{
		this.polarity = polarity;
		this.strict = strict;
		this.primary = primary;
		this.minObservations = minObservations;
		this.score = score;
		this.subjectivity = subjectivity;
		setIntensity(intensity);
		return this;
	}
	
	public TermSentimentFilter reset()
	{
		polarity = null;
		strict = false;
		primary = false;
		minObservations = 1;
		score = 0.0f;
		subjectivity = 0.0f;
		setIntensity(null);
		return this;
	}
	
	/**
	 * @return the polarity
	 */
	public Polarity getPolarity()
	{
		return polarity;
	}
	
	
	/**
	 * @param polarity
	 *            the polarity to set
	 */
	public TermSentimentFilter setPolarity(Polarity polarity, boolean primary, boolean strict)
	{
		this.polarity = polarity;
		this.strict = strict;
		this.primary = primary;
		return this;
	}
	
	public TermSentimentFilter resetPolarity()
	{
		polarity = null;
		strict = false;
		primary = false;
		return this;
	}
	
	/**
	 * @return the intensity
	 */
	public Intensity getIntensity()
	{
		return intensity;
	}
	
	/**
	 * @param intensity
	 *            the intensity to set
	 */
	public void setIntensity(Intensity intensity)
	{
		this.intensity = intensity;
	}
	
	public void resetIntensity()
	{
		intensity = null;
	}


	/**
	 * @return the strict
	 */
	public boolean isStrict()
	{
		return strict;
	}
	
	
	/**
	 * @param strict
	 *            the strict to set
	 */
	public TermSentimentFilter setStrict()
	{
		strict = true;
		return this;
	}
	
	public TermSentimentFilter resetStrict()
	{
		strict = false;
		return this;
	}
	
	
	/**
	 * @return the minObservations
	 */
	public Integer getMinObservations()
	{
		return minObservations;
	}
	
	
	/**
	 * @param minObservations
	 *            the minObservations to set
	 */
	public TermSentimentFilter setMinObservations(Integer minObservations)
	{
		this.minObservations = minObservations;
		return this;
	}
	
	public TermSentimentFilter resetMinObservations()
	{
		minObservations = 1;
		return this;
	}
	
	
	/**
	 * @return the score
	 */
	public Float getScore()
	{
		return score;
	}
	
	
	/**
	 * @param score
	 *            the score to set
	 */
	public TermSentimentFilter setScore(Float score)
	{
		this.score = score;
		return this;
	}
	
	public TermSentimentFilter resetScore()
	{
		score = 0.0f;
		return this;
	}
	
	
	/**
	 * @return the subjectivity
	 */
	public Float getSubjectivity()
	{
		return subjectivity;
	}
	
	
	/**
	 * @param subjectivity
	 *            the subjectivity to set
	 */
	public TermSentimentFilter setSubjectivity(Float subjectivity)
	{
		this.subjectivity = subjectivity;
		return this;
	}
	
	public TermSentimentFilter resetSubjectivity()
	{
		subjectivity = 0.0f;
		return this;
	}
	
	private static boolean checkScore(Sentiment sentiment)
	{
		return sentiment.getScore() > Globals.LexiconParameters.polarityScoreThreshold;
	}
	
	private boolean testPolarity(TermSentiment sentiment, boolean primary, boolean strict)
	{
		int sentiment_ranks = Math.min(sentiment.getSentimentRanks(score),
										Globals.SentimentParameters.UsableSentimentsPerTerm);
		if (sentiment_ranks == 0)
			return false;
		else {
			if (primary && (!checkScore(sentiment.getSentiment()) //
				|| sentiment.getSentiment().getPolarity() != polarity))
				return false;
			
			if (strict) {
				for (int rank = 1 ; rank <= sentiment_ranks ; rank++) {
					Sentiment s = sentiment.getSentiment(rank);
					if (checkScore(s)) {
						if (polarity != s.getPolarity())
							return false;
					}
				}
				
				return true;
			}
			else {
				for (int rank = 1 ; rank <= sentiment_ranks ; rank++) {
					Sentiment s = sentiment.getSentiment(rank);
					if (checkScore(s)) {
						if (polarity == s.getPolarity())
							return true;
					}
				}
				
				return false;
			}
		}
	}
	
	
	private boolean testMinObservations(TermSentiment sentiment)
	{
		Integer obs = sentiment.getObservations();
		return obs == null || obs == 0 ? true : obs >= minObservations;
	}
	
	private boolean testScore(TermSentiment sentiment)
	{
		if (sentiment.getSentiment() != null)
			return sentiment.getSentiment().getScore() >= score;
		else
			return false;
	}
	
	private boolean testSubjectivity(TermSentiment sentiment)
	{
		return sentiment.getSubjectivityScore() >= subjectivity;
	}
	
	private boolean checkIntensity(Intensity intensity)
	{
		return Math.abs(this.intensity.ordinal() - intensity.ordinal()) <= 1;
	}
	
	private boolean testIntensity(TermSentiment sentiment, boolean primary)
	{
		int sentiment_ranks = Math.min(sentiment.getSentimentRanks(score),
										Globals.SentimentParameters.UsableSentimentsPerTerm);
		if (sentiment_ranks == 0)
			return false;
		else {
			if (polarity == null)
				return checkIntensity(sentiment.getSentiment().getIntensity());
			else {
				if (primary) {
					Sentiment s = sentiment.getSentiment();
					if (s.getPolarity() == polarity)
						return checkIntensity(s.getIntensity());
					else
						return false;
				}
				else {
					for (int rank = 1 ; rank <= sentiment_ranks ; rank++) {
						Sentiment s = sentiment.getSentiment(rank);
						if (s.getPolarity() == polarity)
							return checkIntensity(s.getIntensity());
					}
					
					return false;
				}
			}
		}
	}


	/**
	 * Returns true if the predominant {@link Polarity} of a given {@link TermSentiment} object --
	 * or the <i>only</i> {@code Polarity}, if the {@code SentimentFilter}'s {@code strict} flag is
	 * set to <b>true</b> -- matches that of the {@code SentimentFilter}.
	 * 
	 * @param sentiment
	 *            The sentiment to check
	 * @return True if the argument passes the filter, false otherwise
	 */
	public boolean accepts(TermSentiment sentiment)
	{
		return (polarity == null || testPolarity(sentiment, primary, strict)) //
			&& (minObservations == 1 || testMinObservations(sentiment)) //
			&& (score == 0.0f || testScore(sentiment)) //
			&& (subjectivity == 0.0f || testSubjectivity(sentiment)) //
			&& (intensity == null || testIntensity(sentiment, primary));
	}
}
