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
package dependencies;

import java.io.IOException;
import java.util.logging.Level;
import lexicon.RatingHistogramCollector;
import topics.TopicSet;
import util.AppLogger;
import util.State;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class LexiconResources {
	
	private static LexiconResources				lexiconResources		= null;

	private RatingHistogramCollector	lex_plain_collector		= null;
	private RatingHistogramCollector	lex_negated_collector	= null;
	private TopicSet							topics					= null;
	
	
	/**
	 * Constructor for class LexiconResources
	 */
	private LexiconResources()
	{
		try {
			lex_plain_collector = new State<RatingHistogramCollector>( //
					"lex_plain_collector", lex_plain_collector).restoreState();
			lex_negated_collector = new State<RatingHistogramCollector>( //
					"lex_negated_collector", lex_negated_collector).restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot load lexical resources for dependency parsing");
		}
	}
	

	public static LexiconResources getInstance()
	{
		if (lexiconResources == null) {
			lexiconResources = new LexiconResources();
		}
		
		return lexiconResources;
	}

}
