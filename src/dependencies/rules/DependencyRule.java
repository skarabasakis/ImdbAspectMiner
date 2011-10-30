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
package dependencies.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import lexicon.TermSentiment.Sentiment;
import util.AppLogger;
import wordnet.Synset.SynsetCategory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyRule {
	
	private List<SynsetCategory>			gov				= null;
	private List<SynsetCategory>			dep				= null;
	private List<String>					gov_wordlist	= null;
	private List<String>					dep_wordlist	= null;
	// private SentimentInterval govSentiment;
	// private SentimentInterval depSentiment;
	private HashSet<GrammaticalRelation>	reln			= new HashSet<GrammaticalRelation>(1);
	private Outcome							outcome;

	private int								index;
	
	public void setGov(SynsetCategory[] synsetcat)
	{
		if (synsetcat == null || synsetcat.length == 0) {
			gov = Arrays.asList(SynsetCategory.values());
		}
		else {
			gov = Arrays.asList(synsetcat);
		}
	}
	
	public void setGov(String[] wordlist)
	{
		if (!(wordlist == null || wordlist.length == 0)) {
			gov_wordlist = new ArrayList<String>();
			for (String word : wordlist) {
				gov_wordlist.add(word.toLowerCase().trim());
			}
		}
	}

	public void setDep(SynsetCategory[] synsetcat)
	{
		if (synsetcat == null || synsetcat.length == 0) {
			dep = Arrays.asList(SynsetCategory.values());
		}
		else {
			dep = Arrays.asList(synsetcat);
		}
	}
	
	public void setDep(String[] wordlist)
	{
		if (!(wordlist == null || wordlist.length == 0)) {
			dep_wordlist = new ArrayList<String>();
			for (String word : wordlist) {
				dep_wordlist.add(word.toLowerCase().trim());
			}
		}
	}

	public void setRelations(String[] relnames)
	{
		reln.clear();
		if (relnames == null || relnames.length == 0) {
			reln.addAll(EnglishGrammaticalRelations.values());
		}
		else {
			for (String relname : relnames) {
				GrammaticalRelation relation = EnglishGrammaticalRelations.valueOf(relname);
				if (relation != null) {
					reln.add(relation);
				}
				else {
					if (relname.isEmpty() && relnames.length == 1) {
						reln.addAll(EnglishGrammaticalRelations.values());
					}
					else {
						AppLogger.error.log(Level.WARNING, "Invalid relation \"" + relname
							+ "\" was not included in dependency rule");
					}
				}
			}
		}

	}
	
	public void setRelations(Set<GrammaticalRelation> relations)
	{
		reln.clear();
		if (relations == null || relations.isEmpty()) {
			reln.addAll(EnglishGrammaticalRelations.values());
		}
		else {
			reln.addAll(relations);
		}
	}
	
	public void setRelations(List<GrammaticalRelation> relations)
	{
		reln.clear();
		if (relations == null || relations.isEmpty()) {
			reln.addAll(EnglishGrammaticalRelations.values());
		}
		else {
			reln.addAll(relations);
		}
	}

	public void setOutcome(Outcome outcome)
	{
		this.outcome = outcome;
	}

	public boolean matches(DependencyPattern pattern)
	{
		return reln.contains(pattern.getReln()) //
			&& (gov == null || gov.contains(pattern.getGov().getCategory())) //
			&& (dep == null || dep.contains(pattern.getDep().getCategory()))
			&& (gov_wordlist == null || gov_wordlist.contains(pattern.getGov().getWord().toLowerCase().trim())) //
			&& (dep_wordlist == null || dep_wordlist.contains(pattern.getDep().getWord().toLowerCase().trim()));
	}

	public Sentiment apply(DependencyPattern pattern)
	{
		return outcome.calculate(pattern.getGov().getSentiment(), pattern.getDep().getSentiment());
	}
	
	/**
	 * Constructor for class DependencyRule
	 */
	public DependencyRule()
	{
	}
	
	/**
	 * Constructor for class DependencyRule
	 */
	public DependencyRule(String reln, Outcome outcome)
	{
		this.setRelations(reln.split("[ ,]"));
		setOutcome(outcome);
	}

	public DependencyRule(String reln, SynsetCategory[] gov, SynsetCategory[] dep, Outcome outcome)
	{
		this.setRelations(reln.split("[ ,]"));
		setGov(gov);
		setDep(dep);
		setOutcome(outcome);
	}
	
	public DependencyRule(String reln, String[] gov, String[] dep, Outcome outcome)
	{
		this.setRelations(reln.split("[ ,]"));
		setGov(gov);
		setDep(dep);
		setOutcome(outcome);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return printCatList(gov) + printWordList(gov_wordlist) + " x " + printCatList(dep)
			+ printWordList(dep_wordlist) + "\t"
			+ printRelnList() + "\t--> " + outcome.toString();
	}
	
	private String printRelnList()
	{
		if (reln == null || reln.isEmpty())
			return "/any";
		else {
			StringBuilder str = new StringBuilder();
			for (Object element : reln) {
				str.append(element + " ");
			}
			return "/" + str.toString().trim().replace(' ', '/');
		}

	}
	
	private String printCatList(List<SynsetCategory> list)
	{
		if (list == null || list.isEmpty() || list.size() == SynsetCategory.values().length)
			return "{*}";
		else {
			StringBuilder str = new StringBuilder();
			for (Object element : list) {
				str.append(element + " ");
			}
			return "{" + str.toString().trim() + "}";
		}
	}
	
	private String printWordList(List<String> list)
	{
		if (list == null || list.isEmpty())
			return "";
		else {
			StringBuilder str = new StringBuilder();
			try {
				for (String element : list.subList(0, 2)) {
					str.append(element + " ");
				}
			} catch ( IndexOutOfBoundsException e ) {
				for (String element : list) {
					str.append(element + " ");
				}
			}
			return "[" + str.toString().trim().replace(' ', ',') + "]";
		}
	}
	
	
	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(int index)
	{
		this.index = index;
	}
	
	
	/**
	 * @return the index
	 */
	public int getIndex()
	{
		return index;
	}
}
