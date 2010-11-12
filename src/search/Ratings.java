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

import java.util.HashMap;
import search.SentimentClassification.Polarity;
import config.Globals;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Ratings {
	
	public static final Integer					N_RATINGS	= Globals.Corpus.N_SENTIMENT_CLASSES;
	public static final Integer					MIN_RATING	= 1;
	public static final Integer					MAX_RATING	= N_RATINGS;

	public static HashMap<Polarity, Integer>	MIN			= new HashMap<Polarity, Integer>();
	public static HashMap<Polarity, Integer>	MAX			= new HashMap<Polarity, Integer>();
	public static boolean						initialized	= setup();

	/**
	 * Constructor for class Ratings
	 */
	private static boolean setup()
	{
		MIN.put(Polarity.NEGATIVE, MIN_RATING);
		MAX.put(Polarity.NEGATIVE, N_RATINGS / 2);
		MIN.put(Polarity.POSITIVE, N_RATINGS / 2 + 1);
		MAX.put(Polarity.POSITIVE, MAX_RATING);
		
		return true;
	}
	
	public static int capacity()
	{
		return MAX_RATING + 1;
	}
}
