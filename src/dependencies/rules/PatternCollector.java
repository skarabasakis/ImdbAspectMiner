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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.Ratings;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import util.AppLogger;
import classes.Counter;
import config.Paths;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class PatternCollector {
	
	public class SentimentPair implements Serializable, Comparable {
		
		private static final long	serialVersionUID	= 2621024557373156316L;
		public Sentiment	gov;
		public Sentiment	dep;
		
		
		/**
		 * Constructor for class PatternCollector.SentimentPair
		 */
		public SentimentPair(Sentiment gov, Sentiment dep)
		{
			gov = gov;
			dep = dep;
		}

		@Override
		public int compareTo(Object o)
		{
			return ((SentimentPair)o).pairid() - pairid();
			
		}
		
		@Override
		public boolean equals(Object obj)
		{
			return pairid() == ((SentimentPair)obj).pairid();
		}

		private int pairid() {
			return gov.toNumber(SentimentFormat.RATING) * Ratings.N_RATINGS + dep.toNumber(SentimentFormat.RATING);
		}

		@Override
		public String toString()
		{
			return "(" + gov + "," + dep + ")";
		}
	}

	private TreeMap<GrammaticalRelation, TreeMap<SentimentPair, Counter>>	patternCounters;
	
	/**
	 * Constructor for class PatternCollector
	 */
	public PatternCollector()
	{
		patternCounters = new TreeMap<GrammaticalRelation, TreeMap<SentimentPair, Counter>>();
	}
	
	public void addPattern(DependencyPattern pattern)
	{
		TreeMap<SentimentPair, Counter> reln_map = patternCounters.get(pattern.getReln());
		if (reln_map == null) {
			patternCounters.put(pattern.getReln(), new TreeMap<SentimentPair, Counter>());
			reln_map = patternCounters.get(pattern.getReln());
		}
		
		SentimentPair pair = new SentimentPair(pattern.getGov().getSentiment(), pattern.getDep().getSentiment());
		Counter pair_counter = reln_map.get(pair);
		if (pair_counter == null) {
			reln_map.put(pair, new Counter());
			pair_counter = reln_map.get(pair);
		}
		
		pair_counter.increment();
	}
	
	public void exportPatterns()
	{
		
		try {
			BufferedWriter patternfile = new BufferedWriter(new FileWriter(Paths.ruleSetFilesPath + "patterns.csv", false));
			
			Set<Entry<GrammaticalRelation, TreeMap<SentimentPair, Counter>>> relationmaps = patternCounters.entrySet();
			for (Entry<GrammaticalRelation, TreeMap<SentimentPair, Counter>> relationmap : relationmaps) {
				String relation_str = relationmap.getKey().getShortName();
				Set<Entry<SentimentPair, Counter>> relationmap_entries = relationmap.getValue().entrySet();
				for (Entry<SentimentPair, Counter> relationmap_entry : relationmap_entries) {
					String gov_str = relationmap_entry.getKey().gov.toString(SentimentFormat.SIGNED_INTEGER, false);
					String dep_str = relationmap_entry.getKey().dep.toString(SentimentFormat.SIGNED_INTEGER, false);
					patternfile.append(relation_str + "," + gov_str + "," + dep_str + "," + relationmap_entry.getValue());
					patternfile.newLine();
				}
			}
			
			patternfile.close();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot open pattern file at location " + Paths.ruleSetFilesPath + "patterns.csv");
		}
	}
}
