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
package dependencies.rules;

import lexicon.Ratings;
import lexicon.TermSentiment;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;

public class SentimentInterval {
	
	private boolean		hasSentiment;
	private Sentiment	minSentiment;
	private Sentiment	maxSentiment;
	
	/**
	 * Constructor for class DependencyRule.SentimentInterval
	 */
	public SentimentInterval()
	{
		hasSentiment = false;
		Sentiment s = TermSentiment.NEUTRAL_SENTIMENT.getSentiment();
		setInterval(s, s);
	}

	public SentimentInterval(Sentiment min, Sentiment max)
	{
		hasSentiment = true;
		setInterval(min, max);
	}
	
	public void setInterval(Sentiment min, Sentiment max) throws IllegalArgumentException
	{
		if (min.compareTo(max) > 0)
			throw new IllegalArgumentException(
					"The sentiment on the right side of the interval should be greater than the sentiment on the left");
		else {
			minSentiment = min;
			maxSentiment = max;
		}
	}

	public static int compareSentiments(Sentiment s1, Sentiment s2)
	{
		Integer r1 = s1.toNumber(SentimentFormat.RATING);
		Integer r2 = s2.toNumber(SentimentFormat.RATING);
		return r1.compareTo(r2);
	}
	
	public boolean contains(Sentiment s)
	{
		return compareSentiments(minSentiment, s) <= 0 && compareSentiments(s, maxSentiment) <= 0;
	}
	
	// toString
	
	public static final SentimentInterval	NONE		= new SentimentInterval();
	public static final SentimentInterval	ANY			= new SentimentInterval(new Sentiment(Ratings.MIN_RATING),
																new Sentiment(Ratings.MAX_RATING));
	public static final SentimentInterval	POSITIVE	= new SentimentInterval(new Sentiment(Ratings.MIN
															.get(Polarity.POSITIVE)), new Sentiment(Ratings.MAX
															.get(Polarity.POSITIVE)));
	public static final SentimentInterval	NEGATIVE	= new SentimentInterval(new Sentiment(Ratings.MIN
															.get(Polarity.NEGATIVE)), new Sentiment(Ratings.MAX
															.get(Polarity.NEGATIVE)));
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return "[" + (hasSentiment ? minSentiment + "-" + maxSentiment : "") + "]";
	}
}