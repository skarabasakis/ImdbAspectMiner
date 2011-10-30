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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import topics.Topic.KeywordPrinter;
import util.AppLogger;
import classes.Counter;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class TopicSet<Keyword> implements TopicLexicon<Keyword>, Serializable {
	
	private static final long					serialVersionUID	= -2810790800744264929L;
	
	protected String							descriptor;
	protected Counter							topicCounter;
	protected HashMap<Integer, Topic<Keyword>>	topics;
	protected HashMap<Keyword, Integer>			keywordTopicIndex;
	protected boolean							disableUnnamed		= false;
	
	/**
	 * Constructor for class TopicSet
	 */
	public TopicSet(String descriptor)
	{
		this.descriptor = descriptor;
		topicCounter = new Counter();
		topics = new HashMap<Integer, Topic<Keyword>>();
		keywordTopicIndex = new HashMap<Keyword, Integer>();
	}
	
	
	/**
	 * @param disableUnnamed
	 *            the disableUnnamed to set
	 */
	public void disableUnnamed(boolean disableUnnamed)
	{
		this.disableUnnamed = disableUnnamed;
	}

	public void addTopic(Topic<Keyword> topic)
	{
		topicCounter.increment();
		topics.put(topicCounter.get(), topic);
		if (!disableUnnamed || topic.hasName()) {
			indexTopic(topicCounter.get());
		}
	}
	
	public void replaceTopic(Integer topic_id, Topic<Keyword> topic)
	{
		topics.put(topic_id, topic);
		if (!disableUnnamed || topic.hasName()) {
			indexTopic(topic_id);
		}
	}
	
	public void indexAll()
	{
		keywordTopicIndex = new HashMap<Keyword, Integer>();
		Set<Entry<Integer, Topic<Keyword>>> topic_entries = topics.entrySet();
		for (Entry<Integer, Topic<Keyword>> topic_entry : topics.entrySet()) {
			if (!disableUnnamed || topic_entry.getValue().hasName()) {
				indexTopic(topic_entry.getKey());
			}
		}
	}
	
	private void indexTopic(Integer topic_id)
	{
		Topic<Keyword> topic = topics.get(topic_id);
		if (topic != null) {
			Set<Keyword> keywords = topic.keywords();
			for (Keyword keyword : keywords) {
				if (keywordTopicIndex.get(keyword) != topic_id) {
					indexKeyword(keyword, topic.getWeight(keyword), topic_id);
				}
			}
		}
		else {
			AppLogger.error.log(Level.WARNING, "Cannot index topic with id " + topic_id
				+ " because it is not present in topic set");
		}
	}
	
	private void indexKeyword(Keyword keyword, Integer weight, Integer topic_id)
	{
		if (keywordTopicIndex.containsKey(keyword)) {
			Integer previous_weight = getTopic(keywordTopicIndex.get(keyword)).getWeight(keyword);
			if (weight > previous_weight) {
				keywordTopicIndex.put(keyword, topic_id);
			}
		}
		else {
			keywordTopicIndex.put(keyword, topic_id);
		}
	}
	
	public Topic<Keyword> lookup(Keyword keyword)
	{
		return getTopic(keywordTopicIndex.get(keyword));
	}

	public Topic<Keyword> getTopic(Integer topic_id)
	{
		return topics.get(topic_id);
	}
	
	public Set<Integer> topicIds()
	{
		return new HashSet<Integer>(topics.keySet());
	}

	public TopicSet<Keyword> mergeWith(TopicSet<Keyword> newTopicSet, boolean verbose)
	{
		Set<Integer> old_topicset_ids = this.topicIds();
		Set<Integer> new_topicset_ids = newTopicSet.topicIds();
		
		for (Integer new_topic_id : new_topicset_ids) {
			Topic<Keyword> new_topic = newTopicSet.getTopic(new_topic_id);
			
			Integer closest_old_topic_id = 0;
			Float closest_old_topic_similarity = 0.0F;
			for (Integer old_topic_id : old_topicset_ids) {
				Float old_topic_similarity = this.getTopic(old_topic_id).similarity(new_topic);
				if (old_topic_similarity > closest_old_topic_similarity) {
					closest_old_topic_similarity = old_topic_similarity;
					closest_old_topic_id = old_topic_id;
				}
			}
			
			if (closest_old_topic_similarity >= Globals.TopicParameters.topicMergeSimilarityThreshold) {
				JointTopic<Keyword> merged_topic = getTopic(closest_old_topic_id).join(new_topic.toSingleTopic());
				replaceTopic(closest_old_topic_id, merged_topic);
				old_topicset_ids.remove(closest_old_topic_id);
				if (verbose) {
					System.out.println("NEW TOPIC " + new_topic_id + " matched to OLD TOPIC " + closest_old_topic_id
						+ "\tsimilarity: " + closest_old_topic_similarity);
				}
			}
			else {
				this.addTopic(new_topic);
				if (verbose) {
					System.out.println("NEW TOPIC " + topicCounter.get() + " added to set. \tsimilarity: "
						+ closest_old_topic_similarity);
				}
			}
		}
		
		return this;
	}
	
	public void assignNames(int minTopicCount)
	{
		Set<Integer> topic_ids = topics.keySet();
		keywordTopicIndex = new HashMap<Keyword, Integer>();
		for (Integer topic_id : topic_ids) {
			Topic<Keyword> topic = topics.get(topic_id);
			if (topic.topicCount() > minTopicCount) {
				System.out.print("\n\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n\n"
					+ "Please pick a name for the following topic, or leave empty to delete topic \n\n");
				System.out.println("TOPIC #" + topic_id + "\n" + topic.size() + " top keywords from "
					+ (topic.topicCount() > 1 ? topic.topicCount() + " joint topics" : "a single topic"));
				System.out.println(topic.toString(new TreeSet<Keyword>(keywordTopicIndex.keySet())));
				topic.setNameInteractive();
				indexTopic(topic_id);
			}
		}
	}
	
	
	/**
	 * 
	 */
	public void setKeywordPrinter(KeywordPrinter<Keyword> printer)
	{
		Collection<Topic<Keyword>> topics = this.topics.values();
		for (Topic<Keyword> topic : topics) {
			topic.setKeywordPrinter(printer);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getTopicDetailsString(false);
		// return getKeywordOrderedFrequenciesString();
	}

	public String getTopicsString(boolean named_only)
	{
		StringBuilder str = new StringBuilder();
		
		Set<Entry<Integer, Topic<Keyword>>> topicset = topics.entrySet();
		for (Entry<Integer, Topic<Keyword>> topic_entry : topicset) {
			Topic<Keyword> topic = topic_entry.getValue();
			if (named_only == false || topic.hasName()) {
				// HashSet<Keyword> blacklist = keywordTopicIndex.r
				str.append(topic.getName()).append(":") //
					.append(topic.toString(getTopicBlacklist(topic_entry.getKey()))) //
					.append('\n');
			}
		}
		
		return str.toString();
	}
	
	private Set<Keyword> getTopicTerms(int topic_id)
	{
		Set<Keyword> keywords = new HashSet<Keyword>(topics.get(topic_id).keywords());
		Set<Keyword> tokeep = new HashSet<Keyword>();
		for (Keyword keyword : keywords) {
			if (topic_id == keywordTopicIndex.get(keyword)) {
				tokeep.add(keyword);
			}
		}
		
		keywords.retainAll(tokeep);
		return keywords;
	}
	
	private Set<Keyword> getTopicBlacklist(int topic_id)
	{
		Set<Keyword> keywords = new HashSet<Keyword>(topics.get(topic_id).keywords());
		Set<Keyword> toremove = new HashSet<Keyword>();
		for (Keyword keyword : keywords) {
			if (topic_id == keywordTopicIndex.get(keyword)) {
				toremove.add(keyword);
			}
		}
		
		keywords.removeAll(toremove);
		return keywords;
	}

	public String getTopicDetailsString(boolean named_only)
	{
		StringBuilder str = new StringBuilder();

		Set<Entry<Integer, Topic<Keyword>>> topicset = topics.entrySet();
		for (Entry<Integer, Topic<Keyword>> topic_entry : topicset) {
			Topic<Keyword> topic = topic_entry.getValue();
			
			// Updating topic keywords if necessary before topic is printed
			// topic.keywords();
			
			if (named_only == false || topic.hasName()) {
				str.append("TOPIC #" + topic_entry.getKey() + ": " + topic.getName() + "\n") //
					.append(topic.size() + " top keywords from ") //
					.append(topic.topicCount() > 1 ? topic.topicCount() + " joint topics" : "a single topic") //
					.append("\n\n") //
					.append(topic.toString()) //
					.append("\n\n");
			}
		}
		
		return str.toString();
	}
	
	private KeywordPrinter<Keyword> getDefaultKeywordPrinter()
	{
		// A very hack-ish way to get a reference to a working keyword printer. It won't work if the
		// topicset is empty.
		return topics.get(1).keywordPrinter;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#isTopic(java.lang.Object)
	 */
	@Override
	public boolean isTopic(Keyword keyword)
	{
		return keywordTopicIndex.containsKey(keyword);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#lookupTopicId(java.lang.Object)
	 */
	@Override
	public int lookupTopicId(Keyword keyword)
	{
		return isTopic(keyword) ? 0 : keywordTopicIndex.get(keyword);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#lookupTopicName(java.lang.Object)
	 */
	@Override
	public String lookupTopicName(Keyword keyword)
	{
		Integer topic_id = keywordTopicIndex.get(keyword);
		return topic_id == null ? null : topics.get(topic_id).getName();
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#renameTopic(java.lang.String, java.lang.String)
	 */
	@Override
	public void renameTopic(String oldname, String newname)
	{
		Collection<Topic<Keyword>> topicset = topics.values();
		for (Topic<Keyword> topic : topicset) {
			if (topic.getName().equals(oldname)) {
				topic.setName(newname);
			}
		}
	}
	
	
	public FastLookupTopicLexicon<Keyword> exportLexicon()
	{
		indexAll();
		Set<Entry<Keyword, Integer>> index_entries = keywordTopicIndex.entrySet();
		HashMap<Keyword, String> lexicon_entries = new HashMap<Keyword, String>();
		for (Entry<Keyword, Integer> index_entry : index_entries) {
			lexicon_entries.put(index_entry.getKey(), topics.get(index_entry.getValue()).getName());
		}
		return new FastLookupTopicLexicon<Keyword>(lexicon_entries);
	}
	
	public void printClusterSizeReport(int max_topic_size)
	{
		// Count named vs unnamed topics
		Counter[] named_topics = new Counter[max_topic_size + 1];
		Counter[] total_topics = new Counter[max_topic_size + 1];
		for (int i = 0 ; i <= max_topic_size ; i++) {
			named_topics[i] = new Counter();
			total_topics[i] = new Counter();
		}

		Collection<Topic<Keyword>> topicset = this.topics.values();
		for (Topic<Keyword> topic : topicset) {
			int topiccount = topic.topicCount();
			if (topic.hasName()) {
				named_topics[topiccount].increment();
			}
			total_topics[topiccount].increment();
		}
		
		// Count named vs unnamed topic keywords
		Counter[] named_topic_keywords = new Counter[max_topic_size + 1];
		Counter[] total_topic_keywords = new Counter[max_topic_size + 1];
		for (int i = 0 ; i <= max_topic_size ; i++) {
			named_topic_keywords[i] = new Counter();
			total_topic_keywords[i] = new Counter();
		}

		Set<Entry<Keyword, Integer>> keywords = this.keywordTopicIndex.entrySet();
		for (Entry<Keyword, Integer> keyword : keywords) {
			Topic<Keyword> topic = topics.get(keyword.getValue());
			int topiccount = topic.topicCount();
			if (topic.hasName()) {
				named_topic_keywords[topiccount].increment();
			}
			total_topic_keywords[topiccount].increment();

		}
		

		// Calculating totals
		for (int i = max_topic_size ; i > 0 ; i--) {
			named_topics[0].add(named_topics[i].get());
			total_topics[0].add(total_topics[i].get());
			named_topic_keywords[0].add(named_topic_keywords[i].get());
			total_topic_keywords[0].add(total_topic_keywords[i].get());
		}
		
		// Print report
		System.out.println("\n\nTopic Cluster Size\tAspect Topics\tAspect Keywords");
		for (int i = max_topic_size ; i >= 0 ; i--) {
			System.out.println(i + "\t" + named_topics[i] + " / " + total_topics[i] + "\t" + named_topic_keywords[i]
				+ " / " + total_topic_keywords[i]);
		}
	}
	
	public void printAspectReport(int max_keywords, KeywordPrinter<Keyword> printer)
	{
		HashMap<String, Counter> aspect_topic_counts = new HashMap<String, Counter>();
		
		Set<Entry<Integer, Topic<Keyword>>> topic_entries = topics.entrySet();
		for (Entry<Integer, Topic<Keyword>> topic_entry : topic_entries) {
			Topic<Keyword> topic = topic_entry.getValue();
			if (topic.hasName()) {
				String topicname = topic.getName();
				if (!aspect_topic_counts.containsKey(topicname)) {
					aspect_topic_counts.put(topicname, new Counter());
				}
				aspect_topic_counts.get(topicname).increment();
			}
		}

		TreeMap<String, LinkedList<Keyword>> aspect_keywords = new TreeMap<String, LinkedList<Keyword>>();
		TreeMap<String, LinkedList<Integer>> aspect_keyword_weights = new TreeMap<String, LinkedList<Integer>>();
		
		Set<Entry<Keyword, Integer>> keyword_entries = keywordTopicIndex.entrySet();
		for (Entry<Keyword, Integer> keyword_entry : keyword_entries) {
			Topic<Keyword> topic = topics.get(keyword_entry.getValue());
			String topicname = topic.getName();
			int keyword_weight = topic.getWeight(keyword_entry.getKey());
			
			if (!aspect_keywords.containsKey(topicname)) {
				aspect_keywords.put(topicname, new LinkedList<Keyword>());
				aspect_keyword_weights.put(topicname, new LinkedList<Integer>());
			}
			
			LinkedList<Integer> weights = aspect_keyword_weights.get(topicname);
			for (int index = weights.size() ; index >= 0 ; index--) {
				if (index == 0 || keyword_weight <= weights.get(index - 1)) {
					aspect_keywords.get(topicname).add(index, keyword_entry.getKey());
					aspect_keyword_weights.get(topicname).add(index, keyword_weight);
					break;
				}
			}
		}
		
		// Printing report
		Set<String> aspects = aspect_keywords.keySet();
		System.out.println("\n\nASPECT\t#Topics\t#Keywords\tSample");
		for (String aspect : aspects) {
			LinkedList<Keyword> keyword_list = aspect_keywords.get(aspect);
			System.out.print(aspect + "\t" + aspect_topic_counts.get(aspect) + "\t" + keyword_list.size() + "\t");
			int keywords_size = max_keywords == 0 ? keyword_list.size() : max_keywords;
			for (int i = 0 ; i < keywords_size ; i++) {
				System.out.print("{ " + printer.print(keyword_list.get(i)) + " } ");
			}
			System.out.println();
		}
	}
}
