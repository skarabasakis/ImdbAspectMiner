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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import lexicon.TermSentiment.Sentiment;
import util.AppLogger;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class SimpleEnglishRuleSet extends DependencyRuleSet {
	
	protected ArrayList<DependencyRule>	ruleSet;
	protected Set<GrammaticalRelation>	clausalRelations;
	
	/**
	 * Constructor for class SimpleEnglishRuleSet
	 */
	public SimpleEnglishRuleSet()
	{
		ruleSet = new ArrayList<DependencyRule>();
	}
	
	protected SimpleEnglishRuleSet(Collection<DependencyRule> ruleSet, Collection<GrammaticalRelation> clausalRelations)
	{
		this.ruleSet = new ArrayList<DependencyRule>(ruleSet);
		this.clausalRelations = new HashSet<GrammaticalRelation>(clausalRelations);
	}

	/* (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#addRule(dependencies.rules.DependencyRule)
	 */
	@Override
	public void addRule(DependencyRule rule)
	{
		ruleSet.add(rule);
	}
	
	/* (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#applyRule(dependencies.rules.DependencyPattern, dependencies.rules.DependencyRule)
	 */
	@Override
	public Sentiment applyRule(DependencyPattern pattern, DependencyRule rule)
	{
		return rule.apply(pattern);
	}
	
	/* (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#getMatchingRule(dependencies.rules.DependencyPattern)
	 */
	@Override
	public DependencyRule getMatchingRule(DependencyPattern pattern)
	{
		for (DependencyRule rule : ruleSet) {
			if (rule.matches(pattern))
				return rule;
		}
		
		AppLogger.error.log(Level.INFO, pattern + " (No matching tule found)");
		return null;
	}
	
	/* (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#printAllRules()
	 */
	@Override
	public void printAllRules()
	{
		for (DependencyRule rule : ruleSet) {
			System.out.println(rule);
		}
	}
	

	
	/*
	 * (non-Javadoc)
	 * @see
	 * dependencies.rules.DependencyRuleSet#isClausalRelation(edu.stanford.nlp.trees.GrammaticalRelation
	 * )
	 */
	@Override
	public boolean isClausalRelation(GrammaticalRelation reln)
	{
		return clausalRelations.contains(reln);
	}

}
