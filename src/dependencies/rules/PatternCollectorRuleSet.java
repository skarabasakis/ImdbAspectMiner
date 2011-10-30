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

import java.io.IOException;
import lexicon.TermSentiment.Sentiment;
import util.State;
import wordnet.Synset.SynsetCategory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class PatternCollectorRuleSet extends DependencyRuleSet {
	
	private PatternCollector	patterns;
	private static DependencyRule	dummyRule	= dummyRule();

	
	/**
	 * 
	 */
	private static DependencyRule dummyRule()
	{
		DependencyRule dummyRule = new DependencyRule();
		SynsetCategory[] synsetcats = null;
		dummyRule.setGov(synsetcats);
		dummyRule.setDep(synsetcats);
		dummyRule.setRelations(EnglishGrammaticalRelations.values());
		dummyRule.setOutcome(DependencyRuleOutcomes.UNCHANGED_GOV);
		return dummyRule;
	}
	
	/**
	 * Constructor for class PatternCollectorRuleSet
	 */
	public PatternCollectorRuleSet()
	{
		try {
			patterns = new State<PatternCollector>("patterns", patterns).restoreState();
		} catch ( IOException e ) {
			patterns = new PatternCollector();
		}
	}
	
	public PatternCollector getCollector()
	{
		return patterns;
	}
	
	public void saveCollector()
	{
		State<PatternCollector> patterns_state = new State<PatternCollector>("patterns", patterns);
		patterns_state.setObj(patterns);
		patterns_state.saveState();
	}

	
	/*
	 * (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#addRule(dependencies.rules.DependencyRule)
	 */
	@Override
	public void addRule(DependencyRule rule)
	{
		// do nothing
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#applyRule(dependencies.rules.DependencyPattern,
	 * dependencies.rules.DependencyRule)
	 */
	@Override
	public Sentiment applyRule(DependencyPattern pattern, DependencyRule rule)
	{
		patterns.addPattern(pattern);
		return pattern.getGov().getSentiment();
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see
	 * dependencies.rules.DependencyRuleSet#getMatchingRule(dependencies.rules.DependencyPattern)
	 */
	@Override
	public DependencyRule getMatchingRule(DependencyPattern pattern)
	{
		return dummyRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see dependencies.rules.DependencyRuleSet#printAllRules()
	 */
	@Override
	public void printAllRules()
	{
		// do nothing
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
		return false;
	}

}
