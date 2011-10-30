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
package topics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import util.AppLogger;
import classes.Counter;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 * @param <Keyword>
 */
public abstract class Topic<Keyword> implements Serializable {
	
	private static final long			serialVersionUID	= -6740605497336621057L;
	
	protected HashMap<Keyword, Integer>	topKeywords;
	
	/**
	 * Constructor for class Topic
	 */
	public Topic()
	{
		topKeywords = new HashMap<Keyword, Integer>();
		unsetKeywordPrinter();
	}
	
	public Integer getWeight(Keyword keyword)
	{
		if (topKeywords.containsKey(keyword))
			return topKeywords.get(keyword);
		else
			return 0;
	}
	
	public Integer getWeightNorm(Keyword keyword)
	{
		if (topKeywords.containsKey(keyword))
			return topKeywords.get(keyword) / topicCount();
		else
			return 0;
	}

	protected HashMap<Keyword, Integer> topKeywords(int cutoff)
	{
		if (cutoff < topKeywords.size()) {
			HashMap<Keyword, Integer> keywords = new HashMap<Keyword, Integer>();
			
			ArrayList<Integer> weights = new ArrayList<Integer>(topKeywords.values());
			Collections.sort(weights);
			Collections.reverse(weights);
			int cutoff_weight;
			try {
				cutoff_weight = weights.get(cutoff - 1);
			} catch ( ArrayIndexOutOfBoundsException e ) {
				AppLogger.error.log(Level.SEVERE, "Improper cutoff value");
				cutoff_weight = 0;
			}
			
			Set<Entry<Keyword, Integer>> entries = topKeywords.entrySet();
			for (Entry<Keyword, Integer> entry : entries) {
				if (entry.getValue() >= cutoff_weight) {
					keywords.put(entry.getKey(), entry.getValue());
				}
			}
			
			return keywords;
		}
		else
			return topKeywords;
	}

	public abstract Set<Keyword> keywords();

	public abstract int size();
	
	public abstract int topicCount();

	public abstract JointTopic<Keyword> join(SingleTopic<Keyword> topic);
	
	public abstract SingleTopic<Keyword> toSingleTopic();

	public float similarity(Topic<Keyword> newTopic)
	{
		Set<Keyword> base_keywords = this.keywords();
		Set<Keyword> new_keywords = newTopic.keywords();
		Counter junction_keyword_weights = new Counter();
		Counter union_keyword_weights = new Counter();
		
		for (Keyword base_keyword : base_keywords) {
			if (new_keywords.contains(base_keyword)) {
				junction_keyword_weights.add(this.getWeight(base_keyword));
			}
			union_keyword_weights.add(this.getWeight(base_keyword));
		}
		for (Keyword new_keyword : new_keywords) {
			if (base_keywords.contains(new_keyword)) {
				junction_keyword_weights.add(newTopic.getWeight(new_keyword));
			}
			union_keyword_weights.add(this.getWeight(new_keyword));
		}
		
		int new_size = newTopic.size(), base_size = this.size();
		return (float)junction_keyword_weights.get() / (float)union_keyword_weights.get();
	}
	

	public interface KeywordPrinter<Keyword> {

		public String print(Keyword keyword);

	}
	
	protected transient KeywordPrinter<Keyword>	keywordPrinter	= null;

	public void setKeywordPrinter(KeywordPrinter<Keyword> keywordPrinter)
	{
		if (keywordPrinter != null) {
			this.keywordPrinter = keywordPrinter;
		}
	}
	
	public void unsetKeywordPrinter()
	{
		keywordPrinter = new KeywordPrinter<Keyword>() {
			
			@Override
			public String print(Keyword keyword)
			{
				return keyword.toString() + "\n";
			}
		};
	}
	
	public String toString()
	{
		if (keywordPrinter == null) {
			unsetKeywordPrinter();
		}
		return toString(Globals.TopicParameters.topKeywordsCutoff);
	}

	public String toString(Set<Keyword> blacklist)
	{
		return toString(Globals.TopicParameters.topKeywordsCutoff, blacklist);
	}

	public String toString(int cutoff)
	{
		StringBuilder str = new StringBuilder();
		
		HashMap<Keyword, Integer> topKeywords = topKeywords(cutoff);
		Set<Entry<Keyword, Integer>> keyword_entries = topKeywords.entrySet();
		Collection<Integer> weights = topKeywords.values();
		Integer max_weight = Collections.max(weights);
		Integer min_weight = Collections.min(weights);
		
		for (int weight = max_weight ; weight >= min_weight ; weight--) {
			if (weights.contains(weight)) {
				for (Entry<Keyword, Integer> keyword_entry : keyword_entries) {
					if (keyword_entry.getValue() == weight) {
						str.append(/*
									 * getWeightNorm(keyword_entry.getKey()) + "\t" +
									 */keywordPrinter.print(keyword_entry.getKey()));
					}
				}
			}
		}
		
		return str.toString();
	}
	
	public String toString(int cutoff, Set<Keyword> blacklist)
	{
		StringBuilder str = new StringBuilder();
		
		HashMap<Keyword, Integer> topKeywords = topKeywords(cutoff);
		Set<Entry<Keyword, Integer>> keyword_entries = topKeywords.entrySet();
		Collection<Integer> weights = topKeywords.values();
		Integer max_weight = Collections.max(weights);
		Integer min_weight = Collections.min(weights);
		
		for (int weight = max_weight ; weight >= min_weight ; weight--) {
			if (weights.contains(weight)) {
				for (Entry<Keyword, Integer> keyword_entry : keyword_entries) {
					if (getWeight(keyword_entry.getKey()) == weight) {
						if (!blacklist.contains(keyword_entry.getKey())) {
							str.append(/*
										 * getWeightNorm(keyword_entry.getKey()) + "\t" +
										 */keywordPrinter.print(keyword_entry.getKey()));
						}
					}
				}
			}
		}
		
		return str.toString();
	}

	private String	name	= null;
	
	
	public boolean hasName()
	{
		return name != null;
	}
	
	public String getName()
	{
		return name != null ? name : "";
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public void setNameInteractive()
	{
		BufferedReader input_reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Name: ");
			String input = input_reader.readLine().trim().toUpperCase();
			name = input.length() == 0 ? null : input;
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot read user input");
		}
	}
}