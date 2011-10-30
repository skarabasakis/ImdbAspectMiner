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
import indexing.Token;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import lexicon.SentimentLexicon;
import lexicon.TermSentiment;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import net.didion.jwnl.data.POS;
import topics.TopicLexicon;
import wordnet.Synset;
import config.Globals;
import dependencies.rules.DependencyPattern;
import dependencies.rules.DependencyRuleSet;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public abstract class DependencyTreeParse extends DependencyTree {
	
	private HashSet<Integer>							clauseRoots;

	protected int										next;
	protected LinkedHashMap<Integer, Sentiment>			nodeSentimentsInit;
	protected LinkedHashMap<Integer, Sentiment>			nodeSentiments;
	protected LinkedHashMap<Integer, String>			nodeTopics;
	protected LinkedHashMap<Integer, Integer>			nodeTopicRoots;
	protected HashMap<Integer, PriorityQueue<Integer>>	nodeTokens;
	protected HashSet<Integer>							traversedNodes;
	
	protected StringBuilder								explanation;
	
	
	/**
	 * Constructor for class DependencyTreeParse
	 * 
	 * @param sentence
	 */
	public DependencyTreeParse(ArrayList<Token> sentence) throws IllegalArgumentException
	{
		super(sentence);
	}
	
	public DependencyTreeParse(DependencyTree tree)
	{
		tokenIndex = tree.tokenIndex;
		relationIndex = tree.relationIndex;
		relationTerms = tree.relationTerms;
	}

	private void reset()
	{
		resetSentiments();
		resetTopics();

		nodeTokens = new HashMap<Integer, PriorityQueue<Integer>>();
		for (Integer nodeid : relationTerms) {
			PriorityQueue<Integer> q = new PriorityQueue<Integer>();
			q.add(nodeid);
			nodeTokens.put(nodeid, q);
		}
		explanation = new StringBuilder();
		next = 0;
		
		traversedNodes = new HashSet<Integer>();
	}
	
	private Sentiment resolvePriorSentiment(Token token, SentimentLexicon lexicon)
	{
		Synset synset = token.synset();
		String term = token.word();
		
		TermSentiment token_sentiment = synset.hasOffset() ? lexicon.getEntry(synset) //
			: lexicon.getEntry(synset.getPos(), term);
		return token_sentiment != null
			&& token_sentiment.getSentiment().getScore() > Globals.DependencyParameters.minimumSentimentScore ? //
		token_sentiment.getSentiment() //
			: TermSentiment.NEUTRAL_SENTIMENT.getSentiment();
		
		// TODO comparison degree
	}
	
	private String resolveTopicRelation(Token token, TopicLexicon<Synset> topiclexicon)
	{
		return topiclexicon.lookupTopicName(token.synset());
	}
	

	public void initSentiments(SentimentLexicon lexicon)
	{
		nodeSentimentsInit = new LinkedHashMap<Integer, Sentiment>();

		for (Integer nodeid : relationTerms) {
			Sentiment s = resolvePriorSentiment(tokenIndex.get(nodeid - 1), lexicon);
			nodeSentimentsInit.put(nodeid, s);
		}
		
		resetSentiments();
	}
	
	private void resetSentiments()
	{
		if (nodeSentimentsInit != null) {
			nodeSentiments = new LinkedHashMap<Integer, Sentiment>();
			Set<Entry<Integer, Sentiment>> entries = nodeSentimentsInit.entrySet();
			for (Entry<Integer, Sentiment> entry : entries) {
				nodeSentiments.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void initSentiments(SentimentLexicon mainLexicon, SentimentLexicon auxLexicon)
	{
		nodeSentimentsInit = new LinkedHashMap<Integer, Sentiment>();
		
		if (relationTerms.isEmpty()) {
			
		}
		else {
			for (Integer nodeid : relationTerms) {
				Sentiment s = resolvePriorSentiment(tokenIndex.get(nodeid - 1), mainLexicon);
				Sentiment aux = resolvePriorSentiment(tokenIndex.get(nodeid - 1), auxLexicon);
				if (!aux.isNeutral() && s.getPolarity() != aux.getPolarity()) {
					nodeSentimentsInit.put(nodeid, aux);
				}
				else {
					nodeSentimentsInit.put(nodeid, s);
				}
			}
		}
		
		resetSentiments();
	}

	private void initClauseRoots(DependencyRuleSet ruleset)
	{
		clauseRoots = new HashSet<Integer>();
		for (TypedDependencyWrapper relation : relationIndex) {
			if (ruleset.isClausalRelation(relation.getRelation())) {
				clauseRoots.add(relation.getDepIndex());
			}
		}
	}

	public void initTopics(TopicLexicon<Synset> topiclexicon)
	{
		nodeTopics = new LinkedHashMap<Integer, String>();

		for (Integer nodeid : relationTerms) {
			String topicname = resolveTopicRelation(tokenIndex.get(nodeid - 1), topiclexicon);
			if (topicname != null && PosTag.toPOS(tokenIndex.get(nodeid - 1).type) != POS.VERB) {
				nodeTopics.put(nodeid, topicname);
				nodeSentiments.put(nodeid, TermSentiment.NEUTRAL_SENTIMENT.getSentiment());
			}
		}
		
		resetTopics();
		
		// Assign global topic to head node, i.e. the gov node of the last relation
		try {
			nodeTopics.put(0, Globals.TopicParameters.globalTopicName);
			nodeTopicRoots.put(0, root());
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void resetTopics()
	{
		HashSet<Integer> topics_to_remove = new HashSet<Integer>();

		if (nodeTopics != null) {
			nodeTopicRoots = new LinkedHashMap<Integer, Integer>();
			
			Set<Integer> topicids = nodeTopics.keySet();
			for (Integer topicid : topicids) {
				Integer subroot = subroot(topicid);
				if (subroot != null) {
					nodeTopicRoots.put(topicid, subroot);
				}
				else {
					topics_to_remove.remove(topicid);
				}
			}
		}
		
		for (Integer topicid : topics_to_remove) {
			nodeTopics.remove(topicid);
		}
	}
	
	protected Integer subroot(int node)
	{
		ArrayList<Integer> pathToRoot = pathToRoot(node);

		Integer ancestor, previous_ancestor = node;
		for (int i = pathToRoot.size() - 1 ; i >= 0 ; i--) {
			ancestor = pathToRoot.get(i);
			
			// If current relation separates clauses, don't climb further up
			if (clauseRoots.contains(ancestor))
				return ancestor;
			
			// If current ancestor is a topic term, don't climb further up
			if (nodeTopicRoots.keySet().contains(ancestor)) {
				if (nodeTopics.get(ancestor).equals(nodeTopics.get(node)))
					return null;
				else
					return previous_ancestor;
			}
			
			previous_ancestor = ancestor;
		}
		
		return node;
	}

	private String getTopicName(int rootId)
	{
		for (Entry<Integer, Integer> entry : nodeTopicRoots.entrySet()) {
			if (entry.getValue() == rootId)
				if (entry.getKey() != 0)
					return nodeTopics.get(entry.getKey());
		}
		
		return null;
	}

	private boolean hasNext()
	{
		return next < relationIndex.size();
	}

	private DependencyPattern getPatternOf(TypedDependencyWrapper reln)
	{
		// Collect gov node info
		Integer gov = reln.getGovIndex();
		Token govt = getToken(gov);
		Sentiment govs = nodeSentiments.get(gov);
		
		// Collect dep node info
		Integer dep = reln.getDepIndex();
		Token dept = getToken(dep);
		Sentiment deps = nodeSentiments.get(dep);
		
		// Compose dependency pattern based on current (gov,dep,reln) tuple
		return new DependencyPattern(govt, dept, govs, deps, reln);
	}
	
	/**
	 * @param gov
	 * @param dep
	 */
	// private void applyCurrentTopic(Integer gov, Integer dep)
	// {
	// int relns = relationIndex.size();
	//
	// // For each topic that currently sits on dep
	// for (Entry<Integer, Integer> topic : nodeTopicRoots.entrySet()) {
	// if (dep == topic.getValue()) {
	// boolean go_up = true;
	//
	// // Return without doing anything, if the dependency tree branches right above this
	// // topic, i.e. if there is another not-yet-traversed relation with the same gov
	// // node, whose dep is not a neutral leaf.
	// for (int reln = next + 1 ; reln < relns ; reln++) {
	// if (gov == relationIndex.get(reln).getGovIndex()) {
	// int branch_dep = relationIndex.get(reln).getDepIndex();
	// if (!(leafNodes.contains(branch_dep) && nodeSentiments.get(branch_dep).isNeutral())) {
	// go_up = false;
	// break;
	// }
	// }
	// }
	//
	// // If no branch right above, then make gov the root of the current topic subtree
	// if (go_up) {
	// nodeTopicRoots.put(topic.getKey(), gov);
	// }
	// }
	// }
	// }
	

	private void traverseNext(DependencyRuleSet ruleset)
	{
		// Retrieve requested relation
		TypedDependencyWrapper reln = relationIndex.get(next);
		Integer gov = reln.getGovIndex();
		Integer dep = reln.getDepIndex();
		DependencyPattern pattern = getPatternOf(reln);
		
		traversedNodes.add(dep);
		
		// Apply matching rule from the ruleset, updating tree and explanation
		explanation.append(getRuleConditionsString(gov, dep));
		applyClosestRule(ruleset, gov, dep, pattern);
		// applyCurrentTopic(gov, dep);
		mergeNodes(gov, dep);
		explanation.append(getRuleOutcomeString(gov));
		
		// Call visualization code here
		if (super.relationIndex.size() > 0) {
			visualize();
		}
		
		traversedNodes.add(gov);

		next++;
	}
	

	private void mergeNodes(int govIndex, int depIndex)
	{
		nodeTokens.get(govIndex).addAll(nodeTokens.get(depIndex));
		// nodeTokens.remove(depIndex); // Remove dep token
	}
	
	private void applyClosestRule(DependencyRuleSet ruleset, int govIndex, int depIndex, DependencyPattern pattern)
	{
		Sentiment result = ruleset.applyMatchingRule(pattern);
		nodeSentiments.put(govIndex, result); // Store result sentiment as sentiment of gov
		// if (!nodeTopics.containsKey(depIndex)) {
		// nodeSentiments.remove(depIndex); // Remove sentiment of dep, unless dep represents a
		// // topic
		// }
	}
	
	private String getRuleConditionsString(int govIndex, int depIndex)
	{
		StringBuilder str = new StringBuilder();
		
		str.append(getNodeString(depIndex)). //
			append('[').append(nodeSentiments.get(depIndex).toString(SentimentFormat.RATING, false)).append(']');
		str.append(" x ");
		str.append(getNodeString(govIndex)). //
			append('[').append(nodeSentiments.get(govIndex).toString(SentimentFormat.RATING, false)).append(']');
		
		return str.toString();
	}
	
	private String getRuleOutcomeString(int govIndex)
	{
		return " --> " + nodeSentiments.get(govIndex).toString(SentimentFormat.RATING, false) + "\n";
	}


	private String getNodeString(int index)
	{
		Iterator<Integer> tokens = nodeTokens.get(index).iterator();
		String combined = tokenIndex.get(tokens.next() - 1).word();
		while ( tokens.hasNext() ) {
			combined += " " + tokenIndex.get(tokens.next() - 1).word();
		}
		return combined;
	}
	
	public LinkedHashMap<String, Sentiment> parse(DependencyRuleSet ruleset, SentimentLexicon sentilexicon,
			SentimentLexicon auxLexicon, TopicLexicon<Synset> topiclexicon, boolean print_output)
	{
		reset();
		initClauseRoots(ruleset);
		initSentiments(sentilexicon, auxLexicon);
		initTopics(topiclexicon);
		
		// if (super.relationIndex.size() > 0) {
		// visualize_init();
		// }

		while ( hasNext() ) {
			traverseNext(ruleset);
		}
		traversedNodes.add(Globals.TopicParameters.globalTopicId);
		
		if (print_output) {
			System.out.println(explanation);
		}
		
		// Set<Entry<Integer, Sentiment>> sentiment = nodeSentiments.entrySet();
		// if (sentiment.size() != 1) {
		// AppLogger.error.log(Level.WARNING,
		// "More than a single sentiment entry after tree parse complete");
		// }
		if (super.relationIndex.size() > 0) {
			visualize();
		}
		return getTopicSentimentMap();
	}
	
	/**
	 * @return
	 */
	private LinkedHashMap<String, Sentiment> getTopicSentimentMap()
	{
		LinkedHashMap<String, Sentiment> topicSentimentMap = new LinkedHashMap<String, Sentiment>();
		
		Set<Entry<Integer, Integer>> topic_entries = nodeTopicRoots.entrySet();
		for (Entry<Integer, Integer> topic_entry : topic_entries) {
			if (topicSentimentMap.get(topic_entry.getKey()) == null) {
				topicSentimentMap.put(nodeTopics.get(topic_entry.getKey()), nodeSentiments.get(topic_entry.getValue()));
			}
			else {
				topicSentimentMap.put(nodeTopics.get(topic_entry.getKey()) + "_" + topic_entry.getValue(),
										nodeSentiments.get(topic_entry.getValue()));
			}
		}
		return topicSentimentMap;
	}
	
	public ArrayList<DependencyExample> findExamples(DependencyQuery query, SentimentLexicon sentilexicon)
	{
		initSentiments(sentilexicon);

		ArrayList<DependencyExample> examples = new ArrayList<DependencyExample>();
		for (TypedDependencyWrapper reln : relationIndex) {
			DependencyPattern pattern = getPatternOf(reln);
			if (query.matches(pattern)) {
				examples.add(new DependencyExample(pattern, getTerm(reln.getGovIndex()), getTerm(reln.getDepIndex()),
						getSentence(reln, 15)));
			}
		}
		
		return examples;
	}
	
	abstract protected void visualize_init();

	abstract protected void visualize();

}
