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

import indexing.Token;
import lexicon.TermSentiment.Sentiment;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import dependencies.TypedDependencyWrapper;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyPattern {
	
	public class Node {
		
		private SynsetCategory	category;
		private Sentiment		sentiment;
		private String			word	= null;
		
		
		/**
		 * Constructor for class DependencyPattern.Node
		 */
		public Node(SynsetCategory category, Sentiment sentiment, String word)
		{
			this.category = category;
			this.sentiment = sentiment;
			this.word = word;
		}
		
		
		/**
		 * @return the category
		 */
		public SynsetCategory getCategory()
		{
			return category;
		}
		
		
		/**
		 * @return the sentiment
		 */
		public Sentiment getSentiment()
		{
			return sentiment;
		}

		public boolean hasSentiment()
		{
			return category != SynsetCategory.NONE;
		}


		/**
		 * @return the word
		 */
		public String getWord()
		{
			return word;
		}
	}
	
	protected Node					gov;
	protected Node					dep;
	protected GrammaticalRelation	reln;
	
	
	/**
	 * Constructor for class DependencyPattern
	 */
	public DependencyPattern(Token gov, Token dep, Sentiment govs, Sentiment deps, TypedDependencyWrapper reln)
	{
		this.gov = new Node(Synset.convertPosCategory(gov.payload.getPosCat()), govs, gov.word());
		this.dep = new Node(Synset.convertPosCategory(dep.payload.getPosCat()), deps, dep.word());
		this.reln = reln.getRelation();
	}
	
	/**
	 * Constructor for class DependencyPattern
	 * 
	 * @param pattern
	 */
	protected DependencyPattern(DependencyPattern pattern)
	{
		gov = pattern.getGov();
		dep = pattern.getDep();
		reln = pattern.getReln();
	}
	
	/**
	 * @return the gov
	 */
	public Node getGov()
	{
		return gov;
	}
	
	/**
	 * @return the dep
	 */
	public Node getDep()
	{
		return dep;
	}
	
	/**
	 * @return the reln
	 */
	public GrammaticalRelation getReln()
	{
		return reln;
	}

}
