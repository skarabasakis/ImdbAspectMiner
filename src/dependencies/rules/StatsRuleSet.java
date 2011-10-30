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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import lexicon.TermSentiment.Sentiment;
import classes.Counter;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class StatsRuleSet extends SimpleEnglishRuleSet {
	
	private ArrayList<Counter>	ruleCounters;

	/**
	 * Constructor for class StatsRuleSet
	 */
	public StatsRuleSet()
	{
		super();
		ruleCounters = new ArrayList<Counter>();
	}
	
	/**
	 * Constructor for class StatsRuleSet
	 * @param ruleSet
	 */
	public StatsRuleSet(Collection<DependencyRule> ruleSet, Collection<GrammaticalRelation> clausalRelations)
	{
		super(ruleSet, clausalRelations);
		resetCounts();
	}
	
	/*
	 * (non-Javadoc)
	 * @see dependencies.rules.SimpleEnglishRuleSet#addRule(dependencies.rules.DependencyRule)
	 */
	@Override
	public void addRule(DependencyRule rule)
	{
		super.addRule(rule);
		ruleCounters.add(new Counter());
	}

	public LinkedHashMap<DependencyRule, Integer> getCounts()
	{
		int size = ruleSet.size();
		HashMap<Integer, ArrayList<DependencyRule>> ruleCountMap = new HashMap<Integer, ArrayList<DependencyRule>>();
		for (int i = 0; i < size; i++) {
			Integer count = ruleCounters.get(i).get();
			DependencyRule rule = ruleSet.get(i);
			
			if (!ruleCountMap.containsKey(count)) {
				ruleCountMap.put(count, new ArrayList<DependencyRule>());
			}
			ruleCountMap.get(count).add(rule);
		}
		
		ArrayList<Integer> countlist = new ArrayList<Integer>(ruleCountMap.keySet());
		Collections.sort(countlist);
		Collections.reverse(countlist);
		
		LinkedHashMap<DependencyRule, Integer> sortedRules = new LinkedHashMap<DependencyRule, Integer>();
		for (Integer count : countlist) {
			for (DependencyRule rule : ruleCountMap.get(count)) {
				sortedRules.put(rule, count);
			}
		}
		
		return sortedRules;
	}
	
	public void resetCounts()
	{
		ruleCounters = new ArrayList<Counter>(ruleSet.size());
		for (DependencyRule rule : ruleSet) {
			ruleCounters.add(new Counter());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see dependencies.rules.SimpleEnglishRuleSet#applyRule(dependencies.rules.DependencyPattern,
	 * dependencies.rules.DependencyRule)
	 */
	@Override
	public Sentiment applyRule(DependencyPattern pattern, DependencyRule rule)
	{
		ruleCounters.get(ruleSet.indexOf(rule)).increment();
		return super.applyRule(pattern, rule);
	}

}
