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

import java.util.Set;
import classes.Counter;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class JointTopic<Keyword> extends Topic<Keyword> {
	
	private static final long			serialVersionUID	= -6449825542760558498L;

	private Counter						topicCounter;

	/**
	 * Constructor for class JointTopic
	 */
	public JointTopic()
	{
		super();
		topicCounter = new Counter();
	}
	
	public JointTopic<Keyword> join(SingleTopic<Keyword> topic)
	{
		topicCounter.increment();
		Set<Keyword> keywords = topic.keywords();
		for (Keyword keyword : keywords) {
			addKeyword(keyword, topic.getWeight(keyword));
		}
		
		return this;
	}
	
	private void addKeyword(Keyword keyword, Integer weight)
	{
		topKeywords.put(keyword, topKeywords.containsKey(keyword) ? topKeywords.get(keyword) + weight : weight);
	}

	public int topicCount()
	{
		return topicCounter.get();
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.Topic#keywords()
	 */
	@Override
	public Set<Keyword> keywords()
	{
		return topKeywords(Globals.TopicParameters.topKeywordsCutoff).keySet();
	}
	

	@Override
	public int size()
	{
		return topKeywords(Globals.TopicParameters.topKeywordsCutoff).size();
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.Topic#toSingleTopic()
	 */
	@Override
	public SingleTopic<Keyword> toSingleTopic()
	{
		SingleTopic<Keyword> single_topic = new SingleTopic<Keyword>();

		Set<Keyword> keywords = this.keywords();
		for (Keyword keyword : keywords) {
			single_topic.addKeyword(keyword, getWeight(keyword));
		}
		
		single_topic.setKeywordPrinter(keywordPrinter);

		return single_topic;
	}
	


}
