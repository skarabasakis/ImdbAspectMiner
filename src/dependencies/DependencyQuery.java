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
package dependencies;

import indexing.PosTag;
import lexicon.TermSentiment.Sentiment;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import dependencies.rules.DependencyPattern;
import dependencies.rules.SentimentInterval;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyQuery {
	
	private class NodeQuery {
		
		private SynsetCategory		category	= SynsetCategory.NONE;
		private SentimentInterval	interval	= SentimentInterval.ANY;
		
		
		/**
		 * Constructor for class DependencyQuery.nodeQuery
		 */
		public NodeQuery()
		{
		}

		// public NodeQuery setCategory(SynsetCategory category)
		// {
		// this.category = category;
		// return this;
		// }
		//
		// public NodeQuery setSentimentInterval(int min, int max)
		// {
		// interval = new SentimentInterval(new Sentiment(min), new Sentiment(max));
		// return this;
		// }
		//
		// public NodeQuery setSentimentInterval(boolean positive)
		// {
		// interval = positive ? SentimentInterval.POSITIVE : SentimentInterval.NEGATIVE;
		// return this;
		// }

		/*
		 * Format: {*|n|v|a|r}{(*)|(+)|(-)|(x,y)}
		 */
		public NodeQuery(String query) throws IllegalArgumentException
		{
			query = query.split("\\)")[0];
			int divider_index = query.indexOf('(');
			switch (divider_index) {
				case -1:
					category = Synset.convertPosCategory(PosTag.toCategory(query.substring(0, 1)));
					break;
				case 0:
					interval = convertToInterval(query.substring(1));
					break;
				default:
					category = Synset.convertPosCategory(PosTag.toCategory(query.substring(0, 1)));
					interval = convertToInterval(query.split("\\(")[1]);
					break;
			}
		}
		
		private SentimentInterval convertToInterval(String intervalStr) throws IllegalArgumentException
		{
			String[] intervalArgs = intervalStr.split("-");
			if (intervalArgs.length >= 2)
				return new SentimentInterval(new Sentiment(Integer.decode(intervalArgs[0])), new Sentiment(Integer
					.decode(intervalArgs[1])));
			else {
				switch (intervalStr.charAt(0)) {
					case '*':
						return SentimentInterval.ANY;
					case '+':
						return SentimentInterval.POSITIVE;
					case '-':
						return SentimentInterval.NEGATIVE;
					default:
						throw new IllegalArgumentException("Invalid sentiment interval");
				}
			}
		}

		public boolean matches(DependencyPattern.Node node)
		{
			return (category == SynsetCategory.NONE || category == node.getCategory())
				&& interval.contains(node.getSentiment());
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return category.name() + interval;
		}
	}
	
	private enum NodeSentimentComparison {
		NONE, STRONGER_DEP, WEAKER_DEP
	}
	
	
	private NodeQuery				govQuery	= new NodeQuery();
	private NodeQuery				depQuery	= new NodeQuery();
	private NodeSentimentComparison	comparison	= NodeSentimentComparison.NONE;
	private GrammaticalRelation		reln;
	

	/*
	 * Format (any part in curly brackets is optional: reln {*|n|v|a|r}{(*)|(+)|(-)|(x,y)} {>|<}
	 * {*|n|v|a|r}{(*)|(+)|(-)|(x,y)}
	 */
	public DependencyQuery(String query) throws IllegalArgumentException
	{
		String[] args = query.split(" ");
		switch (args.length) {
			case 4:
				if (args[3].startsWith(">")) {
					comparison = NodeSentimentComparison.STRONGER_DEP;
				}
				else if (args[3].startsWith("<")) {
					comparison = NodeSentimentComparison.WEAKER_DEP;
				}
				else
					throw new IllegalArgumentException("Invalid relative comparison specifier in query string");
				
				// DO NOT break

			case 3:
				try {
					govQuery = new NodeQuery(args[2]);
				} catch ( IllegalArgumentException e ) {
					throw new IllegalArgumentException(e.getMessage() + " (gov)", e);
				}
				
				try {
					depQuery = new NodeQuery(args[1]);
				} catch ( IllegalArgumentException e ) {
					throw new IllegalArgumentException(e.getMessage() + " (dep)", e);
				}

				// DO NOT break

			case 1:
				reln = EnglishGrammaticalRelations.valueOf(args[0]);
				if (reln == null)
					throw new IllegalArgumentException("Invalid grammatical relation specified in query string");
				break;

			default:
				throw new IllegalArgumentException("Invalid number of arguments in query string");
		}
	}
	
	public boolean matches(DependencyPattern pattern)
	{
		return reln.equals(pattern.getReln()) //
			&& govQuery.matches(pattern.getGov()) //
			&& depQuery.matches(pattern.getDep()) //
			&& compareNodeSentiments(pattern);
	}
	
	private boolean compareNodeSentiments(DependencyPattern pattern)
	{
		switch (comparison) {
			case NONE:
				return true;
			case STRONGER_DEP:
				return pattern.getDep().getSentiment().compareTo(pattern.getGov().getSentiment()) > 0;
			case WEAKER_DEP:
				return pattern.getDep().getSentiment().compareTo(pattern.getGov().getSentiment()) < 0;
			default:
				return false;
		}
	}
	
	public String toString()
	{
		return reln.getLongName() + " dep:" + depQuery + " gov:" + govQuery + " comp:" + comparison.name();
	}

}
